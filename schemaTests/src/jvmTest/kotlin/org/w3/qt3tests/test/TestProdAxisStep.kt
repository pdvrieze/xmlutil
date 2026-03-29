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

import io.github.pdvrieze.formats.xpath.data.ErrorCodes
import io.github.pdvrieze.formats.xpath.data.EvaluationException
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import org.junit.jupiter.api.Named
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.w3.qt3tests.resolved.ResolvedQt3TestCase
import org.w3.qt3tests.test.TestParseCatalog.Companion.getTestSetSpec
import org.w3.qt3tests.test.TestParseCatalog.Companion.parseTestSetImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(XPathInternal::class)
class TestProdAxisStep : AbstractTestSetSuite() {

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

    @Test
    fun testK2Axes43() {
        val evalValue = testEvalTestCaseImpl(getTestCase("K2-Axes-43"))
        val e = assertIs<EvaluationException>(evalValue.exceptionOrNull())
        assertEquals(ErrorCodes.XPDY0002_ABSENT_DYNAMIC_CONTEXT, e.errorCode)
    }

    @Test
    fun testCurrent() {
        val evalValue = testEvalTestCaseImpl(getTestCase("K2-Axes-50"))
    }

    @ParameterizedTest
    @MethodSource("getTestCases")
    fun testEvalTestCase(testCase: ResolvedQt3TestCase) {
        testEvalTestCaseImpl(testCase)
    }

    companion object : CompanionBase("prod-AxisStep") {
        @JvmStatic
        override fun getTestCases(): List<Named<ResolvedQt3TestCase>> {
            return getTestCases(testSetName)
        }
    }

}

