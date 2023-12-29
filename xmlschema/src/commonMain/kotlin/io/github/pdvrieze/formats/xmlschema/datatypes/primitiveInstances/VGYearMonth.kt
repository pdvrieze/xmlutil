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
value class VGYearMonth(val monthYear: ULong) : IDateTime {

    init {
        require(month in 1u..12u) { "Month values must be between 1 and 12, was $month"}
    }

    constructor(year: Int, month: UInt) : this(
        month.toLBits(4) or
                year.toLBits(52, 4)
    )

    constructor(year: Int, month: UInt, timezoneOffset: Int?) : this(
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

    override val day: Nothing? get() = null
    override val hour: Nothing? get() = null
    override val minute: Nothing? get() = null
    override val second: Nothing? get() = null

    override val xmlString: String get() = "${yearFrag()}-${monthFrag()}${timeZoneFrag()}"

    override fun toString(): String = xmlString

}
