/*
 * Copyright (c) 2026.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.github.pdvrieze.formats.xpath.functions.impl

import io.github.pdvrieze.formats.xmlschema.regex.XRegex
import io.github.pdvrieze.formats.xmlschema.regex.impl.RegexVariant
import io.github.pdvrieze.formats.xmlschema.regex.impl.XMatchResult
import io.github.pdvrieze.formats.xmlschema.regex.impl.XRPatternSyntaxException
import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.data.XdmAtomic
import io.github.pdvrieze.formats.xpath.eval.data.XdmAtomicOrSequence
import io.github.pdvrieze.formats.xpath.eval.data.XdmSequence
import io.github.pdvrieze.formats.xpath.eval.data.XdmString
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmNodeKindTest
import io.github.pdvrieze.formats.xpath.functions.*
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunction.Companion.FN_NAMESPACE
import io.github.pdvrieze.formats.xpath.impl.*
import io.github.pdvrieze.xml.schematypes.types.Base64BinaryType
import io.github.pdvrieze.xml.schematypes.values.XsdDouble
import io.github.pdvrieze.xml.schematypes.values.XsdInteger
import io.github.pdvrieze.xml.schematypes.values.XsdString
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.core.internal.appendCodepoint
import nl.adaptivity.xmlutil.core.internal.codepointAt
import nl.adaptivity.xmlutil.core.internal.nextCodePointPos
import nl.adaptivity.xmlutil.xmlCollapseWhitespace
import kotlin.jvm.JvmInline
import kotlin.math.roundToInt

@XPathInternal
object StringFunctions : AbstractFunctionObject() {

    //region 5.2 functions to assemble and disassemble strings
    val fnCodepointsToString = BuiltinFunctionImpl.Fn("codepoints-to-string", STRING, INTEGER.any) Fn@{ args ->
        val arg = args[0].asSequence().map { ((it as XdmAtomic<*>).value as XsdInteger) }

        val s = buildString {
            for (cpInt in arg) {
                when(val cp = cpInt.toLong()) {
                    0L, in 0xD800..0xDFFF
                        -> throw EvaluationException(ErrorCodes.FOCH0001, "0x${cp.toString(16)} is not a valid xml codepoint")

                    in Int.MIN_VALUE..-1
                        -> throw EvaluationException(ErrorCodes.FOCH0001, "negative values are not valid codepoints")

                    in 0xFDD0..0xFDEF,
                    0xFFFEL, 0xFFFFL
                        -> throw EvaluationException(ErrorCodes.FOCH0001, "NonCharacter (0x${cp.toString(16)})")

                    in 0x110000L..Long.MAX_VALUE
                        -> throw EvaluationException(ErrorCodes.FOCH0001, "codepoint out of range")

                    else -> appendCodepoint(cp.toInt())
                }
            }
        }

        atomic(s)
    }

    val fnStringToCodepoints = BuiltinFunctionImpl.Fn("string-to-codepoints", INTEGER.any, STRING.opt) Fn@{ args ->
        val arg = args.atomicArgOrEmpty<XsdString>(0)?.xmlString ?: return@Fn XdmSequence.EMPTY

        val result = buildList(arg.length) {
            var i = 0
            while (i < arg.length) {
                add(atomic(arg.codepointAt(i)))
                i = arg.nextCodePointPos(i)
            }
        }

        XdmSequence.fromList(result, INTEGER.opt.toValueType())
    }

    //endregion

    //region 5.3 Comparison of strings

    val fnCompare = BuiltinFunctionImpl.Fn("compare", listOf(
        functionType(INTEGER, STRING.opt, STRING.opt),
        functionType(INTEGER, STRING.opt, STRING.opt, STRING),
    )) Fn@{ args ->
        val comparand1 = args.atomicArgOrEmpty<XsdString>(0) ?: return@Fn XdmSequence.EMPTY
        val comparand2 = args.atomicArgOrEmpty<XsdString>(1) ?: return@Fn XdmSequence.EMPTY
        val collation = args.maybeCollation(2)
        atomic(collation.compare(comparand1.xmlString, comparand2.xmlString))
    }

    val fnCodePointEqual = BuiltinFunctionImpl.Fn("codepoint-equal", BOOLEAN.opt, STRING.opt, STRING.opt) Fn@{ args ->
        val comparand1 = args.atomicArgOrEmpty<XsdString>(0) ?: return@Fn XdmSequence.EMPTY
        val comparand2 = args.atomicArgOrEmpty<XsdString>(1) ?: return@Fn XdmSequence.EMPTY

        atomic(comparand1.xmlString == comparand2.xmlString)
    }

    val fnCollationKey = BuiltinFunctionImpl.Fn("collation-key", listOf(
        functionType(Base64BinaryType.Instance,STRING),
        functionType(Base64BinaryType.Instance,STRING, STRING),
    )) Fn@{ args ->
        val key = args.atomicArgN<XsdString>(0).xmlString
        val collation = args.maybeCollation(1)
        atomic(collation.key(key))
    }

    val fnContainsToken = BuiltinFunctionImpl.Fn("contains-token", listOf(
        functionType(BOOLEAN, STRING.any, STRING),
        functionType(BOOLEAN, STRING.any, STRING, STRING),
    )) Fn@{ args ->
        val input = args[0]
        val token = args[1]
        val collation = args.maybeCollation(2)
        if (input.isEmpty()) return@Fn XdmSequence.EMPTY
        val tokens = fnTokenize(token).mapTo(HashSet()) { collation.key(it.value.xmlString) }
        val inputStrings = input.asSequence().map { collation.key((it as XdmAtomic<*>).value.xmlString) }

        atomic(inputStrings.any { a -> a in tokens })
    }

    //endregion

    //region 5.4 functions on string values
    val fnConcat: BuiltinFunctionImpl<XdmString> = BuiltinFunctionImpl.Fn("concat", flexFunctionType(STRING, ATOMIC.opt, ATOMIC.opt)) Fn@{ args ->
        if (args.size < 2) throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT, "Concat requires at least two arguments")
        val concat = args.asSequence().map { Accessors.fnString(it).value.xmlString }.joinToString("")
        atomic(concat)
    }

    val fnStringJoin: BuiltinFunctionImpl<XdmString> = BuiltinFunctionImpl.Fn("string-join", contextFunctionTypes(STRING, STRING, ATOMIC.any)) Fn@{ args ->
        if (args.size > 2) throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT, "String-join takes 1 or 2 arguments")
        val seq = args.argN<XdmAtomicOrSequence<XdmAtomic<*>>>(0)
        val separator = if (args.size == 2) args.atomicArgN<XsdString>(1) else ""
        val join = seq.asSequence().map { Accessors.fnString(it).value.xmlString }.joinToString(separator)
        atomic(join)
    }

    val fnSubstring = BuiltinFunctionImpl.Fn("substring", listOf(
        functionType(STRING, STRING.opt, DOUBLE, DOUBLE),
        functionType(STRING, STRING.opt, DOUBLE),
    )) Fn@{ args ->
        val sourceString = args.atomicArgOrEmpty<XsdString>(0) ?: return@Fn XdmSequence.EMPTY
        val start = args.atomicArgN<XsdDouble>(1).value.roundToInt() - 1
        val length = if (args.size == 2) Int.MAX_VALUE else args.atomicArgN<XsdDouble>(2).value.roundToInt()

        var result = buildString {
            var sourcePos: Int = 0
            var charCount = 0
            while (sourcePos < sourceString.length && charCount < start) {
                sourcePos = sourceString.nextCodePointPos(sourcePos)
                charCount += 1
            }
            charCount = 0
            while (sourcePos < sourceString.length && charCount < length) {
                appendCodepoint(sourceString.codepointAt(sourcePos))
                sourcePos = sourceString.nextCodePointPos(sourcePos)
                charCount += 1
            }
        }

        atomic(result)
    }

    val fnStringLength = BuiltinFunctionImpl.Fn("string-length", contextFunctionTypes(INTEGER, STRING.opt)) Fn@{ args ->
        val arg = args.toSingleAtomic<XsdString>(true) ?: return@Fn atomic(0)

        atomic(arg.xmlString.length)
    }

    val fnNormalizeSpace = BuiltinFunctionImpl.Fn("normalize-space", contextFunctionTypes(STRING, STRING.opt)) Fn@{ args ->
        val arg = args.toSingleAtomic<XsdString>(true) ?: return@Fn atomic("")

        atomic(xmlCollapseWhitespace(arg.xmlString))
    }

    val fnNormalizeUnicode = BuiltinFunctionImpl.Fn("normalize-unicode", listOf(
        functionType(STRING, STRING.opt, STRING),
        functionType(STRING, STRING.opt),
    )) Fn@{ args ->
        //XdmAtomic(XsdString(""))
        TODO("Unicode normalization not yet supported")
    }

    val fnUpperCase = BuiltinFunctionImpl.Fn("upper-case", functionType(STRING, STRING.opt)) Fn@{ args ->
        val arg = args.toSingleAtomic<XsdString>() ?: return@Fn atomic("")

        atomic(arg.xmlString.uppercase())
    }

    val fnLowerCase = BuiltinFunctionImpl.Fn("lower-case", functionType(STRING, STRING.opt)) Fn@{ args ->
        val arg = args.toSingleAtomic<XsdString>() ?: return@Fn atomic("")

        atomic(arg.xmlString.lowercase())
    }

    private fun Appendable.translateCodepoints(arg: String, mapArray: IntArray, transArray: IntArray) {
        var pos = 0
        while (pos < arg.length) {
            val cp = arg.codepointAt(pos)
            val idx = mapArray.indexOf(cp)
            if (idx >=0) {
                appendCodepoint(transArray[idx])
            } else {
                appendCodepoint(cp)
            }

            pos += arg.nextCodePointPos(pos)
        }
    }

    private fun String.getCodepointArray(): IntArray {
        val len = this.count { !it.isLowSurrogate() } // skip low surrogates to get character length
        val result = IntArray(len)
        var pos = 0
        var charPos = 0
        while (pos < length) {
            result[charPos++] = codepointAt(pos)
            pos = nextCodePointPos(pos)
        }
        return result
    }

    val fnTranslate = BuiltinFunctionImpl.Fn("translate", functionType(STRING, STRING.opt, STRING, STRING)) Fn@{ args ->
        val arg = args.atomicOrEmpty<XsdString>(0)?.xmlString ?: return@Fn atomic("")
        val mapString = args.atomicArgN<XsdString>(1).xmlString
        val transString = args.atomicArgN<XsdString>(1).xmlString

        val result = buildString {
            if (mapString.any { it.isSurrogate() } || transString.any { it.isSurrogate() }) {
                translateCodepoints(arg, mapString.getCodepointArray(), transString.getCodepointArray())
            } else {
                // note we can ignore codepoints as they cannot need translation
                for (c in arg) {
                    val repIdx = mapString.indexOf(c)
                    if (repIdx >= 0) append(transString[repIdx])
                    else append(c)
                }
            }
        }

        atomic(result)
    }
    //endregion

    //region 5.5 Functions on substring matching
    val fnSubstringBefore = BuiltinFunctionImpl.Fn("substring-before", listOf(
        functionType(STRING, STRING.opt, STRING.opt),
        functionType(STRING, STRING.opt, STRING.opt, STRING),
    )) Fn@{ args ->
        val arg1 = args.atomicOrEmpty<XsdString>(0)?.xmlString ?: ""
        val arg2 = args.atomicOrEmpty<XsdString>(1)?.xmlString ?: return@Fn atomic(arg1)
        // TODO support collation
        atomic(arg1.substringBefore(arg2, ""))
    }

    val fnSubstringAfter = BuiltinFunctionImpl.Fn("substring-after", listOf(
        functionType(STRING, STRING.opt, STRING.opt),
        functionType(STRING, STRING.opt, STRING.opt, STRING),
    )) Fn@{ args ->
        val arg1 = args.atomicOrEmpty<XsdString>(0)?.xmlString ?: ""
        val arg2 = args.atomicOrEmpty<XsdString>(1)?.xmlString ?: return@Fn atomic(arg1)
        // TODO support collation
        atomic(arg1.substringAfter(arg2, ""))
    }

    //endregion

    //region 5.6 String functions using regex

    context(ctx: ExprEvalContext)
    private fun parseFlags(flags: String): RegexFlags {
        var r = RegexFlags()

        for (c in flags) {
            when (c) {
                's' -> r = r.setDotMatchesAll()
                'm' -> r = r.setMultilineMode()
                'i' -> r = r.setCaseInsensitive()
                'x' -> r = r.setRemoveRegexWS()
                'q' -> r = r.setEscapeMetachars()
                else -> throw EvaluationException(ErrorCodes.FORX0001, "Unexpected flag '$c' in regex flags")
            }
        }
        return r
    }



    val fnMatches = BuiltinFunctionImpl.Fn("matches", listOf(
        functionType(BOOLEAN, STRING.opt, STRING, STRING),
        functionType(BOOLEAN, STRING.opt, STRING),
    )) Fn@{ args ->
        val input = args.atomicOrEmpty<XsdString>(0)?.xmlString ?: ""
        val pattern = args.atomicArgN<XsdString>(1).xmlString
        val flags = if (args.size == 3) parseFlags(args.atomicArgN<XsdString>(2).xmlString) else RegexFlags()

        val regex = try {
            XRegex(pattern, RegexVariant.XPath_2_0, flags.string)
        } catch (e: XRPatternSyntaxException) {
            throw EvaluationException(ErrorCodes.FORX0002, e)
        }
        if (regex.matches("")) throw EvaluationException(ErrorCodes.FORX0003, "Pattern '$pattern' matches the empty string")

        // TODO support flags
        val result = regex.containsMatchIn(input)
        atomic(result)
    }

    val fnReplace = BuiltinFunctionImpl.Fn("replace", listOf(
        functionType(STRING, STRING.opt, STRING, STRING, STRING),
        functionType(STRING, STRING.opt, STRING, STRING),
    )) Fn@{ args ->
        val input = args.atomicOrEmpty<XsdString>(0)?.xmlString ?: ""
        val pattern = args.atomicArgN<XsdString>(1).xmlString
        val replacement = args.atomicArgN<XsdString>(2).xmlString
        val flags = if (args.size == 3) parseFlags(args.atomicArgN<XsdString>(2).xmlString) else RegexFlags()

        val regex = try {
            XRegex(pattern, RegexVariant.XPath_2_0, flags.string)
        } catch (e: XRPatternSyntaxException) {
            throw EvaluationException(ErrorCodes.FORX0002, e)
        }
        if (regex.matches("")) throw EvaluationException(ErrorCodes.FORX0003, "Pattern '$pattern' matches the empty string")

        var start = 0
        val result = StringBuilder()
        var match = regex.find(input, start)
        while (match != null) {
            if (match.range.first > start) result.append(input, start, match.range.first)
            result.append(replacement)
            start = match.range.last + 1

            match = regex.find(input, start)
        }
        if (start < input.length) result.append(input, start, input.length)
        atomic(result.toString())
    }

    val fnTokenize = BuiltinFunctionImpl.Fn("tokenize", listOf(
        functionType(STRING.any, STRING.opt, STRING, STRING),
        functionType(STRING.any, STRING.opt, STRING),
        functionType(STRING.any, STRING.opt),
    )) Fn@{ args ->
        var input = args.atomicOrEmpty<XsdString>(0)?.xmlString?.takeUnless { it.isEmpty() } ?: return@Fn XdmSequence.EMPTY
        val pattern: String
        if (args.size == 1) {
            input = xmlCollapseWhitespace(input)
            pattern = " "
        } else {
            pattern = args.atomicArgN<XsdString>(1).xmlString
        }

        val flags = if (args.size == 3) parseFlags(args.atomicArgN<XsdString>(2).xmlString) else RegexFlags()

        val regex = try {
            XRegex(pattern, RegexVariant.XPath_2_0, flags.string)
        } catch (e: XRPatternSyntaxException) {
            throw EvaluationException(ErrorCodes.FORX0002, e)
        }
        if (regex.matches("")) throw EvaluationException(ErrorCodes.FORX0003, "Pattern '$pattern' matches the empty string")

        var start = 0
        var match: XMatchResult? = regex.find(input, start)
            ?: return@Fn atomic(input)

        val result = mutableListOf<XdmAtomic<XsdString>>()
        do {
            result.add(atomic(input.substring(start, match!!.range.first)))
            start = match.range.last + 1
            if (start >= input.length) break

            match = regex.find(input, start)
        } while (match != null)
        result.add(atomic(input.substring(start, input.length)))

        XdmSequence.fromList(result, STRING.any.toValueType())
    }

    @OptIn(NeedsXPath2::class)
    val fnAnalyzeString = BuiltinFunctionImpl.Fn("analyze-string", listOf(
        functionType(
            XdmNodeKindTest(
                NodeKindTest.ElementTest(QName(FN_NAMESPACE, "analyze-string-result")),
                SequenceType.OccurrenceType.SINGLE
            ),
            STRING.opt,
            STRING,
        ),
        functionType(
            XdmNodeKindTest(
                NodeKindTest.ElementTest(QName(FN_NAMESPACE, "analyze-string-result")),
                SequenceType.OccurrenceType.SINGLE
            ),
            STRING.opt,
            STRING,
            STRING,
        ),
    )) FN@{ args ->
        val input = args.atomicArgOrEmpty<XsdString>(0)?.xmlString ?: ""
        val pattern = args.atomicArgN<XsdString>(1).xmlString
        val flags = if (args.size == 3) parseFlags(args.atomicArgN<XsdString>(2).xmlString) else RegexFlags()

        val regex = try {
            XRegex(pattern, RegexVariant.XPath_2_0, flags.string)
        } catch (e: XRPatternSyntaxException) {
            throw EvaluationException(ErrorCodes.FORX0002, e)
        }
        if (regex.matches("")) throw EvaluationException(ErrorCodes.FORX0003, "Pattern '$pattern' matches the empty string")

        val ctx = contextOf<ExprEvalContext>()
        val doc = ctx.outputDocument
        val outer = doc.createElementNS(FN_NAMESPACE, "analyze-string-result")

        var lastIdx = 0
        for (match in regex.findAll(input)) {
            if (lastIdx<match.range.first) {
                outer.appendChild(doc.createElementNS(FN_NAMESPACE, "non-match").apply {
                    appendChild(doc.createTextNode(input.substring(lastIdx, match.range.first)))
                })
            }

            outer.appendChild(doc.createElementNS(FN_NAMESPACE, "match").also { matchElem ->
                val groups = match.groups.asSequence().withIndex()
                    .mapNotNull { (index, group) -> group?.let { IndexedValue(index, it) } }
                    .filter { it.index > 0 }
                for ((groupIdx, group) in groups) {
                    if (lastIdx < group.range.first)
                        matchElem.appendChild(doc.createTextNode(input.substring(lastIdx, group.range.first)))
                    matchElem.appendChild(doc.createElementNS(FN_NAMESPACE, "group").also { groupElem ->
                        groupElem.setAttribute("nr", "$groupIdx")
                        groupElem.appendChild(doc.createTextNode(group.value))
                    })
                    lastIdx = group.range.last + 1
                }

                if (lastIdx < match.range.last) {
                    matchElem.appendChild(doc.createTextNode(input.substring(lastIdx, match.range.last)))
                }
            })
            lastIdx = match.range.last + 1
        }
        if (lastIdx < input.length) outer.appendChild(doc.createTextNode(input.substring(lastIdx)))
        outer
    }

    //endregion
}

@JvmInline
value class RegexFlags(val value: Int) {
    constructor(): this(0)
    val isDotMatchesAll: Boolean get() = value and REGEX_FLAG_DOTMATCHESALL != 0
    val isMultilineMode: Boolean get() = value and REGEX_FLAG_MULTILINEMODE != 0
    val isCaseInsensitive: Boolean get() = value and REGEX_FLAG_CASEINSENSITIVE != 0
    val isRemoveRegexWS: Boolean get() = value and REGEX_FLAG_REMOVEREGEXWS != 0
    val isEscapeMetachars: Boolean get() = value and REGEX_FLAG_ESCAPEMETACHARS != 0

    fun setDotMatchesAll(): RegexFlags = RegexFlags(value or REGEX_FLAG_DOTMATCHESALL)
    fun setMultilineMode(): RegexFlags = RegexFlags(value or REGEX_FLAG_MULTILINEMODE)
    fun setCaseInsensitive(): RegexFlags = RegexFlags(value or REGEX_FLAG_CASEINSENSITIVE)
    fun setRemoveRegexWS(): RegexFlags = RegexFlags(value or REGEX_FLAG_REMOVEREGEXWS)
    fun setEscapeMetachars(): RegexFlags = RegexFlags(value or REGEX_FLAG_ESCAPEMETACHARS)

    val string get(): String = buildString {
        if (isDotMatchesAll) append("s")
        if (isMultilineMode) append("m")
        if (isCaseInsensitive) append("i")
        if (isRemoveRegexWS) append("x")
        if (isEscapeMetachars) append("q")
    }


    companion object {

        const val REGEX_FLAG_DOTMATCHESALL = 1
        const val REGEX_FLAG_MULTILINEMODE = 2
        const val REGEX_FLAG_CASEINSENSITIVE = 4
        const val REGEX_FLAG_REMOVEREGEXWS = 8
        const val REGEX_FLAG_ESCAPEMETACHARS = 16

    }
}
