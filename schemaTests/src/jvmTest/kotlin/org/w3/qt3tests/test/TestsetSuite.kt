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
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.Parameter
import org.junit.jupiter.params.ParameterizedClass
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.w3.qt3tests.resolved.ResolvedQt3TestCase
import org.w3.qt3tests.resolved.ResolvedQt3TestSet

@ParameterizedClass
@MethodSource("suite")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
class TestsetSuite : AbstractTestSetSuite() {

    @Parameter
    lateinit var testSet: ResolvedQt3TestSet

    @ParameterizedTest
    @MethodSource("tests")
    fun testCase(test: ResolvedQt3TestCase) {
        testEvalTestCaseImpl(test)
    }

    fun tests(): List<Named<ResolvedQt3TestCase>> {
        return testSet.testCases.asSequence()
            .filter {
                it.test.expr.getOrNull() is XPathExpression
            }
            .filter { "namespace-axis" !in it.neededFeatures() }
            .map { Named.named(it.name, it) }
            .toList()
    }

    companion object {
        @JvmStatic
        fun suite(): List<Named<ResolvedQt3TestSet>> {
            return TestParseCatalog.getTestSetSpecs(false).mapNotNull {
                val testSet = it.payload.resolve()
                when {
                    testSet.testCases.none { it.test.expr.getOrNull() is XPathExpression } -> null
                    else -> Named.named(it.name, testSet)
                }
            }
        }
    }
}
