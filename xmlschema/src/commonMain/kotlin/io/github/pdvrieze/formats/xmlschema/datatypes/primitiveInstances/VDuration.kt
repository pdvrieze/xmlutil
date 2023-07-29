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

class VDuration(override val months: Long, val millis: Long) : IDuration {

    override val seconds: Double
        get() = millis.toDouble() / 1000.0

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
            val d = aMillis / (24u * 3600_000u)
            val h = (aMillis / 3600_000u) % 24u
            val mi = (aMillis / 60_000u) % 60u
            val ms = aMillis % 60_000u
            if (y != 0uL) {
                append(y).append('Y')
            }
            if (mo != 0uL) {
                append(mo).append('M')
            }
            if (d != 0uL) {
                append(d).append('D')
            }

            if (h != 0uL || mi != 0uL || ms != 0uL) {
                append('T')
                if (h != 0uL) {
                    append(h).append('H')
                }
                if (mi != 0uL) {
                    append(mi).append('M')
                }
                if (ms != 0uL) {
                    append(ms.toDouble() / 1000.0).append('S')
                }

            } else if (months == 0L && millis == 0L) {
                append("0D") // no days as zero value
            }
        }

    companion object {
        operator fun invoke(representation: String): VDuration {
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
             *  3 -- days set
             *  4 -- hours set
             *  5 -- minutes set
             *  6 -- seconds set
             */
            var stage = 0
            var years = 0u
            var months = 0u
            var days = 0u
            var hours = 0u
            var minutes = 0u
            var milliSeconds = 0uL

            require(representation[i++] == 'P')

            while (stage < 3 && i < representation.length && representation[i] != 'T') {
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

                    'D' -> {
                        require(stage < 3) { "Day must be the first fragment in a duration" }
                        days = representation.substring(i, end).toUInt()
                        stage = 3
                    }

                    else -> error("Unexpected yearmonth qualifier")
                }
                i = end + 1
            }
            if (i < representation.length) { //
                require(stage <= 3)
                stage = 3
                require(representation[i++] == 'T')

                while (stage < 6 && i < representation.length) {
                    var end = i
                    while (end < representation.length && (representation[end] == '.' || representation[end] in '0'..'9')) {
                        ++end
                    }
                    when (representation[end]) {
                        'H' -> {
                            require(stage < 4) { "minutes must be the first part of the time fragment" }
                            hours = representation.substring(i, end).toUInt()
                            stage = 4
                        }

                        'M' -> {
                            require(stage < 5) { "Minutes must be before seconds in a duration" }
                            minutes = representation.substring(i, end).toUInt()
                            stage = 5
                        }

                        'S' -> {
                            require(stage < 6) { "Secons must be the last fragment in a duration" }
                            milliSeconds = (representation.substring(i, end).toDouble()*1000.0).toULong()
                            stage = 6
                        }

                        else -> error("Unexpected daytime qualifier")
                    }
                    i = end + 1
                }
            }
            require(i>=representation.length)

            val realMonths = (years * 12uL + months).toLong() * sign
            val realMillis = ((((days * 24uL) + hours) * 60uL + minutes) * 60000uL + milliSeconds).toLong() * sign

            return VDuration(realMonths, realMillis)
        }
    }
}
