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

@file:MustUseReturnValues

package nl.adaptivity.xml.serialization.regressions

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import nl.adaptivity.xml.serialization.pedantic
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.util.CompactFragment
import kotlin.test.Test
import kotlin.test.assertEquals

class EmptyTagWithValueChild {

    val xml = XML.v1.pedantic()

    @Test
    fun testSerializeStr() {
        val actual = xml.encodeToString(OuterStr(InnerStr("")))
        assertXmlEquals("<Outer><Inner /></Outer>", actual)
    }


    @Test
    fun testSerializeCF() {
        val actual = xml.encodeToString(OuterFrag(InnerFrag(CompactFragment(""))))
        assertXmlEquals("<Outer><Inner /></Outer>", actual)
    }

    @Test
    fun testDeserializeCF() {
        val expected = OuterFrag(InnerFrag(CompactFragment("")))
        val actual = xml.decodeFromString<OuterFrag>("<Outer><Inner /></Outer>")
        assertEquals(expected, actual)
    }

    @Test
    fun testSerializeCFlist() {
        val actual = xml.encodeToString(OuterFrags(InnerFrags(emptyList())))
        assertXmlEquals("<Outer><Inner /></Outer>", actual)
    }

    @Test
    fun testDeserializeCFlist() {
        val expected = OuterFrags(InnerFrags(emptyList()))
        val actual = xml.decodeFromString<OuterFrags>("<Outer><Inner /></Outer>")
        assertEquals(expected, actual)
    }

    @Test
    fun testDeserializeStr() {
        val expected = OuterStr(InnerStr(""))
        val actual = xml.decodeFromString<OuterStr>("<Outer><Inner /></Outer>")
        assertEquals(expected, actual)
    }


    @Serializable
    @XmlSerialName("Outer")
    private data class OuterStr(val inner: InnerStr)

    @Serializable
    @XmlSerialName("Inner")
    private data class InnerStr(@XmlValue val value: String)

    @Serializable
    @XmlSerialName("Outer")
    private data class OuterFrags(val inner: InnerFrags)

    @Serializable
    @XmlSerialName("Inner")
    private data class InnerFrags(@XmlValue val values: List<CompactFragment>)

    @Serializable
    @XmlSerialName("Inner")
    private data class InnerFrag(@XmlValue val value: CompactFragment)

    @Serializable
    @XmlSerialName("Outer")
    private data class OuterFrag(val inner: InnerFrag)
}
