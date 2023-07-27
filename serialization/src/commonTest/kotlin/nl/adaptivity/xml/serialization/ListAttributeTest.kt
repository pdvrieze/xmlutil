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

@file:UseSerializers(QNameSerializer::class)

package nl.adaptivity.xml.serialization

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.DefaultXmlSerializationPolicy
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.structure.SafeParentInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ListAttributeTest : PlatformTestBase<ListAttributeTest.Container>(
    Container(listOf("a", "b")),
    Container.serializer()
) {
    override val expectedXML: String =
        "<container xmlns=\"urn:example.org\" elements=\"a b\" />"
    override val expectedJson: String =
        "{\"elements\":[\"a\",\"b\"]}"

    enum class AddresStatus { VALID, INVALID, TEMPORARY }

    @Test
    fun testDeserializeOtherWhitespace() {
        val xmlInputs = arrayOf('\t', '\n', '\r'). map { "<container xmlns=\"urn:example.org\" elements=\"a${it}b\" />" }
        for (input in xmlInputs) {
            val actual = baseXmlFormat.decodeFromString(serializer, input)
            assertEquals(value, actual)
        }
    }

    @Test
    fun testDeserializeRepeatedWhitespace() {
        val input = "<container xmlns=\"urn:example.org\" elements=\"a  b\" />"
        val actual = baseXmlFormat.decodeFromString(serializer, input)
        assertEquals(value, actual)
    }

    @OptIn(ExperimentalXmlUtilApi::class)
    @Test
    fun testCustomDelimiter() {
        val testDelimiters = arrayOf(", ", "--", ";")
        val xml = baseXmlFormat.copy {
            policy = object : DefaultXmlSerializationPolicy(policy) {
                @ExperimentalXmlUtilApi
                override fun attributeListDelimiters(
                    serializerParent: SafeParentInfo,
                    tagParent: SafeParentInfo
                ): Array<String> = testDelimiters
            }
        }
        val inputs = testDelimiters.map { "<container xmlns=\"urn:example.org\" elements=\"a${it}b\" />" }
        for (input in inputs) {
            val actual = xml.decodeFromString(serializer, input)
            assertEquals(value, actual)
        }

        val actualXML = xml.encodeToString(serializer, value)
        assertXmlEquals("<container xmlns=\"urn:example.org\" elements=\"a, b\" />", actualXML)
    }

    @Serializable
    @XmlSerialName("container", namespace = "urn:example.org", prefix = "")
    data class Container(
        @XmlElement(false)
        val elements: List<String>,
    )

}
