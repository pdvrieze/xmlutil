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
import io.github.pdvrieze.xml.schematypes.values.instances.XSNonNegativeIntegerStringImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.xmlTrimWhitespace


@Serializable(XSNonNegativeInteger.Companion::class)
interface XSNonNegativeInteger : XSInteger {
    fun toULong(): ULong
    fun toUInt(): UInt

    operator fun plus(other: XSNonNegativeInteger): XSNonNegativeInteger
    operator fun times(other: XSNonNegativeInteger): XSNonNegativeInteger

    override fun compareTo(other: XSInteger): Int = when (other) {
        is XSNonNegativeInteger -> toULong().compareTo(other.toULong())
        else -> {
            val ol = other.toLong()
            if (ol < 0) 1 else toULong().compareTo(ol.toULong())
        }
    }

    operator fun compareTo(other: XSNonNegativeInteger): Int =
        toULong().compareTo(other.toULong())

    operator fun plus(other: ULong): XSNonNegativeInteger

    companion object : SimpleTypeSerializer<XSNonNegativeInteger>("xsd.nonNegativeInteger") {
        override fun deserialize(raw: String, input: XmlReader?): XSNonNegativeInteger {
            return invoke(xmlTrimWhitespace(raw))
        }

        val ONE = XSUnsignedInt(1u)
        val ZERO = XSUnsignedInt(0u)

        operator fun invoke(charSequence: CharSequence) =
            invoke(rawValue = charSequence.toString())

        operator fun invoke(rawValue: String): XSNonNegativeInteger = when {
            rawValue.length > MAXLONG.length -> XSNonNegativeIntegerStringImpl(rawValue)

            rawValue == "0" -> ZERO
            rawValue == "1" -> ONE

            rawValue.length == MAXLONG.length && (rawValue[0] == '0' || rawValue[0] == '1')
                    && rawValue.substring(1).toLong() <= MAXNONSIGNDIGITS ->
                invoke(rawValue.toULong())

            rawValue.toLong() <= MAXUINT -> invoke(rawValue.toUInt())

            else -> invoke(rawValue.toULong())
        }

        operator fun invoke(value: ULong): XSUnsignedLong = XSUnsignedLong.Companion(value)
        operator fun invoke(value: UInt): XSUnsignedInt = XSUnsignedInt.Companion(value)
        operator fun invoke(value: Long): XSUnsignedLong = run { require(value >= 0); XSUnsignedLong(value.toULong()) }
        operator fun invoke(value: Int): XSUnsignedInt = run { require(value >= 0); XSUnsignedInt(value.toUInt()) }

        private val MAXLONG = ULong.MAX_VALUE.toString()
        private val MAXNONSIGNDIGITS = MAXLONG.substring(1).toLong()
        private val MAXUINT = UInt.MAX_VALUE.toLong()

    }
}

