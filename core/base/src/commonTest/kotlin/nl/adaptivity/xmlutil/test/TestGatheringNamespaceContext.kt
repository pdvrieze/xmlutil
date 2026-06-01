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

import nl.adaptivity.xmlutil.SimpleNamespaceContext
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.XmlEvent
import nl.adaptivity.xmlutil.util.GatheringNamespaceContext
import kotlin.test.*

class TestGatheringNamespaceContext {

    private fun parentContext(vararg pairs: Pair<String, String>): SimpleNamespaceContext {
        return SimpleNamespaceContext.from(pairs.map { (p, u) -> XmlEvent.NamespaceImpl(p, u) })
    }

    @Test
    fun testGetNamespaceURIGathersIntoMap() {
        val map = mutableMapOf<String, String>()
        val ctx = GatheringNamespaceContext(parentContext("ex" to "http://example.com"), map)
        val uri = ctx.getNamespaceURI("ex")
        assertEquals("http://example.com", uri)
        assertEquals(mapOf("ex" to "http://example.com"), map)
    }

    @Test
    fun testGetNamespaceURIDoesNotGatherXmlnsPrefix() {
        // "xmlns" prefix → XMLNS_ATTRIBUTE_NS_URI from SimpleNamespaceContext, but should NOT be gathered
        val map = mutableMapOf<String, String>()
        val ctx = GatheringNamespaceContext(parentContext(), map)
        // SimpleNamespaceContext returns XMLNS_ATTRIBUTE_NS_URI for "xmlns" internally,
        // but GatheringNamespaceContext skips prefix == XMLNS_ATTRIBUTE
        val _ = ctx.getNamespaceURI(XMLConstants.XMLNS_ATTRIBUTE)
        assertTrue(map.isEmpty(), "xmlns prefix should not be gathered, but map was $map")
    }

    @Test
    fun testGetNamespaceURIDoesGatherXmlPrefix() {
        // "xml" prefix IS gathered (only "xmlns" is excluded)
        val map = mutableMapOf<String, String>()
        val ctx = GatheringNamespaceContext(parentContext(), map)
        val uri = ctx.getNamespaceURI(XMLConstants.XML_NS_PREFIX)
        // SimpleNamespaceContext returns XML_NS_URI for "xml"
        assertEquals(XMLConstants.XML_NS_URI, uri)
        assertEquals(mapOf(XMLConstants.XML_NS_PREFIX to XMLConstants.XML_NS_URI), map)
    }

    @Test
    fun testGetNamespaceURIDoesNotGatherEmptyResult() {
        val map = mutableMapOf<String, String>()
        // parentContext with no mapping for "missing"
        val ctx = GatheringNamespaceContext(parentContext(), map)
        val uri = ctx.getNamespaceURI("missing")
        // Returns null (no mapping), nothing gathered
        assertNull(uri)
        assertTrue(map.isEmpty())
    }

    @Test
    fun testGetNamespaceURIWithNullParentReturnsNull() {
        val map = mutableMapOf<String, String>()
        val ctx = GatheringNamespaceContext(null, map)
        assertNull(ctx.getNamespaceURI("ex"))
        assertTrue(map.isEmpty())
    }

    @Test
    fun testGetPrefixGathersIntoMap() {
        val map = mutableMapOf<String, String>()
        val ctx = GatheringNamespaceContext(parentContext("ex" to "http://example.com"), map)
        val prefix = ctx.getPrefix("http://example.com")
        assertEquals("ex", prefix)
        assertEquals(mapOf("ex" to "http://example.com"), map)
    }

    @Test
    fun testGetPrefixDoesNotGatherXmlNsUri() {
        val map = mutableMapOf<String, String>()
        val ctx = GatheringNamespaceContext(parentContext(), map)
        val _ = ctx.getPrefix(XMLConstants.XML_NS_URI)
        assertTrue(map.isEmpty(), "XML_NS_URI prefix should not be gathered, but map was $map")
    }

    @Test
    fun testGetPrefixDoesNotGatherXmlnsNsUri() {
        val map = mutableMapOf<String, String>()
        val ctx = GatheringNamespaceContext(parentContext(), map)
        val _ = ctx.getPrefix(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)
        assertTrue(map.isEmpty(), "XMLNS_ATTRIBUTE_NS_URI prefix should not be gathered, but map was $map")
    }

    @Test
    fun testGetPrefixesGathersAllPrefixes() {
        val map = mutableMapOf<String, String>()
        val ctx = GatheringNamespaceContext(parentContext("ex" to "http://example.com"), map)
        val it = ctx.getPrefixes("http://example.com")
        // consume the iterator to trigger gathering
        val prefixes = buildList { while (it.hasNext()) add(it.next()) }
        assertTrue("ex" in prefixes)
        assertEquals(mapOf("ex" to "http://example.com"), map)
    }

    @Test
    fun testGetPrefixesDoesNotGatherXmlNsUri() {
        val map = mutableMapOf<String, String>()
        val ctx = GatheringNamespaceContext(parentContext(), map)
        val it = ctx.getPrefixes(XMLConstants.XML_NS_URI)
        while (it.hasNext()) { val _ = it.next() }
        assertTrue(map.isEmpty())
    }

    @Test
    fun testGetPrefixesWithNullParentReturnsEmpty() {
        val map = mutableMapOf<String, String>()
        val ctx = GatheringNamespaceContext(null, map)
        assertFalse(ctx.getPrefixes("http://example.com").hasNext())
    }
}
