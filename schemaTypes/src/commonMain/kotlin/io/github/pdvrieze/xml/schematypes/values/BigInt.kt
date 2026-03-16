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
import kotlin.math.absoluteValue

@OptIn(ExperimentalUnsignedTypes::class)
class BigInt internal constructor(override val sign: Int, ints: UIntArray, exp: ULong) :
    AbstractBigInteger<BigInt>(ints, exp), XsdInteger {
    init {
        require(ints.isNotEmpty()) { "At least one integer must be present" }
        require(sign in -1..1) { "Invalid sign" }
    }

    private constructor(r: ParseResult): this(r.sign, r.ints, r.exp)

    constructor(str: String): this(parse(str))

    constructor(int: Int) : this(
        int.compareTo(0),
        uintArrayOf(int.absoluteValue.toUInt()),
        0uL
    )

    constructor(long: Long) : this(
        long.compareTo(0L),
        long.absoluteValue.toULong().let { uintArrayOf(it.toUInt(), (it shr 32).toUInt()) },
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

    override val schemaType: IntegerType<XsdInteger>
        get() = IntegerType.Instance

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

    override fun divRem(divider: BigInt): AbstractBigInteger.DivRem<BigInt, BigInt> {
        val finalSign = when {
            divider.sign < 0 -> -sign
            divider.sign == 0 -> throw ArithmeticException("Division by zero")
            else -> sign
        }
        val base = unsignedDivRem(divider)
        return DivRem(
            quotient = BigInt(finalSign, base.quotient.ints, base.quotient.exp),
            remainder = BigInt(sign, base.remainder.ints, base.remainder.exp),
        )

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

    override fun equals(other: Any?): Boolean {
        return compareTo(other as? XsdInteger ?: return false) == 0
    }

    class DivRem(
        override val quotient: BigInt,
        override val remainder: BigInt
    ): AbstractBigInteger.DivRem<BigInt, BigInt>

    private class ParseResult(val sign: Int, val ints: UIntArray, val exp: ULong)

    companion object {
        public val ZERO: BigInt = BigInt(0, uintArrayOf(0u), 0uL)

        private fun parse(s: String): ParseResult {
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
            val sign = if (isNegative) -1 else if (base.ints.size == 1 && base.ints[0] == 0u) 0 else 1
            return ParseResult(sign, base.ints, base.exp)
        }
    }
}
