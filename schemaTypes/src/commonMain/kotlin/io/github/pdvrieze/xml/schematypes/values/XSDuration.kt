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
import io.github.pdvrieze.xml.schematypes.types.DurationType
import io.github.pdvrieze.xml.schematypes.values.instances.XSDurationImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlReader

@ExperimentalXmlUtilApi
@Serializable(XSDuration.Companion::class)
interface XSDuration : XSAtomic {
    // TODO implement DayTimeDuration and YearMonthDuration
    override val schemaType: DurationType<XSDuration>

    val months: Long
    val seconds: Double

    companion object : SimpleTypeSerializer<XSDuration>("xsd.duration") {

        operator fun invoke(str: String): XSDuration {
            return XSDurationImpl(str)
        }

        operator fun invoke(months: Long, millis: Long): XSDuration {
            return XSDurationImpl(months, millis)
        }

        override fun deserialize(raw: String, input: XmlReader?): XSDuration {
            return invoke(raw)
        }
    }
}
