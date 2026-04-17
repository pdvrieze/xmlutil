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
class InfBigDecimal(ints: UIntArray, decimalPositions: Int) :
    AbstractBigDecimal<InfBigDecimal>(ints, decimalPositions) {

    private constructor(parseResult: ParseResult) : this(parseResult.ints, parseResult.decimalDigits)

    constructor(value: CharSequence): this(parse(value))

    override val self: InfBigDecimal get() = this

    override val isNaN: Boolean
        get() = ints[0] and SPECIAL_BIT.toUInt() != 0u
                && (ints[0] and NAN_BIT.toUInt()) == 1u

    override val isInfinity: Boolean
        get() = ints[0] and (SPECIAL_MASK.toUInt()) == (INFINITY_BIT or SPECIAL_BIT).toUInt()

    override val isNegativeInfinity: Boolean
        get() = ints[0] and (SPECIAL_MASK.toUInt()) == (SIGN_BIT or INFINITY_BIT or SPECIAL_BIT).toUInt()


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

    override fun toDouble(): Double = when (ints[0] shr 28) {
        in 0b0000u.. 0b0011u, in 0b1000u..0b1011u -> super.toDouble()
        0b0110u, 0b0111u, 0b1110u, 0b1111u -> Double.NaN
        0b0101u -> Double.POSITIVE_INFINITY
        else -> Double.NEGATIVE_INFINITY
    }

    override fun createOptimizedInstance(
        elems: UIntArray,
        exp: Int
    ): InfBigDecimal = when (sign ushr 29) {
        0b001 -> POSITIVE_INFINITY
        0b110 -> NEGATIVE_INFINITY
        0b000, 0b111 -> super.createOptimizedInstance(elems, exp)
        else -> newInstance(NaN.ints, NaN.exponent)
    }

    override fun roundToInteger(): XsdInteger {
        if (!isFinite) throw IllegalStateException("INF/NaN cannot be converted to an integer")
        return super.roundToInteger()
    }

    override fun XsdDecimal.asT(): InfBigDecimal = when (this) {
        is InfBigDecimal -> this
        is AbstractBigDecimal<*> -> InfBigDecimal(ints, exponent)
        is XsdInteger -> InfBigDecimal(this)
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
        return when {
            divider.sign == 0 -> throw ArithmeticException("Division by zero")
            isFinite -> super.divRem(divider)
            sign == 0 -> DivRem(self, ZERO)
            isNaN || divider.isNaN -> DivRem(self, NaN)
            !divider.isFinite -> DivRem(NaN, NaN)
            sign > 0 == divider.sign > 0 -> DivRem(self, NaN)
            else -> DivRem(NEGATIVE_INFINITY, ZERO)
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
        val int0 = ints[0]
        when {
            int0 == 0u -> appendable.append('0')
            int0 and SPECIAL_BIT.toUInt() != 0u -> when {
                int0 and NAN_BIT.toUInt() != 0u -> appendable.append("NaN")
                int0 and SIGN_BIT.toUInt() != 0u -> appendable.append("-INF")
                else -> appendable.append("INF")
            }

            else -> super.appendTo(appendable)
        }
    }

    companion object: CompanionBase<InfBigDecimal>() {
        override val ZERO = InfBigDecimal(uintArrayOf(0u), 0)
        override val ONE = InfBigDecimal(uintArrayOf(1u), 0)
        override val MINUSONE = InfBigDecimal(uintArrayOf(1u or SIGN_BIT.toUInt()), 0)
        val NaN = InfBigDecimal(uintArrayOf((SPECIAL_BIT or NAN_BIT).toUInt()), 0)
        val POSITIVE_INFINITY = InfBigDecimal(uintArrayOf((SPECIAL_BIT or INFINITY_BIT).toUInt()), 0)
        val NEGATIVE_INFINITY = InfBigDecimal(uintArrayOf((SPECIAL_BIT or INFINITY_BIT or SIGN_BIT).toUInt()), 0)

        override fun newInstance(ints: UIntArray, decimalPositions: Int): InfBigDecimal {
            return InfBigDecimal(ints, decimalPositions)
        }

        override fun parseNormalised(
            normalised: CharSequence,
            sign: Int
        ): ParseResult {
            if (normalised == "INF") {
                val ref = if (sign > 0) POSITIVE_INFINITY else NEGATIVE_INFINITY

                return ParseResult(ref.ints, ref.exponent)
            } else if (normalised == "NaN") {
                return ParseResult(NaN.ints, NaN.exponent)
            }

            return super.parseNormalised(normalised, sign)
        }
    }

}
