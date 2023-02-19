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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import kotlin.test.Test
import kotlin.test.assertEquals

class ListTest : PlatformTestBase<ListTest.SimpleList>(
    SimpleList("1", "2", "3"),
    SimpleList.serializer()
) {
    override val expectedXML: String = "<l><value>1</value><value>2</value><value>3</value></l>"
    override val expectedJson: String = "{\"values\":[\"1\",\"2\",\"3\"]}"

    @Serializable
    @SerialName("l")
    data class SimpleList(@XmlSerialName("value", "", "") val values: List<String>) {
        constructor(vararg values: String) : this(values.toList())
    }

    @Test
    fun testUnwrappedListSerialization() {
        val data = listOf(
            SimpleList("1"),
            SimpleList("2"),
            SimpleList("3"),
        )
        val expectedXml = "<ArrayList><l><value>1</value></l><l><value>2</value></l><l><value>3</value></l></ArrayList>"
        val serializedXml = XML.encodeToString(data)
        assertEquals(expectedXml, serializedXml)
    }

    @Test
    fun testUnwrappedListDeserialization() {
        val expectedData = listOf(
            SimpleList("1"),
            SimpleList("2"),
            SimpleList("3"),
        )
        val serialXml = "<ArrayList><l><value>1</value></l><l><value>2</value></l><l><value>3</value></l></ArrayList>"
        val decodedData = XML.decodeFromString<List<SimpleList>>(serialXml)
        assertEquals(expectedData, decodedData)
    }

}
