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
import io.github.pdvrieze.xml.schematypes.types.PositiveIntegerType
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.xmlTrimWhitespace

@OptIn(ExperimentalUnsignedTypes::class)
@Serializable(XsdPositiveInteger.Companion::class)
interface XsdPositiveInteger : XsdNonNegativeInteger {
    override fun toBigInt(): XsdPositiveInteger = this

    override val schemaType: PositiveIntegerType<XsdPositiveInteger>
    override fun unaryMinus(): XsdNegativeInteger

    fun coerceAtMost(maxMax: XsdPositiveInteger): XsdPositiveInteger = when {
        this < maxMax -> this
        else -> maxMax
    }

    operator fun times(other: XsdPositiveInteger): XsdPositiveInteger

    override fun compareTo(other: XsdInteger): Int

    operator fun compareTo(other: XsdPositiveInteger): Int =
        toULong().compareTo(other.toULong())

    companion object : SimpleTypeSerializer<XsdPositiveInteger>("xsd.positiveInteger") {
        override fun deserialize(raw: String, input: XmlReader?): XsdPositiveInteger {
            return invoke(xmlTrimWhitespace(raw))
        }

        val ONE = BigPositiveInt(1u)

        operator fun invoke(charSequence: CharSequence) =
            invoke(rawValue = charSequence.toString())

        operator fun invoke(rawValue: String): XsdPositiveInteger = when {
            rawValue == "0" -> throw NumberFormatException("Positive integers may not be zero")
            rawValue == "1" -> ONE
            rawValue.length > MAXLONG.length -> BigPositiveInt(rawValue)


            rawValue.length == MAXLONG.length && (rawValue[0] == '0' || rawValue[0] == '1')
                    && rawValue.substring(1).toLong() <= MAXNONSIGNDIGITS ->
                invoke(rawValue.toULong())

            rawValue.toLong() <= MAXUINT -> invoke(rawValue.toUInt())

            else -> invoke(rawValue.toULong())
        }

        operator fun invoke(value: ULong): BigPositiveInt = BigPositiveInt(value)
        operator fun invoke(value: UInt): BigPositiveInt = BigPositiveInt(value)
        operator fun invoke(value: Long): BigPositiveInt = run { require(value > 0); BigPositiveInt(value.toULong()) }
        operator fun invoke(value: Int): BigPositiveInt = run { require(value > 0); BigPositiveInt(value.toUInt()) }

        private val MAXLONG = ULong.MAX_VALUE.toString()
        private val MAXNONSIGNDIGITS = MAXLONG.substring(1).toLong()
        private val MAXUINT = UInt.MAX_VALUE.toLong()

    }
}

