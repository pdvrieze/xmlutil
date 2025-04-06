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

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.serialization.DefaultXmlSerializationPolicy
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlChildrenName
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlKeyName
import nl.adaptivity.xmlutil.serialization.XmlSerialName
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
        assertEquals(expected, actual)
    }

    @Test
    fun testSerializeCollapsed() {
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
        assertEquals(expected, actual)
    }


    @Serializable
    class MapElement(val value: String)

    @Serializable
    class MapContainer(
        @XmlElement(true)
        @XmlSerialName("MapOuter", "", "")
        @XmlKeyName("name")
        val map: Map<String, MapElement>
    )
}
