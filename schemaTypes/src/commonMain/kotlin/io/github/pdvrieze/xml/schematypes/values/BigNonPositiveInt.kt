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
class BigNonPositiveInt internal constructor(ints: UIntArray, exp: ULong) :
    AbstractBigNonPositiveInt<BigNonPositiveInt>(ints, exp), XsdNonPositiveInteger {

    constructor(value: Int, exp: ULong = 0uL) : this(
        uintArrayOf(value.let {
            require(it <= 0) { "Integer must be non-positive: $it" }
            it.absoluteValue.toUInt()
        }),
        exp
    )

    constructor(value: Long, exp: ULong = 0uL) : this(
        value.let {
            require(it <= 0L) { "Integer must be non-positive: $it" }
            val f = it.absoluteValue.toULong()
            uintArrayOf(f.toUInt(), (f shr 32).toUInt())
        },
        exp
    )

    constructor(str: String) : this(parse(str))

    constructor(orginal: XsdNonPositiveInteger) : this(convert(orginal))

    private constructor(r: ParseResult) : this(r.ints, r.exp)

    override val self: BigNonPositiveInt get() = this

    override fun newInstance(sign: Int, elems: UIntArray, exp: ULong): BigNonPositiveInt {
        require(sign >=0) { "Unsigned integers may not have a negative sign: $sign"}
        return BigNonPositiveInt(elems, exp)
    }

    override fun newInstance(value: Long): BigNonPositiveInt {
        return BigNonPositiveInt(value)
    }

    /**
     * Ensure the instance uses maximum exponent and minimum ints.
     */
    @ExperimentalXmlUtilApi
    fun normalize(): BigNonPositiveInt = createOptimizedInstance(ints, exp)

    override val sign: Int
        get() = if (ints.size == 1 && ints[0] == 0u) 0 else -1

    override fun unaryMinus(): BigUnsignedInt {
        return BigUnsignedInt(ints, exp)
    }

    fun div(divider: XsdNonNegativeInteger): BigNonPositiveInt {
        val unsignedQuotient = abs().div(divider)
        return BigNonPositiveInt(unsignedQuotient.ints, unsignedQuotient.exp)
    }

    override fun div(divider: BigNonPositiveInt): BigUnsignedInt {
        return divRem(divider).quotient
    }

    override fun divRem(divider: UInt): IntDivRem {
        val absDivRem = abs().divRem(divider)
        return IntDivRem(absDivRem.quotient.unaryMinus(), -absDivRem.remainder.toInt())
    }

    override fun divRem(divider: XsdNonNegativeInteger): NegDivRem {
        val absDivRem = abs().divRem(divider)
        val q = absDivRem.quotient.unaryMinus()
        val r: BigNonPositiveInt = absDivRem.remainder.unaryMinus()

        return NegDivRem(q, r)
    }

    override fun divRem(divider: XsdNonPositiveInteger): PosDivRem {
        return divRem(BigNonPositiveInt(divider))
    }

    override fun divRem(divider: BigNonPositiveInt): PosDivRem {
        val absDivRem = abs().divRem(divider.abs())
        return PosDivRem(absDivRem.quotient.asBigUnsignedInt(), BigNonPositiveInt(absDivRem.remainder.ints, absDivRem.remainder.exp))
    }

    override fun times(other: XsdInteger): XsdInteger = when (other) {
        is XsdNonPositiveInteger -> times(other)
        else -> BigInt(other).times(this)
    }

    override fun times(other: XsdNonPositiveInteger): XsdNonNegativeInteger {
        when {
            sign == 0 || other.sign == 0 -> return BigUnsignedInt.ZERO
            else -> return abs().times(other.abs())
        }
    }

    override fun plus(other: XsdInteger): XsdInteger {
        return super<AbstractBigNonPositiveInt>.plus(other)
    }

    data class IntDivRem(
        override val quotient: BigNonPositiveInt,
        override val remainder: Int
    ) : DivRem<BigNonPositiveInt, Int>

    data class PosDivRem(
        override val quotient: BigUnsignedInt,
        override val remainder: BigNonPositiveInt
    ) : DivRem<BigUnsignedInt, BigNonPositiveInt> {
        constructor(orig: BigUnsignedInt.PosDivRem) : this(
            orig.quotient,
            orig.remainder.unaryMinus()
        )
    }

    data class NegDivRem(
        override val quotient: BigNonPositiveInt,
        override val remainder: BigNonPositiveInt
    ) : DivRem<BigNonPositiveInt, BigNonPositiveInt>

    companion object {

        val ZERO: BigNonPositiveInt get() = XsdNonPositiveInteger.ZERO
        val MINUSONE: BigNonPositiveInt get() = XsdNonPositiveInteger.MINUSONE

        operator fun invoke(init: AbstractBigNonPositiveInt<*>): BigNonPositiveInt {
            return init as? BigNonPositiveInt ?: BigNonPositiveInt(init.ints, init.exp)
        }

    }
}
