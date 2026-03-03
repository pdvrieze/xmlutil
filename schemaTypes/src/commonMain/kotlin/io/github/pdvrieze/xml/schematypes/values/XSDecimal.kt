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
import io.github.pdvrieze.xml.schematypes.types.DecimalType
import io.github.pdvrieze.xml.schematypes.values.instances.XSBigDecimal
import io.github.pdvrieze.xml.schematypes.values.instances.XSDecimalStringImpl
import io.github.pdvrieze.xml.schematypes.values.instances.XSIntImpl
import io.github.pdvrieze.xml.schematypes.values.instances.XSLongImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.xmlTrimWhitespace

@ExperimentalXmlUtilApi
@Serializable(XSDecimal.Companion::class)
interface XSDecimal : XSAtomic {
    override val schemaType: DecimalType<XSDecimal>

    fun toLong(): Long
    fun toInt(): Int
    fun toDouble(): Double = xmlString.toString().toDouble()
    fun toVDecimal(): XSBigDecimal = XSDecimalStringImpl(xmlString.toString())

    operator fun compareTo(other: XSDecimal): Int

    companion object : SimpleTypeSerializer<XSDecimal>("xsd.decimal") {
        override fun deserialize(
            raw: String,
            input: XmlReader?
        ): XSDecimal {
            return invoke(raw)
        }

        operator fun invoke(value: String): XSDecimal {
            val trimmed = xmlTrimWhitespace(value)
            val hasDecimal = '.' in trimmed
            if (hasDecimal) return XSDecimalStringImpl(trimmed)
            val digitCount: Int
            val negative: Boolean
            when (trimmed.firstOrNull()) {
                '-' -> {
                    digitCount = trimmed.length - 1
                    negative = true
                }

                '+' -> {
                    digitCount = trimmed.length - 1
                    negative = false
                }

                else -> {
                    digitCount = trimmed.length
                    negative = false
                }
            }

            when (digitCount) {
                0 -> throw NumberFormatException("$value is not a valid number")
                in 1..9 -> return XSIntImpl(trimmed.toInt())
                10 -> {
                    val l = trimmed.toLong()
                    if (l < Int.MIN_VALUE || l > Int.MAX_VALUE) return XSLongImpl(l)
                    return XSIntImpl(l.toInt())
                }

                in 11..18 -> {
                    return XSLongImpl(trimmed.toLong())
                }

                19 -> { // maxLong starts with 9 so no need to check the first digit
                    val firstChar = if (negative) trimmed[1] else trimmed[0]
                    if (firstChar <= '8') return XSLongImpl(trimmed.toLong())
                    trimmed.toLongOrNull()?.let { return XSLongImpl(it)}
                    return XSDecimalStringImpl(trimmed)
                }
                else -> return XSDecimalStringImpl(trimmed)
            }

        }

        private const val MAX_INT_DIGITS = 10 // up to 2 * 10^9
    }
}

