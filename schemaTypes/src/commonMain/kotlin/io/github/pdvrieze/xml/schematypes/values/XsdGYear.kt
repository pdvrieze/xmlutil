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
import io.github.pdvrieze.xml.schematypes.types.GYearType
import io.github.pdvrieze.xml.schematypes.values.instances.XsdGYearImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi

@Serializable(with = XsdGYear.Companion::class)
@ExperimentalXmlUtilApi
interface XsdGYear : IXsdDateTime {

    override val schemaType: GYearType<XsdGYear>

    override val year: Int

    override val day: Nothing? get() = null
    override val hour: Nothing? get() = null
    override val minute: Nothing? get() = null
    override val second: Nothing? get() = null
    override val month: Nothing? get() = null

    companion object: SimpleTypeSerializer<XsdGYear>("xsd.gYear") {
        operator fun invoke(str: String): XsdGYear = XsdGYearImpl(str)

        operator fun invoke(year: Int): XsdGYear = XsdGYearImpl(year, null)

        operator fun invoke(year: Int, timezoneOffset: Int): XsdGYear {
            return XsdGYearImpl(year, timezoneOffset)
        }

        override fun deserialize(raw: String, input: nl.adaptivity.xmlutil.XmlReader?): XsdGYear {
            return XsdGYearImpl(raw)
        }
    }
}

