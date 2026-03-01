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

import io.github.pdvrieze.xml.schematypes.values.XSInteger
import io.github.pdvrieze.xml.schematypes.values.XSNonNegativeInteger
import io.github.pdvrieze.xml.schematypes.values.XSShort

internal class XSShortImpl(override val shortValue: Short) : XSShort {
    override val xmlString: String get() = intValue.toString()

    override fun toString(): String = xmlString

    override fun compareTo(other: XSInteger): Int = when (other) {
        is XSNonNegativeInteger -> if (intValue<0) -1 else intValue.toULong().compareTo(other.toULong())
        else -> longValue.compareTo(other.toLong())
    }
}
