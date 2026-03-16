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
import io.github.pdvrieze.xml.schematypes.types.UnsignedShortType
import io.github.pdvrieze.xml.schematypes.values.instances.XsdLongImpl
import io.github.pdvrieze.xml.schematypes.values.instances.XsdUnsignedShortImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.xmlTrimWhitespace

@Serializable(XsdUnsignedShort.Companion::class)
interface XsdUnsignedShort : XsdUnsignedInt {

    override val schemaType: UnsignedShortType<XsdUnsignedShort>

    val uShortValue: UShort
    override val uIntValue: UInt get() = uShortValue.toUInt()
    override val uLongValue: ULong get() = uShortValue.toULong()

    override fun toInt(): Int = uShortValue.toInt()

    override fun toLong(): Long = uShortValue.toLong()

    override fun toUInt(): UInt = uShortValue.toUInt()

    override fun toULong(): ULong = uShortValue.toULong()

    override fun abs(): XsdUnsignedShort = this

    override fun compareTo(other: XsdNonNegativeInteger): Int {
        if (other !is XsdUnsignedShort) return -other.compareTo(this)
        return uShortValue.compareTo(other.uShortValue)
    }

    override fun significantBitsFromZero(): ULong {
        return (64 - uLongValue.countLeadingZeroBits()).toULong()
    }

    override fun plus(other: XsdInteger): XsdInteger = when (other) {
        is XsdUnsignedShort -> {
            val v = uShortValue + other.uShortValue
            when {
                v <= UShort.MAX_VALUE -> XsdUnsignedShort(v.toUShort())
                else -> XsdUnsignedInt(v)
            }
        }

        is XsdShort -> {
            val v = uShortValue.toInt() + other.shortValue
            return when {
                v > UShort.MAX_VALUE.toInt() -> XsdUnsignedInt(v.toUInt())
                v >= 0 -> XsdUnsignedShort(v.toUShort())
                v >= Short.MIN_VALUE -> XsdShort(v.toShort())
                else -> XsdInt(v)
            }
        }
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

    companion object : SimpleTypeSerializer<XsdUnsignedShort>("xsd.unsignedLong") {
        override fun deserialize(raw: String, input: XmlReader?): XsdUnsignedShort {
            return XsdUnsignedShortImpl(xmlTrimWhitespace(raw).toUShort())
        }

        operator fun invoke(value: UShort): XsdUnsignedShort = XsdUnsignedShortImpl(value)
    }

}
