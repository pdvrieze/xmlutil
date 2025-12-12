/*
 * Copyright (c) 2025.
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

package nl.adaptivity.xmlutil.serialization

import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.serialization.impl.PseudoBufferedReader
import nl.adaptivity.xmlutil.serialization.impl.SubDocumentReader
import nl.adaptivity.xmlutil.xmlStreaming
import kotlin.test.*

class TestSubDocumentReader {


    @Test
    fun testReadWholeDocument() {
        val r = PseudoBufferedReader(xmlStreaming.newGenericReader(TEST_DOC))

        r.requireNextTag(EventType.START_ELEMENT, "", "root")
        assertEquals(1, r.depth)
        assertEquals(EventType.IGNORABLE_WHITESPACE, r.next())
        assertEquals(1, r.depth)

        val nested = SubDocumentReader(r, true)
        assertTrue(nested.hasNext())
        assertEquals(EventType.IGNORABLE_WHITESPACE, nested.next())
        assertEquals(1, r.depth)
        assertEquals(0, nested.depth)

        assertEquals(EventType.COMMENT, nested.next())
        assertEquals(" a comment ", nested.text)
        assertEquals(1, r.depth)
        assertEquals(0, nested.depth)

        assertEquals(EventType.IGNORABLE_WHITESPACE, nested.next())
        assertEquals(1, r.depth)
        assertEquals(0, nested.depth)

        nested.requireNext(EventType.START_ELEMENT, "", "subtag")
        assertEquals(2, r.depth)
        assertEquals(1, nested.depth)

        nested.requireNext(EventType.START_ELEMENT, "", "nested")
        assertEquals(3, r.depth)
        assertEquals(2, nested.depth)

        assertEquals(EventType.TEXT, nested.next())
        assertEquals("Some text", nested.text)
        assertEquals(3, r.depth)
        assertEquals(2, nested.depth)

        nested.requireNext(EventType.END_ELEMENT, "", "nested")
        assertEquals(3, r.depth)
        assertEquals(2, nested.depth)

        assertEquals(EventType.TEXT, nested.next())
        assertContains(nested.text, "Other text")
        assertEquals(2, r.depth)
        assertEquals(1, nested.depth)

        nested.requireNext(EventType.END_ELEMENT, "", "subtag")
        assertEquals(2, r.depth)
        assertEquals(1, nested.depth)

        assertEquals(EventType.IGNORABLE_WHITESPACE, nested.next())
        assertEquals(1, r.depth)
        assertEquals(0, nested.depth)

        nested.requireNext(EventType.START_ELEMENT, "", "othertag")
        assertEquals(2, r.depth)
        assertEquals(1, nested.depth)

        nested.requireNext(EventType.END_ELEMENT, "", "othertag")
        assertEquals(2, r.depth)
        assertEquals(1, nested.depth)

        assertEquals(EventType.IGNORABLE_WHITESPACE, nested.next())
        assertEquals(1, r.depth)
        assertEquals(0, nested.depth)

        assertFalse(nested.hasNext())
        assertEquals(
            assertFailsWith<IllegalStateException> { nested.next() }.message!!,
            "Reading beyond end of subdocument reader"
        )

        r.requireNext(EventType.END_ELEMENT, "", "root")
        assertEquals(1, r.depth)
    }

    @Test
    fun testReadFirstWhitespace() {
        val r = PseudoBufferedReader(xmlStreaming.newGenericReader(TEST_DOC))
        r.requireNextTag(EventType.START_ELEMENT, "", "root")
        assertEquals(EventType.IGNORABLE_WHITESPACE, r.next())

        val nested = SubDocumentReader(r, false)
        assertTrue(nested.hasNext())
        assertEquals(EventType.IGNORABLE_WHITESPACE, nested.next())
        assertFalse(nested.hasNext())
        assertFailsWith<IllegalStateException> { nested.next() }
        assertEquals(EventType.COMMENT, r.next())
    }

    @Test
    fun testReadFirstTag() {
        val r = PseudoBufferedReader(xmlStreaming.newGenericReader(TEST_DOC))
        r.requireNextTag(EventType.START_ELEMENT, "", "root")
        r.requireNextTag(EventType.START_ELEMENT, "", "subtag")

        val nested = SubDocumentReader(r, false)
        assertTrue(nested.hasNext())
        nested.requireNext(EventType.START_ELEMENT, "", "subtag")

        nested.requireNext(EventType.START_ELEMENT, "", "nested")
        assertEquals(EventType.TEXT, nested.next())
        assertEquals("Some text", nested.text)
        nested.requireNext(EventType.END_ELEMENT, "", "nested")

        assertEquals(EventType.TEXT, nested.next())
        assertContains(nested.text, "Other text")
        nested.requireNext(EventType.END_ELEMENT, "", "subtag")

        assertFalse(nested.hasNext())
        assertFailsWith<IllegalStateException> { nested.next() }

        assertEquals(EventType.IGNORABLE_WHITESPACE, r.next())

        r.requireNext(EventType.START_ELEMENT, "", "othertag")
    }

    @Test
    fun testReadFromFirstTag() {
        val r = PseudoBufferedReader(xmlStreaming.newGenericReader(TEST_DOC))
        r.requireNextTag(EventType.START_ELEMENT, "", "root")
        r.requireNextTag(EventType.START_ELEMENT, "", "subtag")

        val nested = SubDocumentReader(r, isParseAllSiblings = true)
        assertTrue(nested.hasNext())
        nested.requireNext(EventType.START_ELEMENT, "", "subtag")

        nested.requireNext(EventType.START_ELEMENT, "", "nested")
        assertEquals(EventType.TEXT, nested.next())
        assertEquals("Some text", nested.text)
        nested.requireNext(EventType.END_ELEMENT, "", "nested")

        assertEquals(EventType.TEXT, nested.next())
        assertContains(nested.text, "Other text")
        nested.requireNext(EventType.END_ELEMENT, "", "subtag")

        assertEquals(EventType.IGNORABLE_WHITESPACE, nested.next())
        nested.requireNext(EventType.START_ELEMENT, "", "othertag")
        nested.requireNext(EventType.END_ELEMENT, "", "othertag")
        assertEquals(EventType.IGNORABLE_WHITESPACE, nested.next())

        assertFalse(nested.hasNext())
        assertFailsWith<IllegalStateException> { nested.next() }

        r.requireNext(EventType.END_ELEMENT, "", "root")

    }

    @Test
    fun testReadNestedTagOnly() {
        val r = PseudoBufferedReader(xmlStreaming.newGenericReader(TEST_DOC))
        r.requireNextTag(EventType.START_ELEMENT, "", "root")
        r.requireNextTag(EventType.START_ELEMENT, "", "subtag")
        r.requireNext(EventType.START_ELEMENT, "", "nested")

        val nested = SubDocumentReader(r, false)

        nested.requireNext(EventType.START_ELEMENT, "", "nested")
        assertEquals(EventType.TEXT, nested.next())
        assertEquals("Some text", nested.text)
        assertTrue(nested.hasNext())
        nested.requireNext(EventType.END_ELEMENT, "", "nested")
        assertFalse(nested.hasNext())
        assertFailsWith<IllegalStateException> { nested.next() }

        assertEquals(EventType.TEXT, r.next())
        assertContains(r.text, "Other text")
    }

    @Test
    fun testReadFromNestedTag() {
        val r = PseudoBufferedReader(xmlStreaming.newGenericReader(TEST_DOC))
        r.requireNextTag(EventType.START_ELEMENT, "", "root")
        r.requireNextTag(EventType.START_ELEMENT, "", "subtag")
        r.requireNext(EventType.START_ELEMENT, "", "nested")

        val nested = SubDocumentReader(r, true)

        nested.requireNext(EventType.START_ELEMENT, "", "nested")
        assertEquals(EventType.TEXT, nested.next())
        assertEquals("Some text", nested.text)
        assertTrue(nested.hasNext())
        nested.requireNext(EventType.END_ELEMENT, "", "nested")
        assertEquals(EventType.TEXT, nested.next())
        assertContains(nested.text, "Other text")
        assertFalse(nested.hasNext())
        assertFailsWith<IllegalStateException> { nested.next() }
        r.requireNextTag(EventType.END_ELEMENT, "", "subtag")

    }


    companion object {
        val TEST_DOC =
            """ |<root>
            |   <!-- a comment -->
            |   <subtag><nested>Some text</nested>
            |       Other text
            |   </subtag>
            |   <othertag />
            |</root>""".trimMargin()
    }
}
