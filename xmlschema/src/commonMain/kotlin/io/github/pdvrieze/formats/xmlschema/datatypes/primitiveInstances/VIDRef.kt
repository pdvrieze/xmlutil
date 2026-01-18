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

package io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.IDRefType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.xmlCollapseWhitespace
import kotlin.jvm.JvmInline

@JvmInline
@Serializable(VIDRef.Serializer::class)
value class VIDRef(override val xmlString: String) : VNCName {

    init {
        IDRefType.mdlFacets.validateValue(this)
    }

    override fun toString(): String = xmlString

    private class Serializer : SimpleTypeSerializer<VIDRef>("IDREF") {
        override fun deserialize(decoder: Decoder): VIDRef {
            val r = super.deserialize(decoder)
            if (decoder is XML.XmlInput) {
                checkNotNull(decoder.resolveIdRef(r.xmlString)) {"Unresolvable ID Reference: '${r.xmlString}'"}
            }
            return r
        }

        override fun deserialize(
            raw: String,
            input: XmlReader?
        ): VIDRef {
            return VIDRef(xmlCollapseWhitespace(raw))
        }
    }
}
