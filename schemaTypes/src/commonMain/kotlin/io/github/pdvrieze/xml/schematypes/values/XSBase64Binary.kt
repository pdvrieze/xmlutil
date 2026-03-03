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
import io.github.pdvrieze.xml.schematypes.types.Base64BinaryType
import io.github.pdvrieze.xml.schematypes.values.instances.XSBase64BinaryImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlReader
import kotlin.io.encoding.Base64

@ExperimentalXmlUtilApi
@Serializable(XSBase64Binary.Companion::class)
interface XSBase64Binary : XSByteArray {
    override val schemaType: Base64BinaryType<XSBase64Binary>

    companion object : SimpleTypeSerializer<XSBase64Binary>("xsd.base64Binary") {

        override fun deserialize(
            raw: String,
            input: XmlReader?
        ): XSBase64Binary {
            return XSBase64BinaryImpl(Base64.Default.decode(raw))
        }

    }

}
