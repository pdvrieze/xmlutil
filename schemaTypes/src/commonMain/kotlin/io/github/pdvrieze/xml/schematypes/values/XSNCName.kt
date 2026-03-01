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
import io.github.pdvrieze.xml.schematypes.values.instances.XSNCNameImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.core.XmlVersion
import kotlin.jvm.JvmName

@ExperimentalXmlUtilApi
@Serializable(XSNCName.Companion::class)
interface XSNCName : XSName {

    fun toQname(targetNamespace: XSAnyURI?): QName {
        return QName(targetNamespace?.value ?: "", xmlString.toString())
    }

    @OptIn(XmlUtilInternal::class)
    companion object : SimpleTypeSerializer<XSNCName>("xs.NCName") {
        override fun deserialize(
            raw: String,
            input: XmlReader?
        ): XSNCName {
            val version = when {
                input?.version == "1.0" -> XmlVersion.XML10
                else -> XmlVersion.XML11
            }
            return XSNCNameImpl(xmlCollapseWhitespace(raw), version)
        }

        operator fun invoke(value: String): XSNCName = XSNCNameImpl(value)

        @JvmName("invokeNullable")
        operator fun invoke(value: String?): XSNCName? = value?.let { XSNCNameImpl(it) }
    }
}

