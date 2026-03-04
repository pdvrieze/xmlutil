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
import io.github.pdvrieze.xml.schematypes.types.NCNameType
import io.github.pdvrieze.xml.schematypes.values.instances.XsdNCNameImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.xmlCollapseWhitespace
import kotlin.jvm.JvmName

@ExperimentalXmlUtilApi
@Serializable(XsdNCName.Companion::class)
interface XsdNCName : XsdName {

    override val schemaType: NCNameType<XsdNCName>

    fun toQname(targetNamespace: XsdAnyURI?): XsdQName {
        return XsdQName(targetNamespace?.value ?: "", xmlString)
    }

    @OptIn(XmlUtilInternal::class)
    companion object : SimpleTypeSerializer<XsdNCName>("xs.NCName") {
        override fun deserialize(
            raw: String,
            input: XmlReader?
        ): XsdNCName {
            val version = when {
                input?.version == "1.0" -> XmlVersion.XML10
                else -> XmlVersion.XML11
            }
            return XsdNCNameImpl(xmlCollapseWhitespace(raw), version)
        }

        operator fun invoke(value: String): XsdNCName = XsdNCNameImpl(value)

        @JvmName("invokeNullable")
        operator fun invoke(value: String?): XsdNCName? = value?.let { XsdNCNameImpl(it) }
    }
}

