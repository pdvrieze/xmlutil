/*
 * Copyright (c) 2023-2026.
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
import io.github.pdvrieze.xml.schematypes.types.DurationType
import io.github.pdvrieze.xml.schematypes.values.instances.XsdDurationImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlReader

@ExperimentalXmlUtilApi
@Serializable(XsdDuration.Companion::class)
interface XsdDuration : XsdAtomic {
    // TODO implement DayTimeDuration and YearMonthDuration
    override val schemaType: DurationType<XsdDuration>

    val months: Long
    val millis: Long
    val seconds: Double get() = millis.toDouble() / 1000.0

    operator fun compareTo(other: XsdDuration): Int = when(val m = months.compareTo(other.months)) {
        0 -> seconds.compareTo(other.seconds)
        else -> m
    }

    companion object : SimpleTypeSerializer<XsdDuration>("xsd.duration") {

        operator fun invoke(str: String): XsdDuration {
            return XsdDurationImpl(str)
        }

        operator fun invoke(months: Long, millis: Long): XsdDuration {
            return XsdDurationImpl(months, millis)
        }

        override fun deserialize(raw: String, input: XmlReader?): XsdDuration {
            return invoke(raw)
        }
    }
}
