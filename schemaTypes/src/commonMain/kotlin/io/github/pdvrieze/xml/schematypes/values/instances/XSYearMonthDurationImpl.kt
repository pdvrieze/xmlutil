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

import io.github.pdvrieze.xml.schematypes.values.XSYearMonthDuration

class XSYearMonthDurationImpl(override val months: Long) : XSYearMonthDuration {
    val millis: Long get() = 0L

    operator fun compareTo(other: XSDurationImpl): Int = months.compareTo(other.months)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as XSDurationImpl

        return months == other.months
    }

    override fun hashCode(): Int {
        return months.hashCode()
    }

    override val xmlString: String
        get() = buildString {
            val aMonths: ULong
            val aMillis: ULong
            if (months < 0) {
                append('-')
                aMonths = (-months).toULong()
                aMillis = (-millis).toULong()
            } else {
                aMonths = months.toULong()
                aMillis = millis.toULong()
            }
            append('P')
            val y = aMonths / 12u
            val mo = aMonths % 12u
            if (y != 0uL) {
                append(y).append('Y')
            }
            if (mo != 0uL) {
                append(mo).append('M')
            }
        }

    companion object {
        operator fun invoke(representation: String): XSYearMonthDurationImpl {
            require(representation.length >= 3) // some value is needed with suffix
            var i = 0
            val sign = when {
                representation[0] == '-' -> {
                    ++i; -1
                }

                else -> 1
            }

            /** stages:
             *  0 -- nothing set
             *  1 -- year set
             *  2 -- month set
             *  3 -- days set -- ignored
             *  4 -- hours set -- ignored
             *  5 -- minutes set -- ignored
             *  6 -- seconds set -- ignored
             */
            var stage = 0
            var years = 0u
            var months = 0u

            require(representation[i++] == 'P')

            while (stage < 2 && i < representation.length && representation[i] != 'T') {
                var end = i
                while (end < representation.length && representation[end] in '0'..'9') {
                    ++end
                }
                when (representation[end]) {
                    'Y' -> {
                        require(stage < 1) { "Year must be the first fragment in a duration" }
                        years = representation.substring(i, end).toUInt()
                        stage = 1
                    }

                    'M' -> {
                        require(stage < 2) { "Month must be the first fragment in a duration" }
                        months = representation.substring(i, end).toUInt()
                        stage = 2
                    }

                    'D' -> throw IllegalArgumentException("YearMonthDuration does not support days: $representation")

                    else -> error("Unexpected yearmonth qualifier")
                }
                i = end + 1
            }
            require(i>=representation.length)

            val realMonths = (years * 12uL + months).toLong() * sign

            return XSYearMonthDurationImpl(realMonths)
        }
    }
}
