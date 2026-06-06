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

@file:OptIn(nl.adaptivity.xmlutil.XmlUtilInternal::class)

package nl.adaptivity.xmlutil.test

import nl.adaptivity.xmlutil.SimpleNamespaceContext
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.XmlEvent
import nl.adaptivity.xmlutil.util.impl.CombiningNamespaceContext
import kotlin.test.*

class TestCombiningNamespaceContext {

    private fun nsCtx(vararg pairs: Pair<String, String>): SimpleNamespaceContext {
        return SimpleNamespaceContext.from(pairs.map { (p, u) -> XmlEvent.NamespaceImpl(p, u) })
    }

    @Test
    fun testGetNamespaceURIFromPrimary() {
        val primary = nsCtx("ex" to "http://primary.com")
        val secondary = nsCtx("ex" to "http://secondary.com")
        val ctx = CombiningNamespaceContext(primary, secondary)
        assertEquals("http://primary.com", ctx.getNamespaceURI("ex"))
    }

    @Test
    fun testGetNamespaceURIFallsBackToSecondaryWhenPrimaryReturnsNull() {
        val primary = nsCtx() // no "ex" mapping
        val secondary = nsCtx("ex" to "http://secondary.com")
        val ctx = CombiningNamespaceContext(primary, secondary)
        assertEquals("http://secondary.com", ctx.getNamespaceURI("ex"))
    }

    @Test
    fun testGetNamespaceURIFallsBackToSecondaryWhenPrimaryReturnsNullNsUri() {
        // Primary maps "ex" to NULL_NS_URI (empty namespace), secondary has real mapping
        val primary = nsCtx("ex" to XMLConstants.NULL_NS_URI)
        val secondary = nsCtx("ex" to "http://secondary.com")
        val ctx = CombiningNamespaceContext(primary, secondary)
        // primary returns NULL_NS_URI → fallback to secondary
        assertEquals("http://secondary.com", ctx.getNamespaceURI("ex"))
    }

    @Test
    fun testGetPrefixFromPrimary() {
        val primary = nsCtx("ex" to "http://example.com")
        val secondary = nsCtx("other" to "http://example.com")
        val ctx = CombiningNamespaceContext(primary, secondary)
        assertEquals("ex", ctx.getPrefix("http://example.com"))
    }

    @Test
    fun testGetPrefixFallsBackToSecondaryWhenPrimaryReturnsNull() {
        val primary = nsCtx() // no mapping for http://example.com
        val secondary = nsCtx("ex" to "http://example.com")
        val ctx = CombiningNamespaceContext(primary, secondary)
        assertEquals("ex", ctx.getPrefix("http://example.com"))
    }

    @Test
    fun testGetPrefixNullNsUriDefaultPrefixFallsBackToSecondary() {
        // Special case: namespaceURI == NULL_NS_URI && prefix from primary == DEFAULT_NS_PREFIX
        // → fallback to secondary
        val primary = nsCtx("" to XMLConstants.NULL_NS_URI) // default ns → NULL_NS_URI
        val secondary = nsCtx("" to "http://secondary.com")
        val ctx = CombiningNamespaceContext(primary, secondary)
        // primary.getPrefix(NULL_NS_URI) returns DEFAULT_NS_PREFIX (""), which triggers fallback
        val prefix = ctx.getPrefix(XMLConstants.NULL_NS_URI)
        assertEquals("", prefix)
    }

    @Test
    fun testGetPrefixesUnionFromBothContexts() {
        val primary = nsCtx("a" to "http://example.com")
        val secondary = nsCtx("b" to "http://example.com")
        val ctx = CombiningNamespaceContext(primary, secondary)
        val prefixes = buildList {
            val it = ctx.getPrefixes("http://example.com")
            while (it.hasNext()) add(it.next())
        }.toSet()
        assertEquals(setOf("a", "b"), prefixes)
    }

    @Test
    fun testGetPrefixesNoDuplicates() {
        val primary = nsCtx("ex" to "http://example.com")
        val secondary = nsCtx("ex" to "http://example.com")
        val ctx = CombiningNamespaceContext(primary, secondary)
        val prefixes = buildList {
            val it = ctx.getPrefixes("http://example.com")
            while (it.hasNext()) add(it.next())
        }
        assertEquals(1, prefixes.size, "Duplicate prefixes should be merged: $prefixes")
    }

    @Test
    fun testFreezeReturnsSelfWhenBothAreSimpleNamespaceContext() {
        val primary = nsCtx("ex" to "http://example.com")
        val secondary = nsCtx("other" to "http://other.com")
        val ctx = CombiningNamespaceContext(primary, secondary)
        assertSame(ctx, ctx.freeze())
    }

    @Test
    fun testFreezePreservesNamespaceResolution() {
        val primary = nsCtx("a" to "http://a.com")
        val secondary = nsCtx("b" to "http://b.com")
        val ctx = CombiningNamespaceContext(primary, secondary)
        val frozen = ctx.freeze()
        assertEquals("http://a.com", frozen.getNamespaceURI("a"))
        assertEquals("http://b.com", frozen.getNamespaceURI("b"))
    }

    @Test
    fun testFreezeWithEmptySecondaryReturnsPrimary() {
        // Use CombiningNamespaceContext as primary so the SimpleNamespaceContext-short-circuit doesn't apply
        val innerPrimary = nsCtx("ex" to "http://example.com")
        val innerSecondary = nsCtx()
        val emptySecondary = nsCtx()
        val ctx = CombiningNamespaceContext(
            CombiningNamespaceContext(innerPrimary, innerSecondary),
            emptySecondary
        )
        // secondary is empty → freeze returns primary.freeze() which is innerPrimary (SimpleNamespaceContext)
        val frozen = ctx.freeze()
        assertEquals("http://example.com", frozen.getNamespaceURI("ex"))
        assertNull(frozen.getNamespaceURI("b"))
    }

    @Test
    fun testFreezeWithEmptyPrimaryReturnsSecondary() {
        val realSecondary = nsCtx("ex" to "http://example.com")
        val ctx = CombiningNamespaceContext(
            CombiningNamespaceContext(nsCtx(), nsCtx()),
            realSecondary
        )
        // primary is empty → freeze returns secondary.freeze() which is realSecondary
        val frozen = ctx.freeze()
        assertEquals("http://example.com", frozen.getNamespaceURI("ex"))
    }

    @Test
    fun testIteratorConcatenatesBothContexts() {
        val primary = nsCtx("a" to "http://a.com")
        val secondary = nsCtx("b" to "http://b.com")
        val ctx = CombiningNamespaceContext(primary, secondary)
        val all = ctx.toList()
        val prefixes = all.map { it.prefix }.toSet()
        assertTrue("a" in prefixes)
        assertTrue("b" in prefixes)
    }

    @Test
    fun testPlusCreatesNewCombiningContext() {
        val primary = nsCtx("a" to "http://a.com")
        val secondary = nsCtx("b" to "http://b.com")
        val ctx = CombiningNamespaceContext(primary, secondary)
        val extra = nsCtx("c" to "http://c.com")
        val combined = ctx + extra
        val prefixes = combined.toList().map { it.prefix }.toSet()
        assertTrue("a" in prefixes)
        assertTrue("b" in prefixes)
        assertTrue("c" in prefixes)
    }
}
