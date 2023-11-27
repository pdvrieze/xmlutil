/*
 * Copyright (c) 2023.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.xmlCollapseWhitespace

@Serializable(VRelaxedURI.Serializer::class)
class VRelaxedURI(override val xmlString: String) : VAnyURI() {
    constructor(charSequence: CharSequence) : this(charSequence.toString())

    init {
        // This can not be AnyURIType as it is used in defining AtomicDataType
        require(value == xmlCollapseWhitespace(value))
    }


    override val length: Int get() = xmlString.length

    override fun get(index: Int): Char = xmlString.get(index)

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
        xmlString.subSequence(startIndex, endIndex)

    override fun toString(): String = xmlString

    companion object Serializer : KSerializer<VRelaxedURI> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("xsd.anyURI", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): VRelaxedURI {
            return VRelaxedURI(xmlCollapseWhitespace(decoder.decodeString()))
        }


        override fun serialize(encoder: Encoder, value: VRelaxedURI) {
            encoder.encodeString(value.xmlString)
        }
    }
}
