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

package io.github.pdvrieze.xml.schematypes.values.instances

import io.github.pdvrieze.xml.schematypes.types.DoubleType
import io.github.pdvrieze.xml.schematypes.values.XsdDecimal
import io.github.pdvrieze.xml.schematypes.values.XsdDouble
import nl.adaptivity.xmlutil.XmlUtilInternal
import kotlin.math.absoluteValue
import kotlin.math.nextDown
import kotlin.math.nextUp
import kotlin.math.pow
import kotlin.math.floor as kmFloor
import kotlin.math.round as kmRound

@XmlUtilInternal
class XsdDoubleImpl(override val value: Double): XsdDouble {

    override val schemaType: DoubleType<*> get() = DoubleType.Instance

    override fun toDouble(): Double = value

    override fun abs(): XsdDouble = XsdDoubleImpl(value.absoluteValue)

    override fun unaryMinus(): XsdDouble = XsdDoubleImpl(-value)

    override fun ceiling(): XsdDouble {
        return XsdDouble(value.nextUp())
    }

    override fun floor(): XsdDouble {
        return XsdDouble(value.nextDown())
    }

    override fun round(): XsdDouble {
        return XsdDouble(kmRound(value))
    }

    override fun round(precision: Int): XsdDouble {
        val factor = 10.0.pow(precision)
        return XsdDouble(kmRound(value * factor) / factor)
    }

    override fun roundToHalfEven(): XsdDouble {
        val scaled = value
        val floorVal = kmFloor(value)
        val fraction = scaled - floorVal

        val result = when {
            fraction > 0.5 -> (floorVal + 1.0)
            fraction < 0.5 -> floorVal
            floorVal % 2.0 == 0.0 -> floorVal
            else -> (floorVal + 1.0)
        }
        return XsdDouble(result)
    }

    override fun roundToHalfEven(precision: Int): XsdDouble {
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
        return XsdDouble(result)
    }

    override fun toString(): String = xmlString

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other is XsdDouble -> other.toDouble() == value
        other is XsdDecimal -> other == XsdDecimal(xmlString)
        else -> false
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

}
