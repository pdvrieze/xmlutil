/*
 * Copyright (c) 2025-2026.
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
import io.github.pdvrieze.xml.schematypes.types.DateTimeType
import io.github.pdvrieze.xml.schematypes.values.instances.XsdDateTimeImpl
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlReader
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@ExperimentalXmlUtilApi
@OptIn(ExperimentalTime::class)
@Serializable(XsdDateTime.Companion::class)
interface XsdDateTime : IXsdDateTime, XsdPrimitive {
    override val schemaType: DateTimeType<XsdDateTime>

    override val year: Int
    override val month: UInt
    override val day: UInt
    override val hour: UInt
    override val minute: UInt
    override val second: XsdDecimal

    override fun compareTo(other: XsdPrimitive, collation: Collation): Int {
        return when (other) {
            is IXsdDateTime -> compareTo(other)
            else -> throw IllegalArgumentException("Cannot compare $this with $other")
        }
    }

    fun toDate(): XsdDate = XsdDate(year, month, day, timezoneOffset)
    fun toTime(): XsdTime = XsdTime(hour, minute, second, timezoneOffset)
    fun toLocalDateTime(fallbackTimezone: TimeZone = TimeZone.UTC): LocalDateTime = instant().toLocalDateTime(timeZone ?: fallbackTimezone)

    override fun ensureTimezone(fallbackTimezone: TimeZone): XsdDateTimeStamp

    operator fun plus(duration: XsdDuration): XsdDateTime {
        val newBase = when (val monthsToAdd = duration.months) {
            0L -> instant()
            else -> {
                // todo check validity for dates below 0
                val fullMonths = year * 12 + month.toInt() + monthsToAdd
                val newYear = (fullMonths/12L).toInt()
                val newMonth = (fullMonths.absoluteValue % 12).toUInt()
                XsdDateTimeImpl(newYear, newMonth, day, hour, minute, second, timezoneOffset).instant()
            }
        }
        val newInstant = when (val millisToAdd = duration.millis) {
            0L -> newBase
            else -> newBase + millisToAdd.milliseconds
        }
        return XsdDateTimeImpl(newInstant, timeZone)
    }

    operator fun minus(duration: XsdDuration): XsdDateTime {
        val newBase = when (val monthsToSubtract = duration.months) {
            0L -> instant()
            else -> {
                // todo check validity for dates below 0
                val fullMonths = year * 12 + month.toInt() - monthsToSubtract
                val newYear = (fullMonths/12L).toInt()
                val newMonth = (fullMonths.absoluteValue % 12).toUInt()
                XsdDateTimeImpl(newYear, newMonth, day, hour, minute, second, timezoneOffset).instant()
            }
        }
        val newInstant = when (val millisToSubtract = duration.millis) {
            0L -> newBase
            else -> newBase - millisToSubtract.milliseconds
        }
        return XsdDateTimeImpl(newInstant, timeZone)
    }

    operator fun minus(other: XsdDateTime): XsdDayTimeDuration {
        val diff = (instant()-other.instant())
        return XsdDayTimeDuration(diff.inWholeMilliseconds)
    }

    companion object: SimpleTypeSerializer<XsdDateTime>("xsd.dateTime") {

        operator fun invoke(str: CharSequence): XsdDateTime = XsdDateTimeImpl(str)

        operator fun invoke(
            year: Int,
            month: UInt,
            day: UInt,
            hour: UInt,
            minute: UInt,
            second: XsdDecimal,
            timezoneOffset: Int? = null
        ): XsdDateTime {
            return XsdDateTimeImpl(year, month, day, hour, minute, second, timezoneOffset)
        }

        operator fun invoke(instant: Instant, timezone: TimeZone?) : XsdDateTime {
            return XsdDateTimeImpl(instant, timezone)
        }

        operator fun invoke(date: XsdDate, time: XsdTime): XsdDateTime {
            return XsdDateTimeImpl(date, time)
        }

        override fun deserialize(raw: String, input: XmlReader?): XsdDateTime {
            return invoke(raw)
        }
    }
}

