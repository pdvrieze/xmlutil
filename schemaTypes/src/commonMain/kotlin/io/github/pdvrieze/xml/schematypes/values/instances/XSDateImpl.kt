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
import io.github.pdvrieze.xml.schematypes.values.XSDate
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.xmlCollapseWhitespace
import kotlin.jvm.JvmInline

@JvmInline
@XmlUtilInternal
value class XSDateImpl(val dateVal: ULong) : XSDate {
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
        dateVal and 0x80000000_00000000uL == 0uL -> null
        else -> (dateVal shr 50).intFromBits(13)
    }

    override val xmlString: String
        get() = "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${
            day.toString().padStart(2, '0')
        }"

    override fun toString(): String = xmlString

    companion object {
        operator fun invoke(str: String) : XSDate {
            val normalized = xmlCollapseWhitespace(str)
            val monthIdx = normalized.indexOf('-', 1) // sign can be start
            val year = normalized.substring(0, monthIdx).toInt()
            val month = normalized.substring(monthIdx + 1, monthIdx + 3).toInt()
            if (normalized[monthIdx + 3] != '-') throw NumberFormatException("Missing - between month and day")
            val day = normalized.substring(monthIdx + 4, monthIdx + 6).toInt()

            return when {
                normalized.length >= monthIdx + 6 ->
                    XSDateImpl(
                        year,
                        month,
                        day,
                        XSDateTimeImpl.timezoneFragValue(
                            normalized.substring(monthIdx + 6)
                        )
                    )

                else ->
                    XSDateImpl(year, month, day)
            }
        }
    }
}
