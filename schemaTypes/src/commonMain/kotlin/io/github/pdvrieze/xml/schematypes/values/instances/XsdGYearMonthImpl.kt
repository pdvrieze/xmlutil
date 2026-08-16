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

import io.github.pdvrieze.xml.schematypes.impl.intFromBits
import io.github.pdvrieze.xml.schematypes.impl.toLBits
import io.github.pdvrieze.xml.schematypes.impl.uintFromBits
import io.github.pdvrieze.xml.schematypes.requireRange
import io.github.pdvrieze.xml.schematypes.types.GYearMonthType
import io.github.pdvrieze.xml.schematypes.values.XsdGYearMonth
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import nl.adaptivity.xmlutil.XmlUtilInternal
import kotlin.jvm.JvmInline

@XmlUtilInternal
@JvmInline
value class XsdGYearMonthImpl(val monthYear: ULong) : XsdGYearMonth {

    init {
        requireRange(month in 1u..12u) { "Month values must be between 1 and 12, was $month"}
    }

    constructor(year: Long, month: UInt) : this(
        month.toLBits(4) or
                year.toLBits(52, 4)
    )

    constructor(year: Long, month: UInt, timezoneOffset: Int?) : this(
        month.toLBits(4) or
                year.toLBits(52, 4) or
                when (timezoneOffset) {
                    null -> 0uL
                    else -> (1uL shl 63) or timezoneOffset.toLBits(13, 50)
                }
    )

    override val month: UInt get() = monthYear.uintFromBits(4)
    override val year: Int get() = (monthYear shr 4).intFromBits(46)

    override val timezoneOffset: Int? get() = when {
        monthYear and 0x80000000_00000000uL == 0uL -> null
        else -> (monthYear shr 50).intFromBits(13)
    }


    override fun ensureTimezone(fallbackTimezone: TimeZone): XsdGYearMonth = when {
        monthYear and TZ_MARKER == 0uL -> {
            val newOffset = fallbackTimezone.offsetAt(instant()).totalSeconds / 60
            XsdGYearMonthImpl(monthYear.toLBits(50) or newOffset.toLBits(13, 50) or TZ_MARKER)
        }

        else -> this
    }

    override val xmlString: String get() = "${yearFrag()}-${monthFrag()}${timeZoneFrag()}"
    override val schemaType: GYearMonthType<*> get() = GYearMonthType.Instance

    override fun toString(): String = xmlString

    companion object {
        val TZ_MARKER = 1uL shl 63

        operator fun invoke(str: CharSequence): XsdGYearMonth {
            val yearSplitIdx = str.indexOf('-', 1) // skip leading digit/sign
            requireRange(yearSplitIdx>=0) { "Invalid yearMonth format: $str" }
            val monthEnd = str.indexOfAny(charArrayOf('-', '+', 'Z'), yearSplitIdx + 1)
                .let { if (it < 0) str.length else it }

            val year = str.substring(0, yearSplitIdx).xsToLong()
            val month = str.substring(yearSplitIdx + 1, monthEnd).xsToLong()

            val tz = XsdDateTimeImpl.timezoneFragValue(str.substring(monthEnd))

            requireRange(month in 1..12) { "Month values must be between 1 and 12, was $month"}
            return XsdGYearMonthImpl(year, month.toUInt(), tz)
        }
    }

}
