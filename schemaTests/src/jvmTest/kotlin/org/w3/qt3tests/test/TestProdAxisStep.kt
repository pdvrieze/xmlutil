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

package org.w3.qt3tests.test

import io.github.pdvrieze.formats.xpath.XPathExpression
import io.github.pdvrieze.formats.xpath.data.XdmNode
import io.github.pdvrieze.formats.xpath.data.XdmSequence
import io.github.pdvrieze.formats.xpath.data.XdmValue
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import nl.adaptivity.xmlutil.dom2.Document
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.opentest4j.AssertionFailedError
import org.w3.qt3tests.resolved.ResolvedQt3TestCase
import org.w3.qt3tests.resolved.assertions.AssertionResult
import org.w3.qt3tests.test.TestParseCatalog.Companion.getTestSetSpec
import org.w3.qt3tests.test.TestParseCatalog.Companion.parseTestSetImpl
import kotlin.test.Test

@OptIn(XPathInternal::class)
class TestProdAxisStep {

    @Test
    fun testParse() {

        val testSet = parseTestSetImpl(getTestSetSpec("prod-AxisStep"))
    }

    @Test
    fun testAxes001_1() {
        val evalValue = testEvalTestCaseImpl(getTestCase("Axes001-1"))
    }

    @Test
    fun testAxes001_3() {
        val evalValue = testEvalTestCaseImpl(getTestCase("Axes001-3"))
    }

    @Test
    fun testAxes003_4() {
        val evalValue = testEvalTestCaseImpl(getTestCase("Axes003-4"))
    }

    @Test
    fun testAxes008_3() {
        val evalValue = testEvalTestCaseImpl(getTestCase("Axes008-3"))
    }

    @Test
    fun testAxes036_2() {
        val evalValue = testEvalTestCaseImpl(getTestCase("Axes036-2"))
    }

    @ParameterizedTest
    @MethodSource("getTestCases")
    fun testEvalTestCase(testCase: ResolvedQt3TestCase) {
        testEvalTestCaseImpl(testCase)
    }

    @IgnorableReturnValue
    private fun testEvalTestCaseImpl(testCase: ResolvedQt3TestCase): Result<XdmValue> {
        val environment = testCase.environment?.getOrThrow()
        val contextDoc: Document? = environment?.run {
            val s = sources.filter { it.role == "." }
            if (s.isEmpty()) {
                null
            } else {
                s.single().content
            }
        }

        val context = contextDoc?.let { XdmNode(it.documentElement!!) } ?: XdmSequence.EMPTY

        val testExpression = testCase.test.expr.getOrThrow() as XPathExpression
        val evalResult = runCatching { testExpression.eval(context) }

        if (testCase.result != null) {
            for (a in testCase.result.assertions) {
                val verifyResult = a.verify(evalResult)
                if (verifyResult is AssertionResult.Failure) {
                    if (evalResult.isFailure) throw AssertionFailedError("Unexpected failure", evalResult.exceptionOrNull())
                    else throw AssertionFailedError("Unexpected assertion failure for result: ${verifyResult.error}", verifyResult.cause)
                }
            }
        } else if (evalResult.isFailure) throw evalResult.exceptionOrNull()!!
        return evalResult
    }


    companion object {
        fun getTestCase(name: String): ResolvedQt3TestCase {
            val testCases = getTestCases()
            return testCases.single { it.name == name }.payload
        }

        @JvmStatic
        fun getTestCases(): List<Named<ResolvedQt3TestCase>> {
            val testSet = parseTestSetImpl(getTestSetSpec("prod-AxisStep"))
            return testSet.testCases.asSequence()
                .filter {
                    it.test.expr.getOrNull() is XPathExpression
                }
                .filter { "namespace-axis" !in it.neededFeatures() }
                .map { named(it.name, it) }
                .toList()
        }
    }

}
