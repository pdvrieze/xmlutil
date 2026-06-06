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

import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.XmlEvent
import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.core.KtXmlWriter
import nl.adaptivity.xmlutil.core.impl.multiplatform.use
import nl.adaptivity.xmlutil.util.CompactFragment
import kotlin.test.*

class TestCompactFragment {

    // -----------------------------------------------------------------------
    // Constructors and isEmpty / contentString
    // -----------------------------------------------------------------------

    @Test
    @Suppress("DEPRECATION")
    fun testStringConstructorWithEmptyString() {
        val frag = CompactFragment("")
        assertTrue(frag.isEmpty)
        assertEquals("", frag.contentString)
        assertEquals(0, frag.content.size)
    }

    @Test
    fun testStringConstructorWithContent() {
        val frag = CompactFragment("<item/>")
        assertFalse(frag.isEmpty)
        assertEquals("<item/>", frag.contentString)
    }

    @Test
    @Suppress("DEPRECATION")
    fun testNamespacesAndNullCharArrayConstructorIsEmpty() {
        val frag = CompactFragment(emptyList(), null as CharArray?)
        assertTrue(frag.isEmpty)
        assertEquals(0, frag.content.size)
    }

    @Test
    fun testNamespacesAndCharArrayConstructorPopulatesContent() {
        val ns = listOf(XmlEvent.NamespaceImpl("ex", "http://example.com"))
        val frag = CompactFragment(ns, "<ex:item/>".toCharArray())
        assertFalse(frag.isEmpty)
        assertEquals("<ex:item/>", frag.contentString)
    }

    @Test
    fun testNamespacesAndStringConstructor() {
        val ns = listOf(XmlEvent.NamespaceImpl("ex", "http://example.com"))
        val frag = CompactFragment(ns, "<ex:item/>")
        assertFalse(frag.isEmpty)
        assertEquals("ex", frag.namespaces.iterator().next().prefix)
        assertEquals("http://example.com", frag.namespaces.iterator().next().namespaceURI)
    }

    @Test
    fun testCopyConstructorProducesEqualFragment() {
        val original = CompactFragment(
            listOf(XmlEvent.NamespaceImpl("ex", "http://example.com")),
            "<ex:item/>"
        )
        val copy = CompactFragment(original)
        assertEquals(original.contentString, copy.contentString)
        assertEquals(original.namespaces, copy.namespaces)
    }

    // -----------------------------------------------------------------------
    // equals / hashCode
    // -----------------------------------------------------------------------

    @Test
    fun testEqualFragments() {
        val a = CompactFragment(emptyList(), "<item/>")
        val b = CompactFragment(emptyList(), "<item/>")
        assertEquals(a, b)
    }

    @Test
    fun testUnequalFragmentsDifferentContent() {
        val a = CompactFragment(emptyList(), "<item/>")
        val b = CompactFragment(emptyList(), "<other/>")
        assertNotEquals(a, b)
    }

    @Test
    fun testHashCodeConsistentWithEquals() {
        val a = CompactFragment(emptyList(), "<item/>")
        val b = CompactFragment(emptyList(), "<item/>")
        assertEquals(a.hashCode(), b.hashCode())
    }

    // -----------------------------------------------------------------------
    // toString
    // -----------------------------------------------------------------------

    @Test
    fun testToStringWithoutNamespaces() {
        val frag = CompactFragment("<item/>")
        val str = frag.toString()
        assertTrue(str.contains("content=<item/>"), "toString should include content: $str")
        assertTrue(str.contains("namespaces=[]"), "toString should show empty namespaces: $str")
    }

    @Test
    fun testToStringWithNamespace() {
        val frag = CompactFragment(listOf(XmlEvent.NamespaceImpl("ex", "http://example.com")), "<ex:item/>")
        val str = frag.toString()
        assertTrue(str.contains("ex -> http://example.com"), "toString should include ns mapping: $str")
    }

    // -----------------------------------------------------------------------
    // getXmlReader
    // -----------------------------------------------------------------------

    @Test
    fun testGetXmlReaderReturnsReaderAtFirstElement() {
        val frag = CompactFragment(emptyList(), "<item/>")
        val reader = frag.getXmlReader()
        val _ = reader.nextTag()
        assertEquals("item", reader.localName)
    }

    @Test
    fun testGetXmlReaderHandlesNestedElements() {
        val frag = CompactFragment(emptyList(), "<root><child/></root>")
        val reader = frag.getXmlReader()
        val _ = reader.nextTag()
        assertEquals("root", reader.localName)
    }

    // -----------------------------------------------------------------------
    // serialize
    // -----------------------------------------------------------------------

    @Test
    fun testSerializeWritesContentToWriter() {
        val frag = CompactFragment(emptyList(), "<item/>")
        val sb = StringBuilder()
        KtXmlWriter(sb, isRepairNamespaces = false, xmlDeclMode = XmlDeclMode.None).use { writer ->
            frag.serialize(writer)
        }
        // KtXmlWriter canonicalizes empty elements with a space before '/>'
        assertEquals("<item />", sb.toString())
    }

    @Test
    fun testSerializeWithNestedElement() {
        val frag = CompactFragment(emptyList(), "<root><child/></root>")
        val sb = StringBuilder()
        KtXmlWriter(sb, isRepairNamespaces = false, xmlDeclMode = XmlDeclMode.None).use { writer ->
            frag.serialize(writer)
        }
        assertEquals("<root><child /></root>", sb.toString())
    }

    @Test
    fun testSerializeEmptyFragmentIsEmpty() {
        // An empty CompactFragment has no content; verify isEmpty
        val frag = CompactFragment("")
        assertTrue(frag.isEmpty)
        assertEquals("", frag.contentString)
    }

    // -----------------------------------------------------------------------
    // deserialize (static companion function)
    // -----------------------------------------------------------------------

    @Test
    fun testDeserializeFromReaderCapturesSingleElement() {
        val reader = KtXmlReader("<item/>")
        val frag = CompactFragment.deserialize(reader)
        // KtXmlWriter re-serializes with canonical spacing: <item />
        assertEquals("<item />", frag.contentString)
    }

    @Test
    fun testDeserializeFromReaderWithTextContent() {
        val reader = KtXmlReader("<item>hello</item>")
        val frag = CompactFragment.deserialize(reader)
        assertTrue(frag.contentString.contains("item"), "Content should contain 'item': ${frag.contentString}")
    }

    @Test
    fun testDeserializeRoundTrip() {
        val original = CompactFragment(emptyList(), "<root><child attr=\"val\" /></root>")
        val reader = original.getXmlReader()
        val roundTripped = CompactFragment.deserialize(reader)
        assertEquals(original.contentString, roundTripped.contentString)
    }
}
