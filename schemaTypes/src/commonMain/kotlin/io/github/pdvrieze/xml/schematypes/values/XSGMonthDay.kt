/*
 * Copyright (c) 2023-2026.
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

import io.github.pdvrieze.xml.schematypes.impl.SimpleTypeSerializer
import io.github.pdvrieze.xml.schematypes.types.GMonthDayType
import io.github.pdvrieze.xml.schematypes.values.instances.XSGMonthDayImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi

@ExperimentalXmlUtilApi
@Serializable(XSGMonthDay.Companion::class)
interface XSGMonthDay: IXSDateTime {

    override val schemaType: GMonthDayType<XSGMonthDay>

    override val day: UInt
    override val month: UInt

    override val year: Nothing? get() = null
    override val hour: Nothing? get() = null
    override val minute: Nothing? get() = null
    override val second: Nothing? get() = null

    companion object: SimpleTypeSerializer<XSGMonthDay>("xsd.gMonthDay") {
        operator fun invoke(str: String): XSGMonthDay = XSGMonthDayImpl(str)

        operator fun invoke(month: UInt, day: UInt): XSGMonthDay =
            XSGMonthDayImpl(month, day)

        operator fun invoke(month: UInt, day: UInt, timezoneOffset: Int?): XSGMonthDay =
            XSGMonthDayImpl(month, day, timezoneOffset)

        override fun deserialize(raw: String, input: nl.adaptivity.xmlutil.XmlReader?): XSGMonthDay {
            return XSGMonthDayImpl(raw.toUInt())
        }
    }
}
