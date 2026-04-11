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

import io.github.pdvrieze.xml.schematypes.values.XsdDecimal
import io.github.pdvrieze.xml.schematypes.values.XsdInteger

@OptIn(ExperimentalUnsignedTypes::class)
class InfBigDecimal(sign: Int, ints: UIntArray, decimalPositions: Long) :
    AbstractBigDecimal<InfBigDecimal>(sign, ints, decimalPositions) {

    private constructor(parseResult: ParseResult) : this(parseResult.sign, parseResult.ints, parseResult.decimalDigits)

    constructor(value: CharSequence): this(parse(value))

    override val self: InfBigDecimal get() = this

    override val isNaN: Boolean
        get() = ((_sign and SIGN_BIT).ushr(1) xor (_sign and NAN_BIT)) != 0

    override val isInfinity: Boolean
        get() = sign and (SPECIAL_MASK) == INFINITY_BIT

    override val isNegativeInfinity: Boolean
        get() = sign xor (SPECIAL_MASK) == INFINITY_BIT


    override val companion: Companion get() = Companion

    override fun toULong(): ULong = when {
        isFinite -> super.toULong()
        isInfinity -> ULong.MAX_VALUE
        else -> throw IllegalStateException("NaN cannot be converted to an unsigned long")
    }


    override fun toUInt(): UInt = when {
        isFinite -> super.toUInt()
        isInfinity -> UInt.MAX_VALUE
        else -> throw IllegalStateException("NaN cannot be converted to an unsigned int")
    }

    override fun toLong(): Long = when {
        isFinite -> super.toLong()
        isInfinity -> Long.MAX_VALUE
        isNegativeInfinity -> Long.MIN_VALUE
        else -> throw IllegalStateException("NaN cannot be converted to a long")
    }

    override fun toInt(): Int {
        return when {
            isFinite -> super.toInt()
            isInfinity -> Int.MAX_VALUE
            isNegativeInfinity -> Int.MIN_VALUE
            else -> throw IllegalStateException("NaN cannot be converted to an int")
        }
    }

    override fun toDouble(): Double = when (_sign ushr 29) {
        0b000, 0b111 -> super.toDouble()
        0b001 -> Double.POSITIVE_INFINITY
        0b110 -> Double.NEGATIVE_INFINITY
        else -> Double.NaN
    }

    override fun createOptimizedInstance(
        sign: Int,
        elems: UIntArray,
        decimalPositions: Long
    ): InfBigDecimal = when (sign ushr 29) {
        0b001 -> POSITIVE_INFINITY
        0b110 -> NEGATIVE_INFINITY
        0b000, 0b111 -> super.createOptimizedInstance(sign, elems, decimalPositions)
        else -> newInstance(sign, NaN.ints, NaN.decimalPositions)
    }

    override fun roundToInteger(): XsdInteger {
        if (!isFinite) throw IllegalStateException("INF/NaN cannot be converted to an integer")
        return super.roundToInteger()
    }

    override fun XsdDecimal.asT(): InfBigDecimal = when (this) {
        is InfBigDecimal -> this
        is AbstractBigDecimal<*> -> InfBigDecimal(sign, ints, decimalPositions)
        is XsdInteger -> InfBigDecimal(sign, UIntArray(size.toInt()) { get(it) }, 0L)
        else -> InfBigDecimal(xmlString) // fallback to parsing
    }

    override fun plus(other: AbstractBigDecimal<*>): InfBigDecimal = when {
        !isFinite -> when {
            isNaN -> self
            other.isNaN -> other.asT()
            // one infinity but we don't know the sign
            // if the sign is equal, the result is infinity
            sign == other.sign -> self // keep infinity if the sign is the same
            // different sign, one must be infinite, the other finite
            other.isFinite -> self
            // -Inf + INF is NaN
            else -> NaN
        }

        other.isFinite -> super.plus(other)

        other.isNaN -> other.asT()
        // if the sign is equal, the result is infinity
        sign == other.sign -> self
        // different sign, one must be infinite, the other finite
        else -> other.asT()
    }

    override fun minus(other: AbstractBigDecimal<*>): InfBigDecimal = when {
        !isFinite -> when {
            isNaN -> self
            other.isNaN -> other.asT()
            // one infinity but we don't know the sign
            // if the sign is equal, the result is infinity
            sign == -other.sign -> self // keep infinity if the sign is the same
            // different sign, one must be infinite, the other finite
            other.isFinite -> self
            // -Inf + INF is NaN
            else -> NaN
        }

        other.isFinite -> super.minus(other)

        other.isNaN -> other.asT()
        // one infinity but we don't know the sign
        // if the sign is equal, the result is infinity
        sign == -other.sign -> self // keep infinity if the sign is the same
        // different sign, one must be infinite, the other finite
        else -> -(other.asT()) // opposite sign, unary minus is efficient
    }

    override fun times(other: AbstractBigDecimal<*>): InfBigDecimal {
        return when {
            !isFinite -> when {
                isNaN -> self
                other.isNaN -> other.asT()
                // one infinity but we don't know the sign
                // if the sign is equal, the result is infinity
                sign == other.sign -> self // keep infinity if the sign is the same

                other.isFinite -> self
                // -Inf * INF is NaN
                else -> NaN
            }

            other.isFinite -> super.times(other)

            other.isNaN -> other.asT()
            // one infinity but we don't know the sign
            // if the sign is equal, the result is infinity
            sign == other.sign -> self // keep infinity if the sign is the same
            // different sign, one must be infinite, the other finite
            else -> other.asT()
        }

    }

    override operator fun div(divider: AbstractBigDecimal<*>): InfBigDecimal {
        return when {
            isFinite && divider.isFinite -> super.divRem(divider).quotient.asT()
            isNaN -> self
            divider.isNaN -> divider.asT()

            // neither value is NaN so one is infinite

            // if we are not special the other must be infinite, and finite/infinite approaches 0
            isFinite -> ZERO

            // left is infinite, so answer always infinite Same sign will lead to pos infinity
            sign == divider.sign -> POSITIVE_INFINITY
            else -> NEGATIVE_INFINITY
        }
    }

    override fun divRem(divider: UInt): DivRem<InfBigDecimal> = when {
        divider == 0u -> throw ArithmeticException("Division by zero")
        isFinite -> super.divRem(divider)
        else -> DivRem(self, NaN)
    }

    override fun divRem(divider: InfBigDecimal): DivRem<InfBigDecimal> {
        // will (initially) expand exponents
        when {
            divider._sign == 0 -> throw ArithmeticException("Division by zero")
            isFinite -> return super.divRem(divider)
            _sign == 0 -> return DivRem(self, ZERO)
            isNaN || divider.isNaN -> return DivRem(self, NaN)
            !divider.isFinite -> return DivRem(NaN, NaN)
            sign > 0 == divider.sign > 0 -> return DivRem(self, NaN)
            else -> return DivRem(NEGATIVE_INFINITY, ZERO)
        }
    }

    override fun compareTo(other: XsdDecimal): Int {
        return when {
            isFinite -> super.compareTo(other)
            isNaN || other.isNaN -> throw ArithmeticException("NaN values are not comparable")
            isNegativeInfinity -> if (other.isNegativeInfinity) 0 else -1
            other.isInfinity -> 0
            else -> 1
        }
    }

    override fun appendTo(appendable: Appendable) {
        when (_sign) {
            0 -> appendable.append('0')

            -1, 1 -> super.appendTo(appendable)
            0x2000_0000 -> appendable.append("INF")
            0xDFFF_FFFFu.toInt() -> appendable.append("-INF")
            else -> appendable.append("NaN")
        }
    }

    companion object: CompanionBase<InfBigDecimal>() {
        override val ZERO = InfBigDecimal(0, uintArrayOf(0u), 0)
        override val ONE = InfBigDecimal(1, uintArrayOf(1u), 0)
        override val MINUSONE = InfBigDecimal(-1, uintArrayOf(1u), 0)
        val NaN = InfBigDecimal(NAN_BIT, UIntArray(0), 0)
        val POSITIVE_INFINITY = InfBigDecimal(INFINITY_BIT, NaN.ints, 0)
        val NEGATIVE_INFINITY = InfBigDecimal(-1 xor INFINITY_BIT, NaN.ints, 0)

        override fun newInstance(sign: Int, ints: UIntArray, decimalPositions: Long): InfBigDecimal {
            return InfBigDecimal(sign, ints, decimalPositions)
        }

        override fun parseNormalised(
            normalised: CharSequence,
            sign: Int
        ): ParseResult {
            if (normalised == "INF") {
                val ref = if (sign > 0) POSITIVE_INFINITY else NEGATIVE_INFINITY
                return ParseResult(ref._sign, ref.ints, ref.decimalPositions)
            } else if (normalised == "NaN") {
                return ParseResult(NaN._sign, NaN.ints, NaN.decimalPositions)
            }

            return super.parseNormalised(normalised, sign)
        }
    }

}
