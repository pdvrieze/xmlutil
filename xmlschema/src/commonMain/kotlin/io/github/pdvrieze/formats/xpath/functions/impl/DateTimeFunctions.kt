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

package io.github.pdvrieze.formats.xpath.functions.impl

import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.data.XdmSequence
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunctionImpl
import io.github.pdvrieze.formats.xpath.functions.atomicArgN
import io.github.pdvrieze.formats.xpath.functions.atomicArgOrEmpty
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.DateTimeType
import io.github.pdvrieze.xml.schematypes.types.DateType
import io.github.pdvrieze.xml.schematypes.types.DayTimeDurationType
import io.github.pdvrieze.xml.schematypes.types.TimeType
import io.github.pdvrieze.xml.schematypes.values.*
import io.github.pdvrieze.xml.schematypes.values.formatters.DateTimeFormatter
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlin.math.roundToInt

@XPathInternal
object DateTimeFunctions : AbstractFunctionObject() {

    //region Constructing dateTime 9.3
    val fnDateTime = BuiltinFunctionImpl.Fn("dateTime",
        functionType(DateTimeType.Instance.opt, DateType.Instance.opt, TimeType.Instance.opt)
    ) Fn@{ args ->
        val date = args.atomicArgOrEmpty<XsdDate>(0) ?: return@Fn XdmSequence.EMPTY
        val time = args.atomicArgOrEmpty<XsdTime>(1) ?: return@Fn XdmSequence.EMPTY

        if (date.timezoneOffset!=null && time.timezoneOffset!=null && date.timezoneOffset!=time.timezoneOffset) {
            throw EvaluationException(ErrorCodes.FORG0008, "Inconsistent timezone offsets: ${date.timezoneOffset} and ${time.timezoneOffset}")
        }

        atomic(XsdDateTime(date, time))
    }
    //endregion

    //region Extraction functions 9.5
    val fnYearFromDateTime = BuiltinFunctionImpl.Fn("year-from-dateTime",
        functionType(INTEGER, DateTimeType.Instance.opt)
    ) Fn@{ args ->
        val dateTime = args.atomicArgOrEmpty<XsdDateTime>(0) ?: return@Fn XdmSequence.EMPTY
        atomic(dateTime.year)
    }

    val fnMonthFromDateTime = BuiltinFunctionImpl.Fn("month-from-dateTime",
        functionType(INTEGER, DateTimeType.Instance.opt)
    ) Fn@{ args ->
        val dateTime = args.atomicArgOrEmpty<XsdDateTime>(0) ?: return@Fn XdmSequence.EMPTY
        atomic(dateTime.month)
    }

    val fnDayFromDateTime = BuiltinFunctionImpl.Fn("day-from-dateTime",
        functionType(INTEGER, DateTimeType.Instance.opt)
    ) Fn@{ args ->
        val dateTime = args.atomicArgOrEmpty<XsdDateTime>(0) ?: return@Fn XdmSequence.EMPTY
        atomic(dateTime.day)
    }

    val fnHoursFromDateTime = BuiltinFunctionImpl.Fn("hours-from-dateTime",
        functionType(INTEGER, DateTimeType.Instance.opt)
    ) Fn@{ args ->
        val dateTime = args.atomicArgOrEmpty<XsdDateTime>(0) ?: return@Fn XdmSequence.EMPTY
        atomic(dateTime.hour)
    }

    val fnMinutesFromDateTime = BuiltinFunctionImpl.Fn("minutes-from-dateTime",
        functionType(INTEGER, DateTimeType.Instance.opt)
    ) Fn@{ args ->
        val dateTime = args.atomicArgOrEmpty<XsdDateTime>(0) ?: return@Fn XdmSequence.EMPTY
        atomic(dateTime.minute)
    }

    val fnSecondsFromDateTime = BuiltinFunctionImpl.Fn("seconds-from-dateTime",
        functionType(INTEGER, DateTimeType.Instance.opt)
    ) Fn@{ args ->
        val dateTime = args.atomicArgOrEmpty<XsdDateTime>(0) ?: return@Fn XdmSequence.EMPTY
        atomic(dateTime.second.roundToInteger())
    }

    val fnTimezoneFromDateTime = BuiltinFunctionImpl.Fn(
        "timezone-from-dateTime",
        functionType(DayTimeDurationType.Instance.opt, DateTimeType.Instance.opt)
    ) Fn@{ args ->
        val dateTime = args.atomicArgOrEmpty<XsdDateTime>(0) ?: return@Fn XdmSequence.EMPTY
        val offset = dateTime.timezoneOffset ?: return@Fn XdmSequence.EMPTY
        atomic(XsdDayTimeDuration.ofMinutes(offset))
    }

    val fnYearFromDate = BuiltinFunctionImpl.Fn("year-from-date",
        functionType(INTEGER, DateType.Instance.opt)
    ) Fn@{ args ->
        val date = args.atomicArgOrEmpty<XsdDate>(0) ?: return@Fn XdmSequence.EMPTY
        atomic(date.year)
    }

    val fnMonthFromDate = BuiltinFunctionImpl.Fn("month-from-date",
        functionType(INTEGER, DateType.Instance.opt)
    ) Fn@{ args ->
        val date = args.atomicArgOrEmpty<XsdDate>(0) ?: return@Fn XdmSequence.EMPTY
        atomic(date.month)
    }

    val fnDayFromDate = BuiltinFunctionImpl.Fn("day-from-date",
        functionType(INTEGER, DateType.Instance.opt)
    ) Fn@{ args ->
        val date = args.atomicArgOrEmpty<XsdDate>(0) ?: return@Fn XdmSequence.EMPTY
        atomic(date.day)
    }

    val fnTimezoneFromDate = BuiltinFunctionImpl.Fn(
        "timezone-from-date",
        functionType(DayTimeDurationType.Instance.opt, DateType.Instance.opt)
    ) Fn@{ args ->
        val date = args.atomicArgOrEmpty<XsdDate>(0) ?: return@Fn XdmSequence.EMPTY
        val offset = date.timezoneOffset ?: return@Fn XdmSequence.EMPTY
        atomic(XsdDayTimeDuration.ofMinutes(offset))
    }


    val fnHoursFromTime = BuiltinFunctionImpl.Fn("hours-from-time",
        functionType(INTEGER, TimeType.Instance.opt)
    ) Fn@{ args ->
        val time = args.atomicArgOrEmpty<XsdTime>(0) ?: return@Fn XdmSequence.EMPTY
        atomic(time.hour)
    }

    val fnMinutesFromTime = BuiltinFunctionImpl.Fn("minutes-from-time",
        functionType(INTEGER, TimeType.Instance.opt)
    ) Fn@{ args ->
        val time = args.atomicArgOrEmpty<XsdTime>(0) ?: return@Fn XdmSequence.EMPTY
        atomic(time.minute)
    }

    val fnSecondsFromTime = BuiltinFunctionImpl.Fn("seconds-from-time",
        functionType(INTEGER, TimeType.Instance.opt)
    ) Fn@{ args ->
        val time = args.atomicArgOrEmpty<XsdTime>(0) ?: return@Fn XdmSequence.EMPTY
        atomic(time.second.roundToInteger())
    }


    val fnTimezoneFromTime = BuiltinFunctionImpl.Fn(
        "timezone-from-time",
        functionType(DayTimeDurationType.Instance.opt, TimeType.Instance.opt)
    ) Fn@{ args ->
        val time = args.atomicArgOrEmpty<XsdTime>(0) ?: return@Fn XdmSequence.EMPTY
        val offset = time.timezoneOffset ?: return@Fn XdmSequence.EMPTY
        atomic(XsdDayTimeDuration.ofMinutes(offset))
    }

    //endregion

    //region timezone adjustment functions 9.6
    context(ctx: ExprEvalContext)
    private fun XsdDayTimeDuration.toValidTimezone(): TimeZone {
        val intMinutes = (seconds / 60).roundToInt()
        when {
            intMinutes*60.0 != seconds -> throw EvaluationException(ErrorCodes.FODT0003, "Timezone adjustments must be in whole minutes")
            intMinutes in -14 * 60..14 * 60 -> return UtcOffset(minutes = intMinutes).asTimeZone()
            else -> throw EvaluationException(ErrorCodes.FODT0003, "Timezone offset out of range: $this !in -14:00..14:00")
        }
    }

    val fnAdjustDateTimeToTimezone = BuiltinFunctionImpl.Fn("adjust-dateTime-to-timezone", listOf(
        functionType(DateTimeType.Instance.opt, DateTimeType.Instance.opt),
        functionType(DateTimeType.Instance.opt, DateTimeType.Instance.opt, DayTimeDurationType.Instance.opt),
    )) Fn@{ args ->
        val dateTime = args.atomicArgOrEmpty<XsdDateTime>(0) ?: return@Fn XdmSequence.EMPTY
        val timezone = when {
            args.size >=2 -> args.atomicArgOrEmpty<XsdDayTimeDuration>(1)?.toValidTimezone()
            else -> contextOf<ExprEvalContext>().defaultTimeZone
        }

        when {
            dateTime.timezoneOffset == null -> when (timezone) {
                null -> args[0]
                else -> atomic(dateTime.ensureTimezone(timezone))
            }

            timezone == null -> atomic(XsdDateTime(dateTime.year, dateTime.month, dateTime.day, dateTime.hour, dateTime.minute, dateTime.second))

            else -> atomic(XsdDateTime(dateTime.instant(), timezone))
        }
    }

    val fnAdjustDateToTimezone = BuiltinFunctionImpl.Fn("adjust-date-to-timezone", listOf(
        functionType(DateType.Instance.opt, DateType.Instance.opt),
        functionType(DateType.Instance.opt, DateType.Instance.opt, DayTimeDurationType.Instance.opt),
    )) Fn@{ args ->
        val date = args.atomicArgOrEmpty<XsdDate>(0) ?: return@Fn XdmSequence.EMPTY
        val timezone = when {
            args.size >=2 -> args.atomicArgOrEmpty<XsdDayTimeDuration>(1)?.toValidTimezone()
            else -> contextOf<ExprEvalContext>().defaultTimeZone
        }

        when {
            date.timezoneOffset == null -> when (timezone) {
                null -> args[0]
                else -> atomic(date.ensureTimezone(timezone))
            }

            timezone == null -> atomic(XsdDate(date.year, date.month, date.day))

            else -> atomic(XsdDateTime(date.instant(), timezone).toDate())
        }
    }

    val fnAdjustTimeToTimezone = BuiltinFunctionImpl.Fn("adjust-time-to-timezone", listOf(
        functionType(TimeType.Instance.opt, TimeType.Instance.opt),
        functionType(TimeType.Instance.opt, TimeType.Instance.opt, DayTimeDurationType.Instance.opt),
    )) Fn@{ args ->
        val time = args.atomicArgOrEmpty<XsdTime>(0) ?: return@Fn XdmSequence.EMPTY
        val timezone = when {
            args.size >=2 -> args.atomicArgOrEmpty<XsdDayTimeDuration>(1)?.toValidTimezone()
            else -> contextOf<ExprEvalContext>().defaultTimeZone
        }

        when {
            time.timezoneOffset == null -> when (timezone) {
                null -> args[0]
                else -> atomic(time.ensureTimezone(timezone))
            }

            timezone == null -> atomic(XsdTime(time.hour, time.minute, time.second))

            else -> {
                val timeWithDate = XsdDateTime(XsdDate(1972, 12, 31), time)
                val dateTimeWithNewTz = XsdDateTime(timeWithDate.instant(), timezone)
                atomic(dateTimeWithNewTz.toTime())
            }
        }
    }
    //endregion

    //region Date time formatting 9.8
    val fnFormatDateTime = BuiltinFunctionImpl.Fn("format-dateTime", listOf(
        functionType(STRING.opt, DateTimeType.Instance.opt, STRING),
        functionType(STRING.opt, DateTimeType.Instance.opt, STRING, STRING.opt, STRING.opt, STRING.opt),
    )) Fn@{ args ->
        // use IXsdDateTime to allow using this for the date/time versions.
        val dateTime = args.atomicArgOrEmpty<XsdDateTime>(0) ?: return@Fn XdmSequence.EMPTY
        val picture = args.atomicArgN<XsdString>(1).xmlString
        val language: XsdLanguage?
        val calendar: String?
        val place: String?
        if (args.size==5) {
            language = args.atomicArgOrEmpty<XsdString>(2)?.let { XsdLanguage(it.xmlString) }
            calendar = args.atomicArgOrEmpty<XsdString>(3)?.xmlString
            place = args.atomicArgOrEmpty<XsdString>(4)?.xmlString
        } else {
            language = null
            calendar = null
            place = null
        }
        val formatter = try {
            DateTimeFormatter(picture, language ?: contextOf<ExprEvalContext>().defaultLanguage, calendar, place)
        } catch (e: IllegalArgumentException) {
            throw EvaluationException(ErrorCodes.FOFD1340, "Invalid picture for format-dateTime: '$picture'", e)
        }

        try {
            atomic(formatter.format(dateTime))
        } catch (e: IllegalArgumentException) {
            throw EvaluationException(ErrorCodes.FOFD1350, "Picture and datetime mismatch", e)
        }
    }

    val fnFormatDate = BuiltinFunctionImpl.Fn("format-date", listOf(
        functionType(STRING.opt, DateType.Instance.opt, STRING),
        functionType(STRING.opt, DateType.Instance.opt, STRING, STRING.opt, STRING.opt, STRING.opt),
    )) Fn@{ args ->
        // use IXsdDateTime to allow using this for the date/time versions.
        val dateTime = args.atomicArgOrEmpty<XsdDate>(0) ?: return@Fn XdmSequence.EMPTY
        val picture = args.atomicArgN<XsdString>(1).xmlString
        val language: XsdLanguage?
        val calendar: String?
        val place: String?
        if (args.size==5) {
            language = args.atomicArgOrEmpty<XsdString>(2)?.let { XsdLanguage(it.xmlString) }
            calendar = args.atomicArgOrEmpty<XsdString>(3)?.xmlString
            place = args.atomicArgOrEmpty<XsdString>(4)?.xmlString
        } else {
            language = null
            calendar = null
            place = null
        }
        val formatter = try {
            DateTimeFormatter(picture, language ?: contextOf<ExprEvalContext>().defaultLanguage, calendar, place)
        } catch (e: IllegalArgumentException) {
            throw EvaluationException(ErrorCodes.FOFD1340, "Invalid picture for format-date: '$picture'", e)
        }

        if (formatter.hasTimeParts) throw EvaluationException(ErrorCodes.FOFD1350, "Picture for format-date must not contain time parts")

        try {
            atomic(formatter.format(dateTime))
        } catch (e: IllegalArgumentException) {
            throw EvaluationException(ErrorCodes.FOFD1350, "Picture and datetime mismatch", e)
        }
    }

    val fnFormatTime = BuiltinFunctionImpl.Fn("format-time", listOf(
        functionType(STRING.opt, TimeType.Instance.opt, STRING),
        functionType(STRING.opt, TimeType.Instance.opt, STRING, STRING.opt, STRING.opt, STRING.opt),
    )) Fn@{ args ->
        // use IXsdDateTime to allow using this for the date/time versions.
        val dateTime = args.atomicArgOrEmpty<XsdTime>(0) ?: return@Fn XdmSequence.EMPTY
        val picture = args.atomicArgN<XsdString>(1).xmlString
        val language: XsdLanguage?
        val calendar: String?
        val place: String?
        if (args.size==5) {
            language = args.atomicArgOrEmpty<XsdString>(2)?.let { XsdLanguage(it.xmlString) }
            calendar = args.atomicArgOrEmpty<XsdString>(3)?.xmlString
            place = args.atomicArgOrEmpty<XsdString>(4)?.xmlString
        } else {
            language = null
            calendar = null
            place = null
        }
        val formatter = try {
            DateTimeFormatter(picture, language ?: contextOf<ExprEvalContext>().defaultLanguage, calendar, place)
        } catch (e: IllegalArgumentException) {
            throw EvaluationException(ErrorCodes.FOFD1340, "Invalid picture for format-date: '$picture'", e)
        }

        if (formatter.hasDateParts) throw EvaluationException(ErrorCodes.FOFD1350, "Picture for format-date must not contain time parts")

        try {
            atomic(formatter.format(dateTime))
        } catch (e: IllegalArgumentException) {
            throw EvaluationException(ErrorCodes.FOFD1350, "Picture and datetime mismatch", e)
        }
    }


    //endregion
}
