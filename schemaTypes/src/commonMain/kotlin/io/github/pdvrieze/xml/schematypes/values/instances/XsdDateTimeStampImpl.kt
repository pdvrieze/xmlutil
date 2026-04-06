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
import io.github.pdvrieze.xml.schematypes.values.XsdDateTimeStamp
import io.github.pdvrieze.xml.schematypes.values.XsdDecimal
import io.github.pdvrieze.xml.schematypes.values.XsdInt
import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.number
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import nl.adaptivity.xmlutil.xmlCollapseWhitespace
import kotlin.time.Instant

class XsdDateTimeStampImpl(
    val instant: Instant,
    val timezone: TimeZone = TimeZone.UTC,
) : XsdDateTimeStamp {
    override val schemaType: DateTimeType<XsdDateTimeStamp> get() = DateTimeStampType.Instance

    val localDateTime: LocalDateTime get() = instant.toLocalDateTime(timezone)

    override val year: Int get()= localDateTime.year
    override val month: UInt get()= localDateTime.month.number.toUInt()
    override val day: UInt get()= localDateTime.day.toUInt()
    override val hour: UInt get()= localDateTime.hour.toUInt()
    override val minute: UInt get()= localDateTime.minute.toUInt()
    override val second: XsdInt get()= XsdInt(localDateTime.second)
    override val timezoneOffset: Int get() = timezone.offsetAt(instant).totalSeconds/60

    override fun instant(): Instant = instant

    override val xmlString: String
        get() = "${yearFrag()}-${monthFrag()}-${dayFrag()}T${hourFrag()}:${minuteFrag()}:${secondFrag()}${timeZoneFrag()}"

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
            val localDateTime = LocalDateTime(year, month, day, hour, minute, second.toInt())
            val timezone = FixedOffsetTimeZone(UtcOffset(minutes = timezoneOffset))
            val instant = localDateTime.toInstant(timezone)
            return XsdDateTimeStampImpl(instant, timezone)
        }


        internal operator fun invoke(str: CharSequence): XsdDateTimeStampImpl {
            val s = xmlCollapseWhitespace(str)
            val tIndex = s.indexOf('T')
            require(tIndex >= 0)
            val (year, month, day) = s.substring(0, tIndex).split('-').map { it.toInt() }
            val hour = s.substring(tIndex + 1, tIndex + 3).toInt()
            if (s[tIndex + 3] != ':') throw NumberFormatException("Missing : separtor between hours and minutes")
            val minutes = s.substring(tIndex + 4, tIndex + 6).toInt()
            if (s[tIndex + 6] != ':') throw NumberFormatException("Missing : separtor between minutes and seconds")
            val secEnd = ((tIndex + 7)..<s.length).first {
                s[it] != '.' && s[it] !in '0'..'9'
            }
            val seconds = XsdDecimal(s.substring(tIndex + 7, secEnd))

            val timezoneOffset = requireNotNull(XsdDateTimeImpl.timezoneFragValue(s.substring(secEnd))) {
                "Missing timezone offset"
            }

            return invoke(year, month, day, hour, minutes, seconds, timezoneOffset)
        }

    }
}
