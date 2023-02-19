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

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.StructureKind
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.serialization.structure.SafeParentInfo
import nl.adaptivity.xmlutil.serialization.structure.XmlDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals

/** Test for #102 */
class MapTest : PlatformTestBase<MapTest.ListContainer>(
    ListContainer(
        listOf(
            MapContainer(
                id = "myId", map = mapOf(
                    "a" to MapElement("valueOfA"),
                    "b" to MapElement("valueOfB")
                )
            )
        )
    ),
    ListContainer.serializer()
) {
    override val expectedXML: String =
        "<ListContainer><MapContainer id=\"myId\"><MapElement key=\"a\" name=\"valueOfA\"/><MapElement key=\"b\" name=\"valueOfB\"/></MapContainer></ListContainer>"
    override val expectedJson: String =
        "{\"values\":[{\"id\":\"myId\",\"map\":{\"a\":{\"name\":\"valueOfA\"},\"b\":{\"name\":\"valueOfB\"}}}]}"

    @OptIn(ExperimentalXmlUtilApi::class)
    @Test
    fun testSerializeNotCollapsing() {
        val xml = baseXmlFormat.copy {
            policy = object : DefaultXmlSerializationPolicy(policy) {
                override fun isMapValueCollapsed(mapParent: SafeParentInfo, valueDescriptor: XmlDescriptor): Boolean {
                    return false
                }
            }
        }

        val serialized = xml.encodeToString(serializer, value)
        assertXmlEquals(
            "<ListContainer><MapContainer id=\"myId\">" +
                    "<MapOuter key=\"a\"><MapElement name=\"valueOfA\"/></MapOuter>" +
                    "<MapOuter key=\"b\"><MapElement name=\"valueOfB\"/></MapOuter>" +
                    "</MapContainer></ListContainer>",
            serialized
        )

        assertEquals(value, xml.decodeFromString(serializer, serialized))
    }

    @OptIn(ExperimentalXmlUtilApi::class)
    @Test
    fun testSerializeNotEludingList() {
        val xml = baseXmlFormat.copy {
            policy = object : DefaultXmlSerializationPolicy(policy) {
                override fun isListEluded(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): Boolean {
                    if (serializerParent.elementSerialDescriptor.kind == StructureKind.MAP) return false
                    return super.isListEluded(serializerParent, tagParent)
                }
            }
        }

        val serialized = xml.encodeToString(serializer, value)
        assertXmlEquals(
            "<ListContainer><MapContainer id=\"myId\"><MapOuter>" +
                    "<MapElement key=\"a\" name=\"valueOfA\"/>" +
                    "<MapElement key=\"b\" name=\"valueOfB\"/>" +
                    "</MapOuter></MapContainer></ListContainer>",
            serialized
        )

        assertEquals(value, xml.decodeFromString(serializer, serialized))
    }

    @OptIn(ExperimentalXmlUtilApi::class, ExperimentalSerializationApi::class)
    @Test
    fun testSerializeMaxChildMap() {
        val xml = baseXmlFormat.copy {
            policy = object : DefaultXmlSerializationPolicy(policy) {
                override fun isListEluded(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): Boolean {
                    if (serializerParent.elementSerialDescriptor.kind == StructureKind.MAP) return false
                    return super.isListEluded(serializerParent, tagParent)
                }

                override fun effectiveOutputKind(
                    serializerParent: SafeParentInfo,
                    tagParent: SafeParentInfo,
                    canBeAttribute: Boolean
                ): OutputKind = when {
                    serializerParent.elementUseNameInfo.serialName == "key" -> OutputKind.Element
                    else -> super.effectiveOutputKind(serializerParent, tagParent, canBeAttribute)
                }
            }
        }

        val serialized = xml.encodeToString(serializer, value)
        assertXmlEquals(
            "<ListContainer><MapContainer id=\"myId\"><MapOuter>" +
                    "<entry><key>a</key><MapElement name=\"valueOfA\"/></entry>" +
                    "<entry><key>b</key><MapElement name=\"valueOfB\"/></entry>" +
                    "</MapOuter></MapContainer></ListContainer>",
            serialized
        )

        assertEquals(value, xml.decodeFromString(serializer, serialized))
    }

    @OptIn(ExperimentalXmlUtilApi::class)
    @Test
    fun testSerializeWithConflictingKeyName() {
        val xml = baseXmlFormat.copy {
            policy = object : DefaultXmlSerializationPolicy(policy) {
                override fun mapKeyName(serializerParent: SafeParentInfo): XmlSerializationPolicy.DeclaredNameInfo {
                    return XmlSerializationPolicy.DeclaredNameInfo("name", null)
                }
            }
        }

        val serialized = xml.encodeToString(serializer, value)
        assertXmlEquals(
            "<ListContainer><MapContainer id=\"myId\">" +
                    "<MapOuter name=\"a\"><MapElement name=\"valueOfA\"/></MapOuter>" +
                    "<MapOuter name=\"b\"><MapElement name=\"valueOfB\"/></MapOuter>" +
                    "</MapContainer></ListContainer>",
            serialized
        )

        assertEquals(value, xml.decodeFromString(serializer, serialized))
    }

    enum class AddresStatus { VALID, INVALID, TEMPORARY }

    @Serializable
    data class ListContainer(val values: List<MapContainer>)

    @Serializable
    data class MapContainer(
        val id: String,
        @XmlElement(true)
        @XmlSerialName("MapOuter", "", "")
        val map: Map<String, MapElement> = mapOf(),
    )

    @Serializable
    data class MapElement(val name: String)

}
