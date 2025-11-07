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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.dom2.Node
import nl.adaptivity.xmlutil.dom2.Text
import nl.adaptivity.xmlutil.dom2.createDocument
import nl.adaptivity.xmlutil.dom2.data
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.test.multiplatform.Target
import nl.adaptivity.xmlutil.test.multiplatform.testTarget
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.xmlStreaming
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class EntityInNode291 {
    val expectedXml = "<Tag>&amp;Content</Tag>"

    val xml = XML {
        recommended_0_91_0 { pedantic = true }
        defaultToGenericParser = true
    }

    @SerialName("Tag")
    @Serializable
    data class Tag(@XmlValue val content: String)

    @SerialName("Tag")
    @Serializable
    data class NodeTag(@XmlValue val content: List<Node>)

    @SerialName("Tag")
    @Serializable
    data class CFTag(@XmlValue val content: CompactFragment)

    @SerialName("Tag")
    @Serializable
    data class CFListTag(@XmlValue val content: List<CompactFragment>)

    @Test
    fun assertSerializeTag() {
        val data = Tag("&Content")
        assertXmlEquals(expectedXml, xml.encodeToString(data))
    }

    @Test
    fun assertDeserializeTag() {
        val expected = Tag("&Content")
        assertEquals(expected, xml.decodeFromString(expectedXml))
    }

    @Test
    fun assertSerializeNodeTag() {
        if (testTarget == Target.Node) return

        val document = xmlStreaming.genericDomImplementation.createDocument()
        val data = NodeTag(listOf(document.createTextNode("&Content")))
        assertXmlEquals(expectedXml, xml.encodeToString(data))
    }

    @Test
    fun assertDeserializeNodeTag() {
        if (testTarget == Target.Node) return

        val actual = xml.decodeFromString<NodeTag>(expectedXml)
        assertEquals(2, actual.content.size, "Unexpected content: ${actual.content.joinToString()}")
        val textNode1 = assertIs<Text>(actual.content[0])
        val textNode2 = assertIs<Text>(actual.content[1])
        assertEquals("&", textNode1.data)
        assertEquals("Content", textNode2.data)
    }

    @Test
    fun assertSerializeCFTag() {
        val data = CFTag(CompactFragment("&amp;Content"))
        assertXmlEquals(expectedXml, xml.encodeToString(data))
    }

    @Test
    fun assertDeserializeCFTag() {
        val actual = xml.decodeFromString<CFTag>(expectedXml)
        assertEquals("&amp;Content", actual.content.contentString)
    }

    @Test
    fun assertSerializeCFListTag() {
        val data = CFListTag(listOf(CompactFragment("&amp;Content")))
        assertXmlEquals(expectedXml, xml.encodeToString(data))
    }

    @Test
    fun assertDeserializeCFListTag() {
        val actual = xml.decodeFromString<CFListTag>(expectedXml)
        assertEquals(2, actual.content.size, "Unexpected content: ${actual.content.joinToString()}")
        val textNode1 = actual.content[0]
        val textNode2 = actual.content[1]
        assertEquals("&", textNode1.contentString)
        assertEquals("Content", textNode2.contentString)
    }
}
