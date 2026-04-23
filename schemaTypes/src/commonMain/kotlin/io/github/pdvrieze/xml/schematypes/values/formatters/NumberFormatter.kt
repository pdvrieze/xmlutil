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
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
import nl.adaptivity.xmlutil.core.internal.nextCodePointPos
import kotlin.math.absoluteValue
import kotlin.math.roundToLong

@ExperimentalXmlUtilApi
class NumberFormatter private constructor(internal val format: PosNegFormatter) {

    constructor(picture: String, decimalFormat: DecimalFormat) : this(parsePicture(picture, decimalFormat))

    fun format(value: Int): String =
        buildString { format.formatTo(this, XsdInt(value)) }

    fun format(value: XsdNumeric<*>): String =
        buildString { format.formatTo(this, value) }

    fun formatTo(receiver: Appendable, value: Int) {
        format.formatTo(receiver, XsdInt(value))
    }

    fun formatTo(receiver: Appendable, value: XsdNumeric<*>) {
        format.formatTo(receiver, value)
    }

    override fun toString(): String {
        return format.toString()
    }

    companion object {

        private fun parseSingle(pictureSegment: String, decimalFormat: DecimalFormat): NumberFormatter {
            var state = PARSE_STATE_PREFIX // nothing yet
            var nextState = PARSE_STATE_PREFIX
            var stateStart = 0

            val lastDigit = UnicodeChar(decimalFormat.zeroDigit.codePoint+9)

            var prefix: String? = null
            val intPattern = mutableListOf<FormatElem>()
            val decimalPattern = mutableListOf<FormatElem>()
            val expPattern = mutableListOf<FormatElem>()
            var suffix: String? = null

            var i = 0

            do {
                var incI = false
                while (i < pictureSegment.length) {
                    when (val cp = pictureSegment.unicodeChar(i)) {
                        decimalFormat.decimalSeparator -> {
                            require (state < PARSE_STATE_DECIMAL_MANDATORY) { "Decimal separator must only occur after integer part" }
                            incI = true
                            nextState = PARSE_STATE_DECIMAL_MANDATORY
                            break
                        }

                        decimalFormat.exponentSeparator -> {
                            require(state < PARSE_STATE_EXP_MANDATORY) { "Exponent separator must only once (and excludes percent)" }
                            incI = true
                            nextState = PARSE_STATE_EXP_MANDATORY
                            break
                        }

                        in decimalFormat.zeroDigit..lastDigit -> if (state != PARSE_STATE_INT_MANDATORY && state != PARSE_STATE_DECIMAL_MANDATORY) {
                            when (state) {
                                PARSE_STATE_PREFIX -> if (i > 0) prefix = pictureSegment.substring(0, i)
                                PARSE_STATE_INT_OPT -> if (i > stateStart) intPattern.add(OptDigits((i - stateStart) / decimalFormat.digit.length))
                                PARSE_STATE_DECIMAL_MANDATORY -> if (i > stateStart) decimalPattern.add(ReqDigits((i - stateStart) / decimalFormat.zeroDigit.length))
                                else -> throw IllegalArgumentException("Unexpected state $state")
                            }

                            state = PARSE_STATE_INT_MANDATORY
                            stateStart = i
                        }

                        decimalFormat.digit -> when (state) {
                            PARSE_STATE_PREFIX -> {
                                require(state == PARSE_STATE_PREFIX) { "Integer optional digits need state" }
                                if (i > 0) prefix = pictureSegment.substring(0, i)
                                state = PARSE_STATE_INT_OPT
                                stateStart = i
                            }

                            PARSE_STATE_INT_OPT,
                            PARSE_STATE_DECIMAL_OPT,
                            PARSE_STATE_EXP_OPT -> Unit

                            PARSE_STATE_INT_MANDATORY -> throw IllegalArgumentException("Integer part must only start with optional digits")
                            PARSE_STATE_DECIMAL_MANDATORY -> {
                                if (i > stateStart) decimalPattern.add(ReqDigits((i - stateStart) / decimalFormat.zeroDigit.length))
                                state = PARSE_STATE_DECIMAL_OPT
                                stateStart = i
                            }

                            PARSE_STATE_EXP_MANDATORY -> {
                                if (i > stateStart) expPattern.add(ReqDigits((i - stateStart) / decimalFormat.zeroDigit.length))
                                state = PARSE_STATE_EXP_OPT
                                stateStart = i
                            }


                            else -> throw IllegalArgumentException("Unexpected digit in state $state")
                        }

                        decimalFormat.groupingSeparator -> {
                            when (state) {
                                PARSE_STATE_INT_MANDATORY -> {
                                    intPattern.add(ReqDigits((i - stateStart) / decimalFormat.zeroDigit.length))
                                    intPattern.add(GroupingSeparator)
                                }

                                PARSE_STATE_INT_OPT -> {
                                    intPattern.add(OptDigits((i - stateStart) / decimalFormat.digit.length))
                                    intPattern.add(GroupingSeparator)
                                }

                                PARSE_STATE_DECIMAL_OPT -> {
                                    decimalPattern.add(ReqDigits((i - stateStart) / decimalFormat.zeroDigit.length))
                                    decimalPattern.add(GroupingSeparator)
                                }

                                PARSE_STATE_DECIMAL_MANDATORY -> {
                                    decimalPattern.add(OptDigits((i - stateStart) / decimalFormat.digit.length))
                                    decimalPattern.add(GroupingSeparator)
                                }

                                PARSE_STATE_EXP_OPT -> {
                                    expPattern.add(ReqDigits((i - stateStart) / decimalFormat.zeroDigit.length))
                                    expPattern.add(GroupingSeparator)
                                }

                                PARSE_STATE_EXP_MANDATORY -> {
                                    expPattern.add(OptDigits((i - stateStart) / decimalFormat.digit.length))
                                    expPattern.add(GroupingSeparator)
                                }

                                else -> throw IllegalArgumentException("Unexpected grouping separator in state $state")
                            }
                            i = pictureSegment.nextCodePointPos(i)
                            stateStart = i
                        }

                        decimalFormat.percent, decimalFormat.perMille -> {
                            when (state) {
                                PARSE_STATE_INT_MANDATORY -> intPattern.add(ReqDigits((i - stateStart) / decimalFormat.zeroDigit.length))
                                PARSE_STATE_INT_OPT -> intPattern.add(OptDigits((i - stateStart) / decimalFormat.digit.length))
                                PARSE_STATE_DECIMAL_MANDATORY -> decimalPattern.add(ReqDigits((i - stateStart) / decimalFormat.zeroDigit.length))
                                PARSE_STATE_DECIMAL_OPT -> decimalPattern.add(OptDigits((i - stateStart) / decimalFormat.digit.length))
                                else -> throw IllegalArgumentException("Unexpected grouping separator in state $state")
                            }
                            nextState = if (cp == decimalFormat.percent) PARSE_STATE_PERCENT else PARSE_STATE_PERMILLE
                            i = pictureSegment.nextCodePointPos(i)
                            break
                        }

                        else -> if (state != PARSE_STATE_PREFIX) {
                            nextState = PARSE_STATE_SUFFIX
                            break
                        }

                    }



                    i = pictureSegment.nextCodePointPos(i)
                }

                when (state) {
                    PARSE_STATE_PREFIX -> if (i > 0) prefix = pictureSegment.substring(0, i)
                    PARSE_STATE_INT_OPT -> if (i > stateStart) intPattern.add(OptDigits((i - stateStart) / decimalFormat.digit.length))
                    PARSE_STATE_INT_MANDATORY -> if (i > stateStart) intPattern.add(ReqDigits((i - stateStart) / decimalFormat.zeroDigit.length))
                    PARSE_STATE_DECIMAL_MANDATORY -> if (i > stateStart) decimalPattern.add(ReqDigits((i - stateStart) / decimalFormat.zeroDigit.length))
                    PARSE_STATE_DECIMAL_OPT -> if (i > stateStart) decimalPattern.add(OptDigits((i - stateStart) / decimalFormat.digit.length))
                    PARSE_STATE_EXP_MANDATORY -> if (i > stateStart) expPattern.add(ReqDigits((i - stateStart) / decimalFormat.zeroDigit.length))
                    PARSE_STATE_EXP_OPT -> if (i > stateStart) expPattern.add(OptDigits((i - stateStart) / decimalFormat.digit.length))
                }
                when (nextState) {
                    PARSE_STATE_DECIMAL_MANDATORY,
                    PARSE_STATE_EXP_MANDATORY -> {
                        assert(incI) { "Expected inc to be true in this case" }
                        i = pictureSegment.nextCodePointPos(i)
                    }
                }

                stateStart = i
                if (nextState >= 0) {
                    state = nextState
                    nextState = -1
                }
            } while (state < PARSE_STATE_PERCENT && i < pictureSegment.length)

            if (stateStart < pictureSegment.length) suffix = pictureSegment.substring(stateStart)

            when (state) {
                PARSE_STATE_PERCENT -> {
                    require(expPattern.isEmpty()) { "Exponents and percent are exclusive: $pictureSegment" }
                    return PercentFormatter(prefix, intPattern, decimalPattern, suffix)
                }

                PARSE_STATE_PERMILLE -> {
                    require(expPattern.isEmpty()) { "Exponents and permille are exclusive: $pictureSegment" }
                    return PermilleFormatter(prefix, intPattern, decimalPattern, suffix)
                }
            }

            return DecimalDigitPatternFormatter(prefix, intPattern, decimalPattern, expPattern, suffix)
        }

        private fun parsePicture(picture: String, decimalFormat: DecimalFormat): PosNegFormatter {
            val negIdx = picture.indexOf(decimalFormat.patternSeparator.toString())
            val pos: NumberFormatter
            val neg: NumberFormatter
            if (negIdx >= 0) {
                pos = parseSingle(picture.substring(0, negIdx), decimalFormat)
                neg = parseSingle(picture.substring(negIdx + 1), decimalFormat)
            } else {
                pos = parseSingle(picture, decimalFormat)
                val newPrefix = pos.prefix?.let { "${decimalFormat.minusSign}$it" } ?: decimalFormat.minusSign.toString()
                neg = pos.copy(prefix = newPrefix)
            }
            return PosNegFormatter(
                pos.normalized(),
                neg.normalized(),
                decimalFormat
            )
        }

        private const val PARSE_STATE_PREFIX=0
        private const val PARSE_STATE_INT_OPT=1
        private const val PARSE_STATE_INT_MANDATORY=2
        private const val PARSE_STATE_DECIMAL_MANDATORY=3
        private const val PARSE_STATE_DECIMAL_OPT=4
        private const val PARSE_STATE_EXP_MANDATORY=5
        private const val PARSE_STATE_EXP_OPT=6
        private const val PARSE_STATE_PERCENT=7
        private const val PARSE_STATE_PERMILLE=8
        private const val PARSE_STATE_SUFFIX=9


    }

    internal class PosNegFormatter(
        val posFormatter: NumberFormatter,
        val negFormatter: NumberFormatter,
        val decimalFormat: DecimalFormat
    ) {
        fun formatTo(receiver: Appendable, value: XsdNumeric<*>) {
            when (value) {
                is XsdDecimal -> when {
                    value.sign >= 0 -> posFormatter.formatTo(receiver, value, decimalFormat)
                    else -> negFormatter.formatTo(receiver, value.abs(), decimalFormat)
                }

                is XsdDouble -> when {
                    value.isNaN -> receiver.append(decimalFormat.NaN)
                    value.value >= 0f -> posFormatter.formatTo(receiver, value.value, decimalFormat)
                    else -> negFormatter.formatTo(receiver, value.value.absoluteValue, decimalFormat)
                }

                is XsdFloat -> when {
                    value.isNaN -> receiver.append(decimalFormat.NaN)
                    value.value >= 0f -> posFormatter.formatTo(receiver, value.value, decimalFormat)
                    else -> negFormatter.formatTo(receiver, value.value.absoluteValue, decimalFormat)
                }
            }
        }
    }

    internal sealed class NumberFormatter(
        val prefix: String?,
        val intPattern: List<FormatElem>,
        val decimalPattern: List<FormatElem>,
        val suffix: String?,
        val regularGrouping: Int = -1
    ) {

        val decimalDigits:Int
        val minDecimalDigits: Int

        val minIntDigits: Int = intPattern.asSequence().filterIsInstance<ReqDigits>().sumOf { it.length }

        init {
            var opt = 0
            var req = 0
            for (d in decimalPattern) {
                when (d) {
                    is OptDigits -> opt = d.length
                    is ReqDigits -> req = d.length
                    else -> Unit
                }
            }
            decimalDigits = opt + req
            minDecimalDigits = req
        }


        abstract fun normalized(): NumberFormatter

        abstract fun copy(
            prefix: String? = this.prefix,
            intPattern: List<FormatElem> = this.intPattern,
            decimalPattern: List<FormatElem> = this.decimalPattern,
            suffix: String? = this.suffix,
            regularGrouping: Int = this.regularGrouping,
        ): NumberFormatter

        fun normalizeBase(): NormalizeResult {
            var grouping = 0
            var mandatoryIntCount = 0
            var optIntCount = 0

            var runningDigitCount = 0
            for (x in intPattern.reversed()) {
                when (x) {
                    is GroupingSeparator if (grouping == 0) -> {
                        require(runningDigitCount > 0) { "Grouping separators must not directly follow eachother" }
                        grouping = runningDigitCount
                        runningDigitCount = 0
                    }

                    is GroupingSeparator if (grouping > 0) -> when {
                        grouping != runningDigitCount -> grouping = -1
                        else -> runningDigitCount = 0
                    }

                    is GroupingSeparator -> runningDigitCount = 0

                    is OptDigits -> {
                        optIntCount += x.length
                        runningDigitCount += x.length
                        if (grouping in 1..<runningDigitCount) grouping = -1 // not regular
                    }

                    is ReqDigits -> {
                        mandatoryIntCount += x.length
                        runningDigitCount += x.length
                        if (grouping in 1..<runningDigitCount) grouping = -1
                    }
                }
            }

            var minDecimalCount = 0
            var optDecimalCount = 0
            for (x in decimalPattern) {
                when (x) {
                    is OptDigits -> optDecimalCount += x.length
                    is ReqDigits -> minDecimalCount += x.length
                    is GroupingSeparator -> Unit // ignore
                }
            }

            return NormalizeResult(grouping, mandatoryIntCount, optIntCount, minDecimalCount, optDecimalCount)
        }

        abstract fun formatTo(receiver: Appendable, number: XsdDecimal, decimalFormat: DecimalFormat)

        abstract fun formatTo(receiver: Appendable, number: Float, decimalFormat: DecimalFormat)

        abstract fun formatTo(receiver: Appendable, number: Double, decimalFormat: DecimalFormat)

        fun formatNonSuffixTo(receiver: Appendable, number: XsdDecimal, decimalFormat: DecimalFormat) {
            if (prefix != null) receiver.append(prefix)

            val str = when {
                number !is XsdInteger -> {
                    val bd = number.toBigDecimal()
                    val extraDecimalsNeeded = decimalDigits + bd.exponent
                    buildString(bd.precisionDigits + extraDecimalsNeeded) {
                        for (i in (bd.precisionDigits + bd.exponent - 1) downTo (-decimalDigits)) {
                            append(bd.getDecimalDigit(i))
                        }
                    }

                }

                decimalDigits > 0 -> buildString(number.size.toInt() * 18 + decimalDigits) {
                    append(number.toString())
                    repeat(decimalDigits) { append('0') }
                }

                else -> number.toString()
            }

            formatHelper(str, str.length, intPattern.size + decimalPattern.size - 1, receiver, decimalFormat, true)
        }

        fun formatNonSuffixTo(receiver: Appendable, number: Double, decimalFormat: DecimalFormat) {
            if (prefix != null) receiver.append(prefix)
            if (number.isInfinite()) {
                receiver.append(decimalFormat.infinity)
                return
            }

            var n = number

            var mulRemaining = decimalDigits
            while (mulRemaining > 0) {
                when (mulRemaining) {
                    1 -> n *= 10
                    2 -> n *= 100
                    3 -> n *= 1_000
                    4 -> n *= 10_000
                    5 -> n *= 100_000
                    6 -> n *= 1000_000
                    7 -> n *= 1_000_000
                    8 -> n *= 10_000_000
                    9 -> n *= 100_000_000
                    else -> {
                        n *= 100_000_000
                        mulRemaining -= 9
                        continue
                    }
                }
                break
            }

            val final = n.roundToLong().toString()

            formatHelper(final, final.length, intPattern.size + decimalPattern.size - 1, receiver, decimalFormat, true)
        }

        fun formatNonSuffixTo(receiver: Appendable, number: Float, decimalFormat: DecimalFormat) {
            if (prefix != null) receiver.append(prefix)
            if (number.isInfinite()) {
                receiver.append(decimalFormat.infinity)
                return
            }

            var n = number

            var mulRemaining = decimalDigits
            while (mulRemaining > 0) {
                when (mulRemaining) {
                    1 -> n *= 10
                    2 -> n *= 100
                    3 -> n *= 1_000
                    4 -> n *= 10_000
                    5 -> n *= 100_000
                    6 -> n *= 1000_000
                    7 -> n *= 1_000_000
                    8 -> n *= 10_000_000
                    9 -> n *= 100_000_000
                    else -> {
                        n *= 100_000_000
                        mulRemaining -= 9
                        continue
                    }
                }
                break
            }

            val final = n.roundToLong().toString()

            formatHelper(final, final.length, intPattern.size + decimalPattern.size - 1, receiver, decimalFormat, true)
        }

        private fun Appendable.appendDigit(c: Char, decimalFormat: DecimalFormat) {
            appendUnicode(UnicodeChar(decimalFormat.zeroDigit.codePoint + (c.code - '0'.code)))
        }

        fun formatHelper(
            digitSource: String,
            stringPos: Int,
            patternPos: Int,
            appendable: Appendable,
            decimalFormat: DecimalFormat,
            canBeZero: Boolean,
        ) {
            if (patternPos < 0) {
                val regularGrouping = regularGrouping
                when {
                    stringPos <= 0 -> return

                    regularGrouping > 0 -> {
                        val alreadyGrouped = intPattern.asSequence().takeWhile { it !is GroupingSeparator }.sumOf { it.length }
                        val groupOffset = (stringPos - alreadyGrouped).mod(regularGrouping)
                        for (i in 0 until groupOffset) {
                            appendable.appendDigit(digitSource[i], decimalFormat)
                        }
                        var i = groupOffset
                        while (i < stringPos) {
                            appendable.appendUnicode(decimalFormat.groupingSeparator)
                            for (_ in 0 until regularGrouping) {
                                appendable.appendDigit(digitSource[i++], decimalFormat)
                            }
                        }
                    }

                    else -> appendable.append(digitSource, 0, stringPos)
                }
                return
            }

            if (patternPos >= intPattern.size) { // decimals
                when (val elem = decimalPattern[patternPos - intPattern.size]) {
                    is GroupingSeparator -> {
                        formatHelper(
                            digitSource,
                            stringPos,
                            patternPos - 1,
                            appendable,
                            decimalFormat,
                            canBeZero = true
                        )
                        if (!canBeZero) appendable.appendUnicode(decimalFormat.groupingSeparator)
                    }

                    is OptDigits if (canBeZero && digitSource[stringPos] == '0') -> {
                        val startPos = (stringPos - elem.length).coerceAtLeast(0)
                        val containsNonZero = (startPos..stringPos).any { digitSource[it] != '0' }

                        formatHelper(
                            digitSource,
                            startPos,
                            patternPos - 1,
                            appendable,
                            decimalFormat,
                            canBeZero = !containsNonZero
                        )

                        if (containsNonZero) {
                            var lastPos = stringPos

                            while (lastPos > startPos && digitSource[lastPos] == '0') lastPos -= 1
                            // leading zeros if needed
                            for (_ in 0 until (elem.length - (lastPos - startPos))) {
                                appendable.appendUnicode(decimalFormat.zeroDigit)
                            }

                            for (c in startPos until lastPos) {
                                appendable.appendDigit(digitSource[c], decimalFormat)
                            }
                        }

                    }

                    else -> {
                        val startPos = (stringPos - elem.length).coerceAtLeast(0)

                        formatHelper(
                            digitSource,
                            startPos,
                            patternPos - 1,
                            appendable,
                            decimalFormat,
                            canBeZero = false
                        )

                        if (patternPos == intPattern.size) {
                            appendable.appendUnicode(decimalFormat.decimalSeparator)
                        }

                        // leading zeros if needed
                        for (_ in 0 until (elem.length - (stringPos - startPos))) {
                            appendable.appendUnicode(decimalFormat.zeroDigit)
                        }

                        for (c in startPos until stringPos) {
                            appendable.appendDigit(digitSource[c], decimalFormat)
                        }
                    }

                }
            } else { // integer part

                when (val elem = intPattern[patternPos]) {
                    is GroupingSeparator if ((stringPos < 0 && patternPos == 0) ||
                            (intPattern[patternPos - 1] !is ReqDigits)) -> return

                    is GroupingSeparator -> {
                        formatHelper(digitSource, stringPos, patternPos - 1, appendable, decimalFormat, false)
                        appendable.appendUnicode(decimalFormat.groupingSeparator)
                    }

                    is OptDigits -> {
                        if (stringPos < elem.length) {
                            for (i in 0 until stringPos) {
                                appendable.appendDigit(digitSource[i], decimalFormat)
                            }
                        } else {
                            formatHelper(digitSource, stringPos, patternPos - 1, appendable, decimalFormat, false)
                            for (i in (stringPos - elem.length) until stringPos) {
                                appendable.appendDigit(digitSource[i], decimalFormat)
                            }
                        }
                    }

                    is ReqDigits -> {
                        if (stringPos < elem.length) {
                            formatHelper(digitSource, -1, patternPos - 1, appendable, decimalFormat, false)
                            for (_ in 0 until (elem.length - stringPos)) {
                                appendable.appendDigit('0', decimalFormat)
                            }
                            for (i in 0 until stringPos) {
                                appendable.appendDigit(digitSource[i], decimalFormat)
                            }
                        } else {
                            formatHelper(digitSource, stringPos-elem.length, patternPos - 1, appendable, decimalFormat, false)
                            for (i in (stringPos - elem.length) until stringPos) {
                                appendable.appendDigit(digitSource[i], decimalFormat)
                            }
                        }
                    }
                }
            }
        }

        internal data class NormalizeResult(
            val newGrouping: Int,
            val minIntCount: Int,
            val optIntCount: Int,
            val minDecimalCount: Int,
            val optDecimalCount: Int,
        )
    }

    private class PercentFormatter(
        prefix: String?,
        intPattern: List<FormatElem>,
        decimalPattern: List<FormatElem>,
        suffix: String?,
        regularGrouping: Int = -1
    ) : NumberFormatter(prefix, intPattern, decimalPattern, suffix, regularGrouping) {
        override fun normalized(): PercentFormatter {
            val r = normalizeBase()
            return PercentFormatter(prefix, intPattern, decimalPattern, suffix, r.newGrouping)
        }

        override fun copy(
            prefix: String?,
            intPattern: List<FormatElem>,
            decimalPattern: List<FormatElem>,
            suffix: String?,
            regularGrouping: Int
        ): PercentFormatter {
            return PercentFormatter(prefix, intPattern, decimalPattern, suffix, regularGrouping)
        }

        override fun formatTo(receiver: Appendable, number: XsdDecimal, decimalFormat: DecimalFormat) {
            formatNonSuffixTo(receiver, number * XsdInt(100), decimalFormat)
            receiver.append(decimalFormat.percent)
            if (suffix != null) receiver.append(suffix)
        }

        override fun formatTo(receiver: Appendable, number: Float, decimalFormat: DecimalFormat) {
            formatNonSuffixTo(receiver, number * 100, decimalFormat)
            receiver.append(decimalFormat.percent)
            if (suffix != null) receiver.append(suffix)
        }

        override fun formatTo(receiver: Appendable, number: Double, decimalFormat: DecimalFormat) {
            formatNonSuffixTo(receiver, number * 100, decimalFormat)
            receiver.append(decimalFormat.percent)
            if (suffix != null) receiver.append(suffix)
        }


    }

    private class PermilleFormatter(
        prefix: String?,
        intPattern: List<FormatElem>,
        decimalPattern: List<FormatElem>,
        suffix: String?,
        regularGrouping: Int = -1
    ) : NumberFormatter(prefix, intPattern, decimalPattern, suffix, regularGrouping) {
        override fun normalized(): PercentFormatter {
            val r = normalizeBase()
            return PercentFormatter(prefix, intPattern, decimalPattern, suffix, r.newGrouping)
        }

        override fun copy(
            prefix: String?,
            intPattern: List<FormatElem>,
            decimalPattern: List<FormatElem>,
            suffix: String?,
            regularGrouping: Int
        ): PermilleFormatter {
            return PermilleFormatter(prefix, intPattern, decimalPattern, suffix, regularGrouping)
        }

        override fun formatTo(receiver: Appendable, number: XsdDecimal, decimalFormat: DecimalFormat) {
            formatNonSuffixTo(receiver, number * XsdInt(1000), decimalFormat)
            receiver.append(decimalFormat.perMille)
            if (suffix != null) receiver.append(suffix)
        }

        override fun formatTo(receiver: Appendable, number: Float, decimalFormat: DecimalFormat) {
            formatNonSuffixTo(receiver, number * 1000, decimalFormat)
            receiver.append(decimalFormat.perMille)
            if (suffix != null) receiver.append(suffix)
        }

        override fun formatTo(receiver: Appendable, number: Double, decimalFormat: DecimalFormat) {
            formatNonSuffixTo(receiver, number * 1000, decimalFormat)
            receiver.append(decimalFormat.perMille)
            if (suffix != null) receiver.append(suffix)
        }

    }

    private class DecimalDigitPatternFormatter(
        prefix: String?,
        intPattern: List<FormatElem>,
        decimalPattern: List<FormatElem>,
        val expPattern: List<FormatElem>,
        suffix: String?,
        regularGrouping: Int = -1
    ) : NumberFormatter(prefix, intPattern, decimalPattern, suffix, regularGrouping){

        override fun copy(
            prefix: String?,
            intPattern: List<FormatElem>,
            decimalPattern: List<FormatElem>,
            suffix: String?,
            regularGrouping: Int
        ): DecimalDigitPatternFormatter {
            return DecimalDigitPatternFormatter(prefix, intPattern, decimalPattern, expPattern, suffix, regularGrouping)
        }

        fun copy(
            prefix: String? = this.prefix,
            intPattern: List<FormatElem> = this.intPattern,
            decimalPattern: List<FormatElem> = this.decimalPattern,
            expPattern: List<FormatElem>,
            suffix: String? = this.suffix,
            regularGrouping: Int = this.regularGrouping,
        ): DecimalDigitPatternFormatter {
            return DecimalDigitPatternFormatter(prefix, intPattern, decimalPattern, expPattern, suffix, regularGrouping)
        }


        override fun normalized(): NumberFormatter {
            val r = normalizeBase()
            return DecimalDigitPatternFormatter(prefix, intPattern, decimalPattern, expPattern, suffix, r.newGrouping)
        }

        override fun formatTo(receiver: Appendable, number: XsdDecimal, decimalFormat: DecimalFormat) {
            if (expPattern.isNotEmpty()) {
                val minExpSize = expPattern.asSequence().filterIsInstance<ReqDigits>().sumOf { it.length }
                val scalingFactor = minIntDigits
                

            } else {
                when (number) {
                    is XsdFloat -> formatNonSuffixTo(receiver, number.value, decimalFormat)
                    is XsdDouble -> formatNonSuffixTo(receiver, number.value, decimalFormat)
                    is XsdDecimal -> formatNonSuffixTo(receiver, number, decimalFormat)
                }
                if (suffix != null) receiver.append(suffix)
            }
        }

        override fun formatTo(
            receiver: Appendable,
            number: Float,
            decimalFormat: DecimalFormat
        ) {
            TODO("not implemented")
        }

        override fun formatTo(
            receiver: Appendable,
            number: Double,
            decimalFormat: DecimalFormat
        ) {
            TODO("not implemented")
        }

        override fun toString(): String = buildString {
            if (prefix != null) append( prefix )
            intPattern.forEach { append(it) }
            if (decimalPattern.isNotEmpty()) {
                append(" . ")
                decimalPattern.forEach { append(it) }
            }
            if (suffix != null) append( suffix )
        }
    }

    internal sealed class FormatElem {
        abstract val length: Int
    }

    private class OptDigits(override val length: Int) : FormatElem() {
        override fun toString(): String = "#".repeat(length)
    }
    private class ReqDigits(override val length: Int) : FormatElem() {
        override fun toString(): String = "0".repeat(length)
    }

    private object GroupingSeparator: FormatElem() {
        override val length: Int get() = 0
        override fun toString(): String = ","
    }

}
