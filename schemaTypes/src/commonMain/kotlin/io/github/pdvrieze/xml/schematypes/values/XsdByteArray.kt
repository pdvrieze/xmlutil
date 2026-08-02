/*
 * Copyright (c) 2021-2026.
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

package io.github.pdvrieze.xml.schematypes.values

import io.github.pdvrieze.xml.schematypes.Collation
import io.github.pdvrieze.xml.schematypes.types.AnyAtomicType
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi

@ExperimentalXmlUtilApi
interface XsdByteArray : XsdAtomic, List<Byte> {
    override val schemaType: AnyAtomicType<XsdByteArray>

    val value: ByteArray

    fun compareTo(other: XsdPrimitive, collation: Collation): Int {
        require(other is XsdByteArray) { "Cannot compare $this with $other" }
        for (i in 0 until minOf(value.size, other.value.size)) {
            val diff = value[i].compareTo(other.value[i])
            if (diff != 0) return diff
        }
        return value.size.compareTo(other.value.size)
    }

    fun compareTo(other: XsdByteArray): Int {
        for (i in 0 until minOf(value.size, other.value.size)) {
            val diff = value[i].compareTo(other.value[i])
            if (diff != 0) return diff
        }
        return value.size.compareTo(other.value.size)
    }
}

