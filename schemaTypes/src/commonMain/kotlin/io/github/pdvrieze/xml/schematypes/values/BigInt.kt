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

package io.github.pdvrieze.xml.schematypes.values

import io.github.pdvrieze.xml.schematypes.types.IntegerType
import io.github.pdvrieze.xml.schematypes.values.instances.BigDecimal
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
import kotlin.math.absoluteValue

@OptIn(ExperimentalUnsignedTypes::class)
class BigInt internal constructor(override val sign: Int, ints: UIntArray, exp: ULong) :
    AbstractBigInteger<BigInt>(ints, exp) {
    init {
        require(ints.isNotEmpty()) { "At least one integer must be present" }
        if (ints.all { it == 0u }) {
            require(ints.size == 1) { "Zero must be a single int"}
            require(sign == 0) { "The value is zero, but the sign is not 0" }
            require(exp == 0uL) { "The value is zero, but the exponent is not 0" }
        } else {
            require(sign != 0) { "The value is non-zero, but the sign is 0" }
        }
        require(sign in -1..1) { "Invalid sign" }
    }

    private constructor(r: ParseResult): this(r.sign, r.ints, r.exp)

    constructor(str: CharSequence): this(parse(str))

    constructor(int: Int) : this(
        int.compareTo(0),
        uintArrayOf(int.absoluteValue.toUInt()),
        0uL
    )

    constructor(long: Long) : this(
        long.compareTo(0L),
        when (val a = long.absoluteValue.toULong()) {
            in 0uL..<UInt.MAX_VALUE.toULong() -> uintArrayOf(a.toUInt())
            else -> uintArrayOf(a.toUInt(), (a shr 32).toUInt())
        },
        0uL
    )

    override val self: BigInt get() = this

    override fun newInstance(value: Long): BigInt {
        if (value == 0L) return ZERO
        val unsigned = value.absoluteValue.toULong()
        val arrayValue = when {
            unsigned <= UInt.MAX_VALUE -> uintArrayOf(unsigned.toUInt())
            else -> uintArrayOf(unsigned.toUInt(), (unsigned shr 32).toUInt())
        }

        return when {
            value < 0L -> BigInt(-1, arrayValue, 0uL)
            else -> BigInt(1, arrayValue, 0uL)
        }
    }

    override fun newInstance(sign: Int, elems: UIntArray, exp: ULong): BigInt {
        return BigInt(sign, elems, exp)
    }

    internal fun normalize(): BigInt {
        if (sign == 0) return ZERO

        val trailingBits = countTrailingZeroBits()
        val leadingBits = countLeadingZeroBits()
        if (trailingBits >= 3uL || (trailingBits+leadingBits > 32u)) {
            return createOptimizedInstance(sign, ints, exp)
        }
        return this
    }

    override val schemaType: IntegerType<XsdInteger>
        get() = IntegerType.Instance

    override fun toBigInt(): BigInt {
        return this
    }

    override fun toLong(): Long {
        val positive = when (size) {
            1uL -> get(0).toLong()
            else -> (get(0).toULong() or (get(1) and 0x7FFF_FFFFu).toULong().shl(32)).toLong()
        }
        return if (sign<0) -(positive) else positive
    }

    override fun toInt(): Int {
        val positive = (get(0) and 0x7FFF_FFFFu).toInt()
        return if (sign < 0) -(positive) else positive
    }

    override fun div(divider: BigInt): BigInt = divRem(divider).quotient

    override fun divRem(divider: XsdDecimal): XsdDecimal.DivRem = when (divider) {
        is XsdInteger -> divRem(divider.toBigInt())
        else -> toBigDecimal().divRem(divider)
    }

    override fun divRem(divider: XsdInteger): XsdInteger.DivRem = when (divider) {
        is BigInt -> divRem(divider)
        else -> divRem(divider.toBigInt())
    }

    override fun divRem(divider: UInt): DivRem {
        return divRem(BigInt(if (divider == 0u) 0 else 1, uintArrayOf(divider), 0uL))
    }

    override fun divRem(divider: BigInt): DivRem {
        val nonzeroSign = when {
            divider.sign < 0 -> -sign
            divider.sign == 0 -> throw ArithmeticException("Division by zero")
            else -> sign
        }
        val base = unsignedDivRem(divider)
        val finalSign = if (base.quotient.sign == 0) 0 else nonzeroSign
        val remSign = if (base.remainder.sign == 0) 0 else sign
        return DivRem(
            quotient = BigInt(finalSign, base.quotient.ints, base.quotient.exp),
            remainder = BigInt(remSign, base.remainder.ints, base.remainder.exp),
        )

    }

    override fun plus(other: XsdDecimal): XsdDecimal = when (other) {
        is XsdInteger -> plus(other)
        else -> other.plus(this)
    }

    override fun plus(other: Int): BigInt = plus(XsdInt(other))

    override fun plus(other: Long): BigInt = plus(XsdLong(other))

    override fun plus(other: UInt): BigInt = plus(XsdUnsignedInt(other))

    override fun plus(other: ULong): BigInt = plus(XsdUnsignedLong(other))

    override fun plus(other: XsdInteger): BigInt = when {
        sign == 0 -> BigInt(other)

        other.sign == 0 -> this

        sign < 0 -> when {
            other.sign > 0 -> plusNegPos(other.abs())

            else -> BigInt(abs().plus(other.abs()).unaryMinus())
        }

        // sign > 0
        other.sign < 0 -> BigInt(minus(other.abs()))

        else -> BigInt(abs().plus(other.abs()))
    }

    /**
     * Add the positive value to the existing negative one
     */
    private fun plusNegPos(add: XsdNonNegativeInteger): BigInt {
        return BigInt(abs().minus(add).unaryMinus())
    }

    override fun unaryMinus(): XsdInteger = BigInt(-sign, ints, exp)
    override fun unaryPlus(): XsdInteger = this

    override fun minus(other: XsdDecimal): XsdDecimal = when (other) {
        is XsdInteger -> minus(other)
        else -> plus(other.unaryMinus())
    }

    override fun minus(other: XsdInteger): XsdInteger {
        when {
            sign == 0 -> return BigInt(other.unaryMinus())
            other.sign == 0 -> return this
        }
        val leftAbs = abs()
        val rightAbs = other.abs()
        when {
            other.sign < 0 -> when {
                sign > 0 -> return BigInt((leftAbs.plus(other.abs())))
                else -> return BigInt((leftAbs - other.abs()).unaryMinus())
            }
            sign < 0 -> return BigInt(leftAbs.plus(other.abs()).unaryMinus())
        }
        // comparison is smart and compares MSI first
        val c = leftAbs.compareTo(other.abs())
        if (c == 0) return ZERO
        if (c < 0) return (rightAbs - leftAbs).unaryMinus()
        // The only case remaining is this one is where both are positive and left is larger than right
        val bitsNeeded = maxOf(significantBitsFromZero(), other.significantBitsFromZero())
        val result = UIntArray(((bitsNeeded + 31u) shr 5).toInt())

        var borrow = 0L
        for (i in 0 until result.size) {
            val a = get(i).toLong() - borrow
            val b = other.get(i).toLong()
            if (a>=b) {
                result[i] = (a - b).toUInt()
                borrow = 0L
            } else {
                val neg = (a + 0x1_0000_0000L - b)
                result[i] = neg.toUInt()
                borrow = 1L
            }
        }
        assert(borrow == 0L) { "Sign inversion" }
        // note that recursive calls will sign flip this if needed
        return createOptimizedInstance(1, result, 0uL)
    }

    override fun times(other: XsdDecimal): XsdDecimal = when (other) {
        is XsdInteger -> times(other)
        else -> other.times(this) // must be a decimal, let decimal implement it
    }

    override fun times(other: XsdInteger): XsdInteger {
        return times(BigInt(other))
    }

    override fun compareTo(other: XsdInteger): Int {
        return when {
            sign < 0 -> when {
                other.sign < 0 -> other.abs().compareTo(abs())
                else -> -1
            }

            sign == 0 -> other.sign

            else -> when {
                other.sign > 0 -> abs().compareTo(other.abs())
                else -> 1
            }
        }
    }

    override fun equals(other: Any?): Boolean = when (other) {
        is XsdInteger -> compareTo(other) == 0
        is XsdDecimal -> BigDecimal(this).compareTo(other) == 0
        else -> false
    }

    operator fun rangeTo(other: BigInt): XsdIntegerProgression<BigInt> {
        return Range(this, other)
    }

    override fun rangeTo(other: XsdInteger): XsdIntegerProgression<BigInt> = when (other) {
        is BigInt -> rangeTo(other)
        else -> rangeTo(other.toBigInt())
    }


    internal class Range(override val first: BigInt, val endInclusive: BigInt): XsdIntegerProgression<BigInt> {
        override val last: BigInt get() = endInclusive

        override fun iterator(): Iterator<BigInt> {
            return RangeIterator(first, endInclusive)
        }
    }

    internal class RangeIterator(start: BigInt, private val endInclusive: BigInt): Iterator<BigInt> {
        var pos = start

        override fun hasNext(): Boolean {
            return pos <= endInclusive
        }

        override fun next(): BigInt {
            if (pos > endInclusive) throw NoSuchElementException()
            return pos.also {
                pos += 1
            }
        }
    }

    class DivRem(
        override val quotient: BigInt,
        override val remainder: BigInt
    ): AbstractBigInteger.DivRem<BigInt, BigInt>, XsdInteger.DivRem, IntDivRem<BigInt, BigInt>

    private class ParseResult(val sign: Int, val ints: UIntArray, val exp: ULong)



    companion object {
        public val ZERO: BigInt = BigInt(0, uintArrayOf(0u), 0uL)

        operator fun invoke(init: XsdInteger): BigInt {
            if (init is AbstractBigInteger<*>) return BigInt(init.sign, init.ints, init.exp)
            val sign = init.sign
            val ints = UIntArray(init.size.toInt()) { init[it] }

            val neededInts = ints.indexOfLast { it != 0u }.coerceAtLeast(0) + 1

            val shortened = if (neededInts == ints.size) ints else ints.copyOf(neededInts)

            return BigInt(sign, shortened, 0uL).normalize()
        }

        private fun parse(s: CharSequence): ParseResult {
            if (s.isEmpty()) throw NumberFormatException("Empty string")
            val isNegative: Boolean
            val base: AbstractBigUnsignedInt.ParseResult
            when (s[0]) {
                '-' -> {
                    isNegative = true
                    base = AbstractBigUnsignedInt.parse(s.subSequence(1, s.length))
                }

                '+' -> {
                    isNegative = false
                    base = AbstractBigUnsignedInt.parse(s.subSequence(1, s.length))
                }

                else -> {
                    isNegative = false
                    base = AbstractBigUnsignedInt.parse(s)
                }
            }


            val sign = when {
                base.ints.size == 1 && base.ints[0] == 0u -> 0
                isNegative -> -1
                else -> 1
            }
            return ParseResult(sign, base.ints, base.exp)
        }
    }
}
