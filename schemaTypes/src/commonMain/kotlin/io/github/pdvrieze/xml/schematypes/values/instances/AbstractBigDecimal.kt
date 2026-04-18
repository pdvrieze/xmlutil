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
import nl.adaptivity.xmlutil.core.internal.appendCodepoint

@OptIn(ExperimentalUnsignedTypes::class)
/**
 * @property ints A packed array of sequences of 32-bit integers that contains a sequence of 10-bit
 *           numbers
 *
 * @property exponent the Base 10 exponent
 */
abstract class AbstractBigDecimal<T: AbstractBigDecimal<T>> internal constructor(
    internal val ints: UIntArray,
    internal val exponent: Int
) : XsdBigDecimal {

    /**
     * Determine the amount of decimal digits this number contains.
     */
    val precisionDigits: Int

    init {
        require(ints.size >0) { "There must be at least one int"}
        val lastDigit = ints.last()
        if (ints.size > 1) {
            require(ints.any { it != 0u }) { "Zero sign must have a single int" }
            val valZero = ints[0]
            if (valZero and SPECIAL_BIT.toUInt() != 0u) {
                throw IllegalArgumentException(
                    "Special values must have a single int with the special bit"
                )
            }
            require(lastDigit != 0u) { "Last int must not be zero" }
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
        precisionDigits = (ints.size-1)*9 + offset + when {
            d > 100u -> 3
            d > 10u -> 2
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
                ints.size == 1 && int0 == 0 -> 0

                int0 and (SPECIAL_BIT or NAN_BIT) == (SPECIAL_BIT or NAN_BIT) -> throw ArithmeticException("NaN value")

                else -> int0.shr(31).or(1)
            }
        }

    val intDigitSize: Int
        get() = (ints.size * 9) + exponent






    override val isFinite: Boolean
        get() = ints[0].toInt() and SPECIAL_BIT == 0

    abstract val self: T

    override fun exp10(n: Int): T {
        val newDecimalPosition = exponent - n
        return newInstance(ints, newDecimalPosition)
    }

    override val isInteger: Boolean
        get() = exponent <= 0

    abstract protected val companion: CompanionBase<T>

    private fun newInstance(ints: UIntArray, exponent: Int): T =
        companion.newInstance(ints, exponent)

    private fun newInstance(value: Int, decimalPositions: Int = 0): T =
        companion.invoke(value, decimalPositions)

    private fun newInstance(value: Long, decimalPositions: Int = 0): T =
        companion.invoke(value, decimalPositions)

    abstract protected fun XsdDecimal.asT(): T

    /**
     * Retrieve the "digit" stored at the given position
     */
    private fun getStoredDigit(pos:Int): UInt {
        val intPos = pos / 3
        val rightShift = (pos % 3) * BITS_PER_DIGIT

        return ints[intPos].shr(rightShift).and(DIGIT_MASK.toUInt())
    }

    /**
     * Retrieve digits as they would be if base 0
     */
    private fun pseudoDigitFromZero(pos: Int): UInt {
        // check correct for small positive exponents
        val expCorrection = (-exponent).floorDiv(3)
        val inDigitCorrection = (-exponent).mod(3)
        val newPos = (pos + expCorrection)
        if (newPos !in -1..<(ints.size * 3)) return 0u
        return when (inDigitCorrection) {
            0 -> when {
                newPos >= 0 -> getStoredDigit(newPos)
                else -> 0u
            }
            1 -> {
                val leastSig = if (newPos < 0) 0u else getStoredDigit(newPos) / 10u
                val mostSig = if ((newPos + 1) / 3 >= ints.size) 0u else (getStoredDigit(newPos + 1) * 100u).mod(MAX_DIGIT.toUInt())
                leastSig + mostSig
            }
            else -> { // 2
                val leastSig = if (newPos < 0) 0u else getStoredDigit(newPos) / 100u
                val mostSig = if ((newPos + 1)/3 >= ints.size) 0u else (getStoredDigit(newPos + 1) * 10u).mod(MAX_DIGIT.toUInt())
                leastSig + mostSig
            }
        }
    }


    private fun toULongHelper(maxDigitCount: Int): ULong {
        var r = 0uL
        var mult = 1uL
        for (i in 0..5) {
            val digit = pseudoDigitFromZero(i)
            r += digit * mult
            mult *= 1000uL
        }
        val lastDigit = pseudoDigitFromZero(6)
        requireRange(lastDigit <=18u) {"Number out of range of ULong"}
        if (lastDigit == 18u) {
            requireRange(r <= 446_744_073_709_551_615uL) {"Number out of range of ULong"}
        }
        r += lastDigit * mult

        return r
    }

    private fun toULongHelper2(maxDigitCount: Int): ULong {
        if (exponent > 19) throw RangeException("Value too large to fit in an unsigned long")

        val int0 = ints[0].toInt()
        if (int0 and SPECIAL_BIT != 0) throw IllegalStateException("Non-finite value cannot be converted to integer")
        if (int0 == 0) return 0uL

        var currentValue: ULong = 0u
        var digitPos = ((-exponent).floorDiv(3))

        var correction: ULong
        when ((-exponent).mod(3)) {
            0 -> correction = 1uL

            1 -> { // get one digit from the right (divide by 100)
                if (digitPos>=0) currentValue += getStoredDigit(digitPos) / 10u
                digitPos += 1
                correction = 100uL
            }

            else -> { // get two digits from the right (divide by 10)
                if (digitPos>=0) currentValue += getStoredDigit(digitPos) / 100u
                digitPos += 1
                correction = 10uL
            }
        }

        val end = minOf(ints.size * 3, digitPos + maxDigitCount)

        while (digitPos < end) {
            val storedDigit = getStoredDigit(digitPos)
            when {
                // maxULOng is: 18_446_744_073_709_551_615

                correction > 10_000_000_000_000_000_000uL -> error("Unexpected correction")

                correction == 10_000_000_000_000_000_000uL -> {
                    requireRange(storedDigit < 1u ||
                            (storedDigit == 1u && currentValue<=8_446_744_073_709_551_615uL)) { "Value out of range of ULong" }

                    currentValue += storedDigit * correction
                    break
                }

                correction == 1_000_000_000_000_000_000uL -> {
                    requireRange(storedDigit < 18u ||
                            (storedDigit == 18u && currentValue<=446_744_073_709_551_615uL)) { "Value out of range of ULong" }

                    currentValue += storedDigit * correction
                    break
                }

                correction == 100_000_000_000_000_000uL -> {
                    requireRange(storedDigit < 184u ||
                            (storedDigit == 184u && currentValue<=46_744_073_709_551_615uL)) { "Value out of range of ULong" }

                    currentValue += storedDigit * correction
                    break
                }
            }
            currentValue += storedDigit * correction
            correction *= MAX_DIGIT.toULong()
            digitPos += 1
        }
        return currentValue
    }


    override fun toULong(): ULong {
        val int0 = ints[0].toInt()
        check(int0 and SIGN_BIT == 0) { "Negative value cannot be converted to unsigned long" }
        return toULongHelper(7)
    }

    override fun toUInt(): UInt {
        val int0 = ints[0].toInt()
        check(int0 and SIGN_BIT == 0) { "Negative value cannot be converted to unsigned int" }
        val r =  toULongHelper(4)

        if (r > UInt.MAX_VALUE) throw RangeException("Value too large to fit in an unsigned int")
        return r.toUInt()
    }


    override fun toLong(): Long {
        val int0 = ints[0].toInt()
        val isNegative = int0 and SIGN_BIT == SIGN_BIT
        val r = toULongHelper(7)

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
        val r = toULongHelper(4)

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
            return newInstance(newInts, 0)
        }

        val intDigits = ((precisionDigits+exponent)+2)/3

        val newInts = UIntArray((intDigits+2)/3)
        for (i in 0 until intDigits step 3) {
            val intIdx = i/3
            newInts[intIdx] = pseudoDigitFromZero(i + 2) * 1_000_000u + pseudoDigitFromZero(i + 1) * 1_000u + pseudoDigitFromZero(i)
        }


        if (sign < 0) {
            val negDigits = (2 - exponent) / 3
            if ((-1..negDigits).any { pseudoDigitFromZero(it)==0u }) {
                val int0 = newInts[0]
                // subtract one
                when {
                    int0.and(0x3ffu) != 0x3e7u -> newInts[0] = int0 + 1u

                    int0.shr(10).and(0x3ffu) < 0x3e7u ->
                        newInts[0] = (int0 and 0x3fff_fC00u) + 1000u

                    int0.shr(20).and(0x3ffu) != 0x3e7u ->
                        newInts[0] = (int0 and 0x3ff0_0000u) + 1_000_000u

                    else -> return (newInstance(newInts, 0) + BigDecimal(1u)).unaryMinus()
                }
            }

            newInts[0] = newInts[0] or SIGN_BIT.toUInt() // copy the sign bit
        }
        return newInstance(newInts, 0)
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
        val lower = getStoredDigit(-1) < 500u




        val dec = round(0)
        check(dec.exponent == 0)
        return when (dec.ints.size) {
            1 -> XsdInt.Companion(dec.toInt())
            2 -> XsdLong.Companion(dec.toLong())
            else -> {
                val unOpt = BigInt(dec.sign, dec.ints, 0uL)
                if (unOpt.countTrailingZeroBits() > 48uL) unOpt.normalize() else unOpt
            }
        }
    }

    private fun roundImpl(precision: Int, halfEven: Boolean): T {
        if (sign == 0 || exponent == precision || !isFinite) return unaryPlus()

        if (exponent < precision) {
            val expanded = expandWithEffectiveDecimalPositionsToArray(precision)
            return newInstance(expanded, 0)
        }
        val divisor = newInstance(uintArrayOf(1u), 0)
            .expandWithEffectiveDecimalPositions(exponent - precision)

        val (quotient, remainder) = divRem(divisor)

        val doubleRemainder = remainder.shl(1uL).abs()

        val cmp = doubleRemainder.compareTo(divisor)
        if (cmp < 0) return quotient
        else if (cmp == 0) {
            if (!halfEven) return quotient

            val isOdd = quotient.ints[0] and 0x1u == 1u
            if (isOdd) {
                if (quotient.ints[0] >= UInt.MAX_VALUE) return quotient.plus(newInstance(1))
                quotient.ints[0] += 1u
                return quotient
            }
        }

        if (quotient[0] < UInt.MAX_VALUE) {
            quotient.ints[0] += 1u
            return quotient
        } else {
            return quotient.plus(newInstance(1))
        }
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
        return newInstance(newInts, exponent)
    }

    override fun unaryPlus(): T = self

    operator fun get(index: ULong): UInt {
        return ints[index.toInt()]
    }

    operator fun get(index: Int): UInt {
        return ints[index]
    }

    open operator fun div(divider: AbstractBigDecimal<*>): T {
        return divRem(divider).quotient.asT()
    }


    /**
     * @param elems The base elements for the integer
     * @params exp The exponent of the base elements. This may not be the final value
     */
    protected open fun createOptimizedInstance(elems: UIntArray, exp: Int): T {
        val startIndex = 0 // elems.indexOfFirst { it != 0u }
        val newExp = exp + startIndex * 9
        // must be zero
        if (startIndex < 0) return newInstance(uintArrayOf(0u), newExp)
        val endIndex = elems.indexOfLast { it != 0u }.coerceAtLeast(startIndex) + 1
        if (endIndex - startIndex == elems.size) return newInstance(elems, exp)

        val newElems = elems.copyOfRange(startIndex, endIndex)
        return newInstance(newElems, newExp)
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

        val newExp: Int = (minOf(exponent, other.exponent) / 3) * 3

        val maxDecimalDigitCount = maxOf(precisionDigits + exponent - newExp, precisionDigits *9 + other.exponent - newExp)
        // add 1 to allow for addition overflow
        // add 2 to get ceilDiv functionality
        val maxDigitCount = (1 + 2 + maxDecimalDigitCount) / 3

        // allocate extra int for overflow
        val resultInts = UIntArray((2 + maxDigitCount) / 3)

        var carry = 0u

        for (i in resultInts.indices) {
            var tmpTotal = 0u
            val posBase = i * 3 + newExp / 3
            for (j in 0..2) {
                val digitPos = posBase + j
                val a = pseudoDigitFromZero(digitPos) + other.pseudoDigitFromZero(digitPos) + carry
                tmpTotal += (a.rem(MAX_DIGIT.toUInt())) shl (BITS_PER_DIGIT * j)
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
            cmp == 0 -> return newInstance(uintArrayOf(0u), exponent)
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
            val posBase = i * 3 + newExp / 3
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

        return newInstance(ints, newExp)

/*


        when {
            exponent < other.exponent ->
                return expandWithEffectiveDecimalPositions(other.exponent).minus(other)

            exponent > other.exponent ->
                return minus(other.expandWithEffectiveDecimalPositions(exponent))

            sign == 0 -> return -other.asT()
            other.sign == 0 -> return self
            sign < 0 && other.sign > 0 -> return (abs() + other)
            sign > 0 && other.sign < 0 -> return (abs() + other.abs())
        }

        val cmp = compareTo(other)
        val larger: UIntArray
        val smaller: UIntArray
        val newSign = when {
            cmp == 0 -> return newInstance(uintArrayOf(0u), exponent)
            cmp < 0 -> { larger = other.ints; smaller = ints; -1 }
            else -> { larger = ints; smaller = other.ints; 1 }
        }

        val newInts = UIntArray(maxOf(larger.size))

        var borrow = 0L
        for (i in smaller.indices.reversed()) {
            val a = larger[i].toLong() - borrow
            val b = smaller[i].toLong()
            if (a >= b) {
                newInts[i] = (a - b).toUInt()
                borrow = 0
            } else {
                val neg = (a + 0x1_0000_0000L - b)
                newInts[i] = neg.toUInt()
                borrow = 1L
            }
        }
        for (i in smaller.size until newInts.size) {
            val a = larger[i].toLong() - borrow
            val b = smaller[i].toLong()
            if (a >= b) {
                newInts[i] = (a - b).toUInt()
                borrow = 0
            } else {
                val neg = (a + 0x1_0000_0000L - b)
                newInts[i] = neg.toUInt()
                borrow = 1L
            }
        }

        return createOptimizedInstance(newInts, exponent)
*/
    }

    override fun times(other: XsdDecimal): T = times(other.asT())

    open operator fun times(other: AbstractBigDecimal<*>): T {
        @Suppress("UNCHECKED_CAST")
        if (other.precisionDigits > precisionDigits) return other.asT().times(self)
        val newSign = sign * other.sign
        val newExponent = exponent + other.exponent
        if (newSign == 0) return newInstance(uintArrayOf(0u), newExponent)

        val intsNeeded = (precisionDigits + other.precisionDigits +8)/9
        val newInts = UIntArray(intsNeeded)

        val lDigits = (precisionDigits + 2) / 3
        val rDigits = (other.precisionDigits + 2) / 3

        for (rPos in 0..<rDigits) {
            val right = other.getStoredDigit(rPos)
            if (right == 0u) continue // no need to attempt to multiply
            var carry = 0u
            for (lPos in 0..< lDigits) {
                val targetPos = rPos + lPos
                val targetIntIdx = targetPos / 3
                val shiftInInt = targetPos.mod(3)*BITS_PER_DIGIT

                val cur = newInts[targetIntIdx].shr(shiftInInt) and 0x3ffu
                val left = getStoredDigit(lPos)
                val mult = left * right + carry + cur
                val base = newInts[targetIntIdx] and (0x3ffu.shl(shiftInInt)).inv()
                newInts[targetIntIdx] = base + mult.mod(1000u).shl(shiftInInt)
                carry = mult /1000u
            }
            for (i in lDigits..< (lDigits + rDigits)) {
                if (carry == 0u) break // no carry, we are done
                val targetPos = rPos + i
                val targetIntIdx = targetPos / 3
                val shiftInInt = targetPos.mod(3)*BITS_PER_DIGIT

                val cur = newInts[targetIntIdx].shr(shiftInInt)
                val sum = carry + cur
                val base = newInts[targetIntIdx] and (0x3ffu.shl(shiftInInt)).inv()
                newInts[targetIntIdx] = base + sum.mod(1000u).shl(shiftInInt)
                carry = sum /1000u
            }
        }

        if (newSign< 0) newInts[0] = newInts[0] or SIGN_BIT.toUInt()

        return createOptimizedInstance(newInts, newExponent)
    }

    override operator fun times(multiplier: UInt): T {
        when {
            multiplier == 0u -> return companion.ZERO
            !isFinite -> return self // multiplication of NaN/INF by a finite positive amount is the same result
        }

        val newInts = UIntArray(size.toInt() + 1)

        var carry = 0u
        for (idx in ints.indices) {
            val m = ints[idx].toULong() * multiplier.toULong() + carry + newInts[idx]
            newInts[idx] = m.toUInt()
            carry = m.shr(32).toUInt()
        }

        val m = carry.toULong() + newInts[ints.size]
        newInts[ints.size] = m.toUInt()
        assert(m.shr(32).toUInt() == 0u)

        return createOptimizedInstance(newInts, exponent)
    }

    infix fun shl(shift: Int): T {
        require (shift >=0) { "Shift must be non-negative" }
        return shl(shift.toULong())
    }

    infix fun shl(shift: ULong): T {
        if (shift == 0uL || !isFinite) return self
        val newInts = BigUnsignedInt(ints, shift).expandExp().ints
        return newInstance(newInts, exponent)
    }

    /**
     * Get the absolute value of this value.
     */
    override fun abs(): T {
        val newInts = ints.copyOf()
        newInts[0] = newInts[0] and 0x7FFFFFFFu
        return newInstance(newInts, exponent)
    }

    /**
     * Helper function to ensure decimal positions.
     * @param newDecimalPosition The new decimal position. If positive the value had decimal digits, if negative it is larger
     */
    private fun expandWithEffectiveDecimalPositions(newDecimalPosition: Int): T = when {
        !isFinite -> self
        else -> newInstance(
            expandWithEffectiveDecimalPositionsToArray(newDecimalPosition),
            newDecimalPosition
        )
    }

    private fun expandWithEffectiveDecimalPositionsToArray(newDecimalPosition: Int): UIntArray {
        if (!isFinite) return ints
        var additionalDecimalNeeded = newDecimalPosition - exponent
        check(additionalDecimalNeeded>=0)
        var current = BigUnsignedInt(ints, 0uL)
        do {
            // Note that the target exp must be 0 as we are only retaining the ints.
            when (additionalDecimalNeeded) {
                0 -> return current.ints
                1 -> return (current.times(10u, 0uL)).ints
                2 -> return (current.times(100u, 0uL)).ints
                3 -> return (current.times(1_000u, 0uL)).ints
                4 -> return (current.times(10_000u, 0uL)).ints
                5 -> return (current.times(100_000u, 0uL)).ints
                6 -> return (current.times(1_000_000u, 0uL)).ints
                7 -> return (current.times(10_000_000u, 0uL)).ints
                8 -> return (current.times(100_000_000u, 0uL)).ints
                9 -> return (current.times(1_000_000_000u, 0uL)).ints

                else -> {
                    current *= 1_000_000_000u
                    additionalDecimalNeeded -= 9
                }
            }
        } while (true)
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
        val maxDigit = (maxOf(td, otd) - 1).floorDiv(3)
        val minDigit = minOf(exponent, other.exponent).floorDiv(3)

        for (i in maxDigit downTo minDigit) {
            val c = pseudoDigitFromZero(i).compareTo(other.pseudoDigitFromZero(i))
            if (c != 0) return c
        }
        return pseudoDigitFromZero(minDigit).compareTo(other.pseudoDigitFromZero(minDigit))
    }

    private fun compareBDInts(other: AbstractBigDecimal<*>): Int {
        val otherInts = when {
            exponent < other.exponent ->
                return expandWithEffectiveDecimalPositions(other.exponent).compareBDInts(other)

            exponent > other.exponent ->
                other.expandWithEffectiveDecimalPositions(exponent).ints

            else -> other.ints
        }

        for (i in ints.indices.reversed()) {
            val v = ints[i]
            val o = otherInts[i]
            when {
                v < o -> return -1
                v > o -> return 1
            }
        }
        return 0
    }

    override val xmlString: String get() = buildString { appendTo(this) }

    internal open fun appendTo(appendable: Appendable) {
        val s = sign
        if (s == 0) {
            appendable.append('0')
            return
        } else if (s < 0) {
            appendable.append('-')
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
        when (divider) {
            0u -> throw ArithmeticException("Division by zero")
            1u if exponent <= 0L -> return DivRem<T>(self, newInstance(companion.ZERO.ints, 0))
            else if (ints.size == 1 && ints[0] == 0u) -> return DivRem(self, companion.ZERO)
        }
        // If we can just extend from UInt to ULong do that here
        val divider = when (exponent) {
            0 -> divider.toULong()
            1 -> divider.toULong() * 10u
            2 -> divider.toULong() * 100u
            3 -> divider.toULong() * 1_000u
            4 -> divider.toULong() * 10_000u
            5 -> divider.toULong() * 100_000u
            6 -> divider.toULong() * 1_000_000u
            7 -> divider.toULong() * 10_000_000u
            8 -> divider.toULong() * 100_000_000u
            9 -> divider.toULong() * 1_000_000_000u
            else -> return divRem(newInstance(uintArrayOf(divider), 0))
        }
        val v = BigUnsignedInt(ints, 0uL)
        val quotient: BigUnsignedInt
        val remainder: UIntArray
        val newRemSign: Int
        when {
            divider <= UInt.MAX_VALUE -> {
                val (q, _, r) = v.divRem(divider.toUInt())
                quotient = q

                newRemSign = if(r == 0u) 0 else sign
                remainder = uintArrayOf(r)
            }
            else -> {
                val (q, r) = v.divRem(divider)
                quotient = q

                newRemSign = if(r.sign == 0) 0 else sign
                remainder = r.ints
            }
        }

        val newSign = if(quotient.sign == 0) 0 else sign

        // We "Fixed" the position so the decimal position difference is 0
        val quotientDec = newInstance(quotient.ints, 0)

        val remainderDec = newInstance(remainder, exponent)

        return DivRem(quotientDec, remainderDec)

    }

    fun divRem(other: XsdUnsignedInt): DivRem<T> = divRem(other.uIntValue)
    fun divRem(other: XsdUnsignedLong): DivRem<T> = divRem(other.uLongValue)

    open fun divRem(divider: T): DivRem<T> { // will (initially) expand exponents
        when {
            divider.sign == 0 -> throw ArithmeticException("Division by zero")
            sign == 0 -> return DivRem(self, companion.ZERO)
        }

        if (divider.sign == 0) throw ArithmeticException("Division by zero")
        else if (sign == 0) return DivRem(self, companion.ZERO)

        val resultPrecision = maxOf(precisionDigits, divider.precisionDigits)


        // use BigUnsignedInts to actually perform the division
        val v = BigUnsignedInt(ints, 0uL)
        val d = BigUnsignedInt(divider.ints, 0uL)
        val unsignedDivRem = v.unsignedDivRem(d)

        val newSign = when {
            unsignedDivRem.quotient.sign == 0 -> 0
            sign == divider.sign -> 1
            else -> -1
        }
        val newDecimalPositions = exponent - divider.exponent
        val quotient = newInstance(unsignedDivRem.quotient.ints, newDecimalPositions)

        val remainderSign = if (unsignedDivRem.remainder.sign == 0) 0 else sign
        val remainder = newInstance(unsignedDivRem.remainder.ints, exponent)

        return DivRem(quotient, remainder)
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
        abstract val ONE: T
        abstract val MINUSONE: T

        internal abstract fun newInstance(ints: UIntArray, exponent: Int): T

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

        open internal fun parseNormalised(
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
            var v = value
            val part0 = v % MAX_DIGIT.toUInt()
            v /= MAX_DIGIT.toUInt()
            val part1 = v % MAX_DIGIT.toUInt()
            v /= MAX_DIGIT.toUInt()
            val part2 = v % MAX_DIGIT.toUInt()
            v /= MAX_DIGIT.toUInt()
            val uint0 = part0 + (part1 shl BITS_PER_DIGIT) + (part2 shl (2 * BITS_PER_DIGIT))
            return when (v) {
                0u -> uintArrayOf(uint0)
                else -> uintArrayOf(uint0, v)
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

        operator fun invoke(value: Int, decimalPositions: Int = 0): T = when {
            value < 0 -> {
                val uints = valToUInts((-value).toUInt())
                uints[0] = uints[0] or SIGN_BIT.toUInt()
                newInstance(uints, decimalPositions)
            }

            value == 0 -> ZERO

            else -> newInstance(valToUInts(value.toUInt()), decimalPositions)
        }

        operator fun invoke(value: UInt, decimalPositions: Int = 0): T = when {
            value == 0u -> ZERO
            else -> newInstance(valToUInts(value), decimalPositions)
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
