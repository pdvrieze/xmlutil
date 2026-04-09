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
import io.github.pdvrieze.xml.schematypes.types.DoubleType
import io.github.pdvrieze.xml.schematypes.values.instances.XsdDoubleImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlReader

@ExperimentalXmlUtilApi
@Serializable(XsdDouble.Companion::class)
interface XsdDouble: XsdPrimitive, XsdNumeric<XsdDouble> {
    override val schemaType: DoubleType<XsdDouble>

    val value: Double
    override fun toLong(): Long = value.toLong()
    override fun toInt(): Int = value.toInt()

    operator fun plus(other: XsdDouble): XsdDouble = XsdDoubleImpl(value + other.value)
    operator fun minus(other: XsdDouble): XsdDouble = XsdDoubleImpl(value - other.value)
    operator fun times(other: XsdDouble): XsdDouble = XsdDoubleImpl(value * other.value)
    operator fun div(other: XsdDouble): XsdDouble = XsdDoubleImpl(value / other.value)
    operator fun rem(other: XsdDouble): XsdDouble = XsdDoubleImpl(value % other.value)

    override fun times(other: XsdNumeric<*>): XsdNumeric<*> = when (other) {
        is XsdDouble -> XsdDouble(value * other.value)
        else -> XsdDouble(value * other.toDouble())
    }

    override fun compareTo(other: XsdNumeric<*>): Int {
        return when (other) {
            is XsdDouble -> value.compareTo(other.toDouble())
            is XsdFloat -> value.compareTo(other.toDouble())
            is XsdDecimal -> value.compareTo(other.toDouble())
        }
    }

    override val xmlString: String
        get() = when (value) {
            Double.POSITIVE_INFINITY -> "INF"
            Double.NEGATIVE_INFINITY -> "-INF"
            else if (value.isNaN()) -> "NaN"
            else -> buildString {
                append(value)
                // drop trailing zeros per the spec
                if (this.endsWith(".0")) {
                    this.deleteRange(length -2, length)
                }
            }
        }

    companion object : SimpleTypeSerializer<XsdDouble>("xsd.double") {
        operator fun invoke(value: Double): XsdDouble = XsdDoubleImpl(value)
        operator fun invoke(value: CharSequence): XsdDouble {
            return when (val v = value.trim().toString()) {
                "+INF", "INF" -> XsdDoubleImpl(Double.POSITIVE_INFINITY)
                "-INF" -> XsdDoubleImpl(Double.NEGATIVE_INFINITY)
                "NaN" -> XsdDoubleImpl(Double.NaN)
                else -> XsdDoubleImpl(v.toDouble())
            }
        }

        override fun deserialize(
            raw: String,
            input: XmlReader?
        ): XsdDouble {
            return XsdDoubleImpl(raw.toDouble())
        }
    }
}

