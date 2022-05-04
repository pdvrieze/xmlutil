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
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.XmlStreaming
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import kotlin.test.Test
import kotlin.test.assertEquals

class TestLongTextContent {

    @Test
    fun testSerializeLongTextToAttributeDefault() {
        val data = "xy".repeat(6000)

        val serialized = XML.encodeToString(AttributeTextContainer.serializer(), AttributeTextContainer(data))
        assertXmlEquals("<tag data=\"$data\" />", serialized)
    }

    @Test
    fun testSerializeLongTextToAttributeCrossPlatform() {
        val data = "xy".repeat(6000)

        val serialized = buildString {
            val writer = XmlStreaming.newGenericWriter(this)
            XML.encodeToWriter(writer, AttributeTextContainer.serializer(), AttributeTextContainer(data))
        }

        assertXmlEquals("<tag data=\"$data\" />", serialized)
    }

    @Test
    fun testDeserializeLongTextToAttributeDefault() {
        val data = "xy".repeat(6000)

        val obj = XML.decodeFromString(AttributeTextContainer.serializer(), "<tag data=\"$data\" />")
        assertEquals(AttributeTextContainer(data), obj)
    }

    @Test
    fun testDeserializeLongTextToAttributeCrossPlatform() {
        val data = "xy".repeat(6000)

        val reader = XmlStreaming.newGenericReader("<tag data=\"$data\" />")
        val obj = XML.decodeFromReader(AttributeTextContainer.serializer(), reader)

        assertEquals(AttributeTextContainer(data), obj)
    }

    @Test
    fun testSerializeLongTextToContentDefault() {
        val data = "xy".repeat(6000)

        val serialized = XML.encodeToString(ContentTextContainer.serializer(), ContentTextContainer(data))
        assertXmlEquals("<tag>$data</tag>", serialized)
    }

    @Test
    fun testSerializeLongTextToContentCrossPlatform() {
        val data = "xy".repeat(6000)

        val serialized = buildString {
            val writer = XmlStreaming.newGenericWriter(this)
            XML.encodeToWriter(writer, ContentTextContainer.serializer(), ContentTextContainer(data))
        }

        assertXmlEquals("<tag>$data</tag>", serialized)
    }

    @Test
    fun testDeserializeLongTextToContentDefault() {
        val data = "xy".repeat(6000)

        val obj = XML.decodeFromString(ContentTextContainer.serializer(), "<tag>$data</tag>")
        assertEquals(ContentTextContainer(data), obj)
    }

    @Test
    fun testDeserializeLongTextToContentCrossPlatform() {
        val data = "xy".repeat(6000)

        val reader = XmlStreaming.newGenericReader("<tag>$data</tag>")
        val obj = XML.decodeFromReader(ContentTextContainer.serializer(), reader)

        assertEquals(ContentTextContainer(data), obj)
    }

    @Serializable
    @XmlSerialName("tag", "", "")
    data class AttributeTextContainer(val data: String)

    @Serializable
    @XmlSerialName("tag", "", "")
    data class ContentTextContainer(@XmlValue(true) val data: String)
}
