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
import io.github.pdvrieze.xml.schematypes.types.TimeType
import io.github.pdvrieze.xml.schematypes.values.XsdDecimal
import io.github.pdvrieze.xml.schematypes.values.XsdTime
import io.github.pdvrieze.xml.schematypes.values.XsdUnsignedInt
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import nl.adaptivity.xmlutil.XmlUtilInternal

@XmlUtilInternal
class XsdTimeImpl private constructor(val msecVal: ULong) : XsdTime {
    constructor(hours: UInt, minutes: UInt, millis: UInt) : this(
        hours.toLBits(5) or
                minutes.toLBits(6, 5) or
                millis.toLBits(16, 11)
    ) {
        requireRange(minutes < 60u) { "Minutes out of range: $minutes" }
        requireRange(millis < 60000u) { "Millis out of range: $millis" }
    }

    constructor(hours: UInt, minutes: UInt, seconds: XsdDecimal) : this(
        hours, minutes, millis = ((seconds* XsdUnsignedInt(1000u)).toUInt())
    )

    constructor(hours: UInt, minutes: UInt, millis: UInt, timezoneOffset: Int?) : this(
        hours.toLBits(5) or
                minutes.toLBits(6, 5) or
                millis.toLBits(16, 11) or
                when (timezoneOffset) {
                    null -> 0uL
                    else -> (1uL shl 63) or timezoneOffset.toLBits(13, 27)
                }
    ) {
        requireRange(minutes < 60u) { "Minutes out of range: $minutes" }
        requireRange(millis < 60000u) { "Millis out of range: $millis" }
        requireRange(timezoneOffset == null || timezoneOffset in -1440..1440) { "Timezone offset out of range: $timezoneOffset" }
    }


    override val hour: UInt
        get() = msecVal.uintFromBits(5)

    override val minute: UInt
        get() = (msecVal shr 5).uintFromBits(6)

    val millis: UInt = (msecVal shr 11).uintFromBits(16)

    override val second: XsdDecimal
        get() {
            val millis = millis
            return when {
                millis % 1000u == 0u -> XsdUnsignedInt(millis / 1000u)
                else -> BigDecimal(millis, 3)
            }
        }

    val totalMillis: ULong
        get() {
            return (hour * 24uL + minute) * 60_000uL + millis
        }

    override val timezoneOffset: Int?
        get() = when {
            msecVal and 0x80000000_00000000uL == 0uL -> null
            else -> (msecVal shr 27).intFromBits(13)
        }

    override fun ensureTimezone(fallbackTimezone: TimeZone): XsdTime = when {
        msecVal and TZ_MARKER == 0uL -> {
            val newOffset = fallbackTimezone.offsetAt(instant()).totalSeconds / 60
            XsdTimeImpl(msecVal.toLBits(27) or newOffset.toLBits(13, 27) or TZ_MARKER)
        }

        else -> this
    }

    override val xmlString: String get() = "${hourFrag()}:${minuteFrag()}:${secondFrag()}${timeZoneFrag()}"
    override val schemaType: TimeType<*> get() = TimeType.Instance

    override fun toString(): String = xmlString

    override fun hashCode(): Int = when (val tzMinutes = timezoneOffset){
        null -> msecVal.hashCode() // this will work if there is no timezone

        // in this case calculate the UTC seconds and use that as hashcode// the addition of 24 hours
        // is to deal with "negative" timezones
        else -> (totalMillis + (tzMinutes + 24 * 60).mod(24 * 60).toULong() * 60_000uL)
            .hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is XsdTimeImpl) return false // extend to broader

        if (msecVal == other.msecVal) return true
        if ((timezoneOffset == null) != (other.timezoneOffset == null)) return false
        if (timezoneOffset == null) return false // should not happen due to the msecVal comparison

        val leftMillis = totalMillis + (timezoneOffset!! + 24 * 60).mod(24 * 60).toULong() * 60_000uL
        val rightMillis = other.totalMillis + (other.timezoneOffset!! + 24 * 60).mod(24 * 60).toULong() * 60_000uL

        return leftMillis == rightMillis
    }

    companion object {
        val TZ_MARKER = 1uL shl 63

        operator fun invoke(representation: CharSequence): XsdTimeImpl {
            require(representation.length >= 8)
            val hours = representation.substring(0, 2).xsToUInt()
            require(representation[2] == ':') { "Expected ':' at position 2 of '$representation'"}
            val minutes = representation.substring(3, 5).xsToUInt()
            requireRange(minutes < 60u) { "Minutes out of range: $minutes" }
            require(representation[5] == ':') { "Expected ':' at position5 of '$representation'" }
            val secEnd = (6..<representation.length)
                .firstOrNull { val c = representation[it]; c != '.' && c !in '0'..'9' }
                ?: representation.length
            val millis = (representation.substring(6, secEnd).xsToDouble() * 1000.0).toUInt()
            requireRange(millis < 60000u) { "Millis out of range: ${representation.substring(6, secEnd)}" }

            requireRange(hours in 0u..24u) { "Hour out of range: $hours" }
            if (hours == 24u) {
                requireRange(minutes == 0u && millis == 0u) {
                    "24:00:00 is the largest time that can be represented (got: $representation)"
                }
            }


            return when {
                secEnd < representation.length -> {
                    val tz = XsdDateTimeImpl.timezoneFragValue(representation.substring(secEnd))
                    XsdTimeImpl(hours, minutes, millis, tz)
                }

                else -> XsdTimeImpl(hours, minutes, millis)
            }

        }
    }
}
