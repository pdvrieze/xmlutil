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
value class VDate(val dateVal: ULong) : IDateTime {
    constructor(year: Int, month: Int, day: Int) : this(
        day.toLBits(5) or
                month.toLBits(4, 5) or
                year.toLBits(41, 9)
    )

    constructor(year: Int, month: Int, day: Int, timezoneOffset: Int?) : this(
        day.toLBits(5) or
                month.toLBits(4, 5) or
                year.toLBits(41, 9) or
                when (timezoneOffset) {
                    null -> 0uL
                    else -> (1uL shl 63) or timezoneOffset.toLBits(13, 50)
                }
    )

    override val day: UInt get() = dateVal.uintFromBits(5)

    override val month: UInt get() = (dateVal shr 5).uintFromBits(4)

    override val year: Int get() = (dateVal shr 9).intFromBits(41)

    override val timezoneOffset: Int? get() = when {
        dateVal and 0x7000000000000000uL == 0uL -> null
        else -> (dateVal shr 50).intFromBits(13)
    }

    override val hour: Nothing? get() = null
    override val minute: Nothing? get() = null
    override val second: Nothing? get() = null

    override val xmlString: String
        get() = "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${
            day.toString().padStart(2, '0')
        }"

    override fun toString(): String = xmlString
}
