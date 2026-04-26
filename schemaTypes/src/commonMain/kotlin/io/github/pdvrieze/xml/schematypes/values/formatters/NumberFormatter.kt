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
import io.github.pdvrieze.xml.schematypes.values.instances.BigDecimal
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.core.internal.nextCodePointPos
import kotlin.jvm.JvmStatic
import kotlin.math.absoluteValue
import kotlin.math.sign

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

    @XmlUtilInternal
    fun hasExponent(): Boolean {
        (format.posFormatter as? DecimalDigitPatternFormatter)?.let { if(it.expPattern.isNotEmpty()) return true }
        (format.negFormatter as? DecimalDigitPatternFormatter)?.let { return it.expPattern.isNotEmpty() }
        return false
    }


    override fun toString(): String {
        return format.toString()
    }

    companion object {

        private fun parseSingle(pictureSegment: String, decimalFormat: DecimalFormat): NumberFormatter {
            var state = PARSE_STATE_PREFIX // nothing yet
            var nextState = PARSE_STATE_PREFIX
            var stateStart = 0

            var prefix: String? = null
            val intPattern = mutableListOf<FormatElem>()
            val decimalPattern = mutableListOf<FormatElem>()
            val expPattern = mutableListOf<FormatElem>()
            var suffix: String? = null

            var i = 0

            do {
                while (i < pictureSegment.length) {
                    when (val cp = pictureSegment.unicodeChar(i)) {
                        decimalFormat.decimalSeparator -> {
                            require (state < PARSE_STATE_DECIMAL_MANDATORY) { "Decimal separator must only occur after integer part" }
                            require(i > stateStart || intPattern.lastOrNull() !is GroupingSeparator) { "Decimal separator must not directly follow a grouping separator: $pictureSegment" }
                            nextState = PARSE_STATE_DECIMAL_MANDATORY
                            break
                        }

                        // If prefix there was no active character so ignore exponent (which must be surrounded by actives)
                        decimalFormat.exponentSeparator if (state != PARSE_STATE_PREFIX) -> {
                            nextState = when {
                                // must be (valid) suffix
                                state >= PARSE_STATE_EXP_MANDATORY -> PARSE_STATE_SUFFIX
                                else -> PARSE_STATE_EXP_MANDATORY
                            }
                            break
                        }

                        in decimalFormat.digitRange -> if (state != PARSE_STATE_INT_MANDATORY &&
                            state != PARSE_STATE_DECIMAL_MANDATORY && state != PARSE_STATE_EXP_MANDATORY) {
                            when (state) {
                                PARSE_STATE_PREFIX -> if (i > 0) prefix = pictureSegment.substring(0, i)
                                PARSE_STATE_INT_OPT -> if (i > stateStart) intPattern.add(OptDigits((i - stateStart) / decimalFormat.digit.length))
                                PARSE_STATE_DECIMAL_MANDATORY -> if (i > stateStart) decimalPattern.add(ReqDigits((i - stateStart) / decimalFormat.zeroDigit.length))
                                PARSE_STATE_EXP_MANDATORY -> if (i > stateStart) expPattern.add(ReqDigits((i - stateStart) / decimalFormat.zeroDigit.length))
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
                            val baseLen = i - stateStart
                            val targetPattern = when (state) {
                                PARSE_STATE_PREFIX -> {
                                    state = PARSE_STATE_INT_OPT
                                    intPattern
                                }

                                PARSE_STATE_INT_MANDATORY, PARSE_STATE_INT_OPT, PARSE_STATE_PREFIX -> intPattern
                                PARSE_STATE_DECIMAL_OPT, PARSE_STATE_DECIMAL_MANDATORY -> decimalPattern
                                PARSE_STATE_EXP_OPT, PARSE_STATE_EXP_MANDATORY -> expPattern
                                else -> throw IllegalArgumentException("Unexpected grouping separator in state $state")
                            }
                            if (baseLen > 0) {
                                val pending = when (state) {
                                    PARSE_STATE_INT_OPT, PARSE_STATE_DECIMAL_OPT, PARSE_STATE_EXP_OPT
                                        -> OptDigits(baseLen / decimalFormat.digit.length)

                                    else -> ReqDigits(baseLen / decimalFormat.zeroDigit.length)
                                }
                                targetPattern.add(pending)
                            } else {
                                require (targetPattern.lastOrNull() !is GroupingSeparator) { "Grouping separator must not directly follow a grouping separator: $pictureSegment" }
                            }
                            targetPattern.add(GroupingSeparator)

                            i = pictureSegment.nextCodePointPos(i)
                            stateStart = i
                            continue // skip default increase in position
                        }

                        decimalFormat.percent -> {
                            nextState = PARSE_STATE_PERCENT
                            break
                        }

                        decimalFormat.perMille -> {
                            nextState = PARSE_STATE_PERMILLE
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
                    PARSE_STATE_EXP_MANDATORY -> when {
                        i > stateStart -> expPattern.add(ReqDigits((i - stateStart) / decimalFormat.zeroDigit.length))
                        else -> {
                            stateStart -= decimalFormat.exponentSeparator.length
                            state = PARSE_STATE_SUFFIX
                            break
                        }
                    }
                    PARSE_STATE_EXP_OPT -> if (i > stateStart) expPattern.add(OptDigits((i - stateStart) / decimalFormat.digit.length))
                }
                when (nextState) {
                    PARSE_STATE_DECIMAL_MANDATORY,
                    PARSE_STATE_EXP_MANDATORY,
                    PARSE_STATE_PERCENT,
                    PARSE_STATE_PERMILLE -> i = pictureSegment.nextCodePointPos(i)
                }

                stateStart = i
                if (nextState >= 0) {
                    state = nextState
                    nextState = -1
                }
            } while (state < PARSE_STATE_PERCENT && i < pictureSegment.length)

            if (stateStart < pictureSegment.length) {
                suffix = pictureSegment.substring(stateStart)
                var i = 0
                while (i < suffix.length) {
                    val cp = suffix.unicodeChar(i)
                    if (cp != decimalFormat.exponentSeparator && decimalFormat.isActive(cp)) throw IllegalArgumentException("Suffix must not contain active characters: $suffix")

                    i = suffix.nextCodePointPos(i)
                }
            }

            require (decimalPattern.firstOrNull() !is GroupingSeparator) { "The decimal part cannot start with a grouping separator" }

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

            require (intPattern.isNotEmpty() || decimalPattern.isNotEmpty()) { "No digits in mantissa: $pictureSegment" }

            return DecimalDigitPatternFormatter(prefix, intPattern, decimalPattern, expPattern, suffix)
        }

        private fun parsePicture(picture: String, decimalFormat: DecimalFormat): PosNegFormatter {
            val negIdx = picture.indexOf(decimalFormat.patternSeparator.toString())
            val pos: NumberFormatter
            val neg: NumberFormatter
            if (negIdx >= 0) {
                pos = parseSingle(picture.substring(0, negIdx), decimalFormat).normalized()
                neg = parseSingle(picture.substring(negIdx + 1), decimalFormat).normalized()
            } else {
                pos = parseSingle(picture, decimalFormat).normalized()
                val newPrefix = pos.prefix?.let { "${decimalFormat.minusSign}$it" } ?: decimalFormat.minusSign.toString()
                neg = pos.copy(prefix = newPrefix)
            }
            return PosNegFormatter(pos, neg, decimalFormat)
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
                    value.value >= 0.0 -> posFormatter.formatTo(receiver, value.value, decimalFormat)
                    value.value.isInfinite() -> receiver.appendUnicode(decimalFormat.minusSign).append(decimalFormat.infinity)
                    else -> negFormatter.formatTo(receiver, value.value.absoluteValue, decimalFormat)
                }

                is XsdFloat -> when {
                    value.isNaN -> receiver.append(decimalFormat.NaN)
                    value.value >= 0f -> posFormatter.formatTo(receiver, value.value, decimalFormat)
                    value.value.isInfinite() -> receiver.appendUnicode(decimalFormat.minusSign).append(decimalFormat.infinity)
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
                    is OptDigits -> opt += d.length
                    is ReqDigits -> req += d.length
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

        open fun formatNonSuffixTo(receiver: Appendable, number: XsdDecimal, decimalFormat: DecimalFormat) {
            if (prefix != null) receiver.append(prefix)

            val str = when {
                number.sign == 0 -> "0"
                number !is XsdInteger -> {
                    val bd = number.toBigDecimal().exp10(decimalDigits) // multply with decimal digits
                        .roundToHalfEven()

                    buildString(bd.precisionDigits) {
                        for (i in (bd.precisionDigits + bd.exponent - 1) downTo 0) {
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
                        val alreadyPendingDigitsInGroup = intPattern.asSequence().takeWhile { it !is GroupingSeparator }.sumOf { it.length }
                        val groupOffset = (stringPos + alreadyPendingDigitsInGroup).mod(regularGrouping)

                        // never start with grouping
                        appendable.appendDigit(digitSource[0], decimalFormat)
                        for (i in 1 until stringPos) {
                            if (i.mod(regularGrouping) == groupOffset) appendable.appendUnicode(decimalFormat.groupingSeparator)
                            appendable.appendDigit(digitSource[i], decimalFormat)
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

                    is OptDigits if (canBeZero && stringPos > 0 && digitSource[stringPos - 1] == '0') -> {
                        val startPos = (stringPos - elem.length).coerceAtLeast(0)
                        val containsNonZero = (startPos..<stringPos).any { digitSource[it] != '0' }

                        formatHelper(
                            digitSource,
                            startPos,
                            patternPos - 1,
                            appendable,
                            decimalFormat,
                            canBeZero = !containsNonZero
                        )

                        if (containsNonZero) {
                            if (patternPos == intPattern.size) {
                                appendable.appendUnicode(decimalFormat.decimalSeparator)
                            }
                            var lastPos = stringPos

                            while (lastPos > startPos && digitSource[lastPos - 1] == '0') lastPos -= 1

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
                    is GroupingSeparator if (stringPos < 0 && (patternPos == 0 ||
                            intPattern[patternPos - 1] is OptDigits)) -> return

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
                            formatHelper(digitSource, stringPos - elem.length, patternPos - 1, appendable, decimalFormat, false)
                            for (i in (stringPos - elem.length) until stringPos) {
                                appendable.appendDigit(digitSource[i], decimalFormat)
                            }
                        }
                    }

                    is ReqDigits -> {
                        if (stringPos < elem.length) {
                            formatHelper(digitSource, -1, patternPos - 1, appendable, decimalFormat, false)
                            for (_ in 0 until (elem.length - stringPos.coerceAtLeast(0))) {
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
        ) {
            val maxIntCount get() = minIntCount + optIntCount
            val maxDecimalCount get() = minDecimalCount + optDecimalCount
        }

        companion object {
            @JvmStatic
            protected fun addRequiredDigitTail(source: List<FormatElem>): List<FormatElem> {
                if (source.isEmpty()) return listOf(ReqDigits(1))
                val tail = source.last() as? OptDigits ?: return source
                val head = source.asSequence().take(source.size - 1)

                val result = ArrayList<FormatElem>(source.size + 1)
                result.addAll(head)
                if (tail.length > 1) result.add(OptDigits(tail.length - 1))
                result.add(ReqDigits(1))

                return result
            }

            @JvmStatic
            protected fun addRequiredDigitFront(source: List<FormatElem>): List<FormatElem> {
                if (source.isEmpty()) return listOf(ReqDigits(1))
                val head = source.first() as? OptDigits ?: return source
                val tail = source.asSequence().drop(1)

                val result = ArrayList<FormatElem>(source.size + 1)
                result.add(ReqDigits(1))
                if (head.length > 1) result.add(OptDigits(head.length - 1))
                result.addAll(tail)

                return result
            }

        }
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
            var newIntPattern = intPattern
            var newDecimalPattern = decimalPattern
            if (r.minIntCount == 0 && r.maxDecimalCount == 0) {
                newIntPattern = addRequiredDigitTail(intPattern)
            } else if (r.minIntCount ==0 && r.minDecimalCount == 0) {
                newDecimalPattern = addRequiredDigitFront(decimalPattern)
            }

            return PercentFormatter(prefix, newIntPattern, newDecimalPattern, suffix, r.newGrouping)
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
            val multplied = when (number) {
                is XsdInt if (number.intValue < MAX_INT_BEFORE_MULT) -> number * 100
                is XsdLong if (number.longValue < MAX_LONG_BEFORE_MULT) -> number * 100L
                is XsdUnsignedInt if (number.uIntValue < MAX_UINT_BEFORE_MULT) -> number * 100u
                is XsdUnsignedLong if (number.uLongValue < MAX_ULONG_BEFORE_MULT) -> number * 100uL
                is XsdInteger -> number.toBigInt() * 100
                else -> number.toBigDecimal().exp10(2)
            }

            formatNonSuffixTo(receiver, multplied, decimalFormat)
            receiver.append(decimalFormat.percent)
            if (suffix != null) receiver.append(suffix)
        }

        override fun formatTo(receiver: Appendable, number: Float, decimalFormat: DecimalFormat) {
            formatTo(receiver, BigDecimal(number), decimalFormat)
        }

        override fun formatTo(receiver: Appendable, number: Double, decimalFormat: DecimalFormat) {
            formatTo(receiver, BigDecimal(number), decimalFormat)
        }

        companion object {
            const val MAX_INT_BEFORE_MULT = Int.MAX_VALUE/100
            val MAX_UINT_BEFORE_MULT = UInt.MAX_VALUE/100u
            const val MAX_LONG_BEFORE_MULT = Long.MAX_VALUE/100L
            val MAX_ULONG_BEFORE_MULT = ULong.MAX_VALUE/100uL
        }

    }

    private class PermilleFormatter(
        prefix: String?,
        intPattern: List<FormatElem>,
        decimalPattern: List<FormatElem>,
        suffix: String?,
        regularGrouping: Int = -1
    ) : NumberFormatter(prefix, intPattern, decimalPattern, suffix, regularGrouping) {
        override fun normalized(): PermilleFormatter {
            val r = normalizeBase()
            var newIntPattern = intPattern
            var newDecimalPattern = decimalPattern
            if (r.minIntCount == 0 && r.maxDecimalCount == 0) {
                newIntPattern = addRequiredDigitTail(intPattern)
            } else if (r.minIntCount ==0 && r.minDecimalCount == 0) {
                newDecimalPattern = addRequiredDigitFront(decimalPattern)
            }

            return PermilleFormatter(prefix, newIntPattern, newDecimalPattern, suffix, r.newGrouping)
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
            val multiplied = when (number) {
                is XsdInt if (number.intValue < MAX_INT_BEFORE_MULT) -> number * 1000
                is XsdLong if (number.longValue < MAX_LONG_BEFORE_MULT) -> number * 1000L
                is XsdUnsignedInt if (number.uIntValue < MAX_UINT_BEFORE_MULT) -> number * 1000u
                is XsdUnsignedLong if (number.uLongValue < MAX_ULONG_BEFORE_MULT) -> number * 1000uL
                is XsdInteger -> number.toBigInt() * 1000
                else -> number.toBigDecimal().exp10(3)
            }

            formatNonSuffixTo(receiver, multiplied, decimalFormat)
            receiver.append(decimalFormat.perMille)
            if (suffix != null) receiver.append(suffix)
        }

        override fun formatTo(receiver: Appendable, number: Float, decimalFormat: DecimalFormat) {
            formatTo(receiver, BigDecimal(number), decimalFormat)
        }

        override fun formatTo(receiver: Appendable, number: Double, decimalFormat: DecimalFormat) {
            formatTo(receiver, BigDecimal(number), decimalFormat)
        }

        companion object {
            const val MAX_INT_BEFORE_MULT = Int.MAX_VALUE / 1000
            val MAX_UINT_BEFORE_MULT = UInt.MAX_VALUE / 1000u
            const val MAX_LONG_BEFORE_MULT = Long.MAX_VALUE / 1000L
            val MAX_ULONG_BEFORE_MULT = ULong.MAX_VALUE / 1000uL
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

        val expDigits: Int
        val expMinDigits: Int

        init {
            var d = 0
            var md = 0
            for (e in expPattern) {
                when {
                    e is OptDigits -> d += e.length
                    e is ReqDigits -> md += e.length
                }
            }
            expDigits = d + md
            expMinDigits = md
        }

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
            var newIntPattern: List<FormatElem> = intPattern
            var newDecimalPattern = decimalPattern

            if (r.minIntCount == 0 && r.maxDecimalCount == 0) {
                when {
                    expPattern.isNotEmpty() -> newDecimalPattern = listOf(ReqDigits(1))
                    else -> newIntPattern = addRequiredDigitTail(intPattern)
                }
            } else if (expPattern.isNotEmpty() && r.minIntCount == 0 && r.maxIntCount> 0) {
                newIntPattern = addRequiredDigitTail(intPattern)
            } else if (r.minIntCount ==0 && r.minDecimalCount == 0) {
                newDecimalPattern = addRequiredDigitFront(decimalPattern)
            }

            return DecimalDigitPatternFormatter(prefix, newIntPattern, newDecimalPattern, expPattern, suffix, r.newGrouping)
        }

        override fun formatNonSuffixTo(
            receiver: Appendable,
            number: XsdDecimal,
            decimalFormat: DecimalFormat
        ) {
            if (expPattern.isNotEmpty()) {
                val bd = number.toBigDecimal()
                val targetExp = minIntDigits.coerceAtLeast(0) - bd.precisionDigits
                val expShift = targetExp - bd.exponent
                var expValue = bd.exponent - targetExp

                val expectedDigits = decimalDigits + minIntDigits

                var nonExp = bd.exp10(expShift + decimalDigits) // multply with decimal digits
                    .roundToHalfEven()

                // In case we end up increased size due to rounding, correct for that.
                // A loop should not be needed as the increased size should only be 1
                if (nonExp.precisionDigits > expectedDigits) {
                    nonExp = nonExp.exp10(-1).roundToHalfEven()
                    expValue += 1
                }


                // has optional int digits only and needs leading zero
                if (minIntDigits == 0 && intPattern.isNotEmpty() && nonExp.precisionDigits <= decimalDigits) {
                    receiver.append(decimalFormat.zeroDigit) // add leading zero
                }


                if (super.prefix != null) receiver.append(super.prefix)
                val str = buildString(nonExp.precisionDigits.coerceAtLeast(1)) {
                    for (i in (nonExp.precisionDigits + nonExp.exponent - 1) downTo 0) {
                        this.append(nonExp.getDecimalDigit(i))
                    }
                }
                // format the non-exp part
                formatHelper(
                    str,
                    str.length,
                    intPattern.size + decimalPattern.size - 1,
                    receiver,
                    decimalFormat,
                    true
                )

                receiver.append(decimalFormat.exponentSeparator)
                if (expValue < 0) {
                    receiver.append(decimalFormat.minusSign)
                    expValue = expValue.absoluteValue
                }
                val expString = expValue.toString()
                val extraDigits = expMinDigits - expString.length
                if (extraDigits > 0) {
                    repeat(extraDigits) { receiver.appendUnicode(decimalFormat.zeroDigit) }
                }
                if (decimalFormat.zeroDigit.codePoint == '0'.code) {
                    receiver.append(expString)
                } else {
                    for (d in expString) {
                        receiver.appendUnicode(UnicodeChar(decimalFormat.zeroDigit.codePoint + (d.code - '0'.code)))
                    }
                }
            } else {
                super.formatNonSuffixTo(receiver, number, decimalFormat)
            }
        }

        override fun formatTo(receiver: Appendable, number: XsdDecimal, decimalFormat: DecimalFormat) {
            when (number) {
                is XsdFloat -> return formatTo(receiver, number.value, decimalFormat)
                is XsdDouble -> return formatTo(receiver, number.value, decimalFormat)
                is XsdDecimal -> formatNonSuffixTo(receiver, number, decimalFormat)
            }
            if (suffix != null) receiver.append(suffix)
        }

        override fun formatTo(
            receiver: Appendable,
            number: Float,
            decimalFormat: DecimalFormat
        ) {
            when {
                number.isNaN() -> receiver.append(decimalFormat.NaN)
                number.isFinite() -> formatTo(receiver, BigDecimal(number), decimalFormat)
                number.sign > 0 -> receiver.append(decimalFormat.infinity)
                else -> receiver.append(decimalFormat.minusSign).append(decimalFormat.infinity)
            }
        }

        override fun formatTo(
            receiver: Appendable,
            number: Double,
            decimalFormat: DecimalFormat
        ) {
            when {
                number.isNaN() -> receiver.append(decimalFormat.NaN)
                number.isFinite() -> formatTo(receiver, BigDecimal(number), decimalFormat)
                number.sign > 0 -> receiver.append(decimalFormat.infinity)
                else -> receiver.append(decimalFormat.minusSign).append(decimalFormat.infinity)
            }
        }

        override fun toString(): String = buildString {
            if (prefix != null) append( prefix )
            intPattern.forEach { append(it) }
            if (decimalPattern.isNotEmpty()) {
                append(" . ")
                decimalPattern.forEach { append(it) }
            }
            if (expPattern.isNotEmpty()) {
                append(" e ")
                append(expPattern)
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
