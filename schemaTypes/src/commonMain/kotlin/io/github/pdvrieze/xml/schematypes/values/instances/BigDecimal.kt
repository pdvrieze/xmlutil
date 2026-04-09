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

import io.github.pdvrieze.xml.schematypes.values.*
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
import kotlin.math.abs
import kotlin.math.absoluteValue

@OptIn(ExperimentalUnsignedTypes::class)
class BigDecimal internal constructor(
    override val sign: Int,
    internal val ints: UIntArray,
    internal val decimalPositions: Long
) : XsdBigDecimal {

    init {
        when {
            ints.size == 1 && ints[0] == 0u -> require(sign == 0) { "Zero value must have a 0 sign" }
            sign == 0 ->
                throw IllegalArgumentException("Zero sign must have a single int")
            else -> require(sign != 0) { "Non-zero values must not have a 0 sign" }
        }
        require(ints.size !=2 || ints[1]!=0u) { "The second int must not be zero" }
    }

    val self: BigDecimal get() = this

    private constructor(parseResult: ParseResult) : this(parseResult.sign, parseResult.ints, parseResult.decimalDigits)

    constructor(value: CharSequence): this(parse(value))

    constructor(value: UInt): this(
        sign = if (value == 0u) 0 else 1,
        ints = uintArrayOf(value),
        decimalPositions = 0L
    )

    constructor(value: ULong) : this(
        sign = if (value == 0uL) 0 else 1,
        ints = when {
            value <= UInt.MAX_VALUE -> uintArrayOf(value.toUInt())
            else -> uintArrayOf(value.toUInt(), (value shr 32).toUInt())
        },
        decimalPositions = 0L
    )

    override fun toLong(): Long {
        val base = floor()
        return when (base.ints.size) {
            0 -> 0L
            1 -> base.ints[0].toLong() * sign

            2 if (base.ints[1] and 0x8000_0000u == 0u) -> {
                val uLongValue = base.ints[0].toULong() or (base.ints[1].toULong() shl 32)
                uLongValue.toLong() * sign
            }

            else -> throw ArithmeticException("Value too large to fit in a Long")
        }
    }

    override fun toInt(): Int {
        val base = floor()
        return when (base.ints.size) {
            0 -> 0
            1 if (base.ints[0] and 0x8000_0000u == 0u) -> base.ints[0].toInt() * sign
            else -> throw ArithmeticException("Value too large to fit in a Long")
        }
    }

    override fun toBigDecimal(): BigDecimal {
        return this
    }

    override fun floor(): BigDecimal {
        if (sign == 0 || decimalPositions == 0L) return this
        else if (decimalPositions < 0) {
            val expanded = expandWithEffectiveDecimalPositionsToArray(0)
            return BigDecimal(sign, expanded, 0L)
        }
        val divisor = BigDecimal(1, uintArrayOf(1u), 0)
            .expandWithEffectiveDecimalPositions(decimalPositions)

        val (quotient, _) = divRem(divisor)
        if (sign > 0) return quotient
        else if (quotient[0] < UInt.MAX_VALUE) {
            quotient.ints[0]+=1u
            return quotient
        } else {
            return quotient.plus(BigDecimal(1))
        }
    }

    override fun ceiling(): XsdDecimal {
        return unaryMinus().floor().unaryMinus()
    }

    override fun round(): XsdDecimal {
        return round(0)
    }

    override fun round(precision: Int): BigDecimal {
        return roundImpl(precision, false)
    }

    override fun roundToInteger(): XsdInteger {
        val dec = round(0)
        check(dec.decimalPositions == 0L)
        return when (dec.ints.size) {
            1 -> XsdInt(dec.toInt())
            2 -> XsdLong.Companion(dec.toLong())
            else -> {
                val unOpt = BigInt(dec.sign, dec.ints, 0uL)
                if (unOpt.countTrailingZeroBits() > 48uL) unOpt.normalize() else unOpt
            }
        }
    }

    private fun roundImpl(precision: Int, halfEven: Boolean): BigDecimal {
        if (sign == 0 || decimalPositions == precision.toLong()) return this

        if (decimalPositions < precision) {
            val expanded = expandWithEffectiveDecimalPositionsToArray(precision.toLong())
            return BigDecimal(sign, expanded, 0)
        }
        val divisor = BigDecimal(1, uintArrayOf(1u), 0)
            .expandWithEffectiveDecimalPositions(decimalPositions - precision.toLong())

        val (quotient, remainder) = divRem(divisor)

        val doubleRemainder = remainder.shl(1uL).abs()

        val cmp = doubleRemainder.compareTo(divisor)
        if (cmp < 0) return quotient
        else if (cmp == 0) {
            if (!halfEven) return quotient

            val isOdd = quotient.ints[0] and 0x1u == 1u
            if (isOdd) {
                if (quotient.ints[0] >= UInt.MAX_VALUE) return quotient.plus(BigDecimal(1))
                quotient.ints[0] += 1u
                return quotient
            }
        }

        if (quotient[0] < UInt.MAX_VALUE) {
            quotient.ints[0] += 1u
            return quotient
        } else {
            return quotient.plus(BigDecimal(1))
        }
    }

    override fun roundToHalfEven(): BigDecimal {
        return roundToHalfEven(0)
    }

    override fun roundToHalfEven(precision: Int): BigDecimal {
        return roundImpl(precision, true)
    }

    private fun countTrailingZeroBits(): ULong {
        for (i in ints.indices) {
            if (ints[i] != 0u) return ((i.toULong() * 32uL) + ints[i].countTrailingZeroBits().toULong())
        }
        throw ArithmeticException("The value is zero, ")
    }

    protected fun countLeadingZeroBits(): ULong {
        for (i in ints.indices.reversed()) {
            if (ints[i] != 0u) {
                return (((ints.size - 1 - i).toULong() shl 5) + ints[i].countLeadingZeroBits().toULong())
            }
        }
        return ints.size.toULong() shl 5
    }

    private fun significantBitsFromZero(): ULong {
        return (ints.size.toULong() shl 5) - countLeadingZeroBits()
    }

    private val size: ULong
        get() = ints.size.toULong()

    override fun unaryMinus(): BigDecimal = BigDecimal(-sign, ints, decimalPositions)
    override fun unaryPlus(): BigDecimal = this

    operator fun get(index: ULong): UInt {
        return ints[index.toInt()]
    }

    operator fun get(index: Int): UInt {
        return ints[index]
    }

    operator fun div(divider: BigDecimal): BigDecimal {
        return divRem(divider).quotient
    }


    /**
     * Determines how many ints are needed to store the result. Will always return at least 1
     */
    private fun nonLeadingZeroIntCount(ints: UIntArray): Int {
        for (i in ints.indices.reversed()) {
            if (ints[i] != 0u) return i + 1
        }
        return 1
    }

    /**
     * @param elems The base elements for the integer
     * @params exp The exponent of the base elements. This  may not be the final value
     */
    private fun createOptimizedInstance(sign: Int, elems: UIntArray, decimalPositions: Long): BigDecimal {
        var lastIdx = elems.size - 1
        while (lastIdx > 0 && elems[lastIdx] == 0u) lastIdx -= 1

        val newSize = lastIdx + 1
        val newElems  = if (newSize<elems.size) elems.copyOfRange(0, newSize) else elems


        return BigDecimal(sign, newElems, decimalPositions)
    }

    override fun plus(other: XsdDecimal): XsdDecimal = plus(other.toBigDecimal())

    operator fun plus(other: BigDecimal): BigDecimal {
        when {
            sign == 0 -> return other
            other.sign == 0 -> return this
            sign > 0 && other.sign < 0 -> return minus(other.abs())
            sign < 0 && other.sign > 0 -> return other.minus(abs())

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

    override fun minus(other: XsdDecimal): XsdDecimal = minus(other.toBigDecimal())

    operator fun minus(other: BigDecimal): BigDecimal {
        when {
            decimalPositions < other.decimalPositions ->
                return expandWithEffectiveDecimalPositions(other.decimalPositions).minus(other)

            decimalPositions > other.decimalPositions ->
                return minus(other.expandWithEffectiveDecimalPositions(decimalPositions))

            sign == 0 -> return other.unaryMinus()
            other.sign == 0 -> return this
            sign < 0 && other.sign > 0 -> return (abs() + other)
            sign > 0 && other.sign < 0 -> return (abs() + other.abs())
        }

        val cmp = compareTo(other)
        val larger: UIntArray
        val smaller: UIntArray
        val newSign = when {
            cmp == 0 -> return BigDecimal(0, uintArrayOf(0u), decimalPositions)
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

    override fun times(other: XsdDecimal): XsdDecimal = times(other.toBigDecimal())

    operator fun times(other: BigDecimal): BigDecimal {
        @Suppress("UNCHECKED_CAST")
        if (other.ints.size> ints.size) return other.times(self)

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

    override operator fun times(other: UInt): BigDecimal {
        val newInts = UIntArray(size.toInt() + 1)

        var carry = 0u
        for (idx in ints.indices) {
            val m = ints[idx].toULong() * other.toULong() + carry + newInts[idx]
            newInts[idx] = m.toUInt()
            carry = m.shr(32).toUInt()
        }

        val m = carry.toULong() + newInts[ints.size]
        newInts[ints.size] = m.toUInt()
        assert(m.shr(32).toUInt() == 0u)

        return createOptimizedInstance(sign, newInts, decimalPositions)
    }

    infix fun shl(shift: Int): BigDecimal {
        require (shift >=0) { "Shift must be non-negative" }
        return shl(shift.toULong())
    }

    infix fun shl(shift: ULong): BigDecimal {
        if (shift == 0uL) return self
        val newInts = BigUnsignedInt(ints, shift).expandExp().ints
        return BigDecimal(sign, newInts, decimalPositions)
    }

    /**
     * Get the absolute value of this value.
     */
    final override fun abs(): BigDecimal {
        return when {
            sign == 0 -> ZERO
            sign > 0 -> this
            else -> BigDecimal(1, ints, decimalPositions)
        }
    }

    /**
     * Helper function to ensure decimal positions.
     * @param newDecimalPosition The new decimal position. If positive the value had decimal digits, if negative it is larger
     */
    private fun expandWithEffectiveDecimalPositions(newDecimalPosition: Long): BigDecimal =
        BigDecimal(
            sign,
            expandWithEffectiveDecimalPositionsToArray(newDecimalPosition),
            newDecimalPosition
        )

    private fun expandWithEffectiveDecimalPositionsToArray(newDecimalPosition: Long): UIntArray {
        var additionalDecimalNeeded = newDecimalPosition - decimalPositions
        check(additionalDecimalNeeded>=0)
        var current = BigUnsignedInt(ints, 0uL)
        do {
            when (additionalDecimalNeeded) {
                0L -> return current.ints
                1L -> return (current*10u).ints
                2L -> return (current*100u).ints
                3L -> return (current*1_000u).ints
                4L -> return (current*10_000u).ints
                5L -> return (current*100_000u).ints
                6L -> return (current*1_000_000u).ints
                7L -> return (current*10_000_000u).ints
                8L -> return (current*100_000_000u).ints
                9L -> return (current*1_000_000_000u).ints

                else -> {
                    current *= 1_000_000_000u
                    additionalDecimalNeeded -= 9
                }
            }
        } while (true)
    }

    override fun compareTo(other: XsdDecimal): Int {
        return when {
            sign < other.sign -> -1
            sign > other.sign -> 1

            // Optimization when this BigDecimal could be a BigInt.
            decimalPositions == 0L && other !is BigDecimal -> BigInt(sign, ints, 0uL).compareTo(other)

            else -> compareTo(other.toBigDecimal())
        }
    }

    override fun compareTo(other: XsdBigDecimal): Int = compareTo(other as XsdDecimal)

    operator fun compareTo(other: BigDecimal): Int {
        when {
            sign < other.sign -> return -1
            sign > other.sign -> return 1

            decimalPositions < other.decimalPositions ->
                return expandWithEffectiveDecimalPositions(other.decimalPositions).compareTo(other)

            sign < 0 -> return other.unaryMinus().compareTo(unaryMinus())
            ints.size < other.ints.size -> return -1
            ints.size > other.ints.size -> return 1
        }
        for (i in ints.indices.reversed()) {
            val v = ints[i]
            val o = other.ints[i]
            when {
                v < o -> return -1
                v > o -> return 1
            }
        }
        return 0
    }



    override val xmlString: String
        get() = buildString {appendTo(this) }

    internal fun appendTo(appendable: Appendable) {
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
                        appendable.appendRange(baseString, 0, split)
                        appendable.append('.')
                        appendable.appendRange(baseString, split, baseString.length)
                    }
                }
            }
        }
    }

    /** Implementation of the division algorithm that ignores all signs. */
    protected fun unsignedDivRem(divider: UInt): UIntDivRem { // will (initially) expand exponents
        return when (divider) {
            0u -> throw ArithmeticException("Division by zero")
            1u -> UIntDivRem(this, 0u)
            else -> {
                TODO()

//                val divRem  = unsignedDivRem(BigDecimal(1, uintArrayOf(divider), 0L))
//                return UIntDivRem(divRem.)
            }
        }
    }

    override fun divRem(other: XsdDecimal): XsdDecimal.DivRem {
        return divRem(other.toBigDecimal())
    }

    fun divRem(divider: BigDecimal): DivRem { // will (initially) expand exponents
        if (divider.sign == 0) throw ArithmeticException("Division by zero")
        else if (sign == 0) return DivRem(this, ZERO)

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
        val quotient = BigDecimal(newSign, unsignedDivRem.quotient.ints, newDecimalPositions)

        val remainderSign = if (unsignedDivRem.remainder.sign == 0) 0 else sign
        val remainder = BigDecimal(remainderSign, unsignedDivRem.remainder.ints, decimalPositions)

        return DivRem(quotient, remainder)
    }

    /**
     * Perform an in place multiplySubtract modifying [target]
     * @param target The array to perform the multiplySubtract on
     * @param a The array to multiply. For division this is the divider
     * @param multiplier The single token multiplier the one "int" digit of the divider
     * @param leftOffset The offset of the first int in the target array.
     * @return If a "borrow" was needed
     */
    private fun multiplySubtractInPlace(target: UIntArray, a: UIntArray, multiplier: UInt, leftOffset: Int): Boolean {
        var carry = 0u
        var borrow = 0u
        for (i in 0 until a.size) {
            val ti = leftOffset + i

            val mFull = (a[i].toULong() * multiplier.toULong() + carry)
            carry = mFull.shr(32).toUInt()
            val m = mFull and 0xffff_ffffuL
            val t = target[ti] - borrow
            if (t<m) {
                borrow = ((m-t) shr 32).toUInt()
                target[ti] = (0xffff_ffff_0000_0000uL+t-borrow).toUInt()
            } else {
                borrow = 0u
                target[ti] = (t-m).toUInt()
            }
        }
        val toReduce = borrow + carry

        if (toReduce == 0u) return false

        val t = target[a.size + leftOffset + 1]
        if (t >= toReduce) {
            target[a.size + leftOffset] = t - toReduce
            return false
        }
        // Else Step D6 - Add a again
        carry = 0u
        for(i in 0 until a.size) {
            val add = target[i+leftOffset].toULong() + a[i]
            target[i+leftOffset] = add.toUInt()
            carry = add.shr(32).toUInt()
        }
        target[a.size + leftOffset] = t + carry - toReduce
        return true
    }


    override fun equals(other: Any?): Boolean {
        if (other !is XsdDecimal) return false
        return compareTo(other) == 0
    }

    override fun hashCode(): Int {
        // TODO something more sane (and efficient)
        return xmlString.hashCode()
    }


    override fun toString(): String = buildString {
        append("BigDecimal(")
        ints.reversed().joinTo(this, "_") {
            it.toString(16).padStart(8, '0')
        }
        if (decimalPositions != 0L) append("×10^").append(-decimalPositions)
        append(')')
    }

    interface IDivRem<out R> {
        val quotient: BigDecimal
        val remainder: R
    }

    data class DivRem(
        override val quotient: BigDecimal,
        override val remainder: BigDecimal
    ) : IDivRem<BigDecimal>, XsdDecimal.DivRem

    data class UIntDivRem(
        override val quotient: BigDecimal,
        override val remainder: UInt
    ) : IDivRem<UInt>

    private class RepeatSequence(val char: Char, override val length: Int) : CharSequence {
        override fun get(index: Int): Char = char

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
            RepeatSequence(char, endIndex - startIndex)
    }

    private class ParseResult(val sign: Int, val ints: UIntArray, val decimalDigits: Long)

    companion object {
        val ZERO = BigDecimal(0, uintArrayOf(0u), 0)
        val ONE = BigDecimal(1, uintArrayOf(1u), 0)
        val MINUSONE = BigDecimal(-1, uintArrayOf(1u), 0)

        operator fun invoke(value: Int, decimalPositions: Long = 0L): BigDecimal = when {
            value < 0 -> {
                val absValue = abs(value)
                BigDecimal(-1, uintArrayOf(absValue.toUInt()), decimalPositions)
            }
            value == 0 -> ZERO
            else -> BigDecimal(1, uintArrayOf(value.toUInt()), decimalPositions)
        }

        operator fun invoke(value: Long, decimalPositions: Long = 0L): BigDecimal {
            if (value == 0L) return ZERO
            val absValue = value.absoluteValue.toULong()
            val array = when {
                absValue <= UInt.MAX_VALUE -> uintArrayOf(absValue.toUInt())
                else -> uintArrayOf(absValue.toUInt(), (absValue shr 32).toUInt())
            }
            return when {
                value < 0L -> BigDecimal(-1, array, decimalPositions)
                else -> BigDecimal(1, array, decimalPositions)
            }
        }

        private fun parse(s: CharSequence): ParseResult {
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
            val decimalDigits: Long
            val signPos = normalised.lastIndexOf('.')
            if (signPos >= 0) {
                if (normalised.lastIndexOf('.', signPos-1)>=0) {
                    throw NumberFormatException("Multiple decimal points")
                }
                decimalDigits = (normalised.length - signPos - 1).toLong()
                normalised = normalised.substring(0, signPos) + normalised.substring(signPos + 1)
            } else {
                decimalDigits = 0L
            }

            val intsNeeded = 1 + normalised.length / 9 // not very accurate but good enough for now

            val last = normalised.length

            var numbers = UIntArray(intsNeeded)
            var tmp = UIntArray(intsNeeded)
            var intsUsed = 1

            var first = normalised.length.rem(9) // actually initialise it after the first substring

            if (first > 0) {
                numbers[0] = normalised.substring(0, minOf(first, last)).toUInt()
            }

            while (first < last) {
                val nextInt = normalised.substring(first, minOf(first+9, last)).toULong()

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

                for (i in intsUsed until tmp.size) { tmp[i] = 0u }

                val x = numbers
                numbers = tmp
                tmp = x

                first += 9
            }

            var lastByteToKeep = numbers.lastIndex
            while (lastByteToKeep > 0 && numbers[lastByteToKeep] == 0u) lastByteToKeep -= 1

            val array = if (lastByteToKeep + 1 == numbers.size) numbers else numbers.copyOf(lastByteToKeep + 1)

            // Make sure that the sign field is accurate.
            if (array.size == 1 && array[0] == 0u) sign = 0

            return ParseResult(sign, array, decimalDigits)
        }

    }

}
