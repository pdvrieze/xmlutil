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
import io.github.pdvrieze.xml.schematypes.values.*
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
import kotlin.math.abs
import kotlin.math.absoluteValue

@OptIn(ExperimentalUnsignedTypes::class)
abstract class AbstractBigDecimal<T: AbstractBigDecimal<T>> internal constructor(
    sign: Int,
    internal val ints: UIntArray,
    internal val decimalPositions: Long
) : XsdBigDecimal {

    init {
        when {
            ints.size == 1 && ints[0] == 0u -> require(sign == 0) {
                "Zero value must have a 0 sign"
            }

            sign == 0 -> throw IllegalArgumentException("Zero sign must have a single int")
        }

        require(ints.size != 2 || ints[1] != 0u) { "The second int must not be zero" }
    }

    /*
     * The way the sign stores special values is that (special bits are sign sensitive):
     *  - Bit 32 represents the sign
     *  - Bit 31 represents NaN. If it is equal to the sign bit it is a regular number, otherwise a NaN
     *  - Bit 30 represents infinity (together with the sign). If NaN this value is meaningless
     *  - Bits 2-29 should be equal to the sign bit (although for NaN they could carry information
     *  - Bit 1 represents the positive or zero value
     * A consequence is that normal numbers work:
     * 1 is positive (00000...001)
     * 0 is zero (00000...000)
     * -1 is negative (11111...111)
     */
    internal val _sign = when (sign ushr 29) {
        0 -> {
            require(ints.isNotEmpty()) { "Non-special values must have an int" }
            sign and 1 // positive or zero
        }

        0b111 -> {
            require(ints.isNotEmpty()) { "Non-special values must have an int" }
            -1 // negative
        }

        0b001 -> {
            require(ints.isEmpty()) { "Special values have no ints" }
            INFINITY_BIT
        }

        0b101 -> {
            require(ints.isEmpty()) { "Special values have no ints" }
            -1 xor INFINITY_BIT
        }

        else -> {
            require(ints.size == 0) { "Special values have no ints" }
            sign // Some form of NaN
        }
    }

    override val sign: Int
        get() = when (_sign.ushr(29)) {
            0b000, 0b001 -> _sign and 1
            0b111, 0b110 -> -1
            else -> throw ArithmeticException("NaN value")
        }

    override val isFinite: Boolean get() = ints.isNotEmpty()

    abstract val self: T

    override fun exp10(n: Int): T {
        val newDecimalPosition = decimalPositions - n
        return newInstance(sign, ints, newDecimalPosition)
    }

    override val isInteger: Boolean
        get() = ints.isNotEmpty() && decimalPositions <= 0

    abstract protected val companion: CompanionBase<T>

    private fun newInstance(sign: Int, ints: UIntArray, decimalPositions: Long): T =
        companion.newInstance(sign, ints, decimalPositions)

    private fun newInstance(value: Int, decimalPositions: Long = 0L): T =
        companion.invoke(value, decimalPositions)

    private fun newInstance(value: Long, decimalPositions: Long = 0L): T =
        companion.invoke(value, decimalPositions)

    abstract protected fun XsdDecimal.asT(): T

    override fun toULong(): ULong {
        if (sign == 0) return 0uL
        check(sign > 0) { "Negative value cannot be converted to unsigned long" }
        val base = floor().arrayWithEffectiveDecimalPosition(0)
        return when (base.size) {
            1 -> base[0].toULong()
            2 -> base[0].toULong() or (base[1].toULong() shl 32)
            else -> throw RangeException("Value too large to fit in an unsigned long")
        }
    }

    override fun toUInt(): UInt {
        if (sign == 0) return 0u
        check(sign > 0) { "Negative value cannot be converted to unsigned long" }
        val base = floor().arrayWithEffectiveDecimalPosition(0)
        return when (base.size) {
            1 -> base[0]
            else -> throw RangeException("Value too large to fit in an unsigned int")
        }
    }


    override fun toLong(): Long {
        val base = floor().arrayWithEffectiveDecimalPosition(0)
        return when (base.size) {
            1 -> base[0].toLong() * sign

            2 if (base[1] and 0x8000_0000u == 0u) -> {
                val uLongValue = base[0].toULong() or (base[1].toULong() shl 32)
                uLongValue.toLong() * sign
            }

            else -> throw RangeException("Value too large to fit in a Long")
        }
    }

    override fun toInt(): Int {
        val base = floor().arrayWithEffectiveDecimalPosition(0)
        return when (base.size) {
            1 if (base[0] and 0x8000_0000u == 0u) -> base[0].toInt() * sign
            else -> throw RangeException("Value too large to fit in an Int: ${this.xmlString}")
        }
    }

    override fun toBigDecimal(): T {
        return unaryPlus()
    }

    override fun floor(): T {
        when {
            sign == 0 || decimalPositions == 0L || !isFinite -> return unaryPlus()
            decimalPositions < 0 -> {
                val expanded = expandWithEffectiveDecimalPositionsToArray(0)
                return newInstance(sign, expanded, 0L)
            }
        }

        val divisor = newInstance(1, uintArrayOf(1u), 0)
            .expandWithEffectiveDecimalPositions(decimalPositions)

        val (quotient, _) = divRem(divisor)
        if (sign > 0) return quotient
        else if (quotient[0] < UInt.MAX_VALUE) {
            quotient.ints[0]+=1u
            return quotient
        } else {
            return quotient.plus(newInstance(1))
        }
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
        val dec = round(0)
        check(dec.decimalPositions == 0L)
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
        if (sign == 0 || decimalPositions == precision.toLong() || !isFinite) return unaryPlus()

        if (decimalPositions < precision) {
            val expanded = expandWithEffectiveDecimalPositionsToArray(precision.toLong())
            return newInstance(sign, expanded, 0)
        }
        val divisor = newInstance(1, uintArrayOf(1u), 0)
            .expandWithEffectiveDecimalPositions(decimalPositions - precision.toLong())

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
        return newInstance(-sign, ints, decimalPositions)
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
     * @params exp The exponent of the base elements. This  may not be the final value
     */
    protected open fun createOptimizedInstance(sign: Int, elems: UIntArray, decimalPositions: Long): T {
        var lastIdx = elems.size - 1
        while (lastIdx > 0 && elems[lastIdx] == 0u) lastIdx -= 1

        val newSize = lastIdx + 1
        val newElems  = if (newSize<elems.size) elems.copyOfRange(0, newSize) else elems


        return newInstance(sign, newElems, decimalPositions)
    }

    override fun plus(other: XsdDecimal): T = plus(other.asT())

    open operator fun plus(other: AbstractBigDecimal<*>): T {
        when {
            sign == 0 -> return other.asT()

            other.sign == 0 -> return self

            sign > 0 && other.sign < 0 -> return minus(other.abs())

            sign < 0 && other.sign > 0 -> return other.asT().minus(abs())

            decimalPositions < other.decimalPositions ->
                return expandWithEffectiveDecimalPositions(other.decimalPositions).plus(other)

            decimalPositions > other.decimalPositions ->
                return plus(other.expandWithEffectiveDecimalPositions(decimalPositions))
        }
        val shorter: UIntArray
        val longer: UIntArray
        when {
            ints.size <= other.ints.size -> { shorter = ints; longer = other.ints }

            else -> { shorter = other.ints; longer = ints }
        }
        val newInts = UIntArray(longer.size + 1)

        var carry = 0u
        for (i in 0 until shorter.size) {
            val x = shorter[i].toULong() + longer[i].toULong() + carry
            newInts[i] = x.toUInt()
            carry = x.shr(32).toUInt()
        }

        for (i in shorter.size until longer.size) {
            val x = longer[i].toULong() + carry
            newInts[i] = x.toUInt()
            carry = x.shr(32).toUInt()
        }
        return createOptimizedInstance(sign, newInts, decimalPositions)
    }

    override fun minus(other: XsdDecimal): T = minus(other.toBigDecimal())

    open operator fun minus(other: AbstractBigDecimal<*>): T {
        when {
            decimalPositions < other.decimalPositions ->
                return expandWithEffectiveDecimalPositions(other.decimalPositions).minus(other)

            decimalPositions > other.decimalPositions ->
                return minus(other.expandWithEffectiveDecimalPositions(decimalPositions))

            sign == 0 -> return -other.asT()
            other.sign == 0 -> return self
            sign < 0 && other.sign > 0 -> return (abs() + other)
            sign > 0 && other.sign < 0 -> return (abs() + other.abs())
        }

        val cmp = compareTo(other)
        val larger: UIntArray
        val smaller: UIntArray
        val newSign = when {
            cmp == 0 -> return newInstance(0, uintArrayOf(0u), decimalPositions)
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

        return createOptimizedInstance(newSign, newInts, decimalPositions)
    }

    override fun times(other: XsdDecimal): T = times(other.asT())

    open operator fun times(other: AbstractBigDecimal<*>): T {
        @Suppress("UNCHECKED_CAST")
        if (other.ints.size> ints.size) return other.asT().times(self)

        val newInts = UIntArray(size.toInt() + other.size.toInt() + 1)
        val newDecimalPositions = decimalPositions + other.decimalPositions

        for (otherIdx in other.ints.indices) {
            var carry = 0u
            for (idx in ints.indices) {
                val newIdx = idx + otherIdx
                val m = ints[idx].toULong() * other.ints[otherIdx].toULong() + carry + newInts[newIdx]
                newInts[newIdx] = m.toUInt()
                carry = m.shr(32).toUInt()
            }
            for (idx in ints.size until newInts.size) {
                val newIdx = idx + otherIdx
                val m = carry.toULong() + newInts[newIdx]
                newInts[newIdx] = m.toUInt()
                carry = m.shr(32).toUInt()
                if (carry == 0u) break // no carry, we are done
            }
        }

        return createOptimizedInstance(sign * other.sign, newInts, newDecimalPositions)
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

        return createOptimizedInstance(sign, newInts, decimalPositions)
    }

    infix fun shl(shift: Int): T {
        require (shift >=0) { "Shift must be non-negative" }
        return shl(shift.toULong())
    }

    infix fun shl(shift: ULong): T {
        if (shift == 0uL || !isFinite) return self
        val newInts = BigUnsignedInt(ints, shift).expandExp().ints
        return newInstance(sign, newInts, decimalPositions)
    }

    /**
     * Get the absolute value of this value.
     */
    override fun abs(): T {
        val s = _sign
        return when (s) {
            0 -> companion.ZERO
            1 -> self
            // Will preserve "special" bits except with flipped sign
            else -> newInstance(-_sign, ints, decimalPositions)
        }
    }

    /**
     * Helper function to ensure decimal positions.
     * @param newDecimalPosition The new decimal position. If positive the value had decimal digits, if negative it is larger
     */
    private fun expandWithEffectiveDecimalPositions(newDecimalPosition: Long): T = when {
        !isFinite -> self
        else -> newInstance(
            sign,
            expandWithEffectiveDecimalPositionsToArray(newDecimalPosition),
            newDecimalPosition
        )
    }

    private fun arrayWithEffectiveDecimalPosition(newDecimalPosition: Long): UIntArray {
        if (!isFinite) return ints
        if (newDecimalPosition == decimalPositions || ints.isEmpty()) return ints
        var shiftNeeded = newDecimalPosition - decimalPositions
        var current = BigUnsignedInt(ints, 0uL)
        do {
            // Note that the target exp must be 0 as we are only retaining the ints.
            when (shiftNeeded) {
                -9L -> return (current.div(1_000_000_000u, 0uL)).ints
                -8L -> return (current.div(100_000_000u, 0uL)).ints
                -7L -> return (current.div(10_000_000u, 0uL)).ints
                -6L -> return (current.div(1_000_000u, 0uL)).ints
                -5L -> return (current.div(100_000u, 0uL)).ints
                -4L -> return (current.div(10_000u, 0uL)).ints
                -3L -> return (current.div(1_000u, 0uL)).ints
                -2L -> return (current.div(100u, 0uL)).ints
                -1L -> return (current.div(10u, 0uL)).ints
                0L -> return current.ints
                1L -> return (current.times(10u, 0uL)).ints
                2L -> return (current.times(100u, 0uL)).ints
                3L -> return (current.times(1_000u, 0uL)).ints
                4L -> return (current.times(10_000u, 0uL)).ints
                5L -> return (current.times(100_000u, 0uL)).ints
                6L -> return (current.times(1_000_000u, 0uL)).ints
                7L -> return (current.times(10_000_000u, 0uL)).ints
                8L -> return (current.times(100_000_000u, 0uL)).ints
                9L -> return (current.times(1_000_000_000u, 0uL)).ints

                else if newDecimalPosition > 9L-> {
                    current *= 1_000_000_000u
                    shiftNeeded -= 9
                }
                else -> {
                    current /= 1_000_000_000u
                    shiftNeeded++
                }
            }
        } while (true)

    }

    private fun expandWithEffectiveDecimalPositionsToArray(newDecimalPosition: Long): UIntArray {
        if (!isFinite) return ints
        var additionalDecimalNeeded = newDecimalPosition - decimalPositions
        check(additionalDecimalNeeded>=0)
        var current = BigUnsignedInt(ints, 0uL)
        do {
            // Note that the target exp must be 0 as we are only retaining the ints.
            when (additionalDecimalNeeded) {
                0L -> return current.ints
                1L -> return (current.times(10u, 0uL)).ints
                2L -> return (current.times(100u, 0uL)).ints
                3L -> return (current.times(1_000u, 0uL)).ints
                4L -> return (current.times(10_000u, 0uL)).ints
                5L -> return (current.times(100_000u, 0uL)).ints
                6L -> return (current.times(1_000_000u, 0uL)).ints
                7L -> return (current.times(10_000_000u, 0uL)).ints
                8L -> return (current.times(100_000_000u, 0uL)).ints
                9L -> return (current.times(1_000_000_000u, 0uL)).ints

                else -> {
                    current *= 1_000_000_000u
                    additionalDecimalNeeded -= 9
                }
            }
        } while (true)
    }

    override fun compareTo(other: XsdDecimal): Int {
        return when {
            !other.isFinite -> when {
                other.isNaN -> throw ArithmeticException("NaN values are not comparable")
                isNegativeInfinity -> 1 // we are always greater than negative infinity
                else -> -1 // we are always smaller than positive infinity
            }

            sign < other.sign -> -1
            sign > other.sign -> 1

            // Optimization when this BigDecimal could be a BigInt.
            decimalPositions == 0L && other !is AbstractBigDecimal<*> -> BigInt(sign, ints, 0uL).compareTo(other)

            // compareBDInts compares positive ints only
            sign < 0 -> other.unaryMinus().compareTo(unaryMinus())

            else -> compareBDInts(other.asT())
        }
    }

    private fun compareBDInts(other: AbstractBigDecimal<*>): Int {
        val otherInts = when {
            decimalPositions < other.decimalPositions ->
                return expandWithEffectiveDecimalPositions(other.decimalPositions).compareBDInts(other)

            decimalPositions > other.decimalPositions ->
                other.expandWithEffectiveDecimalPositions(decimalPositions).ints

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
        if (sign == 0) {
            appendable.append('0')
            return
        }
        when (decimalPositions) {
            0L -> BigInt(sign, ints, 0uL).appendTo(appendable)

            else -> {
                val baseString = BigInt(sign, ints, 0uL).toString()

                (appendable as? StringBuilder)?.ensureCapacity((baseString.length + if (decimalPositions < 0) decimalPositions + 1 else 0).toInt())

                when {
                    decimalPositions < 0 -> appendable
                        .append(baseString).append('.')
                        .append(RepeatSequence(' ', -decimalPositions.toInt()))

                    else -> {
                        val split = baseString.length - decimalPositions.toInt()
                        when {
                            split < 0 -> appendable.append('0')
                            else -> appendable.appendRange(baseString, 0, split)
                        }

                        appendable.append('.')
                        if (split < 0) repeat(-split) { appendable.append('0') }
                        appendable.appendRange(baseString, split.coerceAtLeast(0), baseString.length)
                    }
                }
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
            1u if decimalPositions <= 0L -> return DivRem<T>(self, newInstance(0, companion.ZERO.ints, 0))
            else if (_sign == 0) -> return DivRem(self, companion.ZERO)
        }
        // If we can just extend from UInt to ULong do that here
        val divider = when (decimalPositions) {
            0L -> divider.toULong()
            1L -> divider.toULong() * 10u
            2L -> divider.toULong() * 100u
            3L -> divider.toULong() * 1_000u
            4L -> divider.toULong() * 10_000u
            5L -> divider.toULong() * 100_000u
            6L -> divider.toULong() * 1_000_000u
            7L -> divider.toULong() * 10_000_000u
            8L -> divider.toULong() * 100_000_000u
            9L -> divider.toULong() * 1_000_000_000u
            else -> return divRem(newInstance(1, uintArrayOf(divider), 0L))
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
        val quotientDec = newInstance(newSign, quotient.ints, 0)

        val remainderDec = newInstance(newRemSign, remainder, decimalPositions)

        return DivRem(quotientDec, remainderDec)

    }

    fun divRem(other: XsdUnsignedInt): DivRem<T> = divRem(other.uIntValue)
    fun divRem(other: XsdUnsignedLong): DivRem<T> = divRem(other.uLongValue)

    open fun divRem(divider: T): DivRem<T> { // will (initially) expand exponents
        when {
            divider._sign == 0 -> throw ArithmeticException("Division by zero")
            _sign == 0 -> return DivRem(self, companion.ZERO)
        }

        if (divider.sign == 0) throw ArithmeticException("Division by zero")
        else if (sign == 0) return DivRem(self, companion.ZERO)

        // Extend decimal positions to avoid losing digits in the remainder.
        if (decimalPositions > divider.decimalPositions) return divRem(divider.expandWithEffectiveDecimalPositions(decimalPositions))

        // use BigUnsignedInts to actually perform the division
        val v = BigUnsignedInt(ints, 0uL)
        val d = BigUnsignedInt(divider.ints, 0uL)
        val unsignedDivRem = v.unsignedDivRem(d)

        val newSign = when {
            unsignedDivRem.quotient.sign == 0 -> 0
            sign == divider.sign -> 1
            else -> -1
        }
        val newDecimalPositions = decimalPositions - divider.decimalPositions
        val quotient = newInstance(newSign, unsignedDivRem.quotient.ints, newDecimalPositions)

        val remainderSign = if (unsignedDivRem.remainder.sign == 0) 0 else sign
        val remainder = newInstance(remainderSign, unsignedDivRem.remainder.ints, decimalPositions)

        return DivRem(quotient, remainder)
    }


    override fun equals(other: Any?): Boolean {
        return when (other) {
            !is XsdDecimal -> false

            else -> when (_sign ushr 29) {
                0b001 -> other.isInfinity
                0b110 -> other.isNegativeInfinity
                0b000, 0b111 -> other.isFinite && compareTo(other) == 0
                else -> false // NaN
            }
        }

    }

    override fun hashCode(): Int {
        var result = _sign.hashCode()
        if (ints.isEmpty()) return result
        result = 31 * result + decimalPositions.hashCode()
        var i = ints.lastIndex
        while (i>= 0 && ints[i]==0u) { i-=1 }
        for (j in 0..i) {
            result = 31 * result + ints[j].hashCode()
        }
        return result
    }


    override fun toString(): String = buildString {
        append("BigDecimal(")
        ints.reversed().joinTo(this, "_") {
            it.toString(16).padStart(8, '0')
        }
        if (decimalPositions != 0L) append("×10^").append(-decimalPositions)
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

    internal class ParseResult(val sign: Int, val ints: UIntArray, val decimalDigits: Long)

    abstract class CompanionBase<T: AbstractBigDecimal<T>> {

        abstract val ZERO: T
        abstract val ONE: T
        abstract val MINUSONE: T

        internal abstract fun newInstance(sign: Int, ints: UIntArray, decimalPositions: Long): T

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
            var normalised1 = normalised
            var sign1 = sign
            val decimalDigits: Long
            val signPos = normalised1.lastIndexOf('.')
            if (signPos >= 0) {
                if (normalised1.lastIndexOf('.', signPos - 1) >= 0) {
                    throw NumberFormatException("Multiple decimal points")
                }
                decimalDigits = (normalised1.length - signPos - 1).toLong()
                normalised1 = normalised1.substring(0, signPos) + normalised1.substring(signPos + 1)
            } else {
                decimalDigits = 0L
            }

            val intsNeeded = 1 + normalised1.length / 9 // not very accurate but good enough for now

            val last = normalised1.length

            var numbers = UIntArray(intsNeeded)
            var tmp = UIntArray(intsNeeded)
            var intsUsed = 1

            var first = normalised1.length.rem(9) // actually initialise it after the first substring

            if (first > 0) {
                numbers[0] = normalised1.substring(0, minOf(first, last)).toUInt()
            }

            while (first < last) {
                val nextInt = normalised1.substring(first, minOf(first + 9, last)).toULong()

                val m = numbers[0].toULong() * 1_000_000_000uL + nextInt
                tmp[0] = m.toUInt()
                var carry: UInt = m.shr(32).toUInt()

                for (i in 1 until intsUsed) {
                    val m = numbers[i].toULong() * 1_000_000_000uL + carry

                    tmp[i] = m.toUInt()
                    carry = m.shr(32).toUInt()
                }

                if (carry != 0u) {
                    tmp[intsUsed] = carry
                    intsUsed += 1
                }

                for (i in intsUsed until tmp.size) {
                    tmp[i] = 0u
                }

                val x = numbers
                numbers = tmp
                tmp = x

                first += 9
            }

            var lastByteToKeep = numbers.lastIndex
            while (lastByteToKeep > 0 && numbers[lastByteToKeep] == 0u) lastByteToKeep -= 1

            val array = if (lastByteToKeep + 1 == numbers.size) numbers else numbers.copyOf(lastByteToKeep + 1)

            // Make sure that the sign field is accurate.
            if (array.size == 1 && array[0] == 0u) sign1 = 0

            return ParseResult(sign1, array, decimalDigits)
        }

        operator fun invoke(value: Int, decimalPositions: Long = 0L): T = when {
            value < 0 -> {
                val absValue = abs(value)
                newInstance(-1, uintArrayOf(absValue.toUInt()), decimalPositions)
            }
            value == 0 -> ZERO
            else -> newInstance(1, uintArrayOf(value.toUInt()), decimalPositions)
        }

        operator fun invoke(value: UInt, decimalPositions: Long = 0L): T = when {
            value == 0u -> ZERO
            else -> newInstance(1, uintArrayOf(value), decimalPositions)
        }

        operator fun invoke(value: Long, decimalPositions: Long = 0L): T {
            if (value == 0L) return ZERO
            val absValue = value.absoluteValue.toULong()
            val array = when {
                absValue <= UInt.MAX_VALUE -> uintArrayOf(absValue.toUInt())
                else -> uintArrayOf(absValue.toUInt(), (absValue shr 32).toUInt())
            }
            return when {
                value < 0L -> newInstance(-1, array, decimalPositions)
                else -> newInstance(1, array, decimalPositions)
            }
        }

        operator fun invoke(value: ULong, decimalPositions: Long = 0L): T {
            if (value == 0uL) return ZERO
            val array = when {
                value <= UInt.MAX_VALUE -> uintArrayOf(value.toUInt())
                else -> uintArrayOf(value.toUInt(), (value shr 32).toUInt())
            }
            return newInstance(1, array, decimalPositions)
        }

    }

    companion object {

        @XmlUtilInternal
        protected const val SIGN_BIT = 1 shl 31
        @XmlUtilInternal
        protected const val NAN_BIT = 1 shl 30
        @XmlUtilInternal
        protected const val INFINITY_BIT = 1 shl 29
        @XmlUtilInternal
        protected const val SPECIAL_MASK = SIGN_BIT or NAN_BIT or INFINITY_BIT

    }

}
