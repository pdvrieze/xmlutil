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
import io.github.pdvrieze.xml.schematypes.types.IntegerType
import io.github.pdvrieze.xml.schematypes.values.instances.XsdIntImpl
import io.github.pdvrieze.xml.schematypes.values.instances.XsdLongImpl
import io.github.pdvrieze.xml.schematypes.values.instances.xsToDouble
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlReader

@ExperimentalXmlUtilApi
@Serializable(XsdInteger.Companion::class)
interface XsdInteger : XsdDecimal {
    override val isFinite: Boolean get() = true
    override val isNaN: Boolean get() = false
    override val isInfinity: Boolean get() = false
    override val isNegativeInfinity: Boolean get() = false

    override val schemaType: IntegerType<XsdInteger>

    override fun toLong(): Long
    override fun toInt(): Int
    fun toBigInt(): XsdInteger

    override fun toDouble(): Double = when (size) {
        1uL, 2uL -> toLong().toDouble()
        else -> xmlString.xsToDouble()
    }

    override fun toFloat(): Float = when (size) {
        1uL, 2uL -> toLong().toFloat()
        else -> xmlString.toFloat()
    }

    override fun roundToInteger(): XsdInteger = this

    /** The conceptual size in 32-bit values from 0. */
    val size: ULong

    /**
     * Return the amount of zero bits at the end of the number (the least significant part). Note
     * that this function is not valid for a value of zero.
     */
    fun countTrailingZeroBits(): ULong

    /**
     * Count the amount of significant bits used for this value.
     */
    fun significantBitsFromZero(): ULong

    /**
     * An indicator of the sign of the number. For a logical value of `0` a sign of `0` is returned.
     */
    override val sign: Int
    override val isNegative: Boolean get() = sign < 0

    /**
     * Retrieve the [index] 32bit value from zero.
     */
    operator fun get(index: ULong): UInt

    operator fun get(index: Int): UInt

    operator fun plus(other: XsdInteger): XsdInteger

    operator fun minus(other: XsdInteger): XsdInteger

    operator fun times(other: XsdInteger): XsdInteger

    override fun times(other: XsdDecimal): XsdDecimal = when (other) {
        is XsdInteger -> times(other)
        else -> toBigDecimal().times(other)
    }
    override fun times(multiplier: Int): XsdInteger = times(XsdInt(multiplier))
    override fun times(multiplier: Long): XsdInteger =
        times(XsdLong(multiplier))
    override fun times(multiplier: UInt): XsdInteger = times(XsdUnsignedInt(multiplier))
    override fun times(multiplier: ULong): XsdInteger = times(XsdUnsignedLong(multiplier))

    operator fun div(other: XsdInteger): XsdInteger = divRem(other).quotient
    override fun div(divider: XsdDecimal): XsdDecimal = when (divider) {
        is XsdInteger -> div(divider)
        else -> divRem(divider).quotient
    }

    fun rem(divider: XsdInteger): XsdInteger = divRem(divider).remainder

    override fun rem(divider: XsdDecimal): XsdDecimal = when (divider) {
        is XsdInteger -> rem(divider)
        else -> divRem(divider).remainder
    }

    fun divRem(divider: XsdInteger): DivRem

    override fun divRem(divider: ULong): DivRem {
        return divRem(XsdUnsignedLong(divider))
    }

    override fun divRem(divider: UInt): IntDivRem {
        return IntDivRemImpl(divRem(XsdUnsignedInt(divider)))
    }

    override fun divRem(divider: XsdDecimal): XsdDecimal.DivRem = when (divider) {
        is XsdInteger -> divRem(divider)
        else -> toBigDecimal().divRem(divider)
    }

    override fun abs(): XsdNonNegativeInteger

    override operator fun unaryMinus(): XsdInteger

    override fun ceiling(): XsdInteger = this

    override fun floor(): XsdInteger = this

    override fun round(): XsdInteger = this

    override fun round(precision: Int): XsdInteger = this

    override fun roundToHalfEven(): XsdInteger = this

    override fun roundToHalfEven(precision: Int): XsdInteger = this

    override fun compareTo(other: XsdDecimal): Int = when (other) {
        is XsdInteger -> compareTo(other)
        else -> -other.compareTo(this)
    }

    operator fun compareTo(other: XsdInteger): Int

    interface DivRem: XsdDecimal.DivRem {
        override val quotient: XsdInteger
        override val remainder: XsdInteger
    }

    interface IntDivRem: DivRem {
        override val quotient: XsdInteger
        override val remainder: XsdInteger
        val intRemainder: Int get() = remainder.toInt()
    }

    interface UIntDivRem: DivRem {
        override val quotient: XsdInteger
        override val remainder: XsdInteger
        val uintRemainder: UInt get() = remainder.toUInt()

        operator fun component3() = uintRemainder
    }

    private class IntDivRemImpl(override val quotient: XsdInteger, override val intRemainder: Int) : IntDivRem {
        constructor(orig: XsdDecimal.DivRem) : this(orig.quotient.roundToInteger(), orig.remainder.toInt())

        override val remainder: XsdInteger get() = XsdInt(intRemainder)
    }

    companion object : SimpleTypeSerializer<XsdInteger>("xsd.integer") {
        val ZERO: XsdInteger = BigInt(0)

        override fun deserialize(raw: String, input: XmlReader?): XsdInteger {
            return BigInt(raw)
        }

        operator fun invoke(i: Int): XsdInt {
            return XsdIntImpl(i)
        }

        operator fun invoke(l: Long): XsdLong {
            return XsdLongImpl(l)
        }

        operator fun invoke(l: ULong): XsdUnsignedLong {
            return XsdUnsignedLong(l)
        }

        operator fun invoke(l: UInt): XsdUnsignedInt {
            return XsdUnsignedInt(l)
        }

        operator fun invoke(value: CharSequence): XsdInteger {
            return BigInt(value)
        }
    }
}

