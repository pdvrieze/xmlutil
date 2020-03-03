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

class TestXmlWriter {
    @Test
    fun testSerializeSimplest() {
        val serialized = buildString {
            val w = XmlStreaming.newWriter(this, false, true)
            w.smartStartTag("foobar".toQname()) { text("xx")}
            w.close()
        }
        assertEquals("<foobar>xx</foobar>", serialized)
    }

    @Test
    fun testIndentXml5Spaces() {
        val serialized = buildString {
            val w = XmlStreaming.newWriter(this, false, true)
            w.indentString="     "
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
                 <bar>
                      <deeper>something</deeper>
                      <shallow/>
                 </bar>
            </foo>
        """.trimIndent()

        assertEquals(expected, serialized)
    }

    @Test
    fun testIndentXmlTab() {
        val serialized = buildString {
            val w = XmlStreaming.newWriter(this, false, true)
            w.indentString="\t"
            w.smartStartTag("foo".toQname()) {
                smartStartTag("bar".toQname()) {
                    text("something")
                }
            }
            w.close()
        }
        val expected = """
            <foo>
            ${'\t'}<bar>something</bar>
            </foo>
        """.trimIndent()

        assertEquals(expected, serialized)
    }
}