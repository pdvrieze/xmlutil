/*
 * Copyright (c) 2022.
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

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.serialization.*
import nl.adaptivity.xmlutil.serialization.*
import kotlin.jvm.JvmInline
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import nl.adaptivity.xml.serialization.NamespaceCollectionStackoverflowTest.Option as RealOption
import nl.adaptivity.xml.serialization.NamespaceCollectionStackoverflowTest.Repeat as RealRepeat

/**
 * Test for #106. Invalid recursion in types. It also tests for a related issue in an inconsistency
 * between the name used when encoding/decoding polymorphic value classes. *
 */
class NamespaceCollectionStackoverflowTest {
    val EXPECTEDXML = run {
        val repeat = if (Select.RepeatWrapper.serializer().descriptor.isInline) {
            "<d:repeat text=\"test\"/>"
        } else {
            "<d:RepeatWrapper>\n                <d:repeat text=\"test\"/>\n            </d:RepeatWrapper>"
        }

        "<d:document xmlns:d=\"http://custom\">\n" +
                "    <d:head>\n" +
                "        <d:title>ISSUE 106</d:title>\n" +
                "    </d:head>\n" +
                "    <d:body>\n" +
                "        <d:select variable=\"foo\">\n" +
                "            $repeat\n" +
                "        </d:select>\n" +
                "    </d:body>\n" +
                "</d:document>"
    }

    lateinit var document: Document
    lateinit var xml: XML

    @Serializable
    sealed interface Element // Any

    @BeforeTest
    fun init() {
        document = Document(
            head = Head("ISSUE 106"),
            body = Body(
                elements = listOf(
                    Select(variable = "foo", selectElements = listOf(Select.RepeatWrapper(RealRepeat("test")))),
                )
            )
        )
        xml = XML {
            indent = 4
            autoPolymorphic = true
            encodeDefault = XmlSerializationPolicy.XmlEncodeDefault.NEVER
            isCollectingNSAttributes = true // BUG
        }
    }


    @Test
    fun testSerialize() {
        val actualXML = xml.encodeToString(document)
        assertXmlEquals(EXPECTEDXML, actualXML)
    }

    @Test
    fun testDeserialize() {
        // Skip if not inline (on javascript) as it is broken
        if (Select.Node.serializer().descriptor.getElementDescriptor(0).isInline) {
            val actualDocument = xml.decodeFromString<Document>(EXPECTEDXML)
            assertEquals(document, actualDocument)
        }
    }

    @Serializable
    @XmlSerialName("document", Namespaces.view, Namespaces.prefixView)
    data class Document(
        val head: Head,
        val body: Body,
        val nature: String? = null, // DEFAULT XMLNS
        @XmlOtherAttributes
        val other: Map<String, String> = emptyMap()
    )

    @Serializable
    @XmlSerialName("head", Namespaces.view, Namespaces.prefixView)
    data class Head(@XmlElement(true) val title: String? = null)


    @Serializable
    @XmlSerialName("body", Namespaces.view, Namespaces.prefixView)
    data class Body(
        val elements: List<Element> = emptyList(),
        @XmlOtherAttributes
        val other: Map<String, String> = emptyMap()
    )

    @Serializable
    @XmlSerialName("select", Namespaces.view, Namespaces.prefixView)
    data class Select(
        val variable: String = "",
        val selectElements: List<Node> = emptyList()
    ) : Element {

        @Serializable
        sealed interface Node // typing scheme

        @JvmInline
        @Serializable
        value class RepeatWrapper(val repeatAttr: RealRepeat) : Node

        @JvmInline
        @Serializable
        value class OptionWrapper(val optionAttr: RealOption) : Node
    }

    @Serializable
    @XmlSerialName("repeat", Namespaces.view, Namespaces.prefixView)
    data class Repeat(
        val text: String = "",
        val repeatElements: List<Element> = emptyList()
    ) : Element

    @Serializable
    @XmlSerialName("option", Namespaces.view, Namespaces.prefixView)
    data class Option(@Required val value: String, val text: String = "", val selected: String = "") : Element

    object Namespaces {
        const val view = "http://custom"
        const val prefixView = "d"
    }
}
