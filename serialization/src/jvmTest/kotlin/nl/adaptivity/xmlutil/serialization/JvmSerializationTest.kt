/*
 * Copyright (c) 2020-2025.
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

package nl.adaptivity.xmlutil.serialization

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.serialization.json.Json
import nl.adaptivity.xmlutil.dom.Element
import nl.adaptivity.xmlutil.dom.Node
import nl.adaptivity.xmlutil.newReader
import nl.adaptivity.xmlutil.newWriter
import nl.adaptivity.xmlutil.writeCurrent
import nl.adaptivity.xmlutil.xmlStreaming
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.assertEquals

class JvmSerializationTest {

    @Suppress("DEPRECATION")
    @Test
    fun `deserialize DOM node from xml`() {
        val contentText = "<tag>some text <b>some bold text<i>some bold italic text</i></b></tag>"
        val doc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder().newDocument()
        val expectedObj = doc.createElementNS("", "tag").apply {
            appendChild(doc.createTextNode("some text "))
            appendChild(doc.createElementNS("","b").apply {
                appendChild(doc.createTextNode("some bold text"))
                appendChild(doc.createElementNS("", "i").apply {
                    appendChild(doc.createTextNode("some bold italic text"))
                })
            })
        }

        val xml = XML {
            autoPolymorphic = true
        }
        val deserialized = xml.decodeFromString(ElementSerializer, contentText)

        val expected:String = expectedObj.toXmlString()

        val actual: String = deserialized.toXmlString()

        try {
            assertXmlEquals(expected, actual)

//            assertEquals(expectedObj, deserialized)
        } catch (e: AssertionError) {
            assertEquals(expected, actual)
            throw e // if we reach here, throw anyway
        }
    }

    /**
     * An issue from #78
     */
    @Suppress("DEPRECATION")
    @Test
    fun `update dom node with additional attribute`() {
        val xml = XML {}
        val rootNode: Element = xml.decodeFromString(ElementSerializer, "<root></root>")
        rootNode.setAttribute("test", "value")
        assertEquals("<root test=\"value\"/>", xml.encodeToString(ElementSerializer, rootNode))
        println()
    }

    @Test
    fun `serialize DOM content to xml`() {
        val expected = "<tag>some text <b>some bold text<i>some bold italic text</i></b></tag>"
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
        val element = doc.createElement("tag").apply {
            appendChild(doc.createTextNode("some text "))
            appendChild(doc.createElement("b").apply {
                appendChild(doc.createTextNode("some bold text"))
                appendChild(doc.createElement("i").apply {
                    appendChild(doc.createTextNode("some bold italic text"))
                })
            })
        }

        val xml = XML {
            indentString = ""
            autoPolymorphic = true
        }

        val serialized = xml.encodeToString(ElementSerializer, element)
        assertEquals(expected, serialized)
    }

    @Test
    fun `serialize DOM content to json`() {
        @Language("JSON")
        val expected =
            """{"localname":"tag","content":[["text","some text "],["element",{"localname":"b","content":[["text","some bold text"],["element",{"localname":"i","content":[["text","some bold italic text"]]}]]}]]}"""

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
        val element = doc.createElement("tag").apply {
            appendChild(doc.createTextNode("some text "))
            appendChild(doc.createElement("b").apply {
                appendChild(doc.createTextNode("some bold text"))
                appendChild(doc.createElement("i").apply {
                    appendChild(doc.createTextNode("some bold italic text"))
                })
            })
        }

        val json = Json

        val serialized = json.encodeToString(ElementSerializer, element)
        assertEquals(expected, serialized)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `deserialize DOM node from json`() {
        val contentText =
            """{"localname":"tag","attributes":{},"content":[["text","some text "],["element",{"localname":"b","attributes":{},"content":[["text","some bold text"],["element",{"localname":"i","attributes":{},"content":[["text","some bold italic text"]]}]]}]]}"""

        val doc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder().newDocument()
        val expectedObj = doc.createElementNS("", "tag").apply {
            appendChild(doc.createTextNode("some text "))
            appendChild(doc.createElementNS("","b").apply {
                appendChild(doc.createTextNode("some bold text"))
                appendChild(doc.createElementNS("", "i").apply {
                    appendChild(doc.createTextNode("some bold italic text"))
                })
            })
        }

        val json = Json {
            isLenient = true
        }

        val deserialized = json.decodeFromString(ElementSerializer, contentText)

        val expected = expectedObj.toXmlString()
        val actual = deserialized.toXmlString()
        try {
            assertXmlEquals(expected, actual)
        } catch (e: AssertionError) {
            assertEquals(expected, actual)
            throw e // if we reach here, throw anyway
        }
    }

}

private fun Node.toXmlString(): String = buildString {
    xmlStreaming.newReader(this@toXmlString).use { r ->
        xmlStreaming.newWriter(this).use { w ->
            while (r.hasNext()) {
                r.next()
                r.writeCurrent(w)
            }
        }
    }
}
