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
import io.github.pdvrieze.xml.schematypes.types.UnsignedLongType
import io.github.pdvrieze.xml.schematypes.values.instances.BigDecimal
import io.github.pdvrieze.xml.schematypes.values.instances.XsdLongImpl
import io.github.pdvrieze.xml.schematypes.values.instances.XsdUnsignedLongImpl
import io.github.pdvrieze.xml.schematypes.values.instances.xsToULong
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.xmlTrimWhitespace

@ExperimentalXmlUtilApi
@Serializable(XsdUnsignedLong.Companion::class)
interface XsdUnsignedLong : XsdNonNegativeInteger {

    override val schemaType: UnsignedLongType<XsdUnsignedLong>

    val uLongValue: ULong

    override fun unaryMinus(): XsdLong

    override fun toDouble(): Double = uLongValue.toDouble()

    override fun toFloat(): Float = uLongValue.toFloat()

    override fun toLong(): Long = uLongValue.toLong()

    override fun toInt(): Int = uLongValue.toInt()

    override fun toULong(): ULong = uLongValue

    override fun toUInt(): UInt = uLongValue.toUInt()

    override fun toBigInt(): XsdNonNegativeInteger = BigUnsignedInt(uLongValue)

    override fun toBigDecimal(): BigDecimal = BigDecimal(uLongValue)

    override fun countTrailingZeroBits(): ULong {
        return uLongValue.countTrailingZeroBits().toULong()
    }

    override fun plus(other: XsdDecimal): XsdDecimal = when (other) {
        is XsdInteger -> plus(other)
        else -> other.plus(this)
    }

    override fun plus(other: XsdInteger): XsdInteger = when (other) {
        is XsdUnsignedLong -> XsdUnsignedLongImpl(uLongValue + other.uLongValue)
        is XsdLong -> XsdLong(uLongValue.toLong() + other.longValue)
        else -> other.plus(this)
    }

    override fun significantBitsFromZero(): ULong {
        return (64 - uLongValue.countLeadingZeroBits()).toULong()
    }

    override fun minus(other: XsdDecimal): XsdDecimal = when (other){
        is XsdInteger -> minus(other)
        else -> other.plus(unaryMinus())
    }

    override fun minus(other: XsdInteger): XsdInteger = when (other) {
        is XsdUnsignedLong -> XsdLongImpl(uLongValue.toLong() - other.uLongValue.toLong())
        is XsdLong -> XsdLong(uLongValue.toLong() - other.longValue)
        else if (other.significantBitsFromZero() < 64u) -> XsdLongImpl(uLongValue.toLong() - other.toLong())
        else -> BigInt(other).minus(this)
    }

    override val size: ULong
        get() = 2uL

    override fun get(index: ULong): UInt = when (index) {
        0uL -> uLongValue.toUInt()
        1uL -> uLongValue.shr(32).toUInt()
        else -> throw IndexOutOfBoundsException("Index $index out of bounds")
    }

    override fun get(index: Int): UInt = when (index) {
        0 -> uLongValue.toUInt()
        1 -> uLongValue.shr(32).toUInt()
        else -> throw IndexOutOfBoundsException("Index $index out of bounds")
    }

    override fun plus(other: XsdNonNegativeInteger): XsdNonNegativeInteger {
        if (other !is XsdUnsignedLong) return other.plus(this)
        return XsdUnsignedLongImpl(uLongValue + other.toULong())
    }

    override fun plus(other: ULong): XsdNonNegativeInteger {
        return XsdUnsignedLongImpl(uLongValue + other)
    }

    override fun times(other: XsdDecimal): XsdDecimal = when (other) {
        is XsdNonNegativeInteger -> times(other)
        else -> other.times(this)
    }
    override fun times(other: XsdInteger): XsdInteger = when (other) {
        is XsdUnsignedLong -> times(other)
        is XsdLong if other.longValue > 0L -> times(other.longValue.toULong())
        is XsdLong -> XsdLong(toLong()).times(other.toLong())
        else -> other.times(this)
    }

    operator fun times(other: XsdUnsignedLong): XsdUnsignedLong = XsdUnsignedLong(uLongValue * other.uLongValue)

    override fun times(other: XsdNonNegativeInteger): XsdNonNegativeInteger = when (other) {
        is XsdUnsignedLong -> times(other)
        else -> XsdNonNegativeInteger(uLongValue).times(other)
    }

    override fun div(other: XsdInteger): XsdInteger = when (other) {
        is XsdUnsignedLong -> div(other)
        is XsdLong -> XsdLong(toLong()) / other
        else -> super.div(other)
    }

    operator fun div(divider: XsdUnsignedLong): XsdUnsignedLong = XsdUnsignedLong(uLongValue / divider.uLongValue)

    override fun divRem(divider: XsdInteger): XsdInteger.DivRem = when (divider) {
        is XsdUnsignedLong -> divRem(divider)
        is XsdLong -> XsdLong(toLong()).divRem(divider)
        else -> toBigInt().divRem(divider)
    }

    fun divRem(divider: XsdUnsignedLong): DivRem

    override fun rem(divider: XsdInteger): XsdInteger = when (divider) {
        is XsdUnsignedLong -> rem(divider)
        is XsdLong -> XsdLong(toLong()).rem(divider)
        else -> super.rem(divider)
    }

    operator fun rem(divider: XsdUnsignedLong): XsdUnsignedLong = XsdUnsignedLong(uLongValue % divider.uLongValue)

    override val sign: Int get() = if (uLongValue == 0uL) 0 else 1

    override fun abs(): XsdUnsignedLong = this


    override fun compareTo(other: XsdNonNegativeInteger): Int {
        if (other !is XsdUnsignedLong) return -other.compareTo(this)
        return uLongValue.compareTo(other.uLongValue)
    }

    interface DivRem: XsdInteger.DivRem {
        override val quotient: XsdUnsignedLong
        override val remainder: XsdUnsignedLong
    }

    companion object : SimpleTypeSerializer<XsdUnsignedLong>("xsd.unsignedLong") {
        override fun deserialize(
            raw: String,
            input: XmlReader?
        ): XsdUnsignedLong {
            return XsdUnsignedLongImpl(xmlTrimWhitespace(raw).xsToULong())
        }

        val ZERO: XsdUnsignedLong = XsdUnsignedLongImpl(0u)

        operator fun invoke(value: ULong): XsdUnsignedLong = XsdUnsignedLongImpl(value)
        operator fun invoke(value: UInt): XsdUnsignedInt = XsdUnsignedInt(value)

        operator fun invoke(value: CharSequence): XsdUnsignedLong = when (value.getOrNull(0) ?: throw NumberFormatException("Empty string is not a number")) {
            '-' -> if (value.length == 2 && value[1]=='0') ZERO else throw NumberFormatException("Negative numbers are not allowed")
            else -> XsdUnsignedLongImpl(value.xsToULong())
        }

    }

}
