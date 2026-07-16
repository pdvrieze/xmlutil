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
import io.github.pdvrieze.xml.schematypes.values.XsdDecimal
import io.github.pdvrieze.xml.schematypes.values.XsdInt
import io.github.pdvrieze.xml.schematypes.values.instances.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class TestBigDecimal {
    @Test
    fun testExpToULong() {
        val sub = BigDecimal(1234567890, 10)
        val uLong = sub.toULong()
        assertEquals(12_345_678_900_000_000_000uL, uLong)
    }

    @Test
    fun testSimpleInteger() {
        val bigInt = BigDecimal("1234567890")
        assertEquals(1234567890, bigInt.toLong())
        assertEquals(1234567890, bigInt.toInt())
        assertEquals("1234567890", bigInt.xmlString)
    }

    @Test
    fun testLongInteger() {
        val l = -1917194577280790444L
        val bigInt = BigDecimal(l)
        assertEquals(l, bigInt.toLong())
        assertEquals(l.toString(), bigInt.xmlString)
    }


    @Test
    fun testMaxLongStr() {
        val bigInt = BigDecimal(Long.MAX_VALUE.toString())
        assertEquals(Long.MAX_VALUE, bigInt.toLong())
        assertEquals(Long.MAX_VALUE.toString(), bigInt.xmlString)
    }

    @Test
    fun testMinLongStr() {
        val bigInt = BigDecimal(Long.MIN_VALUE.toString())
        assertEquals(Long.MIN_VALUE, bigInt.toLong())
        assertEquals(Long.MIN_VALUE.toString(), bigInt.xmlString)
    }

    @Test
    fun testMaxULongStr() {
        val bigInt = BigDecimal(Long.MAX_VALUE.toString())
        assertEquals(Long.MAX_VALUE, bigInt.toLong())
        assertEquals(Long.MAX_VALUE.toString(), bigInt.xmlString)
    }

    @Test
    fun testSimpleInteger2() {
        val bigInt = BigDecimal("12345678901")
        assertEquals(12345678901, bigInt.toLong())
        assertEquals("12345678901", bigInt.xmlString)
    }

    @Test
    fun testExp1() {
        assertEquals(1234560, BigDecimal(123456, 1).toLong())
    }

    @Test
    fun testExp2() {
        assertEquals(12345600, BigDecimal(123456, 2).toLong())
    }

    @Test
    fun testExpM1() {
        assertEquals(123456, BigDecimal(1234560, -1).toLong())
    }

    @Test
    fun testExpM2() {
        assertEquals(123456, BigDecimal(12345600, -2).toLong())
    }


    @Test
    fun testNegDecimal() {
        val bigInt = BigDecimal(1234567890, -2)
        assertEquals("12345678.90", bigInt.xmlString)
    }

    @Test
    fun testPosDecimal() {
        val bigInt = BigDecimal(1234567890, 2)
        assertEquals("123456789000", bigInt.xmlString)
        assertEquals(123456789000L, bigInt.toLong())
    }

    @Test
    fun testAddLong() {
        val nanos = BigDecimal(563464971, 9)
        val sum = nanos + XsdInt(26)
        assertEquals("563464971000000026", sum.xmlString)
    }

    @Test
    fun testAddNegLong() {
        val nanos = BigDecimal(563464971, -9)
        val sum = nanos + XsdInt(26)
        assertEquals("26.563464971", sum.xmlString)
    }

    @Test
    fun testSub() {
        val base = BigDecimal(1234567890, 17)
        val sub = BigDecimal(1234567890, 13)
        val diff = base - sub
        assertEquals("123444443321100000000000000", diff.xmlString)
    }

    @Test
    fun testFixToString() {
        assertEquals("0.000012", BigDecimal(12, -6).xmlString)
    }

    @Test
    fun testDiv() {
        val main = BigDecimal(412, -2)
        val divisor = BigDecimal(1, 0)
        val result = main.divRem(divisor)
        assertEquals(4, result.quotient.toLong())
        assertEquals(12, result.remainder.exp10(2).toLong())
    }

    @Test
    fun testDivFrac() {
        val main = BigDecimal(412)
        val divisor = BigDecimal(3, 0)
        val result = main.div(divisor)
        assertEquals(BigDecimal(137333333333, -9), result)
    }

    @Test
    fun testIntLargerDiv() {
        val main = BigDecimal(Long.MAX_VALUE, -5)
        val divisor = BigDecimal(2, 0)
        val result = main.divRem(divisor)
        assertEquals(Long.MAX_VALUE / 200_000, result.quotient.toLong())
        assertEquals(Long.MAX_VALUE % 200_000, (result.remainder.exp10(5)).toLong())
    }

    private fun testBinaryOperator(
        a: Int,
        b: Int,
        expectedOperator: (Long, Long) -> Long,
        actualOperator: (BigDecimal, BigDecimal) -> XsdDecimal,
    ) {
        val expected = expectedOperator(a.toLong(), b.toLong())
        val bigA = BigDecimal(a)
        val bigB = BigDecimal(b)
        val bigResult = actualOperator(bigA, bigB)

        assertEquals(BigDecimal(expected), bigResult)
        assertEquals(expected, bigResult.toLong())
        assertEquals(expected.toString(), bigResult.xmlString)
    }

    private fun testMultiply(a: Int, b: Int) {
        testBinaryOperator(a, b, Long::times, BigDecimal::times)
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
    fun testAddPosPos() {
        testBinaryOperator(0x34151717, 0x7EADBEEF, Long::plus, { a, b -> a.plus(b) })
    }

    @Test
    fun testAddNegPos() {
        testBinaryOperator(-0x34158fe2, 0x7EAD2556, Long::plus, { a, b -> a.plus(b) })
    }

    @Test
    fun testAddPosNeg() {
        testBinaryOperator(0x34151717, -0x7EADBEEF, Long::plus, { a, b -> a.plus(b) })
    }

    @Test
    fun testAddNegNegs() {
        testBinaryOperator(-0x34FE4fe2, -0x3514BEEF, Long::plus, { a, b -> a.plus(b) })
    }


    private fun testDiv(x: Int, y: Int) {
        testBinaryOperator(x, y, Long::div, { a, b -> a.divRem(b).quotient })
        testBinaryOperator(x, y, Long::div, { a, b -> a.div(b).roundToInteger() })
    }

    @Test
    fun testDivPosPos() {
        testDiv(0x34151717, 0x7EADBEEF)
        testDiv(0x7EADBEEF, 0x34151717)
    }

    @Test
    fun testDivNegPos() {
        testDiv(-0x34158fe2, 0x7EAD2556)
        testDiv(-0x7EAD2556, 0x34158fe2)
    }

    @Test
    fun testDivPosNeg() {
        testDiv(0x34151717, -0x7EADBEEF)
        testDiv(0x7EADBEEF, -0x34151717)
    }

    @Test
    fun testDivNegNegs() {
        testDiv(-0x34FE4fe2, -0x3514BEEF)
        testDiv(-0x3514BEEF, -0x34FE4fe2)
    }

    @Test
    fun testToBigDecimal() {
        val bigInt = BigInt(-999999999999999999)
        val bigDecimal = bigInt.toBigDecimal()

        assertEquals("-999999999999999999", bigInt.xmlString)
        assertEquals(-999999999999999999, bigDecimal.toLong())
    }

}
