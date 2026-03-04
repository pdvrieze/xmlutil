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
import io.github.pdvrieze.xml.schematypes.types.GYearMonthType
import io.github.pdvrieze.xml.schematypes.values.XsdGYearMonth
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.xmlCollapseWhitespace
import kotlin.jvm.JvmInline

@XmlUtilInternal
@JvmInline
value class XsdGYearMonthImpl(val monthYear: ULong) : XsdGYearMonth {

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

    override val xmlString: String get() = "${yearFrag()}-${monthFrag()}${timeZoneFrag()}"
    override val schemaType: GYearMonthType<*> get() = GYearMonthType.Instance

    override fun toString(): String = xmlString

    companion object {
        operator fun invoke(str: String): XsdGYearMonth {
            val (year, month) = xmlCollapseWhitespace(str).split('-').map { it.toInt() }
            return XsdGYearMonthImpl(year, month.toUInt())
        }
    }

}
