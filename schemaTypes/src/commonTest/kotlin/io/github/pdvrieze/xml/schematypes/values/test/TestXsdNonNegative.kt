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

import io.github.pdvrieze.xml.schematypes.values.BigUnsignedInt
import kotlin.test.Test
import kotlin.test.assertEquals

class TestXsdNonNegative {

    @Test
    fun testSimpleInteger() {
        val bigInt = BigUnsignedInt("1234567890")
        assertEquals(1234567890, bigInt.toLong())
        assertEquals(1234567890, bigInt.toInt())
        assertEquals("1234567890", bigInt.xmlString)
    }

    @Test
    fun testParseSlightlyLarger() {
        val str = "12345678901234567890"
        val bigInt = BigUnsignedInt(str)
        assertEquals(str, bigInt.xmlString)
    }

    @Test
    fun testParseLong() {
        val value = 0x123456789L
        val str = value.toString()
        val bigInt = BigUnsignedInt(str)
        assertEquals(str, bigInt.xmlString)
    }

    @Test
    fun testLongInteger() {
        /*
                val str = buildString {
                    val r = Random(0xdeadbeef)
                    append(r.nextInt(1_000_000_000))
                    for (i in 1..5) append(r.nextInt(1_000_000_000).toString().padStart(9, '0'))
                }
        */
        val str = "697371550937419271808991527491782891510896728514817744"

        val bigInt = BigUnsignedInt(str)
        assertEquals(str, bigInt.xmlString)
    }

    @Test
    fun testGet() {
        val bigInt = BigUnsignedInt(0xDEADBEEFDEADBEEFuL, 12uL)

        assertEquals("DBEEF000", bigInt[0uL].toString(16).uppercase())
        assertEquals("DBEEFDEA", bigInt[1uL].toString(16).uppercase())
        assertEquals("DEA", bigInt[2uL].toString(16).uppercase())

    }

    @Test
    fun testGetBits() {
        val bigInt = BigUnsignedInt(0xDEADBEEFDEADBEEFuL, 12uL)

        assertEquals("FDEADBEE", bigInt.getBitIndex(16uL).toString(16).uppercase())
        assertEquals("B7DDFBD5", bigInt.getBitIndex(31uL).toString(16).uppercase())
        assertEquals("DEAD", bigInt.getBitIndex(60uL).toString(16).uppercase())
    }

    @Test
    fun testMultiplySimpleInteger() {
        val a = BigUnsignedInt(1234u).shl(47)
        val b = BigUnsignedInt(5678u).shl(13)

        val mult = a * b

        assertEquals(1234 * 5678, mult.shr(60).toLong())

        assertEquals((1234L * 5678L).shl(2), mult.shr(58).toLong())
        assertEquals((1234L * 5678L).shr(3), mult.shr(63).toLong())

        val div = mult.div(BigUnsignedInt(617uL, 60uL))
        assertEquals(11356uL, div.toULong())
    }

}
