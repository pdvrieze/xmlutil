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

import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import org.junit.jupiter.api.Named
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.w3.qt3tests.resolved.ResolvedQt3TestCase
import kotlin.test.Test

@OptIn(XPathInternal::class)
class TestCurrentPathSet : AbstractTestSetSuite() {

    @Test
    fun testCurrent() {
        val evalValue = testEvalTestCaseImpl(getTestCase("K-SeqBooleanFunc-6"))
    }

    @ParameterizedTest
    @MethodSource("getTestCases")
    fun testEvalTestCase(testCase: ResolvedQt3TestCase) {
        testEvalTestCaseImpl(testCase)
    }

    companion object : CompanionBase("fn-deep-equal") {
        @JvmStatic
        override fun getTestCases(): List<Named<ResolvedQt3TestCase>> {
            return getTestCases(testSetName)
        }
    }

}

