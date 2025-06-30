/*
 * Copyright (c) 2024-2025.
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

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.core.impl.multiplatform.StringReader
import nl.adaptivity.xmlutil.core.impl.multiplatform.use
import nl.adaptivity.xmlutil.test.TestCommonReader
import kotlin.test.*

class TestKtXmlReaderExpandEntities : TestCommonReader() {

    override fun createReader(it: String): XmlReader {
        return xmlStreaming.newGenericReader(it, expandEntities = true).also {
            assertIs<KtXmlReader>(it)
            assertTrue(it.expandEntities, "Expand entities not set")
        }
    }

    @Test
    fun testReadEntityInAttribute() {
        val data = "<tag attr=\"&lt;xx&gt;\"/>"
        val reader = KtXmlReader(StringReader(data))
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
    override fun testWhiteSpaceWithEntity() {
        val data = "<x>   dude &amp; &lt;dudette&gt;   </x>"
        val r = assertIs<KtXmlReader>(createReader(data))
        assertTrue(r.expandEntities)
        r.nextTag()
        r.require(EventType.START_ELEMENT, "", "x")
        assertEquals(EventType.TEXT, r.next())
        r.require(EventType.TEXT, null)
        assertEquals("   dude & <dudette>   ", r.text)

        assertEquals(EventType.END_ELEMENT, r.next())
    }
}
