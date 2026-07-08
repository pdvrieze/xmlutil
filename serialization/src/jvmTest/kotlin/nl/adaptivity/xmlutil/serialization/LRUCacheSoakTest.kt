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

package nl.adaptivity.xmlutil.serialization

import nl.adaptivity.xmlutil.serialization.impl.LRUCache
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.random.Random
import kotlin.test.assertFalse
import kotlin.test.assertTrue

public class LRUCacheSoakTest {

    @ParameterizedTest
    @MethodSource("provideSeeds")
    public fun soakTest(seed: Long) {
        val r = Random(seed)
        val cacheSize = CACHE_SIZES.random(r)

        val cache = LRUCache<CacheElem, CacheElem>(cacheSize)
        val regularMap = LinkedHashSet<CacheElem>(cacheSize)

        val eventCount = cacheSize * 2 + (0..4).sumOf { r.nextInt(200) }

        val eventSeq = mutableListOf(CacheElem(r.nextInt(), 0))
        val uniqueEvents = eventSeq.toMutableSet()


        for (i in 1 until eventCount) {
            val effectiveHitRatio = (HIT_RATIO * uniqueEvents.size) / cacheSize
            val effectiveCollisionProb = (HASH_COLLISION_PROBABILITY * uniqueEvents.size) / cacheSize
            var isReused = false
            val elem = when {
                r.nextDouble() < effectiveHitRatio -> {
                    isReused = true
                    uniqueEvents.random(r)
                }

                r.nextDouble() < effectiveCollisionProb -> CacheElem(uniqueEvents.random().hashKey, i)

                else -> CacheElem(r.nextInt(), i)
            }
            if (!isReused) uniqueEvents.add(elem)
            eventSeq.add(elem)
        }

        for (elem in eventSeq) {
            if (elem in regularMap) {
                // remove + add to simulate adding at end
                regularMap.remove(elem)
                regularMap.add(elem)

                assertEquals(elem, cache[elem])
                assertEquals(regularMap.size, cache.size)
            } else { // new item
                if (regularMap.size == cacheSize) {
                    val regIt = regularMap.iterator()
                    val evicted = regIt.next()
                    regIt.remove() // evict the first in iterator ("oldest")

                    regularMap.add(elem)

                    assertTrue(evicted in cache)
                    cache.put(elem, elem)
                    assertFalse(evicted in cache)
                    assertEquals(cacheSize, cache.size)
                } else {
                    regularMap.add(elem)
                    cache.put(elem, elem)
                    assertEquals(regularMap.size, cache.size)
                }

            }

            assertTrue(elem in cache)
        }

    }

    public class CacheElem(public val hashKey: Int, public val iter: Int) {
        public override fun hashCode(): Int = hashKey
        public override fun equals(other: Any?): Boolean {
            return other is CacheElem && hashKey == other.hashKey && iter == other.iter
        }

        public override fun toString(): String = "($hashKey,$iter)"
    }

    public companion object {

        @JvmStatic
        public val CACHE_SIZES = intArrayOf(32, 64, 128, 256)

        @JvmStatic
        public val HIT_RATIO = 0.8

        @JvmStatic
        public val HASH_COLLISION_PROBABILITY = 0.3

        @JvmStatic
        public fun provideSeeds(): List<Long> {
            val r = Random(123456789)
            return (0..<1000).map { r.nextLong() }
        }
    }
}
