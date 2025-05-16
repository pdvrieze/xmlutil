/*
 * Copyright (c) 2025.
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

package nl.adaptivity.xml.serialization.regressions

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.serialization.structure.SafeParentInfo
import nl.adaptivity.xmlutil.serialization.structure.XmlDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals

class CustomMapKey274 {

    @Test
    fun testSerialize() {
        val data = MapContainer(mapOf("a" to MapElement("avalue"), "b" to MapElement("bvalue")))
        val expected = "<MapContainer><MapElement name=\"a\" value=\"avalue\"/><MapElement name=\"b\" value=\"bvalue\"/></MapContainer>"
        val actual = XML.encodeToString(data)
        assertXmlEquals(expected, actual)
    }

    @Test
    fun testDeserialize() {
        val expected = MapContainer(mapOf("a" to MapElement("avalue"), "b" to MapElement("bvalue")))
        val data = "<MapContainer><MapElement name=\"a\" value=\"avalue\"/><MapElement name=\"b\" value=\"bvalue\"/></MapContainer>"
        val actual = XML.decodeFromString<MapContainer>(data)
        assertEquals(expected, actual)
    }

    @Test
    fun testSerializeNotCollapsed() {
        val xml = XML{}.copy {
            policy = object : DefaultXmlSerializationPolicy(policy) {
                override fun isMapValueCollapsed(mapParent: SafeParentInfo, valueDescriptor: XmlDescriptor): Boolean {
                    return false
                }
            }
        }


        val data = MapContainer2(mapOf("a" to MapElement("avalue"), "b" to MapElement("bvalue")))
        val expected = "<MapContainer2><MapInner name=\"a\"><MapElement value=\"avalue\"/></MapInner><MapInner name=\"b\"><MapElement value=\"bvalue\"/></MapInner></MapContainer2>"
        val actual = xml.encodeToString<MapContainer2>(data)
        assertXmlEquals(expected, actual)
    }

    @Test
    fun testDeserializeNotCollapsed() {
        val xml = XML{}.copy {
            policy = object : DefaultXmlSerializationPolicy(policy) {
                override fun isMapValueCollapsed(mapParent: SafeParentInfo, valueDescriptor: XmlDescriptor): Boolean {
                    return false
                }
            }
        }


        val expected = MapContainer2(mapOf("a" to MapElement("avalue"), "b" to MapElement("bvalue")))
        val data = "<MapContainer2><MapInner name=\"a\"><MapElement value=\"avalue\"/></MapInner><MapInner name=\"b\"><MapElement value=\"bvalue\"/></MapInner></MapContainer2>"
        val actual = xml.decodeFromString<MapContainer2>(data)
        assertEquals(expected, actual)
    }

    @Test
    fun testSerializeNotCollapsedPolicy() {
        val xml = XML{}.copy {
            policy = object : DefaultXmlSerializationPolicy(policy) {
                override fun isMapValueCollapsed(mapParent: SafeParentInfo, valueDescriptor: XmlDescriptor): Boolean {
                    return false
                }
            }
        }


        val data = MapContainer(mapOf("a" to MapElement("avalue"), "b" to MapElement("bvalue")))
        val expected = "<MapContainer><MapOuter name=\"a\"><MapElement value=\"avalue\"/></MapOuter><MapOuter name=\"b\"><MapElement value=\"bvalue\"/></MapOuter></MapContainer>"
        val actual = xml.encodeToString<MapContainer>(data)
        assertXmlEquals(expected, actual)
    }

    @Test
    fun testDeserializeNotCollapsedPolicy() {
        val xml = XML{}.copy {
            policy = object : DefaultXmlSerializationPolicy(policy) {
                override fun isMapValueCollapsed(mapParent: SafeParentInfo, valueDescriptor: XmlDescriptor): Boolean {
                    return false
                }
            }
        }


        val expected = MapContainer(mapOf("a" to MapElement("avalue"), "b" to MapElement("bvalue")))
        val data = "<MapContainer><MapOuter name=\"a\"><MapElement value=\"avalue\"/></MapOuter><MapOuter name=\"b\"><MapElement value=\"bvalue\"/></MapOuter></MapContainer>"
        val actual = xml.decodeFromString<MapContainer>(data)
        assertEquals(expected, actual)
    }

    @Test
    fun testSerializeStringMap() {
        val data = MyClass(mapOf("abc" to "def"))
        val expected="<MyClass><value key=\"abc\" value=\"def\"/></MyClass>"
        assertXmlEquals(expected, XML.encodeToString(data))
    }

    @Serializable
    data class MapElement(val value: String)

    @Serializable
    data class MapContainer(
        @XmlElement(true)
        @XmlSerialName("MapOuter", "", "")
        @XmlKeyName("name")
        val map: Map<String, MapElement>
    )

    @Serializable
    data class MapContainer2(
        @XmlElement(true)
        @XmlMapEntryName("MapInner")
        @XmlKeyName("name")
        val map: Map<String, MapElement>
    )

    @Serializable
    class MyClass(val map: Map<String, String>)
}
