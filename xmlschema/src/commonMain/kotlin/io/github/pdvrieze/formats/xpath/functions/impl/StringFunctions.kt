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
import io.github.pdvrieze.formats.xmlschema.resolved.SchemaVersion
import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.data.XdmAtomic
import io.github.pdvrieze.formats.xpath.eval.data.XdmAtomicOrSequence
import io.github.pdvrieze.formats.xpath.eval.data.XdmSequence
import io.github.pdvrieze.formats.xpath.eval.data.XdmString
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunctionImpl
import io.github.pdvrieze.formats.xpath.functions.argN
import io.github.pdvrieze.formats.xpath.functions.atomicArgN
import io.github.pdvrieze.formats.xpath.functions.atomicArgOrEmpty
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.values.XsdDouble
import io.github.pdvrieze.xml.schematypes.values.XsdInt
import io.github.pdvrieze.xml.schematypes.values.XsdInteger
import io.github.pdvrieze.xml.schematypes.values.XsdString
import nl.adaptivity.xmlutil.core.internal.appendCodepoint
import nl.adaptivity.xmlutil.core.internal.codepointAt
import nl.adaptivity.xmlutil.core.internal.nextCodePointPos
import nl.adaptivity.xmlutil.xmlCollapseWhitespace
import kotlin.math.roundToInt

@XPathInternal
object StringFunctions : AbstractFunctionObject() {

    //region functions to assemble and disassemble strings
    val fnCodepointsToString = BuiltinFunctionImpl("codepoints-to-string", STRING, INTEGER.any) { args ->
        val arg = args[0].asSequence().map { ((it as XdmAtomic<*>).value as XsdInteger) }

        val s = buildString {
            for (cpInt in arg) {
                when {
                    cpInt.sign == 0 -> throw NumberFormatException("0 is not a valid xml codepoint")
                    cpInt.sign < -1 -> throw NumberFormatException("negative values are not valid codepoints")
                    cpInt > XsdInt(0x10ffff) -> throw NumberFormatException("codepoint out of range")
                    else -> appendCodepoint(cpInt.toInt())
                }
            }
        }

        atomic(s)
    }

    val fnStringToCodepoints = BuiltinFunctionImpl("string-to-codepoints", INTEGER.any, STRING.opt) { args ->
        val arg = args.atomicArgOrEmpty<XsdString>(0)?.xmlString ?: return@BuiltinFunctionImpl XdmSequence.EMPTY

        val result = arg.map { atomic(it.code) }


        XdmSequence.fromList(result, INTEGER.opt.toValueType())
    }

    //endregion

    //region functions on string values 5.4
    val fnConcat: BuiltinFunctionImpl<XdmString> = BuiltinFunctionImpl("concat", flexFunctionType(STRING, ATOMIC.opt, ATOMIC.opt)) { args ->
        if (args.size < 2) throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT, "Concat requires at least two arguments")
        val concat = args.asSequence().map { Accessors.fnString(it).value.xmlString }.joinToString("")
        atomic(concat)
    }

    val fnStringJoin: BuiltinFunctionImpl<XdmString> = BuiltinFunctionImpl("string-join", contextFunctionTypes(STRING, STRING, ATOMIC.any)) { args ->
        if (args.size > 2) throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT, "String-join takes 1 or 2 arguments")
        val seq = args.argN<XdmAtomicOrSequence<XdmAtomic<*>>>(0)
        val separator = if (args.size == 2) args.atomicArgN<XsdString>(1) else ""
        val join = seq.asSequence().map { Accessors.fnString(it).value.xmlString }.joinToString(separator)
        atomic(join)
    }

    val fnSubstring = BuiltinFunctionImpl("substring", listOf(
        functionType(STRING, STRING.opt, DOUBLE, DOUBLE),
        functionType(STRING, STRING.opt, DOUBLE),
    )) { args ->
        val sourceString = args.atomicArgOrEmpty<XsdString>(0) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
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

    val fnStringLength = BuiltinFunctionImpl("string-length", contextFunctionTypes(INTEGER, STRING.opt)) { args ->
        val arg = args.toSingleAtomic<XsdString>(true) ?: return@BuiltinFunctionImpl atomic(0)

        atomic(arg.xmlString.length)
    }

    val fnNormalizeSpace = BuiltinFunctionImpl("normalize-space", contextFunctionTypes(STRING, STRING.opt)) { args ->
        val arg = args.toSingleAtomic<XsdString>(true) ?: return@BuiltinFunctionImpl atomic("")

        atomic(xmlCollapseWhitespace(arg.xmlString))
    }

    val fnNormalizeUnicode = BuiltinFunctionImpl("normalize-unicode", listOf(
        functionType(STRING, STRING.opt, STRING),
        functionType(STRING, STRING.opt),
    )) { args ->
        //XdmAtomic(XsdString(""))
        TODO("Unicode normalization not yet supported")
    }

    val fnUpperCase = BuiltinFunctionImpl("upper-case", functionType(STRING, STRING.opt)) { args ->
        val arg = args.toSingleAtomic<XsdString>() ?: return@BuiltinFunctionImpl atomic("")

        atomic(arg.xmlString.uppercase())
    }

    val fnLowerCase = BuiltinFunctionImpl("lower-case", functionType(STRING, STRING.opt)) { args ->
        val arg = args.toSingleAtomic<XsdString>() ?: return@BuiltinFunctionImpl atomic("")

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

    val fnTranslate = BuiltinFunctionImpl("translate", functionType(STRING, STRING.opt, STRING, STRING)) { args ->
        val arg = args.atomicOrEmpty<XsdString>(0)?.xmlString ?: return@BuiltinFunctionImpl atomic("")
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

    //region Functions on substring matching 5.5
    val fnSubstringBefore = BuiltinFunctionImpl("substring-before", listOf(
        functionType(STRING, STRING.opt, STRING.opt),
        functionType(STRING, STRING.opt, STRING.opt, STRING),
    )) { args ->
        val arg1 = args.atomicOrEmpty<XsdString>(0)?.xmlString ?: ""
        val arg2 = args.atomicOrEmpty<XsdString>(1)?.xmlString ?: return@BuiltinFunctionImpl atomic(arg1)
        // TODO support collation
        atomic(arg1.substringBefore(arg2, ""))
    }

    val fnSubstringAfter = BuiltinFunctionImpl("substring-after", listOf(
        functionType(STRING, STRING.opt, STRING.opt),
        functionType(STRING, STRING.opt, STRING.opt, STRING),
    )) { args ->
        val arg1 = args.atomicOrEmpty<XsdString>(0)?.xmlString ?: ""
        val arg2 = args.atomicOrEmpty<XsdString>(1)?.xmlString ?: return@BuiltinFunctionImpl atomic(arg1)
        // TODO support collation
        atomic(arg1.substringAfter(arg2, ""))
    }

    //endregion

    //region String functions using regex 5.6
    val fnMatches = BuiltinFunctionImpl("matches", listOf(
        functionType(BOOLEAN, STRING.opt, STRING, STRING),
        functionType(BOOLEAN, STRING.opt, STRING),
    )) { args ->
        val input = args.atomicOrEmpty<XsdString>(0)?.xmlString ?: ""
        val pattern = args.atomicArgN<XsdString>(1).xmlString
        val flags = if (args.size >2) args.atomicArgN<XsdString>(2).xmlString else ""

        val regex = XRegex(pattern, SchemaVersion.V1_1)
        // TODO support flags
        val result = regex.containsMatchIn(input)
        atomic(result)
    }

    val fnReplace = BuiltinFunctionImpl("replace", listOf(
        functionType(STRING, STRING.opt, STRING, STRING, STRING),
        functionType(STRING, STRING.opt, STRING, STRING),
    )) { args ->
        val input = args.atomicOrEmpty<XsdString>(0)?.xmlString ?: ""
        val pattern = args.atomicArgN<XsdString>(1).xmlString
        val replacement = args.atomicArgN<XsdString>(2).xmlString
        val flags = if (args.size == 4) args.atomicArgN<XsdString>(3).xmlString else ""

        val regex = XRegex(pattern, SchemaVersion.V1_1)
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

    val fnTokenize = BuiltinFunctionImpl("tokenize", listOf(
        functionType(STRING.any, STRING.opt, STRING, STRING),
        functionType(STRING.any, STRING.opt, STRING),
        functionType(STRING.any, STRING.opt),
    )) { args ->
        var input = args.atomicOrEmpty<XsdString>(0)?.xmlString ?: ""
        val pattern: String
        if (args.size == 1) {
            input = xmlCollapseWhitespace(input)
            pattern = " "
        } else {
            pattern = args.atomicArgN<XsdString>(1).xmlString
        }

        val flags = if (args.size == 3) args.atomicArgN<XsdString>(2).xmlString else ""

        val regex = XRegex(pattern, SchemaVersion.V1_1)
        var start = 0
        val result = mutableListOf<XdmAtomic<XsdString>>()
        var match = regex.find(input, start)
        while (match != null) {
            if (match.range.first > start) {
                result.add(atomic(input.substring(start, match.range.first)))
            }
            start = match.range.last + 1

            match = regex.find(input, start)
        }
        if (start < input.length) result.add(atomic(input.substring(start, input.length)))
        XdmSequence.fromList(result, STRING.any.toValueType())
    }

    //endregion
}
