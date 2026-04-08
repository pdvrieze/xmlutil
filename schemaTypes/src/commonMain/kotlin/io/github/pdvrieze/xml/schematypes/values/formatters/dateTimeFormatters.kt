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

import io.github.pdvrieze.xml.schematypes.values.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import nl.adaptivity.xmlutil.core.internal.appendCodepoint
import nl.adaptivity.xmlutil.core.internal.codepointAt


class DateTimeFormatter private constructor(
    private val parts: List<DateTimePartFormatter>,
    private val language: XsdLanguage = XsdLanguage("en"),
    private val calendar: String? = null,
    private val place: String? = null
) {

    constructor(picture: String, language: XsdLanguage, calendar: String? = null, place: String? = null) : this(parsePicture(picture, language), language, calendar, place)

    fun format(dateTime: IXsdDateTime): String {
        return buildString {
            for (part in parts) {
                part.formatTo(this, dateTime)
            }
        }
    }

    companion object {
        private fun parsePicture(picture: String, language: XsdLanguage): List<DateTimePartFormatter> {
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
                            if (idx > i) { parts.add(TextFormatter(picture.substring(i, idx))) }
                            val markerEnd = picture.indexOf(']', idx + 1)
                            if (markerEnd == -1) throw IllegalArgumentException("Unclosed bracket")
                            parts.add(parseMarker(picture.substring(idx + 1, markerEnd), language))
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

        private fun parseMarker(marker: String, lang: XsdLanguage): DateTimePartFormatter {
            val widthModIdx = marker.lastIndexOf(',')
            val widthModifier = if (widthModIdx >= 0) WidthModifier(marker.substring(widthModIdx + 1)) else null
            val markerContent = when {
                widthModIdx >= 0 -> marker.substring(0, widthModIdx)
                else -> marker.substring(1).takeIf { it.isNotEmpty() }
            }
            return when (marker[0]) {
                'Y' -> YearFormatter(markerContent ?: "1", widthModifier, lang)
                'M' -> when (markerContent) {
                    "n" -> MonthNameInYearFormatter(Case.LOWER, lang, widthModifier)
                    "Nn" -> MonthNameInYearFormatter(Case.TITLE, lang, widthModifier)
                    "N" -> MonthNameInYearFormatter(Case.UPPER, lang, widthModifier)
                    else -> MonthInYearFormatter(markerContent ?: "1", lang, widthModifier)
                }
                'D' -> DayInMonthFormatter(markerContent ?: "1", widthModifier, lang)
                'd' -> DayInYearFormatter(markerContent ?: "1", widthModifier, lang)
                'F' -> when (markerContent) {
                    null, "n" -> DayNameInWeekAsTextFormatter(Case.LOWER, lang, widthModifier)
                    "N" -> DayNameInWeekAsTextFormatter(Case.UPPER, lang, widthModifier)
                    "Nn" -> DayNameInWeekAsTextFormatter(Case.TITLE, lang, widthModifier)
                    else -> DayOfWeekFormatter(markerContent, widthModifier, lang)
                }
                'W' -> WeekInYearFormatter(markerContent ?: "1", widthModifier, lang)
                'w' -> WeekInMonthFormatter(markerContent ?: "1", widthModifier, lang)
                'H' -> Hour24InDayFormatter(markerContent ?: "1", widthModifier, lang)
                'h' -> Hour12InDayFormatter(markerContent ?: "1", widthModifier, lang)
                'P' -> AmPmMarkerFormatter(markerContent ?: "n", lang)
                'm' -> MinuteInHourFormatter(markerContent ?: "01", lang, widthModifier)
                's' -> SecondInMinuteFormatter(markerContent ?: "01", widthModifier, lang)
                'f' -> FractionalSecondsFormatter(markerContent ?: "1", widthModifier, lang)
                'Z' -> TimeZoneFormatter(markerContent ?: "01:01", lang)
                'z' -> TimeZonePrefixedFormatter(markerContent ?: "01:01", lang)
                'C' -> CalendarNameFormatter(markerContent ?: "n", lang)
                'E' -> EraFormatter(markerContent ?: "n", lang)
                else -> error("Unknown marker $marker")
            }
        }
    }

}


private abstract class DateTimePartFormatter protected constructor(val widthModifier: WidthModifier?) {

    open fun formatTo(dest: Appendable, dateTime: IXsdDateTime) {
        TODO("Not yet implemented")
    }

    protected fun toLocalDateTime(dateTime: IXsdDateTime, fallbackTimezone: TimeZone = TimeZone.UTC): LocalDateTime? = when (dateTime) {
        is XsdDate -> XsdDateTime(dateTime, XsdTime(1u, 1u, 0u)).toLocalDateTime(fallbackTimezone)
        is XsdDateTime -> dateTime.toLocalDateTime(fallbackTimezone)
        else -> return null
    }

    protected fun toLocalDate(dateTime: IXsdDateTime): LocalDate? {
        return LocalDate(
            dateTime.year ?: return null,
            dateTime.month?.toInt() ?: return null,
            dateTime.day?.toInt() ?: return null,
        )
    }
}

private abstract class NumericFormatter(val intFormat: IntegerFormatter, widthModifier: WidthModifier?): DateTimePartFormatter(widthModifier) {
    protected open fun getValue(dateTime: IXsdDateTime): Long? = throw UnsupportedOperationException("Not implemented")
    protected open fun getXsdValue(dateTime: IXsdDateTime): XsdInteger? = getValue(dateTime)?.let { XsdLong(it) }

    private fun adjustWithFormatter(widthModifier: WidthModifier?, formatter: IntegerFormatter, minDigits: Int): WidthModifier {
        val newMin: Int
        if (widthModifier != null) {
            if (widthModifier.maxWidth >= 0) {
                return widthModifier
            }
            newMin = widthModifier.minWidth
        } else {
            newMin = formatter.minDigits
        }
        val fTotalDigits = formatter.totalDigitCount
        return when {
            fTotalDigits >= minDigits -> WidthModifier(newMin, fTotalDigits)
            else -> WidthModifier(newMin)
        }
    }

    fun formatWithClipping(dest: Appendable, dateTime: IXsdDateTime, minDigits: Int) {
        val adjustedWidthModifier = adjustWithFormatter(widthModifier, intFormat, minDigits)

        appendClipped(dateTime, adjustedWidthModifier, dest)
    }

    private fun appendClipped(
        dateTime: IXsdDateTime,
        widthModifier: WidthModifier,
        dest: Appendable
    ) {
        val elemValue = getXsdValue(dateTime) ?: throw IllegalArgumentException("Format requires a value not provided")
        when {
            widthModifier.minWidth > 1 || widthModifier.maxWidth >= 0 -> {
                val s = intFormat.format(elemValue)
                when {
                    s.length < widthModifier.minWidth -> // deal with surrogate pairs
                        dest.append(s.padStart(widthModifier.minWidth, '0'))

                    s.length > widthModifier.maxWidth ->
                        dest.appendRange(s, s.length - widthModifier.maxWidth, s.length)

                    else -> dest.append(s)
                }
            }

            else -> intFormat.formatTo(dest, elemValue)
        }
    }

    override fun formatTo(dest: Appendable, dateTime: IXsdDateTime) {
        val elemValue = getXsdValue(dateTime) ?: throw IllegalArgumentException("Format requires a value not provided")
        val m = widthModifier
        if (m != null && (m.maxWidth >= 0 || m.minWidth > 1)) {
            appendClipped(dateTime, m, dest)
        } else {
            intFormat.formatTo(dest, elemValue)
        }
    }
}

private class TextFormatter(val text: String) : DateTimePartFormatter(null) {
    override fun formatTo(dest: Appendable, dateTime: IXsdDateTime) {
        dest.append(text)
    }
}

private class YearFormatter(format: IntegerFormatter, widthModifier: WidthModifier?) :
    NumericFormatter(format, widthModifier) {
    constructor(markerContent: String, widthModifier: WidthModifier?, lang: XsdLanguage):
            this(IntegerFormatter(markerContent, lang), widthModifier)

    override fun formatTo(dest: Appendable, dateTime: IXsdDateTime) {
        formatWithClipping(dest, dateTime, 2)
    }

    override fun getValue(dateTime: IXsdDateTime): Long? = dateTime.year?.toLong()
    override fun toString(): String {
        return "Y$intFormat"
    }
}

private class MonthInYearFormatter(format: IntegerFormatter, widthModifier: WidthModifier?) :
    NumericFormatter(format, widthModifier) {
    constructor(markerContent: String, lang: XsdLanguage, widthModifier: WidthModifier?): this(
        IntegerFormatter(markerContent, lang),
        widthModifier
    )

    override fun getValue(dateTime: IXsdDateTime): Long? = dateTime.month?.toLong()
}

private class MonthNameInYearFormatter(val case: Case, val lang: XsdLanguage, widthModifier: WidthModifier?) :
    DateTimePartFormatter(widthModifier) {
    override fun formatTo(dest: Appendable, dateTime: IXsdDateTime) {
        val month = requireNotNull(dateTime.month).toInt() - 1
        when (case) {
            Case.UPPER -> dest.append(months[month].uppercase())
            Case.LOWER -> dest.append(months[month].lowercase())
            Case.TITLE -> dest.append(months[month])
        }
    }

    companion object {
        private val months = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
    }
}

private class DayInMonthFormatter(format: IntegerFormatter, widthModifier: WidthModifier?) :
    NumericFormatter(format, widthModifier) {
    constructor(markerContent: String, widthModifier: WidthModifier?, lang: XsdLanguage): this(
        IntegerFormatter(markerContent, lang),
        widthModifier
    )

    override fun getValue(dateTime: IXsdDateTime): Long? = dateTime.day?.toLong()
}

private class DayInYearFormatter(format: IntegerFormatter, widthModifier: WidthModifier?) :
    NumericFormatter(format, widthModifier) {
    constructor(markerContent: String, widthModifier: WidthModifier?, lang: XsdLanguage): this(
        IntegerFormatter(markerContent, lang),
        widthModifier
    )

    override fun getValue(dateTime: IXsdDateTime): Long? {
        return toLocalDate(dateTime)?.run { dayOfYear.toLong() + 1L }
    }
}

private class DayOfWeekFormatter(format: IntegerFormatter, widthModifier: WidthModifier?) : NumericFormatter(
    format,
    widthModifier
) {
    constructor(markerContent: String, widthModifier: WidthModifier?, lang: XsdLanguage): this(
        IntegerFormatter(markerContent, lang),
        widthModifier
    )

    override fun getValue(dateTime: IXsdDateTime): Long? {
        return toLocalDate(dateTime)?.run { dayOfWeek.ordinal.toLong() + 1 }
    }
}

private class DayNameInWeekAsTextFormatter(val case: Case, val lang: XsdLanguage, widthModifier: WidthModifier?) : DateTimePartFormatter(
    widthModifier
) {
    override fun formatTo(dest: Appendable, dateTime: IXsdDateTime) {
        val localDateTime = toLocalDate(dateTime) ?: return
        dest.append(case.adjust(localDateTime.dayOfWeek.name))
    }
}

private class WeekInYearFormatter(format: IntegerFormatter, widthModifier: WidthModifier?) : NumericFormatter(
    format,
    widthModifier
) {
    constructor(markerContent: String, widthModifier: WidthModifier?, lang: XsdLanguage): this(
        IntegerFormatter(markerContent, lang),
        widthModifier
    )

    override fun getValue(dateTime: IXsdDateTime): Long? {
        val date = toLocalDate(dateTime) ?: return null
        val firstDayOfYear = LocalDate(date.year, 1, 1)
        val daysFromFirstDay = date.dayOfYear - firstDayOfYear.dayOfYear
        val firstDayOfWeek = firstDayOfYear.dayOfWeek.isoDayNumber
        return (((daysFromFirstDay + firstDayOfWeek - 1) / 7) + 1).toLong()
    }
}

private class WeekInMonthFormatter(format: IntegerFormatter, widthModifier: WidthModifier?) : NumericFormatter(
    format,
    widthModifier
) {
    constructor(markerContent: String, widthModifier: WidthModifier?, lang: XsdLanguage): this(
        IntegerFormatter(markerContent, lang),
        widthModifier
    )

    override fun getValue(dateTime: IXsdDateTime): Long? {
        val date = toLocalDate(dateTime) ?: return null
        val firstDayOfMonth = LocalDate(date.year, date.month, 1)
        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.isoDayNumber
        return (((date.day + firstDayOfWeek - 1) / 7) + 1).toLong()
    }
}

private class Hour24InDayFormatter(format: IntegerFormatter, widthModifier: WidthModifier?) :
    NumericFormatter(format, widthModifier) {
    constructor(markerContent: String, widthModifier: WidthModifier?, lang: XsdLanguage): this(
        IntegerFormatter(markerContent, lang),
        widthModifier
    )

    override fun getValue(dateTime: IXsdDateTime): Long? = dateTime.hour?.let { (((it +23u) % 24u) + 1u).toLong() }
}

private class Hour12InDayFormatter(format: IntegerFormatter, widthModifier: WidthModifier?) : NumericFormatter(
    format,
    widthModifier
) {
    constructor(markerContent: String, widthModifier: WidthModifier?, lang: XsdLanguage): this(
        IntegerFormatter(markerContent, lang),
        widthModifier
    )

    override fun getValue(dateTime: IXsdDateTime): Long? = dateTime.hour?.let { (((it+11u) % 12u)+ 1u).toLong() }
}

private class AmPmMarkerFormatter(val lang: XsdLanguage) : DateTimePartFormatter(null) {
    constructor(markerContent: String, lang: XsdLanguage): this(lang)

    override fun formatTo(dest: Appendable, dateTime: IXsdDateTime) {
        val hour = requireNotNull(dateTime.hour) { "Format requires hour, but not provided" }
        if ((hour % 24u) < 12u) dest.append("AM") else dest.append("PM")
    }
}

private class MinuteInHourFormatter(format: IntegerFormatter, widthModifier: WidthModifier?) :
    NumericFormatter(format, widthModifier) {

    constructor(markerContent: String, lang: XsdLanguage, widthModifier: WidthModifier?): this(
        IntegerFormatter(markerContent, lang),
        widthModifier
    )

    override fun getValue(dateTime: IXsdDateTime): Long? = dateTime.minute?.toLong()
}

private class SecondInMinuteFormatter(format: IntegerFormatter, widthModifier: WidthModifier?) :
    NumericFormatter(format, widthModifier) {
    constructor(markerContent: String, widthModifier: WidthModifier?, lang: XsdLanguage): this(
        IntegerFormatter(markerContent, lang),
        widthModifier
    )

    override fun getValue(dateTime: IXsdDateTime): Long? = dateTime.second?.toLong()
}

private class FractionalSecondsFormatter(format: IntegerFormatter, widthModifier: WidthModifier?) : NumericFormatter(format, widthModifier) {
    constructor(markerContent: String, widthModifier: WidthModifier?, lang: XsdLanguage):
            this(adjustMarker(markerContent.reversed(), widthModifier, lang), widthModifier)

    override fun formatTo(dest: Appendable, dateTime: IXsdDateTime) {
        val seconds = requireNotNull(dateTime.second) { "Format requires second, but not provided" }
        val fractionText = seconds.rem(XsdInt(1)).xmlString.substringAfterLast('.', "0")
        val fractionDigitReversed = XsdInteger(fractionText.reversed())
        val formatedReversed = intFormat.format(fractionDigitReversed)
        val max = intFormat.totalDigitCount

        // TODO: this does not deal with surrogate pairs or markers
        val start = maxOf(formatedReversed.length - max, 0)
        for (i in formatedReversed.length-1 downTo start) {
            dest.append(formatedReversed[i])
        }
    }

    companion object {
        private fun adjustMarker(marker:String, widthModifier: WidthModifier?, lang: XsdLanguage): IntegerFormatter {
            if (widthModifier == null && marker.length == 1) return IntegerFormatter(marker, lang)
            val minDigits = widthModifier?.minWidth ?: 0
            val maxDigits = widthModifier?.maxWidth ?: Int.MAX_VALUE
            val adjustedMarker = StringBuilder()
            var seenDigits: Int = 0
            var seenOptional: Int = 0
            var digitFamily = '0'.code
            for (idx in marker.indices.reversed()) {
                val c = marker.codepointAt(idx)
                when {
                    c == '#'.code -> when {
                        seenDigits + 1 < minDigits -> {
                            adjustedMarker.appendCodepoint(digitFamily)
                            seenDigits += 1
                        }

                        else -> {
                            adjustedMarker.appendCodepoint(c)
                            seenOptional += 1
                        }
                    }

                    c.toChar().isDigit() -> {
                        if (seenDigits == 0) digitFamily = c
                        adjustedMarker.appendCodepoint(c)
                        seenDigits+=1
                    }

                    else -> adjustedMarker.appendCodepoint(c)
                }
                if ((seenDigits + seenOptional)>=maxDigits) break
            }
            return IntegerFormatter(adjustedMarker.toString(), lang)
        }

    }
}

private class TimeZoneFormatter() : DateTimePartFormatter(null) {
    constructor(markerContent: String, lang: XsdLanguage): this()

}

private class TimeZonePrefixedFormatter() : DateTimePartFormatter(null) {
    constructor(markerContent: String, lang: XsdLanguage): this()

}

private class CalendarNameFormatter() : DateTimePartFormatter(null) {
    constructor(markerContent: String, lang: XsdLanguage): this()

}

private class EraFormatter() : DateTimePartFormatter(null) {
    constructor(markerContent: String, lang: XsdLanguage): this()

}

class WidthModifier(val minWidth: Int, val maxWidth: Int = -1) {
    private constructor(l: Long): this(l.shr(32).toInt(), l.toInt())
    constructor(str: String): this(parse(str))

    override fun toString(): String {
        return when {
            maxWidth >= 0 -> "$minWidth-$maxWidth"
            else -> "$minWidth"
        }
    }

    companion object {
        private fun parse(str: String): Long {
            val idx = str.indexOf('-')
            return when {
                idx < 0 -> str.toLong() shl 32
                else -> str.substring(0, idx).toLong().shl(32) or str.substring(idx + 1).toLong()
            }
        }
    }
}

enum class Case {
    UPPER {
        override fun adjust(str: String): String {
            return str.uppercase()
        }
    },

    LOWER {
        override fun adjust(str: String): String {
            return str.lowercase()
        }
    },

    TITLE {
        override fun adjust(str: String): String {
            return str.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    };

    abstract fun adjust(str: String): String
}
