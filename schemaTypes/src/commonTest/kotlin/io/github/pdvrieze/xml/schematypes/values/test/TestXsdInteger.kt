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

    @Test
    fun testMultiplyPosNeg() {
        val a = 0x12345678
        val b = -0x7890abcd
        val smallMul = a.toLong() * b.toLong()

        val bigA = BigInt(a)
        val bigB = BigInt(b)
        val bigMul = bigA * bigB

        assertEquals(BigInt(smallMul), bigMul)
        assertEquals(smallMul, bigMul.toLong())

        assertEquals(smallMul.toString(), bigMul.xmlString)
    }

}
