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
import io.github.pdvrieze.xml.schematypes.types.DayTimeDurationType
import io.github.pdvrieze.xml.schematypes.values.instances.XsdDayTimeDurationImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlReader

@ExperimentalXmlUtilApi
@Serializable(XsdDayTimeDuration.Companion::class)
interface XsdDayTimeDuration : XsdDuration {
    // TODO implement DayTimeDuration and YearMonthDuration
    override val schemaType: DayTimeDurationType<XsdDayTimeDuration>

    override val months: Long get() = 0L

    companion object : SimpleTypeSerializer<XsdDayTimeDuration>("xsd.dayTimeDuration") {

        operator fun invoke(str: String): XsdDayTimeDuration {
            return XsdDayTimeDurationImpl.Companion(str)
        }

        operator fun invoke(millis: Long): XsdDayTimeDuration {
            return XsdDayTimeDurationImpl(millis)
        }

        override fun deserialize(raw: String, input: XmlReader?): XsdDayTimeDuration {
            return invoke(raw)
        }
    }
}
