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
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

public typealias SerializableElement=@Serializable(ElementSerializer::class) Element

public object ElementSerializer : XmlSerializer<Element> {
    private val attrSerializer = MapSerializer(String.serializer(), String.serializer())

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("element") {
        element("namespace", serialDescriptor<String>(), isOptional = true)
        element("localname", serialDescriptor<String>())
        element("attributes", attrSerializer.descriptor, isOptional = true)
        element("content", ListSerializer(NodeSerializer).descriptor, isOptional = true)
    }

    override fun deserialize(decoder: Decoder): Element = when (decoder) {
        is XML.XmlInput -> deserializeXML(decoder, decoder.input)
        is DocumentDecoder -> deserialize(decoder)
        else -> deserialize(DocumentDecoder(decoder))
    }

    override fun deserializeXML(decoder: Decoder, input: XmlReader, previousValue: Element?, isValueChild: Boolean): Element {
        val document = previousValue?.ownerDocument ?: createDocument(input.name)
        val fragment = document.createDocumentFragment()
        val out = DomWriter(fragment)
        out.writeElement(null, input)
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
            if (attributes == null) attributes = mapOf()
            if (content == null) content = listOf()

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

    override fun serializeXML(encoder: Encoder, output: XmlWriter, value: Element, isValueChild: Boolean) {
        writeElem(output, value)
    }

    override fun serialize(encoder: Encoder, value: Element) {
        when (encoder) {
            is XML.XmlOutput -> serializeXML(encoder, encoder.target, value)

            else -> {
                encoder.encodeStructure(descriptor) {
                    val ln = value.localName
                    if (ln == null) {
                        encodeStringElement(descriptor, 1, value.tagName)
                    } else {
                        val namespaceURI = value.getNamespaceURI()
                        if (!namespaceURI.isNullOrEmpty()) {
                            encodeStringElement(descriptor, 0, namespaceURI)
                        }
                        encodeStringElement(descriptor, 1, value.localName)
                    }

                    if (value.attributes.length > 0) {
                        val attrIterator: Iterator<Attr> = value.attributes.iterator()
                        val m = attrIterator.asSequence().associate { it.nodeName to it.getValue() }
                        encodeSerializableElement(descriptor, 2, attrSerializer, m)
                    }

                    if (value.childNodes.length > 0) {
                        val n = value.childNodes.iterator().asSequence().toList()
                        encodeSerializableElement(descriptor, 3, ListSerializer(NodeSerializer), n)
                    }
                }
            }
        }
    }

}

internal class DocumentDecoder(private val delegate: Decoder, val document: Document) : Decoder by delegate {

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

private fun writeElem(output: XmlWriter, value: Element) {
    output.smartStartTag(value.getNamespaceURI(), value.localName ?: value.tagName, value.prefix) {
        for (n: Attr in value.attributes) {
            writeAttr(output, n)
        }
        for (child in value.childNodes) {
            child.writeTo(output)
        }
    }
}

private fun writeAttr(output: XmlWriter, value: Attr) {
    output.attribute(
        value.getNamespaceURI(),
        value.getLocalName() ?: value.getName(),
        value.getPrefix(),
        value.getValue()
    )
}

private fun writeCData(output: XmlWriter, value: CDATASection) {
    output.cdsect(value.textContent!!)
}

private fun writeText(output: XmlWriter, value: Text) {
    output.text(value.textContent!!)
}

private fun writeComment(output: XmlWriter, value: Comment) {
    output.comment(value.textContent!!)
}

private fun writePI(output: XmlWriter, value: ProcessingInstruction) {
    output.processingInstruction("${value.getTarget()} ${value.textContent ?: ""}")
}

internal fun Node.writeTo(output: XmlWriter) = when (nodeType) {
    NodeConsts.ELEMENT_NODE -> writeElem(output, this as Element)
    NodeConsts.ATTRIBUTE_NODE -> writeAttr(output, this as Attr)
    NodeConsts.CDATA_SECTION_NODE -> writeCData(output, this as CDATASection)
    NodeConsts.TEXT_NODE -> writeText(output, this as Text)
    NodeConsts.COMMENT_NODE -> writeComment(output, this as Comment)
    NodeConsts.PROCESSING_INSTRUCTION_NODE -> writePI(output, this as ProcessingInstruction)
    else -> throw IllegalArgumentException("Can not serialize node: ${this}")
}

