/*
 * Copyright (c) 2023.
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

package nl.adaptivity.serialutil.test

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import nl.adaptivity.serialutil.nonNullSerializer
import kotlin.test.Test
import kotlin.test.assertEquals

class NonNullSerializerTest() {

    @Test
    fun testNormalPrimitiveSerializer() {
        val actual = String.serializer().nonNullSerializer()
        assertEquals(String.serializer(), actual)
    }

    @Test
    fun testNullablePrimitiveSerializer() {
        val actual = String.serializer().nullable.nonNullSerializer()
        assertEquals(String.serializer(), actual)
    }

    @Test
    fun testNormalClassSerializer() {
        val actual = Foo.serializer().nonNullSerializer()
        assertEquals(Foo.serializer(), actual)
    }

    @Test
    fun testNullableClassSerializer() {
        val actual = Foo.serializer().nullable.nonNullSerializer()
        assertEquals(Foo.serializer(), actual)
    }

    @Serializable
    class Foo(val a: String, val b: Int)
}
