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
import io.github.pdvrieze.xml.schematypes.types.UnsignedByteType
import io.github.pdvrieze.xml.schematypes.values.instances.XsdLongImpl
import io.github.pdvrieze.xml.schematypes.values.instances.XsdUnsignedByteImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.xmlTrimWhitespace

@Serializable(XsdUnsignedByte.Companion::class)
interface XsdUnsignedByte : XsdUnsignedShort {

    override val schemaType: UnsignedByteType<XsdUnsignedByte>

    val uByteValue: UByte
    override val uShortValue: UShort get() = uByteValue.toUShort()
    override val uIntValue: UInt get() = uByteValue.toUInt()
    override val uLongValue: ULong get() = uByteValue.toULong()

    override fun toInt(): Int = uByteValue.toInt()

    override fun toLong(): Long = uByteValue.toLong()

    override fun toUInt(): UInt = uByteValue.toUInt()

    override fun toULong(): ULong = uByteValue.toULong()

    override fun abs(): XsdUnsignedByte = this

    override fun plus(other: XsdInteger): XsdInteger = when (other) {
        is XsdUnsignedByte -> {
            val v = uByteValue + other.uByteValue
            when {
                v <= UByte.MAX_VALUE -> XsdUnsignedByte(v.toUByte())
                else -> XsdUnsignedShort(v.toUShort())
            }
        }

        is XsdByte -> {
            val v = uByteValue.toInt() + other.byteValue
            return when {
                v > UByte.MAX_VALUE.toInt() -> XsdUnsignedShort(v.toUShort())
                v >= 0 -> XsdUnsignedByte(v.toUByte())
                v >= Byte.MIN_VALUE -> XsdByte(v.toByte())
                else -> XsdShort(v.toShort())
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

    override fun compareTo(other: XsdInteger): Int {
        if (other.sign < 0) return 1
        else if (other !is XsdUnsignedByte) return -other.compareTo(this)
        return uByteValue.compareTo(other.uByteValue)
    }

    override fun compareTo(other: XsdNonNegativeInteger): Int {
        if (other !is XsdUnsignedByte) return -other.compareTo(this)
        return uByteValue.compareTo(other.uByteValue)
    }

    companion object : SimpleTypeSerializer<XsdUnsignedByte>("xsd.unsignedLong") {
        override fun deserialize(raw: String, input: XmlReader?): XsdUnsignedByte {
            return XsdUnsignedByteImpl(xmlTrimWhitespace(raw).toUByte())
        }

        operator fun invoke(value: UByte): XsdUnsignedByte = XsdUnsignedByteImpl(value)

        operator fun invoke(value: CharSequence): XsdUnsignedByte = invoke(value.toString().toUByte())
    }

}
