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
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlReader

@ExperimentalXmlUtilApi
@Serializable(XsdInteger.Companion::class)
interface XsdInteger : XsdDecimal {

    override val schemaType: IntegerType<XsdInteger>

    override fun toLong(): Long
    override fun toInt(): Int
    fun toBigInt(): XsdInteger

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
    override fun times(other: Int): XsdInteger = times(XsdInteger(other))
    override fun times(other: Long): XsdInteger = times(XsdInteger(other))
    override fun times(other: UInt): XsdInteger = times(XsdInteger(other))
    override fun times(other: ULong): XsdInteger = times(XsdInteger(other))

    operator fun div(other: XsdInteger): XsdInteger = divRem(other).quotient
    override fun div(other: XsdDecimal): XsdDecimal = when (other) {
        is XsdInteger -> div(other)
        else -> divRem(other).quotient
    }

    fun rem(divider: XsdInteger): XsdInteger = divRem(divider).remainder

    override fun rem(other: XsdDecimal): XsdDecimal = when (other) {
        is XsdInteger -> rem(other)
        else -> divRem(other).remainder
    }

    fun divRem(divider: XsdInteger): DivRem

    override fun divRem(other: XsdDecimal): XsdDecimal.DivRem = when (other) {
        is XsdInteger -> divRem(other)
        else -> toBigDecimal().divRem(other)
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

