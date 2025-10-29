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

import nl.adaptivity.xmlutil.core.impl.multiplatform.use
import nl.adaptivity.xmlutil.test.multiplatform.Target
import nl.adaptivity.xmlutil.test.multiplatform.testTarget
import nl.adaptivity.xmlutil.util.CompactFragment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

open class TestXmlWriter {

    open fun createWriter(output: Appendable): XmlWriter {
        return xmlStreaming.newGenericWriter(output, isRepairNamespaces = false, xmlDeclMode = XmlDeclMode.None)
    }

    private fun testIndentImpl1(indent: String) {
        val serialized = buildString {
            val w = createWriter(this)
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
            val w = createWriter(this)
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
            val w = createWriter(this)
            w.smartStartTag("foobar".toQname()) { text("xx") }
            w.close()
        }
        assertEquals("<foobar>xx</foobar>", serialized)
    }

    @Test
    fun testCorrectWithFlush() {
        val serialized = buildString {
            createWriter(this).use { writer ->
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

    /** Test to check/confirm #304 */
    @Test
    fun testCData() {
        val serialized = buildString {
            createWriter(this).use { writer ->
                writer.smartStartTag(null, "a") {
                    safeCdsect("a & b ]]>>c")
                }
            }
        }
        assertEquals("<a><![CDATA[a & b ]]]]><![CDATA[>>c]]></a>", serialized)
    }

    /** Test to check/confirm #304 */
    @Test
    fun testCDataGeneric() {
        val serialized = buildString {
            xmlStreaming.newGenericWriter(this).use { writer ->
                writer.smartStartTag(null, "a") {
                    cdsect("a & b ]]>>c")
                }
            }
        }
        assertEquals("<a><![CDATA[a & b ]]]]><![CDATA[>>c]]></a>", serialized)
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
        val w = createWriter(StringBuilder())
        assertFailsWith<XmlException> {
            w.indentString = "  ..."
        }
    }

    @Test
    fun testIndentIncompleteComment() {
        val w = createWriter(StringBuilder())
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
        createWriter(builder).use { output ->
            output.startTag("foobar", "root", "")
            output.namespaceAttr("", "foobar")

            fragment.serialize(output)
            output.endTag("foobar", "root", "")
        }
        assertEquals(xml, builder.toString())
    }

    @Test
    fun testWritePlatformSmartTag() {
        val expected = "<a xmlns=\"ns/a\"><b xmlns=\"ns/b\"><c xmlns=\"\" val=\"value\"/></b></a>"
        val builder = StringBuilder()
        createWriter(builder).use { out ->
            out.smartStartTag("ns/a", "a", "") {
                smartStartTag("ns/b", "b", "") {
                    smartStartTag("", "c", "")
                    attribute("", "val", "", "value")
                    endTag("", "c", "")
                }
            }
        }

        assertEquals(expected, builder.toString().replace(" />", "/>"))
    }

    @Test
    fun testWriteGenericSmartTag() {
        val expected = "<a xmlns=\"ns/a\"><b xmlns=\"ns/b\"><c xmlns=\"\" val=\"value\"/></b></a>"
        val builder = StringBuilder()
        xmlStreaming.newGenericWriter(builder).use { out ->
            out.smartStartTag("ns/a", "a", "") {
                smartStartTag("ns/b", "b", "") {
                    smartStartTag("", "c", "")
                    attribute("", "val", "", "value")
                    endTag("", "c", "")
                }
            }
        }

        assertEquals(expected, builder.toString().replace(" />", "/>"))
    }

    @Test
    fun testWriteDomSmartTag() {
        if (testTarget != Target.Node) {
            val expected = "<a xmlns=\"ns/a\"><b xmlns=\"ns/b\"><c xmlns=\"\" val=\"value\"/></b></a>"
            val builder = StringBuilder()
            val dw = DomWriter()
            dw.let { out ->
                out.smartStartTag("ns/a", "a", "") {
                    smartStartTag("ns/b", "b", "") {
                        smartStartTag("", "c", "")
                        attribute("", "val", "", "value")
                        endTag("", "c", "")
                    }
                }
            }
            xmlStreaming.newWriter(builder, false).use { out ->
                val input = xmlStreaming.newReader(dw.target)
                while (input.hasNext()) {
                    input.next()
                    if (!input.eventType.isIgnorable)
                        input.writeCurrent(out)
                }
            }

            assertEquals(expected, builder.toString().replace(" />", "/>"))
        }

    }

    @Test
    fun testWriteAttrWithTabOrNewline() {
        val actual = buildString {
            xmlStreaming.newWriter(this).use { out ->
                out.smartStartTag(null, "foo") {
                    attribute(null, "bar", "", "\nbaz\r\t")
                }
            }
        }.lowercase()
            .replace(" />", "/>")
            .replace("&#10;", "&#xa;")
            .replace("&#9;", "&#x9;")
            .replace("&#13;", "&#xd;")


        val expected = "<foo bar=\"&#xa;baz&#xd;&#x9;\"/>"
        assertEquals(expected, actual.lowercase())
    }

    @Test
    fun testWriteAttrWithTabOrNewlineGeneric() {
        val actual = buildString {
            xmlStreaming.newGenericWriter(this).use { out ->
                out.smartStartTag(null, "foo") {
                    attribute(null, "bar", "", "\nbaz\r\t")
                }
            }
        }
        val expected = "<foo bar=\"&#xa;baz&#xd;&#x9;\" />"
        assertEquals(expected, actual)
    }

}
