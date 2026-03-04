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
import io.github.pdvrieze.xml.schematypes.types.IDRefType
import io.github.pdvrieze.xml.schematypes.values.instances.XsdIDRefImpl
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.xmlCollapseWhitespace

@Serializable(XsdIDRef.Companion::class)
interface XsdIDRef : XsdNCName {

    override val schemaType: IDRefType<XsdIDRef>

    companion object : SimpleTypeSerializer<XsdIDRef>("xs.IDREF") {
        override fun deserialize(decoder: Decoder): XsdIDRef {
            val r = super.deserialize(decoder)
            if (decoder is XML.XmlInput) {
                checkNotNull(decoder.resolveIdRef(r.xmlString)) {"Unresolvable ID Reference: '${r.xmlString}'"}
            }
            return r
        }

        override fun deserialize(
            raw: String,
            input: XmlReader?
        ): XsdIDRef {
            return XsdIDRefImpl(xmlCollapseWhitespace(raw))
        }
    }

}
