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

import io.github.pdvrieze.xml.schematypes.values.XSDateTime
import io.github.pdvrieze.xml.schematypes.values.XSDecimal
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.xmlCollapseWhitespace

@XmlUtilInternal
open class XSDateTimeImpl(
    final override val year: Int,
    final override val month: UInt,
    final override val day: UInt,
    final override val hour: UInt,
    final override val minute: UInt,
    final override val second: XSDecimal,
    final override val timezoneOffset: Int? = null,
) : XSDateTime {

    init {
        when (month) {
            1u, 3u, 5u, 7u, 8u, 10u, 12u -> require(day in 1u..31u) { "Long months must have days 1..31 (was $day)" }
            4u, 6u, 9u, 11u -> require(day in 1u..30u) { "Short months must have days 1..30 (was $day)" }
            2u -> {
                val isLeap = year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)
                val days = if (isLeap) 29u else 28u
                require(day in 1u..days) { "February must have day $day in 1..$days" }
            }

            else -> throw IllegalArgumentException("Month value out of range: $month")
        }
        require(hour in 0u..23u) { "Hour value $hour !in 0..23" }
        require(minute in 0u..59u) { "Minute value $minute !in 0..59" }
        require(second.toDouble() in 0.0..<60.0) { "Second value !in 0.0..<60.0" }
        require(timezoneOffset == null || timezoneOffset in -840..840) { "Timezone offset must be in -840..840 or null" }
    }

    override val xmlString: String get() = when {
        timezoneOffset == null ->
            "${yearFrag()}-${monthFrag()}-${dayFrag()}T${hourFrag()}:${minuteFrag()}:${secondFrag()}"

        else ->
            "${yearFrag()}-${monthFrag()}-${dayFrag()}T${hourFrag()}:${minuteFrag()}:${secondFrag()}${timeZoneFrag()}"
    }

    companion object {
        internal fun timezoneFragValue(tz: String): Int? {
            if (tz.isEmpty()) return null
            if (tz == "Z") return 0 // handle Z case differently
            if (tz.length != 6) throw NumberFormatException("Timezone fragments are 6 characters long: '$tz'")
            val sign = when (tz[0]) {
                '+' -> false
                '-' -> true
                else -> throw NumberFormatException("Missing sign in timezone, found ${tz[0]}")
            }
            val hours = tz[1].digitToInt() * 10 + tz[2].digitToInt()
            if (hours !in 0..14) throw NumberFormatException("Timezone hours must be between 0 and 14")
            if (tz[3] != ':') throw NumberFormatException("Missing : between hours and minutes in timezone")
            val minutes = tz[4].digitToInt() * 10 + tz[5].digitToInt()
            if (minutes !in 0..59) throw NumberFormatException("Minutes must be between 0 and 59")
            return (if (sign) -1 else 1) * ((hours * 60) + minutes)
        }


        internal operator fun invoke(str: String): XSDateTimeImpl {
            val s = xmlCollapseWhitespace(str)
            val tIndex = s.indexOf('T')
            require(tIndex >= 0)
            val (year, month, day) = s.substring(0, tIndex).split('-').map { it.toInt() }
            val hour = s.substring(tIndex + 1, tIndex + 3).toUInt()
            if (s[tIndex + 3] != ':') throw NumberFormatException("Missing : separtor between hours and minutes")
            val minutes = s.substring(tIndex + 4, tIndex + 6).toUInt()
            if (s[tIndex + 6] != ':') throw NumberFormatException("Missing : separtor between minutes and seconds")
            val secEnd = ((tIndex + 7)..<s.length).firstOrNull {
                s[it] != '.' && s[it] !in '0'..'9'
            }
            val seconds = XSDecimal(s.substring(tIndex + 7, secEnd ?: s.length))

            return when (secEnd) {
                null -> XSDateTimeImpl(
                    year,
                    month.toUInt(),
                    day.toUInt(),
                    hour,
                    minutes,
                    seconds
                )

                else -> {
                    val timezoneOffset = timezoneFragValue(s.substring(secEnd))
                    XSDateTimeImpl(
                        year,
                        month.toUInt(),
                        day.toUInt(),
                        hour,
                        minutes,
                        seconds,
                        timezoneOffset
                    )
                }
            }

        }

    }
}
