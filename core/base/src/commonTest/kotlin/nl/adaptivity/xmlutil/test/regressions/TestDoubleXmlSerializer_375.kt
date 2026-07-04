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

import dev.mokkery.MockMode
import dev.mokkery.answering.returnsBy
import dev.mokkery.every
import dev.mokkery.matcher.capture.Capture
import dev.mokkery.matcher.capture.capture
import dev.mokkery.matcher.capture.get
import dev.mokkery.mock
import dev.mokkery.verify
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.util.XmlOnlyDoubleSerializer
import kotlin.test.BeforeTest
import kotlin.test.Test

class TestDoubleXmlSerializer_375 {

    lateinit var mockXmlWriter: XmlWriter
    lateinit var mockEncoder: Encoder

    private fun doTest(value: Double, expected: String) {
        XmlOnlyDoubleSerializer.serializeXML(mockEncoder, mockXmlWriter, value)
        verify {
            mockEncoder.encodeString(expected)
        }
    }

    @BeforeTest
    fun init() {
        mockXmlWriter = mock<XmlWriter>()
        mockEncoder = mock<Encoder>(MockMode.autoUnit) {
            val doubleArg = Capture.slot<Double>()
            every { encodeDouble(capture(doubleArg)) } returnsBy { encodeString(doubleArg.get().toString()) }
        }
    }

    @Test
    fun testDoubleInf() = doTest(Double.POSITIVE_INFINITY, "INF")

    @Test
    fun testDoubleMax() = doTest(Double.MAX_VALUE, Double.MAX_VALUE.toString())

    @Test
    fun testDoublePos() = doTest(23456.789, "23456.789")

    @Test
    fun testDoubleMin() = doTest(Double.MIN_VALUE, Double.MIN_VALUE.toString())

    @Test
    fun testDoubleZero() = doTest(0.0, "0.0")

    @Test
    fun testDoubleNegInf() = doTest(Double.NEGATIVE_INFINITY, "-INF")

    @Test
    fun testDoubleNegMax() = doTest(-Double.MAX_VALUE, (-Double.MAX_VALUE).toString())

    @Test
    fun testDoubleNeg() = doTest(-23456.789, "-23456.789")

    @Test
    fun testDoubleNegMin() = doTest(-Double.MIN_VALUE, (-Double.MIN_VALUE).toString())

    @Test
    fun testFloatNegZero() = doTest(-0.0, "-0.0")

}
