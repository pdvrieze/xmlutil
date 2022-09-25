/*
 * Copyright (c) 2020.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.core.impl.multiplatform.use
import nl.adaptivity.xmlutil.util.CompactFragment
import kotlin.jvm.JvmOverloads
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TestXmlWriter {
    private fun testIndentImpl1(indent: String) {
        val serialized = buildString {
            val w = XmlStreaming.newWriter(this, repairNamespaces = false, xmlDeclMode = XmlDeclMode.None)
            w.indentString = indent
            w.smartStartTag("foo".toQname()) {
                smartStartTag("bar".toQname()) {
                    smartStartTag("deeper".toQname()) {
                        text("something")
                    }
                    smartStartTag("shallow".toQname()) {}
                }
            }
            w.close()
        }.replace(" />", "/>")
        val expected = """
                <foo>
                $indent<bar>
                $indent$indent<deeper>something</deeper>
                $indent$indent<shallow/>
                $indent</bar>
                </foo>
            """.trimIndent()

        assertEquals(expected, serialized)
    }

    private fun testIndentImpl2(indent: String) {
        val serialized = buildString {
            val w = XmlStreaming.newWriter(this, repairNamespaces = false, xmlDeclMode = XmlDeclMode.None)
            w.indentString = indent
            w.smartStartTag("foo".toQname()) {
                smartStartTag("bar".toQname()) {
                    text("something")
                }
            }
            w.close()
        }
        val expected = """
                <foo>
                $indent<bar>something</bar>
                </foo>
            """.trimIndent()

        assertEquals(expected, serialized)
    }

    @Test
    fun testSerializeSimplest() {
        val serialized = buildString {
            val w = XmlStreaming.newWriter(
                this,
                repairNamespaces = false,
                xmlDeclMode = XmlDeclMode.None
            )
            w.smartStartTag("foobar".toQname()) { text("xx") }
            w.close()
        }
        assertEquals("<foobar>xx</foobar>", serialized)
    }

    @Test
    fun testCorrectWithFlush() {
        val serialized = buildString {
            XmlStreaming.newWriter(this).use { writer ->
                writer.smartStartTag(null, "a") {
                    flush()
                    smartStartTag(null, "b") {
                        attribute(null, "c", null, "xx")
                    }
                }
            }
        }
        assertEquals("<a><b c=\"xx\"/></a>", serialized.replace(" />", "/>"))
    }

    @Test
    fun testIndentXml5Spaces1() {
        testIndentImpl1("     ")
    }

    @Test
    fun testIndentXml5Spaces2() {
        testIndentImpl2("     ")
    }

    @Test
    fun testIndentXmlTab1() {
        testIndentImpl1("\t")
    }

    @Test
    fun testIndentXmlTab2() {
        testIndentImpl2("\t")
    }

    @Test
    fun testIndentXmlMixed1() {
        testIndentImpl1("  <!--\t__-->")
    }

    @Test
    fun testIndentXmlMixed2() {
        testIndentImpl2("<!-- -->\t  ")
    }

    @Test
    fun testIndentXmlComment1() {
        testIndentImpl1("<!--.-->")
    }

    @Test
    fun testIndentXmlComment2() {
        testIndentImpl2("<!--xxx-->")
    }

    @Test
    fun testIndentTextContent() {
        val w = XmlStreaming.newWriter(StringBuilder())
        assertFailsWith<XmlException> {
            w.indentString = "  ..."
        }
    }

    @Test
    fun testIndentIncompleteComment() {
        val w = XmlStreaming.newWriter(StringBuilder())
        assertFailsWith<XmlException> {
            w.indentString = "<!--"
        }
        assertFailsWith<XmlException> {
            w.indentString = "<!-- "
        }
        assertFailsWith<XmlException> {
            w.indentString = "<!-- -"
        }
        assertFailsWith<XmlException> {
            w.indentString = "<!-- --"
        }
    }

    @Test
    fun testWriteCompactFragment() {
        val inner = """
            |  <sub1>
            |        <sub2>tala
            |  </sub2>  </sub1>
            |""".trimMargin()
        val xml = "<root xmlns=\"foobar\">$inner</root>"

        val fragment = CompactFragment(listOf(XmlEvent.NamespaceImpl("", "foobar")), inner)

        val builder = StringBuilder()
        XmlStreaming.newWriter(builder).use { output ->
            output.startTag("foobar", "root", "")
            output.namespaceAttr("", "foobar")

            fragment.serialize(output)
            output.endTag("foobar", "root", "")
        }
        assertEquals(xml, builder.toString())
    }

    @Test
    fun testWriteSmartTag() {
        val expected = "<a xmlns=\"ns/a\"><b xmlns=\"ns/b\"><c xmlns=\"\" val=\"value\"/></b></a>"
        val builder = StringBuilder()
        XmlStreaming.newWriter(builder).use { out ->
            out.smartStartTag("ns/a", "a", "") {
                smartStartTag("ns/b", "b", "") {
                    smartStartTag2("", "c", "")
                    attribute("", "val", "", "value")
                    endTag("", "c", "")
                }
            }
        }

        assertEquals(expected, builder.toString())

    }

    @JvmOverloads
    public fun XmlWriter.smartStartTag2(nsUri: String?, localName: String, prefix: String? = null) {
        if (nsUri == null) {
            val namespace = namespaceContext.getNamespaceURI(prefix ?: XMLConstants.DEFAULT_NS_PREFIX) ?: XMLConstants.NULL_NS_URI
            startTag(namespace, localName, prefix)
        } else {
            var writeNs = false

            val usedPrefix = getPrefix(nsUri) ?: run {
                val currentNs = prefix?.let { getNamespaceUri(it) } ?: XMLConstants.NULL_NS_URI
                if (nsUri != currentNs) {
                    writeNs = true
                }; prefix ?: XMLConstants.DEFAULT_NS_PREFIX
            }

            startTag(nsUri, localName, usedPrefix)

            if (writeNs) this.namespaceAttr(usedPrefix, nsUri)
        }
    }

}
