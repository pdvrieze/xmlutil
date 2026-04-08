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

import io.github.pdvrieze.xml.schematypes.values.XsdInt
import io.github.pdvrieze.xml.schematypes.values.XsdInteger
import io.github.pdvrieze.xml.schematypes.values.XsdLanguage
import io.github.pdvrieze.xml.schematypes.values.XsdUnsignedInt
import nl.adaptivity.xmlutil.core.internal.appendCodepoint
import nl.adaptivity.xmlutil.core.internal.codepointAt

class IntegerFormatter private constructor(private val format: FormatterImpl, private val modifier: Modifier?) {

    private constructor(r: Pair<FormatterImpl, Modifier?>): this(r.first, r.second)

    constructor(picture: String, language: XsdLanguage) : this(parsePicture(picture, language))

    fun format(value: Int): String = format.format(XsdInt(value), modifier)

    fun format(value: XsdInteger): String = format.format(value, modifier)

    fun formatTo(receiver: Appendable, value: Int): Unit =
        format.formatTo(receiver, XsdInt(value), modifier)

    fun formatTo(receiver: Appendable, value: XsdInteger): Unit =
        format.formatTo(receiver, value, modifier)

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

    val digitFamily: Int get() = (format as? DecimalDigitPatternFormatter)?.digitFamily ?: '0'.code

    override fun toString(): String {
        return format.toString() + (modifier?.let { ";$it" } ?: "")
    }

    companion object {

        fun Int.toDigitFamily(): Int? = when (this) {
            in '0'.code.. '9'.code -> '0'.code
            in 0x660..0x669 -> 0x0660 // Arabic-Indic
            in 0x104a0 .. 0x104a9 -> 0x104a0
            else -> null
        }

        private fun parseModifier(modifier: String): Modifier? {
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

            var seenDigit: Int = -1

            while (i < primary.length) {
                val c = primary[i]
                if (c.isHighSurrogate()) check(i + 1 < primary.length) {
                    "High surrogate must be followed by low surrogate"
                }
                val cp = primary.codepointAt(i)
                val cpDigitFamily = cp.toDigitFamily()
                when (cp) {
                    '#'.code -> {
                        require(seenDigit < 0) { "Picture must optional digits must precede mandatory digits" }
                        var j = i + 1
                        while (j < primary.length && primary[j] == '#') j++
                        result.add(OptDigits(j - i))
                        i = j
                    }

                    else if cpDigitFamily != null -> {
                        if (seenDigit < 0) seenDigit = cpDigitFamily
                        else if (cpDigitFamily != seenDigit) throw IllegalArgumentException("Digits of different families in picture")

                        var count = 1 // manual counting needed to deal with surrogates
                        var j = primary.nextCharPos(i)
                        while (j < primary.length && primary[j].isDigit()) {
                            count += 1
                            if (primary.codepointAt(j)
                                    .toDigitFamily() != seenDigit
                            ) throw IllegalArgumentException("Digits of different families in picture")
                            j = primary.nextCharPos(j)
                        }
                        result.add(ReqDigits(count))
                        i = j
                    }

                    else if !c.isLetter() -> {
                        // early return to handle format-integer-38 if following groups
                        val prev = result.lastOrNull()// ?: return SimpleFormatter to modifier
                        //
                        require(prev !is GroupingSeparator) { "Grouping separators must not follow each other" }
                        result.add(GroupingSeparator(cp))
                        i = primary.nextCharPos(i)
                    }

                    else if (i == 0) -> when (c) {

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
            if (seenDigit < 0) return SimpleFormatter to modifier

            if (seenDigit == '0'.code) { // this case can be optimized to a simple formatter
                result.singleOrNull()?.let {
                    if (it is ReqDigits && it.length == 1) return SimpleFormatter to modifier
                }
            }

            return DecimalDigitPatternFormatter(result, seenDigit) to modifier
        }
    }

    private abstract class FormatterImpl {
        open val minDigits: Int get() = 1
        open val optionalDigitCount: Int get() = -1

        open fun format(int: XsdInteger, modifier: Modifier?): String = buildString { formatTo(this, int, modifier) }
        abstract fun formatTo(receiver: Appendable, int: XsdInteger, modifier: Modifier?)
    }

    private object CapitalLetterFormatter : FormatterImpl() {
        val divider = XsdUnsignedInt(26u)

        private fun recurseTo(int: XsdInteger, appendable: Appendable) {
            if (int.sign > 0) {
                val divRem = int.divRem(divider)
                recurseTo(divRem.quotient, appendable)
                appendable.append(('A'.code - 1 + divRem.remainder.toInt()).toChar())
            }
        }

        override fun formatTo(receiver: Appendable, int: XsdInteger, modifier: Modifier?) {
            require(int.sign != 0) { "Value must be positive" }
            if (int.sign < 0) {
                receiver.append('-')
                recurseTo(int.abs(), receiver)
            } else {
                recurseTo(int, receiver)
            }
        }

        override fun toString(): String = "A"
    }

    private object LowerLetterFormatter : FormatterImpl() {
        override fun format(int: XsdInteger, modifier: Modifier?): String {
            return CapitalLetterFormatter.format(int, modifier).lowercase()
        }

        override fun formatTo(receiver: Appendable, int: XsdInteger, modifier: Modifier?) {
            val b = StringBuilder()
            CapitalLetterFormatter.formatTo(b, int, modifier)
            receiver.append(b.toString().lowercase())
        }

        override fun toString(): String = "a"
    }

    private abstract class RomanFormatter(val capital: Boolean) : FormatterImpl() {
        val values = intArrayOf(1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1)
        val capitalSymbols = arrayOf("M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I")
        val lowerSymbols = arrayOf("m", "cm", "d", "cd", "c", "xc", "l", "xl", "x", "ix", "v", "iv", "i")

        final override fun formatTo(receiver: Appendable, int: XsdInteger, modifier: Modifier?) {
            val symbols = if (capital) capitalSymbols else lowerSymbols

            if (int.sign < 0 || int > XsdInt(3999)) throw IllegalArgumentException("Value must be between -100000 and 100000")

            var n = int.toInt()
            for (i in values.indices) {
                while (n >= values[i]) {
                    n -= values[i]
                    receiver.append(symbols[i])
                }
            }
        }

        override fun toString(): String = if (capital) "I" else "i"
    }

    private object CapitalLetterRomanFormatter : RomanFormatter(true)
    private object LowerLetterRomanFormatter : RomanFormatter(false)

    private class LowerWordFormatter : TitleCaseWordFormatter() {
        override fun format(int: XsdInteger, modifier: Modifier?): String {
            return super.format(int, modifier).lowercase()
        }

        override fun formatTo(receiver: Appendable, int: XsdInteger, modifier: Modifier?) {
            receiver.append(buildString { super.formatTo(this, int, modifier) }.lowercase())
        }

        override fun toString(): String = "w"
    }

    private open class TitleCaseWordFormatter() : FormatterImpl() {

        override fun formatTo(receiver: Appendable, int: XsdInteger, modifier: Modifier?) {
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
        override fun format(int: XsdInteger, modifier: Modifier?): String {
            return super.format(int, modifier).uppercase()
        }

        override fun formatTo(receiver: Appendable, int: XsdInteger, modifier: Modifier?) {
            receiver.append(buildString { super.formatTo(this, int, modifier) }.uppercase())
        }

        override fun toString(): String = "W"
    }

    private object SimpleFormatter: FormatterImpl() {
        override fun formatTo(receiver: Appendable, int: XsdInteger, modifier: Modifier?) {
            receiver.append(int.xmlString)
        }

        override fun toString(): String = "1"
    }

    private class DecimalDigitPatternFormatter(val pattern: List<IntFormatElem>, val digitFamily: Int) : FormatterImpl() {

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
            var groupMarker: Int = -1
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
                    val len = if (digitFamily >= 0x10000) patternElem.length shl 2 else patternElem.length
                    if (stringPos > len) formatHelper(digitSource, stringPos-len, patternPos-1, appendable)
                    appendable.appendRange(digitSource, (stringPos - len).coerceAtLeast(0), stringPos)
                }

                is GroupingSeparator -> {
                    formatHelper(digitSource, stringPos, patternPos - 1, appendable)
                    appendable.appendCodepoint(patternElem.cp)
                }
            }
        }

        override fun formatTo(receiver: Appendable, int: XsdInteger, modifier: Modifier?) {
            val str = int.xmlString
            val startPos: Int
            val extraDigits: Int
            if(str[0] == '-') {
                startPos = 1
                extraDigits = minDigits - str.length + 1
            } else {
                startPos = 0
                extraDigits = minDigits - str.length
            }
            val base = when {
                extraDigits<=0 && digitFamily == '0'.code -> str
                else -> StringBuilder().apply {
                    if (startPos > 0) receiver.append(str[0])
                    repeat(extraDigits) {
                        appendCodepoint(digitFamily)
                    }
                    when (digitFamily) {
                        '0'.code -> appendRange(str, startPos, str.length)
                        else -> for (i in startPos until str.length) {
                            appendCodepoint((str[i].code - '0'.code + digitFamily))
                        }
                    }
                }
            }

            if (regularGroupingLength > 0) {
                val utf16GroupLength = if (digitFamily >=0x10000) regularGroupingLength shl 1 else regularGroupingLength
                val groupMarkerCp = pattern.asSequence().filterIsInstance<GroupingSeparator>().first().cp
                val offset = ((base.length -1) % utf16GroupLength) + 1
                receiver.appendRange(base, 0, offset)
                for (s in offset until base.length step utf16GroupLength) {
                    receiver.appendCodepoint(groupMarkerCp)
                    receiver.appendRange(base, s, s+utf16GroupLength)
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

    private class GroupingSeparator(val cp: Int) : IntFormatElem() {
        override val length: Int get() = 1
        override fun toString(): String = buildString {
            append('\'').appendCodepoint(cp).append('\'')
        }
    }

    private sealed class Modifier(val variant: String?, val isAlphabetic: Boolean)

    private class CardinalModifier(variant: String? = null, isAlphabetic: Boolean = true) : Modifier(variant, isAlphabetic) {
        override fun toString(): String {
            return "c${variant ?: ""}${if (isAlphabetic) "a" else "t"}"
        }
    }
    private class OrdinalModifier(variant: String? = null, isAlphabetic: Boolean = true) : Modifier(variant, isAlphabetic) {
        override fun toString(): String {
            return "o${variant ?: ""}${if (isAlphabetic) "a" else "t"}"
        }
    }

}

internal fun CharSequence.nextCharPos(pos: Int): Int = when {
    get(pos).isHighSurrogate() -> pos + 2
    else -> pos + 1
}
