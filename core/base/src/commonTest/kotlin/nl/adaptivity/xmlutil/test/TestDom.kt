/*
 * Copyright (c) 2025.
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

import nl.adaptivity.xmlutil.DomWriter
import nl.adaptivity.xmlutil.dom2.Element
import nl.adaptivity.xmlutil.dom2.documentElement
import nl.adaptivity.xmlutil.test.multiplatform.Target
import nl.adaptivity.xmlutil.test.multiplatform.testTarget
import nl.adaptivity.xmlutil.writeCurrent
import nl.adaptivity.xmlutil.xmlStreaming
import kotlin.test.Test
import kotlin.test.assertEquals

class TestDom {

    /**
     * Test that getElementsByTagName works correctly. Thanks to #265
     */
    @Test
    fun test_getElementsByTagName_withNestedTags_worksCorrectly() {
        if (testTarget != Target.Node) {
            val element = getTestElementRoot()
            //.decodeFromString(Element.serializer(), NESTED_ELEMENTS_TAG_SOUP)
            val children = element.getElementsByTagName("child").toList()
            assertEquals(16, children.size)
            children.forEachIndexed { index, node ->
                assertEquals((node as Element).getAttribute("prop"), index.toString())
            }
        }
    }

    /**
     * Test that getElementsByTagName works correctly. Thanks to #265
     */
    @Test
    fun test_getElementsByTagNameWildcard_withNestedTags_worksCorrectly() {
        if (testTarget != Target.Node) {
            val element = getTestElementRoot()
            //.decodeFromString(Element.serializer(), NESTED_ELEMENTS_TAG_SOUP)
            val children = element.getElementsByTagName("*").toList()
            assertEquals(16, children.size)
            children.forEachIndexed { index, node ->
                assertEquals((node as Element).getAttribute("prop"), index.toString())
            }
        }
    }

    /**
     * Test that getElementsByTagName works correctly. Thanks to #265
     */
    @Test
    fun test_getElementsByTagNameNS_withNestedTags_worksCorrectly() {
        if (testTarget != Target.Node) {
            val element = getTestElementRoot()
            //.decodeFromString(Element.serializer(), NESTED_ELEMENTS_TAG_SOUP)
            val children = element.getElementsByTagNameNS("", "child").toList()
            assertEquals(16, children.size)
            children.forEachIndexed { index, node ->
                assertEquals((node as Element).getAttribute("prop"), index.toString())
            }
        }
    }

    /**
     * Test that getElementsByTagName works correctly. Thanks to #265
     */
    @Test
    fun test_getElementsByTagNameNSWildcard_withNestedTags_worksCorrectly() {
        if (testTarget != Target.Node) {
            val element = getTestElementRoot()
            //.decodeFromString(Element.serializer(), NESTED_ELEMENTS_TAG_SOUP)
            val children = element.getElementsByTagNameNS("*", "*").toList()
            assertEquals(16, children.size)
            children.forEachIndexed { index, node ->
                assertEquals((node as Element).getAttribute("prop"), index.toString())
            }
        }
    }

    /**
     * Test that getElementsByTagName works correctly. Thanks to #265
     */
    @Test
    fun test_getElementsByTagNameNSWildcardPart_withNestedTags_worksCorrectly() {
        if (testTarget != Target.Node) {
            val element = getTestElementRoot()
            //.decodeFromString(Element.serializer(), NESTED_ELEMENTS_TAG_SOUP)
            val children = element.getElementsByTagNameNS("*", "child").toList()
            assertEquals(16, children.size)
            children.forEachIndexed { index, node ->
                assertEquals((node as Element).getAttribute("prop"), index.toString())
            }
        }
    }

    /**
     * Test that getElementsByTagName works correctly. Thanks to #265
     */
    @Test
    fun test_getElementsByTagNameNSWildcardPartLocal_withNestedTags_worksCorrectly() {
        if (testTarget != Target.Node) {
            val element = getTestElementRoot()
            val children = element.getElementsByTagNameNS("", "*").toList()
            assertEquals(16, children.size)
            children.forEachIndexed { index, node ->
                assertEquals((node as Element).getAttribute("prop"), index.toString())
            }
        }
    }

    /**
     * Test that getElementsByTagName works correctly. Thanks to #265
     */
    @Test
    fun test_getElementsByTagNameNSWildcardNonMatchingLocal_withNestedTags_worksCorrectly() {
        if (testTarget != Target.Node) {
            val element = getTestElementRoot()
            val children = element.getElementsByTagNameNS("xx", "*").toList()
            assertEquals(0, children.size)
        }
    }

    companion object {
        val NESTED_ELEMENTS_TAG_SOUP =
            """
            <root>
                <child prop="0">
                </child>
                <child prop="1">
                    <child prop="2">
                    </child>
                </child>
                <child prop="3">
                    <child prop="4">
                        <child prop="5">
                        </child>
                        <child prop="6">
                            <child prop="7">
                            </child>
                        </child>
                        <child prop="8">
                        </child>
                        <child prop="9">
                            <child prop="10">                                
                                <child prop="11">
                                </child>                          
                                <child prop="12">
                                </child>                          
                                <child prop="13">
                                </child>                          
                                <child prop="14">
                                </child>
                            </child>
                        </child>
                    </child>
                </child>
                <child prop="15" />
            </root>
            """.trimIndent()


        fun getTestElementRoot(): Element = xmlStreaming.newReader(NESTED_ELEMENTS_TAG_SOUP).let { r ->
            DomWriter().also { w ->
                while (r.hasNext()) {
                    val _ = r.next()
                    r.writeCurrent(w)
                }
            }.target.documentElement
        }!!

    }


}
