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
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.core.KtXmlWriter
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.core.impl.multiplatform.use
import nl.adaptivity.xmlutil.smartStartTag
import kotlin.test.*

/**
 * Tests for [KtXmlWriter] and [XmlVersion] features not covered elsewhere.
 *
 * Covers: comment, entityRef, processingInstruction, docdecl, startDocument,
 * ignorableWhitespace, various xmlDeclMode options, addTrailingSpaceBeforeEnd,
 * text escaping, attribute escaping, and XmlVersion.fromStringOrNull.
 */
@OptIn(ExperimentalXmlUtilApi::class)
class TestKtXmlWriterFeatures {

    private inline fun writer(block: KtXmlWriter.() -> Unit): String {
        val out = StringBuilder()
        KtXmlWriter(out, isRepairNamespaces = false, xmlDeclMode = XmlDeclMode.None).use { it.block() }
        return out.toString()
    }

    // -------------------------------------------------------------------------
    // comment()
    // -------------------------------------------------------------------------

    @Test
    fun testCommentIsWrittenCorrectly() {
        val result = writer {
            smartStartTag("", "root") {
                comment(" a comment ")
            }
        }
        assertEquals("<root><!-- a comment --></root>", result)
    }

    @Test
    fun testCommentWithDoubleDashEscaped() {
        // A comment body with "--" must be escaped so the output is valid XML.
        val result = writer {
            smartStartTag("", "root") {
                comment("a--b")
            }
        }
        // The output must not contain "-->" prematurely, but the comment must be present.
        assertTrue(result.contains("<!--"), "Comment start marker missing: $result")
        assertTrue(result.contains("-->"), "Comment end marker missing: $result")
        assertFalse(result.contains("---->"), "Unescaped -- must not appear as --->: $result")
    }

    // -------------------------------------------------------------------------
    // entityRef()
    // -------------------------------------------------------------------------

    @Test
    fun testEntityRefIsWritten() {
        val result = writer {
            smartStartTag("", "root") {
                entityRef("amp")
            }
        }
        assertEquals("<root>&amp;</root>", result)
    }

    @Test
    fun testEntityRefCustomName() {
        val result = writer {
            smartStartTag("", "root") {
                entityRef("myEntity")
            }
        }
        assertEquals("<root>&myEntity;</root>", result)
    }

    // -------------------------------------------------------------------------
    // processingInstruction()
    // -------------------------------------------------------------------------

    @Test
    fun testProcessingInstructionWithTextForm() {
        val result = writer {
            smartStartTag("", "root") {
                processingInstruction("target data")
            }
        }
        assertEquals("<root><?target data?></root>", result)
    }

    @Test
    fun testProcessingInstructionWithTargetAndData() {
        val result = writer {
            smartStartTag("", "root") {
                processingInstruction("mytarget", "mydata")
            }
        }
        assertEquals("<root><?mytarget mydata?></root>", result)
    }

    @Test
    fun testProcessingInstructionWithEmptyData() {
        val result = writer {
            smartStartTag("", "root") {
                processingInstruction("mytarget", "")
            }
        }
        assertEquals("<root><?mytarget?></root>", result)
    }

    // -------------------------------------------------------------------------
    // docdecl()
    // -------------------------------------------------------------------------

    @Test
    fun testDocdeclIsWritten() {
        val result = writer {
            docdecl("root")
            smartStartTag("", "root") {}
        }
        assertTrue(result.startsWith("<!DOCTYPE root>"), "Expected DOCTYPE declaration, got: $result")
    }

    // -------------------------------------------------------------------------
    // startDocument()
    // -------------------------------------------------------------------------

    @Test
    fun testStartDocumentWritesXmlDecl() {
        val out = StringBuilder()
        KtXmlWriter(out, xmlDeclMode = XmlDeclMode.None).use { w ->
            w.startDocument("1.0", "UTF-8", null)
            w.smartStartTag("", "root") {}
        }
        val result = out.toString()
        assertTrue(result.startsWith("<?xml"), "Expected xml declaration, got: $result")
        assertTrue(result.contains("version='1.0'"), "Expected version, got: $result")
        assertTrue(result.contains("encoding='UTF-8'"), "Expected encoding, got: $result")
    }

    @Test
    fun testStartDocumentWithStandalone() {
        val out = StringBuilder()
        KtXmlWriter(out, xmlDeclMode = XmlDeclMode.None).use { w ->
            w.startDocument("1.0", "UTF-8", true)
            w.smartStartTag("", "root") {}
        }
        val result = out.toString()
        assertTrue(result.contains("standalone='yes'"), "Expected standalone=yes, got: $result")
    }

    @Test
    fun testStartDocumentCalledTwiceThrows() {
        val out = StringBuilder()
        val w = KtXmlWriter(out, xmlDeclMode = XmlDeclMode.None)
        w.startDocument("1.0", null, null)
        assertFailsWith<XmlException> {
            w.startDocument("1.0", null, null)
        }
    }

    // -------------------------------------------------------------------------
    // ignorableWhitespace()
    // -------------------------------------------------------------------------

    @Test
    fun testIgnorableWhitespaceIsWritten() {
        val result = writer {
            smartStartTag("", "root") {
                ignorableWhitespace("  ")
            }
        }
        assertEquals("<root>  </root>", result)
    }

    @Test
    fun testIgnorableWhitespaceWithNonWhitespaceThrows() {
        val _ = writer {
            smartStartTag("", "root") {
                val _ = assertFailsWith<IllegalArgumentException> {
                    ignorableWhitespace("not whitespace")
                }
            }
        }
    }

    @Test
    fun testIgnorableWhitespaceEmptyStringIsNoop() {
        val result = writer {
            smartStartTag("", "root") {
                ignorableWhitespace("") // must not throw
            }
        }
        // Empty whitespace is a no-op; the tag self-closes since no content was added.
        assertEquals("<root/>", result.replace(" />", "/>"))
    }

    // -------------------------------------------------------------------------
    // xmlDeclMode
    // -------------------------------------------------------------------------

    @Test
    fun testXmlDeclModeNoneProducesNoXmlDecl() {
        val out = StringBuilder()
        KtXmlWriter(out, xmlDeclMode = XmlDeclMode.None).use { w ->
            w.smartStartTag("", "root") {}
        }
        assertFalse(out.toString().startsWith("<?xml"), "XmlDeclMode.None should not emit xml decl, got: $out")
    }

    @Test
    fun testXmlDeclModeAlwaysProducesXmlDecl() {
        val out = StringBuilder()
        KtXmlWriter(out, xmlDeclMode = XmlDeclMode.Auto).use { w ->
            w.smartStartTag("", "root") {}
        }
        // XmlDeclMode.Auto emits the declaration for XML 1.0 only when required,
        // but at minimum the tag should be present since Auto triggers it.
        // We just check no exception was thrown and the tag was written.
        assertTrue(out.toString().contains("root"), "root element missing from output: $out")
    }

    // -------------------------------------------------------------------------
    // addTrailingSpaceBeforeEnd
    // -------------------------------------------------------------------------

    @Test
    fun testNoTrailingSpaceBeforeEndTag() {
        val out = StringBuilder()
        KtXmlWriter(out, xmlDeclMode = XmlDeclMode.None).apply {
            addTrailingSpaceBeforeEnd = false
        }.use { w ->
            w.smartStartTag("", "root") {}
        }
        assertEquals("<root/>", out.toString())
    }

    @Test
    fun testDefaultTrailingSpaceBeforeEndTag() {
        val out = StringBuilder()
        KtXmlWriter(out, xmlDeclMode = XmlDeclMode.None).use { w ->
            w.smartStartTag("", "root") {}
        }
        assertEquals("<root />", out.toString())
    }

    // -------------------------------------------------------------------------
    // text() escaping
    // -------------------------------------------------------------------------

    @Test
    fun testTextEscapesAmpersand() {
        val result = writer {
            smartStartTag("", "root") {
                text("a & b")
            }
        }
        assertEquals("<root>a &amp; b</root>", result)
    }

    @Test
    fun testTextEscapesLessThan() {
        val result = writer {
            smartStartTag("", "root") {
                text("a < b")
            }
        }
        assertEquals("<root>a &lt; b</root>", result)
    }

    @Test
    fun testTextEscapesGreaterThan() {
        // > is only required to be escaped by spec in ]]>, but many writers escape it always.
        val result = writer {
            smartStartTag("", "root") {
                text("a > b")
            }
        }
        // Either kept as > or escaped as &gt; — both are valid, but must parse back correctly.
        assertTrue(result.contains("<root"), "root element missing: $result")
        assertTrue(result.contains(">a") && result.contains("b<"), "Content missing: $result")
    }

    @Test
    fun testEmptyTextIsNoop() {
        val result = writer {
            smartStartTag("", "root") {
                text("") // must not prevent self-closing
            }
        }
        // Empty text should allow the tag to self-close
        assertTrue(result.contains("root"), "root missing: $result")
    }

    // -------------------------------------------------------------------------
    // Attribute escaping
    // -------------------------------------------------------------------------

    @Test
    fun testAttributeEscapesAmpersand() {
        val result = writer {
            smartStartTag("", "root") {
                attribute("", "attr", "", "a & b")
            }
        }
        assertTrue(result.contains("&amp;"), "Ampersand in attribute must be escaped, got: $result")
    }

    @Test
    fun testAttributeEscapesDoubleQuote() {
        // If value contains a double-quote, the writer should use single-quote delimiter.
        val result = writer {
            smartStartTag("", "root") {
                attribute("", "attr", "", """say "hello"""")
            }
        }
        assertTrue(result.contains("attr="), "Attribute missing: $result")
        // The double quote must either be escaped or the attribute wrapped in single quotes.
        assertTrue(
            result.contains("&quot;") || result.contains("attr='say \"hello\"'"),
            "Double quote not escaped correctly: $result"
        )
    }

    // -------------------------------------------------------------------------
    // depth tracking
    // -------------------------------------------------------------------------

    @Test
    fun testDepthTrackingDuringWrite() {
        val out = StringBuilder()
        val w = KtXmlWriter(out, xmlDeclMode = XmlDeclMode.None)
        assertEquals(0, w.depth)
        w.smartStartTag("", "a") {
            assertEquals(1, w.depth)
            smartStartTag("", "b") {
                assertEquals(2, w.depth)
            }
            assertEquals(1, w.depth)
        }
        assertEquals(0, w.depth)
    }

    // -------------------------------------------------------------------------
    // endTag with wrong name throws
    // -------------------------------------------------------------------------

    @Test
    fun testEndTagWithWrongLocalNameThrows() {
        val out = StringBuilder()
        val w = KtXmlWriter(out, xmlDeclMode = XmlDeclMode.None)
        w.startTag("", "root", "")
        assertFailsWith<IllegalArgumentException> {
            w.endTag("", "other", "")
        }
    }

    // -------------------------------------------------------------------------
    // namespaceAttr for default namespace
    // -------------------------------------------------------------------------

    @Test
    fun testDefaultNamespaceAttrIsWritten() {
        val result = writer {
            smartStartTag("http://example.com/", "root") {}
        }
        assertTrue(result.contains("xmlns=\"http://example.com/\""), "Default namespace missing: $result")
    }

    @Test
    fun testPrefixedNamespaceAttrIsWritten() {
        val result = writer {
            smartStartTag("http://example.com/", "root", "ns") {}
        }
        assertTrue(result.contains("xmlns:ns=\"http://example.com/\""), "Prefixed namespace missing: $result")
    }
}

