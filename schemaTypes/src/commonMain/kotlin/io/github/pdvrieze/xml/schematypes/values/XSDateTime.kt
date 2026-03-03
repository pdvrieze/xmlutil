/*
 * Copyright (c) 2025-2026.
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
import io.github.pdvrieze.xml.schematypes.types.DateTimeType
import io.github.pdvrieze.xml.schematypes.values.instances.XSDateTimeImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlReader
import kotlin.time.ExperimentalTime

@ExperimentalXmlUtilApi
@OptIn(ExperimentalTime::class)
@Serializable(XSDateTime.Companion::class)
interface XSDateTime : IXSDateTime {
    override val schemaType: DateTimeType<XSDateTime>

    companion object: SimpleTypeSerializer<XSDateTime>("xsd.dateTime") {

        operator fun invoke(str: String): XSDateTime = XSDateTimeImpl(str)

        operator fun invoke(
            year: Int,
            month: UInt,
            day: UInt,
            hour: UInt,
            minute: UInt,
            second: XSDecimal,
            timezoneOffset: Int? = null
        ): XSDateTime {
            return XSDateTimeImpl(year, month, day, hour, minute, second, timezoneOffset)
        }

        override fun deserialize(raw: String, input: XmlReader?): XSDateTime {
            return invoke(raw)
        }
    }
}

