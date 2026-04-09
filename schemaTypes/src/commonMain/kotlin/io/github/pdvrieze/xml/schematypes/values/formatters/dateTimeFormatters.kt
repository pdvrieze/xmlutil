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
import nl.adaptivity.xmlutil.core.internal.appendCodepoint
import nl.adaptivity.xmlutil.core.internal.codepointAt


class DateTimeFormatter private constructor(
    private val parts: List<DateTimePartFormatter>,
    private val language: XsdLanguage = XsdLanguage("en"),
    private val calendar: String? = null,
    private val place: String? = null
) {

    constructor(picture: String, language: XsdLanguage, calendar: String? = null, place: String? = null) : this(parsePicture(
        picture,
        language,
        calendar
    ), language, calendar, place)

    fun format(dateTime: IXsdDateTime): String {
        return buildString {
            for (part in parts) {
                part.formatTo(this, dateTime)
            }
        }
    }

    companion object {
        private fun parsePicture(picture: String, language: XsdLanguage, calendar: String?): List<DateTimePartFormatter> {
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
                            parts.add(parseMarker(picture.substring(idx + 1, markerEnd), language, calendar))
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

        private fun parseMarker(marker: String, lang: XsdLanguage, calendar: String?): DateTimePartFormatter {
            val widthModIdx = marker.lastIndexOf(',')
            val widthModifier = if (widthModIdx >= 0) WidthModifier(marker.substring(widthModIdx + 1)) else WidthModifier(0)
            val markerContent = when {
                widthModIdx >= 0 -> marker.substring(1, widthModIdx)
                else -> marker.substring(1)
            }.takeIf { it.isNotEmpty() }
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
                'W' -> WeekInYearFormatter(markerContent ?: "1", widthModifier, lang, calendar)
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


private abstract class DateTimePartFormatter protected constructor(val widthModifier: WidthModifier) {

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

private abstract class NumericFormatter(val intFormat: IntegerFormatter, widthModifier: WidthModifier): DateTimePartFormatter(widthModifier) {
    protected open fun getValue(dateTime: IXsdDateTime): Long? = throw UnsupportedOperationException("Not implemented")
    protected open fun getXsdValue(dateTime: IXsdDateTime): XsdInteger? = getValue(dateTime)?.let { XsdLong(it) }

    private fun adjustWithFormatter(widthModifier: WidthModifier, formatter: IntegerFormatter, minDigits: Int): WidthModifier {
        val newMin: Int
        if (widthModifier.maxWidth < Int.MAX_VALUE) {
            return widthModifier
        }
        newMin = widthModifier.minWidth
        val fTotalDigits = formatter.totalDigitCount
        return when {
            fTotalDigits >= minDigits -> WidthModifier(newMin, fTotalDigits)
            else -> WidthModifier(newMin)
        }
    }

    fun formatWithClipping(dest: Appendable, dateTime: IXsdDateTime, minDigits: Int) {
        val adjustedWidthModifier = adjustWithFormatter(widthModifier, intFormat, minDigits)

        appendClipped(dest, dateTime, adjustedWidthModifier)
    }

    private fun appendClipped(
        dest: Appendable,
        dateTime: IXsdDateTime,
        widthModifier: WidthModifier
    ) {
        val elemValue = getXsdValue(dateTime) ?: throw IllegalArgumentException("Format requires a value not provided")
        intFormat.formatTo(dest, elemValue, widthModifier)
    }

    override fun formatTo(dest: Appendable, dateTime: IXsdDateTime) {
        val elemValue = getXsdValue(dateTime) ?: throw IllegalArgumentException("Format requires a value not provided")
        if (widthModifier.minWidth > 0 && intFormat.format is IntegerFormatter.RomanFormatter) {
            val str = intFormat.format(elemValue)
            dest.append(str)
            repeat(maxOf(0, widthModifier.minWidth - str.length)) { dest.append(' ') }
        } else {
            intFormat.formatTo(dest, elemValue, widthModifier)
        }
    }
}

private class TextFormatter(val text: String) : DateTimePartFormatter(WidthModifier(0)) {
    override fun formatTo(dest: Appendable, dateTime: IXsdDateTime) {
        dest.append(text)
    }
}

private class YearFormatter(format: IntegerFormatter, widthModifier: WidthModifier) :
    NumericFormatter(format, widthModifier) {
    constructor(markerContent: String, widthModifier: WidthModifier, lang: XsdLanguage) :
            this(IntegerFormatter(markerContent, lang), widthModifier)

    override fun formatTo(dest: Appendable, dateTime: IXsdDateTime) {
        formatWithClipping(dest, dateTime, 2)
    }

    override fun getValue(dateTime: IXsdDateTime): Long? = dateTime.year?.toLong()
    override fun toString(): String {
        return "Y$intFormat"
    }
}

private class MonthInYearFormatter(format: IntegerFormatter, widthModifier: WidthModifier) :
    NumericFormatter(format, widthModifier) {
    constructor(markerContent: String, lang: XsdLanguage, widthModifier: WidthModifier) : this(
        IntegerFormatter(markerContent, lang),
        widthModifier
    )

    override fun getValue(dateTime: IXsdDateTime): Long? = dateTime.month?.toLong()
}

private class MonthNameInYearFormatter(val case: Case, val lang: XsdLanguage, widthModifier: WidthModifier) :
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

private class DayInMonthFormatter(format: IntegerFormatter, widthModifier: WidthModifier) :
    NumericFormatter(format, widthModifier) {
    constructor(markerContent: String, widthModifier: WidthModifier, lang: XsdLanguage): this(
        IntegerFormatter(markerContent, lang),
        widthModifier
    )

    override fun getValue(dateTime: IXsdDateTime): Long? = dateTime.day?.toLong()
}

private class DayInYearFormatter(format: IntegerFormatter, widthModifier: WidthModifier) :
    NumericFormatter(format, widthModifier) {
    constructor(markerContent: String, widthModifier: WidthModifier, lang: XsdLanguage): this(
        IntegerFormatter(markerContent, lang),
        widthModifier
    )

    override fun getValue(dateTime: IXsdDateTime): Long? {
        return toLocalDate(dateTime)?.run { dayOfYear.toLong() + 1L }
    }
}

private class DayOfWeekFormatter(format: IntegerFormatter, widthModifier: WidthModifier) : NumericFormatter(
    format,
    widthModifier
) {
    constructor(markerContent: String, widthModifier: WidthModifier, lang: XsdLanguage): this(
        IntegerFormatter(markerContent, lang),
        widthModifier
    )

    override fun getValue(dateTime: IXsdDateTime): Long? {
        return toLocalDate(dateTime)?.run { dayOfWeek.ordinal.toLong() + 1 }
    }
}

private class DayNameInWeekAsTextFormatter(val case: Case, val lang: XsdLanguage, widthModifier: WidthModifier) : DateTimePartFormatter(
    widthModifier
) {
    override fun formatTo(dest: Appendable, dateTime: IXsdDateTime) {
        val localDateTime = toLocalDate(dateTime) ?: return
        dest.append(case.adjust(localDateTime.dayOfWeek.name))
    }
}

private class WeekInYearFormatter(format: IntegerFormatter, widthModifier: WidthModifier, val isISO: Boolean) : NumericFormatter(
    format,
    widthModifier
) {
    constructor(markerContent: String, widthModifier: WidthModifier, lang: XsdLanguage, calendar: String?): this(
        IntegerFormatter(markerContent, lang),
        widthModifier,
        calendar == "ISO"
    )

    override fun getValue(dateTime: IXsdDateTime): Long? {
        return dateTime.weekOfYear()?.toLong()
    }
}

private class WeekInMonthFormatter(format: IntegerFormatter, widthModifier: WidthModifier) : NumericFormatter(
    format,
    widthModifier
) {
    constructor(markerContent: String, widthModifier: WidthModifier, lang: XsdLanguage): this(
        IntegerFormatter(markerContent, lang),
        widthModifier
    )

    override fun getValue(dateTime: IXsdDateTime): Long? {
        return dateTime.weekOfMonth()?.toLong()
    }
}

private class Hour24InDayFormatter(format: IntegerFormatter, widthModifier: WidthModifier) :
    NumericFormatter(format, widthModifier) {
    constructor(markerContent: String, widthModifier: WidthModifier, lang: XsdLanguage): this(
        IntegerFormatter(markerContent, lang),
        widthModifier
    )

    override fun getValue(dateTime: IXsdDateTime): Long? = dateTime.hour?.let { (((it +23u) % 24u) + 1u).toLong() }
}

private class Hour12InDayFormatter(format: IntegerFormatter, widthModifier: WidthModifier) : NumericFormatter(
    format,
    widthModifier
) {
    constructor(markerContent: String, widthModifier: WidthModifier, lang: XsdLanguage): this(
        IntegerFormatter(markerContent, lang),
        widthModifier
    )

    override fun getValue(dateTime: IXsdDateTime): Long? = dateTime.hour?.let { (((it+11u) % 12u)+ 1u).toLong() }
}

private class AmPmMarkerFormatter(val lang: XsdLanguage) : DateTimePartFormatter(WidthModifier(0)) {
    constructor(markerContent: String, lang: XsdLanguage): this(lang)

    override fun formatTo(dest: Appendable, dateTime: IXsdDateTime) {
        val hour = requireNotNull(dateTime.hour) { "Format requires hour, but not provided" }
        if ((hour % 24u) < 12u) dest.append("AM") else dest.append("PM")
    }
}

private class MinuteInHourFormatter(format: IntegerFormatter, widthModifier: WidthModifier) :
    NumericFormatter(format, widthModifier) {

    constructor(markerContent: String, lang: XsdLanguage, widthModifier: WidthModifier): this(
        IntegerFormatter(markerContent, lang),
        widthModifier
    )

    override fun getValue(dateTime: IXsdDateTime): Long? = dateTime.minute?.toLong()
}

private class SecondInMinuteFormatter(format: IntegerFormatter, widthModifier: WidthModifier) :
    NumericFormatter(format, widthModifier) {
    constructor(markerContent: String, widthModifier: WidthModifier, lang: XsdLanguage): this(
        IntegerFormatter(markerContent, lang),
        widthModifier
    )

    override fun getValue(dateTime: IXsdDateTime): Long? = dateTime.second?.toLong()
}

private class FractionalSecondsFormatter(format: IntegerFormatter, widthModifier: WidthModifier) : NumericFormatter(format, widthModifier) {
    constructor(markerContent: String, widthModifier: WidthModifier, lang: XsdLanguage):
            this(adjustMarker(markerContent.reversed(), widthModifier, lang), widthModifier)

    override fun formatTo(dest: Appendable, dateTime: IXsdDateTime) {
        val seconds = requireNotNull(dateTime.second) { "Format requires second, but not provided" }
        val fractionText = seconds.rem(XsdInt(1)).xmlString.substringAfterLast('.', "0")
        val fractionDigitReversed = XsdInteger(fractionText.reversed())
        val formatedReversed = intFormat.format(fractionDigitReversed) // do not use length modifier, it breaks things
        val max = if (widthModifier.maxWidth < Int.MAX_VALUE) widthModifier.maxWidth else intFormat.totalDigitCount

        // TODO: this does not deal with surrogate pairs or markers
        val start = maxOf(formatedReversed.length - max, 0)
        for (i in formatedReversed.length-1 downTo start) {
            dest.append(formatedReversed[i])
        }
        repeat(maxOf(0, widthModifier.minWidth - formatedReversed.length)) { dest.appendCodepoint(intFormat.digitFamily) }
    }

    companion object {
        private fun adjustMarker(marker:String, widthModifier: WidthModifier, lang: XsdLanguage): IntegerFormatter {
            if (widthModifier.minWidth <=1 && marker.length == 1) return IntegerFormatter(marker, lang)
            val minDigits = widthModifier.minWidth
            val maxDigits = widthModifier.maxWidth
            val adjustedMarker = StringBuilder()
            var seenDigits = 0
            var seenOptional = 0
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
            return IntegerFormatter(if(adjustedMarker.isEmpty()) "0" else adjustedMarker.toString(), lang)
        }

    }
}

private class TimeZoneFormatter() : DateTimePartFormatter(WidthModifier(0)) {
    constructor(markerContent: String, lang: XsdLanguage): this()

}

private class TimeZonePrefixedFormatter() : DateTimePartFormatter(WidthModifier(0)) {
    constructor(markerContent: String, lang: XsdLanguage): this()

}

private class CalendarNameFormatter() : DateTimePartFormatter(WidthModifier(0)) {
    constructor(markerContent: String, lang: XsdLanguage): this()

}

private class EraFormatter() : DateTimePartFormatter(WidthModifier(0)) {
    constructor(markerContent: String, lang: XsdLanguage): this()

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
