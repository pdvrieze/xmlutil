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

import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.core.KtXmlWriter
import nl.adaptivity.xmlutil.core.XmlEntity
import nl.adaptivity.xmlutil.core.impl.EntityMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression tests for bugs found in the nl.adaptivity.xmlutil.core package
 * (excluding KtXmlReader which is covered by TestKtXmlReaderBugs).
 */
@OptIn(ExperimentalXmlUtilApi::class, XmlUtilInternal::class)
class TestCoreBugs {

    // -----------------------------------------------------------------------
    // Bug A: KtXmlWriter.endDocument() swaps localName and prefix arguments
    //   in the auto-close loop, causing it to throw IllegalArgumentException
    //   for any open element.
    //
    //   Call: endTag(namespaceAt(d), prefixAt(d), localNameAt(d))
    //   Fix:  endTag(namespaceAt(d), localNameAt(d), prefixAt(d))
    // -----------------------------------------------------------------------

    @Test
    fun testEndDocumentAutoClosesUnprefixedElement() {
        val output = StringBuilder()
        val writer = KtXmlWriter(output, xmlDeclMode = XmlDeclMode.None)
        writer.startTag("", "root", "")
        // endDocument() should auto-close <root> without throwing
        writer.endDocument()
        val result = output.toString()
        assertTrue(result.contains("root"), "Output should contain 'root', got: $result")
        assertTrue(result.endsWith("</root>") || result.endsWith("/>"),
            "Output should end with a closing tag, got: $result")
    }

    @Test
    fun testEndDocumentAutoClosesPrefixedElement() {
        val output = StringBuilder()
        val writer = KtXmlWriter(output, xmlDeclMode = XmlDeclMode.None)
        writer.startTag("http://example.com", "root", "ns")
        writer.namespaceAttr("ns", "http://example.com")
        // endDocument() must auto-close <ns:root> without throwing
        writer.endDocument()
        val result = output.toString()
        assertTrue(result.contains("ns:root"), "Output should contain 'ns:root', got: $result")
    }

    @Test
    fun testEndDocumentAutoClosesNestedElements() {
        val output = StringBuilder()
        val writer = KtXmlWriter(output, xmlDeclMode = XmlDeclMode.None)
        writer.startTag("", "root", "")
        writer.startTag("", "child", "")
        // endDocument() should auto-close child, then root, without throwing
        writer.endDocument()
        val result = output.toString()
        assertTrue(result.contains("root"), "Output should mention root, got: $result")
        assertTrue(result.contains("child"), "Output should mention child, got: $result")
    }

    // -----------------------------------------------------------------------
    // Bug B: NamespaceHolder.namespaceAtCurrentDepth() uses `step 2` when
    //   iterating pair indices, causing it to skip every odd-indexed pair.
    //   This prevents KtXmlWriter.namespaceAttr() from detecting duplicate
    //   namespace declarations at odd positions (e.g. the 2nd declaration).
    //
    //   Fix: remove `step 2` from the for loop.
    // -----------------------------------------------------------------------

    @Test
    fun testDuplicateNamespaceAtOddIndexIsDetectedWithRepairMode() {
        val output = StringBuilder()
        // With isRepairNamespaces=true, a duplicate namespace declaration should be silently ignored
        val writer = KtXmlWriter(output, isRepairNamespaces = true, xmlDeclMode = XmlDeclMode.None)
        writer.startTag("", "root", "")
        writer.namespaceAttr("a", "http://ns1")  // pair index 0
        writer.namespaceAttr("b", "http://ns2")  // pair index 1
        // Declare xmlns:b="http://ns2" again — should be silently ignored (repair mode)
        writer.namespaceAttr("b", "http://ns2")  // duplicate of pair index 1
        writer.endTag("", "root", "")

        val result = output.toString()
        // Count occurrences of xmlns:b: should be exactly 1
        val count = result.split("xmlns:b").size - 1
        assertEquals(1, count, "xmlns:b should appear exactly once but got: $result")
    }

    @Test
    fun testDuplicateNamespaceAtOddIndexIsDetectedWithoutRepairMode() {
        val output = StringBuilder()
        // With isRepairNamespaces=false, duplicate namespace at odd pair index must throw
        val writer = KtXmlWriter(output, isRepairNamespaces = false, xmlDeclMode = XmlDeclMode.None)
        writer.startTag("", "root", "")
        writer.namespaceAttr("a", "http://ns1")  // pair index 0
        writer.namespaceAttr("b", "http://ns2")  // pair index 1
        var threw = false
        try {
            writer.namespaceAttr("b", "http://ns2")  // exact duplicate of pair index 1
        } catch (e: IllegalStateException) {
            threw = true
        }
        assertTrue(threw, "Expected IllegalStateException for duplicate namespace declaration at odd index")
    }

    // -----------------------------------------------------------------------
    // Bug C: DefaultEntityMap.APOS and QUOT have replacement values missing
    //   the trailing semicolon: "&#39" and "&#34" instead of "&#39;" and "&#34;".
    //   These are malformed numeric character references.
    //
    //   Tested indirectly through XmlEntity.resolveEmbeddedEntities since
    //   DefaultEntityMap is internal to the module.
    //
    //   Fix: add ";" to both replacement values.
    // -----------------------------------------------------------------------

    @Test
    fun testAposEntityResolvesCorrectly() {
        // Build an entity whose replacementValue contains &apos;
        val entity = XmlEntity("it&apos;s fine", false)
        val entityMap = EntityMap()
        val result = entity.resolveEmbeddedEntities(entityMap)
        assertEquals("it's fine", result, "Entity &apos; should resolve to apostrophe")
    }

    @Test
    fun testQuotEntityResolvesCorrectly() {
        val entity = XmlEntity("say &quot;hello&quot;", false)
        val entityMap = EntityMap()
        val result = entity.resolveEmbeddedEntities(entityMap)
        assertEquals("say \"hello\"", result, "Entity &quot; should resolve to double-quote")
    }

    // -----------------------------------------------------------------------
    // Bug D: XmlEntity.resolveEmbeddedEntities() has two issues:
    //   1. No `else -> append(replacementValue[i])` branch — all non-entity
    //      characters are silently dropped from the output.
    //   2. For named entity references, the index `i` is already advanced past
    //      ';' inside the branch, then the outer `i += 1` advances it one more
    //      character, skipping the char immediately after the entity reference.
    //   3. Simple entities use `replacementValue` instead of `simpleValue`
    //      (e.g. &amp; resolves to "&#38;" instead of "&").
    //
    //   Fix: add the missing else branch, use simpleValue for simple entities,
    //        and remove the outer `i += 1` (or use `continue` after entity).
    // -----------------------------------------------------------------------

    @Test
    fun testResolveEmbeddedEntitiesPreservesNonEntityCharacters() {
        val entity = XmlEntity("plain text", false)
        val entityMap = EntityMap()
        val result = entity.resolveEmbeddedEntities(entityMap)
        assertEquals("plain text", result, "Plain text should be preserved unchanged")
    }

    @Test
    fun testResolveEmbeddedEntitiesDecodesEntityAndPreservesSurroundingText() {
        val entity = XmlEntity("hello &amp; world", false)
        val entityMap = EntityMap()
        val result = entity.resolveEmbeddedEntities(entityMap)
        assertEquals("hello & world", result,
            "Entity should be decoded and surrounding text preserved")
    }

    @Test
    fun testResolveEmbeddedEntitiesDecodesSuffix() {
        // Specifically tests the double-advance bug: the char after ';' must not be skipped
        val entity = XmlEntity("&amp;!", false)
        val entityMap = EntityMap()
        val result = entity.resolveEmbeddedEntities(entityMap)
        assertEquals("&!", result, "Character after entity reference must not be dropped")
    }

    @Test
    fun testResolveEmbeddedEntitiesDecodesCharacterEntityReference() {
        val entity = XmlEntity("&#65;&#66;&#67;", false) // ABC in decimal
        val entityMap = EntityMap()
        val result = entity.resolveEmbeddedEntities(entityMap)
        assertEquals("ABC", result, "Numeric character references should decode correctly")
    }

}
