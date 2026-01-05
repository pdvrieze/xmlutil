/*
 * Copyright (c) 2025-2026.
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
import nl.adaptivity.xmlutil.serialization.DefaultFormatCache
import nl.adaptivity.xmlutil.serialization.TestFormatCache
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlValue
import kotlin.jvm.JvmInline
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for #280 where a value class is polymorphically serialized (containing a string).
 */
class PolymorphicValue280 {

    val xml = XML.v1.pedantic {
        policy {
            formatCache = TestFormatCache(DefaultFormatCache())
        }
    }

    @Test
    fun testSerializeNontextPolymorphic() {
        val expected = "<OuterNontext><WrappedInt>1</WrappedInt><Inner>bar</Inner><WrappedInt>3</WrappedInt></OuterNontext>"
        val data = OuterNontext(listOf(
            NonText(WrappedInt(1)),
            Inner("bar"),
            NonText(WrappedInt(3)),
        ))

        assertXmlEquals(expected, xml.encodeToString(data))
    }

    @Test
    fun testDeSerializeNontextPolymorphic() {
        val data = "<OuterNontext><WrappedInt>1</WrappedInt><Inner>bar</Inner><WrappedInt>3</WrappedInt></OuterNontext>"
        val expected = OuterNontext(listOf(
            NonText(WrappedInt(1)),
            Inner("bar"),
            NonText(WrappedInt(3)),
        ))

        assertEquals(expected, xml.decodeFromString<OuterNontext>(data))
    }

    @Test
    fun testSerializeTextPolymorphic() {
        val expected = "<Outer>1<Inner>bar</Inner>3</Outer>"
        val data = Outer(listOf(
            Text("1"),
            Inner("bar"),
            Text("3"),
        ))

        assertXmlEquals(expected, xml.encodeToString<Outer>(data))
    }

    @Test
    fun testDeserializeTextPolymorphic() {
        val data = "<Outer>1<Inner>bar</Inner>3</Outer>"
        val expected = Outer(listOf(
            Text("1"),
            Inner("bar"),
            Text("3"),
        ))

        assertEquals(expected, xml.decodeFromString<Outer>(data))
    }

    @Test
    fun testSerializeTextPolymorphicNonList() {
        val expected = "<OuterNonList>bar</OuterNonList>"
        val data = OuterNonList(
            Text("bar"),
        )

        assertXmlEquals(expected, xml.encodeToString(data))
    }

    @Test
    fun testDeserializeTextPolymorphicNonList() {
        val data = "<OuterNonList>bar</OuterNonList>"
        val expected = OuterNonList(Text("bar"))

        assertEquals(expected, xml.decodeFromString<OuterNonList>(data))
    }

    @Serializable
    data class Outer(
        @XmlValue
        val content: List<Content>
    )

    @Serializable
    data class OuterNonList(
        @XmlValue
        val content: Content
    )

    @Serializable
    data class OuterNonValue(
        val content: Content
    )

    @Serializable
    data class OuterNontext(
        @XmlValue
        val content: List<NontextContent>
    )

    @Serializable
    sealed interface Content;

    @Serializable
    sealed interface NontextContent;

    @Serializable
    @JvmInline
    value class Text(val textContent: String): Content

    @JvmInline
    @Serializable
    value class NonText(val intContent: WrappedInt): NontextContent

    @Serializable
    data class WrappedInt(@XmlValue val number: Int)

    @Serializable
    data class Inner(
        @XmlValue
        val innerContent: String
    ): Content, NontextContent
}
