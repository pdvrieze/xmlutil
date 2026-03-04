/*
 * Copyright (c) 2021-2026.
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

import io.github.pdvrieze.xml.schematypes.WhitespaceValue
import io.github.pdvrieze.xml.schematypes.impl.SimpleTypeSerializer
import io.github.pdvrieze.xml.schematypes.types.NormalizedStringType
import io.github.pdvrieze.xml.schematypes.values.instances.XsdNormalizedStringImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlReader

@ExperimentalXmlUtilApi
@Serializable(XsdNormalizedString.Companion::class)
interface XsdNormalizedString : XsdString {

    override val schemaType: NormalizedStringType<XsdNormalizedString>

    companion object : SimpleTypeSerializer<XsdNormalizedString>("xsd.normalizedString") {
        operator fun invoke(string: String): XsdNormalizedString {
            return XsdNormalizedStringImpl(WhitespaceValue.REPLACE.normalize(XsdString(string)).xmlString)
        }

        override fun deserialize(raw: String, input: XmlReader?): XsdNormalizedString {
            return invoke(raw)
        }
    }

}

