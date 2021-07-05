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
import nl.adaptivity.xmlutil.XmlStreaming
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.smartStartTag
import nl.adaptivity.xmlutil.writeElement
import org.w3c.dom.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.dom.DOMResult
import kotlin.collections.Iterator
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
        val document = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().newDocument()
        val fragment = document.createDocumentFragment()
        val out = XmlStreaming.newWriter(DOMResult(fragment))
        out.writeElement(null, decoder.input)
        var e = fragment.firstChild
        while (e != null && e !is Element) {
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
                        if (!value.namespaceURI.isNullOrEmpty()) {
                            encodeStringElement(descriptor, 0, value.namespaceURI)
                        }
                        encodeStringElement(descriptor, 1, value.localName)
                    }

                    val m = value.attributes.iterator().asSequence().associate { it.nodeName to it.nodeValue }
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
        for (n in value.attributes) {
            serialize(encoder, n as Attr)
        }
        for (child in value.childNodes) {
            serialize(encoder, child)
        }
    }
}

private class DocumentDecoder(private val delegate: Decoder, val document: Document) : Decoder by delegate {

    constructor(delegate: Decoder) : this(
        delegate,
        DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
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
        return delegate.decodeSerializableElement(descriptor, index, deserializer.wrap(document), previousValue)
    }

    @ExperimentalSerializationApi
    override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        previousValue: T?
    ): T? {
        return delegate.decodeNullableSerializableElement(descriptor, index, deserializer.wrap(document), previousValue)
    }
}

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

private fun <T> DeserializationStrategy<T>.wrap(document: Document): WrappedDeserializationStrategy<T> {
    return WrappedDeserializationStrategy(this, document)
}

private fun serialize(encoder: XmlWriter, value: Attr) {
    encoder.attribute(value.namespaceURI, value.localName ?: value.name, value.prefix, value.value)
}

private fun serialize(encoder: XmlWriter, value: CDATASection) {
    encoder.cdsect(value.textContent)
}

private fun serialize(encoder: XmlWriter, value: Text) {
    encoder.text(value.textContent)
}

private fun serialize(encoder: XmlWriter, value: Comment) {
    encoder.comment(value.textContent)
}

private fun serialize(encoder: XmlWriter, value: ProcessingInstruction) {
    encoder.processingInstruction(value.textContent)
}

private fun serialize(encoder: XmlWriter, value: Node) = when (value) {
    is Element -> serialize(encoder, value)
    is Attr -> serialize(encoder, value)
    is CDATASection -> serialize(encoder, value)
    is Text -> serialize(encoder, value)
    is Comment -> serialize(encoder, value)
    is ProcessingInstruction -> serialize(encoder, value)
    else -> throw IllegalArgumentException("Can not serialize node of type: ${value.javaClass}")
}

internal operator fun NodeList.iterator() = object : Iterator<Node> {
    val list = this@iterator
    private var nextIdx = 0

    override fun hasNext() = nextIdx < list.length

    override fun next(): Node = list.item(nextIdx++)
}

internal operator fun NamedNodeMap.iterator() = object : Iterator<Node> {
    private val map = this@iterator
    private var nextIdx = 0

    override fun hasNext(): Boolean = nextIdx < map.length

    override fun next(): Node {
        val idx = nextIdx++ // update nextIdx afterwards
        return map.item(idx) as Node
    }

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
        encoder.encodeStructure(descriptor) {
            when (value) {
                is Element -> {
                    encodeStringElement(descriptor, 0, "element")
                    encodeSerializableElement(descriptor, 1, ElementSerializer, value)
                }
                is Attr -> {
                    encodeStringElement(descriptor, 0, "attr")
                    encodeSerializableElement(descriptor, 1, attrSerializer, mapOf(value.name to value.value))
                }
                is Text,
                is CDATASection -> {
                    encodeStringElement(descriptor, 0, "text")
                    encodeStringElement(descriptor, 1, value.textContent)
                }
                is Comment -> {
                    encodeStringElement(descriptor, 0, "comment")
                    encodeStringElement(descriptor, 1, value.textContent)
                } // ignore comments
                is ProcessingInstruction -> throw SerializationException("Processing instructions can not be serialized")
                else -> throw SerializationException("Cannot serialize: ${value.javaClass.name}")
            }

        }

    }

}
