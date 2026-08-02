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

import io.github.pdvrieze.xml.schematypes.impl.SimpleTypeSerializer
import io.github.pdvrieze.xml.schematypes.types.IntType
import io.github.pdvrieze.xml.schematypes.values.instances.BigDecimal
import io.github.pdvrieze.xml.schematypes.values.instances.XsdIntImpl
import io.github.pdvrieze.xml.schematypes.values.instances.xsToInt
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlUtilInternal
import kotlin.math.absoluteValue

@Serializable(XsdInt.Companion::class)
@XmlUtilInternal
interface XsdInt : XsdLong {

    override val schemaType: IntType<XsdInt>

    val intValue: Int
    override val longValue: Long get() = intValue.toLong()
    override fun toInt(): Int = intValue

    override fun toBigInt(): BigInt = BigInt(intValue)

    override fun toBigDecimal(): BigDecimal = BigDecimal(intValue)
    override fun toDouble(): Double = intValue.toDouble()
    override fun toFloat(): Float = intValue.toFloat()

    override val size: ULong get() = 1uL

    override val sign: Int get() = intValue.compareTo(0)

    override fun abs(): XsdUnsignedInt

    override fun plus(other: Int): XsdInt = XsdInt(intValue + other)

    override fun plus(other: UInt): XsdInt = XsdInt(intValue + other.toInt())

    override fun minus(other: Int): XsdInt {
        return XsdInt(intValue - other)
    }

    override fun minus(other: UInt): XsdInt {
        return XsdInt(intValue - other.toInt())
    }

    override fun times(multiplier: Int): XsdInt {
        return XsdInt(intValue * multiplier)
    }

    override fun times(multiplier: UInt): XsdInt {
        return XsdInt(intValue - multiplier.toInt())
    }

    override fun div(other: XsdLong): XsdLong = when (other) {
        is XsdInt -> div(other)
        else -> XsdLong(longValue / other.longValue)
    }

    operator fun div(other: XsdInt): XsdInt = XsdInt(intValue / other.intValue)

    override fun rem(other: XsdLong): XsdLong = when (other) {
        is XsdInt -> rem(other)
        else -> XsdLong(longValue % other.longValue)
    }

    operator fun rem(other: XsdInt): XsdInt = XsdInt(intValue % other.intValue)

    override fun divRem(other: XsdLong): XsdLong.DivRem = when (other) {
        is XsdInt -> divRem(other)
        else -> XsdLong(longValue).divRem(other)
    }

    fun divRem(other: XsdInt): DivRem {
        return DivRem(intValue / other.intValue, intValue % other.intValue)
    }

    override fun get(index: Int): UInt {
        if (index != 0) throw IndexOutOfBoundsException("Index $index out of bounds")
        return intValue.absoluteValue.toUInt()
    }

    override fun get(index: ULong): UInt {
        if (index != 0uL) throw IndexOutOfBoundsException("Index $index out of bounds")
        return intValue.absoluteValue.toUInt()
    }

    operator fun rangeTo(other: XsdInt): XsdIntegerProgression<XsdInt> {
        return XsdIntImpl.Range(intValue, other.intValue)
    }

    override fun rangeTo(other: XsdLong): XsdIntegerProgression<XsdLong> = when {
        other is XsdInt -> rangeTo(other)
        other.longValue in Int.MIN_VALUE..Int.MAX_VALUE ->
            rangeTo(XsdInt(other.toInt()))

        else -> XsdLong(longValue).rangeTo(other)
    }

    override operator fun rangeTo(other: XsdInteger): XsdIntegerProgression<XsdInteger> = when (other) {
        is XsdInt -> rangeTo(other)
        else -> super.rangeTo(other)
    }

    override fun countTrailingZeroBits(): ULong {
        return intValue.countTrailingZeroBits().toULong()
    }


    override fun significantBitsFromZero(): ULong {
        return 32u - intValue.countLeadingZeroBits().toULong()
    }

    data class DivRem(
        override val quotient: XsdInt,
        override val remainder: XsdInt
    ) : XsdLong.DivRem {
        constructor(quotient: Int, remainder: Int): this(XsdInt(quotient), XsdInt(remainder))
    }

    companion object : SimpleTypeSerializer<XsdInt>("xsd.int") {
        val ZERO = XsdInt(0)
        val ONE = XsdInt(1)

        operator fun invoke(value: Int): XsdInt = XsdIntImpl(value)

        operator fun invoke(value: CharSequence): XsdInt = XsdIntImpl(value.xsToInt())

        override fun deserialize(raw: String, input: XmlReader?): XsdInt {
            return XsdIntImpl(raw.xsToInt())
        }
    }

}
