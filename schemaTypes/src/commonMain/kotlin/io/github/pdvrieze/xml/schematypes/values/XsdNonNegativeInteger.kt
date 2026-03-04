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
import io.github.pdvrieze.xml.schematypes.types.NonNegativeIntegerType
import io.github.pdvrieze.xml.schematypes.values.instances.XsdNonNegativeIntegerStringImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.xmlTrimWhitespace


@Serializable(XsdNonNegativeInteger.Companion::class)
interface XsdNonNegativeInteger : XsdInteger {

    override val schemaType: NonNegativeIntegerType<XsdNonNegativeInteger>

    fun toULong(): ULong
    fun toUInt(): UInt

    operator fun plus(other: XsdNonNegativeInteger): XsdNonNegativeInteger
    operator fun times(other: XsdNonNegativeInteger): XsdNonNegativeInteger

    override fun compareTo(other: XsdInteger): Int = when (other) {
        is XsdNonNegativeInteger -> toULong().compareTo(other.toULong())
        else -> {
            val ol = other.toLong()
            if (ol < 0) 1 else toULong().compareTo(ol.toULong())
        }
    }

    operator fun compareTo(other: XsdNonNegativeInteger): Int =
        toULong().compareTo(other.toULong())

    operator fun plus(other: ULong): XsdNonNegativeInteger

    companion object : SimpleTypeSerializer<XsdNonNegativeInteger>("xsd.nonNegativeInteger") {
        override fun deserialize(raw: String, input: XmlReader?): XsdNonNegativeInteger {
            return invoke(xmlTrimWhitespace(raw))
        }

        val ONE = XsdUnsignedInt(1u)
        val ZERO = XsdUnsignedInt(0u)

        operator fun invoke(charSequence: CharSequence) =
            invoke(rawValue = charSequence.toString())

        operator fun invoke(rawValue: String): XsdNonNegativeInteger = when {
            rawValue.length > MAXLONG.length -> XsdNonNegativeIntegerStringImpl(rawValue)

            rawValue == "0" -> ZERO
            rawValue == "1" -> ONE

            rawValue.length == MAXLONG.length && (rawValue[0] == '0' || rawValue[0] == '1')
                    && rawValue.substring(1).toLong() <= MAXNONSIGNDIGITS ->
                invoke(rawValue.toULong())

            rawValue.toLong() <= MAXUINT -> invoke(rawValue.toUInt())

            else -> invoke(rawValue.toULong())
        }

        operator fun invoke(value: ULong): XsdUnsignedLong = XsdUnsignedLong.Companion(value)
        operator fun invoke(value: UInt): XsdUnsignedInt = XsdUnsignedInt.Companion(value)
        operator fun invoke(value: Long): XsdUnsignedLong = run { require(value >= 0); XsdUnsignedLong(value.toULong()) }
        operator fun invoke(value: Int): XsdUnsignedInt = run { require(value >= 0); XsdUnsignedInt(value.toUInt()) }

        private val MAXLONG = ULong.MAX_VALUE.toString()
        private val MAXNONSIGNDIGITS = MAXLONG.substring(1).toLong()
        private val MAXUINT = UInt.MAX_VALUE.toLong()

    }
}

