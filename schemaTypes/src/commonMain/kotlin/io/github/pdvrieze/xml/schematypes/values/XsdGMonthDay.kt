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

import io.github.pdvrieze.xml.schematypes.Collation
import io.github.pdvrieze.xml.schematypes.impl.SimpleTypeSerializer
import io.github.pdvrieze.xml.schematypes.types.GMonthDayType
import io.github.pdvrieze.xml.schematypes.values.instances.XsdGMonthDayImpl
import io.github.pdvrieze.xml.schematypes.values.instances.xsToUInt
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi

@ExperimentalXmlUtilApi
@Serializable(XsdGMonthDay.Companion::class)
interface XsdGMonthDay: IXsdDateTime, XsdPrimitive {

    override val schemaType: GMonthDayType<XsdGMonthDay>

    override val day: UInt
    override val month: UInt

    override val year: Nothing? get() = null
    override val hour: Nothing? get() = null
    override val minute: Nothing? get() = null
    override val second: Nothing? get() = null

    override fun compareTo(other: XsdPrimitive, collation: Collation): Int = when (other) {
        is XsdGMonthDay -> compareTo(other)
        is IXsdDateTime -> toStandardDateTime().compareTo(other.toStandardDateTime())
        else -> throw IllegalArgumentException("Cannot compare $this with $other")
    }

    override fun ensureTimezone(fallbackTimezone: TimeZone): XsdGMonthDay

    override fun toStandardDateTime(): XsdDateTime {
        return XsdDateTime(1972, month, day, 0u, 0u, XsdInt.ZERO, timezoneOffset)
    }

    companion object: SimpleTypeSerializer<XsdGMonthDay>("xsd.gMonthDay") {
        operator fun invoke(str: CharSequence): XsdGMonthDay = XsdGMonthDayImpl(str)

        operator fun invoke(month: UInt, day: UInt): XsdGMonthDay =
            XsdGMonthDayImpl(month, day)

        operator fun invoke(month: UInt, day: UInt, timezoneOffset: Int?): XsdGMonthDay =
            XsdGMonthDayImpl(month, day, timezoneOffset)

        override fun deserialize(raw: String, input: nl.adaptivity.xmlutil.XmlReader?): XsdGMonthDay {
            return XsdGMonthDayImpl(raw.xsToUInt())
        }
    }
}
