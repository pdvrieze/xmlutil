/*
 * Copyright (c) 2023-2026.
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

package io.github.pdvrieze.formats.xpath.test

import io.github.pdvrieze.formats.xpath.XPathExpression
import io.github.pdvrieze.formats.xpath.XPathVersion.*
import io.github.pdvrieze.formats.xpath.eval.data.XdmAtomic
import io.github.pdvrieze.formats.xpath.eval.data.XdmSequence
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunction
import io.github.pdvrieze.formats.xpath.impl.*
import io.github.pdvrieze.formats.xpath.impl.token.Axis
import io.github.pdvrieze.formats.xpath.impl.token.NodeType
import io.github.pdvrieze.formats.xpath.impl.token.Operator
import io.github.pdvrieze.xml.schematypes.values.XsdBoolean
import io.github.pdvrieze.xml.schematypes.values.XsdLong
import io.github.pdvrieze.xmlutil.testutil.assertQNameEquivalent
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.toCName
import kotlin.test.*

@OptIn(XPathInternal::class, NeedsXPath2::class, NeedsXPath3_0::class, NeedsXPath3_1::class)
class XPathTest {

    @Test
    fun testOperatorLogic() {
        testPath("2 or 3 and 5 or 6") {
            assertOperator(Operator.OR) {
                assertCount(3)
                assertOperand(0) {
                    assertNumber(2)
                }
                assertOperand(1) {
                    assertOperator(Operator.AND) {
                        assertCount(2)
                        assertOperand(0) {
                            assertNumber(3)
                        }
                        assertOperand(1) {
                            assertNumber(5)
                        }
                    }
                }
                assertOperand(2) { assertNumber(6) }
            }
        }
    }


    @Test
    fun testSequence() {
        val expr = testPath("1,2,3") {}
        val evalResult = assertIs<XdmSequence<*>>(expr.eval())

        assertEquals(listOf(XsdLong(1), XsdLong(2), XsdLong(3)), evalResult.map { (it as XdmAtomic<*>).value })
    }

    @Test
    fun testNeq() {
        val expr = testPath("fn:false() != fn:false()", "fn" to BuiltinFunction.FN_NAMESPACE) {}
        val evalResult = assertIs<XdmAtomic<XsdBoolean>>(expr.eval())
        assertEquals(false, evalResult.value.value)
    }

    @Test
    fun testEq() {
        val expr = testPath("fn:false() = fn:false()", "fn" to BuiltinFunction.FN_NAMESPACE) {}
        val evalResult = assertIs<XdmAtomic<XsdBoolean>>(expr.eval())
        assertEquals(true, evalResult.value.value)
    }

    @Test
    fun testForExprWithout24() {
        testPath("for \$fn:name in (1, 1) return \$fn:name") {}
    }

    @Test
    fun testStringConcat() {
        testPath(XPath3_0, "matches(\$d||\$d)") {}
    }


    @Test
    fun testWildcardLocalname() {
        testPath("name((//@xml:*)[1])") {}
    }

    @Test
    fun testLetExpr() {
        testPath("""let ${'$'}index-of-node := function(${'$'}seqParam as node()*, ${'$'}srchParam as node()) as xs:integer* 
                                    { filter( 1 to count(${'$'}seqParam), function(${'$'}this as xs:integer) as xs:boolean
                                              {${'$'}seqParam[${'$'}this] is ${'$'}srchParam} ) },
            ${'$'}nodes := /*/*,
            ${'$'}perm := (${'$'}nodes[1], ${'$'}nodes[2], ${'$'}nodes[3], ${'$'}nodes[1], ${'$'}nodes[2], ${'$'}nodes[4], ${'$'}nodes[2], ${'$'}nodes[1]) 
            return ${'$'}index-of-node(${'$'}perm, ${'$'}nodes[2])""".trimIndent()) {}
    }

    @Test
    fun testPlaceholderParam() {
        testPath("filter((\"apple\", \"pear\", \"apricot\", \"advocado\", \"orange\"),starts-with(?, \"a\"))") {}
    }

    @Test
    fun testHigherOrder() {
        testPath("(contains-token#2, starts-with#2, ends-with#2)!.(\"abc def\", \"def\")") {}
    }

    @Test
    fun testInequality() {
        testPath("fn:compare(\"database\", \"DÃTABASE\", \"http://www.w3.org/2013/collation/UCA?lang=en;strength=secondary\") != 0") {}
    }

    @Test
    fun testEmptySequenceParam2() {
        testPath(XPath2_0, "codepoints-to-string((),())") {}
    }

    @Test
    fun testEmptySequenceParam3() {
        testPath(XPath3_0, "codepoints-to-string((),())") {}
    }

    @Test
    fun testEmptySequenceParam31() {
        testPath(XPath3_1, "codepoints-to-string((),())") {}
    }

    @Test
    fun testInlineExpr() {
        testPath("let \$func := function(\$a,\$b,\$c) { \$a + \$b + \$c }, \$args := [ 1, 2, 3 ] return apply(\$func, \$args)") {

        }
    }


    @Test
    fun testCast() {
        testPath("\$result?1?(xs:double(\"1.0\") cast as xs:decimal) eq 3") {
            val outer = assertIs<BinaryExpr>(expr)
            assertEquals(Operator.VAL_EQ,outer.operator)
            val lookup1 = assertIs<LookupExpr>(outer.left)
            val lookup2 = assertIs<LookupExpr>(lookup1.context)
            val varRef = assertIs<VariableRef>(lookup2.context)
            val key2 = assertIs<LookupExpr.IntegerKey>(lookup2.key)
            assertEquals(1, key2.value)

            val key1 = assertIs<LookupExpr.ParenKey>(lookup1.key)
            val castExpr = assertIs<CastExpr>(key1.params.singleOrNull())

            val right1 = assertIs<LiteralExpr<*>>(outer.right)

        }
    }

    @Test
    fun testParseNodeArgs() {
        testPath("\$result/*[1][self::a][not(child::node())]") {

        }
    }

    @Test
    fun testNegative() {
        testPath("-42") {
            assertEquals(UnaryExpr.Minus(LongLiteral(42)), expr)
        }
    }

    @Test
    fun testMapConstructor() {
        testPath("map{}") {
            val map = assertIs<MapConstructor>(expr)
            assertEquals(0, map.entries.size)
        }
    }

    @Test
    fun testArrayWithEmptySequence() {
        testPath("[()]") {
            val array = assertIs<ArrayConstructor.Square>(expr)
            assertIs<EmptySequenceExpr>(array.values.singleOrNull())
        }
    }


    @Test
    fun testDynamicFuncCall() {
        val expr = XPathExpression("\$result(\"output\") instance of document-node()")
        val e = assertIs<InstanceOfExpr>(expr.expr)

    }

    @kotlin.test.Test
    fun testInlineFunction() {
        testPath("function(\$in as xs:decimal*) as xs:decimal {sum(\$in, 0.0)}(xs:NMTOKENS('1 1.2 1.3 1.4')!xs:untypedAtomic(.))") {}
    }

    @Test
    fun testPathWithDynamicFuncCall() {
        val expr = XPathExpression("exists(\$result(\"output\")/out)")
        val e = assertIs<StaticFunctionCall>(expr.expr)
        val p = assertIs<LocationPath>(e.args.singleOrNull())
        assertEquals(2, p.steps.size)
        val step = assertIs<AxisStep>(p.steps[1])
        assertEquals(Axis.CHILD, step.axis)
        val t = assertIs<NodeTest.QNameTest>(step.test)
        assertEquals(QName("out"), t.qName)
    }

    @Test
    fun testParseNoParenSequence() {
        val expr = XPathExpression("\"http://www.w3.org/\"\"2005/xpath-functions\", \"http://www.w3.org/XML/1998/namespace\"")
        val e = assertIs<SequenceExpr>(expr.expr)
        assertEquals(2, e.elements.size)

        assertEquals(StringLiteral("http://www.w3.org/\"2005/xpath-functions"), e.elements[0])
        assertEquals(StringLiteral("http://www.w3.org/XML/1998/namespace"), e.elements[1])
    }

    @Test
    fun testNoTextInSchemaElement() {
        val e = assertFailsWith<IllegalArgumentException> {
            val _ = XPathExpression("schema-element(\"quotesAreNotAllowed\")")
        }
        assertContains(e.message!!, "Expected NCName, found '\"'")
    }


    @Test
    fun testParseParenSequence() {
        val expr = XPathExpression("(1,2,3)")
        val p = assertIs<ParenExpr>(expr.expr)
        val e = assertIs<SequenceExpr>(p.expr)
        assertEquals(3, e.elements.size)

        assertEquals(LongLiteral(1), e.elements[0])
        assertEquals(LongLiteral(2), e.elements[1])
        assertEquals(LongLiteral(3), e.elements[2])
    }

    @Test
    fun testMissingSelector() {
        val e = assertFailsWith<IllegalArgumentException> { val _ = XPathExpression.Serializer("child::") }

        assertContains(e.message ?: "", "Missing node test in ")

    }

    @Test
    fun testWSAfterAxis() {
        testPath("child:: imp:iid", "imp" to "imp") {
            assertPath {
                assertStep(QName("imp", "iid", "imp")) {}
            }
        }
    }

    @Test
    fun testMultipleContext() {
        testPath(".//./.") {
            assertPath {
                assertStepSelf()
                assertStepDescendant()
                assertStepSelf()
                assertStepSelf()
            }
        }
    }

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
                assertStep<NodeKindTest> { assertEquals(NodeType.TEXT, it.type) }
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
                        val funcCall: StaticFunctionCall = when (expr) {
                            is LocationPath -> {
                                val path = assertIs<LocationPath>(expr)
                                assertEquals(1, path.steps.size)
                                val filter = assertIs<FilterExpr>(path.steps[0])
                                assertEquals(0, filter.predicates.size)

                                assertIs<StaticFunctionCall>(filter.primaryExpr)

                            }

                            else -> assertIs<StaticFunctionCall>(expr, "Expected function call")
                        }
                        if (funcCall.args.isNotEmpty()) {
                            assertEquals(emptyList(), funcCall.args)
                        }
                        assertQNameEquivalent(QName("last"), funcCall.name)
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
                assertRooted()
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
                assertRooted()
                assertStepDescendant()
                assertStep("para")
            }
        }
    }

    @Test
    fun testAnyOlistItem() {
        val expr = XPathExpression("//olist/item")
        assertEquals("//olist/item", expr.xmlString)
        testPath("//olist/item") {
            assertPath {
                assertRooted()
                assertStep(Axis.DESCENDANT_OR_SELF, NodeType.ANY_NODE)
                assertStep("olist")
                assertStep("item")
            }
        }

    }

    @Test
    fun testDelim() {
        val e= assertFailsWith<IllegalArgumentException> { testPath("3div 5") {} }
        assertContains(e.message!!, "Missing delimiter before non-delimiting operator",)
    }

    @Test
    fun testDelim2() {
        testPath("for \$x in 65 to 75 return boolean(codepoints-to-string(\$x[. mod 2 = 0] to (\$x+9)[. mod 2 = 0]))") {}
    }

    @Test
    fun testDelim3() {
        testPath("empty((1 div 0))") {}
    }

    @Test
    fun testDelim4() {
        val e = assertFailsWith<IllegalArgumentException> { testPath("10 div3") {} }
        assertContains(e.message!!, "Multiple non-delimiting tokens succeeding each other")
    }

    @Test
    fun testDelim5() {
        testPath("\$result?1?a1 = \"string\"") {}
    }

    @Test
    fun testOperatorError() {
        // Per the leading-lone-slash rule this should not be valid

        val e = assertFailsWith<IllegalArgumentException> { testPath("/ * 5") {} }
        assertContains(e.message!!, "Trailing content in expression")
    }

    @Test
    fun testNoWSInQName() {
        val e = assertFailsWith<IllegalArgumentException> { testPath("*:(:hey:)ncname") {} }
        assertContains(e.message!!, "Expected NCName, found '('")
    }

    @Test
    fun testNoInvalidFunctionName() {
        val e = assertFailsWith<IllegalArgumentException> {
            testPath(":f()") {}
        }
        assertContains(e.message!!, "Expected NCName, found ':'")
    }

    @Test
    fun testLeadingDotDecimal() {
        testPath(".65535032") {}
    }

    @Test
    fun testNoInvalidNumber() {
        val e = assertFailsWith<IllegalArgumentException> {
            testPath(".0.1") {}
        }
        assertContains(e.message!!, "Trailing content in expression")
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

    @Test
    fun testEqualsInPredicate() {
        testPath("./fn:group[@nr=1]") {
            assertPath {
                assertStepSelf()

                assertStep<NodeTest.QNameTest> {
                    assertQNameEquivalent(QName("http://www.w3.org/2005/xpath-functions", "group", "fn"), it.qName)

                    assertPredicate {// @nr = 1
                        assertBinary(Operator.EQ) {
                            assertLeft<LocationPath> {
                                assertPath {
                                    assertStep(Axis.ATTRIBUTE, "nr")
                                }
                            }
                            assertRight<LongLiteral> {
                                assertEquals(1, expr.value)
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testComplexPath() {
        testPath("\$result/out/fn:analyze-string-result[1]/fn:match[1]/fn:group[@nr=1] = '/OPDH/'") {

        }
    }

    @Test
    fun testParseFnApply06() {
        testPath("apply(substring('flower', ?, ?), [ 3, 2 ])") {

        }
    }

    @Test
    fun testParamPlaceholders() {
        val p = "let \$f := function(\$ff as (function(item()) as item()), \$s as xs:string){\$ff(\$ff(\$s))} return\n" +
                "for-each((upper-case#1, lower-case#1, normalize-space#1, concat(?, '!')), \$f(?, ' Say NO! '))"

        testPath(p) {}
    }

    @Test
    fun testAttrPath() {
        testPath("/root/@attribute/fn:has-children()") {

        }
    }

    @Test
    fun testDoubleOpeningBraceInQName() {
        val e = assertFailsWith<IllegalArgumentException> {
            testPath(XPath3_0, "Q{{http://www.w3.org/2005/xpath-functions/math}pi()"){
                assertFunctionCall(QName("{http://www.w3.org/2005/xpath-functions/math", "pi"))
            }
        }

        testPath("for-each((1,4,9,16,25), Q{http://www.w3.org/2005/xpath-functions/math}sqrt#1)") {}
//        assertEquals(ErrorCodes.XPST0003_INVALID_GRAMMAR, e.errorCode)
    }

    @Test
    fun testVarNameWithPrefix() {
        testPath("1 eq (for \$xs:a in 1 return \$xs:a)") {}
    }

    @Test
    fun testNoReservedEmptySequence() {
        val e = assertFailsWith<IllegalArgumentException> {
            testPath("empty-sequence()") {
                assertFunctionCall("empty-sequence")
            }
        }
        assertContains(e.message!!, "empty-sequence is reserved")
    }

    @Test
    fun testNoReservedTypeswitch() {
        val e = assertFailsWith<IllegalArgumentException> {
            testPath("typeswitch()") {
                assertFunctionCall("typeswitch")
            }
        }
        assertContains(e.message!!, "typeswitch is reserved")
    }

    @Test
    fun testInstanceof132() {
        testPath("filter#2 instance of function(item()*, function(item()) as xs:boolean) as item()*") {
            val outer = assertIs<InstanceOfExpr>(expr)
            val left = assertIs<FunctionItem.NamedRef>(outer.expr)
            assertEquals(2, left.index)
            assertEquals("filter", left.name.toCName())
            val typeSeq = assertIs<SequenceType.ItemTypeSequence>(outer.sequenceType)
            assertEquals(SequenceType.OccurrenceType.SINGLE, typeSeq.occurrence)
            val functionType = assertIs<FunctionTypeTest.Typed>(typeSeq.itemType)
            assertEquals(2, functionType.paramTypes.size)
            assertEquals(SequenceType.ItemTypeSequence(ItemTypeTest.ItemTestTest, SequenceType.OccurrenceType.ANY), functionType.returnType)


        }
    }

    @Test
    fun testUnaryLookup014() {
        testPath("(['a', 'b', 'c'], ['b', 'c', 'd'], ['e', 'f', 'b'])[ ?* = 'c']") {
        }
    }
}
