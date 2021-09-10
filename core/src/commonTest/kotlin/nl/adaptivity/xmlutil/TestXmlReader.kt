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

package nl.adaptivity.xmlutil

import kotlin.test.Test
import kotlin.test.assertEquals

class TestXmlReader {

    @Test
    fun testNamespaceDecls() {
        val reader = XmlStreaming.newReader(
            """
            <root xmlns="foo"><ns2:bar xmlns:ns2="bar"/></root>
        """.trimIndent()
        )

        run {
            var event = reader.next()
            if (event == EventType.START_DOCUMENT) event = reader.next()
            assertEquals(EventType.START_ELEMENT, event)

        }
        assertEquals(listOf(XmlEvent.NamespaceImpl("", "foo")), reader.namespaceDecls)

        assertEquals(EventType.START_ELEMENT, reader.next())
        assertEquals(listOf(XmlEvent.NamespaceImpl("ns2", "bar")), reader.namespaceDecls)

        assertEquals(EventType.END_ELEMENT, reader.next())
        assertEquals(EventType.END_ELEMENT, reader.next())
    }

    @Test
    fun testReadCompactFragment() {
        val inner = """
            |  <sub1>
            |        <sub2>tala
            |  </sub2>  </sub1>
            |""".trimMargin()
        val xml = "<root xmlns=\"foobar\">$inner</root>"

        val input = XmlStreaming.newReader(xml)
        input.nextTag()
        input.require(EventType.START_ELEMENT, "foobar", "root")
        input.next()
        val frag = input.siblingsToFragment()
        assertEquals(inner, frag.contentString)
        assertEquals(listOf(XmlEvent.NamespaceImpl("", "foobar")), frag.namespaces.toList())
    }


    @Test
    fun testReadCompactFragmentWithNamespaceInOuter() {
        val inner = """
            |  <sub1>
            |        <sub2>tala
            |           <sub3 xmlns="baz"/>
            |  </sub2>  </sub1>
            |""".trimMargin()
        val xml = "<f:root xmlns:f=\"foobar\">$inner</f:root>"

        val input = XmlStreaming.newReader(xml)
        input.nextTag()
        input.require(EventType.START_ELEMENT, "foobar", "root")
        input.next()
        val frag = input.siblingsToFragment()
        assertEquals(inner, frag.contentString.replace(" />", "/>"))
        assertEquals(emptyList(), frag.namespaces.toList())
    }
}
