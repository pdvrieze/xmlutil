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
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.core.internal.appendCodepoint
import nl.adaptivity.xmlutil.core.internal.codepointAt
import nl.adaptivity.xmlutil.core.internal.nextCodePointPos
import nl.adaptivity.xmlutil.isXmlWhitespace
import kotlin.math.absoluteValue
import kotlin.math.sign


class DateTimeFormatter private constructor(
    private val parts: List<DateTimePartFormatter>,
    private val language: XsdLanguage = XsdLanguage("en"),
    private val calendar: QName? = null,
    private val place: String? = null
) {

    constructor(picture: String, language: XsdLanguage, calendar: QName? = null, place: String? = null) :
            this(
                parsePicture(
                    picture,
                    language,
                    calendar,
                    place
                ), language, calendar, place
            )

    val hasDateParts: Boolean
        get() {
            return parts.any { it.isDateFormatter }
        }

    val hasTimeParts: Boolean
        get() {
            return parts.any { it.isTimeFormatter }
        }

    fun format(dateTime: IXsdDateTime): String {
        return buildString {
            if (!language.xmlString.let { it.isEmpty() || it.startsWith("en", ignoreCase = true) }) {
                append("Language: en; ")
            }
            when {
                calendar.let {
                    it == null ||
                            (it.getLocalPart() in setOf(null, "ISO", "AD") && it.getNamespaceURI().isEmpty())
                } -> {
                }

                else -> append("Calendar: $calendar; ")
            }

            for (part in parts) {
                part.formatTo(this, dateTime)
            }
        }
    }

    companion object {
        private fun parsePicture(
            picture: String,
            language: XsdLanguage,
            calendar: QName?,
            place: String?
        ): List<DateTimePartFormatter> {
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
                            if (idx > i) {
                                parts.add(TextFormatter(picture.substring(i, idx)))
                            }
                            val markerEnd = picture.indexOf(']', idx + 1)
                            if (markerEnd == -1) throw IllegalArgumentException("Unclosed bracket")
                            parts.add(parseMarker(picture.substring(idx + 1, markerEnd), language, calendar, place))
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

        private fun parseMarker(
            wsMarker: String,
            lang: XsdLanguage,
            calendar: QName?,
            place: String?
        ): DateTimePartFormatter {
            val marker = wsMarker.filterNot { isXmlWhitespace(it) }

            val widthModIdx = marker.lastIndexOf(',')
            val widthModifier =
                if (widthModIdx >= 0) WidthModifier(marker.substring(widthModIdx + 1)) else WidthModifier()
            val markerContent = when {
                widthModIdx >= 0 -> marker.substring(1, widthModIdx)
                marker.isEmpty() -> ""
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
                'P' -> when (markerContent) {
                    "n" -> AmPmMarkerFormatter(Case.LOWER, lang)
                    "Nn" -> AmPmMarkerFormatter(Case.TITLE, lang)
                    "N" -> AmPmMarkerFormatter(Case.UPPER, lang)
                    else -> AmPmMarkerFormatter(Case.LOWER, lang)
                }

                'm' -> MinuteInHourFormatter(markerContent ?: "01", lang, widthModifier)
                's' -> SecondInMinuteFormatter(markerContent ?: "01", widthModifier, lang)
                'f' -> FractionalSecondsFormatter(markerContent ?: "1", widthModifier, lang)
                'Z' -> parseTimezoneMarker(false, markerContent ?: "01:01", widthModifier, lang, place)
                'z' -> parseTimezoneMarker(true, markerContent ?: "01:01", widthModifier, lang, place)
                'C' -> CalendarNameFormatter(markerContent ?: "n", lang)
                'E' -> EraFormatter(markerContent ?: "n", lang)
                else -> throw IllegalArgumentException("Unknown marker $marker")
            }
        }

        private fun parseTimezoneMarker(
            prefixed: Boolean,
            markerContent: String,
            widthModifier: WidthModifier,
            lang: XsdLanguage,
            place: String?
        ): TimeZoneFormatter {
            var variants = if (prefixed) TimeZoneFormatter.VAR_PREFIXED else 0u
            val format: IntegerFormatter
            var firstDigitIdx = -1

            var idx = 0
            do {
                val zeroDigit = markerContent.unicodeChar(idx).zeroDigitOrNull
                if (zeroDigit != null) {
                    firstDigitIdx = idx; break
                }
                idx = markerContent.nextCodePointPos(idx)
            } while (idx < markerContent.length)
            var lastDigitIdx = firstDigitIdx
            while (idx < markerContent.length) {
                if (markerContent.unicodeChar(idx).zeroDigitOrNull != null) {
                    lastDigitIdx = idx
                }
                idx = markerContent.nextCodePointPos(idx)
            }


            var modifierChars: String
            val markerDigits = when {
                firstDigitIdx < 0 || lastDigitIdx < 0 -> {
                    if (markerContent.getOrNull(0).let { it == 'N' || it == 'Z' }) {
                        modifierChars = markerContent.substring(1)
                        markerContent.substring(0, 1)
                    } else {
                        modifierChars = markerContent
                        ""
                    }
                }
                else -> {
                    val posAfterLastDigit = markerContent.nextCodePointPos(lastDigitIdx)
                    modifierChars = markerContent.substring(posAfterLastDigit)
                    markerContent.substring(firstDigitIdx, posAfterLastDigit)
                }
            }

            val modifiers = when (modifierChars) {
                "t" -> IntegerFormatter.CardinalModifier(null, true)
                else -> null
            }

            if (modifiers?.isAlphabetic == true) {
                variants = variants or TimeZoneFormatter.VAR_ZULU
            }

            when {
                // fewer than 3 digits, and no separator should display hours only
                markerDigits.length <= 2 && markerDigits.all { it.isDigit() } -> {
                    format = IntegerFormatter(markerDigits, lang)
                    variants = variants or TimeZoneFormatter.VAR_HOURS_ONLY
                }

                markerDigits == "Z" -> {
                    format = IntegerFormatter("01:01", lang)
                    variants = variants or TimeZoneFormatter.VAR_MILTIME
                }

                markerDigits == "N" -> {
                    format = IntegerFormatter("01:01", lang)
                    variants = variants or TimeZoneFormatter.VAR_NAME
                }

                else -> format = IntegerFormatter(markerDigits, lang)
            }

            val newWidthModifier =
                if (!widthModifier.isSpecified) widthModifier else WidthModifier(widthModifier.minWidth)

            return TimeZoneFormatter(format, newWidthModifier, variants, lang, place)
        }
    }

}


private abstract class DateTimePartFormatter protected constructor(val widthModifier: WidthModifier) {

    open val isTimeFormatter: Boolean get() = false
    open val isDateFormatter: Boolean get() = false

    abstract fun formatTo(dest: Appendable, dateTime: IXsdDateTime)/* {
        TODO("Not yet implemented")
    }*/

    protected fun toLocalDateTime(dateTime: IXsdDateTime, fallbackTimezone: TimeZone = TimeZone.UTC): LocalDateTime? =
        when (dateTime) {
            is XsdDate -> XsdDateTime(dateTime, XsdTime(1u, 1u, 0uL)).toLocalDateTime(fallbackTimezone)
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

private abstract class NumericFormatter(val intFormat: IntegerFormatter, widthModifier: WidthModifier) :
    DateTimePartFormatter(widthModifier) {
    protected open fun getValue(dateTime: IXsdDateTime): Long? = throw UnsupportedOperationException("Not implemented")
    protected open fun getXsdValue(dateTime: IXsdDateTime): XsdInteger? = getValue(dateTime)?.let { XsdLong(it) }

    private fun adjustWithFormatter(
        widthModifier: WidthModifier,
        formatter: IntegerFormatter,
        noImplicitClippingBelow: Int
    ): WidthModifier {
        if (widthModifier.isMaxSpecified) return widthModifier

        val fTotalDigits = formatter.totalDigitCount
        return when {
            fTotalDigits >= noImplicitClippingBelow && fTotalDigits >= widthModifier.minWidth -> WidthModifier(widthModifier.minWidth, fTotalDigits)
            else -> WidthModifier(widthModifier.minWidth)
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
        if (widthModifier.isSpecified && intFormat.format is IntegerFormatter.RomanFormatter) {
            val str = intFormat.format(elemValue)
            dest.append(str)
            repeat(maxOf(0, widthModifier.minWidth - str.length)) { dest.append(' ') }
        } else {
            intFormat.formatTo(dest, elemValue, widthModifier)
        }
    }
}

private class TextFormatter(val text: String) : DateTimePartFormatter(WidthModifier()) {
    override fun formatTo(dest: Appendable, dateTime: IXsdDateTime) {
        dest.append(text)
    }
}

private class YearFormatter(format: IntegerFormatter, widthModifier: WidthModifier) :
    NumericFormatter(format, widthModifier) {
    constructor(markerContent: String, widthModifier: WidthModifier, lang: XsdLanguage) :
            this(IntegerFormatter("$markerContent;", lang), widthModifier)

    override val isDateFormatter: Boolean get() = true

    override fun formatTo(dest: Appendable, dateTime: IXsdDateTime) {
        formatWithClipping(dest, dateTime, 2)
    }

    override fun getValue(dateTime: IXsdDateTime): Long? = dateTime.year?.toLong()?.absoluteValue
    override fun toString(): String {
        return "Y$intFormat"
    }
}

private class MonthInYearFormatter(format: IntegerFormatter, widthModifier: WidthModifier) :
    NumericFormatter(format, widthModifier) {
    constructor(markerContent: String, lang: XsdLanguage, widthModifier: WidthModifier) : this(
        IntegerFormatter("$markerContent;", lang),
        widthModifier
    )

    override val isDateFormatter: Boolean get() = true

    override fun getValue(dateTime: IXsdDateTime): Long? = dateTime.month?.toLong()
}

private class MonthNameInYearFormatter(val case: Case, val lang: XsdLanguage, widthModifier: WidthModifier) :
    DateTimePartFormatter(widthModifier) {

    override val isDateFormatter: Boolean get() = true

    override fun formatTo(dest: Appendable, dateTime: IXsdDateTime) {
        val month = requireNotNull(dateTime.month).toInt() - 1
        val monthStr = widthModifier.adjustStr(months[month], ' ')

        when (case) {
            Case.UPPER -> dest.append(monthStr.uppercase())
            Case.LOWER -> dest.append(monthStr.lowercase())
            Case.TITLE -> dest.append(monthStr)
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
    constructor(markerContent: String, widthModifier: WidthModifier, lang: XsdLanguage) : this(
        IntegerFormatter("$markerContent;", lang),
        widthModifier
    )

    override val isDateFormatter: Boolean get() = true

    override fun getValue(dateTime: IXsdDateTime): Long? = dateTime.day?.toLong()
}

private class DayInYearFormatter(format: IntegerFormatter, widthModifier: WidthModifier) :
    NumericFormatter(format, widthModifier) {
    constructor(markerContent: String, widthModifier: WidthModifier, lang: XsdLanguage) : this(
        IntegerFormatter("$markerContent;", lang),
        widthModifier
    )

    override val isDateFormatter: Boolean get() = true

    override fun getValue(dateTime: IXsdDateTime): Long? {
        return toLocalDate(dateTime)?.run { dayOfYear.toLong() }
    }
}

private class DayOfWeekFormatter(format: IntegerFormatter, widthModifier: WidthModifier) : NumericFormatter(
    format,
    widthModifier
) {
    constructor(markerContent: String, widthModifier: WidthModifier, lang: XsdLanguage) : this(
        IntegerFormatter("$markerContent;", lang),
        widthModifier
    )

    override val isDateFormatter: Boolean get() = true

    override fun getValue(dateTime: IXsdDateTime): Long? {
        return toLocalDate(dateTime)?.run { dayOfWeek.ordinal.toLong() + 1 }
    }

    override fun toString(): String {
        return "F$intFormat$widthModifier"
    }
}

private class DayNameInWeekAsTextFormatter(val case: Case, val lang: XsdLanguage, widthModifier: WidthModifier) :
    DateTimePartFormatter(widthModifier) {
    override val isDateFormatter: Boolean get() = true

    override fun formatTo(dest: Appendable, dateTime: IXsdDateTime) {
        val localDateTime = toLocalDate(dateTime) ?: return
        val dayName = localDateTime.dayOfWeek.name
        val widthAdj = widthModifier.adjLength(dayName.length)

        when {
            widthAdj == 0 -> dest.append(case.adjust(dayName))

            widthAdj > 0 -> {
                dest.append(case.adjust(dayName))
                repeat(widthAdj) { dest.append(' ') }
            }

            else -> {
                val noSuffix = ABBREV_NAME[localDateTime.dayOfWeek.ordinal]
                val nsAdj = widthModifier.adjLength(noSuffix.length)
                when {
                    nsAdj == 0 -> dest.append(case.adjust(noSuffix))
                    else -> dest.append(case.adjust(dayName.substring(0, widthModifier.maxWidth)))
                }
            }
        }
    }

    companion object {
        val ABBREV_NAME = arrayOf(
            "MON",
            "TUES",
            "WEDS",
            "THUR",
            "FRI",
            "SAT",
            "SUN",
        )

    }

}

private class WeekInYearFormatter(format: IntegerFormatter, widthModifier: WidthModifier, val isISO: Boolean) :
    NumericFormatter(
        format,
        widthModifier
    ) {
    constructor(markerContent: String, widthModifier: WidthModifier, lang: XsdLanguage, calendar: QName?) : this(
        IntegerFormatter("$markerContent;", lang),
        widthModifier,
        calendar.isEquivalent(QName("", "ISO"))
    )

    override val isDateFormatter: Boolean get() = true

    override fun getValue(dateTime: IXsdDateTime): Long? {
        return dateTime.weekOfYear()?.toLong()
    }
}

private class WeekInMonthFormatter(format: IntegerFormatter, widthModifier: WidthModifier) : NumericFormatter(
    format,
    widthModifier
) {
    constructor(markerContent: String, widthModifier: WidthModifier, lang: XsdLanguage) : this(
        IntegerFormatter("$markerContent;", lang),
        widthModifier
    )

    override val isDateFormatter: Boolean get() = true

    override fun getValue(dateTime: IXsdDateTime): Long? {
        return dateTime.weekOfMonth()?.toLong()
    }
}

private class Hour24InDayFormatter(format: IntegerFormatter, widthModifier: WidthModifier) :
    NumericFormatter(format, widthModifier) {
    constructor(markerContent: String, widthModifier: WidthModifier, lang: XsdLanguage) : this(
        IntegerFormatter("$markerContent;", lang),
        widthModifier
    )

    override val isTimeFormatter: Boolean get() = true

    override fun getValue(dateTime: IXsdDateTime): Long? = dateTime.hour?.let { (((it + 23u) % 24u) + 1u).toLong() }
}

private class Hour12InDayFormatter(format: IntegerFormatter, widthModifier: WidthModifier) : NumericFormatter(
    format,
    widthModifier
) {
    constructor(markerContent: String, widthModifier: WidthModifier, lang: XsdLanguage) : this(
        IntegerFormatter("$markerContent;", lang),
        widthModifier
    )

    override val isTimeFormatter: Boolean get() = true

    override fun getValue(dateTime: IXsdDateTime): Long? = dateTime.hour?.let { (((it + 11u) % 12u) + 1u).toLong() }
}

private class AmPmMarkerFormatter(val case: Case, val lang: XsdLanguage) : DateTimePartFormatter(WidthModifier()) {

    override val isTimeFormatter: Boolean get() = true

    override fun formatTo(dest: Appendable, dateTime: IXsdDateTime) {
        val hour = requireNotNull(dateTime.hour) { "Format requires hour, but not provided" }
        val marker = when (case) {
            Case.UPPER -> if ((hour % 24u) < 12u) "AM" else "PM"
            Case.LOWER -> if ((hour % 24u) < 12u) "am" else "pm"
            Case.TITLE -> if ((hour % 24u) < 12u) "Am" else "Pm"
        }
        dest.append(marker)
    }
}

private class MinuteInHourFormatter(format: IntegerFormatter, widthModifier: WidthModifier) :
    NumericFormatter(format, widthModifier) {

    constructor(markerContent: String, lang: XsdLanguage, widthModifier: WidthModifier) : this(
        IntegerFormatter("$markerContent;", lang),
        widthModifier
    )

    override val isTimeFormatter: Boolean get() = true

    override fun getValue(dateTime: IXsdDateTime): Long? = dateTime.minute?.toLong()
}

private class SecondInMinuteFormatter(format: IntegerFormatter, widthModifier: WidthModifier) :
    NumericFormatter(format, widthModifier) {
    constructor(markerContent: String, widthModifier: WidthModifier, lang: XsdLanguage) : this(
        IntegerFormatter("$markerContent;", lang),
        widthModifier
    )

    override fun getValue(dateTime: IXsdDateTime): Long? = dateTime.second?.toLong()
}

private class FractionalSecondsFormatter(format: IntegerFormatter, widthModifier: WidthModifier) :
    NumericFormatter(format, widthModifier) {
    constructor(markerContent: String, widthModifier: WidthModifier, lang: XsdLanguage) :
            this(adjustMarker(markerContent.reversed(), widthModifier, lang), widthModifier)

    override val isTimeFormatter: Boolean get() = true

    override fun formatTo(dest: Appendable, dateTime: IXsdDateTime) {
        val seconds = requireNotNull(dateTime.second) { "Format requires second, but not provided" }
        val fractionText = seconds.rem(XsdInt(1)).xmlString.substringAfterLast('.', "0")
        val fractionDigitReversed = XsdInteger(fractionText.reversed())
        val formatedReversed = intFormat.format(fractionDigitReversed) // do not use length modifier, it breaks things
        val max = if (widthModifier.isSpecified) widthModifier.maxWidth else intFormat.totalDigitCount

        // TODO: this does not deal with surrogate pairs or markers
        val start = maxOf(formatedReversed.length - max, 0)
        for (i in formatedReversed.length - 1 downTo start) {
            dest.append(formatedReversed[i])
        }
        repeat(maxOf(0, widthModifier.minWidth - formatedReversed.length)) { dest.appendUnicode(intFormat.digitFamily) }
    }

    companion object {
        private fun adjustMarker(marker: String, widthModifier: WidthModifier, lang: XsdLanguage): IntegerFormatter {
            if ((!widthModifier.isSpecified || widthModifier.minWidth <= 1) && marker.length == 1) {
                return IntegerFormatter(if (marker.endsWith(';')) marker else "$marker;", lang)
            }
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
                        seenDigits += 1
                    }

                    else -> adjustedMarker.appendCodepoint(c)
                }
                if ((seenDigits + seenOptional) >= maxDigits) break
            }
            if (adjustedMarker.isEmpty()) adjustedMarker.append('0')
            adjustedMarker.append(';')
            return IntegerFormatter(adjustedMarker.toString(), lang)
        }

    }
}

private class TimeZoneFormatter(
    private val format: IntegerFormatter,
    widthModifier: WidthModifier,
    private val variants: UInt,
    private val lang: XsdLanguage,
    private val place: String?
) : DateTimePartFormatter(widthModifier) {

    override fun formatTo(dest: Appendable, dateTime: IXsdDateTime) {
        val offset = dateTime.timezoneOffset ?: run {
            if (variants and VAR_MILTIME != 0u) dest.append('J')
            return
        }

        if (variants and VAR_PREFIXED != 0u) dest.append("GMT")

        val abs = offset.absoluteValue
        val hours = abs / 60
        val minutes = abs % 60
        when (offset) {
            0 if (variants and (VAR_ZULU or VAR_MILTIME) != 0u) -> dest.append('Z')

            else if minutes == 0 && (variants and VAR_MILTIME != 0u && hours <= 12) -> {
                dest.append(MILTIME_HOURS[(offset.sign * hours) + 12])
            }

            else -> {
                dest.append(if (offset >= 0) '+' else '-')
                when {
                    variants and VAR_HOURS_ONLY != 0u -> {
                        if (hours < 10 && widthModifier.minWidth > 1) dest.append('0')
                        format.formatTo(dest, hours)
                        if (minutes != 0) {
                            dest.append(':')
                            if (minutes < 10) dest.append('0')
                            dest.append(minutes.toString())
                        }
                    }

                    else -> {
                        val combined = hours * 100 + minutes
                        format.formatTo(dest, combined, widthModifier)
                    }

                }
            }
        }
    }

    companion object {
        // just a simple lookup starting at -12
        private val MILTIME_HOURS = arrayOf(
            'Y', 'X', 'W', 'V', 'U', 'T', 'S', 'R', 'Q', 'P', 'O',
            'N', 'Z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'K', 'L', 'M'
        )

        val VAR_HOURS_ONLY: UInt = 1u shl 0
        val VAR_ZULU: UInt = 1u shl 1
        val VAR_MILTIME: UInt = 1u shl 2
        val VAR_NAME: UInt = 1u shl 3
        val VAR_PREFIXED: UInt = 1u shl 4
    }
}


private class CalendarNameFormatter() : DateTimePartFormatter(WidthModifier()) {
    constructor(markerContent: String, lang: XsdLanguage) : this()

    override fun formatTo(
        dest: Appendable,
        dateTime: IXsdDateTime
    ) {
        TODO("not implemented")
    }
}

private class EraFormatter() : DateTimePartFormatter(WidthModifier()) {
    constructor(markerContent: String, lang: XsdLanguage) : this()

    override fun formatTo(
        dest: Appendable,
        dateTime: IXsdDateTime
    ) {
        val y = requireNotNull(dateTime.year) { "Era formatter requires year, but not provided" }
        when {
            y >= 0 -> dest.append("A.D.")
            else -> dest.append("B.C.")
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
            return buildString(str.length) {
                append(str[0].uppercase())
                append(str.substring(1).lowercase())
            }
        }
    };

    abstract fun adjust(str: String): String
}
