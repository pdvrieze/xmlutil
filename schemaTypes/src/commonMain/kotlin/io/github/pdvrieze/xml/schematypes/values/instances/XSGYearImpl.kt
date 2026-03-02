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
import io.github.pdvrieze.xml.schematypes.impl.toIBits
import io.github.pdvrieze.xml.schematypes.values.XSGYear
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.xmlCollapseWhitespace
import kotlin.jvm.JvmInline

@XmlUtilInternal
@JvmInline
value class XSGYearImpl(val yearVal: UInt) : XSGYear {

    constructor(year: Int, dummy: Nothing? = null) : this(year.toIBits(18))

    constructor(year: Int, timezoneOffset: Int? = null) : this(
        year.toIBits(18) or
                when (timezoneOffset) {
                    null -> 0u
                    else -> (1u shl 31) or timezoneOffset.toIBits(13, 18)
                }
    )

    override val year: Int get() = yearVal.intFromBits(18)

    override val timezoneOffset: Int?
        get() = when {
            yearVal and 0x80000000u == 0u -> null
            else -> (yearVal shr 18).intFromBits(13)
        }


    override val xmlString: String get() = "${yearFrag()}${timeZoneFrag()}"

    companion object {
        operator fun invoke(str: String): XSGYearImpl {
            val s = xmlCollapseWhitespace(str)
            val yearEnd = s.substring(1).indexOfFirst { it !in '0'..'9' }.let { if (it >= 0) it + 1 else s.length }
            val year = s.substring(0, yearEnd).toInt()
            val tzOffset = XSDateTimeImpl.timezoneFragValue(s.substring(yearEnd))
            return XSGYearImpl(year, tzOffset)
        }
    }
}
