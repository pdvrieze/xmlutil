/*
 * Copyright (c) 2024-2026.
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

@file:Suppress("UnusedVariable", "unused")
@file:MustUseReturnValues

package nl.adaptivity.xml.serialization.regressions

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import nl.adaptivity.xml.serialization.pedantic
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.serialization.structure.XmlCompositeDescriptor
import nl.adaptivity.xmlutil.serialization.structure.XmlListDescriptor
import nl.adaptivity.xmlutil.serialization.structure.XmlPolymorphicDescriptor
import nl.adaptivity.xmlutil.serialization.structure.XmlPrimitiveDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TestCachedDescriptorCache {

    @Test
    fun testParameterisedSerializerCache() {
        val format = XML.v1.pedantic()

        val serialized1 = format.encodeToString(Outer(Inner1(1, 2)))
        assertXmlEquals("<Outer><Inner1 data1=\"1\" data2=\"2\"/></Outer>", serialized1)

        val serialized2 = format.encodeToString(Outer(Inner2("a", "b", "c")))
        assertXmlEquals("<Outer><Inner2 data3=\"a\" data4=\"b\" data5=\"c\"/></Outer>", serialized2)
    }

    @Test
    fun testCacheXmlValues() {
        val format = XML.v1.recommended(listModule) {
            policy {
                formatCache = DefaultFormatCache() // skip the layering for debugging
            }
        }

        val desc1 =
            assertIs<XmlCompositeDescriptor>(format.xmlDescriptor(OuterList1.serializer()).getElementDescriptor(0))
        val desc2 =
            assertIs<XmlCompositeDescriptor>(format.xmlDescriptor(OuterList2.serializer()).getElementDescriptor(0))


        val deserialized1 =
            format.decodeFromString<OuterList1>("<OuterList1><string xmlns=\"http://www.w3.org/2001/XMLSchema\">text</string></OuterList1>")

        assertEquals(1, desc1.elementsCount)
        val desc1list = assertIs<XmlListDescriptor>(desc1.getElementDescriptor(0))
        val desc1data = assertIs<XmlPolymorphicDescriptor>(desc1list.getElementDescriptor(0))
        assertEquals(2, desc1data.polyInfo.size)
        val desc1string = assertIs<XmlPrimitiveDescriptor>(desc1data.polyInfo["kotlin.String"])
        val desc1inner =
            assertIs<XmlCompositeDescriptor>(desc1data.polyInfo["nl.adaptivity.xml.serialization.regressions.TestCachedDescriptorCache.Inner1"])
        assertEquals(OutputKind.Element, desc1string.outputKind)

        assertEquals(listOf("text"), deserialized1.data)

        assertEquals(1, desc2.elementsCount)
        val desc2list = assertIs<XmlListDescriptor>(desc2.getElementDescriptor(0))
        val desc2data = assertIs<XmlPolymorphicDescriptor>(desc2list.getElementDescriptor(0))
        assertEquals(2, desc2data.polyInfo.size)
        val desc2string = assertIs<XmlPrimitiveDescriptor>(desc2data.polyInfo["kotlin.String"])
        val desc2inner =
            assertIs<XmlCompositeDescriptor>(desc2data.polyInfo["nl.adaptivity.xml.serialization.regressions.TestCachedDescriptorCache.Inner1"])
        assertEquals(OutputKind.Mixed, desc2string.outputKind)

        val deserialized2 = format.decodeFromString<OuterList2>("<OuterList>text</OuterList>")
        assertEquals(listOf("text"), deserialized2.data)
    }

    @Serializable
    data class Outer<T>(val data: T)

    @Serializable
    data class OuterList1(val data: List<@Polymorphic Any>)

    @Serializable
    data class OuterList2(@XmlValue val data: List<@Polymorphic Any>)

    @Serializable
    data class Inner1(val data1: Int, val data2: Int)

    @Serializable
    data class Inner2(val data3: String, val data4: String, val data5: String)

    companion object {
        internal val listModule = SerializersModule {
            polymorphic(Any::class) {
                subclass(Inner1::class)
                subclass(String::class)
            }
        }
    }
}
