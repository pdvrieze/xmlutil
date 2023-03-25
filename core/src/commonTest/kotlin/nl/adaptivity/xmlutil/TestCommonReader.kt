/*
 * Copyright (c) 2023.
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

import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.core.impl.multiplatform.use
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

abstract class TestCommonReader {
    protected fun testReadCompactFragmentWithNamespaceInOuter(createReader: (String) -> XmlReader) {
        val inner = """
                |  <sub1>
                |        <sub2>tala
                |           <sub3 xmlns="baz"/>
                |  </sub2>  </sub1>
                |""".trimMargin()
        val xml = "<f:root xmlns:f=\"foobar\">$inner</f:root>"

        val input = createReader(xml)
        input.nextTag()
        input.require(EventType.START_ELEMENT, "foobar", "root")
        input.next()
        val frag = input.siblingsToFragment()
        assertEquals(inner, frag.contentString.replace(" />", "/>"))
        assertEquals(emptyList(), frag.namespaces.toList())
    }

    protected fun testNamespaceDecls(createReader: (String) -> XmlReader) {
        val reader = createReader(
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

    protected fun testReadCompactFragment(createReader: (String) -> XmlReader) {
        val inner = """
                |  <sub1>
                |        <sub2>tala
                |  </sub2>  </sub1>
                |""".trimMargin()
        val xml = "<root xmlns=\"foobar\">$inner</root>"

        val input = createReader(xml)
        input.nextTag()
        input.require(EventType.START_ELEMENT, "foobar", "root")
        input.next()
        val frag = input.siblingsToFragment()
        assertEquals(inner, frag.contentString)
        assertEquals(listOf(XmlEvent.NamespaceImpl("", "foobar")), frag.namespaces.toList())
    }

    protected fun testReadSingleTag(createReader: (String) -> XmlReader) {
        val xml = "<MixedAttributeContainer xmlns:a=\"a\" xmlns:e=\"c\" attr1=\"value1\" a:b=\"dyn1\" e:d=\"dyn2\" attr2=\"value2\"/>"

        createReader(xml).use { reader ->
            assertTrue(reader.hasNext())
            assertEquals(EventType.START_DOCUMENT, reader.next())

            assertTrue(reader.hasNext())
            assertEquals(EventType.START_ELEMENT, reader.next())
            assertEquals(QName("MixedAttributeContainer"), reader.name)
            assertEquals(4, reader.attributeCount)

            assertTrue(reader.hasNext())
            assertEquals(EventType.END_ELEMENT, reader.next())

            assertTrue(reader.hasNext())
            assertEquals(
                EventType.END_DOCUMENT,
                reader.next(),
                "Expected end of document, location: ${reader.locationInfo}"
            )

            assertFalse(reader.hasNext())

        }
    }

    protected fun testReadEntity(createReader: (String) -> XmlReader) {
        val xml = """<tag>&lt;foo&amp;&#039;&gt;</tag>"""
        createReader(xml).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertEquals(QName("tag"), reader.name)

            val actualText = StringBuilder()
            while (reader.next().isTextElement) {
                actualText.append(reader.text)
            }

            assertEquals("<foo&'>", actualText.toString())

            assertEquals(EventType.END_ELEMENT, reader.eventType)
            assertEquals(QName("tag"), reader.name)
        }
    }

    protected fun testReadUnknownEntity(createReader: (String) -> XmlReader) {
        val xml = """<tag>&unknown;</tag>"""
        createReader(xml).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertEquals(QName("tag"), reader.name)

            assertEquals(EventType.ENTITY_REF, reader.next())
            assertEquals("unknown", reader.localName)

            assertEquals(EventType.END_ELEMENT, reader.next())
            assertEquals(QName("tag"), reader.name)
        }
    }

}
