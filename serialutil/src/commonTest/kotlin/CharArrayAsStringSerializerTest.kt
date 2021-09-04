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

package nl.adaptivity.serialutil.test

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nl.adaptivity.serialutil.CharArrayAsStringSerializer
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlValue
import kotlin.test.Test
import kotlin.test.assertEquals

class CharArrayAsStringSerializerTest {

    @Test
    fun testSerializeXML() {
        val data = Container("abcdefg".toCharArray())
        val expected = "<Container>abcdefg</Container>"
        val actual = XML.encodeToString(data)
        assertEquals(expected, actual)
    }

    @Test
    fun testDeserializeXML() {
        val expected = Container("abcdefg".toCharArray())
        val xml = "<Container>abcdefg</Container>"
        val actual = XML.decodeFromString<Container>(xml)
        assertEquals(expected, actual)
    }

    @Test
    fun testSerializeJson() {
        val data = Container("abcdefg".toCharArray())
        val expected = "{\"data\":\"abcdefg\"}"
        val actual = Json.encodeToString(data)
        assertEquals(expected, actual)
    }

    @Test
    fun testDeserializeJson() {
        val expected = Container("abcdefg".toCharArray())
        val json = "{\"data\":\"abcdefg\"}"
        val actual = Json.decodeFromString<Container>(json)
        assertEquals(expected, actual)
    }

    @Serializable
    class Container(@Serializable(CharArrayAsStringSerializer::class) @XmlValue(true) val data: CharArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Container) return false

            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            return data.contentHashCode()
        }
    }
}
