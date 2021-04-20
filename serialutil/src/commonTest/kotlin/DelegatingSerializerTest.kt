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

package nl.adaptivity.serialutil.test

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nl.adaptivity.serialutil.DelegatingSerializer
import kotlin.test.Test
import kotlin.test.assertEquals

internal class DelegatingSerializerTest {

    @Test
    fun deserialize() {
        val json = "{ \"propA\": \"bar\", \"propB\":777 }"
        val expected = ClassWithDelegatingSerializer("bar", "777")
        val actual = Json.decodeFromString<ClassWithDelegatingSerializer>(json)
        assertEquals(expected, actual)
    }

    @Test
    fun serialize() {
        val orig = ClassWithDelegatingSerializer("foo", "42")
        val serial = Json.encodeToString(orig)
        val expectedJson = "{\"propA\":\"foo\",\"propB\":42}"
        assertEquals(expectedJson, serial)
    }

    @Serializable(with = ClassWithDelegatingSerializer.Companion::class)
    data class ClassWithDelegatingSerializer(val a: String, val b: String) {

        @Serializable
        internal class SerialDelegate(val propA: String, val propB: Int)

        companion object: DelegatingSerializer<ClassWithDelegatingSerializer, SerialDelegate>(SerialDelegate.serializer()) {
            override fun fromDelegate(delegate: SerialDelegate): ClassWithDelegatingSerializer {
                return ClassWithDelegatingSerializer(delegate.propA, delegate.propB.toString())
            }

            override fun ClassWithDelegatingSerializer.toDelegate(): SerialDelegate {
                return SerialDelegate(a, b.toInt())
            }
        }
    }
}