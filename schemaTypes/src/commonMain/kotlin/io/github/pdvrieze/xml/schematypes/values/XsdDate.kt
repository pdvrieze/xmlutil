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
import io.github.pdvrieze.xml.schematypes.types.DateType
import io.github.pdvrieze.xml.schematypes.values.instances.XsdDateImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlReader

@ExperimentalXmlUtilApi
@Serializable(XsdDate.Companion::class)
interface XsdDate : IXsdDateTime {
    override val schemaType: DateType<XsdDate>

    override val hour: Nothing? get() = null
    override val minute: Nothing? get() = null
    override val second: Nothing? get() = null

    companion object: SimpleTypeSerializer<XsdDate>("xsd.date") {

        operator fun invoke(str: String): XsdDate = XsdDateImpl(str)

        operator fun invoke(year: Int, month: Int, day: Int, timezoneOffset: Int? = null): XsdDate {
            return XsdDateImpl(year, month, day, timezoneOffset)
        }

        override fun deserialize(
            raw: String,
            input: XmlReader?
        ): XsdDate {
            return XsdDateImpl(raw)
        }
    }

}

