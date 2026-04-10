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
import io.github.pdvrieze.formats.xpath.eval.data.XdmAtomic
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
    val fnDateTime = BuiltinFunctionImpl("dateTime",
        functionType(DateTimeType.Instance.opt, DateType.Instance.opt, TimeType.Instance.opt)
    ) { args ->
        val date = args.atomicArgOrEmpty<XsdDate>(0) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        val time = args.atomicArgOrEmpty<XsdTime>(1) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY

        if (date.timezoneOffset!=null && time.timezoneOffset!=null && date.timezoneOffset!=time.timezoneOffset) {
            throw EvaluationException(ErrorCodes.FORG0008, "Inconsistent timezone offsets: ${date.timezoneOffset} and ${time.timezoneOffset}")
        }

        XdmAtomic(XsdDateTime(date, time))
    }
    //endregion

    //region Extraction functions 9.5
    val fnYearFromDateTime = BuiltinFunctionImpl("year-from-dateTime",
        functionType(INTEGER, DateTimeType.Instance.opt)
    ) { args ->
        val dateTime = args.atomicArgOrEmpty<XsdDateTime>(0) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        XdmAtomic(XsdInteger(dateTime.year))
    }

    val fnMonthFromDateTime = BuiltinFunctionImpl("month-from-dateTime",
        functionType(INTEGER, DateTimeType.Instance.opt)
    ) { args ->
        val dateTime = args.atomicArgOrEmpty<XsdDateTime>(0) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        XdmAtomic(XsdInteger(dateTime.month))
    }

    val fnDayFromDateTime = BuiltinFunctionImpl("day-from-dateTime",
        functionType(INTEGER, DateTimeType.Instance.opt)
    ) { args ->
        val dateTime = args.atomicArgOrEmpty<XsdDateTime>(0) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        XdmAtomic(XsdInteger(dateTime.day))
    }

    val fnHoursFromDateTime = BuiltinFunctionImpl("hours-from-dateTime",
        functionType(INTEGER, DateTimeType.Instance.opt)
    ) { args ->
        val dateTime = args.atomicArgOrEmpty<XsdDateTime>(0) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        XdmAtomic(XsdInteger(dateTime.hour))
    }

    val fnMinutesFromDateTime = BuiltinFunctionImpl("minutes-from-dateTime",
        functionType(INTEGER, DateTimeType.Instance.opt)
    ) { args ->
        val dateTime = args.atomicArgOrEmpty<XsdDateTime>(0) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        XdmAtomic(XsdInteger(dateTime.minute))
    }

    val fnSecondsFromDateTime = BuiltinFunctionImpl("seconds-from-dateTime",
        functionType(INTEGER, DateTimeType.Instance.opt)
    ) { args ->
        val dateTime = args.atomicArgOrEmpty<XsdDateTime>(0) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        XdmAtomic(dateTime.second.roundToInteger())
    }

    val fnTimezoneFromDateTime = BuiltinFunctionImpl(
        "timezone-from-dateTime",
        functionType(DayTimeDurationType.Instance.opt, DateTimeType.Instance.opt)
    ) { args ->
        val dateTime = args.atomicArgOrEmpty<XsdDateTime>(0) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        val offset = dateTime.timezoneOffset ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        XdmAtomic(XsdDayTimeDuration.ofMinutes(offset))
    }

    val fnYearFromDate = BuiltinFunctionImpl("year-from-date",
        functionType(INTEGER, DateType.Instance.opt)
    ) { args ->
        val date = args.atomicArgOrEmpty<XsdDate>(0) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        XdmAtomic(XsdInteger(date.year))
    }

    val fnMonthFromDate = BuiltinFunctionImpl("month-from-date",
        functionType(INTEGER, DateType.Instance.opt)
    ) { args ->
        val date = args.atomicArgOrEmpty<XsdDate>(0) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        XdmAtomic(XsdInteger(date.month))
    }

    val fnDayFromDate = BuiltinFunctionImpl("day-from-date",
        functionType(INTEGER, DateType.Instance.opt)
    ) { args ->
        val date = args.atomicArgOrEmpty<XsdDate>(0) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        XdmAtomic(XsdInteger(date.day))
    }

    val fnTimezoneFromDate = BuiltinFunctionImpl(
        "timezone-from-date",
        functionType(DayTimeDurationType.Instance.opt, TimeType.Instance.opt)
    ) { args ->
        val date = args.atomicArgOrEmpty<XsdDate>(0) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        val offset = date.timezoneOffset ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        XdmAtomic(XsdDayTimeDuration.ofMinutes(offset))
    }


    val fnHoursFromTime = BuiltinFunctionImpl("hours-from-time",
        functionType(INTEGER, TimeType.Instance.opt)
    ) { args ->
        val time = args.atomicArgOrEmpty<XsdTime>(0) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        XdmAtomic(XsdInteger(time.hour))
    }

    val fnMinutesFromTime = BuiltinFunctionImpl("minutes-from-time",
        functionType(INTEGER, TimeType.Instance.opt)
    ) { args ->
        val time = args.atomicArgOrEmpty<XsdTime>(0) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        XdmAtomic(XsdInteger(time.minute))
    }

    val fnSecondsFromTime = BuiltinFunctionImpl("seconds-from-time",
        functionType(INTEGER, TimeType.Instance.opt)
    ) { args ->
        val time = args.atomicArgOrEmpty<XsdTime>(0) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        XdmAtomic(time.second.roundToInteger())
    }


    val fnTimezoneFromTime = BuiltinFunctionImpl(
        "timezone-from-time",
        functionType(DayTimeDurationType.Instance.opt, TimeType.Instance.opt)
    ) { args ->
        val time = args.atomicArgOrEmpty<XsdTime>(0) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        val offset = time.timezoneOffset ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        XdmAtomic(XsdDayTimeDuration.ofMinutes(offset))
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

    val fnAdjustDateTimeToTimezone = BuiltinFunctionImpl("adjust-dateTime-to-timezone", listOf(
        functionType(DateTimeType.Instance.opt, DateTimeType.Instance.opt),
        functionType(DateTimeType.Instance.opt, DateTimeType.Instance.opt, DayTimeDurationType.Instance.opt),
    )) { args ->
        val dateTime = args.atomicArgOrEmpty<XsdDateTime>(0) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        val timezone = when {
            args.size >=2 -> args.atomicArgOrEmpty<XsdDayTimeDuration>(1)?.toValidTimezone()
            else -> contextOf<ExprEvalContext>().defaultTimeZone
        }

        when {
            dateTime.timezoneOffset == null -> when (timezone) {
                null -> args[0]
                else -> XdmAtomic(dateTime.ensureTimezone(timezone))
            }

            timezone == null -> XdmAtomic(XsdDateTime(dateTime.year, dateTime.month, dateTime.day, dateTime.hour, dateTime.minute, dateTime.second))

            else -> XdmAtomic(XsdDateTime(dateTime.instant(), timezone))
        }
    }

    val fnAdjustDateToTimezone = BuiltinFunctionImpl("adjust-date-to-timezone", listOf(
        functionType(DateType.Instance.opt, DateType.Instance.opt),
        functionType(DateType.Instance.opt, DateType.Instance.opt, DayTimeDurationType.Instance.opt),
    )) { args ->
        val date = args.atomicArgOrEmpty<XsdDate>(0) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        val timezone = when {
            args.size >=2 -> args.atomicArgOrEmpty<XsdDayTimeDuration>(1)?.toValidTimezone()
            else -> contextOf<ExprEvalContext>().defaultTimeZone
        }

        when {
            date.timezoneOffset == null -> when (timezone) {
                null -> args[0]
                else -> XdmAtomic(date.ensureTimezone(timezone))
            }

            timezone == null -> XdmAtomic(XsdDate(date.year, date.month, date.day))

            else -> XdmAtomic(XsdDateTime(date.instant(), timezone).toDate())
        }
    }

    val fnAdjustTimeToTimezone = BuiltinFunctionImpl("adjust-time-to-timezone", listOf(
        functionType(TimeType.Instance.opt, TimeType.Instance.opt),
        functionType(TimeType.Instance.opt, TimeType.Instance.opt, DayTimeDurationType.Instance.opt),
    )) { args ->
        val time = args.atomicArgOrEmpty<XsdTime>(0) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        val timezone = when {
            args.size >=2 -> args.atomicArgOrEmpty<XsdDayTimeDuration>(1)?.toValidTimezone()
            else -> contextOf<ExprEvalContext>().defaultTimeZone
        }

        when {
            time.timezoneOffset == null -> when (timezone) {
                null -> args[0]
                else -> XdmAtomic(time.ensureTimezone(timezone))
            }

            timezone == null -> XdmAtomic(XsdTime(time.hour, time.minute, time.second))

            else -> XdmAtomic(XsdDateTime(XsdDateTime(XsdDateTime(XsdDate(1972, 12, 31), time).instant(), null).instant(), timezone))
        }
    }
    //endregion

    //region Date time formatting 9.8
    val fnFormatDateTime = BuiltinFunctionImpl("format-dateTime", listOf(
        functionType(STRING.opt, DateTimeType.Instance.opt, STRING),
        functionType(STRING.opt, DateTimeType.Instance.opt, STRING, STRING.opt, STRING.opt, STRING.opt),
    )) { args ->
        // use IXsdDateTime to allow using this for the date/time versions.
        val dateTime = args.atomicArgOrEmpty<IXsdDateTime>(0) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
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

        val formatted = try {
            formatter.format(dateTime)
        } catch (e: IllegalArgumentException) {
            throw EvaluationException(ErrorCodes.FOFD1350, "Picture and datetime mismatch", e)
        }
        XdmAtomic(XsdString(formatted))
    }
    //endregion
}
