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
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.core.impl.multiplatform.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Advance the reader past an optional START_DOCUMENT event to reach the first real event.
 * Returns the current event type after advancement.
 */
private fun KtXmlReader.advancePastDocumentStart(): EventType {
    var et = next()
    if (et == EventType.START_DOCUMENT) et = next()
    return et
}

/**
 * Regression tests for bugs found in [KtXmlReader].
 *
 * Bug 1: [KtXmlReader.require] used internal `_eventType` instead of the public `eventType`
 *        property, causing it to fail when `expandEntities=true` and text content starts with an
 *        entity reference (e.g. `&amp;`). Internally the event is `ENTITY_REF`, but the public
 *        contract maps it to `TEXT` when entity expansion is enabled.
 *
 * Bug 2: The error-message string inside [KtXmlReader.require] interpolated `namespaceURI` and
 *        `localName`, which throw [IllegalStateException] for non-element event types (TEXT,
 *        COMMENT, etc.). A type mismatch on such events therefore produced an
 *        [IllegalStateException] instead of the expected [XmlException].
 *
 * Bug 3 (dead code): Inside [KtXmlReader.parseStartTag] the whitespace branch of the attribute-
 *        parsing `when` expression called `this.next()` — which would advance the entire parser
 *        to the next XML event — rather than consuming a single whitespace character via
 *        `inOutBuffer.read()`. In practice the preceding `skipWS()` call makes this branch
 *        unreachable, but the semantics are wrong and should be corrected.
 */
class TestKtXmlReaderBugs {

    // -------------------------------------------------------------------------
    // Bug 1: require() compared against internal _eventType, not public eventType
    // -------------------------------------------------------------------------

    /**
     * When `expandEntities=true` and text content *starts* with an entity reference, the
     * internal `_eventType` is `ENTITY_REF` but the public `eventType` is `TEXT`.
     * `require(TEXT, …)` must NOT throw in this situation.
     */
    @Test
    fun testRequireDoesNotFailWhenExpandedEntityTextStartsWithAmpersand() {
        val xml = "<x>&amp;text</x>"
        val reader = KtXmlReader(StringReader(xml), expandEntities = true)

        assertEquals(EventType.START_ELEMENT, reader.advancePastDocumentStart(), "Expected START_ELEMENT for <x>")

        val et = reader.next()
        // expandEntities maps ENTITY_REF to TEXT
        assertEquals(EventType.TEXT, et, "Expected TEXT after expansion of &amp;")

        // Bug 1 would cause this to throw XmlException("expected: TEXT … found: ENTITY_REF …")
        reader.require(EventType.TEXT, null, null)
        assertEquals("&text", reader.text)
    }

    /**
     * Verify the full text value when the entity-starting TEXT is followed by more content.
     * The combined output should span both the resolved entity and the literal text.
     */
    @Test
    fun testRequireDoesNotFailForEntityRefExpandedAsText_mixedContent() {
        val xml = "<x>&lt;hello&gt;</x>"
        val reader = KtXmlReader(StringReader(xml), expandEntities = true)

        assertEquals(EventType.START_ELEMENT, reader.advancePastDocumentStart())

        assertEquals(EventType.TEXT, reader.next())
        // Must not throw (Bug 1)
        reader.require(EventType.TEXT, null, null)
        assertEquals("<hello>", reader.text)
    }

    /**
     * `require` must also work for entity refs that are not at the very start of a text node —
     * i.e. text that begins with literal characters and then contains `&...;`.
     * (These are read as plain TEXT internally, so this is a non-regression check.)
     */
    @Test
    fun testRequireWorksForTextWithEmbeddedEntity() {
        val xml = "<x>hello &amp; world</x>"
        val reader = KtXmlReader(StringReader(xml), expandEntities = true)

        assertEquals(EventType.START_ELEMENT, reader.advancePastDocumentStart())

        assertEquals(EventType.TEXT, reader.next())
        reader.require(EventType.TEXT, null, null) // must not throw
        assertEquals("hello & world", reader.text)
    }

    // -------------------------------------------------------------------------
    // Bug 2: require() error message caused IllegalStateException instead of XmlException
    // -------------------------------------------------------------------------

    /**
     * At a TEXT event, `require(END_ELEMENT, …)` must throw [XmlException] — not
     * [IllegalStateException] from the error-message string template accessing `namespaceURI`
     * or `localName` (which are only valid for element events).
     */
    @Test
    fun testRequireTypeMismatchAtTextEventThrowsXmlException() {
        val xml = "<root>text content</root>"
        val reader = KtXmlReader(StringReader(xml))

        assertEquals(EventType.START_ELEMENT, reader.advancePastDocumentStart(), "Expected to be at START_ELEMENT")

        assertEquals(EventType.TEXT, reader.next(), "Expected to be at TEXT")

        // Bug 2 caused IllegalStateException here because the error message string evaluated
        // `namespaceURI` / `localName`, which throw for non-element events.
        assertFailsWith<XmlException> {
            reader.require(EventType.END_ELEMENT, null, null)
        }
    }

    /**
     * At a COMMENT event, `require(TEXT, …)` must throw [XmlException], not
     * [IllegalStateException].
     */
    @Test
    fun testRequireTypeMismatchAtCommentEventThrowsXmlException() {
        val xml = "<root><!-- comment --></root>"
        val reader = KtXmlReader(StringReader(xml))

        assertEquals(EventType.START_ELEMENT, reader.advancePastDocumentStart())

        assertEquals(EventType.COMMENT, reader.next())

        assertFailsWith<XmlException> {
            reader.require(EventType.TEXT, null, null)
        }
    }

    /**
     * Before `next()` is called (parsing not yet started), `require(…)` must throw
     * [XmlException] matching the interface contract ("Parsing not started yet"), not
     * [IllegalStateException] from accessing internal null state.
     */
    @Test
    fun testRequireBeforeParsingStartedThrowsXmlException() {
        val reader = KtXmlReader(StringReader("<root/>"))
        // reader.isStarted == false, _eventType is null internally
        assertFailsWith<XmlException> {
            reader.require(EventType.START_DOCUMENT, null, null)
        }
    }

    // -------------------------------------------------------------------------
    // Positive sanity checks (must remain working after fixes)
    // -------------------------------------------------------------------------

    /** `require` on a matched START_ELEMENT with correct namespace/localName must not throw. */
    @Test
    fun testRequireSucceedsForMatchingStartElement() {
        val xml = """<ns:root xmlns:ns="http://example.com/"/>"""
        val reader = KtXmlReader(StringReader(xml))

        assertEquals(EventType.START_ELEMENT, reader.advancePastDocumentStart())

        reader.require(EventType.START_ELEMENT, "http://example.com/", "root") // must not throw
    }

    /** `require` with wrong namespace on START_ELEMENT must throw [XmlException]. */
    @Test
    fun testRequireFailsForWrongNamespaceOnStartElement() {
        val xml = """<ns:root xmlns:ns="http://example.com/"/>"""
        val reader = KtXmlReader(StringReader(xml))

        assertEquals(EventType.START_ELEMENT, reader.advancePastDocumentStart())

        assertFailsWith<XmlException> {
            reader.require(EventType.START_ELEMENT, "http://other.com/", "root")
        }
    }

    /** `require` with wrong local name on START_ELEMENT must throw [XmlException]. */
    @Test
    fun testRequireFailsForWrongLocalNameOnStartElement() {
        val xml = "<root/>"
        val reader = KtXmlReader(StringReader(xml))

        assertEquals(EventType.START_ELEMENT, reader.advancePastDocumentStart())

        assertFailsWith<XmlException> {
            reader.require(EventType.START_ELEMENT, null, "other")
        }
    }

    /** With `expandEntities=false`, an `ENTITY_REF` event is exposed as such. */
    @Test
    fun testRequireEntityRefEventWhenNotExpandingEntities() {
        val xml = "<x>&amp;</x>"
        val reader = KtXmlReader(StringReader(xml), expandEntities = false)

        assertEquals(EventType.START_ELEMENT, reader.advancePastDocumentStart())

        assertEquals(EventType.ENTITY_REF, reader.next())
        reader.require(EventType.ENTITY_REF, null, null) // must not throw
    }
}
