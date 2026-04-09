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

import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import kotlin.math.absoluteValue

@ExperimentalUnsignedTypes
class BigNegativeInt internal constructor(ints: UIntArray, exp: ULong) :
    AbstractBigNonPositiveInt<BigNegativeInt>(ints, exp), XsdNegativeInteger {

    init {
        require(ints.isNotEmpty()) { "At least one integer must be present" }
        if (ints[0] ==0u) throw NumberFormatException("Negative may not contain the value zero")
    }

    constructor(value: Int, exp: ULong = 0uL) : this(
        uintArrayOf(value.let {
            require(it < 0) { "Integer must be negative: $it" }
            it.absoluteValue.toUInt()
        }),
        exp
    )

    constructor(value: Long, exp: ULong = 0uL) : this(
        value.let {
            require(it < 0L) { "Integer must be negative: $it" }
            val f = it.absoluteValue.toULong()
            uintArrayOf(f.toUInt(), (f shr 32).toUInt())
        },
        exp
    )

    constructor(str: String) : this(parse(str))

    constructor(orginal: XsdNegativeInteger) : this(convert(orginal))

    private constructor(r: ParseResult) : this(r.ints, r.exp)

    override val self: BigNegativeInt get() = this

    override fun newInstance(sign: Int, elems: UIntArray, exp: ULong): BigNegativeInt {
        require(sign >=0) { "Unsigned integers may not have a negative sign: $sign"}
        return BigNegativeInt(elems, exp)
    }

    override fun newInstance(value: Long): BigNegativeInt {
        return BigNegativeInt(value)
    }

    /**
     * Ensure the instance uses maximum exponent and minimum ints.
     */
    @ExperimentalXmlUtilApi
    fun normalize(): BigNegativeInt = createOptimizedInstance(ints, exp)

    override val sign: Int get() = -1

    override fun unaryMinus(): BigPositiveInt = BigPositiveInt(ints, exp)

    override fun unaryPlus(): BigNegativeInt = this

    override fun div(divider: BigNegativeInt): BigUnsignedInt {
        return divRem(divider).quotient
    }


    override fun divRem(divider: UInt): IntDivRem {
        val absDivRem = abs().divRem(divider)
        return IntDivRem(absDivRem.quotient.unaryMinus(), -absDivRem.remainder.toInt())
    }

    override fun divRem(divider: XsdNonNegativeInteger): NegDivRem {
        val absDivRem = abs().divRem(divider)
        val q = absDivRem.quotient.unaryMinus()
        val r = absDivRem.remainder.unaryMinus()

        return NegDivRem(q, r)
    }

    override fun divRem(other: XsdDecimal): XsdDecimal.DivRem = when (other) {
        is XsdInteger -> divRem(other)
        else -> toBigDecimal().divRem(other)
    }

    override fun divRem(divider: XsdInteger): XsdInteger.DivRem = when (divider) {
        is XsdNonPositiveInteger -> divRem(divider)
        else -> toBigInt().divRem(divider)
    }

    override fun divRem(divider: XsdNonPositiveInteger): PosDivRem {
        val absDivRem = abs().divRem(divider.abs())
        return PosDivRem(absDivRem.quotient.asBigUnsignedInt(), BigNonPositiveInt(absDivRem.remainder.ints, absDivRem.remainder.exp))
    }

    override fun divRem(divider: BigNegativeInt): PosDivRem {
        val absDivRem = abs().divRem(divider.abs())
        return PosDivRem(absDivRem.quotient.asBigUnsignedInt(), BigNonPositiveInt(absDivRem.remainder.ints, absDivRem.remainder.exp))
    }

    override fun times(other: XsdInteger): XsdInteger = when (other) {
        is XsdNegativeInteger -> times(other)
        is XsdNonPositiveInteger -> times(other)
        else -> BigInt(other).times(this)
    }

    /** Note that neither value is zero, and both are negative, so the result must be positive */
    override fun times(other: XsdNegativeInteger): XsdPositiveInteger {
        val t = times(other as XsdNonPositiveInteger)
        return BigPositiveInt(t.ints, t.exp)
    }

    override fun times(other: XsdNonPositiveInteger): BigUnsignedInt {
        when {
            sign == 0 || other.sign == 0 -> return BigUnsignedInt.ZERO
            else -> return abs().times(other.abs())
        }
    }

    override fun plus(other: XsdNegativeInteger): XsdNegativeInteger {
        val t = abs().plus(other.abs())
        return BigNegativeInt(t.ints, t.exp)
    }

    override fun plus(other: XsdInteger): XsdInteger {
        return super<AbstractBigNonPositiveInt>.plus(other)
    }

    override fun compareTo(other: XsdNegativeInteger): Int {
        return -(abs().compareTo(other.abs()))
    }

    data class IntDivRem(
        override val quotient: BigNonPositiveInt,
        override val remainder: Int
    ) : DivRem<BigNonPositiveInt, Int>

    data class PosDivRem(
        override val quotient: BigUnsignedInt,
        override val remainder: BigNonPositiveInt
    ) : DivRem<BigUnsignedInt, BigNonPositiveInt>, XsdInteger.DivRem {
        constructor(orig: BigUnsignedInt.PosDivRem) : this(
            orig.quotient,
            orig.remainder.unaryMinus()
        )
    }

    data class NegDivRem(
        override val quotient: BigNonPositiveInt,
        override val remainder: BigNonPositiveInt
    ) : DivRem<BigNonPositiveInt, BigNonPositiveInt>, XsdInteger.DivRem

    companion object {

        val MINUSONE: BigNegativeInt get() = XsdNegativeInteger.MINUSONE as BigNegativeInt

        operator fun invoke(init: AbstractBigNonPositiveInt<*>): BigNegativeInt {
            return init as? BigNegativeInt ?: BigNegativeInt(init.ints, init.exp)
        }

    }
}
