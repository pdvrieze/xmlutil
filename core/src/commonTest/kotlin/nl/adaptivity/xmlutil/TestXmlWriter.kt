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
            w.smartStartTag("foobar".toQname()) { text("xx")}
            w.close()
        }
        assertEquals("<foobar>xx</foobar>", serialized)
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
            w.indentString="  ..."
        }
    }

    @Test
    fun testIndentIncompleteComment() {
        val w = XmlStreaming.newWriter(StringBuilder())
        assertFailsWith<XmlException> {
            w.indentString="<!--"
        }
        assertFailsWith<XmlException> {
            w.indentString="<!-- "
        }
        assertFailsWith<XmlException> {
            w.indentString="<!-- -"
        }
        assertFailsWith<XmlException> {
            w.indentString="<!-- --"
        }
    }
}