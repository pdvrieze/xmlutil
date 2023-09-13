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

import io.github.pdvrieze.xmlutil.testutil.assertQNameEquivalent
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.*
import kotlin.jvm.JvmInline
import kotlin.test.*

@OptIn(ExperimentalXmlUtilApi::class)
class IdAttrTest : PlatformTestBase<IdAttrTest.Container>(
    Container(listOf(Element("a"), Element("b")), listOf(OtherElement(ID("c")))),
    Container.serializer()
) {
    override val expectedXML: String =
        "<container><element id=\"a\"/><element id=\"b\"/><other id=\"c\"/></container>"

    override val expectedJson: String =
        "{\"data\":[{\"id\":\"a\"},{\"id\":\"b\"}],\"others\":[{\"id\":\"c\"}]}"

    private val duplicateIds1: String = "<container><element id=\"a\"/><element id=\"a\"/></container>"

    private val duplicateIds2: String = "<container><element id=\"a\"/><other id=\"a\"/></container>"

    @Test
    fun testIdInDescriptor() {
        val descriptor = XML.xmlDescriptor(serializer)
        assertQNameEquivalent(QName("container"), descriptor.tagName)
        val element = descriptor.getElementDescriptor(0).getElementDescriptor(0).getElementDescriptor(0)
        assertQNameEquivalent(QName("element"), element.tagName)
        val idAttr = element.getElementDescriptor(0)
        assertQNameEquivalent(QName("id"), idAttr.tagName)

        val otherElement = descriptor.getElementDescriptor(0).getElementDescriptor(1).getElementDescriptor(0)
        assertQNameEquivalent(QName("other"), otherElement.tagName)
        val otherIdAttr = otherElement.getElementDescriptor(0)
        assertQNameEquivalent(QName("id"), otherIdAttr.tagName)

        assertTrue(idAttr.isIdAttr)
        assertTrue(otherIdAttr.isIdAttr)
    }

    @Test
    fun testDuplicateIds() {
        assertFails {
            XML.decodeFromString(serializer, duplicateIds1)
        }
    }

    @Test
    fun testDuplicateIds2() {
        assertFails {
            XML.decodeFromString(serializer, duplicateIds2)
        }
    }

    @JvmInline
    @Serializable
    value class ID(val value: String)

    @Serializable
    @XmlSerialName("other", namespace = "", prefix = "")
    data class OtherElement(@XmlId val id: ID)

    @Serializable
    @XmlSerialName("element", namespace = "", prefix = "")
    data class Element(@XmlId val id: String)

    @Serializable
    @XmlSerialName("container", namespace = "", prefix = "")
    data class Container(val data: List<Element>, val others: List<OtherElement>)

}
