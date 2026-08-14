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

import io.github.pdvrieze.xml.schematypes.types.DateTimeStampType
import io.github.pdvrieze.xml.schematypes.types.DateTimeType
import io.github.pdvrieze.xml.schematypes.values.IXsdDateTime.Companion.splitToSecondsAndNanos
import io.github.pdvrieze.xml.schematypes.values.XsdDateTime
import io.github.pdvrieze.xml.schematypes.values.XsdDateTimeStamp
import io.github.pdvrieze.xml.schematypes.values.XsdDecimal
import io.github.pdvrieze.xml.schematypes.values.XsdInt
import kotlinx.datetime.*
import nl.adaptivity.xmlutil.xmlCollapseWhitespace
import kotlin.time.Instant

class XsdDateTimeStampImpl(
    val instant: Instant,
    override val timezone: TimeZone = TimeZone.UTC,
) : XsdDateTimeStamp {
    override val schemaType: DateTimeType<XsdDateTimeStamp> get() = DateTimeStampType.Instance

    val localDateTime: LocalDateTime get() = instant.toLocalDateTime(timezone)

    override val year: Int get()= localDateTime.year
    override val month: UInt get()= localDateTime.month.number.toUInt()
    override val day: UInt get()= localDateTime.day.toUInt()
    override val hour: UInt get()= localDateTime.hour.toUInt()
    override val minute: UInt get()= localDateTime.minute.toUInt()
    override val second: XsdDecimal
        get() = when (val ns = localDateTime.nanosecond) {
            0 -> XsdInt(localDateTime.second)
            else -> BigDecimal(ns, -9) + XsdInt(localDateTime.second)
        }
    override val timezoneOffset: Int get() = timezone.offsetAt(instant).totalSeconds/60

    override fun instant(): Instant = instant

    override val xmlString: String
        get() = "${yearFrag()}-${monthFrag()}-${dayFrag()}T${hourFrag()}:${minuteFrag()}:${secondFrag()}${timeZoneFrag()}"


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is XsdDateTime) return false
        if (other.timezoneOffset == null) return false
        return instant() == other.instant()
    }

    override fun hashCode(): Int {
        return instant().hashCode()
    }

    override fun toString(): String = xmlString


    companion object {
        internal operator fun invoke(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int,
            second: XsdDecimal,
            timezoneOffset: Int
        ): XsdDateTimeStampImpl {
            val timezone = FixedOffsetTimeZone(UtcOffset(minutes = timezoneOffset))
            return invoke(year, month, day, hour, minute, second, timezone)
        }

        internal operator fun invoke(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int,
            second: XsdDecimal,
            timezone: TimeZone
        ): XsdDateTimeStampImpl {
            val t = second.splitToSecondsAndNanos()
            val localDateTime = LocalDateTime(year, month, day, hour, minute, (t shr 32).toInt(), t.toInt())
            val instant = localDateTime.toInstant(timezone)
            return XsdDateTimeStampImpl(instant, timezone)
        }

        internal operator fun invoke(
            year: Int,
            month: UInt,
            day: UInt,
            hour: UInt,
            minute: UInt,
            second: XsdDecimal,
            timezoneOffset: Int
        ): XsdDateTimeStampImpl {
            val timezone = FixedOffsetTimeZone(UtcOffset(minutes = timezoneOffset))
            return invoke(year, month, day, hour, minute, second, timezone)
        }

        internal operator fun invoke(
            year: Int,
            month: UInt,
            day: UInt,
            hour: UInt,
            minute: UInt,
            second: XsdDecimal,
            timezone: TimeZone
        ): XsdDateTimeStampImpl {
            val t = second.splitToSecondsAndNanos()
            val localDateTime = LocalDateTime(year, month.toInt(), day.toInt(), hour.toInt(), minute.toInt(), (t shr 32).toInt(), t.toInt())

            val instant = localDateTime.toInstant(timezone)
            return XsdDateTimeStampImpl(instant, timezone)
        }


        internal operator fun invoke(str: CharSequence): XsdDateTimeStampImpl {
            val s = xmlCollapseWhitespace(str)
            val tIndex = s.indexOf('T')
            require(tIndex >= 0)
            val (year, month, day) = s.substring(0, tIndex).split('-').map { it.xsToInt() }
            val hour: Int
            hour = s.substring(tIndex + 1, tIndex + 3).xsToInt()
            if (s[tIndex + 3] != ':') throw NumberFormatException("Missing : separator between hours and minutes")
            val minutes = s.substring(tIndex + 4, tIndex + 6).xsToInt()
            if (s[tIndex + 6] != ':') throw NumberFormatException("Missing : separator between minutes and seconds")
            val secEnd = ((tIndex + 7)..<s.length).firstOrNull() {
                s[it] != '.' && s[it] !in '0'..'9'
            } ?: throw NumberFormatException("Missing timezone")
//            if (s[secEnd] == '.') {
//
//            }

            val seconds = XsdDecimal(s.substring(tIndex + 7, secEnd))

            val timezoneOffset = requireNotNull(XsdDateTimeImpl.timezoneFragValue(s.substring(secEnd))) {
                "Missing timezone offset"
            }

            return invoke(year, month, day, hour, minutes, seconds, timezoneOffset)
        }

    }
}
