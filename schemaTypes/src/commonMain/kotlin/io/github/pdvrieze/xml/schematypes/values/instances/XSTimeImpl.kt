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
import io.github.pdvrieze.xml.schematypes.types.TimeType
import io.github.pdvrieze.xml.schematypes.values.XSDecimal
import io.github.pdvrieze.xml.schematypes.values.XSTime
import io.github.pdvrieze.xml.schematypes.values.XSUnsignedInt
import nl.adaptivity.xmlutil.XmlUtilInternal
import kotlin.jvm.JvmInline

@XmlUtilInternal
@JvmInline
value class XSTimeImpl private constructor(val msecVal: ULong) : XSTime {
    constructor(hours: UInt, minutes: UInt, millis: UInt) : this(
        hours.toLBits(5) or
                minutes.toLBits(6, 5) or
                millis.toLBits(16, 11)
    ) {
        require(minutes < 60u) { "Minutes out of range: $minutes" }
        require(millis < 60000u) { "Millis out of range: $millis" }
    }

    constructor(hours: UInt, minutes: UInt, millis: UInt, timezoneOffset: Int?) : this(
        hours.toLBits(5) or
                minutes.toLBits(6, 5) or
                millis.toLBits(16, 11) or
                when (timezoneOffset) {
                    null -> 0uL
                    else -> (1uL shl 63) or timezoneOffset.toLBits(13, 27)
                }
    ) {
        require(minutes < 60u) { "Minutes out of range: $minutes" }
        require(millis < 60000u) { "Millis out of range: $millis" }
        require(timezoneOffset in -1440..1440) { "Timezone offset out of range: $timezoneOffset" }
    }


    override val hour: UInt
        get() = msecVal.uintFromBits(5)

    override val minute: UInt
        get() = (msecVal shr 5).uintFromBits(6)

    override val second: XSDecimal
        get() {
            val millis = (msecVal shr 11).uintFromBits(16)
            return when {
                millis % 1000u == 0u -> XSUnsignedInt.Companion(millis / 1000u)
                else -> XSDecimalStringImpl((millis.toDouble() / 1000.0).toString())
            }
        }

    override val timezoneOffset: Int?
        get() = when {
            msecVal and 0x80000000_00000000uL == 0uL -> null
            else -> (msecVal shr 27).intFromBits(13)
        }

    override val xmlString: String get() = "${hourFrag()}:${minuteFrag()}:${secondFrag()}${timeZoneFrag()}"
    override val schemaType: TimeType<*> get() = TimeType.Instance

    override fun toString(): String = xmlString

    companion object {
        operator fun invoke(representation: String): XSTimeImpl {
            require(representation.length >= 8)
            val hours = representation.substring(0, 2).toUInt()
            require(representation[2] == ':')
            val minutes = representation.substring(3, 5).toUInt()
            require(representation[5] == ':')
            val secEnd = (6..<representation.length)
                .firstOrNull { val c = representation[it]; c != '.' && c !in '0'..'9' }
                ?: representation.length
            val millis = (representation.substring(6, secEnd).toDouble() * 1000.0).toUInt()

            return when {
                secEnd < representation.length -> {
                    val tz = XSDateTimeImpl.timezoneFragValue(representation.substring(secEnd))
                    XSTimeImpl(hours, minutes, millis, tz)
                }

                else -> XSTimeImpl(hours, minutes, millis)
            }

        }
    }
}
