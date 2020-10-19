/*
 * Copyright (c) 2020.
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

package net.devrieze.serialization.examples.dynamictagnames

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.serializer
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.serialization.XML

abstract class CommonContainerSerializer: KSerializer<Container> {

    protected val elementSerializer = serializer<TestElement>()
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Container") {
        element("data", ListSerializer(elementSerializer).descriptor)
    }

    override fun deserialize(decoder: Decoder): Container {
        if (decoder is XML.XmlInput) {
            return deserializeDynamic(decoder, decoder.input)
        } else {
            val data = decoder.decodeStructure(descriptor) {
                decodeSerializableElement(descriptor, 0, ListSerializer(elementSerializer))
            }
            return Container(data)
        }
    }

    protected abstract fun deserializeDynamic(decoder: Decoder, reader: XmlReader): Container
    override fun serialize(encoder: Encoder, value: Container) {
        if (encoder is XML.XmlOutput) {
            return serializeDynamic(encoder, encoder.target, value.data)
        } else {
            encoder.encodeStructure(descriptor) {
                encodeSerializableElement(descriptor, 0, ListSerializer(elementSerializer), value.data)
            }
        }
    }

    protected abstract fun serializeDynamic(encoder: Encoder, target: XmlWriter, data: List<TestElement>)

    abstract fun delegateFormat(decoder: Decoder): XML
    abstract fun delegateFormat(encoder: Encoder): XML
}