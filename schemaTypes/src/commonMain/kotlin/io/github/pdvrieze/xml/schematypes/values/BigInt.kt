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
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
import kotlin.math.absoluteValue

@OptIn(ExperimentalUnsignedTypes::class)
class BigInt internal constructor(override val sign: Int, ints: UIntArray, exp: ULong) :
    AbstractBigInteger<BigInt>(ints, exp) {
    init {
        require(ints.isNotEmpty()) { "At least one integer must be present" }
        if (sign != 0 || ints.size > 1 || exp != 0uL) {
            require(ints.any { it != 0u }) { "Zero values must be represented as a single int" }
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
        when (long) {
            in Int.MIN_VALUE..<Int.MAX_VALUE -> uintArrayOf(long.absoluteValue.toUInt())
            else -> long.absoluteValue.toULong().let { uintArrayOf(it.toUInt(), (it shr 32).toUInt()) }
        },
        0uL
    )

    override val self: BigInt get() = this

    override fun newInstance(value: Long): BigInt {
        if (value == 0L) return ZERO
        if (value < 0L) {
            val unsigned = (-value).toULong()
            return BigInt(-1, uintArrayOf(unsigned.toUInt(), (unsigned shr 32).toUInt()), 0uL)
        } else {
            val unsigned = value.toULong()
            return BigInt(1, uintArrayOf(unsigned.toUInt(), (unsigned shr 32).toUInt()), 0uL)
        }
    }

    override fun newInstance(sign: Int, elems: UIntArray, exp: ULong): BigInt {
        return BigInt(sign, elems, exp)
    }

    internal fun normalize(): BigInt {
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

    override fun divRem(divider: BigInt): AbstractBigInteger.DivRem<BigInt, BigInt> {
        val nonzeroSign = when {
            divider.sign < 0 -> -sign
            divider.sign == 0 -> throw ArithmeticException("Division by zero")
            else -> sign
        }
        val base = unsignedDivRem(divider)
        val finalSign = if (base.quotient.sign == 0) 0 else nonzeroSign
        return DivRem(
            quotient = BigInt(finalSign, base.quotient.ints, base.quotient.exp),
            remainder = BigInt(sign, base.remainder.ints, base.remainder.exp),
        )

    }

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

        var borrow: Long = 0L
        for (i in 0 until result.size) {
            val a = get(0).toLong() - borrow
            val b = other.get(0).toLong()
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

    override fun hashCode(): Int {
        return sign + ints.contentHashCode() + exp.toInt() * 31
    }
    override fun equals(other: Any?): Boolean {
        return compareTo(other as? XsdInteger ?: return false) == 0
    }

    override fun toString(): String {
        return xmlString
    }

    class DivRem(
        override val quotient: BigInt,
        override val remainder: BigInt
    ): AbstractBigInteger.DivRem<BigInt, BigInt>

    private class ParseResult(val sign: Int, val ints: UIntArray, val exp: ULong)

    companion object {
        public val ZERO: BigInt = BigInt(0, uintArrayOf(0u), 0uL)

        operator fun invoke(init: XsdInteger): BigInt {
            if (init is AbstractBigInteger<*>) return BigInt(init.sign, init.ints, init.exp)
            val sign = init.sign
            val ints = UIntArray(init.size.toInt()) { init.get(it) }
            return BigInt(sign, ints, 0uL).normalize()
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
