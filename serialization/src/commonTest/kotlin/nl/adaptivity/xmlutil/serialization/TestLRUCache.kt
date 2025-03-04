/*
 * Copyright (c) 2025.
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

package nl.adaptivity.xmlutil.serialization

import nl.adaptivity.xmlutil.serialization.impl.LRUCache
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestLRUCache {

    @Test
    fun testPutItems() {
        val cache = LRUCache<String, String>(40)
        for (v in DEFAULT_VALUES) {
            cache.put(v, v)
            assertTrue(cache.size <= 40)
        }
    }

    @Test
    fun testReadItems() {
        val cache = LRUCache<String, String>(40)
        for (v in DEFAULT_VALUES.take(20)) {
            cache.put(v, v)
            assertTrue(cache.size <= 20)
        }
        for (v in DEFAULT_VALUES.take(20)) {
            assertEquals(v, cache[v])
        }
        for (v in DEFAULT_VALUES.drop(20)) {
            assertNull(cache[v])
        }
    }

    @Test
    fun testSimpleEviction() {
        val cache = LRUCache<String, String>(40)
        DEFAULT_VALUES.forEach { cache[it] = it }

        for (v in DEFAULT_VALUES.take(DEFAULT_VALUES.size - 40)) {
            assertNull(cache[v])
        }
        for (v in DEFAULT_VALUES.drop(DEFAULT_VALUES.size - 40)) {
            assertEquals(v, cache[v])
        }
    }

    @Test
    fun testMoreComplexEviction() {
        val cache = LRUCache<String, String>(40)
        DEFAULT_VALUES.take(40).forEach { cache[it] = it }
        assertEquals(DEFAULT_VALUES[20], cache[DEFAULT_VALUES[20]])
        assertEquals(DEFAULT_VALUES[0], cache[DEFAULT_VALUES[0]])
        assertEquals(40, cache.size)
        cache.put(DEFAULT_VALUES[40], DEFAULT_VALUES[40])
        assertEquals(DEFAULT_VALUES[20], cache[DEFAULT_VALUES[20]])
        assertEquals(DEFAULT_VALUES[0], cache[DEFAULT_VALUES[0]])
        assertNull(cache[DEFAULT_VALUES[1]]) // should have been evicted
        assertEquals(40, cache.size)
    }

    @Test
    fun testMoreComplexEvictionWithGetOrPut() {
        val cache = LRUCache<String, String>(40)
        DEFAULT_VALUES.take(40).forEach { cache.getOrPut(it) { it } }
        assertEquals(DEFAULT_VALUES[20], cache.getOrPut(DEFAULT_VALUES[20]) { "INVALID" })
        assertEquals(DEFAULT_VALUES[0], cache.getOrPut(DEFAULT_VALUES[0]) { "INVALID" })
        assertEquals(40, cache.size)
        cache.getOrPut(DEFAULT_VALUES[40]) { DEFAULT_VALUES[40] }
        assertEquals(DEFAULT_VALUES[20], cache.getOrPut(DEFAULT_VALUES[20]) { "INVALID" })
        assertEquals(DEFAULT_VALUES[0], cache.getOrPut(DEFAULT_VALUES[0]) { "INVALID" })
        assertNull(cache[DEFAULT_VALUES[1]]) // should have been evicted
        assertEquals(40, cache.size)
        assertEquals("NEWVALUE", cache.getOrPut(DEFAULT_VALUES[1]) { "NEWVALUE"})
        assertEquals(40, cache.size)
    }

    @Test
    fun testPutAll() {
        val cache1 = LRUCache<String, String>(40)
        DEFAULT_VALUES.take(40).forEach { cache1.getOrPut(it) { it } }
        assertEquals(40, cache1.size)

        val cache2 = LRUCache<String, String>(50)
        DEFAULT_VALUES.asSequence().drop(20).take(50).forEach { cache2.getOrPut(it) { it } }
        assertEquals(50, cache2.size)

        cache1.putAll(cache2)
        assertEquals(40, cache1.size)

        for (v in DEFAULT_VALUES.asSequence().take(30)) {
            assertNull(cache1[v])
        }

        for (v in DEFAULT_VALUES.asSequence().drop(30).take(40)) {
            assertEquals(v, cache1[v])
        }
    }

    companion object {

        val DEFAULT_VALUES = Array(100) { "$it" }
    }

}
