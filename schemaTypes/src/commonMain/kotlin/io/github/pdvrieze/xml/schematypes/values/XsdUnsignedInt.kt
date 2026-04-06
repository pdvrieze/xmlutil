/*
 * Copyright (c) 2021-2026.
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
import io.github.pdvrieze.xml.schematypes.types.UnsignedIntType
import io.github.pdvrieze.xml.schematypes.values.instances.BigDecimal
import io.github.pdvrieze.xml.schematypes.values.instances.XsdLongImpl
import io.github.pdvrieze.xml.schematypes.values.instances.XsdUnsignedIntImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.xmlTrimWhitespace

@Serializable(XsdUnsignedInt.Companion::class)
interface XsdUnsignedInt : XsdUnsignedLong {

    override val schemaType: UnsignedIntType<XsdUnsignedInt>

    val uIntValue: UInt
    override val uLongValue: ULong
        get() = uIntValue.toULong()

    override fun unaryMinus(): XsdInt = XsdInt(-uIntValue.toInt())

    override fun toInt(): Int = uIntValue.toInt()

    override fun toLong(): Long = uIntValue.toLong()

    override fun toUInt(): UInt = uIntValue

    override fun toULong(): ULong = uIntValue.toULong()

    override fun toDouble(): Double = uIntValue.toDouble()

    override fun toFloat(): Float = uIntValue.toFloat()

    override fun toBigInt(): XsdNonNegativeInteger {
        return BigUnsignedInt(uIntValue)
    }

    override fun toBigDecimal(): BigDecimal = BigDecimal(uIntValue)

    override val size: ULong get() = 1uL

    override fun get(index: ULong): UInt = when (index) {
        0uL -> uIntValue
        else -> throw IndexOutOfBoundsException("Index $index out of bounds")
    }

    override fun get(index: Int): UInt = when (index) {
        0 -> uIntValue
        else -> throw IndexOutOfBoundsException("Index $index out of bounds")
    }

    override fun plus(other: XsdNonNegativeInteger): XsdNonNegativeInteger {
        if (other !is XsdUnsignedInt) return other.plus(this)
        return XsdUnsignedIntImpl(uIntValue + other.toUInt())
    }

    override fun plus(other: ULong): XsdUnsignedInt {
        return XsdUnsignedIntImpl(uIntValue + other.toUInt())
    }

    override fun times(other: XsdNonNegativeInteger): XsdNonNegativeInteger = when (other) {
        is XsdUnsignedInt -> XsdUnsignedIntImpl(uIntValue * other.uIntValue)
        else -> other.times(this)
    }

    override operator fun times(other: XsdUnsignedLong): XsdUnsignedLong = when (other) {
        is XsdUnsignedInt -> times(other.uIntValue) as XsdUnsignedLong
        else -> other.times(this)
    }

    override operator fun times(other: UInt): XsdUnsignedInt = XsdUnsignedIntImpl(uIntValue * other)
    operator fun times(other: XsdUnsignedInt): XsdUnsignedInt = times(other.uIntValue)

    override fun div(divider: XsdUnsignedLong): XsdUnsignedLong = when (divider) {
        is XsdUnsignedInt -> div(divider)
        else -> super.div(divider)
    }

    operator fun div(divider: XsdUnsignedInt): XsdUnsignedInt = XsdUnsignedIntImpl(uIntValue / divider.uIntValue)

    override fun divRem(divider: XsdUnsignedLong): XsdUnsignedLong.DivRem = when (divider) {
        is XsdUnsignedInt -> divRem(divider)
        else -> XsdUnsignedLong(uLongValue).divRem(divider)
    }

    fun divRem(divider: XsdUnsignedInt): DivRem = DivRem(uIntValue / divider.uIntValue, uIntValue % divider.uIntValue)

    override fun rem(divider: XsdUnsignedLong): XsdUnsignedLong = when (divider) {
        is XsdUnsignedInt -> rem(divider)
        else -> super.rem(divider)
    }

    operator fun rem(divider: XsdUnsignedInt): XsdUnsignedInt = XsdUnsignedIntImpl(uIntValue % divider.uIntValue)

    override fun abs(): XsdUnsignedInt = this

    override val sign: Int get() = if (uIntValue == 0u) 0 else 1

    override fun significantBitsFromZero(): ULong {
        return (64 - uLongValue.countLeadingZeroBits()).toULong()
    }

    override fun plus(other: XsdInteger): XsdInteger = when (other) {
        is XsdUnsignedInt -> XsdUnsignedIntImpl(uIntValue + other.uIntValue)
        is XsdInt -> XsdInt(uIntValue.toInt() + other.intValue)
        else -> other.plus(this)
    }

    override fun minus(other: XsdInteger): XsdInteger = when (other) {
        is XsdUnsignedInt -> XsdInt(uIntValue.toInt() - other.uIntValue.toInt())
        is XsdInt -> XsdInt(uIntValue.toInt() - other.intValue)
        is XsdUnsignedLong -> XsdLongImpl(uLongValue.toLong() - other.uLongValue.toLong())
        is XsdLong -> XsdLong(uLongValue.toLong() - other.longValue)
        else if (other.significantBitsFromZero() < 64u) -> XsdLongImpl(uLongValue.toLong() - other.toLong())
        else -> BigInt(other).minus(this)
    }

    override fun compareTo(other: XsdNonNegativeInteger): Int {
        if (other !is XsdUnsignedInt) return -other.compareTo(this)
        return uIntValue.compareTo(other.uIntValue)
    }

    data class DivRem(override val quotient: XsdUnsignedInt, override val remainder: XsdUnsignedInt) :
        XsdUnsignedLong.DivRem {

        constructor(quotient: UInt, remainder: UInt): this(
            XsdUnsignedIntImpl(quotient),
            XsdUnsignedIntImpl(remainder)
        )
    }

    companion object : SimpleTypeSerializer<XsdUnsignedInt>("xsd.unsignedInt") {
        val ZERO: XsdUnsignedInt = XsdUnsignedIntImpl(0u)

        override fun deserialize(raw: String, input: XmlReader?): XsdUnsignedInt {
            return XsdUnsignedIntImpl(xmlTrimWhitespace(raw).toUInt())
        }

        operator fun invoke(value: UInt): XsdUnsignedInt = XsdUnsignedIntImpl(value)

        operator fun invoke(value: CharSequence): XsdUnsignedInt = when (value.getOrNull(0) ?: throw NumberFormatException("Empty string is not a number")) {
            '-' -> if (value.length == 2 && value[1]=='0') ZERO else throw NumberFormatException("Negative numbers are not allowed")
            else -> XsdUnsignedIntImpl(value.toString().toUInt())
        }

    }

}

