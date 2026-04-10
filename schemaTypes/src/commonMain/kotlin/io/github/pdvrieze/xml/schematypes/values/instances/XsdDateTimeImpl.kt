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

import io.github.pdvrieze.xml.schematypes.RangeException
import io.github.pdvrieze.xml.schematypes.requireRange
import io.github.pdvrieze.xml.schematypes.types.DateTimeType
import io.github.pdvrieze.xml.schematypes.values.*
import kotlinx.datetime.*
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.xmlCollapseWhitespace
import kotlin.time.Instant

@XmlUtilInternal
open class XsdDateTimeImpl(
    final override val year: Int,
    final override val month: UInt,
    final override val day: UInt,
    final override val hour: UInt,
    final override val minute: UInt,
    final override val second: XsdDecimal,
    final override val timezoneOffset: Int? = null,
) : XsdDateTime {

    private constructor(dateTime: LocalDateTime, timezoneOffset: Int?) : this(
        year = dateTime.year,
        month = dateTime.month.number.toUInt(),
        day = dateTime.day.toUInt(),
        hour = dateTime.hour.toUInt(),
        minute = dateTime.minute.toUInt(),
        second = dateTime.nanosecond.let {// retain nano seconds
            when {
                it % 1_000_000_000 == 0 -> XsdInt(dateTime.second)
                else -> BigDecimal(it, 9) + XsdInt(dateTime.second)
            }
        },
        timezoneOffset = timezoneOffset,
    )

    constructor(instant: Instant, timezone: TimeZone?) : this(
        instant.toLocalDateTime(timezone ?: TimeZone.UTC),
        timezone?.offsetAt(instant)?.totalSeconds?.let { it / 60 }
    )

    constructor(date: XsdDate, time: XsdTime) : this(
        date.year,
        date.month,
        date.day,
        time.hour,
        time.minute,
        time.second,
        when {
            date.timezoneOffset == null -> time.timezoneOffset
            time.timezoneOffset == null || date.timezoneOffset == time.timezoneOffset -> date.timezoneOffset
            else -> throw IllegalArgumentException("Inconsistent timezone offsets: ${date.timezoneOffset} and ${time.timezoneOffset}")
        }
    )

    init {
        when (month) {
            1u, 3u, 5u, 7u, 8u, 10u, 12u -> requireRange(day in 1u..31u) { "Long months must have days 1..31 (was $day)" }
            4u, 6u, 9u, 11u -> requireRange(day in 1u..30u) { "Short months must have days 1..30 (was $day)" }
            2u -> {
                val isLeap = year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)
                val days = if (isLeap) 29u else 28u
                requireRange(day in 1u..days) { "February must have day $day in 1..$days" }
            }

            else -> throw IllegalArgumentException("Month value out of range: $month")
        }
        requireRange(hour in 0u..23u) { "Hour value $hour !in 0..23" }
        requireRange(minute in 0u..59u) { "Minute value $minute !in 0..59" }
        requireRange(second.toDouble() in 0.0..<60.0) { "Second value !in 0.0..<60.0" }
        requireRange(timezoneOffset == null || timezoneOffset in -840..840) { "Timezone offset must be in -840..840 or null, was: $timezoneOffset" }
    }

    override val xmlString: String
        get() = when {
        timezoneOffset == null ->
            "${yearFrag()}-${monthFrag()}-${dayFrag()}T${hourFrag()}:${minuteFrag()}:${secondFrag()}"

        else ->
            "${yearFrag()}-${monthFrag()}-${dayFrag()}T${hourFrag()}:${minuteFrag()}:${secondFrag()}${timeZoneFrag()}"
    }

    override fun toString(): String = xmlString

    override val schemaType: DateTimeType<XsdDateTime> get() = DateTimeType.Instance

    override fun ensureTimezone(fallbackTimezone: TimeZone): XsdDateTimeStamp = when (timezoneOffset) {
        null -> XsdDateTimeStampImpl(year, month, day, hour, minute, second, fallbackTimezone)
        else -> XsdDateTimeStampImpl(instant(), UtcOffset(minutes = timezoneOffset).asTimeZone())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is XsdDateTime) return false
        if ((timezoneOffset == null) != (other.timezoneOffset == null)) return false
        return instant() == other.instant()
    }

    override fun hashCode(): Int {
        return when (timezoneOffset) {
            null -> 31 + instant().hashCode()
            else -> instant().hashCode()
        }
    }

    companion object {
        internal fun timezoneFragValue(tz: CharSequence): Int? {
            if (tz.isEmpty()) return null
            if (tz == "Z") return 0 // handle Z case differently
            if (tz.length != 6) throw NumberFormatException("Timezone fragments are 6 characters long: '$tz'")
            val sign = when (tz[0]) {
                '+' -> false
                '-' -> true
                else -> throw NumberFormatException("Missing sign in timezone, found ${tz[0]}")
            }
            val hours = tz[1].digitToInt() * 10 + tz[2].digitToInt()
            if (hours !in 0..14) throw RangeException("Timezone hours must be between 0 and 14")
            if (tz[3] != ':') throw NumberFormatException("Missing : between hours and minutes in timezone")
            val minutes = tz[4].digitToInt() * 10 + tz[5].digitToInt()
            if (minutes !in 0..59) throw RangeException("Minutes must be between 0 and 59")
            return (if (sign) -1 else 1) * ((hours * 60) + minutes)
        }


        internal operator fun invoke(str: CharSequence): XsdDateTimeImpl {
            val s = xmlCollapseWhitespace(str)
            val tIndex = s.indexOf('T')
            require(tIndex >= 0)
            val digitOffset = if (s.startsWith('-')) 1 else 0
            var (year, month, day) = s.substring(digitOffset, tIndex).split('-').map { it.toInt() }
            var hour = s.substring(tIndex + 1, tIndex + 3).toUInt()
            if (s[tIndex + 3] != ':') throw NumberFormatException("Missing : separtor between hours and minutes")
            val minutes = s.substring(tIndex + 4, tIndex + 6).toUInt()
            if (s[tIndex + 6] != ':') throw NumberFormatException("Missing : separtor between minutes and seconds")
            val secEnd = ((tIndex + 7)..<s.length).firstOrNull {
                s[it] !in '0'..'9'
            }
            val seconds = s.substring(tIndex + 7, secEnd ?: s.length).toInt()
            var nanoEnd = secEnd
            val nanos = if (secEnd == null || s.getOrNull(secEnd) != '.') 0 else {
                nanoEnd = ((secEnd + 1)..<s.length).firstOrNull { s[it] !in '0'..'9' }
                val nanoStr = s.substring(secEnd + 1, nanoEnd ?: s.length)
                nanoStr.padStart(9, '0').toInt()
            }

            val tzOffset = nanoEnd?.let { timezoneFragValue(s.substring(it)) }

            if (hour == 24u) { // special case for 24:00:00 (needs next day)
                requireRange( minutes==0u && seconds== 0) { "Invalid time 24:$minutes:$seconds" }

                // let LocalDateTime handle this (overflow in dates is a mess)
                val tz = ((tzOffset?.let { UtcOffset(minutes = it) }) ?: UtcOffset.ZERO).asTimeZone()
                val dateTime = LocalDateTime(year, month, day, 23, minutes.toInt(), seconds, nanos)
                    .toInstant(tz)
                    .plus(1, DateTimeUnit.HOUR)
                    .toLocalDateTime(tz)

                year = dateTime.year
                month = dateTime.month.number
                day = dateTime.day
                hour = dateTime.hour.toUInt()
            }

            val secDec = when (nanos) {
                0 -> XsdInt(seconds)
                else -> BigDecimal(seconds.toLong()*1_000_000_000 + nanos, 9)
            }

            return XsdDateTimeImpl(
                if (digitOffset > 0) -year else year,
                month.toUInt(),
                day.toUInt(),
                hour,
                minutes,
                secDec,
                tzOffset
            )

        }

    }
}

