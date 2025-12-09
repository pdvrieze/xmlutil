/*
 * Copyright (c) 2021-2025.
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

package nl.adaptivity.xml.serialization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import kotlin.test.*

class SequenceTest {
    val values = listOf(
        SimpleList("1", "2", "3"),
        SimpleList("4", "5", "6"),
        SimpleList("7", "8", "9"),
    )

    fun getFormat() = XML { recommended_0_91_0() }

    val expectedXML: String = "<w>  " +
            "<l2><value>1</value>\n<value>2</value><value>3</value></l2>" +
            "<l2><value>4</value><value>5</value><value>6</value></l2>\n" +
            "<l2><value>7</value><value>8</value><value>9</value></l2>  " +
            "</w>"

    val incorrectXML: String = "<w>  " +
            "<l2><value>1</value>\n<value>2</value><value>3</value></l2>" +
            "<l><value>4</value><value>5</value><value>6</value></l>\n" +
            "<l2><value>7</value><value>8</value><value>9</value></l2>  " +
            "</w>"

    @Serializable
    @SerialName("l")
    data class SimpleList(@XmlSerialName("value", "", "") val values: List<String>) {
        constructor(vararg values: String) : this(values.toList())
    }

    @Serializable
    sealed class Elem

    @Serializable
    @SerialName("a")
    data class A(val value: String) : Elem()

    @Serializable
    @SerialName("b")
    data class B(val value: Int) : Elem()

    @Test
    fun testDecodePolymorphic() {
        val data = """
            <w>
              <a value="foo"/>
              <b value="42"/>
            </w>
        """.trimIndent()

        val seq = getFormat().decodeWrappedToSequence<Elem>(xmlStreaming.newGenericReader(data))
        val it = seq.iterator()
        assertEquals(A("foo"), it.next())
        assertEquals(B(42), it.next())
        assertFalse(it.hasNext())
    }

    @Test
    fun testDecodeWithAttributes() {
        val data = """
            <w attr="unexpected">
              <a value="foo"/>
              <b value="42"/>
            </w>
        """.trimIndent()

        val e = assertFailsWith<XmlException> {
            getFormat().decodeWrappedToSequence<Elem>(xmlStreaming.newGenericReader(data)).toList()
        }
        assertContains(e.message!!, "Unexpected attribute in wrapper: attr=\"unexpected\"")
    }

    @Test
    fun testDecodeRegular() {
        val xml = getFormat()

        val list = xml.decodeFromString<List<SimpleList>>(expectedXML, QName("w"))
        assertExpected(list.iterator())
    }

    @Test
    fun testDecodeWrappedAuto() {
        val xml = getFormat()

        val reader = xmlStreaming.newGenericReader(expectedXML)

        val seq = xml.decodeWrappedToSequence<SimpleList>(reader)
        assertExpected(seq.iterator())
    }

    @Test
    fun testDecodeWrappedAutoNamedElement() {
        val xml = getFormat()

        val reader = xmlStreaming.newGenericReader(expectedXML)

        val seq = xml.decodeWrappedToSequence<SimpleList>(reader, QName("l2"))
        assertExpected(seq.iterator())
    }

    @Test
    fun testDecodeWrappedAutoInconsistentElement() {
        val xml = getFormat()

        val reader = xmlStreaming.newGenericReader(incorrectXML)

        // note that toList is needed to consume the sequence.
        val e = assertFailsWith<XmlException> { xml.decodeWrappedToSequence<SimpleList>(reader).toList() }
        assertContains(e.message!!, "Local name \"l\" for element tag does not match expected name \"l2\"")
    }

    @Test
    fun testDecodeWrappedAutoMisnamedElementFails() {
        val xml = getFormat()

        val reader = xmlStreaming.newGenericReader(expectedXML)

        val e = assertFailsWith<XmlException> {
            xml.decodeWrappedToSequence<SimpleList>(reader, QName("l")).toList()
        }
        assertContains(e.message!!, "name \"l2\" for element tag does not match expected name \"l\"")
    }

    @Test
    fun testDecodeWrappedNamed() {
        val xml = getFormat()

        val reader = xmlStreaming.newGenericReader(expectedXML)

        val seq = xml.decodeToSequence<SimpleList>(reader, QName("w"))

        assertExpected(seq.iterator())
    }

    @Test
    fun testDecodeUnwrappedAuto() {
        val xml = getFormat()

        val reader = xmlStreaming.newGenericReader(expectedXML)
        reader.skipPreamble()
        reader.require(EventType.START_ELEMENT, QName("w"))
        val _ = reader.next()

        val seq = xml.decodeToSequence<SimpleList>(reader, null)
        assertExpected(seq.iterator())
    }

    @Test
    fun testDecodeUnwrappedNamed() {
        val xml = getFormat()

        val reader = xmlStreaming.newGenericReader(expectedXML)
        reader.skipPreamble()
        reader.require(EventType.START_ELEMENT, QName("w"))
        val _ = reader.next()

        val seq = xml.decodeToSequence<SimpleList>(reader, null)

        assertExpected(seq.iterator())
    }

    private fun assertExpected(it: Iterator<SimpleList>) {
        assertEquals(values[0], it.next())
        assertEquals(values[1], it.next())
        assertEquals(values[2], it.next())
        assertFalse(it.hasNext(), "Expected end of sequence")
    }

    /*
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
    */

}
