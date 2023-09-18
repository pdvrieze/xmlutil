/*
 * Copyright (c) 2021.
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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.AnyURIType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.xmlCollapseWhitespace
import kotlin.jvm.JvmInline

@JvmInline
@Serializable(VAnyURI.Serializer::class)
value class VAnyURI(val value: String): VAnyAtomicType, CharSequence {
    constructor(charSequence: CharSequence) : this(charSequence.toString())

    init {
        // This can not be AnyURIType as it is used in defining AtomicDataType
        require(value == xmlCollapseWhitespace(value))
    }


    override val xmlString: String get() = value

    override val length: Int get() = xmlString.length

    override fun get(index: Int): Char = xmlString.get(index)

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
        xmlString.subSequence(startIndex, endIndex)

    override fun toString(): String = value

    companion object Serializer: KSerializer<VAnyURI> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("xsd.anyURI", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): VAnyURI {
            return VAnyURI(xmlCollapseWhitespace(decoder.decodeString()))
        }


        override fun serialize(encoder: Encoder, value: VAnyURI) {
            encoder.encodeString(value.value)
        }
    }
}

internal fun String.toAnyUri(): VAnyURI = VAnyURI(this)
