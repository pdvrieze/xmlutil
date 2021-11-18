/*
 * Copyright (c) 2021.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.github.pdvrieze.formats.xmlschema.test

import org.junit.jupiter.api.Nested
import kotlin.test.Test
import kotlin.test.assertNotNull

class TestBoeingData {

    @Nested
    inner class Ipo1: ResourceTestBase("boeingData/ipo1") {
        @Test
        fun testDeserializeIpo() {
            val deserialized = deserializeXsd("ipo.xsd")
            assertNotNull(deserialized)
        }
    }

    @Nested
    inner class Ipo2: ResourceTestBase("boeingData/ipo2") {

        @Test
        fun testDeserializeIpo() {
            val deserialized = deserializeXsd("ipo.xsd")
            assertNotNull(deserialized)
        }

        @Test
        fun testDeserializeAddress() {
            val deserialized = deserializeXsd("address.xsd")
            assertNotNull(deserialized)
        }
    }

    @Nested
    inner class Ipo3: ResourceTestBase("boeingData/ipo3") {

        @Test
        fun testDeserializeIpo() {
            val deserialized = deserializeXsd("ipo.xsd")
            assertNotNull(deserialized)
        }

        @Test
        fun testDeserializeAddress() {
            val deserialized = deserializeXsd("address.xsd")
            assertNotNull(deserialized)
        }

        @Test
        fun testDeserializeItematt() {
            val deserialized = deserializeXsd("itematt.xsd")
            assertNotNull(deserialized)
        }
    }

    @Nested
    inner class Ipo4: ResourceTestBase("boeingData/ipo4") {

        @Test
        fun testDeserializeIpo() {
            val deserialized = deserializeXsd("ipo.xsd")
            assertNotNull(deserialized)
        }

        @Test
        fun testDeserializeAddress() {
            val deserialized = deserializeXsd("address.xsd")
            assertNotNull(deserialized)
        }

        @Test
        fun testDeserializeItematt() {
            val deserialized = deserializeXsd("itematt.xsd")
            assertNotNull(deserialized)
        }
    }

    @Nested
    inner class Ipo5: ResourceTestBase("boeingData/ipo5") {

        @Test
        fun testDeserializeIpo() {
            val deserialized = deserializeXsd("ipo.xsd")
            assertNotNull(deserialized)
        }

        @Test
        fun testDeserializeAddress() {
            val deserialized = deserializeXsd("address.xsd")
            assertNotNull(deserialized)
        }

        @Test
        fun testDeserializeItematt() {
            val deserialized = deserializeXsd("itematt.xsd")
            assertNotNull(deserialized)
        }
    }

    @Nested
    inner class Ipo6: ResourceTestBase("boeingData/ipo6") {

        @Test
        fun testDeserializeIpo() {
            val deserialized = deserializeXsd("ipo.xsd")
            assertNotNull(deserialized)
        }

        @Test
        fun testDeserializeAddress() {
            val deserialized = deserializeXsd("address.xsd")
            assertNotNull(deserialized)
        }

        @Test
        fun testDeserializeItematt() {
            val deserialized = deserializeXsd("itematt.xsd")
            assertNotNull(deserialized)
        }
    }

}
