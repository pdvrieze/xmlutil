/*
 * Copyright (c) 2023.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class VTime(val msecVal: UInt) : IDateTime {
    constructor(hours: UInt, minutes: UInt, millis: UInt) : this(
        hours.toIBits(5) or
                minutes.toIBits(6, 5) or
                millis.toIBits(16, 11)
    )

    constructor(hours: UInt, minutes: UInt, millis: UInt, timezoneOffset: Int?) : this(
        hours.toIBits(5) or
                minutes.toIBits(6, 5) or
                millis.toIBits(16, 11) or
                when (timezoneOffset) {
                    null -> 0u
                    else -> (1u shl 31) or timezoneOffset.toIBits(13, 18)
                }
    )


    override val hour: UInt
        get() = msecVal.uintFromBits(5)

    override val minute: UInt
        get() = (msecVal shr 5).uintFromBits(6)

    override val second: VDecimal
        get() {
            val millis = msecVal shr 11
            return when {
                millis % 1000u == 0u -> VUnsignedInt(millis / 1000u)
                else -> VBigDecimalImpl((millis.toDouble() / 1000.0).toString())
            }
        }

    override val timezoneOffset: Int?
        get() = when {
            msecVal and 0x70000000u == 0u -> null
            else -> (msecVal shr 18).intFromBits(13)
        }

    override val month: Nothing? get() = null
    override val day: Nothing? get() = null
    override val year: Nothing? get() = null


    override val xmlString: String get() = "--${monthFrag()}-${dayFrag()}"

    override fun toString(): String = xmlString

    companion object {
        operator fun invoke(representation: String) : VTime {
            require(representation.length>=8)
            val hours = representation.substring(0,2).toUInt()
            require(representation[2]==':')
            val minutes = representation.substring(3,5).toUInt()
            require(representation[5]==':')
            val secEnd = (6..<representation.length).firstOrNull { val c = representation[it] ;c != '.' && c !in '0'..'9' } ?: representation.length
            val millis = (representation.substring(6, secEnd).toDouble() * 1000.0).toUInt()
            return when {
                secEnd<representation.length -> {
                    val tz = IDateTime.timezoneFragValue(representation.substring(secEnd))
                    VTime(hours, minutes, millis, tz)
                }

                else -> VTime(hours, minutes, millis)
            }

        }
    }
}
