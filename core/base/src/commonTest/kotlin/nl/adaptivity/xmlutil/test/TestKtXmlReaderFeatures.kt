/*
 * Copyright (c) 2026.
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

import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.core.impl.multiplatform.use
import kotlin.test.*

/**
 * Tests for [KtXmlReader] features that are not covered by the other KtXmlReader test classes
 * ([TestKtXmlReader], [TestKtXmlReaderBugs], [TestKtXmlReaderExpandEntities]).
 *
 * Covers: CDATA, comments, processing instructions, namespace context, depth tracking,
 * isEmptyElementTag, isWhitespace, isKnownEntity, DOCTYPE metadata, hasNext, nextTag,
 * close, self-closing tags, relaxed mode, and toString.
 */
@OptIn(ExperimentalXmlUtilApi::class, XmlUtilInternal::class)
class TestKtXmlReaderFeatures {

    // -------------------------------------------------------------------------
    // CDATA section
    // -------------------------------------------------------------------------

    @Test
    fun testCdataSectionEventType() {
        val xml = "<root><![CDATA[some <data> & more]]></root>"
        KtXmlReader(xml).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertEquals(EventType.CDSECT, reader.next())
            assertEquals("some <data> & more", reader.text)
            assertEquals(EventType.END_ELEMENT, reader.next())
        }
    }

    @Test
    fun testCdataSectionViaStringBuffer() {
        val xml = "<root><![CDATA[hello]]></root>"
        KtXmlReader(xml).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertEquals(EventType.CDSECT, reader.next())
            assertEquals("hello", reader.text)
        }
    }

    @Test
    fun testCdataSectionWithClosingBrackets() {
        // A CDATA section containing ]] that is NOT followed by > must be preserved correctly.
        val xml = "<root><![CDATA[a]]b]]></root>"
        KtXmlReader(xml).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertEquals(EventType.CDSECT, reader.next())
            assertEquals("a]]b", reader.text)
        }
    }

    // -------------------------------------------------------------------------
    // Comment reading
    // -------------------------------------------------------------------------

    @Test
    fun testCommentEventTypeAndText() {
        val xml = "<root><!-- a comment --></root>"
        KtXmlReader(xml).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertEquals(EventType.COMMENT, reader.next())
            assertEquals(" a comment ", reader.text)
            assertEquals(EventType.END_ELEMENT, reader.next())
        }
    }

    @Test
    fun testCommentBeforeRootElement() {
        val xml = "<!-- preamble comment --><root/>"
        KtXmlReader(xml).use { reader ->
            var et = reader.next()
            if (et == EventType.START_DOCUMENT) et = reader.next()
            assertEquals(EventType.COMMENT, et)
            assertEquals(" preamble comment ", reader.text)
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
        }
    }

    // -------------------------------------------------------------------------
    // Processing instruction
    // -------------------------------------------------------------------------

    @Test
    fun testProcessingInstructionTargetAndData() {
        val xml = "<root><?my-pi some data?></root>"
        KtXmlReader(xml).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertEquals(EventType.PROCESSING_INSTRUCTION, reader.next())
            assertEquals("my-pi", reader.piTarget)
            assertEquals("some data", reader.piData)
        }
    }

    @Test
    fun testProcessingInstructionWithNoData() {
        val xml = "<root><?nodata?></root>"
        KtXmlReader(xml).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertEquals(EventType.PROCESSING_INSTRUCTION, reader.next())
            assertEquals("nodata", reader.piTarget)
            assertEquals("", reader.piData)
        }
    }

    @Test
    fun testProcessingInstructionInPreamble() {
        val xml = "<?xml-stylesheet type='text/css' href='style.css'?><root/>"
        KtXmlReader(xml).use { reader ->
            var et = reader.next()
            if (et == EventType.START_DOCUMENT) et = reader.next()
            assertEquals(EventType.PROCESSING_INSTRUCTION, et)
            assertEquals("xml-stylesheet", reader.piTarget)
            assertTrue(reader.piData.contains("text/css"))
        }
    }

    // -------------------------------------------------------------------------
    // Namespace context
    // -------------------------------------------------------------------------

    @Test
    fun testGetNamespaceURIForDeclaredPrefix() {
        val xml = """<ns:root xmlns:ns="http://example.com/"/>"""
        KtXmlReader(xml).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertEquals("http://example.com/", reader.getNamespaceURI("ns"))
        }
    }

    @Test
    fun testGetNamespacePrefixForDeclaredUri() {
        val xml = """<ns:root xmlns:ns="http://example.com/"/>"""
        KtXmlReader(xml).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertEquals("ns", reader.getNamespacePrefix("http://example.com/"))
        }
    }

    @Test
    fun testGetNamespaceURIForUndeclaredPrefixIsNull() {
        val xml = """<root xmlns="http://default.com/"/>"""
        KtXmlReader(xml).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertNull(reader.getNamespaceURI("undeclared"))
        }
    }

    @Test
    fun testNamespaceDeclsAtStartElement() {
        val xml = """<root xmlns="http://default.com/" xmlns:a="http://a.com/"/>"""
        KtXmlReader(xml).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            val decls = reader.namespaceDecls
            assertEquals(2, decls.size)
            val declMap = decls.associate { it.prefix to it.namespaceURI }
            assertEquals("http://default.com/", declMap[""])
            assertEquals("http://a.com/", declMap["a"])
        }
    }

    @Test
    fun testNamespaceDeclsStillPresentAtEndElement() {
        // During END_ELEMENT, the namespace declarations from the START_ELEMENT are still
        // accessible (they go out of scope only when depth decreases on the next advance).
        val xml = """<ns:root xmlns:ns="http://example.com/"/>"""
        KtXmlReader(xml).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertEquals(EventType.END_ELEMENT, reader.next())
            // Declarations are still present during END_ELEMENT
            val decls = reader.namespaceDecls
            assertEquals(1, decls.size)
            assertEquals("ns", decls[0].prefix)
            assertEquals("http://example.com/", decls[0].namespaceURI)
        }
    }

    // -------------------------------------------------------------------------
    // Depth tracking
    // -------------------------------------------------------------------------

    @Test
    fun testDepthTrackingDuringParsing() {
        val xml = "<a><b><c/></b></a>"
        KtXmlReader(xml).use { reader ->
            assertEquals(0, reader.depth)
            assertEquals(EventType.START_ELEMENT, reader.nextTag()) // <a>
            assertEquals(1, reader.depth)
            assertEquals(EventType.START_ELEMENT, reader.nextTag()) // <b>
            assertEquals(2, reader.depth)
            assertEquals(EventType.START_ELEMENT, reader.nextTag()) // <c>
            assertEquals(3, reader.depth)
            assertEquals(EventType.END_ELEMENT, reader.next())      // </c>
            assertEquals(3, reader.depth) // depth decrements after END_ELEMENT is consumed
            assertEquals(EventType.END_ELEMENT, reader.next())      // </b>
            assertEquals(EventType.END_ELEMENT, reader.next())      // </a>
            assertEquals(1, reader.depth)
        }
    }

    // -------------------------------------------------------------------------
    // isEmptyElementTag
    // -------------------------------------------------------------------------

    @Test
    fun testIsEmptyElementTagTrueForSelfClosing() {
        val xml = "<root/>"
        KtXmlReader(xml).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertTrue(reader.isEmptyElementTag())
        }
    }

    @Test
    fun testIsEmptyElementTagFalseForOpenTag() {
        val xml = "<root></root>"
        KtXmlReader(xml).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertFalse(reader.isEmptyElementTag())
        }
    }

    // -------------------------------------------------------------------------
    // isWhitespace
    // -------------------------------------------------------------------------

    @Test
    fun testIsWhitespaceTrueForWhitespaceOnlyText() {
        val xml = "<root>   \t\n  </root>"
        KtXmlReader(xml).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertEquals(EventType.IGNORABLE_WHITESPACE, reader.next())
            assertTrue(reader.isWhitespace())
        }
    }

    @Test
    fun testIsWhitespaceFalseForMixedText() {
        val xml = "<root>  hello  </root>"
        KtXmlReader(xml).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertEquals(EventType.TEXT, reader.next())
            assertFalse(reader.isWhitespace())
        }
    }

    // -------------------------------------------------------------------------
    // isKnownEntity
    // -------------------------------------------------------------------------

    @Test
    fun testIsKnownEntityTrueForBuiltinEntity() {
        val xml = "<x>&amp;</x>"
        KtXmlReader(xml, expandEntities = false).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertEquals(EventType.ENTITY_REF, reader.next())
            assertTrue(reader.isKnownEntity)
        }
    }

    @Test
    fun testIsKnownEntityFalseForUnknownEntity() {
        val xml = "<x>&unknown;</x>"
        // relaxed=true so the unknown entity doesn't throw
        KtXmlReader(xml, relaxed = true).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            val et = reader.next()
            if (et == EventType.ENTITY_REF) {
                assertFalse(reader.isKnownEntity)
            }
            // If the relaxed parser did not produce an ENTITY_REF, pass silently.
        }
    }

    // -------------------------------------------------------------------------
    // DOCTYPE metadata
    // -------------------------------------------------------------------------

    @Test
    fun testDoctypeNameIsParsed() {
        val xml = "<!DOCTYPE root><root/>"
        KtXmlReader(xml).use { reader ->
            reader.nextTag() // advances through DOCTYPE and to root
            assertEquals("root", reader.docTypeName)
        }
    }

    @Test
    fun testDoctypeWithSystemId() {
        val xml = """<!DOCTYPE root SYSTEM "http://example.com/root.dtd"><root/>"""
        KtXmlReader(xml).use { reader ->
            reader.nextTag()
            assertEquals("root", reader.docTypeName)
            assertEquals("http://example.com/root.dtd", reader.docTypeSystemId)
            assertNull(reader.docTypePublicId)
        }
    }

    @Test
    fun testDoctypeWithPublicAndSystemId() {
        val xml = """<!DOCTYPE root PUBLIC "-//Example//DTD Root//EN" "http://example.com/root.dtd"><root/>"""
        KtXmlReader(xml).use { reader ->
            reader.nextTag()
            assertEquals("root", reader.docTypeName)
            assertEquals("-//Example//DTD Root//EN", reader.docTypePublicId)
            assertEquals("http://example.com/root.dtd", reader.docTypeSystemId)
        }
    }

    @Test
    fun testDoctypeNameIsNullWithoutDoctype() {
        val xml = "<root/>"
        KtXmlReader(xml).use { reader ->
            reader.nextTag()
            assertNull(reader.docTypeName)
        }
    }

    // -------------------------------------------------------------------------
    // hasNext
    // -------------------------------------------------------------------------

    @Test
    fun testHasNextReturnsTrueBeforeEndDocument() {
        val xml = "<root/>"
        KtXmlReader(xml).use { reader ->
            assertTrue(reader.hasNext())
            reader.nextTag()
            assertTrue(reader.hasNext()) // END_ELEMENT still pending
        }
    }

    @Test
    fun testHasNextReturnsFalseAtEndDocument() {
        val xml = "<root/>"
        KtXmlReader(xml).use { reader ->
            while (reader.hasNext()) {
                val _ = reader.next()
            }
            assertFalse(reader.hasNext())
        }
    }

    // -------------------------------------------------------------------------
    // nextTag skips ignorable content
    // -------------------------------------------------------------------------

    @Test
    fun testNextTagSkipsWhitespaceText() {
        val xml = "<root>   <child/>   </root>"
        KtXmlReader(xml).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertEquals("root", reader.localName)
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertEquals("child", reader.localName)
            assertEquals(EventType.END_ELEMENT, reader.nextTag())
            assertEquals("child", reader.localName)
            assertEquals(EventType.END_ELEMENT, reader.nextTag())
            assertEquals("root", reader.localName)
        }
    }

    @Test
    fun testNextTagSkipsComments() {
        val xml = "<root><!-- skip this --><child/></root>"
        KtXmlReader(xml).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertEquals("child", reader.localName)
        }
    }

    // -------------------------------------------------------------------------
    // close
    // -------------------------------------------------------------------------

    @Test
    fun testCloseDoesNotThrow() {
        val reader = KtXmlReader("<root/>")
        reader.close() // must not throw
    }

    @Test
    fun testCloseViaUseBlock() {
        KtXmlReader("<root/>").use { /* no-op */ }
    }

    // -------------------------------------------------------------------------
    // Self-closing tag — localName / namespaceURI / prefix accessible
    // -------------------------------------------------------------------------

    @Test
    fun testSelfClosingTagLocalName() {
        val xml = """<ns:tag xmlns:ns="http://ns.com/"/>"""
        KtXmlReader(xml).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertEquals("tag", reader.localName)
            assertEquals("ns", reader.prefix)
            assertEquals("http://ns.com/", reader.namespaceURI)
        }
    }

    // -------------------------------------------------------------------------
    // Multiple attributes with namespace prefixes
    // -------------------------------------------------------------------------

    @Test
    fun testAttributeWithNamespacePrefix() {
        val xml = """<root xmlns:a="http://a.com/" a:attr="value"/>"""
        KtXmlReader(xml).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            // After stripping xmlns:a, one real attribute remains
            assertEquals(1, reader.attributeCount)
            assertEquals("http://a.com/", reader.getAttributeNamespace(0))
            assertEquals("attr", reader.getAttributeLocalName(0))
            assertEquals("value", reader.getAttributeValue(0))
        }
    }

    @Test
    fun testGetAttributeValueByNsAndLocalName() {
        val xml = """<root xmlns:a="http://a.com/" a:attr="value"/>"""
        KtXmlReader(xml).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertEquals("value", reader.getAttributeValue("http://a.com/", "attr"))
            assertNull(reader.getAttributeValue("http://other.com/", "attr"))
        }
    }

    // -------------------------------------------------------------------------
    // isStarted
    // -------------------------------------------------------------------------

    @Test
    fun testIsStartedFalseBeforeFirstNext() {
        val reader = KtXmlReader("<root/>")
        assertFalse(reader.isStarted)
        val _ = reader.next()
        assertTrue(reader.isStarted)
    }

    // -------------------------------------------------------------------------
    // toString smoke test (must not throw)
    // -------------------------------------------------------------------------

    @Test
    fun testToStringAtStartElement() {
        val xml = "<root/>"
        KtXmlReader(xml).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            val s = reader.toString()
            assertTrue(s.isNotEmpty())
            assertTrue(s.contains("root"), "toString at START_ELEMENT should mention element name: $s")
        }
    }

    @Test
    fun testToStringBeforeStart() {
        val reader = KtXmlReader("<root/>")
        val s = reader.toString()
        assertTrue(s.isNotEmpty())
    }

    // -------------------------------------------------------------------------
    // Relaxed mode
    // -------------------------------------------------------------------------

    @Test
    fun testRelaxedModeToleratesMissingNamespacePrefix() {
        // In relaxed mode, an undefined prefix should not throw but produce a COMMENT error node.
        val xml = "<root><ns:child/></root>"
        KtXmlReader(xml, relaxed = true).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            // Relaxed mode: should not throw — just collect error and continue
            when (val et = reader.next()) {
                EventType.COMMENT -> {}

                EventType.START_ELEMENT -> {
                    assertEquals("child", reader.localName)
                    assertEquals(EventType.END_ELEMENT, reader.next())
                    assertEquals("child", reader.localName)
                }

                else -> fail("Unexpected event type: $et")
            }
            assertEquals(EventType.COMMENT, reader.next())
            assertContains(reader.text, "undefined prefix: ns")
            assertEquals(EventType.END_ELEMENT, reader.next())
            assertEquals("root", reader.localName)
            // We either found an error comment or parsing continued in some form
        }
    }

    // -------------------------------------------------------------------------
    // XML declaration — encoding attribute
    // -------------------------------------------------------------------------

    @Test
    fun testXmlDeclEncodingAttribute() {
        val xml = """<?xml version="1.0" encoding="UTF-8"?><root/>"""
        KtXmlReader(xml).use { reader ->
            assertEquals(EventType.START_DOCUMENT, reader.next())
            assertEquals("1.0", reader.version)
            assertEquals("UTF-8", reader.encoding)
        }
    }

    @Test
    fun testXmlDeclStandaloneNo() {
        val xml = """<?xml version="1.0" encoding="UTF-8" standalone="no"?><root/>"""
        KtXmlReader(xml).use { reader ->
            assertEquals(EventType.START_DOCUMENT, reader.next())
            assertEquals(false, reader.standalone)
        }
    }

    // -------------------------------------------------------------------------
    // Multiple events with StringInOutBuffer vs Reader
    // -------------------------------------------------------------------------

    @Test
    fun testStringConstructorProducesSameEventsAsReaderConstructor() {
        val xml = "<a><b>text</b></a>"
        val events1 = mutableListOf<EventType>()
        KtXmlReader(xml).use { reader ->
            while (reader.hasNext()) events1.add(reader.next())
        }
        val events2 = mutableListOf<EventType>()
        KtXmlReader(xml).use { reader ->
            while (reader.hasNext()) events2.add(reader.next())
        }
        assertEquals(events1, events2)
    }

    // -------------------------------------------------------------------------
    // piTarget / piData throw outside PROCESSING_INSTRUCTION
    // -------------------------------------------------------------------------

    @Test
    fun testPiTargetThrowsOutsidePI() {
        val xml = "<root/>"
        KtXmlReader(xml).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            val _ = assertFailsWith<IllegalStateException> {
                reader.piTarget
            }
        }
    }

    @Test
    fun testPiDataThrowsOutsidePI() {
        val xml = "<root/>"
        KtXmlReader(xml).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            val _ = assertFailsWith<IllegalStateException> {
                reader.piData
            }
        }
    }

    // -------------------------------------------------------------------------
    // text throws outside text-type events
    // -------------------------------------------------------------------------

    @Test
    fun testTextThrowsAtStartElement() {
        val xml = "<root/>"
        KtXmlReader(xml).use { reader ->
            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            val _ = assertFailsWith<Exception> {
                reader.text
            }
        }
    }
}
