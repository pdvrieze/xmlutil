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
import io.github.pdvrieze.xml.schematypes.impl.uLongFromBits
import io.github.pdvrieze.xml.schematypes.impl.uintFromBits
import io.github.pdvrieze.xml.schematypes.requireRange
import io.github.pdvrieze.xml.schematypes.types.DateType
import io.github.pdvrieze.xml.schematypes.values.XsdDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.xmlCollapseWhitespace

@XmlUtilInternal
class XsdDateImpl(private val dateVal: ULong) : XsdDate {
    constructor(year: Long, month: Int, day: Int) : this(year, month.toUInt(), day.toUInt())

    constructor(year: Long, month: UInt, day: UInt, overloadMarker: Unit = Unit) : this(
        day.toLBits(5) or
                month.toLBits(4, 5) or
                year.toLBits(41, 9)
    ) {
        requireRange(month in 1uL..12uL) { "Month out of range: $month" }
        requireRange(day in 1uL..31uL) { "Day out of range: $day" }
    }

    constructor(year: Long, month: Int, day: Int, timezoneOffset: Int?) :
            this(year, month.toUInt(), day.toUInt(), timezoneOffset)

    constructor(year: Long, month: UInt, day: UInt, timezoneOffset: Int?, overloadMarker: Unit = Unit) : this(
        day.toLBits(5) or
                month.toLBits(4, 5) or
                year.toLBits(41, 9) or
                when (timezoneOffset) {
                    null -> 0uL
                    else -> TZ_BIT or timezoneOffset.toLBits(13, 50)
                }
    ) {
        requireRange(month in 1uL..12uL) { "Month out of range: $month" }
        requireRange(day in 1uL..31uL) { "Day out of range: $day" }
        requireRange(timezoneOffset == null || timezoneOffset in -14 * 60..14 * 60) { "Timezone offset out of range: $timezoneOffset" }
    }

    override val day: UInt get() = dateVal.uintFromBits(5)

    override val month: UInt get() = (dateVal shr 5).uintFromBits(4)

    override val year: Int get() = (dateVal shr 9).intFromBits(41)

    override val timezoneOffset: Int? get() = when {
        dateVal and TZ_BIT == 0uL -> null
        else -> (dateVal shr 50).intFromBits(13)
    }

    override val xmlString: String
        get() = "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${
            day.toString().padStart(2, '0')
        }${timeZoneFrag()}"

    override val schemaType: DateType<XsdDate> get() = DateType.Instance

    override fun toString(): String = xmlString

    override fun ensureTimezone(fallbackTimezone: TimeZone): XsdDate = when {
        dateVal and 0x80000000_00000000uL == 0uL -> {
            val newOffset = fallbackTimezone.offsetAt(instant()).totalSeconds / 60
            XsdDateImpl(dateVal.toLBits(50) or newOffset.toLBits(13, 50) or TZ_BIT)
        }
        else -> this
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is XsdDate) return false
        if (other is XsdDateImpl) {
            if (dateVal == other.dateVal) return true
            if (timezoneOffset == null) return false
            return instant() == other.instant()
        }
        if (timezoneOffset == null) {
            if (other.timezoneOffset != null) return false
        }
        return instant() == other.instant()
    }

    override fun hashCode(): Int {
        return when (timezoneOffset) {
            null -> 31 + dateVal.uLongFromBits(50).hashCode()
            else -> instant().hashCode()
        }
    }


    companion object {

        private val TZ_BIT: ULong =1uL shl 63

        operator fun invoke(str: CharSequence) : XsdDate {
            val normalized = xmlCollapseWhitespace(str)
            val monthIdx = normalized.indexOf('-', 1) // sign can be start
            val year = normalized.substring(0, monthIdx).toLong()


            val month = normalized.substring(monthIdx + 1, monthIdx + 3).toInt()
            if (normalized[monthIdx + 3] != '-') throw NumberFormatException("Missing - between month and day")
            val day = normalized.substring(monthIdx + 4, monthIdx + 6).toInt()

            return when {
                normalized.length >= monthIdx + 6 ->
                    XsdDateImpl(
                        year,
                        month,
                        day,
                        XsdDateTimeImpl.timezoneFragValue(
                            normalized.substring(monthIdx + 6)
                        )
                    )

                else ->
                    XsdDateImpl(year, month, day)
            }
        }
    }
}
