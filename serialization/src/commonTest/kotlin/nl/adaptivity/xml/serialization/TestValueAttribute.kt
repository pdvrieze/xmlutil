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

package nl.adaptivity.xml.serialization

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.serialization.XmlValue
import kotlin.jvm.JvmInline
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test that checks whether a primitive value attribute can be written as attribute.
 */
class TestValueAttribute {

    val xml = defaultXmlFormat()

    @Test
    fun testSerializeContainer() {
        val data = Container(StringAttr("foo"), IntAttr(42), NestedValueAttr(StringAttr("bar")), StructHolder(Tuple("aa", "bb")))
        val expected = "<Container str=\"foo\" int=\"42\" nested=\"bar\">" +
                "<Tuple a=\"aa\" b=\"bb\"/>" +
                "</Container>"

        val actual = xml.encodeToString(data)
        assertXmlEquals(expected, actual)
    }

    @Test
    fun testDeserializeContainer() {
        val data = "<Container str=\"foo\" int=\"42\" nested=\"bar\">" +
                "<Tuple a=\"aa\" b=\"bb\"/>" +
                "</Container>"
        val expected = Container(StringAttr("foo"), IntAttr(42), NestedValueAttr(StringAttr("bar")), StructHolder(Tuple("aa", "bb")))

        assertEquals(expected, xml.decodeFromString<Container>(data))
    }

    @Test
    fun testSerializeNested() {
        val data = Wrapper(NestedValueAttr(StringAttr("foo")))
        val expected = "<Wrapper value=\"foo\"/>"
        assertXmlEquals(expected, xml.encodeToString(data))
    }

    @Test
    fun testDeserializeNested() {
        val expected = Wrapper(NestedValueAttr(StringAttr("foo")))
        val data = "<Wrapper value=\"foo\"/>"
        assertEquals(expected, xml.decodeFromString(data))
    }

    @Test
    fun testSerializeNestedValue() {
        val data = ValueWrapper(NestedValueAttr(StringAttr("foo")))
        val expected = "<ValueWrapper>foo</ValueWrapper>"
        assertXmlEquals(expected, xml.encodeToString(data))
    }

    @Test
    fun testDeserializeNestedValue() {
        val expected = ValueWrapper(NestedValueAttr(StringAttr("foo")))
        val data = "<ValueWrapper>foo</ValueWrapper>"
        assertEquals(expected, xml.decodeFromString(data))
    }

    @Test
    fun testSerializeStruct() {
        val data = Wrapper(StructHolder(Tuple("foo", "bar")))
        val expected = "<Wrapper><Tuple a=\"foo\" b=\"bar\"/></Wrapper>"
        assertXmlEquals(expected, xml.encodeToString(data))
    }

    @Test
    fun testDeserializeStruct() {
        val expected = Wrapper(StructHolder(Tuple("foo", "bar")))
        val data = "<Wrapper><Tuple a=\"foo\" b=\"bar\"/></Wrapper>"
        assertEquals(expected, xml.decodeFromString(data))
    }

    @Test
    fun testSerializeString() {
        val data = Wrapper(StringAttr("foo"))
        val expected = "<Wrapper value=\"foo\"/>"
        assertXmlEquals(expected, xml.encodeToString(data))
    }

    @Test
    fun testDeserializeString() {
        val expected = Wrapper(StringAttr("foo"))
        val data = "<Wrapper value=\"foo\"/>"
        assertEquals(expected, xml.decodeFromString(data))
    }

    @Test
    fun testSerializeStringValue() {
        val data = ValueWrapper(StringAttr("foo"))
        val expected = "<ValueWrapper>foo</ValueWrapper>"
        assertXmlEquals(expected, xml.encodeToString(data))
    }

    @Test
    fun testDeserializeStringValue() {
        val expected = ValueWrapper(StringAttr("foo"))
        val data = "<ValueWrapper>foo</ValueWrapper>"
        assertEquals(expected, xml.decodeFromString(data))
    }

    @Test
    fun testSerializeInt() {
        val data = Wrapper(IntAttr(42))
        val expected = "<Wrapper value=\"42\"/>"
        assertXmlEquals(expected, xml.encodeToString(data))
    }

    @Test
    fun testDeserializeInt() {
        val expected = Wrapper(IntAttr(42))
        val data = "<Wrapper value=\"42\"/>"
        assertEquals(expected, xml.decodeFromString(data))
    }

    @Test
    fun testSerializeIntValue() {
        val data = ValueWrapper(IntAttr(42))
        val expected = "<ValueWrapper>42</ValueWrapper>"
        assertXmlEquals(expected, xml.encodeToString(data))
    }

    @Test
    fun testDeserializeIntValue() {
        val expected = ValueWrapper(IntAttr(42))
        val data = "<ValueWrapper>42</ValueWrapper>"
        assertEquals(expected, xml.decodeFromString(data))
    }

    @Serializable
    data class Wrapper<T>(val value: T)

    @Serializable
    data class ValueWrapper<T>(@XmlValue val value: T)

    @Serializable
    data class Container(
        val str: StringAttr,
        val int: IntAttr,
        val nested: NestedValueAttr,
        val struct: StructHolder
    )

    @JvmInline
    @Serializable
    value class StringAttr(val value: String)

    @JvmInline
    @Serializable
    value class NestedValueAttr(val value: StringAttr)

    @JvmInline
    @Serializable
    value class IntAttr(val value: Int)

    @JvmInline
    @Serializable
    value class StructHolder(val value: Tuple)

    @Serializable
    data class Tuple(val a: String, val b: String)
}
