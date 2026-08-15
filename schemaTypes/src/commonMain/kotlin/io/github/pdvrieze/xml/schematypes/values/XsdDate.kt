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
import io.github.pdvrieze.xml.schematypes.types.DateType
import io.github.pdvrieze.xml.schematypes.values.instances.XsdDateImpl
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlReader

@ExperimentalXmlUtilApi
@Serializable(XsdDate.Companion::class)
interface XsdDate : IXsdDateTime, XsdPrimitive {
    override val schemaType: DateType<XsdDate>
    override val year: Int
    override val month: UInt
    override val day: UInt

    override val hour: Nothing? get() = null
    override val minute: Nothing? get() = null
    override val second: Nothing? get() = null

    override fun weekOfYear(): Int = super.weekOfYear()!!
    override fun weekOfMonth(): Int = super.weekOfMonth()!!

    override fun compareTo(other: XsdPrimitive, collation: Collation): Int {
        return when (other) {
            is IXsdDateTime -> compareTo(other)
            else -> throw IllegalArgumentException("Cannot compare $this with $other")
        }
    }

    override fun ensureTimezone(fallbackTimezone: TimeZone): XsdDate

    /**
     * Converts this value to a dateTime by filling missing bits to 1972-12-1T:00:00:00
     */
    override fun toStandardDateTime(): XsdDateTime {
        return XsdDateTime(this, XsdTime(0u, 0u, 0uL, timezoneOffset))
    }

    operator fun plus(duration: XsdDuration): XsdDate {
        return toStandardDateTime().plus(duration).toDate()
    }

    operator fun minus(duration: XsdDuration): XsdDate {
        return toStandardDateTime().minus(duration).toDate()
    }

    operator fun minus(other: XsdDate): XsdDayTimeDuration {
        val diff = (instant()-other.instant())
        return XsdDayTimeDuration(diff.inWholeMilliseconds)
    }

    companion object: SimpleTypeSerializer<XsdDate>("xsd.date") {

        operator fun invoke(dateTime: XsdDateTime): XsdDate {
            return invoke(dateTime.year, dateTime.month, dateTime.day, dateTime.timezoneOffset)
        }

        operator fun invoke(str: CharSequence): XsdDate = XsdDateImpl(str)

        operator fun invoke(year: Int, month: Int, day: Int, timezoneOffset: Int? = null): XsdDate {
            return XsdDateImpl(year.toLong(), month, day, timezoneOffset)
        }

        operator fun invoke(year: Long, month: Int, day: Int, timezoneOffset: Int? = null): XsdDate {
            return XsdDateImpl(year, month, day, timezoneOffset)
        }

        operator fun invoke(year: Int, month: UInt, day: UInt, timezoneOffset: Int? = null): XsdDate {
            return XsdDateImpl(year.toLong(), month, day, timezoneOffset)
        }

        operator fun invoke(year: Long, month: UInt, day: UInt, timezoneOffset: Int? = null): XsdDate {
            return XsdDateImpl(year, month, day, timezoneOffset)
        }

        override fun deserialize(
            raw: String,
            input: XmlReader?
        ): XsdDate {
            return XsdDateImpl(raw)
        }
    }

}

