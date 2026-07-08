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

package nl.adaptivity.xmlutil.test.regressions

import dev.mokkery.verify
import nl.adaptivity.xmlutil.util.XmlFloatSerializer
import kotlin.test.BeforeTest
import kotlin.test.Test

class TestFloatSerializer_375 : TestFloatCommon() {
    @BeforeTest
    override fun init() {
        super.init()
    }

    private fun doTest(value: Float, expected: String) {
        XmlFloatSerializer.serialize(mockEncoder, value)
        verify {
            mockEncoder.encodeString(normalizeExpected(expected))
        }
    }

    @Test
    fun testFloatInf() = doTest(Float.POSITIVE_INFINITY, "INF")

    @Test
    fun testFloatMax() = doTest(Float.MAX_VALUE, Float.MAX_VALUE.toString())

    @Test
    fun testFloatPos() = doTest(23456.789f, "23456.79")

    @Test
    fun testFloatMin() = doTest(Float.MIN_VALUE, Float.MIN_VALUE.toString())

    @Test
    fun testFloatZero() = doTest(0f, "0.0")

    @Test
    fun testFloatNegInf() = doTest(Float.NEGATIVE_INFINITY, "-INF")

    @Test
    fun testFloatNegMax() = doTest(-Float.MAX_VALUE, (-Float.MAX_VALUE).toString())

    @Test
    fun testFloatNeg() = doTest(-23456.789f, "-23456.79")

    @Test
    fun testFloatNegMin() = doTest(-Float.MIN_VALUE, (-Float.MIN_VALUE).toString())

    @Test
    fun testFloatNegZero() = doTest(-0f, "-0.0")

}
