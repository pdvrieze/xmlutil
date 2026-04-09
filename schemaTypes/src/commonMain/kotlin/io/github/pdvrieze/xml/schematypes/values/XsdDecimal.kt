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
import io.github.pdvrieze.xml.schematypes.values.instances.BigDecimal
import io.github.pdvrieze.xml.schematypes.values.instances.XsdBigDecimal
import io.github.pdvrieze.xml.schematypes.values.instances.XsdIntImpl
import io.github.pdvrieze.xml.schematypes.values.instances.XsdLongImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.xmlTrimWhitespace

@ExperimentalXmlUtilApi
@Serializable(XsdDecimal.Companion::class)
interface XsdDecimal : XsdPrimitive, XsdNumeric<XsdDecimal> {
    override val schemaType: DecimalType<XsdDecimal>

    val sign: Int

    override fun toLong(): Long
    override fun toInt(): Int
    fun toUInt(): UInt = toLong().toUInt()

    override fun toDouble(): Double = xmlString.toDouble()

    operator fun compareTo(other: XsdDecimal): Int
    operator fun plus(other: XsdDecimal): XsdDecimal

    operator fun minus(other: XsdDecimal): XsdDecimal
    operator fun times(other: XsdDecimal): XsdDecimal

    operator fun plus(other: Int): XsdDecimal = plus(XsdInt(other))
    operator fun plus(other: Long): XsdDecimal = plus(XsdLong(other))
    operator fun plus(other: UInt): XsdDecimal = plus(XsdUnsignedInt(other))
    operator fun plus(other: ULong): XsdDecimal = plus(XsdUnsignedLong(other))

    operator fun minus(other: Int): XsdDecimal = minus(XsdInt(other))
    operator fun minus(other: Long): XsdDecimal = minus(XsdLong(other))
    operator fun minus(other: UInt): XsdDecimal = minus(XsdUnsignedInt(other))
    operator fun minus(other: ULong): XsdDecimal = minus(XsdUnsignedLong(other))

    operator fun times(other: Int): XsdDecimal = times(XsdInt(other))
    operator fun times(other: Long): XsdDecimal = times(XsdLong(other))
    operator fun times(other: UInt): XsdDecimal = times(XsdUnsignedInt(other))
    operator fun times(other: ULong): XsdDecimal = times(XsdUnsignedLong(other))

    override fun times(other: XsdNumeric<*>): XsdNumeric<*> = when (other) {
        is XsdDecimal -> this.times(other)
        else -> other.times(this)
    }

    fun divRem(other: XsdDecimal): DivRem
    operator fun div(other: XsdDecimal): XsdDecimal = divRem(other).quotient
    operator fun rem(other: XsdDecimal): XsdDecimal = divRem(other).remainder

    override fun compareTo(other: XsdNumeric<*>): Int {
        return when (other) {
            is XsdDouble -> toDouble().compareTo(other.toDouble())
            is XsdFloat -> toDouble().compareTo(other.toDouble())
            is XsdDecimal -> compareTo(other)
        }
    }

    fun toBigDecimal(): BigDecimal

    interface DivRem {
        val quotient: XsdDecimal
        val remainder: XsdDecimal

        operator fun component1(): XsdDecimal = quotient
        operator fun component2(): XsdDecimal = remainder
    }

    companion object : SimpleTypeSerializer<XsdDecimal>("xsd.decimal") {
        override fun deserialize(
            raw: String,
            input: XmlReader?
        ): XsdDecimal {
            return invoke(raw)
        }

        operator fun invoke(value: CharSequence): XsdDecimal {
            val trimmed = xmlTrimWhitespace(value)
            val hasDecimal = '.' in trimmed
            if (hasDecimal) return BigDecimal(trimmed)
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
                in 1..9 -> return XsdIntImpl(trimmed.toInt())
                10 -> {
                    val l = trimmed.toLong()
                    if (l < Int.MIN_VALUE || l > Int.MAX_VALUE) return XsdLongImpl(l)
                    return XsdIntImpl(l.toInt())
                }

                in 11..18 -> {
                    return XsdLongImpl(trimmed.toLong())
                }

                19 -> { // maxLong starts with 9 so no need to check the first digit
                    val firstChar = if (negative) trimmed[1] else trimmed[0]
                    if (firstChar <= '8') return XsdLongImpl(trimmed.toLong())
                    trimmed.toLongOrNull()?.let { return XsdLongImpl(it)}
                    return XsdBigDecimal(trimmed)
                }
                else -> return XsdBigDecimal(trimmed)
            }

        }

        private const val MAX_INT_DIGITS = 10 // up to 2 * 10^9
    }
}

