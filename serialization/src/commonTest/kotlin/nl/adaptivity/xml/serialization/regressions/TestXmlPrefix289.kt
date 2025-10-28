/*
 * Copyright (c) 2025.
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

package nl.adaptivity.xml.serialization.regressions

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.namespaceURI
import nl.adaptivity.xmlutil.prefix
import nl.adaptivity.xmlutil.serialization.UnknownXmlFieldException
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TestXmlPrefix289 {

    @Test
    fun textNamespaceInDesc() {
        val desc = XML{ recommended_0_91_0 { pedantic = true } }.xmlDescriptor(SerializableWithLang.serializer())
        val tag = desc.getElementDescriptor(0)
        assertEquals("SerializableWithLang", tag.tagName.localPart)

        val attr = tag.getElementDescriptor(0)
        assertEquals(XMLConstants.XML_NS_URI, attr.tagName.namespaceURI)
        assertEquals(XMLConstants.XML_NS_PREFIX, attr.tagName.prefix)
        assertEquals("lang", attr.tagName.localPart)
    }

    @Test
    fun textNamespaceInDescPedantic() {
        val xml = XML { recommended_0_91_0 { pedantic = true }}
        val desc = xml.xmlDescriptor(SerializableWithLang.serializer())
        val tag = desc.getElementDescriptor(0)
        assertEquals("SerializableWithLang", tag.tagName.localPart)

        val attr = tag.getElementDescriptor(0)
        assertEquals(XMLConstants.XML_NS_URI, attr.tagName.namespaceURI)
        assertEquals(XMLConstants.XML_NS_PREFIX, attr.tagName.prefix)
        assertEquals("lang", attr.tagName.localPart)
    }

    @Test
    fun testSerializeLang() {
        val data = SerializableWithLang("en")
        val expected = "<SerializableWithLang xml:lang=\"en\"/>"

        val actual = XML { recommended { pedantic = true } }.encodeToString( data)
        assertXmlEquals(expected, actual)
    }

    @Test
    fun testDeserializeLang() {
        val expected = SerializableWithLang("en")
        val data = "<SerializableWithLang xml:lang=\"en\"/>"

        val actual = XML { recommended { pedantic = true } }.decodeFromString<SerializableWithLang>( data)
        assertEquals(expected, actual)
    }

    @Test
    fun testDeserializeUnprefixed() {
        val data = "<SerializableWithLang lang=\"en\"/>"

        val xml = XML { recommended { pedantic = true } }
        val e = assertFailsWith<UnknownXmlFieldException> {
            xml.decodeFromString<SerializableWithLang>( data)
        }
        assertContains(e.message!!, ") SerializableWithLang/lang (Attribute)")
        assertContains(e.message!!, "Could not find a field for name (")
    }

    @Serializable
    data class SerializableWithLang(
        @XmlSerialName("lang", prefix = "xml")
        val lang: String
    )
}
