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

import io.github.pdvrieze.xml.schematypes.types.PositiveIntegerType
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert

@ExperimentalUnsignedTypes
class BigPositiveInt internal constructor(ints: UIntArray, exp: ULong): AbstractBigUnsignedInt<BigPositiveInt>(ints, exp), XsdPositiveInteger {

    init {
        if (ints.size == 1 && ints[0] == 0u) { throw NumberFormatException("Positive may not contain the value zero")}
        assert(ints.isNotEmpty()) { "At least one integer must be present" }
    }

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

    override val schemaType: PositiveIntegerType<XsdPositiveInteger>
        get() = PositiveIntegerType.Instance

    override val self: BigPositiveInt get() = this

    override fun asBigUnsignedInt(): BigUnsignedInt = BigUnsignedInt(ints, exp)

    override fun newInstance(sign: Int, elems: UIntArray, exp: ULong): BigPositiveInt {
        require(sign >=1) { "Unsigned integers may not have a non-positive sign: $sign"}
        return BigPositiveInt(elems, exp)
    }

    override fun newInstance(baseValue: ULong): BigPositiveInt {
        return BigPositiveInt(baseValue)
    }

    override val sign: Int
        get() = if (ints.isEmpty() && ints[0] == 0u) 0 else 1

    override fun divRem(divider: XsdNonNegativeInteger): DivRem {
        return when (divider) {
            is BigPositiveInt -> divRem(divider)
            is AbstractBigUnsignedInt<*> -> divRem(divider.asBigUnsignedInt())
            is XsdUnsignedLong -> divRem(BigPositiveInt(divider.toULong()))
            is XsdUnsignedInt -> DivRem(divRem(divider.toUInt()))
            else -> divRem(BigPositiveInt(divider))
        }
    }

    override fun divRem(divider: BigPositiveInt): DivRem {
        return DivRem(unsignedDivRem(divider))
    }

    override fun divRem(divider: UInt): UIntDivRem {
        return UIntDivRem(unsignedDivRem(divider))
    }

    override fun times(other: XsdNonNegativeInteger): XsdNonNegativeInteger {
        return when (other) {
            is BigPositiveInt -> times(other)
            else -> other.times(this)
        }
    }

    override operator fun times(other: XsdPositiveInteger): BigPositiveInt {
        return times(other as? BigPositiveInt ?: BigPositiveInt(other))
    }

    class UIntDivRem(
        override val quotient: BigPositiveInt,
        override val remainder: UInt
    ) : AbstractBigInteger.DivRem<BigPositiveInt, UInt> {
        constructor(orig: BigUnsignedInt.UIntDivRem) :
                this(BigPositiveInt(orig.quotient), orig.remainder)
    }

    class DivRem(
        override val quotient: BigPositiveInt,
        override val remainder: BigUnsignedInt
    ) : AbstractBigInteger.DivRem<BigPositiveInt, BigUnsignedInt> {
        constructor(orig: BigUnsignedInt.DivRem): this(BigPositiveInt(orig.quotient), orig.remainder)

        constructor(base: UIntDivRem): this(base.quotient, BigUnsignedInt(base.remainder))
    }

    companion object {
        val ONE = BigPositiveInt(uintArrayOf(1u), 0uL)
    }
}


