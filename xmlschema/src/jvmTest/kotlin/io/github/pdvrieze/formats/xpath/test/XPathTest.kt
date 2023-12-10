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
import io.github.pdvrieze.formats.xpath.impl.FunctionCall
import io.github.pdvrieze.formats.xpath.impl.LocationPath
import io.github.pdvrieze.formats.xpath.impl.NumberLiteral
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.SimpleNamespaceContext
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

@OptIn(XPathInternal::class)
class XPathTest {

    @Test
    fun testPara() {
        val expr = XPathExpression("para")
        assertEquals("para", expr.test)
        val e = assertIs<LocationPath>(expr.expr)
        assertFalse(e.rooted)
        assertEquals(1, e.steps.size)
        val s = e.steps.single()
        assertEquals(Axis.CHILD, s.axis)
        assertEquals(0, s.predicates.size)
        val name = assertIs<NodeTest.QNameTest>(s.test).qName
        assertEquals(QName("para"), name)
    }

    @Test
    fun testAllChildren() {
        val expr = XPathExpression("*")
        assertEquals("*", expr.test)
        val e = assertIs<LocationPath>(expr.expr)
        assertFalse(e.rooted)
        assertEquals(1, e.steps.size)
        val s = e.steps.single()
        assertEquals(Axis.CHILD, s.axis)
        assertEquals(0, s.predicates.size)
        assertIs<NodeTest.AnyNameTest>(s.test)
    }

    @Test
    fun testText() {
        val expr = XPathExpression("text()")
        assertEquals("text()", expr.test)
        val e = assertIs<LocationPath>(expr.expr)
        assertFalse(e.rooted)
        assertEquals(1, e.steps.size)
        val s = e.steps.single()
        assertEquals(Axis.CHILD, s.axis)
        assertEquals(0, s.predicates.size)
        val n = assertIs<NodeTest.NodeTypeTest>(s.test)
        assertEquals(NodeType.TEXT, n.type)
    }

    @Test
    fun testNameAttr() {
        val expr = XPathExpression("@name")
        assertEquals("@name", expr.test)
        val e = assertIs<LocationPath>(expr.expr)
        assertFalse(e.rooted)
        assertEquals(1, e.steps.size)
        val s = e.steps.single()
        assertEquals(Axis.ATTRIBUTE, s.axis)
        assertEquals(0, s.predicates.size)
        val name = assertIs<NodeTest.QNameTest>(s.test).qName
        assertEquals("", name.namespaceURI)
        assertEquals("", name.prefix)
        assertEquals("name", name.localPart)
    }

    @Test
    fun testAllAttrs() {
        val expr = XPathExpression("@*")
        assertEquals("@*", expr.test)
        val e = assertIs<LocationPath>(expr.expr)
        assertFalse(e.rooted)
        assertEquals(1, e.steps.size)
        val step = e.steps.single()
        assertEquals(step.axis, Axis.ATTRIBUTE)
        assertEquals(0, step.predicates.size)
        val test = assertIs<NodeTest.AnyNameTest>(step.test)
    }

    @Test
    fun testParaOne() {
        val expr = XPathExpression("para[1]")
        assertEquals("para[1]", expr.test)
        val e = assertIs<LocationPath>(expr.expr)
        assertFalse(e.rooted)
        assertEquals(1, e.steps.size)
        val s = e.steps.single()
        assertEquals(Axis.CHILD, s.axis)
        val name = assertIs<NodeTest.QNameTest>(s.test).qName
        assertEquals("", name.namespaceURI)
        assertEquals("", name.prefix)
        assertEquals("para", name.localPart)

        assertEquals(1, s.predicates.size)
        val p = assertIs<NumberLiteral>(s.predicates.single())

        assertEquals(1, p.value)
    }

    @Test
    fun testLastPara() {
        val expr = XPathExpression("para[last()]")
        assertEquals("para[last()]", expr.test)
        val e = assertIs<LocationPath>(expr.expr)
        assertFalse(e.rooted)
        assertEquals(1, e.steps.size)
        val s = e.steps.single()
        assertEquals(Axis.CHILD, s.axis)
        val name = assertIs<NodeTest.QNameTest>(s.test).qName
        assertEquals("", name.namespaceURI)
        assertEquals("", name.prefix)
        assertEquals("para", name.localPart)

        assertEquals(1, s.predicates.size)
        val p = assertIs<FunctionCall>(s.predicates.single())

        assertEquals(0, p.args.size)
        assertEquals(QName("last"), p.name)
    }

    @Test
    fun testParaGrandChildren() {
        val expr = XPathExpression("*/para")
        assertEquals("*/para", expr.test)

        val e = assertIs<LocationPath>(expr.expr)
        assertFalse(e.rooted)
        assertEquals(2, e.steps.size)

        run {
            val s = e.steps[0]
            assertEquals(Axis.CHILD, s.axis)
            assertEquals(0, s.predicates.size)
            assertIs<NodeTest.AnyNameTest>(s.test)
        }

        run {
            val s = e.steps[1]
            assertEquals(Axis.CHILD, s.axis)
            assertEquals(0, s.predicates.size)
            val name = assertIs<NodeTest.QNameTest>(s.test).qName
            assertEquals("", name.namespaceURI)
            assertEquals("", name.prefix)
            assertEquals("para", name.localPart)
        }

    }

    @Test
    fun testSectionInDocChapter() {
        val expr = XPathExpression("/doc/chapter[5]/section[2]")
        assertEquals("/doc/chapter[5]/section[2]", expr.test)

        val e = assertIs<LocationPath>(expr.expr)
        assertTrue(e.rooted)
        assertEquals(3, e.steps.size)

        run {
            val s = e.steps[0]
            assertEquals(Axis.CHILD, s.axis)
            assertEquals(0, s.predicates.size)
            val name = assertIs<NodeTest.QNameTest>(s.test).qName
            assertEquals("", name.namespaceURI)
            assertEquals("", name.prefix)
            assertEquals("doc", name.localPart)
        }

        run {
            val s = e.steps[1]
            assertEquals(Axis.CHILD, s.axis)
            assertEquals(1, s.predicates.size)
            val name = assertIs<NodeTest.QNameTest>(s.test).qName
            assertEquals("", name.namespaceURI)
            assertEquals("", name.prefix)
            assertEquals("chapter", name.localPart)
            val p = assertIs<NumberLiteral>(s.predicates.single())
            assertEquals(5L, p.value)
        }

        run {
            val s = e.steps[2]
            assertEquals(Axis.CHILD, s.axis)
            assertEquals(1, s.predicates.size)
            val name = assertIs<NodeTest.QNameTest>(s.test).qName
            assertEquals("", name.namespaceURI)
            assertEquals("", name.prefix)
            assertEquals("section", name.localPart)
            val p = assertIs<NumberLiteral>(s.predicates.single())
            assertEquals(2L, p.value)
        }

    }

    @Test
    fun testChapterParaDescendants() {
        val expr = XPathExpression("chapter//para")
        assertEquals("chapter//para", expr.test)
        val e = assertIs<LocationPath>(expr.expr)
        assertFalse(e.rooted)
        assertEquals(2, e.steps.size)

        run {
            val s = e.steps[0]
            assertEquals(Axis.CHILD, s.axis)
            assertEquals(0, s.predicates.size)
            val name = assertIs<NodeTest.QNameTest>(s.test).qName
            assertEquals("", name.namespaceURI)
            assertEquals("", name.prefix)
            assertEquals("chapter", name.localPart)
        }

        run {
            val s = e.steps[1]
            assertEquals(Axis.DESCENDANT, s.axis)
            assertEquals(0, s.predicates.size)
            val name = assertIs<NodeTest.QNameTest>(s.test).qName
            assertEquals("", name.namespaceURI)
            assertEquals("", name.prefix)
            assertEquals("para", name.localPart)
        }

    }

    @Test
    fun testParaDescendants() {
        val expr = XPathExpression("//para")
        assertEquals("//para", expr.test)
        val e = assertIs<LocationPath>(expr.expr)
        assertTrue(e.rooted)
        assertEquals(1, e.steps.size)

        val s = e.steps[0]
        assertEquals(Axis.DESCENDANT_OR_SELF, s.axis)
        assertEquals(0, s.predicates.size)
        val name = assertIs<NodeTest.QNameTest>(s.test).qName
        assertEquals("", name.namespaceURI)
        assertEquals("", name.prefix)
        assertEquals("para", name.localPart)

    }

    @Test
    fun testAnyOlistItem() {
        val expr = XPathExpression("//olist/item")
        assertEquals("//olist/item", expr.test)
        val e = assertIs<LocationPath>(expr.expr)
        assertTrue(e.rooted)
        assertEquals(2, e.steps.size)

        run {
            val s = e.steps[0]
            assertEquals(Axis.DESCENDANT_OR_SELF, s.axis)
            assertEquals(0, s.predicates.size)
            val name = assertIs<NodeTest.QNameTest>(s.test).qName
            assertEquals("", name.namespaceURI)
            assertEquals("", name.prefix)
            assertEquals("olist", name.localPart)
        }

        run {
            val s = e.steps[1]
            assertEquals(Axis.CHILD, s.axis)
            assertEquals(0, s.predicates.size)
            val name = assertIs<NodeTest.QNameTest>(s.test).qName
            assertEquals("", name.namespaceURI)
            assertEquals("", name.prefix)
            assertEquals("item", name.localPart)
        }

    }

    @Test
    fun testContextNode() {
        val expr = XPathExpression(".")
        assertEquals(".", expr.test)
        val e = assertIs<LocationPath>(expr.expr)
        assertFalse(e.rooted)
        assertEquals(1, e.steps.size)
        val s = e.steps.single()
        assertEquals(Axis.SELF, s.axis)
        assertEquals(0, s.predicates.size)
        assertIs<NodeTest.AnyNameTest>(s.test)
    }

    @Test
    fun testContextParaDescendants() {
        val expr = XPathExpression(".//para")
        assertEquals(".//para", expr.test)
        val e = assertIs<LocationPath>(expr.expr)
        assertFalse(e.rooted)
        assertEquals(2, e.steps.size)

        run {
            val s = e.steps[0]
            assertEquals(Axis.SELF, s.axis)
            assertEquals(0, s.predicates.size)
            assertIs<NodeTest.AnyNameTest>(s.test)
        }

        run {
            val s = e.steps[1]
            assertEquals(Axis.DESCENDANT, s.axis)
            assertEquals(0, s.predicates.size)
            val name = assertIs<NodeTest.QNameTest>(s.test).qName
            assertEquals(QName("para"), name)
        }

    }

    @Test
    fun testUnionPath() {
        testPath(".//myNS:t | .//myNS:u", "myNS" to "myNS") {
            assertBinary(Operator.UNION) {
                assertLeft<LocationPath> {
                    assertPath {
                        assertStepSelf()
                        assertStep(Axis.DESCENDANT, QName("myNS", "t", "myNS"))
                    }
                }
                assertRight<LocationPath> {
                    assertPath {
                        assertStepSelf()
                        assertStep(Axis.DESCENDANT, QName("myNS", "u", "myNS"))
                    }
                }
            }
        }
    }

}
