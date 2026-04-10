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
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert

@OptIn(ExperimentalUnsignedTypes::class)
class BigUnsignedInt internal constructor(ints: UIntArray, exp: ULong): AbstractBigUnsignedInt<BigUnsignedInt>(ints, exp) {

    init {
        if (ints.size > 1 || exp!=0uL) {
            require(ints.any { it != 0u }) { "Zero values must be represented as a single int" }
        }
    }

    constructor(value: UInt, exp: ULong = 0uL) : this(
        uintArrayOf(value),
        exp
    )

    constructor(value: ULong, exp: ULong = 0uL) : this(
        ints = when {
            value < UInt.MAX_VALUE.toULong() -> uintArrayOf(value.toUInt())
            else -> uintArrayOf(value.toUInt(), (value shr 32).toUInt())
        },
        exp
    )

    constructor(str: String) : this(parse(str))

    private constructor(r: ParseResult) : this(r.ints, r.exp)

    override val self: BigUnsignedInt get() = this
    override fun asBigUnsignedInt(): BigUnsignedInt = this

    override fun newInstance(sign: Int, elems: UIntArray, exp: ULong): BigUnsignedInt {
        require(sign >=0) { "Unsigned integers may not have a negative sign: $sign"}
        return BigUnsignedInt(elems, exp)
    }

    override fun newInstance(baseValue: ULong): BigUnsignedInt {
        return BigUnsignedInt(baseValue)
    }

    /**
     * Ensure the instance uses maximum exponent and minimum ints.
     */
    @ExperimentalXmlUtilApi
    fun normalize(): BigUnsignedInt {
        val trailingBits = countTrailingZeroBits()
        val leadingBits = countLeadingZeroBits()
        if (trailingBits >= 3uL || (trailingBits+leadingBits > 32u)) {
            return createOptimizedInstance(ints, exp)
        }
        return this
    }

    override val sign: Int
        get() = if (ints.size == 1 && ints[0] == 0u) 0 else 1

    override fun plus(other: XsdNonNegativeInteger): BigUnsignedInt {
        if (other is BigUnsignedInt) { return plus(other) }
        return plus(BigUnsignedInt(other))
    }

    override fun unaryMinus(): BigNonPositiveInt = BigNonPositiveInt(ints, exp)
    override fun unaryPlus(): BigUnsignedInt = this

    override fun div(divider: XsdNonNegativeInteger): BigUnsignedInt = when (divider) {
        is AbstractBigUnsignedInt<*> -> divRem(divider.asBigUnsignedInt()).quotient
        is XsdUnsignedLong -> divRem(BigUnsignedInt(divider.toULong())).quotient
        is XsdUnsignedInt -> divRem(divider.toUInt()).quotient
        else -> divRem(BigUnsignedInt(divider)).quotient
    }

    fun div(divider: UInt, targetExp: ULong): BigUnsignedInt {
        return divRem(divider, targetExp).quotient
    }

    override fun divRem(divider: XsdDecimal): XsdDecimal.DivRem = when (divider) {
        is XsdNonNegativeInteger -> divRem(divider)
        is XsdUnsignedInt -> divRem(divider.toUInt()).toDivRem()
        is XsdInteger -> toBigInt().divRem(divider)
        else -> toBigDecimal().divRem(divider)
    }

    override fun divRem(divider: XsdInteger): XsdInteger.DivRem = when (divider) {
        is XsdNonNegativeInteger -> divRem(divider)
        else -> toBigInt().divRem(divider)
    }

    override fun divRem(divider: XsdNonNegativeInteger): PosDivRem {
        return when (divider) {
            is AbstractBigUnsignedInt<*> -> unsignedDivRem(divider.asBigUnsignedInt())
            is XsdUnsignedLong -> unsignedDivRem(BigUnsignedInt(divider.toULong()))
            is XsdUnsignedInt -> unsignedDivRem(divider.toUInt()).toDivRem()
            else -> unsignedDivRem(BigUnsignedInt(divider))
        }
    }

    override fun divRem(divider: BigUnsignedInt): PosDivRem {
        return unsignedDivRem(divider)
    }

    override fun divRem(divider: ULong): PosDivRem {
        return unsignedDivRem(BigUnsignedInt(divider))
    }

    override fun divRem(divider: UInt): UIntDivRem {
        return unsignedDivRem(divider)
    }

    fun divRem(divider: UInt, targetExp: ULong): UIntDivRem {
        return unsignedDivRem(divider, targetExp.toLong())
    }

    override fun times(other: XsdInteger): XsdInteger = when (other) {
        is XsdNonNegativeInteger -> times(other)
        else -> BigInt(other).times(this)
    }

    override operator fun times(other: XsdNonNegativeInteger): BigUnsignedInt {
        return times(other as? BigUnsignedInt ?: BigUnsignedInt(other))
    }

    @XmlUtilInternal
    internal fun times(other: UInt, targetExp: ULong): BigUnsignedInt {
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

        val optInts = newInts.trimTrailingZeros()

        return when (targetExp) {
            exp -> BigUnsignedInt(optInts, exp)
            else -> BigUnsignedInt(optInts, exp).expandWithEffectiveExp(targetExp)
        }
    }

    class UIntDivRem(
        override val quotient: BigUnsignedInt,
        override val uintRemainder: UInt
    ) : DivRem<BigUnsignedInt, BigUnsignedInt>, AbstractBigUnsignedInt.UIntDivRem {
        override val remainder: BigUnsignedInt get() = BigUnsignedInt(uintRemainder)

        fun toDivRem(): PosDivRem {
            return PosDivRem(quotient, BigUnsignedInt(uintRemainder))
        }
    }

    data class PosDivRem(
        override val quotient: BigUnsignedInt,
        override val remainder: BigUnsignedInt
    ) : AbstractBigUnsignedInt.PosDivRem

    companion object {

        val ZERO = BigUnsignedInt(uintArrayOf(0u), 0uL)
        val ONE = BigUnsignedInt(uintArrayOf(1u), 0uL)

        operator fun invoke(value: XsdNonNegativeInteger): BigUnsignedInt = when (value) {
            is BigUnsignedInt -> value
            is AbstractBigUnsignedInt<*> -> value.asBigUnsignedInt()
            is XsdUnsignedLong -> BigUnsignedInt(value.toULong())
            is XsdUnsignedInt -> BigUnsignedInt(value.toUInt())
            else -> BigUnsignedInt(convert(value))
        }

    }
}


