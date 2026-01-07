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

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.serialization.XML

@XmlUtilInternal
abstract class SimpleTypeSerializer<T: VAnySimpleType>(typeName: String): KSerializer<T> {
    @OptIn(InternalSerializationApi::class)
    final override val descriptor: SerialDescriptor = /*InlinePrimitiveDescriptor(
        if ('.' in typeName) typeName else "$PKG_NAME.$typeName", String.serializer()
    )*/PrimitiveSerialDescriptor(if ('.' in typeName) typeName else "$PKG_NAME.$typeName", PrimitiveKind.STRING)

    final override fun serialize(encoder: Encoder, value: T) {
        encoder/*.encodeInline(descriptor)*/.encodeString(value.xmlString)
    }

    override fun deserialize(decoder: Decoder): T {
        return deserialize(decoder/*.decodeInline(descriptor)*/.decodeString(), (decoder as? XML.XmlInput)?.input)
    }

    abstract fun deserialize(raw: String, input: XmlReader?): T

    companion object {
        const val PKG_NAME="io.github.pdvrieze.formats.xml.datatype.primitiveInstances"
    }
}
