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

package nl.adaptivity.xmlutil.test

import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.core.impl.multiplatform.StringReader
import nl.adaptivity.xmlutil.core.impl.multiplatform.use
import kotlin.test.*

class TestKtXmlReaderExpandEntities : TestCommonReader() {

    override fun createReader(xml: String): XmlReader {
        return xmlStreaming.newGenericReader(xml, expandEntities = true).also {
            assertTrue(assertIs<KtXmlReader>(it).expandEntities, "Expand entities not set")
        }
    }

    @Test
    fun testReadEntityInAttributeExpand() {
        testReadEntityInAttribute(true)
    }

    @Test
    fun testReadEntityInAttributeNoExpand() {
        testReadEntityInAttribute(false)
    }

    private fun testReadEntityInAttribute(expandEntities: Boolean) {
        val data = "<tag attr=\"&lt;xx&gt;\"/>"
        val reader = KtXmlReader(StringReader(data), expandEntities)
        var e = reader.next()
        if (e == EventType.START_DOCUMENT) e = reader.next()
        assertEquals(EventType.START_ELEMENT, e)
        assertEquals("tag", reader.localName)
        assertEquals(1, reader.attributeCount)
        assertEquals("attr", reader.getAttributeLocalName(0))
        assertEquals("<xx>", reader.getAttributeValue(0))

        assertEquals(EventType.END_ELEMENT, reader.next())
    }

    @Test
    override fun testReadEntity() {
        val xml = """<tag>&lt;foo&amp;&#039;&gt;</tag>"""
        createReader(xml).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertEquals(QName("tag"), reader.name)

            assertEquals(EventType.TEXT, reader.next())
            assertEquals("<foo&'>", reader.text)

            assertEquals(EventType.END_ELEMENT, reader.next())
            assertEquals(QName("tag"), reader.name)
        }
    }

    @Test
    fun testSection4_5example() {
        val xml = """
            |<!DOCTYPE foo [
            |<!ENTITY % pub    "&#xc9;ditions Gallimard" >
            |<!ENTITY   rights "All rights reserved" >
            |<!ENTITY   book   "La Peste: Albert Camus,
            |&#xA9; 1947 %pub;. &rights;" >
            |]>
            |<root>&book;</root>
        """.trimMargin()
        val reader = KtXmlReader(StringReader(xml), expandEntities = true)

        assertEquals(EventType.START_ELEMENT, reader.nextTag())
        assertEquals(QName("root"), reader.name)

        val entityReplacementValue = reader.entityMap.get("book")
        assertEquals(
            "La Peste: Albert Camus,\n© 1947 Éditions Gallimard. &rights;",
            entityReplacementValue?.replacementValue
        )

        assertEquals(EventType.TEXT, reader.next())
        assertEquals(
            "La Peste: Albert Camus,\n© 1947 Éditions Gallimard. All rights reserved",
            reader.text
        )
    }

    @Test
    fun testAppendixC_example1() {
        val xml = """
            |<!DOCTYPE foo [
            |<!ENTITY example "<p>An ampersand (&#38;#38;) may be escaped
            |numerically (&#38;#38;#38;) or with a general entity
            |(&amp;amp;).</p>" >
            |]>
            |<root>&example;<root>
        """.trimMargin()
        val reader = KtXmlReader(StringReader(xml), expandEntities = true)

        assertEquals(EventType.START_ELEMENT, reader.nextTag())
        assertEquals(QName("root"), reader.name)

        assertEquals(EventType.START_ELEMENT, reader.nextTag())
        assertEquals(QName("p"), reader.name)

        assertEquals(EventType.TEXT, reader.next())
        assertEquals(
            "An ampersand (&) may be escaped\n" +
                    "numerically (&#38;) or with a general entity\n" +
                    "(&amp;).",
            reader.text
        )
    }

    @Test
    fun testAppendixC_example2() {
        val xml = """
            |<?xml version='1.1'?>
            |<!DOCTYPE test [
            |<!ELEMENT test (#PCDATA) >
            |<!ENTITY % xx '&#37;zz;'>
            |<!ENTITY % zz '&#60;!ENTITY tricky "error-prone" >' >
            |%xx;
            |]>
            |<test>This sample shows a &tricky; method.</test>
        """.trimMargin()
        val reader = KtXmlReader(StringReader(xml), expandEntities = true)

        assertEquals(EventType.START_ELEMENT, reader.nextTag())
        assertEquals(QName("test"), reader.name)

        assertEquals(EventType.TEXT, reader.next())
        assertEquals(
            "This sample shows a error-prone method.",
            reader.text
        )
    }

    @Test
    fun testReadDocEntity() {
        val xml = """<!DOCTYPE foo [ <!ENTITY xxe "foobar"> ]>
            |<tag>&xxe;</tag>
        """.trimMargin()
        createReader(xml).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertEquals(QName("tag"), reader.name)

            assertEquals(EventType.TEXT, reader.next())
            assertEquals("foobar", reader.text)

            assertEquals(EventType.END_ELEMENT, reader.next())
            assertEquals(QName("tag"), reader.name)
        }
    }

    @Test
    override fun testReadUnknownEntity() {
        val xml = """<tag>&unknown;</tag>"""
        val e = assertFailsWith<XmlException> {
            createReader(xml).use { reader ->
                assertEquals(EventType.START_ELEMENT, reader.nextTag())
                assertNotEquals(EventType.ENTITY_REF, reader.next())
            }
        }
        assertEquals( "Unknown entity \"&unknown;\" in entity expanding mode", e.message!!.substringAfter(" - "))
    }

    @Test
    fun testReadUnknownEntityInAttributeNoExpand() {
        testReadUnknownEntityInAttribute(false)
    }

    @Test
    fun testReadUnknownEntityInAttributeExpand() {
        testReadUnknownEntityInAttribute(true)
    }

    private fun testReadUnknownEntityInAttribute(expandEntities: Boolean) {
        val xml = """<tag attr="&unknown;"/>"""
        val e = assertFailsWith<XmlException> {
            KtXmlReader(StringReader(xml), expandEntities).use { reader ->
                assertEquals(EventType.START_ELEMENT, reader.nextTag())
                assertNotEquals("&unknown;", reader.getAttributeValue(0))
                assertNotEquals("", reader.getAttributeValue(0))
            }
        }
        assertEquals("Unknown entity \"&unknown;\" in entity expanding mode", e.message!!.substringAfter(" - "))
    }

    @Test
    override fun testWhiteSpaceWithEntity() {
        val data = "<x>   dude &amp; &lt;dudette&gt;   </x>"
        val r = assertIs<KtXmlReader>(createReader(data))
        assertTrue(r.expandEntities)
        r.requireNextTag(EventType.START_ELEMENT, "", "x")
        assertEquals(EventType.TEXT, r.next())
        r.require(EventType.TEXT, null)
        assertEquals("   dude & <dudette>   ", r.text)

        assertEquals(EventType.END_ELEMENT, r.next())
    }
}
