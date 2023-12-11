/*
 * Copyright (c) 2023.
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

package io.github.pdvrieze.formats.xpath.test

import io.github.pdvrieze.formats.xpath.XPathExpression
import io.github.pdvrieze.formats.xpath.impl.*
import nl.adaptivity.xmlutil.QName
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(XPathInternal::class)
class XPathTest {

    @Test
    fun testPara() {
        testPath("para") {
            assertPath {
                assertFalse(path.rooted)
                assertStep("para")
            }
        }
    }

    @Test
    fun testAllChildren() {
        testPath("*") {
            assertPath {
                assertFalse(path.rooted)
                assertStep<NodeTest.AnyNameTest> {}
            }
        }
    }

    @Test
    fun testText() {
        testPath("text()") {
            assertPath {
                assertFalse(path.rooted)
                assertStep<NodeTest.NodeTypeTest> { assertEquals(NodeType.TEXT, it.type) }
            }
        }
    }

    @Test
    fun testNameAttr() {
        testPath("@name") {
            assertPath {
                assertFalse(path.rooted)
                assertStep(Axis.ATTRIBUTE, "name")
            }
        }
    }

    @Test
    fun testAllAttrs() {
        testPath("@*") {
            assertPath {
                assertFalse(path.rooted)
                assertStep<NodeTest.AnyNameTest>(Axis.ATTRIBUTE) {}
            }
        }
    }

    @Test
    fun testParaOne() {
        testPath("para[1]") {
            assertPath {
                assertStep("para") {
                    assertPredicate {
                        assertNumber(1)
                    }
                }
            }
        }
    }

    @Test
    fun testLastPara() {
        testPath("para[last()]") {
            assertPath {
                assertStep("para") {
                    assertPredicate {
                        assertFunctionCall("last")
                    }
                }
            }
        }
    }

    @Test
    fun testParaGrandChildren() {
        testPath("*/para") {
            assertPath {
                assertStep<NodeTest.AnyNameTest> {  }
                assertStep("para")
            }
        }
    }

    @Test
    fun testSectionInDocChapter() {
        testPath("/doc/chapter[5]/section[2]") {
            assertPath {
                assertStep("doc")
                assertStep("chapter") {
                    assertPredicate { assertNumber(5) }
                }
                assertStep("section") {
                    assertPredicate { assertNumber(2) }
                }
            }
        }
    }

    @Test
    fun testChapterParaDescendants() {
        val expr = XPathExpression("chapter//para")
        testPath("chapter//para") {
            assertPath {
                assertStep("chapter")
                assertStepDescendant()
                assertStep("para")
            }
        }
    }

    @Test
    fun testParaDescendants() {
        testPath("//para") {
            assertPath {
                assertTrue(path.rooted)
                assertStepDescendant()
                assertStep("para")
            }
        }
    }

    @Test
    fun testAnyOlistItem() {
        val expr = XPathExpression("//olist/item")
        assertEquals("//olist/item", expr.test)
        testPath("//olist/item") {
            assertPath {
                assertStep(Axis.DESCENDANT_OR_SELF, NodeType.NODE)
                assertStep("olist")
                assertStep("item")
            }
        }

    }

    @Test
    fun testContextNode() {
        testPath(".") {
            assertPath {
                assertStepSelf()
            }
        }
    }

    @Test
    fun testContextParaDescendants() {
        testPath(".//para") {
            assertPath {
                assertStepSelf()
                assertStepDescendant()
                assertStep("para")
            }
        }
    }

    @Test
    fun testUnionPath() {
        testPath(".//myNS:t | .//myNS:u", "myNS" to "myNS") {
            assertBinary(Operator.UNION) {
                assertLeft<LocationPath> {
                    assertPath {
                        assertStepSelf()
                        assertStepDescendant()
                        assertStep(Axis.CHILD, QName("myNS", "t", "myNS"))
                    }
                }
                assertRight<LocationPath> {
                    assertPath {
                        assertStepSelf()
                        assertStepDescendant()
                        assertStep(Axis.CHILD, QName("myNS", "u", "myNS"))
                    }
                }
            }
        }
    }

}
