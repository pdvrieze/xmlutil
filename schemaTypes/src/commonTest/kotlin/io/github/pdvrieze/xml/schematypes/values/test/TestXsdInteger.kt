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

package io.github.pdvrieze.xml.schematypes.values.test

import io.github.pdvrieze.xml.schematypes.values.BigInt
import kotlin.test.Test
import kotlin.test.assertEquals

class TestXsdInteger {

    @Test
    fun testSimpleInteger() {
        val bigInt = BigInt("1234567890")
        assertEquals(1234567890, bigInt.toLong())
        assertEquals(1234567890, bigInt.toInt())
        assertEquals("1234567890", bigInt.xmlString)
    }

    private fun testBinaryOperator(
        a: Int,
        b: Int,
        expectedOperator: (Long, Long) -> Long,
        actualOperator: (BigInt, BigInt) -> BigInt,
    ) {
        val expected = expectedOperator(a.toLong(), b.toLong())
        val bigA = BigInt(a)
        val bigB = BigInt(b)
        val bigResult = actualOperator(bigA, bigB)

        assertEquals(BigInt(expected), bigResult)
        assertEquals(expected, bigResult.toLong())
        assertEquals(expected.toString(), bigResult.xmlString)
    }

    private fun testMultiply(a: Int, b: Int) {
        testBinaryOperator(a, b, Long::times, BigInt::times)
    }

    @Test
    fun testMultiplyPosNeg() {
        testMultiply(0x12345678, -0x7890abcd)
    }

    @Test
    fun testMultiplyPosPos() {
        testMultiply(0x21132149, 0x7edcba09)
    }

    @Test
    fun testMultiplyNegPos() {
        testMultiply(-0x4f27a954, 0x560cad3f)
    }

    @Test
    fun testMultiplyNegNeg() {
        testMultiply(-0x45782acb, -0x3bfd89a2)
    }

    @Test
    fun testAdd() {
        testBinaryOperator(0x34151717, 0x7EADBEEF, Long::plus, { a, b -> a.plus(b) })
    }

}
