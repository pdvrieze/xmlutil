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

package nl.adaptivity.xml.serialization.regressions

import nl.adaptivity.xmlutil.serialization.impl.LRUCache
import kotlin.test.Test

/** Test for #371 that hits invalid cache index positions due to reentry */
class CacheRecurrence371 {

    @Test
    fun testLRUCacheReentry() {
        val cache = LRUCache<Int, String>(512) // = DefaultFormatCache() default (LayeredCache per-decode layer)
        var next = 0

        @IgnorableReturnValue
        fun insert(depth: Int, breadth: Int): String {
            val key = next++
            // Mirrors DefaultFormatCache.lookupDescriptorOrStore: the value-factory passed to getOrPut
            // RE-ENTERS getOrPut on the SAME cache to look up child descriptors.
            return cache.getOrPut(key) {
                if (depth > 0) repeat(breadth) { insert(depth - 1, breadth) }
                "v$key"
            }
        }

        insert(depth = 4, breadth = 8) // ~4681 nested lookups >> 512 -> eviction fires mid-re-entrancy
        // -> ArrayIndexOutOfBoundsException: Index -1 in removeOldestEntry (0.90.4 / 1.0.1)
        //    or IllegalStateException "Cache size exceeded expected bounds!" (1.0.0)
    }
}
