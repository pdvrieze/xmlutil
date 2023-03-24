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

package nl.adaptivity.xml.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.serializer
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

class ValueContainerWithCustomSerializerTest : PlatformXmlTestBase<ValueContainerWithCustomSerializerTest.ValueContainer>(
    ValueContainer(InnerValueContainer("<foo&bar>")),
    ValueContainer.serializer()
) {
    override val expectedXML: String =
        "<valueContainer><innerValueContainer>&lt;foo&amp;bar></innerValueContainer></valueContainer>"

    @Serializable(with = CompatValueContainerSerializer::class)
    @XmlSerialName("valueContainer")
    data class ValueContainer(val inner: InnerValueContainer)

    @Serializable
    @XmlSerialName("innerValueContainer")
    data class InnerValueContainer(@XmlValue(true) val content: String)

    object CompatValueContainerSerializer : ValueContainerSerializer() {
        override fun delegateFormat(decoder: Decoder) = (decoder as XML.XmlInput).delegateFormat()
        override fun delegateFormat(encoder: Encoder) = (encoder as XML.XmlOutput).delegateFormat()
    }

    abstract class ValueContainerSerializer : KSerializer<ValueContainer> {

        private val elementSerializer = serializer<InnerValueContainer>()

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("valueContainer") {
            element("innerValueContainer", elementSerializer.descriptor)
        }

        override fun deserialize(decoder: Decoder): ValueContainer {
            return if (decoder is XML.XmlInput) {
                deserializeContainer(decoder, decoder.input)
            } else {
                val data = decoder.decodeStructure(descriptor) {
                    decodeSerializableElement(descriptor, 0, elementSerializer)
                }
                ValueContainer(data)
            }
        }

        private fun deserializeContainer(
            decoder: Decoder,
            reader: XmlReader
        ): ValueContainer {
            val xml = delegateFormat(decoder)

            var innerValueContainer: InnerValueContainer =
                InnerValueContainer("")

            decoder.decodeStructure(descriptor) {

                while (reader.next() != EventType.END_ELEMENT) {
                    when (reader.eventType) {
                        EventType.COMMENT,
                        EventType.IGNORABLE_WHITESPACE,
                        EventType.ENTITY_REF,
                        EventType.TEXT -> {
                        }
                        EventType.START_ELEMENT -> {
                            if (reader.localName != "innerValueContainer") {
                                reader.skipElement()
                            } else {
                                innerValueContainer = xml.decodeFromReader(serializer(), reader)
                            }
                        }
                        else ->
                            throw XmlException("Unexpected tag content")
                    }
                }
            }

            return ValueContainer(innerValueContainer)
        }

        override fun serialize(encoder: Encoder, value: ValueContainer) {
            if (encoder is XML.XmlOutput) {
                return serializeContainer(encoder, encoder.target, value.inner)
            } else {
                encoder.encodeStructure(descriptor) {
                    encodeSerializableElement(descriptor, 0, elementSerializer, value.inner)
                }
            }
        }

        private fun serializeContainer(
            encoder: Encoder,
            target: XmlWriter,
            data: InnerValueContainer
        ) {
            val xml = delegateFormat(encoder)
            encoder.encodeStructure(descriptor) {
                xml.encodeToWriter(target, elementSerializer, data)
            }
        }

        abstract fun delegateFormat(decoder: Decoder): XML
        abstract fun delegateFormat(encoder: Encoder): XML

    }
}
