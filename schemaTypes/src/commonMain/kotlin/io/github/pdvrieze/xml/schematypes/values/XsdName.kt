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

import io.github.pdvrieze.xml.schematypes.impl.SimpleTypeSerializer
import io.github.pdvrieze.xml.schematypes.isXmlName
import io.github.pdvrieze.xml.schematypes.isXmlName10
import io.github.pdvrieze.xml.schematypes.types.NameType
import io.github.pdvrieze.xml.schematypes.values.instances.XsdNameImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlReader

@ExperimentalXmlUtilApi
@Serializable(XsdName.Companion::class)
interface XsdName : XsdToken {

    override val schemaType: NameType<XsdName>

    companion object : SimpleTypeSerializer<XsdName>("VName") {
        override fun deserialize(raw: String, input: XmlReader?): XsdName {
            when (input?.version) {
                "1.0" -> check(raw.isXmlName10()) { "'$raw' is not an xml name" }
                else -> check(raw.isXmlName()) { "'$raw' is not an xml name" }

            }
            return invoke(raw)
        }

        operator fun invoke(value: String): XsdName {
            check(value.isXmlName()) { "'$value' is not an xml name" }
            return XsdNameImpl(value)
        }

    }

}
