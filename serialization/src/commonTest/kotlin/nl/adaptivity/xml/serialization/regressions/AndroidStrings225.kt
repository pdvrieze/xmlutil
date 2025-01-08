/*
 * Copyright (c) 2024.
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

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.dom.Node as Node1
import nl.adaptivity.xmlutil.dom2.Element
import nl.adaptivity.xmlutil.dom2.Node as Node2
import nl.adaptivity.xmlutil.dom2.textContent
import nl.adaptivity.xmlutil.serialization.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AndroidStrings225 {

    val xml = XML(
        SerializersModule {
            polymorphic(Any::class, String::class, String.serializer())
            polymorphic(Any::class, Element::class, Element.serializer())
        }
    ) {
        recommended_0_90_2()
    }

    @Test
    fun testDecodeAny() {
        val data = """
                <?xml version="1.0" encoding="utf-8"?>
                <resources xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2">
                    <string name="string_android">Test with argument <xliff:g id="argument">%1s</xliff:g> here</string>
                    <string name="test_string_2">test 2</string>
                </resources>""".trimIndent()

        val parsed = xml.decodeFromString(Resources.serializer(), data)

        assertEquals(2, parsed.strings.size)
        val p1 = parsed.strings[0]
        assertEquals("string_android", p1.name)
        assertEquals(3, p1.node.size)
        assertEquals("Test with argument ", p1.node[0])
        assertIs<Element>(p1.node[1])
        assertEquals(" here", p1.node[2])

        val p2 = parsed.strings[1]
        assertEquals("test_string_2", p2.name)
        assertEquals(listOf("test 2"), p2.node)

    }

    @Test
    fun testDecodeAny2() {
        val data = """
                <?xml version="1.0" encoding="utf-8"?>
                <resources xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2">
                    <string name="string_android">Test with argument <xliff:g id="argument">%1s</xliff:g> here</string>
                    <string name="test_string_2">test 2</string>
                </resources>""".trimIndent()

        val parsed = xml.decodeFromString(Resources2.serializer(), data)

        assertEquals(2, parsed.strings.size)
        val p1 = parsed.strings[0]
        assertEquals("string_android", p1.name)
        assertEquals(3, p1.node.size)
        assertEquals("Test with argument ", p1.node[0].textContent)
        assertIs<Element>(p1.node[1])
        assertEquals(" here", p1.node[2].textContent)

        val p2 = parsed.strings[1]
        assertEquals("test_string_2", p2.name)
        assertEquals(listOf("test 2"), p2.node.map { it.textContent })

    }

    @Test
    fun testDecodeAny3() {
        val data = """
                <?xml version="1.0" encoding="utf-8"?>
                <resources xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2">
                    <string name="string_android">Test with argument <xliff:g id="argument">%1s</xliff:g> here</string>
                    <string name="test_string_2">test 2</string>
                </resources>""".trimIndent()

        val parsed = xml.decodeFromString(Resources3.serializer(), data)

        assertEquals(2, parsed.strings.size)
        val p1 = parsed.strings[0]
        assertEquals("string_android", p1.name)
        assertEquals(3, p1.node.size)
        assertEquals("Test with argument ", p1.node[0].textContent)
        assertIs<Element>(p1.node[1])
        assertEquals(" here", p1.node[2].textContent)

        val p2 = parsed.strings[1]
        assertEquals("test_string_2", p2.name)
        assertEquals(listOf("test 2"), p2.node.map { it.textContent })

    }


    @Serializable
    @XmlSerialName("string")
    data class StringTag(
        @XmlElement(false)
        val name: String,
        @XmlValue
        val node: List<@Polymorphic Any>,
    )

    @Serializable
    @XmlSerialName("string")
    data class StringTag2(
        @XmlElement(false)
        val name: String,
        @Suppress("DEPRECATION")
        @XmlValue
        val node: List<@Serializable(NodeSerializer::class) Node1>,
    )

    @Serializable
    @XmlSerialName("string")
    data class StringTag3(
        @XmlElement(false)
        val name: String,
        @XmlValue
        val node: List<Node2>,
    )

    @Serializable
    @XmlSerialName("resources")
    data class Resources(
        val strings: List<StringTag>,
    )

    @Serializable
    @XmlSerialName("resources")
    data class Resources2(
        val strings: List<StringTag2>,
    )

    @Serializable
    @XmlSerialName("resources")
    data class Resources3(
        val strings: List<StringTag3>,
    )
}
