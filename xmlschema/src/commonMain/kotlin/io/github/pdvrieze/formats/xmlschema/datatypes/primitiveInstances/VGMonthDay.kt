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
value class VGMonthDay(val monthdayVal: UInt) : IDateTime {
    constructor(month: UInt, day: UInt) : this(
        day.toIBits(5) or
                month.toIBits(4, 5)
    )

    constructor(month: UInt, day: UInt, timezoneOffset: Int?) : this(
        day.toIBits(5) or
                month.toIBits(4, 5) or
                when (timezoneOffset) {
                    null -> 0u
                    else -> (1u shl 31) or timezoneOffset.toIBits(13, 18)
                }
    )

    init {
        val m = month
        require(m in 1u..12u)
        when(m) {
            2u -> require(day in 1u..29u)
            4u, 6u, 9u, 11u -> require(day in 1u..30u)
            else -> require(day in 1u..31u)
        }
    }

    override val day: UInt get() = monthdayVal.uintFromBits(5)

    override val month: UInt get() = (monthdayVal shr 5).uintFromBits(4)

    override val timezoneOffset: Int? get() = when {
        monthdayVal and 0x70000000u == 0u -> null
        else -> (monthdayVal shr 18).intFromBits(13)
    }


    override val year: Nothing? get() = null
    override val hour: Nothing? get() = null
    override val minute: Nothing? get() = null
    override val second: Nothing? get() = null


    override val xmlString: String get() = "--${monthFrag()}-${dayFrag()}"

    override fun toString(): String = xmlString

}
