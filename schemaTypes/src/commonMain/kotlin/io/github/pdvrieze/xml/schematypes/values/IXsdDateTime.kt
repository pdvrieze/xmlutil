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
import kotlinx.datetime.*
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
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
        (second as? XsdInteger)?.run { toInt().toString().padStart(2, '0') } ?: second?.run { toDouble().toString().let { s ->
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
        val dateTime = LocalDateTime(
            year ?: 0,
            month?.toInt() ?: 1,
            day?.toInt() ?: 1,
            hour?.toInt() ?: 0,
            minute?.toInt() ?: 0,
            second?.toDouble()?.toInt() ?: 0
        )

        return dateTime.toInstant(timeZone ?: TimeZone.UTC)
    }

    val timeZone: TimeZone?
        get() = timezoneOffset?.let { UtcOffset(minutes = it).asTimeZone() }

    operator fun compareTo(other: IXsdDateTime): Int {
        return instant().compareTo(other.instant())
    }

    fun ensureTimezone(fallbackTimezone: TimeZone): IXsdDateTime


    fun format(picture: String, language: XsdLanguage, calendar: String? = null, place: String? = null): String {
        val formatter = DateTimeFormatter(picture, language, calendar, place)
        return formatter.format(this)
    }

}
