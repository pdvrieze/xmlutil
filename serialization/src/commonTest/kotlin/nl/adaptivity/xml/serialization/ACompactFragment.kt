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

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.XmlEvent
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.util.CompactFragment
import kotlin.test.Test
import kotlin.test.assertEquals

class ACompactFragment : PlatformTestBase<CompactFragment>(
    CompactFragment(listOf(XmlEvent.NamespaceImpl("p", "urn:ns")), "<p:a>someA</p:a><b>someB</b>"),
    CompactFragment.serializer()
) {
    override val expectedXML: String =
        "<compactFragment xmlns:p=\"urn:ns\"><p:a>someA</p:a><b>someB</b></compactFragment>"
    override val expectedJson: String =
        "{\"namespaces\":[{\"prefix\":\"p\",\"namespaceURI\":\"urn:ns\"}],\"content\":\"<p:a>someA</p:a><b>someB</b>\"}"

    @Test
    fun testSerializeValueFragment269() {
        val expected = "<root attribute=\"value\"><child>Hello</child><child>World!</child></root>"
        val data = Root("value", CompactFragment("<child>Hello</child><child>World!</child>"))
        val actual = baseXmlFormat.copy { setIndent(0) }.encodeToString(data)
        assertEquals(expected, actual)
    }

    @Test
    fun testDeserializeValueFragment269() {
        val data = "<root attribute=\"value\"><child>Hello</child><child>World!</child></root>"
        val expected = Root("value", CompactFragment("<child>Hello</child><child>World!</child>"))
        val actual = baseXmlFormat.decodeFromString<Root>(data)
        assertEquals(expected, actual)
    }

    @Serializable
    @XmlSerialName("root")
    data class Root(
        val attribute: String,
        @XmlValue val tagSoup: CompactFragment?
    )
}
