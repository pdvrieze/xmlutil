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

import io.github.pdvrieze.xml.schematypes.values.formatters.DateTimeFormatter
import io.github.pdvrieze.xml.schematypes.values.instances.BigDecimal
import kotlinx.datetime.*
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlUtilInternal
import kotlin.math.absoluteValue
import kotlin.time.Instant

/**
 * Interface that is shared among the date/time types to clarify that XSDateTime is an
 * independent type
 */
@ExperimentalXmlUtilApi
interface IXsdDateTime: XsdAtomic {
    /** any integer */
    val year: Int?

    /** 1..12 */
    val month: UInt?

    /** 1..31 or further restricted on month */
    val day: UInt?

    /** 0..23 */
    val hour: UInt?

    /** 0..59 */
    val minute: UInt?

    /** A decimal [0.0, 60.0> */
    val second: XsdDecimal?

    /**
     * Minutes offset from UTC
     */
    val timezoneOffset: Int?

    fun yearFrag(): String = year?.let{ // it must pad to at least 4 digits
        if(it<0) "-${(-it).toString().padStart(4, '0')}" else it.toString().padStart(4, '0')
    } ?: ""

    fun monthFrag(): String = month?.toString()?.padStart(2, '0') ?: ""
    fun dayFrag(): String = day?.toString()?.padStart(2, '0') ?: ""
    fun hourFrag(): String = hour?.toString()?.padStart(2, '0') ?: ""
    fun minuteFrag(): String = minute?.toString()?.padStart(2, '0') ?: ""
    fun secondFrag(): String =
        (second as? XsdInteger)?.run { toInt().toString().padStart(2, '0') } ?: second?.run { xmlString.let { s ->
            s.padStart(
            2 + s.length - s.indexOf('.'),
            '0'
        )
        } } ?: ""

    fun timeZoneFrag(): String = when (val it = timezoneOffset) {
        null -> ""
        0 -> "Z"
        else -> {
            val sign = if (it >= 0) '+' else '-'
            val abs = it.absoluteValue
            val hours = (abs / 60).toString().padStart(2, '0')
            val minutes = (abs % 60).toString().padStart(2, '0')
            "$sign$hours:$minutes"
        }
    }


    fun instant(): Instant {
        val t = second?.splitToSecondsAndNanos() ?: 0uL

        val dateTime = LocalDateTime(
            year ?: 1972,
            month?.toInt() ?: 12,
            day?.toInt() ?: 1,
            hour?.toInt() ?: 0,
            minute?.toInt() ?: 0,
            (t shr 32).toInt(),
            t.toInt(),
        )

        return dateTime.toInstant(timezone ?: TimeZone.UTC)
    }

    fun toStandardDateTime(): XsdDateTime

    val timezone: TimeZone?
        get() = timezoneOffset?.let { UtcOffset(minutes = it).asTimeZone() }

    operator fun compareTo(other: IXsdDateTime): Int {
        return instant().compareTo(other.instant())
    }

    fun ensureTimezone(fallbackTimezone: TimeZone): IXsdDateTime


    fun format(picture: String, language: XsdLanguage, calendar: QName? = null, place: String? = null): String {
        val formatter = DateTimeFormatter(picture, language, calendar, place)
        return formatter.format(this)
    }

    fun weekOfMonth(): Int? {
        val date = year?.let {
            LocalDate(it, month?.toInt() ?: 1, day?.toInt() ?: 1)
        } ?: return null

        val refDay = LocalDate(date.year, date.month.number, 11)
        var differenceInDays = date.dayOfYear + 7 - (refDay.dayOfYear - (refDay.dayOfWeek.isoDayNumber - 1))
        if (differenceInDays < 0) {
            val refDay2 = when (date.month.number) { //just manually handle year wrapping
                1 -> LocalDate(date.year - 1, 12, 11)
                else -> LocalDate(date.year, date.month.number - 1, 11)
            }
            val weekEarly = date.minus(7, DateTimeUnit.DAY)
            differenceInDays = weekEarly.dayOfYear + 14 - (refDay2.dayOfYear - (refDay2.dayOfWeek.isoDayNumber - 1))
        }

        // move up (and down) to handle with div rounding to zero (as weeks start with 1 adding 7 before diff is the same as adding 1 after)
        return ((differenceInDays + 7) / 7)
    }

    fun weekOfYear(): Int? {
        val date = year?.let {
            LocalDate(it, month?.toInt() ?: 1, day?.toInt() ?: 1)
        } ?: return null

        // Use week 2 not to deal with previous years
        val refDay = LocalDate(date.year, 1, 11)
        // compensate for week 2 by adding 7, then also compensate for the day number
        // this is more complex (nested subtractions) to provide logical clarity
        var differenceInDays = date.dayOfYear + 7 - (refDay.dayOfYear - (refDay.dayOfWeek.isoDayNumber - 1))

        if (differenceInDays < 0) { // Have to deal with previous year here to determine 52 or 53 weeks
            val rd2 = LocalDate(date.year - 1, 1, 11)
            val lastDayOfYear = LocalDate(date.year -1 , 12, 31)
            differenceInDays = lastDayOfYear.dayOfYear - rd2.minus(rd2.dayOfWeek.isoDayNumber -1, DateTimeUnit.DAY).dayOfYear + 7
        }

        // move up (and down) to handle with div rounding to zero (as weeks start with 1 adding 7 before diff is the same as adding 1 after)
        return ((differenceInDays + 7) / 7)
    }

    companion object {

        @XmlUtilInternal
        internal fun XsdDecimal.splitToSecondsAndNanos(): ULong {
            return when (this) {
                is XsdInteger -> toULong() shl 32
                is BigDecimal if (isInteger) -> toULong() shl 32
                else -> {
                    val (sec, rem) = toBigDecimal().divRem(1u)
                    val nano = rem.exp10(9)
                    sec.toULong() shl 32 or nano.toULong()
                }
            }

        }
    }


}
