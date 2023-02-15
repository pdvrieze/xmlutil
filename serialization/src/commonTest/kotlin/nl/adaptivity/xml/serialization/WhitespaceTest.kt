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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.serialization.*
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test various forms of whitespace handling. In relation to #120
 */
class WhitespaceTest : PlatformTestBase<WhitespaceTest.WhitespaceContainers>(
    WhitespaceContainers(
        WhitespaceContainer("A "),
        WhitespaceContainer(" "),
        WhitespaceContainer("a b"),
    ),
    WhitespaceContainers.serializer()
) {
    override val expectedXML: String =
        "<WhitespaceContainers><WhitespaceContainer>A </WhitespaceContainer><WhitespaceContainer> </WhitespaceContainer><WhitespaceContainer>a b</WhitespaceContainer></WhitespaceContainers>"
    override val expectedJson: String =
        "{\"elements\":[{\"text\":\"A \"},{\"text\":\" \"},{\"text\":\"a b\"}]}"

    @Test
    fun testDeserializeWithPreserve() {
        val actual: TextContainer = baseXmlFormat.decodeFromString("<TextContainer xml:space=\"preserve\">  x</TextContainer>")
        assertEquals(TextContainer("  x"), actual)
    }

    @Test
    fun testEmitSpacePreserve() {
        val encoded = baseXmlFormat.encodeToString(TextContainer("  x"))
        assertXmlEquals("<TextContainer xml:space=\"preserve\">  x</TextContainer>", encoded)
        assertContains(encoded, "xml:space")
    }

    @Test
    fun testIssue120Example2() {
        val data = A(listOf(
            B(Text("Cc. ")),
            B(Text("     ")),
            B(Text("  hello"))
            ))
        val actual = baseXmlFormat.encodeToString(data)
        assertEquals("<A><B><text>Cc. </text></B><B><text>     </text></B><B><text>  hello</text></B></A>", actual)
    }

    @Serializable
    data class WhitespaceContainers(val elements: List<WhitespaceContainer>) {
        constructor(vararg elements: WhitespaceContainer): this(listOf(*elements))
    }

    @Serializable
    data class WhitespaceContainer(
        @XmlValue(true)
        @XmlIgnoreWhitespace(false)
        val text: String
    )

    @Serializable
    data class TextContainer(
        @XmlValue(true)
        @XmlIgnoreWhitespace(true)
        val text: String
    )

    @Serializable
    data class A(
        @SerialName("B")
        val b: List<B>
    )
    @Serializable
    data class B(
        @XmlElement(true)
        @XmlSerialName("text", "", "")
        val text: Text
    ){

    }
    @Serializable
    data class Text(
        @XmlValue(true)
        @XmlIgnoreWhitespace(false)
        var value:String,
    )

}
