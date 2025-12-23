/*
 * Copyright (c) 2025.
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

@file:OptIn(ExperimentalSerializationApi::class)
@file:MustUseReturnValues

package nl.adaptivity.xml.serialization.regressions

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.serialization.structure.*
import kotlin.jvm.JvmInline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CustomMapKey274 {

    val xml: XML = XML1_0.recommended {
        policy {
            formatCache = TestFormatCache(DefaultFormatCache())
            pedantic = true
        }
    }

    @Test
    fun testSerialize() {
        val data = MapContainer(mapOf("a" to MapElement("avalue"), "b" to MapElement("bvalue")))
        val expected =
            "<MapContainer><MapElement name=\"a\" value=\"avalue\"/><MapElement name=\"b\" value=\"bvalue\"/></MapContainer>"
        val actual = xml.encodeToString(data)
        assertXmlEquals(expected, actual)
    }

    @Test
    fun testMapContainerDescriptor() {
        val myObjDesc = assertIs<XmlCompositeDescriptor>(xml.xmlDescriptor(MapContainer.serializer()).getElementDescriptor(0))
        assertEquals(QName("MapContainer"), myObjDesc.tagName)

        assertEquals(1, myObjDesc.elementsCount)

        val mapDesc = assertIs<XmlMapDescriptor>(myObjDesc.getElementDescriptor(0))
        assertEquals(true, mapDesc.isValueCollapsed)

        val keyDesc = assertIs<XmlPrimitiveDescriptor>(mapDesc.getElementDescriptor(0))
        assertEquals(OutputKind.Attribute, keyDesc.outputKind)
        assertEquals(QName("name"), keyDesc.tagName)

        val valueDesc = assertIs<XmlCompositeDescriptor>(mapDesc.getElementDescriptor(1))
        assertEquals(1, valueDesc.elementsCount)
        assertEquals(QName("MapElement"), valueDesc.tagName)

        val attrDesc = assertIs<XmlPrimitiveDescriptor>(valueDesc.getElementDescriptor(0))
        assertEquals(OutputKind.Attribute, attrDesc.outputKind)
        assertEquals(PrimitiveKind.STRING, attrDesc.kind)
        assertEquals(QName("value"), attrDesc.tagName)
    }

    @Test
    fun testDeserialize() {
        val expected = MapContainer(mapOf("a" to MapElement("avalue"), "b" to MapElement("bvalue")))
        val data =
            "<MapContainer><MapElement name=\"a\" value=\"avalue\"/><MapElement name=\"b\" value=\"bvalue\"/></MapContainer>"
        val actual = xml.decodeFromString<MapContainer>(data)
        assertEquals(expected, actual)
    }

    @Test
    fun testSerializeNotCollapsed() {
        val xml = xml.copy {
            policy = object : DefaultXmlSerializationPolicy(policy) {
                override fun isMapValueCollapsed(mapParent: SafeParentInfo, valueDescriptor: XmlDescriptor): Boolean {
                    return false
                }
            }
        }


        val data = MapContainer2(mapOf("a" to MapElement("avalue"), "b" to MapElement("bvalue")))
        val expected =
            "<MapContainer2><MapInner name=\"a\"><MapElement value=\"avalue\"/></MapInner><MapInner name=\"b\"><MapElement value=\"bvalue\"/></MapInner></MapContainer2>"
        val actual = xml.encodeToString<MapContainer2>(data)
        assertXmlEquals(expected, actual)
    }

    @Test
    fun testDeserializeNotCollapsed() {
        val xml = xml.copy {
            policy = object : DefaultXmlSerializationPolicy(policy) {
                override fun isMapValueCollapsed(mapParent: SafeParentInfo, valueDescriptor: XmlDescriptor): Boolean {
                    return false
                }
            }
        }


        val expected = MapContainer2(mapOf("a" to MapElement("avalue"), "b" to MapElement("bvalue")))
        val data =
            "<MapContainer2><MapInner name=\"a\"><MapElement value=\"avalue\"/></MapInner><MapInner name=\"b\"><MapElement value=\"bvalue\"/></MapInner></MapContainer2>"
        val actual = xml.decodeFromString<MapContainer2>(data)
        assertEquals(expected, actual)
    }

    @Test
    fun testSerializeNotCollapsedPolicy() {
        val xml = xml.copy {
            policy = object : DefaultXmlSerializationPolicy(policy) {
                override fun isMapValueCollapsed(mapParent: SafeParentInfo, valueDescriptor: XmlDescriptor): Boolean {
                    return false
                }
            }
        }


        val data = MapContainer(mapOf("a" to MapElement("avalue"), "b" to MapElement("bvalue")))
        val expected =
            "<MapContainer><MapOuter name=\"a\"><MapElement value=\"avalue\"/></MapOuter><MapOuter name=\"b\"><MapElement value=\"bvalue\"/></MapOuter></MapContainer>"
        val actual = xml.encodeToString<MapContainer>(data)
        assertXmlEquals(expected, actual)
    }

    @Test
    fun testDeserializeNotCollapsedPolicy() {
        val xml = xml.copy {
            policy = object : DefaultXmlSerializationPolicy(policy) {
                override fun isMapValueCollapsed(mapParent: SafeParentInfo, valueDescriptor: XmlDescriptor): Boolean {
                    return false
                }
            }
        }


        val expected = MapContainer(mapOf("a" to MapElement("avalue"), "b" to MapElement("bvalue")))
        val data =
            "<MapContainer><MapOuter name=\"a\"><MapElement value=\"avalue\"/></MapOuter><MapOuter name=\"b\"><MapElement value=\"bvalue\"/></MapOuter></MapContainer>"
        val actual = xml.decodeFromString<MapContainer>(data)
        assertEquals(expected, actual)
    }

    @Test
    fun testSerializeStringMap() {
        val data = MyClass(mapOf("abc" to "def"))
        val expected = "<MyClass><value key=\"abc\">def</value></MyClass>"
        assertXmlEquals(expected, xml.encodeToString(data))
    }

    @Test
    fun testStringMapDescriptor() {
        val myObjDesc =
            assertIs<XmlCompositeDescriptor>(xml.xmlDescriptor(MyClass.serializer()).getElementDescriptor(0))
        assertEquals(QName("MyClass"), myObjDesc.tagName)

        assertEquals(1, myObjDesc.elementsCount)

        val mapDesc = assertIs<XmlMapDescriptor>(myObjDesc.getElementDescriptor(0))
        assertEquals(true, mapDesc.isValueCollapsed)

        val keyDesc = assertIs<XmlPrimitiveDescriptor>(mapDesc.getElementDescriptor(0))
        assertEquals(OutputKind.Attribute, keyDesc.outputKind)

        val valueDesc = assertIs<XmlPrimitiveDescriptor>(mapDesc.getElementDescriptor(1))
        assertEquals(OutputKind.Element, valueDesc.outputKind)
    }

    @Test
    fun testSerializeStringMultipleMap() {
        val data = MyClass(mapOf("abc" to "def", "123" to "456"))
        val expected = "<MyClass><value key=\"abc\">def</value><value key=\"123\">456</value></MyClass>"
        assertXmlEquals(expected, xml.encodeToString(data))
    }

    @Test
    fun testDeserializeStringMap() {
        val expected = MyClass(mapOf("abc" to "def", "123" to "456"))
        val data = "<MyClass><value key=\"abc\">def</value><value key=\"123\">456</value></MyClass>"
        assertEquals(expected, xml.decodeFromString<MyClass>(data))
    }

    @Test
    fun testStringIntHolderDescriptor() {
        val myObjDesc = assertIs<XmlCompositeDescriptor>(xml.xmlDescriptor(StringIntHolderMap.serializer()).getElementDescriptor(0))
        assertEquals(QName("StringIntHolderMap"), myObjDesc.tagName)

        assertEquals(1, myObjDesc.elementsCount)

        val mapDesc = assertIs<XmlMapDescriptor>(myObjDesc.getElementDescriptor(0))
        assertEquals(true, mapDesc.isValueCollapsed)

        val keyDesc = assertIs<XmlPrimitiveDescriptor>(mapDesc.getElementDescriptor(0))
        assertEquals(OutputKind.Attribute, keyDesc.outputKind)

        val valueDesc = assertIs<XmlInlineDescriptor>(mapDesc.getElementDescriptor(1))
        assertEquals(OutputKind.Element, valueDesc.outputKind)

        val inlineValueDesc = assertIs<XmlPrimitiveDescriptor>(valueDesc.getElementDescriptor(0))
        assertEquals(OutputKind.Element, inlineValueDesc.outputKind)
    }

    @Test
    fun testSerializeStringIntHolderMap() {
        val data = StringIntHolderMap(mapOf("abc" to IntHolder(123), "def" to IntHolder(456)))
        val expected = "<StringIntHolderMap><value key=\"abc\">123</value><value key=\"def\">456</value></StringIntHolderMap>"
        assertXmlEquals(expected, xml.encodeToString(data))
    }

    @Test
    fun testDeserializeStringIntHolderMap() {
        val expected = StringIntHolderMap(mapOf("abc" to IntHolder(123), "def" to IntHolder(456)))
        val data = "<StringIntHolderMap><value key=\"abc\">123</value><value key=\"def\">456</value></StringIntHolderMap>"
        assertEquals(expected, xml.decodeFromString(data))
    }

    @Test
    fun testSerializeStringNonValueIntHolderMap() {
        val data = StringNonValueIntHolderMap(mapOf("abc" to NonValueIntHolder(123), "def" to NonValueIntHolder(456)))
        val expected = "<StringNonValueIntHolderMap><NonValueIntHolder key=\"abc\" data=\"123\"/><NonValueIntHolder key=\"def\" data=\"456\"/></StringNonValueIntHolderMap>"
        assertXmlEquals(expected, xml.encodeToString(data))
    }

    @Test
    fun testDeserializeStringNonValueIntHolderMap() {
        val expected = StringNonValueIntHolderMap(mapOf("abc" to NonValueIntHolder(123), "def" to NonValueIntHolder(456)))
        val data = "<StringNonValueIntHolderMap><NonValueIntHolder key=\"abc\" data=\"123\"/><NonValueIntHolder key=\"def\" data=\"456\"/></StringNonValueIntHolderMap>"
        assertEquals(expected, xml.decodeFromString(data))
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
    data class MyClass(val map: Map<String, String>)

    @Serializable
    @JvmInline
    value class IntHolder(val data: Int)

    @Serializable
    data class StringIntHolderMap(val map: Map<String, IntHolder>)

    @Serializable
    data class NonValueIntHolder(val data: Int)

    @Serializable
    data class StringNonValueIntHolderMap(val map: Map<String, NonValueIntHolder>)
}
