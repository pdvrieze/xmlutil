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
import io.github.pdvrieze.xml.schematypes.types.FloatType
import io.github.pdvrieze.xml.schematypes.values.instances.XsdFloatImpl
import io.github.pdvrieze.xml.schematypes.values.instances.xsToFloat
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlReader
import kotlin.math.nextDown
import kotlin.math.nextUp
import kotlin.math.pow
import kotlin.math.floor as kmFloor
import kotlin.math.round as kmRound

@ExperimentalXmlUtilApi
@Serializable(XsdFloat.Companion::class)
interface XsdFloat: XsdPrimitive, XsdNumeric<XsdFloat> {
    val value: Float

    override val isFinite: Boolean get() = value.isFinite()
    override val isNaN: Boolean get() = value.isNaN()
    override val isInfinity: Boolean get() = value == Float.POSITIVE_INFINITY
    override val isNegativeInfinity: Boolean get() = value == Float.NEGATIVE_INFINITY

    override fun toLong(): Long = value.toLong()
    override fun toFloat(): Float = value

    operator fun plus(other: XsdFloat): XsdFloat = XsdFloatImpl(value + other.value)
    operator fun minus(other: XsdFloat): XsdFloat = XsdFloatImpl(value - other.value)
    operator fun times(other: XsdFloat): XsdFloat = XsdFloatImpl(value * other.value)
    operator fun div(other: XsdFloat): XsdFloat = XsdFloatImpl(value / other.value)
    operator fun rem(other: XsdFloat): XsdFloat = XsdFloatImpl(value % other.value)

    override fun times(multiplier: XsdNumeric<*>): XsdNumeric<*> = when (multiplier) {
        is XsdFloat -> XsdFloatImpl(value * multiplier.value)
        else -> XsdFloat((value * multiplier.toDouble()).toFloat())
    }

    override fun compareTo(other: XsdNumeric<*>): Int {
        return when (other) {
            is XsdDouble -> value.toDouble().compareTo(other.toDouble())
            is XsdFloat -> value.compareTo(other.value)
            is XsdDecimal -> value.toDouble().compareTo(other.toDouble())
        }
    }

    override val xmlString: String
        get() = when (value) {
            Float.POSITIVE_INFINITY -> "INF"
            Float.NEGATIVE_INFINITY -> "-INF"
            else if (value.isNaN()) -> "NaN"
            else -> buildString {
                append(value)
                // drop trailing zeros per the spec
                if (this.endsWith(".0")) {
                    this.deleteRange(length -2, length)
                }
            }

        }

    override val schemaType: FloatType<XsdFloat>


    override fun ceiling(): XsdFloat {
        return XsdFloat(value.toDouble().nextUp().toFloat())
    }

    override fun floor(): XsdFloat {
        return XsdFloat(value.toDouble().nextDown().toFloat())
    }

    override fun round(): XsdFloat {
        return XsdFloat(kmRound(value))
    }

    override fun round(precision: Int): XsdFloat {
        val factor = 10.0.pow(precision)
        return XsdFloat((kmRound(value * factor) / factor).toFloat())
    }

    override fun roundToHalfEven(): XsdFloat {
        val scaled = value
        val floorVal = kmFloor(value)
        val fraction = scaled - floorVal

        val result = when {
            fraction > 0.5 -> (floorVal + 1.0)
            fraction < 0.5 -> floorVal
            floorVal % 2.0 == 0.0 -> floorVal
            else -> (floorVal + 1.0)
        }
        return XsdFloat(result.toFloat())
    }

    override fun roundToHalfEven(precision: Int): XsdFloat {
        val factor = 10.0.pow(precision)
        val scaled = value * factor
        val floorVal = kmFloor(scaled)
        val fraction = scaled - floorVal

        val result = when {
            fraction > 0.5 -> (floorVal + 1.0) / factor
            fraction < 0.5 -> floorVal / factor
            // exactly halfway: round to even
            floorVal % 2.0 == 0.0 -> floorVal / factor
            else -> (floorVal + 1.0) / factor
        }
        return XsdFloat(result.toFloat())
    }


    companion object : SimpleTypeSerializer<XsdFloat>("xsd.float") {
        operator fun invoke(value: Float): XsdFloat = XsdFloatImpl(value)

        operator fun invoke(value: CharSequence): XsdFloat {
            return when (val v = value.trim().toString()) {
                "+INF", "INF" -> XsdFloatImpl(Float.POSITIVE_INFINITY)
                "-INF" -> XsdFloatImpl(Float.NEGATIVE_INFINITY)
                "NaN" -> XsdFloatImpl(Float.NaN)
                else -> XsdFloatImpl(v.xsToFloat())
            }
        }

        override fun deserialize(
            raw: String,
            input: XmlReader?
        ): XsdFloat {
            return XsdFloatImpl(raw.xsToFloat())
        }
    }
}
