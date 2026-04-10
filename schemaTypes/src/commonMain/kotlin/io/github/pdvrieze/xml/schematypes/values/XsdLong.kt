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
import io.github.pdvrieze.xml.schematypes.types.LongType
import io.github.pdvrieze.xml.schematypes.values.instances.BigDecimal
import io.github.pdvrieze.xml.schematypes.values.instances.XsdLongImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlUtilInternal
import kotlin.math.absoluteValue

@Serializable(XsdLong.Companion::class)
@XmlUtilInternal
interface XsdLong : XsdInteger {

    override val schemaType: LongType<XsdLong>

    val longValue: Long

    override fun toLong(): Long = longValue
    override fun toBigInt(): BigInt = BigInt(longValue)
    override fun toBigDecimal(): BigDecimal = BigDecimal(longValue)
    override fun toDouble(): Double = longValue.toDouble()
    override fun toFloat(): Float = longValue.toFloat()

    override val size: ULong get() = 2uL
    override val sign: Int get() = longValue.compareTo(0L)

    override fun unaryMinus(): XsdLong

    override fun plus(other: XsdDecimal): XsdDecimal = when (other) {
        is XsdLong -> plus(other)
        is XsdUnsignedLong -> plus(other)
        else -> other.plus(this)
    }

    override fun plus(other: Long): XsdLong =
        XsdLong(longValue + other)
    fun plus(other: XsdLong): XsdLong = plus(other.longValue)
    override fun plus(other: Int): XsdLong = plus(other.toLong())
    override fun plus(other: UInt): XsdLong = plus(other.toLong())
    override fun plus(other: ULong): XsdLong = plus(other.toLong())
    fun plus(other: XsdUnsignedLong): XsdLong = plus(other.uLongValue)

    override fun minus(other: Long): XsdLong =
        XsdLong(longValue - other)
    fun minus(other: XsdLong): XsdLong = minus(other.longValue)
    override fun minus(other: Int): XsdLong = minus(other.toLong())
    override fun minus(other: UInt): XsdLong = minus(other.toLong())
    override fun minus(other: ULong): XsdLong = minus(other.toLong())
    fun minus(other: XsdUnsignedLong): XsdLong = minus(other.uLongValue)

    override fun times(other: XsdInteger): XsdInteger = when (other) {
        is XsdLong -> times(other.toLong())
        is XsdUnsignedLong -> times(other.toLong())
        else -> other.times(this)
    }

    override fun times(multiplier: Long): XsdLong =
        XsdLong(longValue * multiplier)

    override fun times(multiplier: Int): XsdLong = times(multiplier.toLong())
    override fun times(multiplier: UInt): XsdLong = times(multiplier.toLong())
    override fun times(multiplier: ULong): XsdLong = times(multiplier.toLong())

    override fun minus(other: XsdDecimal): XsdDecimal = when (other) {
        is XsdLong -> minus(other)
        is XsdUnsignedLong -> minus(other)
        else -> other.unaryMinus().plus(this)
    }

    override fun times(other: XsdDecimal): XsdDecimal = when (other) {
        is XsdLong -> times(other)
        else -> super.times(other)
    }

    override fun div(other: XsdInteger): XsdInteger = when (other) {
        is XsdLong -> div(other)
        is XsdUnsignedLong -> div(XsdLong(other.toLong()))
        else -> return super.div(other)
    }

    operator fun div(other: XsdLong): XsdLong

    override fun rem(divider: XsdInteger): XsdInteger = when (divider) {
        is XsdLong -> rem(divider)
        is XsdUnsignedLong -> rem(XsdLong(divider.toLong()))
        else -> return super.rem(divider)
    }

    operator fun rem(other: XsdLong): XsdLong

    override fun divRem(divider: XsdInteger): XsdInteger.DivRem = when (divider){
        is XsdLong -> divRem(divider)
        is XsdUnsignedLong -> divRem(XsdLong(divider.toLong()))
        else -> divider.divRem(this)
    }
    fun divRem(other: XsdLong): DivRem


    override fun abs(): XsdUnsignedLong

    override fun get(index: Int): UInt {
        when (index) {
            0 -> return longValue.absoluteValue.toUInt()
            1 -> return longValue.absoluteValue.toULong().shr(32).toUInt()
            else -> throw IndexOutOfBoundsException("Index $index out of bounds")
        }
    }

    override fun get(index: ULong): UInt {
        when (index) {
            0uL -> return longValue.absoluteValue.toUInt()
            1uL -> return longValue.absoluteValue.toULong().shr(32).toUInt()
            else -> throw IndexOutOfBoundsException("Index $index out of bounds")
        }
    }

    override fun countTrailingZeroBits(): ULong {
        return longValue.countTrailingZeroBits().toULong()
    }

    override fun significantBitsFromZero(): ULong {
        return 64u - longValue.countLeadingZeroBits().toULong()
    }

    interface DivRem : XsdInteger.DivRem {
        override val quotient: XsdLong
        override val remainder: XsdLong
    }

    @XmlUtilInternal
    data class LongDivRem(override val quotient: XsdLong, override val remainder: XsdLong) : DivRem

    companion object : SimpleTypeSerializer<XsdLong>("xsd.long") {
        operator fun invoke(value: Long): XsdLong = XsdLongImpl(value)
        operator fun invoke(value: CharSequence): XsdLong = XsdLongImpl(value)

        override fun deserialize(raw: String, input: XmlReader?): XsdLong {
            return XsdLongImpl(raw.toLong())
        }
    }

}
