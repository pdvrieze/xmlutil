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
import io.github.pdvrieze.xml.schematypes.values.instances.XSTimeImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlReader

@ExperimentalXmlUtilApi
@Serializable(XSTime.Companion::class)
interface XSTime : IXSDateTime {
    override val hour: UInt
    override val minute: UInt
    override val second: XSDecimal

    override val month: Nothing? get() = null
    override val day: Nothing? get() = null
    override val year: Nothing? get() = null

    companion object : SimpleTypeSerializer<XSTime>("xsd.time") {
        operator fun invoke(str: String): XSTime = XSTimeImpl(str)
        operator fun invoke(hours: UInt, minutes: UInt, millis: UInt, timezoneOffset: Int?): XSTime =
            XSTimeImpl(hours, minutes, millis, timezoneOffset)

        operator fun invoke(hours: UInt, minutes: UInt, millis: UInt): XSTime =
            XSTimeImpl(hours, minutes, millis)

        override fun deserialize(raw: String, input: XmlReader?): XSTime {
            return invoke(raw)
        }
    }
}

