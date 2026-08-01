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

import io.github.pdvrieze.xml.schematypes.types.FloatType
import io.github.pdvrieze.xml.schematypes.values.XsdDecimal
import io.github.pdvrieze.xml.schematypes.values.XsdDouble
import io.github.pdvrieze.xml.schematypes.values.XsdFloat
import nl.adaptivity.xmlutil.XmlUtilInternal
import kotlin.math.absoluteValue

@XmlUtilInternal
class XsdFloatImpl(override val value: Float): XsdFloat {
    override val schemaType: FloatType<*> get() = FloatType.Instance

    override fun toDouble(): Double = value.toDouble()

    override fun abs(): XsdFloat = XsdFloatImpl(value.absoluteValue)

    override fun unaryMinus(): XsdFloat = XsdFloatImpl(-value)
    override fun unaryPlus(): XsdFloat = this

    override fun toString(): String = xmlString

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other is XsdFloat -> other.value == value
        other is XsdDouble -> other.toDouble() == value.toDouble()
        other is XsdDecimal -> other == XsdDecimal(xmlString)
        else -> false
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

}
