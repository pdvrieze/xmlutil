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

class IntegerFormatter private constructor(private val format: FormatterImpl, modifier: Modifier?) {

    private constructor(r: Pair<FormatterImpl, Modifier?>): this(r.first, r.second)

    constructor(picture: String, language: XsdLanguage) : this(parsePicture(picture, language))

    fun format(value: Int): String = format.format(XsdInt(value))

    fun format(value: XsdInteger): String = format.format(value)

    fun formatTo(value: Int, receiver: Appendable): Unit = format.formatTo(XsdInt(value), receiver)

    fun formatTo(value: XsdInteger, receiver: Appendable): Unit = format.formatTo(value, receiver)

    companion object {


        fun Char.toDigitFamily(): Char = when (this) {
            in '0'.. '9' -> '0'
            in '\u0660'.. '\u0669' -> '\u0660' // Arabic-Indic
            else -> this
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

            var seenDigit: Char = '\u0000'

            while (i < primary.length) {
                val cp = primary
                when (val c = primary[i]) {
                    '#' -> {
                        require(seenDigit == '\u0000') { "Picture must optional digits must precede mandatory digits" }
                        var j = i + 1
                        while (j < primary.length && primary[j] == '#') j++
                        result.add(OptDigits(j - i))
                        i = j
                    }

                    else if c.isDigit() -> {
                        val newFamily = c.toDigitFamily()
                        if (seenDigit == '\u0000') seenDigit = newFamily
                        else if (newFamily != seenDigit) throw IllegalArgumentException("Digits of different families in picture")

                        var j = i+1
                        while (j < primary.length && primary[j].isDigit()) {
                            if (primary[j].toDigitFamily() != seenDigit) throw IllegalArgumentException("Digits of different families in picture")
                            j++
                        }
                        result.add(ReqDigits(j-i))
                        i = j
                    }

                    else if ! c.isLetter() -> {
                        // early return to handle format-integer-38 if following groups
                        val prev = result.lastOrNull()// ?: return SimpleFormatter to modifier
                        //
                        require( prev !is GroupingSeparator) { "Grouping separators must not follow each other" }
                        if (c.isHighSurrogate()) {
                            result.add(GroupingSeparator(primary.substring(i, i+2)))
                            i+=2
                        } else {
                            result.add(GroupingSeparator(primary.substring(i, i+1)))
                            i += 1
                        }
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
                            primary.length == 2 && primary[1]=='w' -> return TitleCaseWordFormatter() to modifier
                            else -> {
                                require(primary.length == 1)
                                return UpperWordFormatter() to modifier
                            }
                        }

                        else -> return SimpleFormatter to modifier // requires fallback to "simple" formatter
                    }


                    else -> return SimpleFormatter to modifier // requires fallback to "simple" formatter
                }
            }
            require(result.isNotEmpty()) { "Picture must not be empty" }
            if (result.none { it is ReqDigits }) return SimpleFormatter to modifier

            require(result.first() !is GroupingSeparator) { "Picture must not start with grouping separator" }
            require(result.last() !is GroupingSeparator) { "Picture must not end with grouping separator" }

            // This is a "recoverable" error by https://www.w3.org/Bugs/Public/show_bug.cgi?id=19004
            if (seenDigit == '\u0000') return SimpleFormatter to modifier

            if (seenDigit == '0') { // this case can be optimized to a simple formatter
                result.singleOrNull()?.let {
                    if (it is ReqDigits && it.length==1) return SimpleFormatter to modifier
                }
            }

            return DecimalDigitPatternFormatter(result, seenDigit) to modifier
        }
    }

    private abstract class FormatterImpl {
        open fun format(int: XsdInteger): String = buildString { formatTo(int, this) }
        abstract fun formatTo(int: XsdInteger, receiver: Appendable)
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

        override fun formatTo(int: XsdInteger, receiver: Appendable) {
            require(int.sign != 0) { "Value must be positive" }
            if (int.sign < 0) {
                receiver.append('-')
                recurseTo(int.abs(), receiver)
            } else {
                recurseTo(int, receiver)
            }
        }

    }

    private object LowerLetterFormatter : FormatterImpl() {
        override fun format(int: XsdInteger): String {
            return CapitalLetterFormatter.format(int).lowercase()
        }

        override fun formatTo(int: XsdInteger, receiver: Appendable) {
            val b = StringBuilder()
            CapitalLetterFormatter.formatTo(int,b)
            receiver.append(b.toString().lowercase())
        }
    }

    private abstract class RomanFormatter(val capital: Boolean) : FormatterImpl() {
        val values = intArrayOf(1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1)
        val capitalSymbols = arrayOf("M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I")
        val lowerSymbols = arrayOf("m", "cm", "d", "cd", "c", "xc", "l", "xl", "x", "ix", "v", "iv", "i")

        final override fun formatTo(int: XsdInteger, receiver: Appendable) {
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
    }

    private object CapitalLetterRomanFormatter : RomanFormatter(true)
    private object LowerLetterRomanFormatter : RomanFormatter(false)

    private class LowerWordFormatter : TitleCaseWordFormatter() {
        override fun format(int: XsdInteger): String {
            return super.format(int).lowercase()
        }

        override fun formatTo(int: XsdInteger, receiver: Appendable) {
            receiver.append(buildString { super.formatTo(int, this) }.lowercase())
        }
    }

    private open class TitleCaseWordFormatter() : FormatterImpl() {

        override fun formatTo(int: XsdInteger, receiver: Appendable) {
            require(int.sign >=0 && int < XsdInt(EnglishValues.size))
            receiver.append(EnglishValues[int.toInt()])
        }

        companion object {
            private val EnglishValues = arrayOf(
                "Zero", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
                "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen", "Twenty",
            )
        }
    }
    private class UpperWordFormatter : TitleCaseWordFormatter() {
        override fun format(int: XsdInteger): String {
            return super.format(int).uppercase()
        }

        override fun formatTo(int: XsdInteger, receiver: Appendable) {
            receiver.append(buildString { super.formatTo(int, this) }.uppercase())
        }
    }

    private object SimpleFormatter: FormatterImpl() {
        override fun formatTo(int: XsdInteger, receiver: Appendable) {
            receiver.append(int.xmlString)
        }
    }

    private class DecimalDigitPatternFormatter(val pattern: List<IntFormatElem>, digit: Char) : FormatterImpl() {
        val digitFamily = when (digit) {
            in '0'.. '9' -> '0'
            in '\u0660'.. '\u0669' -> '\u0660' // Arabic-Indic
            else -> '0'
        }
        val neededDigits = pattern.asSequence().filterIsInstance<ReqDigits>().sumOf { it.length }

        val regularGroupingLength: Int

        init {
            var seenLenBeforeGroup = -1
            var i = pattern.lastIndex
            var lenBeforeGroup = 0
            var groupMarker: String = ""
            inner@do {
                when (val elem = pattern[i]) {
                    is ReqDigits,
                    is OptDigits -> lenBeforeGroup += elem.length

                    is GroupingSeparator if seenLenBeforeGroup < 0 -> {
                        seenLenBeforeGroup = lenBeforeGroup
                        groupMarker = elem.content
                        lenBeforeGroup = 0
                    }

                    is GroupingSeparator -> {
                        if (lenBeforeGroup != seenLenBeforeGroup || groupMarker != elem.content) {
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
                    val len = patternElem.length
                    if (stringPos > len) formatHelper(digitSource, stringPos-len, patternPos-1, appendable)
                    appendable.appendRange(digitSource, (stringPos - len).coerceAtLeast(0), stringPos)
                }

                is GroupingSeparator -> {
                    formatHelper(digitSource, stringPos, patternPos - 1, appendable)
                    appendable.append(patternElem.content)
                }
            }
        }

        override fun formatTo(int: XsdInteger, receiver: Appendable) {
            val str = int.xmlString
            val startPos: Int
            val extraDigits: Int
            if(str[0] == '-') {
                startPos = 1
                extraDigits = neededDigits - str.length + 1
            } else {
                startPos = 0
                extraDigits = neededDigits - str.length
            }
            val base = when {
                extraDigits<=0 && digitFamily == '0' -> str
                else -> StringBuilder().apply {
                    if (startPos > 0) receiver.append(str[0])
                    repeat(extraDigits) {
                        append(digitFamily)
                    }
                    when (digitFamily) {
                        '0' -> appendRange(str, startPos, str.length)
                        else -> for (i in startPos until str.length) {
                            append((str[i].code - '0'.code + digitFamily.code).toChar())
                        }
                    }
                }
            }

            if (regularGroupingLength > 0) {
                val group = pattern.asSequence().filterIsInstance<GroupingSeparator>().first().content
                val offset = base.length % regularGroupingLength
                receiver.appendRange(base, 0, offset)
                for (s in offset until base.length step regularGroupingLength) {
                    receiver.append(group)
                    receiver.appendRange(base, s, s+regularGroupingLength)
                }
            } else {
                formatHelper(base, base.length, pattern.lastIndex, receiver)
            }
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

    private class GroupingSeparator(val content: String) : IntFormatElem() {
        override val length: Int get() = 1
        override fun toString(): String = "'$content'"
    }

    private sealed class Modifier(val variant: String?, val isAlphabetic: Boolean)

    private class CardinalModifier(variant: String? = null, isAlphabetic: Boolean = true) : Modifier(variant, isAlphabetic)
    private class OrdinalModifier(variant: String? = null, isAlphabetic: Boolean = true) : Modifier(variant, isAlphabetic)

}
