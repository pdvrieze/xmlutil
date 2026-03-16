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

@ExperimentalUnsignedTypes
class BigUnsignedInt internal constructor(ints: UIntArray, exp: ULong): AbstractBigUnsignedInt<BigUnsignedInt>(ints, exp) {

    constructor(value: UInt, exp: ULong = 0uL) : this(
        uintArrayOf(value),
        exp
    )

    constructor(value: ULong, exp: ULong = 0uL) : this(
        if (false && value.shr(32) == 0uL) uintArrayOf(value.toUInt()) else uintArrayOf(
            value.toUInt(),
            (value shr 32).toUInt()
        ),
        exp
    )

    constructor(str: String) : this(parse(str))

    constructor(orginal: XsdNonNegativeInteger) : this(convert(orginal))

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
    fun normalize(): BigUnsignedInt = createOptimizedInstance(ints, exp)

    override val sign: Int
        get() = if (ints.isEmpty() && ints[0] == 0u) 0 else 1

    override fun div(divider: XsdNonNegativeInteger): BigUnsignedInt = when (divider) {
        is AbstractBigUnsignedInt<*> -> divRem(divider.asBigUnsignedInt()).quotient
        is XsdUnsignedLong -> divRem(BigUnsignedInt(divider.toULong())).quotient
        is XsdUnsignedInt -> divRem(divider.toUInt()).quotient
        else -> divRem(BigUnsignedInt(divider)).quotient
    }

    override fun divRem(divider: XsdNonNegativeInteger): DivRem {
        return when (divider) {
            is AbstractBigUnsignedInt<*> -> unsignedDivRem(divider.asBigUnsignedInt())
            is XsdUnsignedLong -> unsignedDivRem(BigUnsignedInt(divider.toULong()))
            is XsdUnsignedInt -> unsignedDivRem(divider.toUInt()).toDivRem()
            else -> unsignedDivRem(BigUnsignedInt(divider))
        }
    }

    override fun divRem(divider: BigUnsignedInt): DivRem {
        return unsignedDivRem(divider)
    }

    override fun divRem(divider: UInt): UIntDivRem {
        return unsignedDivRem(divider)
    }

    override operator fun times(other: XsdNonNegativeInteger): BigUnsignedInt {
        return times(other as? BigUnsignedInt ?: BigUnsignedInt(other))
    }

    data class UIntDivRem(
        override val quotient: BigUnsignedInt,
        override val remainder: UInt
    ) : AbstractBigInteger.DivRem<BigUnsignedInt, UInt> {
        fun toDivRem(): DivRem {
            return DivRem(quotient, BigUnsignedInt(remainder))
        }
    }

    data class DivRem(
        override val quotient: BigUnsignedInt,
        override val remainder: BigUnsignedInt
    ) : AbstractBigInteger.DivRem<BigUnsignedInt, BigUnsignedInt>

    companion object {

        val ZERO = BigUnsignedInt(uintArrayOf(0u), 0uL)
        val ONE = BigUnsignedInt(uintArrayOf(1u), 0uL)

    }
}


