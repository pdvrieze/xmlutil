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

package nl.adaptivity.xmlutil.dom2

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.dom.NodeConsts
import nl.adaptivity.xmlutil.util.impl.createDocument

internal object NodeSerializer : XmlSerializer<Node> {
    private val attrSerializer = MapSerializer(String.serializer(), String.serializer())

    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    public val ed: SerialDescriptor =
        buildSerialDescriptor("org.w3c.dom.Node", SerialKind.CONTEXTUAL) {
            element("text", serialDescriptor<String>())
            // don't use ElementSerializer to break initialization loop
            element("element", buildSerialDescriptor("element", SerialKind.CONTEXTUAL) {})
        }

    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor("node", PolymorphicKind.SEALED) {

        element("type", serialDescriptor<String>())
        element("value", ed)


    }


    override fun deserialize(decoder: Decoder): Node = when (decoder) {
        is Document2Decoder -> deserialize(decoder)
        else -> deserialize(Document2Decoder(decoder))
    }

    override fun deserializeXML(decoder: Decoder, input: XmlReader, previousValue: Node?, isValueChild: Boolean): Node {
        val document = previousValue?.ownerDocument ?: createDocument(QName("DummyDoc"))
        val fragment = document.createDocumentFragment()
        val out = DomWriter(fragment)
        when {
            input.eventType == EventType.START_ELEMENT ->
                out.writeElement(null, input)

            else ->
                out.writeCurrentEvent(input)
        }
        return when (fragment.childNodes.getLength()) {
            0 -> throw SerializationException("Expected node, but did not find it")
            1 -> fragment.firstChild!!
            else -> fragment // shouldn't happen, but...
        }
    }


    private fun deserialize(decoder: Document2Decoder): Node {
        var result: Node? = null
        decoder.decodeStructure(descriptor) {
            var type: String? = null
            var nextValue = decodeElementIndex(descriptor)
            while (nextValue != CompositeDecoder.DECODE_DONE) {
                when (nextValue) {
                    0 -> type = decodeStringElement(descriptor, 0)
                    1 -> {
                        when (type) {
                            null -> throw SerializationException("Missing type")
                            "element" -> result = decodeSerializableElement(descriptor, 1, ElementSerializer)
                            "attr" -> {
                                val map = decodeSerializableElement(descriptor, 1, attrSerializer)
                                if (map.size != 1) throw SerializationException("Only a single attribute pair expected")
                                result = decoder.document.createAttribute(map.keys.single())
                                    .also { it.setValue(map.values.single()) }
                            }
                            "text" -> result = decoder.document.createTextNode(decodeStringElement(descriptor, 1))
                            "comment" -> result = decoder.document.createComment(decodeStringElement(descriptor, 1))
                            else -> throw SerializationException("unsupported type: $type")
                        }
                    }
                }
                nextValue = decodeElementIndex(descriptor)
            }
        }
        return result ?: throw SerializationException("Missing value")

    }

    override fun serializeXML(encoder: Encoder, output: XmlWriter, value: Node, isValueChild: Boolean) {
        value.writeTo(output)
    }

    override fun serialize(encoder: Encoder, value: Node) {
        // For XML documents should not be used
        // Note that only for elements
        encoder.encodeStructure(descriptor) {
            when (value.nodeType) {
                NodeConsts.DOCUMENT_NODE,
                NodeConsts.DOCUMENT_FRAGMENT_NODE -> {
                    val type = if (value.nodeType == NodeConsts.DOCUMENT_FRAGMENT_NODE) "fragment" else "document"
                    encodeStringElement(descriptor, 0, type)
                    val children = value.childNodes.iterator().asSequence().toList()
                    encodeSerializableElement(descriptor, 1, ListSerializer(NodeSerializer), children)
                }
                NodeConsts.ELEMENT_NODE -> {
                    encodeStringElement(descriptor, 0, "element")
                    encodeSerializableElement(descriptor, 1, ElementSerializer, value as Element)
                }
                NodeConsts.ATTRIBUTE_NODE -> {
                    encodeStringElement(descriptor, 0, "attr")
                    encodeSerializableElement(descriptor, 1, attrSerializer,
                        mapOf((value as Attr).getName() to value.getValue())
                    )
                }
                NodeConsts.TEXT_NODE,
                NodeConsts.CDATA_SECTION_NODE -> {
                    encodeStringElement(descriptor, 0, "text")
                    encodeStringElement(descriptor, 1, value.textContent ?: "")
                }
                NodeConsts.COMMENT_NODE -> {
                    encodeStringElement(descriptor, 0, "comment")
                    encodeStringElement(descriptor, 1, value.textContent ?: "")
                } // ignore comments
                NodeConsts.PROCESSING_INSTRUCTION_NODE -> throw SerializationException("Processing instructions can not be serialized")
                else -> throw SerializationException("Cannot serialize: $value")
            }

        }

    }

}
