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

package io.github.pdvrieze.xml.schematypes.values

import io.github.pdvrieze.xml.schematypes.impl.SimpleTypeSerializer
import io.github.pdvrieze.xml.schematypes.types.DateTimeType
import io.github.pdvrieze.xml.schematypes.values.instances.XsdDateTimeStampImpl
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlReader
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@ExperimentalXmlUtilApi
@OptIn(ExperimentalTime::class)
@Serializable(XsdDateTimeStamp.Companion::class)
interface XsdDateTimeStamp : XsdDateTime {
    override val timezoneOffset: Int

    override val schemaType: DateTimeType<XsdDateTimeStamp>

    override fun ensureTimezone(fallbackTimezone: TimeZone): XsdDateTimeStamp = this

    companion object: SimpleTypeSerializer<XsdDateTimeStamp>("xsd.dateTime") {

        operator fun invoke(instant: Instant, timezone: TimeZone= TimeZone.UTC): XsdDateTimeStamp {
            return XsdDateTimeStampImpl(instant, timezone)
        }

        operator fun invoke(str: CharSequence): XsdDateTimeStamp = XsdDateTimeStampImpl(str)

        operator fun invoke(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int,
            second: XsdDecimal,
            timezoneOffset: Int
        ): XsdDateTimeStampImpl {
            return XsdDateTimeStampImpl(year, month, day, hour, minute, second, timezoneOffset)
        }


        override fun deserialize(raw: String, input: XmlReader?): XsdDateTimeStamp {
            return invoke(raw)
        }
    }

}
