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
value class VGMonthDay(val monthday: Int) : VAnyAtomicType {

    init {
        val d = monthday shr 5
        require(d in 1..12)
        when(d) {
            2 -> require((monthday and 0x1f) in 1..29)
            4, 6, 9, 11 -> require((monthday and 0x1f) in 1..30)
            else -> require((monthday and 0x1f) in 1..31)
        }
    }

    constructor(day: Int, month: Int) : this((day and 0x1f) or (month shl 5))

    val day: Int get() = monthday and 0x1f
    val month: Int get() = monthday shr 5

    override val xmlString: String get() = "$month-$day"
}
