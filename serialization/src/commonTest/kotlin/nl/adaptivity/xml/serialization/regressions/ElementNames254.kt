/*
 * Copyright (c) 2024-2025.
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

@file:MustUseReturnValues

package nl.adaptivity.xml.serialization.regressions

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import nl.adaptivity.xml.serialization.pedantic
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.OutputKind
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XML1_0
import nl.adaptivity.xmlutil.serialization.XmlElement
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Test for #254 where somehow the name is incorrect */
class ElementNames254 {

    lateinit var xml: XML

    @BeforeTest
    fun setup() {
        xml = XML1_0.pedantic()
    }

    @Serializable
    @SerialName("element")
    data class Element(
        @XmlElement(true)
        val a: String
    )

    @Serializable
    @SerialName("element")
    data class OtherElement(
        @XmlElement(true)
        val b: String
    )

    @Serializable
    data class Parent(
        val element: Element,
    )

    @Serializable
    data class OtherParent(
        val element: OtherElement,
    )

    @Serializable
    data class Root(
        val parent: Parent,
        val otherParent: OtherParent,
    )

    @Test
    fun testDescriptor() {
        val desc = xml.xmlDescriptor(Root.serializer()).getElementDescriptor(0)
        assertEquals(2, desc.elementsCount)
        val pDesc = desc.getElementDescriptor(0)
        assertEquals(QName("Parent"), pDesc.tagName)
        val elem = pDesc.getElementDescriptor(0)
        assertEquals(QName("element"), elem.tagName)
        val aAttr = elem.getElementDescriptor(0)
        assertEquals(QName("a"), aAttr.tagName)

        val opDesc = desc.getElementDescriptor(1)
        assertEquals(QName("OtherParent"), opDesc.tagName)
        val otherElem = opDesc.getElementDescriptor(0)
        assertEquals(QName("element"), otherElem.tagName)

        val bAttr = otherElem.getElementDescriptor(0)
        assertEquals(QName("b"), bAttr.tagName)
        assertEquals(OutputKind.Element, bAttr.effectiveOutputKind)
    }

    @Test
    fun testSerialize() {
        val root = Root(
            parent = Parent(Element("element")),
            otherParent = OtherParent(OtherElement("element")),
        )
        val serialized = xml.encodeToString(root)
        assertXmlEquals(EXPECTED, serialized)
    }

    @Test
    fun testDeserialize() {
        val root = Root(
            parent = Parent(Element("element")),
            otherParent = OtherParent(OtherElement("element")),
        )
        val deserialized: Root = xml.decodeFromString(EXPECTED)

        assertEquals(root, deserialized)
    }

    companion object {
        val EXPECTED = """|<?xml version="1.1"?>
            |<Root>
            |  <Parent><element><a>element</a></element></Parent>
            |  <OtherParent><element><b>element</b></element></OtherParent>
            |</Root>
        """.trimMargin()
    }
}
