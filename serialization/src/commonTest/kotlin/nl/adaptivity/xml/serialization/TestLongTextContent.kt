/*
 * Copyright (c) 2022-2025.
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

package nl.adaptivity.xml.serialization

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.newGenericWriter
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XML1_0
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.xmlStreaming
import kotlin.test.Test
import kotlin.test.assertEquals

class TestLongTextContent {

    @Test
    fun testSerializeLongTextToAttributeDefault() {
        @Suppress("DEPRECATION")
        testSerializeLongTextToAttributeDefault(XML.compat.instance)
    }

    @Test
    fun testSerializeLongTextToAttributeDefault1_0() {
        testSerializeLongTextToAttributeDefault(XML1_0.instance)
    }

    private fun testSerializeLongTextToAttributeDefault(format: XML) {
        val data = "xy".repeat(6000)

        val serialized = format.encodeToString(AttributeTextContainer.serializer(), AttributeTextContainer(data))
        assertXmlEquals("<tag data=\"$data\" />", serialized)
    }

    @Test
    fun testSerializeLongTextToAttributeCrossPlatform() {
        @Suppress("DEPRECATION")
        testSerializeLongTextToAttributeCrossPlatform(XML.compat.instance)
    }

    @Test
    fun testSerializeLongTextToAttributeCrossPlatform1_0() {
        testSerializeLongTextToAttributeCrossPlatform(XML1_0.instance)
    }

    private fun testSerializeLongTextToAttributeCrossPlatform(format: XML) {
        val data = "xy".repeat(6000)

        val serialized = buildString {
            val writer = xmlStreaming.newGenericWriter(this)
            format.encodeToWriter(writer, AttributeTextContainer.serializer(), AttributeTextContainer(data))
        }

        assertXmlEquals("<tag data=\"$data\" />", serialized)
    }

    @Test
    fun testDeserializeLongTextToAttributeDefault() {
        @Suppress("DEPRECATION")
        testDeserializeLongTextToAttributeDefault(XML.compat.instance)
    }

    @Test
    fun testDeserializeLongTextToAttributeDefault1_0() {
        testDeserializeLongTextToAttributeDefault(XML1_0.instance)
    }

    private fun testDeserializeLongTextToAttributeDefault(format: XML) {
        val data = "xy".repeat(6000)

        val obj = format.decodeFromString(AttributeTextContainer.serializer(), "<tag data=\"$data\" />")
        assertEquals(AttributeTextContainer(data), obj)
    }

    @Test
    fun testDeserializeLongTextToAttributeCrossPlatform() {
        @Suppress("DEPRECATION")
        testDeserializeLongTextToAttributeCrossPlatform(XML.compat.instance)
    }

    @Test
    fun testDeserializeLongTextToAttributeCrossPlatform1_0() {
        testDeserializeLongTextToAttributeCrossPlatform(XML1_0.instance)
    }

    private fun testDeserializeLongTextToAttributeCrossPlatform(format: XML) {
        val data = "xy".repeat(6000)

        val reader = xmlStreaming.newGenericReader("<tag data=\"$data\" />")
        val obj = format.decodeFromReader(AttributeTextContainer.serializer(), reader)

        assertEquals(AttributeTextContainer(data), obj)
    }

    @Test
    fun testSerializeLongTextToContentDefault() {
        @Suppress("DEPRECATION")
        testSerializeLongTextToContentDefault(XML.compat.instance)
    }

    @Test
    fun testSerializeLongTextToContentDefault1_0() {
        testSerializeLongTextToContentDefault(XML1_0.instance)
    }

    private fun testSerializeLongTextToContentDefault(format: XML) {
        val data = "xy".repeat(6000)

        val serialized = format.encodeToString(ContentTextContainer.serializer(), ContentTextContainer(data))
        assertXmlEquals("<tag>$data</tag>", serialized)
    }

    @Test
    fun testSerializeLongTextToContentCrossPlatform() {
        @Suppress("DEPRECATION")
        testSerializeLongTextToContentCrossPlatform(XML.compat.instance)
    }

    @Test
    fun testSerializeLongTextToContentCrossPlatform1_0() {
        testSerializeLongTextToContentCrossPlatform(XML1_0.instance)
    }

    private fun testSerializeLongTextToContentCrossPlatform(format: XML) {
        val data = "xy".repeat(6000)

        val serialized = buildString {
            val writer = xmlStreaming.newGenericWriter(this)
            format.encodeToWriter(writer, ContentTextContainer.serializer(), ContentTextContainer(data))
        }

        assertXmlEquals("<tag>$data</tag>", serialized)
    }

    @Test
    fun testDeserializeLongTextToContentDefault() {
        @Suppress("DEPRECATION")
        testDeserializeLongTextToContentDefault(XML.compat.instance)
    }

    @Test
    fun testDeserializeLongTextToContentDefault1_0() {
        testDeserializeLongTextToContentDefault(XML1_0.instance)
    }

    private fun testDeserializeLongTextToContentDefault(format: XML) {
        val data = "xy".repeat(6000)

        val obj = format.decodeFromString(ContentTextContainer.serializer(), "<tag>$data</tag>")
        assertEquals(ContentTextContainer(data), obj)
    }

    @Test
    fun testDeserializeLongTextToContentCrossPlatform() {
        @Suppress("DEPRECATION")
        testDeserializeLongTextToContentCrossPlatform(XML.compat.instance)
    }

    @Test
    fun testDeserializeLongTextToContentCrossPlatform1_0() {
        testDeserializeLongTextToContentCrossPlatform(XML1_0.instance)
    }

    private fun testDeserializeLongTextToContentCrossPlatform(format: XML) {
        val data = "xy".repeat(6000)

        val reader = xmlStreaming.newGenericReader("<tag>$data</tag>")
        val obj = format.decodeFromReader(ContentTextContainer.serializer(), reader)

        assertEquals(ContentTextContainer(data), obj)
    }

    @Serializable
    @XmlSerialName("tag", "", "")
    data class AttributeTextContainer(val data: String)

    @Serializable
    @XmlSerialName("tag", "", "")
    data class ContentTextContainer(@XmlValue(true) val data: String)
}
