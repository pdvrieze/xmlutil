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
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.dom.Document
import nl.adaptivity.xmlutil.dom.Element
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.util.impl.createDocument
import kotlin.test.Test
import kotlin.test.assertEquals


private fun parseToDocument(xmlReader: XmlReader): Document {
    while((! xmlReader.isStarted) || xmlReader.eventType!=EventType.START_ELEMENT) {
        xmlReader.next()
    }

    val document = createDocument(xmlReader.name)

    parseToElementChildren(document.documentElement!!, xmlReader)
    return document
}

private fun parseToElementChildren(parent: Element, xmlReader: XmlReader) {
    for (nsDecl in xmlReader.namespaceDecls) {
        if (nsDecl.prefix.isBlank()) {
            parent.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, XMLConstants.XMLNS_ATTRIBUTE, nsDecl.namespaceURI)
        } else {
            parent.setAttributeNS(
                namespace = XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                cName = "${XMLConstants.XMLNS_ATTRIBUTE}:${nsDecl.prefix}",
                value = nsDecl.namespaceURI
            )
        }
    }

    for (attrIdx in 0 until xmlReader.attributeCount) {
        val prefix = xmlReader.getAttributePrefix(attrIdx)
        val cName = when {
            prefix.isEmpty() -> xmlReader.getAttributeLocalName(attrIdx)
            else -> "$prefix:${xmlReader.getAttributeLocalName(attrIdx)}"
        }

        parent.setAttributeNS(
            namespace = xmlReader.getAttributeNamespace(attrIdx),
            cName = cName,
            value = xmlReader.getAttributeValue(attrIdx)
        )
    }

    while (xmlReader.hasNext() && xmlReader.next() != EventType.END_ELEMENT) {
        when (xmlReader.eventType) {
            EventType.START_ELEMENT -> {
                val newChild = parent.ownerDocument.createElementNS(xmlReader.namespaceURI, xmlReader.name.toCName())

                parent.appendChild(newChild)
                parseToElementChildren(newChild, xmlReader)
            }
            EventType.START_DOCUMENT,
            EventType.END_DOCUMENT,
            EventType.ATTRIBUTE,
            EventType.END_ELEMENT -> throw UnsupportedOperationException("Should not happen: ${xmlReader.eventType}")
            EventType.ENTITY_REF -> parent.appendChild(parent.ownerDocument.createTextNode(xmlReader.text))
            EventType.COMMENT -> parent.appendChild(parent.ownerDocument.createComment(xmlReader.text))
            EventType.TEXT -> parent.appendChild(parent.ownerDocument.createTextNode(xmlReader.text))
            EventType.CDSECT -> parent.appendChild(parent.ownerDocument.createCDATASection(xmlReader.text))
            EventType.PROCESSING_INSTRUCTION -> parent.appendChild(parent.ownerDocument.createProcessingInstruction(xmlReader.name.toCName(), xmlReader.text))
            EventType.DOCDECL -> Unit // ignore
            EventType.IGNORABLE_WHITESPACE -> parent.appendChild(parent.ownerDocument.createTextNode(xmlReader.text))
        }
    }
}

private fun <T> XmlTestBase<T>.testDomSerializeXmlImpl(baseXmlFormat: XML) {
    val expectedDom = parseToDocument(xmlStreaming.newGenericReader(expectedXML))

    println("Expected xml\n${expectedDom.toString().prependIndent("    ")}\n")

    val writer = DomWriter()

    baseXmlFormat.encodeToWriter(writer, serializer, value)

    println("Actual xml\n${writer.target.toString().prependIndent("    ")}\n")

    assertDomEquals(expectedDom, writer.target)
}

private fun <T> XmlTestBase<T>.testDomDeserializeXmlImpl(baseXmlFormat: XML) {
    val expectedDom = parseToDocument(xmlStreaming.newGenericReader(expectedXML))

    val actualReader = DomReader(expectedDom)

    assertEquals(value, baseXmlFormat.decodeFromReader(serializer, actualReader))
}

actual abstract class PlatformXmlTestBase<T> actual constructor(
    value: T,
    serializer: KSerializer<T>,
    serializersModule: SerializersModule,
    baseXmlFormat: XML
) : XmlTestBase<T>(
    value,
    serializer,
    serializersModule,
    baseXmlFormat
) {
    @Test
    open fun testDomSerializeXml() {
        testDomSerializeXmlImpl(baseXmlFormat)
    }

    @Test
    open fun testDomDeserializeXml() {
        testDomSerializeXmlImpl(baseXmlFormat)
    }
}

actual abstract class PlatformTestBase<T> actual constructor(
    value: T,
    serializer: KSerializer<T>,
    serializersModule: SerializersModule,
    baseXmlFormat: XML,
    baseJsonFormat: Json
) : TestBase<T>(value, serializer, serializersModule, baseXmlFormat, baseJsonFormat) {
    @Test
    open fun testDomSerializeXml() {
        testDomSerializeXmlImpl(baseXmlFormat)
    }

    @Test
    open fun testDomDeserializeXml() {
        testDomSerializeXmlImpl(baseXmlFormat)
    }
}

actual abstract class PlatformTestPolymorphicBase<T> actual constructor(
    value: T,
    serializer: KSerializer<T>,
    serializersModule: SerializersModule,
    baseJsonFormat: Json
) : TestPolymorphicBase<T>(value, serializer, serializersModule, baseJsonFormat) {
    @Test
    open fun testDomSerializeXml() {
        testDomSerializeXmlImpl(baseXmlFormat)
    }

    @Test
    open fun testDomDeserializeXml() {
        testDomSerializeXmlImpl(baseXmlFormat)
    }
}

