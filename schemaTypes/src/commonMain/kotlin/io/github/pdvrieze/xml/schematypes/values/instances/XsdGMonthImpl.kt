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
import io.github.pdvrieze.xml.schematypes.impl.uintFromBits
import io.github.pdvrieze.xml.schematypes.types.GMonthType
import io.github.pdvrieze.xml.schematypes.values.XsdGMonth
import nl.adaptivity.xmlutil.XmlUtilInternal
import kotlin.jvm.JvmInline

@XmlUtilInternal
@JvmInline
value class XsdGMonthImpl private constructor(val monthVal: UInt) : XsdGMonth {

    constructor(month: Int, timezoneOffset: Int?) : this(
        month.toIBits(5) or
                when (timezoneOffset) {
                    null -> 0u
                    else -> (1u shl 31) or timezoneOffset.toIBits(13, 18)
                }
    )

    constructor(month: Int, dummy: Nothing? = null) : this(month.toIBits(5))

    init {
        require(month in 1u..12u)
    }

    override val month: UInt get() = monthVal.uintFromBits(5)

    override val timezoneOffset: Int? get() = when {
        monthVal and 0x80000000u == 0u -> null
        else -> (monthVal shr 18).intFromBits(13)
    }


    override val xmlString: String get() = "--${monthFrag()}${timeZoneFrag()}"
    override val schemaType: GMonthType<*> get() = GMonthType.Instance

    override fun toString(): String = xmlString

}
