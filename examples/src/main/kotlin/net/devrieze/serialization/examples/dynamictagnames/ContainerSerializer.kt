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
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.serializer
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XML


object ContainerSerializer: CommonContainerSerializer() {

    override fun delegateFormat(decoder: Decoder) = (decoder as XML.XmlInput).delegateFormat()
    override fun delegateFormat(encoder: Encoder) = (encoder as XML.XmlOutput).delegateFormat()

    override fun deserializeDynamic(decoder: Decoder, reader: XmlReader): Container {
        val xml = delegateFormat(decoder)
        val elementXmlDescriptor = xml.xmlDescriptor(elementSerializer).getElementDescriptor(0)

        val dataList = mutableListOf<TestElement>()
        decoder.decodeStructure(descriptor) {
            while (reader.next() != EventType.END_ELEMENT) {
                when (reader.eventType) {
                    EventType.COMMENT,
                    EventType.IGNORABLE_WHITESPACE -> {
                    }
                    EventType.TEXT                 -> if (reader.text.isNotBlank()) {
                        throw XmlException("Unexpected text content")
                    }
                    EventType.START_ELEMENT        -> {
                        val filter = DynamicTagReader(reader, elementXmlDescriptor)
                        val data = xml.decodeFromReader(elementSerializer, filter)
                        dataList.add(data)
                    }
                    else                           -> {
                        throw XmlException("Unexpected tag content")
                    }
                }
            }
            val input = reader as? XmlBufferedReader ?: XmlBufferedReader(reader)
        }
        return Container(dataList)
    }

    override fun serializeDynamic(encoder: Encoder, target: XmlWriter, data: List<TestElement>) {
        val xml = (encoder as XML.XmlOutput).delegateFormat()
        val elementXmlDescriptor = xml.xmlDescriptor(elementSerializer).getElementDescriptor(0)

        encoder.encodeStructure(descriptor) {
            for (element in data) {
                val writer = DynamicTagWriter(target, elementXmlDescriptor, element.id.toString())
                xml.encodeToWriter(writer, elementSerializer, element)
            }
        }
    }

}