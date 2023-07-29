/*
 * Copyright (c) 2023.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances

open class VDateTime(
    final override val year: Int,
    final override val month: UInt,
    final override val day: UInt,
    final override val hour: UInt,
    final override val minute: UInt,
    final override val second: VDecimal,
    final override val timezoneOffset: Int? = null,
) : IDateTime {

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

    override val xmlString: String
        get() {
            return when {
                timezoneOffset == null ->
                    "${yearFrag()}-${monthFrag()}-${dayFrag()}T${hourFrag()}:${minuteFrag()}:${secondFrag()}"

                else ->
                    "${yearFrag()}-${monthFrag()}-${dayFrag()}T${hourFrag()}:${minuteFrag()}:${secondFrag()}${timeZoneFrag()}"
            }
        }
}
