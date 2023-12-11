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
import nl.adaptivity.xmlutil.SimpleNamespaceContext
import nl.adaptivity.xmlutil.XmlEvent
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@DslMarker
annotation class PathTestDsl

fun testPath(path: String, vararg namespaces: Pair<String, String>, test: TestContext.() -> Unit) {
    val nsContext = SimpleNamespaceContext(namespaces.map { (p, ns) -> XmlEvent.NamespaceImpl(p, ns) })
    val expr = XPathExpression(path, nsContext)
    TestContextImpl(path, expr).apply(test)
}

@OptIn(XPathInternal::class)
@PathTestDsl
interface TestContext {
    val expr: Expr
    fun <T: Expr> nestedContext(expr: T):ExprContext<T>
}

@OptIn(XPathInternal::class)
interface ExprContext<T: Expr>: TestContext {
    override val expr: T
}

@OptIn(XPathInternal::class)
@PathTestDsl
internal class PathContext(val path: LocationPath) {
    private fun getStep(): Step {
        assertTrue(stepCount < path.steps.size, "Expected at least $stepCount steps, but found only ${path.steps.size}")
        return path.steps[stepCount++]
    }

    fun assertStepSelf() {
        val step = getStep()
        assertEquals(Axis.SELF, step.axis)
        assertEquals(NodeTest.NodeTypeTest(NodeType.NODE), step.test)
        assertEquals(0, step.predicates.size)
    }

    fun assertStepDescendant() {
        assertStep(Axis.DESCENDANT_OR_SELF, NodeType.NODE)
    }

    fun assertStep(axis: Axis, qName: QName) {
        assertStep(axis, qName) {}
    }

    fun assertStep(axis: Axis, nodeType: NodeType) {
        assertStep(axis, nodeType) {}
    }

    fun assertStep(s: String) = assertStep(Axis.CHILD, QName(s))

    inline fun assertStep(s: String, test: StepContext.() -> Unit) {
        assertStep(QName(s), test)
    }

    inline fun assertStep(name: QName, test: StepContext.() -> Unit) {
        assertStep(Axis.CHILD, name, test)
    }

    inline fun assertStep(axis: Axis, qName: QName, test: StepContext.() -> Unit) {
        val step = getStep()
        assertEquals(axis, step.axis)
        val t = assertIs<NodeTest.QNameTest>(step.test)
        assertEquals(qName, t.qName)
        val ctx = StepContext(step)
        ctx.test()
        assertEquals(ctx.predicateCount, step.predicates.size, "Expected ${ctx.predicateCount} predicates but found ${step.predicates.size}")
    }

    inline fun assertStep(axis: Axis, nodeType: NodeType, test: StepContext.() -> Unit) {
        val step = getStep()
        assertEquals(axis, step.axis)
        val t = assertIs<NodeTest.NodeTypeTest>(step.test)
        assertEquals(nodeType, t.type)

        val ctx = StepContext(step)
        ctx.test()
        assertEquals(ctx.predicateCount, step.predicates.size, "Expected ${ctx.predicateCount} predicates but found ${step.predicates.size}")
    }

    inline fun <reified T: NodeTest> assertStep(axis: Axis, test: StepContext.(T) -> Unit) {
        val step = getStep()
        assertEquals(axis, step.axis)
        val ctx = StepContext(step)
        ctx.test(assertIs<T>(step.test))
        assertEquals(ctx.predicateCount, step.predicates.size, "Expected ${ctx.predicateCount} predicates but found ${step.predicates.size}")
    }

    inline fun <reified T: NodeTest> assertStep(test: StepContext.(T) -> Unit) {
        assertStep<T>(Axis.CHILD, test)
    }

    fun assertStep(axis: Axis, localName: String) {
        assertStep(axis, QName(localName))
    }


    internal var stepCount = 0
        private set
}

@OptIn(XPathInternal::class)
internal class StepContext(val step: Step) {
    private fun getPredicate(): ExprContext<Expr> {
        assertTrue(predicateCount < step.predicates.size, "Expected at least $predicateCount steps, but found only ${step.predicates.size}")
        return ExprContextImpl(step.predicates[predicateCount++])
    }

    inline fun assertPredicate(test: TestContext.() -> Unit) {
        val pred = getPredicate()
        pred.test()
    }

    var predicateCount: Int = 0
        private set


}

@OptIn(XPathInternal::class)
@PathTestDsl
internal inline fun ExprContext<LocationPath>.assertPath(rooted: Boolean? = null, test: PathContext.() -> Unit) {
    if (rooted!=null) assertEquals(rooted, expr.rooted)
    val ctx = PathContext(expr).apply(test)
    assertEquals(ctx.stepCount, expr.steps.size)
}

@OptIn(XPathInternal::class)
internal class BinaryContext(private val expr: ExprContext<BinaryExpr>) {
    inline fun <reified T: Expr> assertLeft(test: ExprContext<T>.() -> Unit) {
        expr.nestedContext(assertIs<T>(expr.expr.left)).apply(test)
    }

    inline fun <reified T: Expr> assertRight(test: ExprContext<T>.() -> Unit) {
        expr.nestedContext(assertIs<T>(expr.expr.right)).apply(test)
    }
}

@OptIn(XPathInternal::class)
internal inline fun <reified T1 : Expr, reified T2 : Expr> TestContext.assertBinary(
    op: Operator,
    left: ExprContext<T1>.() -> Unit,
    right: ExprContext<T2>.() -> Unit
) {
    val bin = assertIs<BinaryExpr>(expr)
    assertEquals(op, bin.operator)

    nestedContext(assertIs<T1>(bin.left)).apply(left)
    nestedContext(assertIs<T2>(bin.right)).apply(right)
}

@OptIn(XPathInternal::class)
internal fun TestContext.assertPath(test: PathContext.() -> Unit) {
    val path = assertIs<LocationPath>(expr)
    val c = PathContext(path)
    c.test()
    assertEquals(c.stepCount, path.steps.size)
}

@OptIn(XPathInternal::class)
internal fun TestContext.assertNumber(value: Long) {
    val n = assertIs<NumberLiteral>(expr)
    assertEquals(value, n.value)
}

internal fun TestContext.assertFunctionCall(name: String) {
    assertFunctionCall(QName(name))
}
@OptIn(XPathInternal::class)
internal fun TestContext.assertFunctionCall(name: QName) {
    val f = assertIs<FunctionCall>(expr)
    assertEquals(name, f.name)
    assertEquals(0, f.args.size)
}

@OptIn(XPathInternal::class)
internal inline fun TestContext.assertBinary(
    op: Operator,
    test: BinaryContext.() -> Unit,
) {
    val bin = assertIs<BinaryExpr>(expr)
    assertEquals(op, bin.operator)
    BinaryContext(nestedContext(bin)).apply(test)
}

@OptIn(XPathInternal::class)
private class ExprContextImpl<T: Expr>(override val expr: T) : ExprContext<T> {
    override fun <T : Expr> nestedContext(expr: T): ExprContext<T> {
        return ExprContextImpl(expr)
    }
}

@OptIn(XPathInternal::class)
private class TestContextImpl(private val path: String, private val expression: XPathExpression) : TestContext {
    override val expr: Expr get() = expression.expr
    override fun <T : Expr> nestedContext(expr: T): ExprContext<T> {
        return ExprContextImpl(expr)
    }
}
