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

package io.github.pdvrieze.xml.schematypes.values.formatters

import io.github.pdvrieze.xml.schematypes.values.*
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.core.internal.appendCodepoint
import nl.adaptivity.xmlutil.core.internal.codepointAt
import nl.adaptivity.xmlutil.core.internal.nextCodePointPos

@ExperimentalXmlUtilApi
class IntegerFormatter private constructor(internal val format: FormatterImpl, private val modifier: Modifier?) {

    private constructor(r: Pair<FormatterImpl, Modifier?>): this(r.first, r.second)

    constructor(picture: String, language: XsdLanguage) : this(parsePicture(picture, language))

    fun format(value: Int, widthModifier: WidthModifier = WidthModifier()): String =
        format.format(XsdInt(value), modifier, widthModifier)

    fun format(value: XsdInteger, widthModifier: WidthModifier = WidthModifier()): String =
        format.format(value, modifier, widthModifier)

    fun formatTo(receiver: Appendable, value: Int, widthModifier: WidthModifier = WidthModifier()) {
        format.formatTo(receiver, XsdInt(value), modifier, widthModifier)
    }

    fun formatTo(receiver: Appendable, value: XsdInteger, widthModifier: WidthModifier = WidthModifier()) {
        format.formatTo(receiver, value, modifier, widthModifier)
    }

    /** Get the minimum width of this format in digits. */
    val minDigits: Int get() = format.minDigits
    /** Get the amount of specified optional digits. If not valid has the value `-1` */
    val optionalDigitCount: Int get() = format.optionalDigitCount
    val totalDigitCount: Int get() {
        var r = minDigits
        val o = format.optionalDigitCount
        if (o >= 0) r += o
        return r
    }

    val digitFamily: UnicodeChar get() = (format as? DecimalDigitPatternFormatter)?.digitFamily ?: UnicodeChar('0')

    override fun toString(): String {
        return format.toString() + (modifier?.let { ";$it" } ?: "")
    }

    companion object {

        internal fun parseModifier(modifier: String): Modifier? {
            if (modifier.isEmpty()) return null
            val isCardinal = when (modifier[0]) {
                'c' -> true
                'o' -> false
                else -> throw IllegalArgumentException("Invalid modifier (invalid mode) '$modifier'")
            }
            if (modifier.length == 1) return if (isCardinal) CardinalModifier() else OrdinalModifier()
            var variant: String? = null
            var next = 1
            if (modifier[1] == '(') {
                val end = modifier.lastIndexOf(')')
                if (end < 0) throw IllegalArgumentException("Invalid modifier (missing closing paren) '$modifier'")
                variant = modifier.substring(2, end)
                next = end + 1
            }
            val isAlphabetic: Boolean
            if (next <modifier.length) {
                isAlphabetic = when (modifier[next]) {
                    'a' -> true
                    't' -> false
                    else -> throw IllegalArgumentException("Invalid modifier (invalid numbering) '$modifier'")
                }
                if (next+1 < modifier.length) throw IllegalArgumentException("Invalid modifier (trailing content) '$modifier'")
            } else isAlphabetic = true

            return if (isCardinal) CardinalModifier(variant, isAlphabetic) else OrdinalModifier(variant, isAlphabetic)
        }

        private fun parsePicture(picture: String, language: XsdLanguage): Pair<FormatterImpl, Modifier?> {
            val semiIdx = picture.lastIndexOf(';')
            val primary: String
            val modifier: Modifier?
            when {
                semiIdx < 0 -> { primary = picture; modifier = null }
                else -> {
                    primary = picture.substring(0, semiIdx)
                    val modString = picture.substring(semiIdx + 1)
                    modifier = if (modString.isEmpty()) null else parseModifier(modString)
                }
            }

            val result = mutableListOf<IntFormatElem>()
            var i = 0

            var seenDigit: UnicodeChar = UnicodeChar.INVALID

            while (i < primary.length) {
                val char = primary[i]
                if (char.isHighSurrogate()) check(i + 1 < primary.length) {
                    "High surrogate must be followed by low surrogate"
                }
                val cp = primary.unicodeChar(i)
                val cpDigitFamily = cp.zeroDigitOrNull
                when (cp.codePoint) {
                    '#'.code -> {
                        require(!seenDigit.isValid) { "Picture must optional digits must precede mandatory digits" }
                        var j = i + 1
                        while (j < primary.length && primary[j] == '#') j++
                        result.add(OptDigits(j - i))
                        i = j
                    }

                    else if cpDigitFamily != null -> {
                        if (! seenDigit.isValid) seenDigit = cpDigitFamily
                        else if (cpDigitFamily != seenDigit) throw IllegalArgumentException("Digits of different families in picture")

                        var count = 1 // manual counting needed to deal with surrogates
                        var j = primary.nextCodePointPos(i)
                        while (j < primary.length && primary[j].isDigit()) {
                            count += 1
                            if (UnicodeChar(primary.codepointAt(j))
                                    .zeroDigitOrNull != seenDigit
                            ) throw IllegalArgumentException("Digits of different families in picture")
                            j = primary.nextCodePointPos(j)
                        }
                        result.add(ReqDigits(count))
                        i = j
                    }

                    else if !cp.isLetter -> {
                        // early return to handle format-integer-38 if following groups
                        val prev = result.lastOrNull()// ?: return SimpleFormatter to modifier
                        //
                        require(prev !is GroupingSeparator) { "Grouping separators must not follow each other ($prev, $cp)" }
                        result.add(GroupingSeparator(cp))
                        i = primary.nextCodePointPos(i)
                    }

                    else if (i == 0) -> when (cp[0]) {

                        'A' -> {
                            require(primary.length == 1)
                            return CapitalLetterFormatter to modifier
                        }

                        'a' -> {
                            require(primary.length == 1)
                            return LowerLetterFormatter to modifier
                        }

                        'I' -> {
                            require(primary.length == 1)
                            return CapitalLetterRomanFormatter to modifier
                        }

                        'i' -> {
                            require(primary.length == 1)
                            return LowerLetterRomanFormatter to modifier
                        }

                        'w' -> {
                            require(primary.length == 1)
                            return LowerWordFormatter() to modifier
                        }

                        'W' -> when {
                            primary.length == 2 && primary[1] == 'w' -> return TitleCaseWordFormatter() to modifier
                            else -> {
                                require(primary.length == 1)
                                return UpperWordFormatter() to modifier
                            }
                        }

                        else -> return SimpleFormatter to modifier // requires fallback to "simple" formatter
                    }

                    'c'.code, 'o'.code, '('.code -> {
                        if (semiIdx < 0) throw IllegalArgumentException("Modifier without ; separator")
                        else return SimpleFormatter to modifier
                    }

                    else -> return SimpleFormatter to modifier // requires fallback to "simple" formatter
                }
            }
            require(result.isNotEmpty()) { "Picture must not be empty" }
            if (result.none { it is ReqDigits }) return SimpleFormatter to modifier

            require(result.first() !is GroupingSeparator) { "Picture must not start with grouping separator" }
            require(result.last() !is GroupingSeparator) { "Picture must not end with grouping separator" }

            // This is a "recoverable" error by https://www.w3.org/Bugs/Public/show_bug.cgi?id=19004
            if (! seenDigit.isValid) return SimpleFormatter to modifier

            if (seenDigit[0] == '0') { // this case can be optimized to a simple formatter
                result.singleOrNull()?.let {
                    if (it is ReqDigits && it.length == 1) return SimpleFormatter to modifier
                }
            }

            return DecimalDigitPatternFormatter(result, seenDigit) to modifier
        }
    }

    internal abstract class FormatterImpl {
        open val minDigits: Int get() = 1
        open val optionalDigitCount: Int get() = -1

        open fun format(int: XsdInteger, modifier: Modifier?, widthModifier: WidthModifier): String = buildString { formatTo(
            this,
            int,
            modifier,
            widthModifier
        ) }
        abstract fun formatTo(receiver: Appendable, int: XsdInteger, modifier: Modifier?, widthModifier: WidthModifier)
    }

    private abstract class LetterFormatter(val firstLetterCp: Int, range: UInt = 26u) : FormatterImpl() {
        val divider = XsdInt(range.toInt())
        val adj: XsdInt = XsdInt(-1/* - divider.intValue*/)

        private fun recurseTo(int: XsdInteger, appendable: Appendable, minDigits: Int, maxDigits: Int) {
            val divRem = int.divRem(divider)
            if (divRem.quotient.sign > 0) {
                recurseTo(divRem.quotient - XsdInt.ONE, appendable, minDigits, maxDigits)
            }

            val remainder = divRem.remainder.toInt().mod(divider.intValue)

            appendable.appendCodepoint(firstLetterCp + remainder)
        }

        override fun formatTo(receiver: Appendable, int: XsdInteger, modifier: Modifier?, widthModifier: WidthModifier) {
            if (int.sign == 0) {
                receiver.append('0')
                return
            }
            val realMin = if (widthModifier.isSpecified) widthModifier.minWidth else minDigits
            // note that recursion works on one step to zero as 'A' is 1, not 0 and there is no zero digit

            if (int.sign < 0) {
                receiver.append('-')
                recurseTo(int.abs() - XsdInt.ONE, receiver, realMin, widthModifier.maxWidth)
            } else {
                recurseTo(int - XsdInt.ONE, receiver, realMin, widthModifier.maxWidth)
            }
        }

    }

    private object CapitalLetterFormatter : LetterFormatter('A'.code) {
        override fun toString(): String = "A"
    }

    private object LowerLetterFormatter : LetterFormatter('a'.code) {
        override fun toString(): String = "a"
    }

    internal abstract class RomanFormatter(val capital: Boolean) : FormatterImpl() {
        val values = intArrayOf(1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1)
        val capitalSymbols = arrayOf("M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I")
        val lowerSymbols = arrayOf("m", "cm", "d", "cd", "c", "xc", "l", "xl", "x", "ix", "v", "iv", "i")

        final override fun formatTo(
            receiver: Appendable,
            int: XsdInteger,
            modifier: Modifier?,
            widthModifier: WidthModifier
        ) {
            val symbols = if (capital) capitalSymbols else lowerSymbols

            if (int.sign < 0 || int > XsdInt(3999)) throw IllegalArgumentException("Value must be between -100000 and 100000")
            var n = int.toInt()
            when (widthModifier.maxWidth) {
                1 -> n = n % 10
                2 -> n = n % 100
                3 -> n = n % 1000
                // other cases don't require clipping
            }
            val realMin = if (widthModifier.isSpecified) widthModifier.minWidth else minDigits

            var totalChars = 0
            for (i in values.indices) {
                while (n >= values[i]) {
                    n -= values[i]
                    val symbol = symbols[i]
                    totalChars += symbol.length
                    receiver.append(symbol)
                }
            }

            // append spaces to minimum length
            if (realMin > totalChars) {
                repeat(realMin - totalChars) { receiver.append(' ') }
            }
        }

        override fun toString(): String = if (capital) "I" else "i"
    }

    private object CapitalLetterRomanFormatter : RomanFormatter(true)
    private object LowerLetterRomanFormatter : RomanFormatter(false)

    private class LowerWordFormatter : TitleCaseWordFormatter() {
        override fun format(int: XsdInteger, modifier: Modifier?, widthModifier: WidthModifier): String {
            return super.format(int, modifier, widthModifier).lowercase()
        }

        override fun formatTo(receiver: Appendable, int: XsdInteger, modifier: Modifier?, widthModifier: WidthModifier) {
            receiver.append(buildString { super.formatTo(this, int, modifier, widthModifier) }.lowercase())
        }

        override fun toString(): String = "w"
    }

    private open class TitleCaseWordFormatter() : FormatterImpl() {

        override fun formatTo(receiver: Appendable, int: XsdInteger, modifier: Modifier?, widthModifier: WidthModifier) {
            if(int.sign < 0 || int >= XsdInt(EnglishValues.size)) throw UnsupportedOperationException("Only values between 0 and ${EnglishValues.size-1} are supported for title case word formatter")

            receiver.append(EnglishValues[int.toInt()])
        }

        override fun toString(): String = "Ww"

        companion object {
            private val EnglishValues = arrayOf(
                "Zero", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
                "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen", "Twenty",
            )
        }
    }
    private class UpperWordFormatter : TitleCaseWordFormatter() {
        override fun format(int: XsdInteger, modifier: Modifier?, widthModifier: WidthModifier): String {
            return super.format(int, modifier, widthModifier).uppercase()
        }

        override fun formatTo(receiver: Appendable, int: XsdInteger, modifier: Modifier?, widthModifier: WidthModifier) {
            receiver.append(buildString { super.formatTo(this, int, modifier, widthModifier) }.uppercase())
        }

        override fun toString(): String = "W"
    }

    private object SimpleFormatter: FormatterImpl() {
        override fun formatTo(receiver: Appendable, int: XsdInteger, modifier: Modifier?, widthModifier: WidthModifier) {
            val baseString = int.xmlString

            val startPos = when (baseString.getOrElse(0, { '0' })) {
                '-' -> 1
                else -> 0
            }
            when {
                (baseString.length-startPos) < widthModifier.minWidth -> {
                    receiver.appendRange(baseString, 0, startPos)
                    repeat(widthModifier.minWidth - baseString.length - startPos) { receiver.append('0') }
                    receiver.appendRange(baseString, startPos, baseString.length)
                }

                (baseString.length - startPos) > widthModifier.maxWidth -> {
                    receiver.appendRange(baseString, 0, startPos)
                    receiver.appendRange(baseString, baseString.length - widthModifier.maxWidth, baseString.length)

                }

                else -> receiver.append(baseString)
            }
        }

        override fun toString(): String = "1"
    }

    private class DecimalDigitPatternFormatter(val pattern: List<IntFormatElem>, val digitFamily: UnicodeChar) : FormatterImpl() {

        override val minDigits: Int
        override val optionalDigitCount: Int

        init {
            var d = 0
            var o = 0
            for (p in pattern) {
                when (p) {
                    is ReqDigits -> d += p.length
                    is OptDigits -> o += p.length
                }
            }
            minDigits = d
            optionalDigitCount = o
        }

        val regularGroupingLength: Int

        init {
            var seenLenBeforeGroup = -1
            var i = pattern.lastIndex
            var lenBeforeGroup = 0
            var groupMarker: UnicodeChar = UnicodeChar.INVALID
            inner@do {
                when (val elem = pattern[i]) {
                    is ReqDigits,
                    is OptDigits -> {
                        lenBeforeGroup += elem.length
                        if (seenLenBeforeGroup >= 0 && lenBeforeGroup > seenLenBeforeGroup) {
                            seenLenBeforeGroup = -1
                            break
                        }
                    }

                    is GroupingSeparator if seenLenBeforeGroup < 0 -> {
                        seenLenBeforeGroup = lenBeforeGroup
                        groupMarker = elem.cp
                        lenBeforeGroup = 0
                    }

                    is GroupingSeparator -> {
                        if (lenBeforeGroup != seenLenBeforeGroup || groupMarker != elem.cp) {
                            seenLenBeforeGroup = -1 // not regular
                            break
                        }
                        lenBeforeGroup = 0
                    }
                }
                i--
            } while (i>=0) // if there are more digits, but not separator the len is counted so irregular

            regularGroupingLength = seenLenBeforeGroup
        }

        private fun formatHelper(digitSource: CharSequence, stringPos: Int, patternPos: Int, appendable: Appendable) {
            if (patternPos < 0) {
                if (stringPos > 0) appendable.append(digitSource, 0, stringPos)
                return
            }
            when (val patternElem = pattern[patternPos]) {
                is ReqDigits,
                is OptDigits -> { // can be handled together as we already have the needed length
                    val len = digitFamily.length
                    if (stringPos > len) formatHelper(digitSource, stringPos - len, patternPos - 1, appendable)
                    appendable.appendRange(digitSource, (stringPos - len).coerceAtLeast(0), stringPos)
                }

                is GroupingSeparator -> {
                    formatHelper(digitSource, stringPos, patternPos - 1, appendable)
                    appendable.appendUnicode(patternElem.cp)
                }
            }
        }

        override fun formatTo(receiver: Appendable, int: XsdInteger, modifier: Modifier?, widthModifier: WidthModifier) {
            val str = int.xmlString
            val signEnd: Int
            val extraDigits: Int
            val realMin = if (widthModifier.isSpecified) widthModifier.minWidth else minDigits
            if (str[0] == '-') {
                signEnd = 1
                extraDigits = realMin - str.length + 1
            } else {
                signEnd = 0
                extraDigits = realMin - str.length
            }
            val startPos = signEnd + maxOf(0, str.length - widthModifier.maxWidth)

            val base = when {
                extraDigits <= 0 && startPos == signEnd && digitFamily[0] == '0' -> str
                else -> StringBuilder().apply {
                    if (signEnd > 0) receiver.append(str[0])
                    repeat(extraDigits) {
                        appendUnicode(digitFamily)
                    }
                    when (digitFamily[0]) {
                        '0' -> appendRange(str, startPos, str.length)
                        else -> for (i in startPos until str.length) {
                            appendUnicode(UnicodeChar((str[i].code - '0'.code + digitFamily.codePoint)))
                        }
                    }
                }
            }

            if (regularGroupingLength > 0) {
                val utf16GroupLength =
                    if (digitFamily.isSingleChar) regularGroupingLength else regularGroupingLength shl 1
                val groupMarkerCp = pattern.asSequence().filterIsInstance<GroupingSeparator>().first().cp
                val offset = ((base.length - 1) % utf16GroupLength) + 1
                receiver.appendRange(base, 0, offset)
                for (s in offset until base.length step utf16GroupLength) {
                    receiver.appendUnicode(groupMarkerCp)
                    receiver.appendRange(base, s, s + utf16GroupLength)
                }
            } else {
                formatHelper(base, base.length, pattern.lastIndex, receiver)
            }
        }

        override fun toString(): String {
            return pattern.joinToString(separator = "") { it.toString() }
        }
    }

    private abstract class IntFormatElem {
        abstract val length: Int
    }

    private class OptDigits(override val length: Int) : IntFormatElem() {
        override fun toString(): String = "#".repeat(length)
    }

    private class ReqDigits(override val length: Int) : IntFormatElem() {
        override fun toString(): String = "0".repeat(length)
    }

    private class GroupingSeparator(val cp: UnicodeChar) : IntFormatElem() {
        init {
            require(cp.isValid) { "Grouping separator must be a valid codepoint" }
        }

        override val length: Int get() = 1
        override fun toString(): String = buildString {
            append('\'').appendCodepoint(cp.codePoint).append('\'')
        }
    }

    internal sealed class Modifier(val variant: String?, val isAlphabetic: Boolean)

    internal class CardinalModifier(variant: String? = null, isAlphabetic: Boolean = true) :
        Modifier(variant, isAlphabetic) {

        override fun toString(): String {
            return "c${variant ?: ""}${if (isAlphabetic) "a" else "t"}"
        }
    }
    internal class OrdinalModifier(variant: String? = null, isAlphabetic: Boolean = true) : Modifier(variant, isAlphabetic) {
        override fun toString(): String {
            return "o${variant ?: ""}${if (isAlphabetic) "a" else "t"}"
        }
    }

}
