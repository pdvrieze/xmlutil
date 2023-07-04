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
value class VGYearMonth(val monthYear: Int) : VAnyAtomicType {

    init {
        require((monthYear and 0xf) in 1..12)
    }

    constructor(month: Int, year: Int) : this((month and 0xf) or (year shl 4))

    val month: Int get() = monthYear and 0xf
    val year: Int get() = monthYear shr 4

    override val xmlString: String get() = "${year.toString().padStart(4,'0')}-${month.toString().padStart(2,'0')}"
}
