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

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toInstant
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
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

    fun monthFrag(): String = month?.toString() ?: ""
    fun dayFrag(): String = day?.toString() ?: ""
    fun hourFrag(): String = hour?.toString() ?: ""
    fun minuteFrag(): String = minute?.toString() ?: ""
    fun secondFrag(): String =
        (second as? XsdInteger)?.run { toInt().toString() } ?: second?.run { toDouble().toString() } ?: ""

    fun timeZoneFrag(): String = when (val it = timezoneOffset) {
        null -> ""
        0 -> "Z"
        else -> {
            val sign = if (it >= 0) '+' else '-'
            val hours = (it / 60).toString().padStart(2, '0')
            val minutes = (it % 60).toString().padStart(2, '0')
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
        val zoneOffset = timezoneOffset?.let { UtcOffset(seconds = it * 60) } ?: UtcOffset.ZERO
        return dateTime.toInstant(zoneOffset)
    }

    operator fun compareTo(other: XsdDateTime): Int {
        return instant().compareTo(other.instant())
    }

}
