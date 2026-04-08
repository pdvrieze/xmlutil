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

package io.github.pdvrieze.xml.schematypes.values.formatters


class DateTimeFormatter(
    private val parts: List<DateTimePartFormatter>
) {

    constructor(picture: String) : this(parsePicture(picture))

    companion object {
        private fun parsePicture(picture: String): List<DateTimePartFormatter> {
            val parts = mutableListOf<DateTimePartFormatter>()
            var i = 0
            while (i < picture.length) {
                val idx = picture.indexOfAny(charArrayOf('[', ']'), i)
                if (idx == -1) {
                    parts.add(TextFormatter(picture.substring(i)))
                    break
                }
                when (picture[idx]) {
                    '[' -> when {
                        picture.getOrNull(idx + 1) == '[' -> {
                            parts.add(TextFormatter(picture.substring(i, idx + 1)))
                            i = idx + 2
                        }

                        else -> {
                            val markerEnd = picture.indexOf(']', idx + 1)
                            if (markerEnd == -1) throw IllegalArgumentException("Unclosed bracket")
                            parts.add(parseMarker(picture.substring(idx + 1, markerEnd)))
                            i = markerEnd + 1
                        }
                    }

                    ']' -> when {
                        picture.getOrNull(idx + 1) != ']' -> throw IllegalArgumentException("closing unopened bracket")
                        else -> {
                            parts.add(TextFormatter("]"))
                            i += 2
                        }
                    }

                    else -> error("Should not happen")
                }
            }
            // merge adjacent text parts
            return parts.fold(mutableListOf()) { list, part ->
                val last = list.lastOrNull()
                when (part) {
                    is TextFormatter if last is TextFormatter ->
                        list[list.lastIndex] = TextFormatter(last.text + part.text)

                    else -> list.add(part)
                }
                list
            }
        }

        private fun parseMarker(marker: String): DateTimePartFormatter {
            val markerContent = marker.substring(1).takeIf { it.isNotEmpty() }
            return when (marker[1]) {
                'Y' -> YearFormatter(markerContent ?: "1")
                'M' -> MonthInYearFormatter(markerContent ?: "1")
                'D' -> DayInMonthFormatter(markerContent ?: "1")
                'd' -> DayInYearFormatter(markerContent ?: "1")
                'F' -> DayOfWeekFormatter(markerContent ?: "n")
                'W' -> WeekInYearFormatter(markerContent ?: "1")
                'w' -> WeekInMonthFormatter(markerContent ?: "1")
                'H' -> Hour24InDayFormatter(markerContent ?: "1")
                'h' -> Hour12InDayFormatter(markerContent ?: "1")
                'P' -> AmPmMarkerFormatter(markerContent ?: "n")
                'm' -> MinuteInHourFormatter(markerContent ?: "01")
                's' -> SecondInMinuteFormatter(markerContent ?: "01")
                'f' -> FractionalSecondsFormatter(markerContent ?: "1")
                'Z' -> TimeZoneFormatter(markerContent ?: "01:01")
                'z' -> TimeZonePrefixedFormatter(markerContent ?: "01:01")
                'C' -> CalendarNameFormatter(markerContent ?: "n")
                'E' -> EraFormatter(markerContent ?: "n")
                else -> error("Unknown marker $marker")
            }
        }
    }

}


abstract class DateTimePartFormatter {
    protected open fun formatTo(dest: Appendable) {
        TODO("Not yet implemented")
    }


}

class TextFormatter(val text: String) : DateTimePartFormatter() {
    override fun formatTo(dest: Appendable) {
        dest.append(text)
    }
}

class YearFormatter() : DateTimePartFormatter() {
    constructor(markerContent: String): this()

}
class MonthInYearFormatter() : DateTimePartFormatter() {
    constructor(markerContent: String): this()

}
class DayInMonthFormatter() : DateTimePartFormatter() {
    constructor(markerContent: String): this()

}
class DayInYearFormatter() : DateTimePartFormatter() {
    constructor(markerContent: String): this()

}
class DayOfWeekFormatter() : DateTimePartFormatter() {
    constructor(markerContent: String): this()

}
class WeekInYearFormatter() : DateTimePartFormatter() {
    constructor(markerContent: String): this()

}
class WeekInMonthFormatter() : DateTimePartFormatter() {
    constructor(markerContent: String): this()

}
class Hour24InDayFormatter() : DateTimePartFormatter() {
    constructor(markerContent: String): this()

}
class Hour12InDayFormatter() : DateTimePartFormatter() {
    constructor(markerContent: String): this()

}
class AmPmMarkerFormatter() : DateTimePartFormatter() {
    constructor(markerContent: String): this()

}
class MinuteInHourFormatter() : DateTimePartFormatter() {
    constructor(markerContent: String): this()

}
class SecondInMinuteFormatter() : DateTimePartFormatter() {
    constructor(markerContent: String): this()

}
class FractionalSecondsFormatter() : DateTimePartFormatter() {
    constructor(markerContent: String): this()

}
class TimeZoneFormatter() : DateTimePartFormatter() {
    constructor(markerContent: String): this()

}
class TimeZonePrefixedFormatter() : DateTimePartFormatter() {
    constructor(markerContent: String): this()

}
class CalendarNameFormatter() : DateTimePartFormatter() {
    constructor(markerContent: String): this()

}
class EraFormatter() : DateTimePartFormatter() {
    constructor(markerContent: String): this()

}
