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

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.dom.*
import nl.adaptivity.xmlutil.util.impl.createDocument
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.collections.mapOf
import kotlin.collections.single

public object ElementSerializer : KSerializer<Element> {
    private val attrSerializer = MapSerializer(String.serializer(), String.serializer())

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("element") {
        element("namespace", serialDescriptor<String>(), isOptional = true)
        element("localname", serialDescriptor<String>())
        element("attributes", attrSerializer.descriptor)
        element("content", ListSerializer(NodeSerializer).descriptor)
    }

    override fun deserialize(decoder: Decoder): Element = when (decoder) {
        is XML.XmlInput -> deserializeInput(decoder)
        is DocumentDecoder -> deserialize(decoder)
        else -> deserialize(DocumentDecoder(decoder))
    }

    private fun deserializeInput(decoder: XML.XmlInput): Element {
        val document = createDocument(decoder.input.name)

        val fragment = document.createDocumentFragment()
        val out = DomWriter(fragment)
        out.writeElement(null, decoder.input)
        var e = fragment.firstChild
        while (e != null && e.nodeType != NodeConsts.ELEMENT_NODE) {
            e = e.nextSibling
        }
        return e as Element? ?: throw SerializationException("Expected element, but did not find it")
    }

    private fun deserialize(decoder: DocumentDecoder): Element {
        return decoder.decodeStructure(descriptor) {
            val contentSerializer = ListSerializer(NodeSerializer)
            var idx = decodeElementIndex(descriptor)
            var nameSpace: String? = null
            var localName: String? = null
            var attributes: Map<String, String>? = null
            var content: List<Node>? = null
            while (idx != CompositeDecoder.DECODE_DONE) {
                when (idx) {
                    0 -> nameSpace = decodeStringElement(descriptor, 0)
                    1 -> localName = decodeStringElement(descriptor, 1)
                    2 -> attributes = attrSerializer.deserialize(decoder)
                    3 -> content = contentSerializer.deserialize(decoder)
                    CompositeDecoder.UNKNOWN_NAME -> throw SerializationException("Found unexpected child at index: $idx")
                    else -> throw IllegalStateException("Received an unexpected decoder value: $idx")
                }
                idx = decodeElementIndex(descriptor)
            }
            if (localName == null) throw SerializationException("Missing localName")
            if (attributes == null) throw SerializationException("Missing attributes")
            if (content == null) throw SerializationException("Missing content")

            val doc = decoder.document
            (if (nameSpace.isNullOrEmpty()) doc.createElement(localName) else doc.createElementNS(
                nameSpace,
                localName
            )).apply {
                for ((name, value) in attributes) {
                    setAttribute(name, value)
                }
                for (node in content) {
                    appendChild(doc.adoptNode(node))
                }
            }
        }
    }

    override fun serialize(encoder: Encoder, value: Element) {
        when (encoder) {
            is XML.XmlOutput -> {
                serialize(encoder.target, value)
            }
            else -> {
                encoder.encodeStructure(descriptor) {
                    val ln = value.localName
                    if (ln == null) {
                        encodeStringElement(descriptor, 1, value.tagName)
                    } else {
                        val namespaceURI = value.namespaceURI
                        if (!namespaceURI.isNullOrEmpty()) {
                            encodeStringElement(descriptor, 0, namespaceURI)
                        }
                        encodeStringElement(descriptor, 1, value.localName)
                    }

                    val attrIterator: Iterator<Attr> = value.attributes.iterator()
                    val m = attrIterator.asSequence().associate { it.nodeName to it.value }
                    encodeSerializableElement(descriptor, 2, attrSerializer, m)

                    val n = value.childNodes.iterator().asSequence().toList()
                    encodeSerializableElement(descriptor, 3, ListSerializer(NodeSerializer), n)
                }
            }
        }
    }

}

private fun serialize(encoder: XmlWriter, value: Element) {
    encoder.smartStartTag(value.namespaceURI, value.localName ?: value.tagName, value.prefix) {
        for (n: Attr in value.attributes) {
            serialize(encoder, n)
        }
        for (child in value.childNodes) {
            serialize(encoder, child)
        }
    }
}

private class DocumentDecoder(private val delegate: Decoder, val document: Document) : Decoder by delegate {

    constructor(delegate: Decoder) : this(
        delegate,
        createDocument(QName("dummy")).apply { documentElement?.let { removeChild(it) } }
    )

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        return DocumentCompositeDecoder(delegate.beginStructure(descriptor), document)
    }
}

private class DocumentCompositeDecoder(private val delegate: CompositeDecoder, val document: Document) :
    CompositeDecoder by delegate {

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?
    ): T {
        @OptIn(WillBePrivate::class)
        return delegate.decodeSerializableElement(descriptor, index, deserializer.wrap(document), previousValue)
    }

    @ExperimentalSerializationApi
    override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        previousValue: T?
    ): T? {
        @OptIn(WillBePrivate::class)
        return delegate.decodeNullableSerializableElement(descriptor, index, deserializer.wrap(document), previousValue)
    }
}

@WillBePrivate
public class WrappedDeserializationStrategy<T>(
    public val delegate: DeserializationStrategy<T>,
    public val document: Document
) :
    DeserializationStrategy<T> {
    override val descriptor: SerialDescriptor get() = delegate.descriptor

    override fun deserialize(decoder: Decoder): T {
        return delegate.deserialize(DocumentDecoder(decoder, document))
    }
}

@OptIn(WillBePrivate::class)
private fun <T> DeserializationStrategy<T>.wrap(document: Document): WrappedDeserializationStrategy<T> {
    return WrappedDeserializationStrategy(this, document)
}

private fun serialize(encoder: XmlWriter, value: Attr) {
    encoder.attribute(value.namespaceURI, value.localName ?: value.name, value.prefix, value.value)
}

private fun serialize(encoder: XmlWriter, value: CDATASection) {
    encoder.cdsect(value.textContent!!)
}

private fun serialize(encoder: XmlWriter, value: Text) {
    encoder.text(value.textContent!!)
}

private fun serialize(encoder: XmlWriter, value: Comment) {
    encoder.comment(value.textContent!!)
}

private fun serialize(encoder: XmlWriter, value: ProcessingInstruction) {
    encoder.processingInstruction("${value.getTarget()} ${value.textContent ?: ""}")
}

private fun serialize(encoder: XmlWriter, value: Node) = when (value.nodeType) {
    NodeConsts.ELEMENT_NODE -> serialize(encoder, value as Element)
    NodeConsts.ATTRIBUTE_NODE -> serialize(encoder, value as Attr)
    NodeConsts.CDATA_SECTION_NODE -> serialize(encoder, value as CDATASection)
    NodeConsts.TEXT_NODE -> serialize(encoder, value as Text)
    NodeConsts.COMMENT_NODE -> serialize(encoder, value as Comment)
    NodeConsts.PROCESSING_INSTRUCTION_NODE -> serialize(encoder, value as ProcessingInstruction)
    else -> throw IllegalArgumentException("Can not serialize node: ${value}")
}

public object NodeSerializer : KSerializer<Node> {
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
        is DocumentDecoder -> deserialize(decoder)
        else -> deserialize(DocumentDecoder(decoder))
    }


    private fun deserialize(decoder: DocumentDecoder): Node {
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
                                    .apply { value = map.values.single() }
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

    override fun serialize(encoder: Encoder, value: Node) {
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
                    encodeSerializableElement(descriptor, 1, attrSerializer, mapOf((value as Attr).name to value.value))
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
