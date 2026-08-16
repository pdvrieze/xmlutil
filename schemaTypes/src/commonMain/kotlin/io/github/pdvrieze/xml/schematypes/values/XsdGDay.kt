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

import io.github.pdvrieze.xml.schematypes.Collation
import io.github.pdvrieze.xml.schematypes.impl.SimpleTypeSerializer
import io.github.pdvrieze.xml.schematypes.types.GDayType
import io.github.pdvrieze.xml.schematypes.values.instances.XsdDateTimeImpl
import io.github.pdvrieze.xml.schematypes.values.instances.XsdGDayImpl
import io.github.pdvrieze.xml.schematypes.values.instances.xsToInt
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.xmlCollapseWhitespace

@XmlUtilInternal
@Serializable(XsdGDay.Companion::class)
interface XsdGDay : IXsdDateTime, XsdPrimitive {

    override val schemaType: GDayType<XsdGDay>

    override val day: UInt

    override val year: Nothing? get() = null
    override val month: Nothing? get() = null
    override val hour: Nothing? get() = null
    override val minute: Nothing? get() = null
    override val second: Nothing? get() = null

    override fun compareTo(other: XsdPrimitive, collation: Collation): Int = when (other) {
        is XsdGDay -> compareTo(other)
        is IXsdDateTime -> toStandardDateTime().compareTo(other.toStandardDateTime())
        else -> throw IllegalArgumentException("Cannot compare $this with $other")
    }

    override fun ensureTimezone(fallbackTimezone: TimeZone): XsdGDay

    override fun toStandardDateTime(): XsdDateTime {
        return XsdDateTime(1972, 12u, day, 0u, 0u, XsdInt.ZERO, null)
    }

    companion object: SimpleTypeSerializer<XsdGDay>("xsd.gDay") {
        operator fun invoke(raw: CharSequence): XsdGDay {
            val normalized = xmlCollapseWhitespace(raw)
            require(normalized.startsWith("---"))
            val tzIndex = normalized.indexOfAny(charArrayOf('-', '+', 'Z'), 4).let {
                if (it < 0) normalized.length else it
            }

            val day = normalized.substring(3, tzIndex).xsToInt()
            val tz = XsdDateTimeImpl.timezoneFragValue(normalized.substring(tzIndex))

            return XsdGDayImpl( day, tz)
        }

        override fun deserialize(raw: String, input: nl.adaptivity.xmlutil.XmlReader?): XsdGDay {
            return XsdGDayImpl(raw.xsToInt())
        }
    }
}

