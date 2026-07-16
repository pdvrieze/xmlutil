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

package io.github.pdvrieze.xml.schematypes.values.instances

import io.github.pdvrieze.xml.schematypes.RangeException
import io.github.pdvrieze.xml.schematypes.requireRange
import io.github.pdvrieze.xml.schematypes.values.*
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
import nl.adaptivity.xmlutil.core.impl.multiplatform.ifAssertions
import nl.adaptivity.xmlutil.core.internal.appendCodepoint
import kotlin.jvm.JvmInline

@OptIn(ExperimentalUnsignedTypes::class)
/**
 * @property ints A packed array of sequences of 32-bit integers that contains a sequence of 10-bit
 *           numbers
 *
 * @property exponent the Base 10 exponent
 */
abstract class AbstractBigDecimal<T : AbstractBigDecimal<T>> internal constructor(
    internal val ints: UIntArray,
    exponent: Int
) : XsdBigDecimal {

    final override val exponent: Int

    /**
     * Determine the amount of decimal digits this number contains.
     */
    final override val precisionDigits: Int

    init {
        require(ints.size >0) { "There must be at least one int"}
        val lastDigit = ints.last()
        if (ints.size > 1) {
            require(ints.any { (it and 0x7fff_ffffu) != 0u }) { "Zero sign must have a single int" }
            val valZero = ints[0]
            if (valZero and SPECIAL_BIT.toUInt() != 0u) {
                throw IllegalArgumentException(
                    "Special values must have a single int with the special bit"
                )
            }
            require(lastDigit != 0u) { "Last int must not be zero" }
            this.exponent = exponent
        } else {
            this.exponent = if (lastDigit and 0x7fff_ffffu == 0u) 0 else exponent
        }
        val d3 = (lastDigit shr 20) and 0x3ffu
        val d2 = (lastDigit shr 10) and 0x3ffu
        val d1 = (lastDigit shr 0) and 0x3ffu
        val offset: Int
        val d = when {
            d3 != 0u -> { offset = 6; d3 }
            d2 != 0u -> { offset = 3; d2 }
            else -> { offset = 0; d1 }
        }
        precisionDigits = (ints.size - 1) * 9 + offset + when {
            d >= 100u -> 3
            d >= 10u -> 2
            else -> 1
        }
    }

    /*
     * The sign and special values are stored in bits 31 and 32 of the first int:
     *  - Bit 32 represents the sign
     *  - Bit 31 represents the fact that this is one of the special value
     *  - Bit 30 represents NaN. If it is equal to the sign bit it is a regular number, otherwise a NaN
     *  - Bit 29 represents infinity (together with the sign). If NaN this value is meaningless
     *  - Bits 2-29 should be equal to the sign bit (although for NaN they could carry information
     *  - Bit 1 represents the positive or zero value
     * A consequence is that normal numbers work:
     * 1 is positive (00000...001)
     * 0 is zero (00000...000)
     * -1 is negative (11111...111)
     */
    override val sign: Int
        get() {
            val int0 = ints[0].toInt()
            return when {
                ints.size == 1 && int0.and(0x7fff_ffff) == 0 -> 0

                int0 and (SPECIAL_BIT or NAN_BIT) == (SPECIAL_BIT or NAN_BIT) -> throw ArithmeticException("NaN value")

                else -> int0.shr(31).or(1)
            }
        }

    val isZero: Boolean
        get() = ints.size ==1 && ints[0].and(0x3fff_ffffu) == 0u // use mask to allow for -0

    override val isNegative: Boolean get() = ints[0].shr(31) != 0u

    val intDigitSize: Int
        get() = (ints.size * 9) + exponent

    override val isFinite: Boolean
        get() = ints[0].toInt() and SPECIAL_BIT == 0

    abstract val self: T

    override fun exp10(n: Int): T {
        val newDecimalPosition = exponent + n
        return companion.newInstance(ints, newDecimalPosition)
    }

    override val isInteger: Boolean
        get() = exponent >=0

    protected abstract val companion: CompanionBase<T>

    protected abstract fun XsdDecimal.asT(): T

    /**
     * Retrieve the "digit" stored at the given position
     */
    private fun getStoredDigit(pos: D1000StoredPos): UInt {
        return ints.getStoredDigit(pos)
    }

    /**
     * Retrieve the "digit" stored at the given position
     */
    private fun UIntArray.getStoredDigit(pos: D1000StoredPos): UInt {
        return get(pos.intPos).shr(pos.shift).and(DIGIT_MASK.toUInt())
    }

    private fun UIntArray.getStoredDigitOrZero(pos: D1000StoredPos): UInt {
        if (pos.intPos !in this.indices) return 0u
        return get(pos.intPos).shr(pos.shift).and(DIGIT_MASK.toUInt())
    }

    private fun UIntArray.setStoredDigit(pos: D1000StoredPos, value: UInt) {
        ifAssertions {
            assert(value in 0u..<MAX_DIGIT.toUInt()) {
                "Value $value out of range"
            }
        }
        val intPos = pos.intPos
        val shift = pos.shift
        val otherBaseDigits = DIGIT_MASK.toUInt().shl(shift).inv().and(get(intPos))
        this[intPos] = otherBaseDigits.or(value.shl(shift))
    }

    private inline fun UIntArray.updateDigit(pos: D1000StoredPos, update: (UInt) -> UInt) {
        val intPos = pos.intPos
        val shift = pos.shift
        val intAtPos = this[intPos]
        val otherBaseDigits = DIGIT_MASK.toUInt().shl(shift).inv().and(intAtPos)
        val oldValue = intAtPos.shr(shift).and(DIGIT_MASK.toUInt())
        this[intPos] = otherBaseDigits.or(update(oldValue).shl(shift))
    }

    private fun decimalDigitFromStored(pos: D10Pos): UInt {
        val storedDigit = getStoredDigit(pos.toStoredD1000Pos())
        return when (pos.p.mod(3)) {
            0 -> storedDigit % 10u
            1 -> (storedDigit / 10u) % 10u
            else -> storedDigit / 100u
        }
    }

    private fun pseudoDigitFromZeroDec(pos: D10Pos): UInt {
        val lsiPos = D1000StoredPos((pos.p-exponent).floorDiv(3))
        // check correct for small positive exponents
        val inDigitCorrection = (pos.p - exponent).mod(3)
        if (inDigitCorrection == 0) {
            return if (lsiPos.intPos !in ints.indices) 0u else getStoredDigit(lsiPos)
        }
        val nextPos = lsiPos + 1
        // out of range, just return 0
        if (nextPos.intPos < 0 || lsiPos.intPos >= ints.size) return 0u

        return when (inDigitCorrection) {
            1 -> {
                val leastSig = if (lsiPos.p < 0) 0u else getStoredDigit(lsiPos) / 10u
                val mostSig = if (nextPos.intPos !in ints.indices) 0u else (getStoredDigit(nextPos) * 100u).mod(MAX_DIGIT.toUInt())
                leastSig + mostSig
            }

            else -> { // 2 (note that 0 has been handled already
                val leastSig = if (lsiPos.p < 0) 0u else getStoredDigit(lsiPos) / 100u
                val mostSig = if (nextPos.intPos !in ints.indices) 0u else (getStoredDigit(nextPos) * 10u).mod(MAX_DIGIT.toUInt())
                leastSig + mostSig
            }
        }
    }

    /**
     * Retrieve digits as they would be if base 0
     */
    private fun pseudoDigitFromZero(pos: D1000Pos): UInt {
        return pseudoDigitFromZeroDec(D10Pos(pos.p * 3))
    }


    private fun toULongHelper(): ULong {
        var r = 0uL
        var mult = 1uL
        for (i in 0..5) {
            val digit = pseudoDigitFromZero(D1000Pos(i))
            r += digit * mult
            mult *= 1000uL
        }
        val lastDigit = pseudoDigitFromZero(D1000Pos(6))
        requireRange(lastDigit <= 18u) { "Number out of range of ULong" }
        if (lastDigit == 18u) {
            requireRange(r <= 446_744_073_709_551_615uL) {"Number out of range of ULong"}
        }
        r += lastDigit * mult

        return r
    }


    override fun toULong(): ULong {
        val int0 = ints[0].toInt()
        check(int0 and SIGN_BIT == 0) { "Negative value cannot be converted to unsigned long" }
        return toULongHelper()
    }

    override fun toUInt(): UInt {
        val int0 = ints[0].toInt()
        check(int0 and SIGN_BIT == 0) { "Negative value cannot be converted to unsigned int" }
        val r = toULongHelper()

        if (r > UInt.MAX_VALUE) throw RangeException("Value too large to fit in an unsigned int")
        return r.toUInt()
    }


    override fun toLong(): Long {
        val int0 = ints[0].toInt()
        val isNegative = int0 and SIGN_BIT == SIGN_BIT
        val r = toULongHelper()

        when {
            isNegative -> {
                requireRange(r <= Long.MIN_VALUE.toULong()) { "Value too large to fit in a long" }
                return -(r.toLong())
            }

            else -> return r.toLong()
        }
    }

    override fun toInt(): Int {
        val int0 = ints[0].toInt()
        val isNegative = int0 and SIGN_BIT == SIGN_BIT
        val r = toULongHelper()

        requireRange(r shr 31 == 0uL) { "Value too large to fit in an int" }
        return if (isNegative) -(r.toInt()) else r.toInt()
    }

    override fun toBigDecimal(): T {
        return unaryPlus()
    }

    override fun floor(): T {
        if (exponent >= 0 || sign == 0 || !isFinite) return unaryPlus()


        if ((-exponent) %9 ==0) {
            val newInts = ints.copyOfRange((-exponent)/9, ints.size)
            return companion.newInstance(newInts, 0)
        }

        val intDigits = D10Pos(precisionDigits+exponent).toD1000Size()

        val newInts = UIntArray(intDigits.intSize)
        for (j in 0 until intDigits.p step 3) {
            val i = D1000Pos(j)
            newInts[i.intPos] = pseudoDigitFromZero(i + 2) * 1_000_000u + pseudoDigitFromZero(i + 1) * 1_000u + pseudoDigitFromZero(i)
        }


        if (sign < 0) {
            val negDigits = (2 - exponent) / 3
            if ((-1..negDigits).any { pseudoDigitFromZero(D1000Pos(it))==0u }) {
                val int0 = newInts[0]
                // subtract one
                when {
                    int0.and(0x3ffu) != 0x3e7u -> newInts[0] = int0 + 1u

                    int0.shr(10).and(0x3ffu) < 0x3e7u ->
                        newInts[0] = (int0 and 0x3fff_fC00u) + 1000u

                    int0.shr(20).and(0x3ffu) != 0x3e7u ->
                        newInts[0] = (int0 and 0x3ff0_0000u) + 1_000_000u

                    else -> return (companion.newInstance(newInts, 0) + BigDecimal(1u)).unaryMinus()
                }
            }

            newInts[0] = newInts[0] or SIGN_BIT.toUInt() // copy the sign bit
        }
        return companion.newInstance(newInts, 0)
    }

    override fun ceiling(): T {
        return unaryMinus().floor().unaryMinus()
    }

    override fun round(): T {
        return round(0)
    }

    override fun round(precision: Int): T {
        return roundImpl(precision, false)
    }

    override fun roundToInteger(): XsdInteger {
        val lower = pseudoDigitFromZero(D1000Pos(-1)) < 500u

        val pseudoDigitCount = D10Pos(precisionDigits+exponent).toD1000Size()

        val isNeg = sign < 0
        if (pseudoDigitCount.p <= 6) {
            val uLong = toULongHelper()
            return when {
                isNeg -> when {
                    uLong == 0x8000_0000uL -> XsdInt(Int.MIN_VALUE)
                    uLong < 0x8000_0000uL -> XsdInt(-(uLong.toInt()))
                    else -> XsdLong(-(uLong.toLong()))
                }
                uLong < Int.MAX_VALUE.toULong() -> XsdInt(uLong.toInt())
                uLong <= UInt.MAX_VALUE -> XsdUnsignedInt(uLong.toUInt())
                uLong <= Long.MAX_VALUE.toULong() -> XsdLong(uLong.toLong())
                else -> XsdUnsignedLong(uLong)
            }
        }

        val j = pseudoDigitFromZero(D1000Pos(0)) + pseudoDigitFromZero(D1000Pos(1)) * 1000u + pseudoDigitFromZero(D1000Pos(2)) * 1000u

        var runningDigit = BigUnsignedInt(j)

        for (pos in 3 until pseudoDigitCount.p step 3) {
            val i = pseudoDigitFromZero(D1000Pos(pos)) +
                    pseudoDigitFromZero(D1000Pos(pos+1)) * 1000u +
                    pseudoDigitFromZero(D1000Pos(pos+2)) * 1000u

            runningDigit = runningDigit.times(1000_000_000u).plus(XsdUnsignedInt(i))
        }

        return when {
            isNeg && lower -> BigInt(-1, runningDigit.ints, runningDigit.exp) + XsdInt(1)
            isNeg -> BigInt(-1, runningDigit.ints, runningDigit.exp)
            lower -> runningDigit
            else -> runningDigit + XsdInt(1)
        }
    }

    protected open fun roundImpl(precision: Int, halfEven: Boolean): T {
        val adjust = when (sign) {
            0 -> return self
            1 -> XsdInt(1)
            else -> XsdInt(-1)
        }

        val storedLSDPos = D10Pos(-(exponent + precision))
        if (storedLSDPos.p <= 0) return self

        val (resultBase, remainder) = splitAtExponent(-precision)


        // must have omitted leading zeros
        if (remainder.precisionDigits < (precision + remainder.exponent))
            return resultBase

        val leadingD1000 = remainder.ints.getStoredDigit(D1000StoredPos((remainder.precisionDigits-1)/3))
        // note that precisionDigits is a count
        when (remainder.precisionDigits.mod(3)) {
            1 -> when (leadingD1000.mod(10u)) {
                in 0u..4u -> return resultBase
                in 6u..9u -> return resultBase + adjust
            }

            2 -> when (leadingD1000.mod(100u)) {
                in 0u..49u -> return resultBase
                in 51u..99u -> return resultBase + adjust
            }

            else -> when (leadingD1000) {
                in 0u..499u -> return resultBase
                in 501u..999u -> return resultBase + adjust
            }
        }
        if (!halfEven || (0 until remainder.precisionDigits.div(3)).any { remainder.ints[it] != 0u }) {
            return resultBase + adjust
        }
        if (resultBase.ints[0] and 1u == 1u) return resultBase + adjust
        return resultBase
    }

    override fun roundToHalfEven(): T {
        return roundToHalfEven(0)
    }

    override fun roundToHalfEven(precision: Int): T {
        return roundImpl(precision, true)
    }

    protected fun countLeadingZeroBits(): ULong {
        for (i in ints.indices.reversed()) {
            if (ints[i] != 0u) {
                return (((ints.size - 1 - i).toULong() shl 5) + ints[i].countLeadingZeroBits().toULong())
            }
        }
        return ints.size.toULong() shl 5
    }

    private val size: ULong
        get() = ints.size.toULong()

    override fun unaryMinus(): T {
        // Note that flipping the sign works as the "special" bits are opposite to the sign bit
        val newInts = ints.copyOf()
        newInts[0] = newInts[0] xor SIGN_BIT.toUInt()
        return companion.newInstance(newInts, exponent)
    }

    override fun unaryPlus(): T = self

    override fun getDecimalDigit(pos:Int): Char {
        val d1000Pos = D1000StoredPos((pos-exponent).floorDiv(3))
        // check correct for small positive exponents
        val inDigitCorrection = (pos - exponent).mod(3)
        if (d1000Pos.intPos !in ints.indices) return '0'

        val r = when (inDigitCorrection) {
            0 -> getStoredDigit(d1000Pos) % 10u
            1 -> (getStoredDigit(d1000Pos) / 10u) % 10u
            else -> (getStoredDigit(d1000Pos) / 100u)
        }
        return Char((r.toInt() % 10) + '0'.code)
    }

    open operator fun div(divider: AbstractBigDecimal<*>): T {
        val factor = divider.precisionDigits.coerceAtLeast(9)
        val scaled = exp10(factor).divRem(divider).quotient.asT()
        return scaled.exp10(-factor)
    }


    /**
     * @param elems The base elements for the integer
     * @params exp The exponent of the base elements. This may not be the final value
     */
    protected open fun createOptimizedInstance(elems: UIntArray, exp: Int): T {
        val startIndex = 0 // elems.indexOfFirst { it != 0u }
        val newExp = exp + startIndex * 9
        // must be zero
        if (startIndex < 0) return companion.newInstance(uintArrayOf(0u), newExp)
        val endIndex = elems.indexOfLast { it != 0u }.coerceAtLeast(startIndex) + 1
        if (endIndex - startIndex == elems.size) return companion.newInstance(elems, exp)

        val newElems = elems.copyOfRange(startIndex, endIndex)
        return companion.newInstance(newElems, newExp)
    }

    override fun plus(other: XsdDecimal): T = plus(other.asT())

    open operator fun plus(other: AbstractBigDecimal<*>): T {
        val lSign = sign
        val rSign = other.sign
        when {
            lSign == 0 -> return other.asT()

            rSign == 0 -> return self

            lSign > 0 && rSign < 0 -> return minus(other.abs())

            lSign < 0 && rSign > 0 -> return other.asT().minus(abs())
        }

        val newExp: Int = minOf(exponent, other.exponent)//.floorDiv(3)) * 3

        val maxDecimalDigitCount = maxOf(precisionDigits + exponent - newExp, other.precisionDigits + other.exponent - newExp)
        // add 1 to allow for addition overflow
        // add 2 to get ceilDiv functionality
        val maxDigitCount = (1 + 2 + maxDecimalDigitCount) / 3

        // allocate extra int for overflow
        val resultInts = UIntArray((2 + maxDigitCount) / 3)

        var carry = 0u

        for (i in resultInts.indices) {
            var tmpTotal = 0u
            val posBase = D10Pos(i * 9 + newExp)
            for (j in 0..2) {
                val digitPos = posBase + D10Pos(j * 3)
                val left = pseudoDigitFromZeroDec(digitPos)
                val right = other.pseudoDigitFromZeroDec(digitPos)
                val a = left + right + carry
                tmpTotal += a.rem(MAX_DIGIT.toUInt()) shl (10 * j)
                carry = a / MAX_DIGIT.toUInt()
            }
            resultInts[i] = tmpTotal
        }

        if (lSign < 0) resultInts[0] = resultInts[0] or SIGN_BIT.toUInt()

        return createOptimizedInstance(resultInts, newExp)
    }

    override fun minus(other: XsdDecimal): T = minus(other.toBigDecimal())

    open operator fun minus(other: AbstractBigDecimal<*>): T {
        val s = sign
        val os = other.sign
        when {
            s == 0 -> return -other.asT()

            os == 0 -> return self

            s < 0 && os > 0 -> return (abs() + other).unaryMinus()
            s > 0 && os < 0 -> return (abs() + other.abs())

        }
        val cmp = compareTo(other)
        when {
            cmp == 0 -> return companion.newInstance(uintArrayOf(0u), exponent)
            cmp  < 0 -> return other.minus(self).unaryMinus().asT()
        }


        val newExp: Int = (minOf(exponent, other.exponent) / 3) * 3

        val maxDecimalDigitCount = maxOf(ints.size *9 + exponent - newExp, other.ints.size *9 + other.exponent - newExp)
        val maxDigitCount = 1 + (2+maxDecimalDigitCount) / 3 // up to 3 decimal digits per "digit"

        // allocate extra int for overflow
        val resultInts = UIntArray((2 + maxDigitCount) / 3)

        var borrow = 0u

        for (i in resultInts.indices) {
            var tmpTotal = 0u
            val posBase = D1000Pos(i * 3 + newExp / 3)
            for (j in 0..2) {
                val digitPos = posBase + j
                val l = pseudoDigitFromZero(digitPos)
                val r = other.pseudoDigitFromZero(digitPos) + borrow
                val a = if (l < r) {
                    borrow = 1u
                    l + MAX_DIGIT.toUInt() - r
                } else {
                    borrow = 0u
                    l - r
                }
                tmpTotal += a shl (BITS_PER_DIGIT * j)
            }
            resultInts[i] = tmpTotal
        }

        val ints = when {
            resultInts[resultInts.lastIndex] == 0u -> resultInts.copyOf(resultInts.size - 1)
            else -> resultInts
        }

        return companion.newInstance(ints, newExp)

    }

    override fun times(other: XsdDecimal): T = times(other.asT())

    open operator fun times(other: AbstractBigDecimal<*>): T {
        @Suppress("UNCHECKED_CAST")
        if (other.precisionDigits > precisionDigits) return other.asT().times(self)
        val newSign = sign * other.sign
        val newExponent = exponent + other.exponent
        if (newSign == 0) return companion.newInstance(uintArrayOf(0u), newExponent)

        val intsNeeded = (precisionDigits + other.precisionDigits +8)/9
        val newInts = UIntArray(intsNeeded)

        val lDigits = D10Pos(precisionDigits).toStoredD1000Size()
        val rDigits = D10Pos(other.precisionDigits).toStoredD1000Size()

        for (rPosRaw in 0..<rDigits.p) {
            val rPos = D1000StoredPos(rPosRaw)
            val right = other.getStoredDigit(rPos)
            if (right == 0u) continue // no need to attempt to multiply
            var carry = 0u
            for (lPosRaw in 0..< lDigits.p) {
                val lPos = D1000StoredPos(lPosRaw)
                newInts.updateDigit(rPos + lPos) { cur ->
                    val mult = getStoredDigit(lPos) * right + carry + cur
                    carry = mult / 1000u

                    mult % 1000u
                }
            }
            for (i in lDigits.p..< (lDigits + rDigits).p) {
                if (carry == 0u) break // no carry, we are done
                val targetPos = rPos + i
                newInts.updateDigit(targetPos) { cur ->
                    val sum = carry + cur
                    carry = sum / 1000u

                    sum % 1000u
                }
            }
        }

        if (newSign< 0) newInts[0] = newInts[0] or SIGN_BIT.toUInt()

        return createOptimizedInstance(newInts, newExponent)
    }

    override operator fun times(multiplier: UInt): T {
        return times(companion.invoke(multiplier.toLong()))
    }

    infix fun shl(shift: Int): T {
        require (shift >=0) { "Shift must be non-negative" }
        return shl(shift.toULong())
    }

    infix fun shl(shift: ULong): T {
        if (shift == 0uL || !isFinite) return self
        val newInts = BigUnsignedInt(ints, shift).expandExp().ints
        return companion.newInstance(newInts, exponent)
    }

    /**
     * Get the absolute value of this value.
     */
    override fun abs(): T {
        when (ints[0] and SIGN_BIT.toUInt()) {
            0u -> return self
            else -> {
                val newInts = ints.copyOf()
                newInts[0] = newInts[0] and 0x7FFF_FFFFu
                return companion.newInstance(newInts, exponent)
            }
        }
    }

    override fun compareTo(other: XsdDecimal): Int {

        val lsign = sign
        val rsign = other.sign
        when {
            !other.isFinite -> when {
                other.isNaN -> throw ArithmeticException("NaN values are not comparable")
                isNegativeInfinity -> return 1 // we are always greater than negative infinity
                else -> return -1 // we are always smaller than positive infinity
            }

            lsign < rsign -> return -1
            lsign > rsign -> return 1
        }
        if (other !is AbstractBigDecimal<*>) return compareTo(other.toBigDecimal())

        val td = exponent + precisionDigits
        val otd = other.exponent + other.precisionDigits
        when {
            td < otd -> return -1
            td > otd -> return 1
        }

        // uses -1 as this give the highest offset
        val maxDigit = D1000Pos((maxOf(td, otd) - 1).floorDiv(3))
        val minDigit = D1000Pos(minOf(exponent, other.exponent).floorDiv(3))

        for (rawI in maxDigit.p downTo minDigit.p) {
            val i = D1000Pos(rawI)
            val c = pseudoDigitFromZero(i).compareTo(other.pseudoDigitFromZero(i))
            if (c != 0) return c
        }
        return pseudoDigitFromZero(minDigit).compareTo(other.pseudoDigitFromZero(minDigit))
    }

    override val xmlString: String get() = buildString { appendTo(this) }

    internal open fun appendTo(appendable: Appendable) {
        val firstInt = ints[0]
        if (firstInt shr 31 != 0u) appendable.append('-')
        if (ints.size == 1 && (firstInt and 0x3fff_ffffu ==0u)) {
            appendable.append('0')
            return
        }

        val intDigits = intDigitSize
        var seenNonZero = false
        var digitsSeen = 0
        for (d9pack in ints.reversed()) {
            for (j in 8 downTo 0) {
                if (digitsSeen == intDigits) {
                    if (! seenNonZero) appendable.append('0') // leading zero
                    appendable.append('.')
                }
                val d3pack = (d9pack shr (BITS_PER_DIGIT * (j/3))) and 0x3ffu
                val d = when (j % 3) {
                    0 -> d3pack % 10u
                    1 -> (d3pack / 10u) % 10u
                    else -> (d3pack / 100u) % 10u
                }

                if (d != 0u || seenNonZero || digitsSeen >= intDigits) {
                    seenNonZero = true
                    appendable.appendCodepoint(d.toInt() + '0'.code)
                }
                digitsSeen += 1
            }
        }
        if (digitsSeen < intDigits) {
            for (i in digitsSeen ..<intDigits) {
                appendable.append('0')
            }
        }
    }

    override fun divRem(divider: XsdDecimal): DivRem<T> {
        return divRem(divider.asT())
    }

    override fun divRem(divider: ULong): DivRem<T> = when (divider) {
        0uL -> throw ArithmeticException("Division by zero")
        1uL -> return DivRem(self, companion.ZERO)
        else -> divRem(companion.invoke(divider))
    }

    override fun divRem(divider: UInt): DivRem<T> {
        return divRem(BigDecimal(divider))
    }

    fun divRem(other: XsdUnsignedInt): DivRem<T> = divRem(other.uIntValue)
    fun divRem(other: XsdUnsignedLong): DivRem<T> = divRem(other.uLongValue)

    /**
     * Perform an in place multiplySubtract modifying [target]
     * @param target The array to perform the multiplySubtract on
     * @param a The array to multiply. For division this is the divider
     * @param multiplier The single token multiplier. The one base1000 digit of the divider
     * @param leftOffset The offset of the first (least significant) base1000 digit in the target array.
     * @return If a "borrow" was needed
     */
    private fun multiplySubtractInPlace(
        target: UIntArray,
        a: AbstractBigDecimal<*>,
        multiplier: UInt,
        leftOffset: D1000StoredPos
    ): Boolean {
        if (multiplier == 0u) return false
        var borrow = 0u
        val aDigits = D10Pos(a.precisionDigits)
        for (rawI in 0 until aDigits.toD1000Size().p) {
            val i = D1000StoredPos(rawI)
            val partMult = a.getStoredDigit(i) * multiplier + borrow
            borrow = partMult / 1000u
            val toSubtract = partMult % 1000u

            target.updateDigit(i + leftOffset) { cur ->
                when {
                    cur < toSubtract -> {
                        borrow += 1u
                        1000u + cur - toSubtract
                    }
                    else -> (cur - toSubtract)
                }
            }
        }


        if (borrow == 0u) return false

        val tPos = aDigits.toStoredD1000Size() + leftOffset
        val t = target.getStoredDigitOrZero(tPos)
        if (t >= borrow) {
            target.setStoredDigit(tPos, t - borrow)
            return false
        }
        // Else Step D6 - Add a again
        borrow = 0u
        for(iRaw in 0 until aDigits.toStoredD1000Size().p) {
            val i = D1000StoredPos(iRaw)
            target.updateDigit(i+leftOffset) {
                val add = it + a.getStoredDigit(i)
                borrow = add / MAX_DIGIT.toUInt()
                add.mod(MAX_DIGIT.toUInt())
            }
        }
        target.setStoredDigit(D1000StoredPos(a.ints.size / 3) + leftOffset, (t + borrow - borrow) and 0x3ffu)
        return true
    }

    private fun splitAtExponent(newExponent: Int): DivRem<T> {
        if (newExponent <= exponent) return DivRem(self, companion.ZERO)
        val extraExp = D10Pos(newExponent - exponent)

        val intsInRem = extraExp.intSize.coerceAtMost(ints.size)
        val remInts = ints.copyOf(intsInRem)
        when (val pseudoDigit = extraExp.p %3) {
            1 -> remInts.updateDigit(extraExp.toStoredD1000Pos()) { it % 10u }
            2 -> remInts.updateDigit(extraExp.toStoredD1000Pos()) { it % 100u }
        }
        val digitsInLast = D1000Pos(((extraExp.p +2) / 3))
        if (digitsInLast.shift != 0) {
            val mask = 1u.shl(digitsInLast.shift) - 1u
            remInts[remInts.lastIndex] = remInts[remInts.lastIndex] and mask
        }
//        if (digitsInLast.p <=3) remInts.setStoredDigit(D1000StoredPos(intsInRem/3) + 1, 0u)

        val qInts = UIntArray(D10Pos(precisionDigits - newExponent).intSize)
        for (i in qInts.indices) {
            var tmp = 0u
            for (j in 0..2) {
                val pos = D10Pos(i * 9 + j*3 + newExponent)
                val d = pseudoDigitFromZeroDec(pos)
                tmp += d.shl(BITS_PER_DIGIT * j)
            }
            qInts[i] = tmp
        }
        if (ints[0] and SIGN_BIT.toUInt() != 0u) {
            qInts[0] = qInts[0] or SIGN_BIT.toUInt()
        }
        val d = createOptimizedInstance(qInts, newExponent)
        val r = createOptimizedInstance(remInts, exponent)
        return DivRem(d, r)
    }

    private fun withNewExp(newExp: Int): T {
        if (newExp == exponent || isZero) return self

        val newDecDigitCount = D10Pos(precisionDigits - newExp + exponent)

        val newInts = UIntArray(newDecDigitCount.intSize/*.coerceAtLeast(1)*/)
        for (i in newInts.indices) {
            var tmp = 0u
            for (j in 0..2) {
                val pos = D10Pos(i * 9 + j * 3 + newExp)
                val d = pseudoDigitFromZeroDec(pos)
                tmp += d.shl(BITS_PER_DIGIT * j)
            }
            newInts[i] = tmp
        }
        if (ints[0] and SIGN_BIT.toUInt() != 0u) {
            newInts[0] = newInts[0] or SIGN_BIT.toUInt()
        }
        return companion.newInstance(newInts, newExp)
    }


    open fun divRem(divider: T): DivRem<T> { // will (initially) expand exponents
        val dSign = divider.sign
        val lSign = sign
        when {
            dSign == 0 -> throw ArithmeticException("Division by zero")
            lSign == 0 -> return DivRem(self, companion.ZERO)

            // the divident has fewer digits than the divider (is thus smaller)
            precisionDigits + exponent < divider.precisionDigits + divider.exponent -> {
                // We are certain the divider is bigger than the dividend so we will have 0 quotient and
                // dividend as remainder.
                return DivRem(companion.ZERO, self)
            }
        }

        val dividerSize_n = D10Pos(divider.precisionDigits).toStoredD1000Size()
        val divMSD = divider.getStoredDigit(dividerSize_n - 1) // we use this to determine the approximate quotient
        if (divMSD < 100u) {
            val expCorrect = when {
                divMSD < 10u -> -2
                else -> -1
            }
            if (exponent < divider.exponent) {
                val (newDivident, remPart) = splitAtExponent(divider.exponent+expCorrect)

                val (q, r) = newDivident
                    .unsafeDivRemImpl(divider.withNewExp(divider.exponent + expCorrect), dSign = dSign)
                val newQ = companion.newInstance(q.ints, divider.exponent)
                return DivRem(newQ, remPart + r)
            }

            val (q, r) = withNewExp(divider.exponent + expCorrect)
                .unsafeDivRemImpl(divider.withNewExp(divider.exponent + expCorrect), lSign, dSign)
            val newQ = companion.newInstance(q.ints, divider.exponent)

            val newR = r.withNewExp(r.exponent - expCorrect)

            return DivRem(newQ, newR)
        }

        // Deal with the divident having a smaller exponent than the divider by
        // equalizing both and then dividing it
        if (exponent < divider.exponent) {
            val (newDivident, remPart) = splitAtExponent(divider.exponent)
            val actualDivRem = newDivident.unsafeDivRemImpl(divider, lSign, dSign)
            return DivRem(actualDivRem.quotient, remPart + actualDivRem.remainder)
        }


        return unsafeDivRemImpl(divider, lSign, dSign)
    }

    /**
     * Implementation of divRem that assumes that the msd for the divider is large (3 decimal digits)
     * and that the exponent is larger or equal to the divider exponent.
     */
    private fun unsafeDivRemImpl(
        divider: T,
        lSign: Int = sign,
        dSign: Int = divider.sign,
    ): DivRem<T> {
        val dividerSize_n = D10Pos(divider.precisionDigits).toStoredD1000Size()
        val divMSD = divider.getStoredDigit(dividerSize_n - 1)
        val divMSD2 = divider.ints.getStoredDigitOrZero(dividerSize_n - 2)
        // we use this to determine the approximate quotient


        val maxQuotientSize = D10Pos((precisionDigits - divider.precisionDigits) + (exponent - divider.exponent) + 1)

        val mutableDivident = withNewExp(divider.exponent).ints
        val leftSize = D10Pos(precisionDigits+(exponent - divider.exponent)).toStoredD1000Size()

        val growth_m = leftSize - dividerSize_n

        val quotient = UIntArray(maxQuotientSize.intSize) // note this can be negative if there

        run {//
            val divident = mutableDivident.getStoredDigit(leftSize-1)
            var qX: UInt = divident / divMSD // never too big
            var rX: UInt = divident - (qX * divMSD) // approximate remainder

            while (rX < 0x1000u && qX * divMSD2 > (rX * 1000u) + mutableDivident.getStoredDigitOrZero(leftSize - 2)) {
                qX -= 1u
                rX += 1u
            }

            if (multiplySubtractInPlace(mutableDivident, divider, qX, growth_m)) {
                qX -= 1u
            }
            quotient.setStoredDigit(maxQuotientSize.toStoredD1000Pos(), qX)
        }

        for (jRaw in (growth_m - 1).p downTo 0) {
            val j = D1000StoredPos(jRaw)
            val divident = (mutableDivident.getStoredDigit(dividerSize_n + j) * MAX_DIGIT.toUInt() +
                    mutableDivident.getStoredDigit(dividerSize_n + j - 1))

            var qX: UInt = divident / divMSD
            var rX: UInt = divident - (qX * divMSD) // approximate remainder

            do {
                if (qX * divMSD2 > (rX *1000u) + mutableDivident.getStoredDigitOrZero(dividerSize_n + j - 2)) {
                    qX -= 1u
                    rX += divMSD
                } else {
                    break
                }
            } while (rX < 0x1_0000_0000uL)

            if (multiplySubtractInPlace(mutableDivident, divider, qX, j)) {
                qX -= 1u
            }
            quotient.setStoredDigit(j, qX)
        }

        val remainderSize = mutableDivident.indexOfLast { it != 0u }.coerceAtLeast(0) + 1

        val remainder = companion.newInstance(mutableDivident.copyOfRange(0, remainderSize), exponent)

        if (lSign < 0) mutableDivident[0] = mutableDivident[0] or SIGN_BIT.toUInt()
        val quotientSize = quotient.indexOfLast { it != 0u }.coerceAtLeast(0) + 1


        when {
            quotientSize == 1 && quotient[0] == 0u -> return DivRem(companion.ZERO, remainder)
            lSign != dSign -> quotient[0] = quotient[0] or SIGN_BIT.toUInt()
        }

        val quotientX = companion.newInstance(quotient.copyOf(quotientSize), 0)

        return DivRem(quotientX, remainder)
    }


    override fun equals(other: Any?): Boolean {
        return when {
            other !is XsdDecimal -> false

            this.isFinite -> other.isFinite && compareTo(other) == 0

            other.isFinite -> false

            else -> when (ints[0] shr 28) {
                0b0101u -> other.isInfinity
                0b1101u -> other.isNegativeInfinity
                else -> false // NaN is never equal to NaN
            }
        }

    }

    override fun hashCode(): Int {
        var result = exponent.hashCode()
        var i = ints.lastIndex
        while (i >= 0 && ints[i] == 0u) {
            i -= 1
        }
        for (j in 0..i) {
            result = 31 * result + ints[j].hashCode()
        }
        return result
    }


    override fun toString(): String = buildString {
        append("BigDecimal(")
        if (sign < 0) append('-')
        for (i in ints.reversed()) {
            append(((i shr 20) and DIGIT_MASK.toUInt()).toString().padStart(3, '0')).append('_')
            append(((i shr 10) and DIGIT_MASK.toUInt()).toString().padStart(3, '0')).append('_')
            append((i and DIGIT_MASK.toUInt()).toString().padStart(3, '0')).append('_')
        }
        if (isNotEmpty()) deleteAt(lastIndex)

        if (exponent != 0) {
            append(" e").append(exponent)
        }
        append(')')
    }

    interface IDivRem<out T: AbstractBigDecimal<out T>, out R> {
        val quotient: T
        val remainder: R
    }

    data class DivRem<out T: AbstractBigDecimal<out T>>(
        override val quotient: T,
        override val remainder: T
    ) : IDivRem<T, T>, XsdBigDecimal.DivRem

    data class UIntDivRem<out T: AbstractBigDecimal<out T>>(
        override val quotient: T,
        override val remainder: UInt
    ) : IDivRem<T, UInt>

    private class RepeatSequence(val char: Char, override val length: Int) : CharSequence {
        override fun get(index: Int): Char = char

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
            RepeatSequence(char, endIndex - startIndex)
    }

    internal class ParseResult(val ints: UIntArray, val decimalDigits: Int)

    abstract class CompanionBase<T: AbstractBigDecimal<T>> {

        abstract val ZERO: T
        abstract val NEGZERO: T
        abstract val ONE: T
        abstract val MINUSONE: T

        internal abstract fun newInstance(ints: UIntArray, exponent: Int): T

        internal fun toBigDecimal(bigInt: AbstractBigInteger<*>): ParseResult {
            if (bigInt.sign == 0) return ParseResult(uintArrayOf(0u), 0)
            // Normalize to a value that has an exponent 0
            var current = bigInt.expandExp().abs()

            val bits = current.significantBitsFromZero()
            // Fairly conservative estimage of the ints needed.
            val uIntArray = UIntArray(bits.toInt() / 29 )

            var index = 0
            while (current.sign !=0) {
                val dr = current.divRem(1_000_000_000u)
                val int = valToUInt(dr.intRemainder.toUInt()).toUInt()
                uIntArray[index++] = int
                current = dr.quotient
            }

            val neededLen = uIntArray.indexOfLast { it != 0u }.coerceAtLeast(0) + 1
            val shortInts = uIntArray.copyOf(neededLen)

            if (bigInt.sign < 0) shortInts[0] = shortInts[0] or SIGN_BIT.toUInt()

            return ParseResult(shortInts, 0)
        }

        @XmlUtilInternal
        internal fun parse(s: CharSequence): ParseResult {
            if (s.isEmpty()) throw NumberFormatException("Empty string")

            var normalised = s.trim()
            if (normalised.isEmpty()) throw NumberFormatException("Empty string")
            var sign = 1
            when (normalised[0]) {
                '+' -> normalised = normalised.substring(1)
                '-' -> {
                    sign = -1
                    normalised = normalised.substring(1)
                }
            }
            return parseNormalised(normalised, sign)
        }

        internal open fun parseNormalised(
            normalised: CharSequence,
            sign: Int
        ): ParseResult {
            val digitsOnly: CharSequence
            val decimalDigits: Int
            val decPos = normalised.lastIndexOf('.')
            if (decPos >= 0) {
                if (normalised.lastIndexOf('.', decPos - 1) >= 0) {
                    throw NumberFormatException("Multiple decimal points")
                }
                decimalDigits = (normalised.length - decPos - 1)
                digitsOnly = normalised.substring(0, decPos) + normalised.substring(decPos + 1)
            } else {
                digitsOnly = normalised
                decimalDigits = 0
            }

            val intsNeeded = (digitsOnly.length + 8) / 9
            val result = UIntArray(intsNeeded)

            var i = digitsOnly.length

            while (i > 0) {
                val d = when (i) {
                    1 -> digitsOnly[0] - '0'
                    2 -> (digitsOnly[0] - '0') * 10 + (digitsOnly[1] - '0')
                    else -> (digitsOnly[i - 3] - '0') * 100 + (digitsOnly[i - 2] - '0') * 10 + (digitsOnly[i - 1] - '0')
                }
                val iPos = digitsOnly.length - i
                val shift = ((iPos % 9)/3) * BITS_PER_DIGIT
                result[iPos / 9] += d.shl(shift).toUInt()
                i -= 3
            }

            if (sign < 0) {
                result[0] = result[0] or SIGN_BIT.toUInt()
            }
            val neededLen = result.indexOfLast { it != 0u }.coerceAtLeast(0) + 1
            val shortInts = result.copyOf(neededLen)
            return ParseResult(shortInts, -decimalDigits)
        }

        protected fun valToUInts(value: UInt): UIntArray {
            val combined = valToUInt(value)
            val uint0 = combined.toUInt()
            return when (val v = combined.shr(32).toUInt()) {
                0u -> uintArrayOf(uint0)
                else -> uintArrayOf(uint0, v)
            }
        }

        private fun valToUInt(value: UInt): ULong {
            var v = value
            val part0 = v % MAX_DIGIT.toUInt()
            v /= MAX_DIGIT.toUInt()
            val part1 = v % MAX_DIGIT.toUInt()
            v /= MAX_DIGIT.toUInt()
            val part2 = v % MAX_DIGIT.toUInt()
            v /= MAX_DIGIT.toUInt()
            val uint0 = part0 + (part1 shl BITS_PER_DIGIT) + (part2 shl (2 * BITS_PER_DIGIT))
            return when (v) {
                0u -> uint0.toULong()
                else -> uint0.toULong() or v.toULong().shl(32)
            }
        }

        protected fun valToUInts(value: ULong): UIntArray {
            var v = value
            val part0 = v % MAX_DIGIT.toULong()
            v /= MAX_DIGIT.toULong()
            val part1 = v % MAX_DIGIT.toUInt()
            v /= MAX_DIGIT.toULong()
            val part2 = v % MAX_DIGIT.toUInt()
            v /= MAX_DIGIT.toULong()
            val part3 = v % MAX_DIGIT.toUInt()
            v /= MAX_DIGIT.toULong()
            val part4 = v % MAX_DIGIT.toUInt()
            v /= MAX_DIGIT.toULong()
            val part5 = v % MAX_DIGIT.toUInt()
            v /= MAX_DIGIT.toULong()
            val uint0 = (part0 + (part1 shl BITS_PER_DIGIT) + (part2 shl (2 * BITS_PER_DIGIT))).toUInt()
            val uint1 = (part3 + (part4 shl BITS_PER_DIGIT) + (part5 shl (2 * BITS_PER_DIGIT))).toUInt()
            return when (v) {
                0uL if uint1 == 0u -> uintArrayOf(uint0)
                0uL -> uintArrayOf(uint0, uint1)
                else -> uintArrayOf(uint0, uint1, v.toUInt())
            }
        }

        operator fun invoke(value: XsdInteger): T {
            when (value) { // shortcut simple ints
                is XsdInt -> return invoke(value.intValue, 0)
                is XsdLong -> return invoke(value.longValue, 0)
                is XsdUnsignedInt -> return invoke(value.uIntValue, 0)
                is XsdUnsignedLong -> return invoke(value.uLongValue, 0)
            }
            // 30 bits is a bit more than can be stored in new ints. So this array is never too small
            // and fairly accurate
            val resultInts = UIntArray(((value.significantBitsFromZero()+29u)/30u).toInt().coerceAtLeast(1))
            var r = value.divRem(1_000_000_000u)
            // we can just take the first int as the remainder means we never need the follow up int
            resultInts[0] = valToUInts(r.intRemainder.toUInt())[0]

            var i = 1
            while (r.quotient.sign != 0) {
                r = r.quotient.divRem(1_000_000_000u)
                resultInts[i++] = valToUInts(r.intRemainder.toUInt())[0]
            }

            val ints = when (resultInts.size) {
                i -> resultInts
                else -> resultInts.copyOf(i)
            }
            return newInstance(ints, 0)
        }

        operator fun invoke(value: Int, exponent: Int = 0): T = when {
            value < 0 -> {
                val uints = valToUInts((-value).toUInt())
                uints[0] = uints[0] or SIGN_BIT.toUInt()
                newInstance(uints, exponent)
            }

            value == 0 -> ZERO

            else -> newInstance(valToUInts(value.toUInt()), exponent)
        }

        operator fun invoke(value: UInt, exponent: Int = 0): T = when {
            value == 0u -> ZERO
            else -> newInstance(valToUInts(value), exponent)
        }

        operator fun invoke(value: Long, decimalPositions: Int = 0): T = when {
            value < 0 -> {
                val uints = valToUInts((-value).toULong())
                uints[0] = uints[0] or SIGN_BIT.toUInt()
                newInstance(uints, decimalPositions)
            }

            value == 0L -> ZERO

            else -> newInstance(valToUInts(value.toULong()), decimalPositions)
        }

        operator fun invoke(value: ULong, decimalPositions: Int = 0): T = when {
            value == 0uL -> ZERO
            else -> newInstance(valToUInts(value), decimalPositions)
        }

        private fun floor_log2(n: UInt): Int {
            return 31-n.countLeadingZeroBits()
        }

        private fun floor_log2(n: ULong): Int {
            return 63-n.countLeadingZeroBits()
        }

        /**
         * Algorithm based on dragonbox: https://github.com/jk-jeon/dragonbox/blob/master/other_files/Dragonbox.pdf
         */
        internal open fun convertToDecimal(float: Float): ParseResult {
            requireRange(float.isFinite()) { "Not a finite float" }
            if (float == 0.0f) return when {
                float.toRawBits().ushr(31) != 0 -> ParseResult(NEGZERO.ints, 0)
                else -> ParseResult(ZERO.ints, 0)
            }

            val d = FloatToDecimalConverter.to_decimal(float)
            val ints = valToUInts(d.significand)

            if (d.is_negative) ints[0] = ints[0] or SIGN_BIT.toUInt()

            return ParseResult(ints, d.exponent)
        }

        /**
         * Algorithm based on dragonbox: https://github.com/jk-jeon/dragonbox/blob/master/other_files/Dragonbox.pdf
         */
        internal open fun convertToDecimal(double: Double): ParseResult {
            if (double == 0.0) return when {
                double.toRawBits().ushr(63) != 0L -> ParseResult(NEGZERO.ints, 0)
                else -> ParseResult(ZERO.ints, 0)
            }
            val d = toDecimal(double)
            val ints = valToUInts(d.significand)

            if (d.is_negative) ints[0] = ints[0] or SIGN_BIT.toUInt()

            return ParseResult(ints, d.exponent)
        }

    }

    @JvmInline
    value class D1000Pos(val p: Int) {
        val intSize get() = (p + 2) / 3
        val intPos get() = p.floorDiv(3)
        val shift get() = p.mod(3) * BITS_PER_DIGIT

        operator fun plus(other: Int): D1000Pos = D1000Pos(p + other)
        operator fun plus(other: D1000Pos): D1000Pos = D1000Pos(p + other.p)
        operator fun minus(other: Int): D1000Pos = D1000Pos(p - other)
    }

    @JvmInline
    value class D1000StoredPos(val p: Int) {
        val intSize get() = (p + 2) / 3
        val intPos get() = p.floorDiv(3)
        val shift get() = p.mod(3) * BITS_PER_DIGIT

        operator fun plus(other: Int): D1000StoredPos = D1000StoredPos(p + other)

        operator fun plus(other: D1000StoredPos): D1000StoredPos = D1000StoredPos(p + other.p)
        operator fun minus(other: Int): D1000StoredPos = D1000StoredPos(p - other)
        operator fun minus(other: D1000StoredPos): D1000StoredPos = D1000StoredPos(p - other.p)
    }

    @JvmInline
    value class D10Pos(val p: Int) {
        val intSize: Int get() = (p + 8) / 9
        val intPos get() = p.floorDiv(9)

        fun toD1000Size() = D1000Pos((p + 2) / 3)
        fun toStoredD1000Size() = D1000StoredPos((p + 2) / 3)

        fun toD1000Pos() = D1000Pos(p / 3)
        fun toStoredD1000Pos() = D1000StoredPos(p / 3)

        operator fun plus(other: D10Pos): D10Pos = D10Pos(p + other.p)
        operator fun plus(other: Int): D10Pos = D10Pos(p + other)
        operator fun minus(other: D10Pos): D10Pos = D10Pos(p - other.p)
        operator fun minus(other: Int): D10Pos = D10Pos(p - other)
    }

    companion object {
        private const val MAX_DIGIT= 1_000
        private const val MAX_DIGIT2 = MAX_DIGIT * MAX_DIGIT
        private const val MAX_DIGIT3 = MAX_DIGIT.toLong() * MAX_DIGIT.toLong() * MAX_DIGIT.toLong()
        private const val MAX_DIGIT4 = MAX_DIGIT.toLong() * MAX_DIGIT.toLong() * MAX_DIGIT.toLong() * MAX_DIGIT.toLong()
        private const val MAX_DIGIT5 = MAX_DIGIT.toLong() * MAX_DIGIT.toLong() * MAX_DIGIT.toLong() * MAX_DIGIT.toLong() * MAX_DIGIT.toLong()
        private const val BITS_PER_DIGIT = 10
        private const val DIGIT_MASK = (1 shl BITS_PER_DIGIT) - 1

        @XmlUtilInternal
        protected const val SIGN_BIT = 1 shl 31
        @XmlUtilInternal
        protected const val SPECIAL_BIT = 1 shl 30
        @XmlUtilInternal
        protected const val NAN_BIT = 1 shl 29
        @XmlUtilInternal
        protected const val INFINITY_BIT = 1 shl 28
        @XmlUtilInternal
        protected const val SPECIAL_MASK = SIGN_BIT or SPECIAL_BIT or NAN_BIT or INFINITY_BIT

    }

}
