/*
 * Copyright (c) 2025-2026.
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

@file:MustUseReturnValues

package nl.adaptivity.xmlutil.serialization.impl

import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
import nl.adaptivity.xmlutil.core.impl.multiplatform.ifAssertions
import kotlin.jvm.JvmInline
import kotlin.math.ceil

/**
 * LRU Cache implementation from https://github.com/udaysagar2177/fastest-lru-cache/blob/master/src/main/java/com/udaysagar2177/cache/IntIntLRUCache.java
 * This is adapted to work for object types
 *
 * Map and Doubly Linked List are not two different data structures anymore. Data is stored in an
 * integer array and both map lookups and doubly linked list movements are addressed with respect
 * to data layout. See the article related to this implementation for more details -
 * https://medium.com/@udaysagar.2177/fastest-lru-cache-in-java-c22262de42ad
 *
 * Linear probing scheme which benefits from CPU cache line reads is used to resolve key collisions.
 * Each entry is written across 4 integers (16 bytes), so a 64B cache line read makes 4
 * consecutive entries available for CPU.
 *
 * Note: due to NULL being the empty key and for simplicity, allowed keys are [0, Int.MAX) and
 * values are [Int.MIN, INT.MAX]. If negative integers need to stored as keys, there is a
 * workaround by storing the free key and position associated with it separately. Then,
 * put(key, value), get(key) and remove(key) operations should check against free key appropriately.
 *
 * While the combination of map and doubly linked list is my original work, the map functionality
 * part is inspired from https://github.com/mikvor/hashmapTest.
 *
 * @author uday
 */
internal class LRUCache<K : Any, V : Any> private constructor(
    private val cacheSize: Int,
    private val cacheMask: Int,
    private val orderMask: Int,
    private val orderLinks: IntArray, // An array to hold linked list positions
    private val hashMapData: Array<Any?>,// A separate array to hold keys and values
    private var oldestPosition: DoubledPos,
    private var newestPosition: DoubledPos,
    size: Int = 0
) {

    constructor(cacheSize: Int, fillFactor: Float = 0.5f) : this(
        cacheSize = cacheSize,
        capacity = calculateArraySize(cacheSize, fillFactor),
    )

    private constructor(cacheSize: Int, capacity: Int, orderMapSize: Int = capacity * NUM_INTEGERS_TO_HOLD_ENTRY) : this(
        cacheSize = cacheSize,
        cacheMask = capacity - 1,
        orderMask = orderMapSize - 1,
        orderLinks = IntArray(orderMapSize).also { it.fill(-1) },
        hashMapData = arrayOfNulls(orderMapSize),
        oldestPosition = DoubledPos(-1),
        newestPosition = DoubledPos(-1),
    ) {
        assert(capacity.countOneBits() == 1)
    }

    var size = size
        private set


    fun copy(): LRUCache<K, V> = LRUCache(
        cacheSize,
        cacheMask,
        orderMask,
        orderLinks.copyOf(),
        hashMapData.copyOf(),
        oldestPosition,
        newestPosition,
        size
    )

    /**
     * Clears the cache for re-use.
     */
    fun clear() {
        orderLinks.fill(-1)
        hashMapData.fill(null)

        oldestPosition = DoubledPos(-1)
        newestPosition = DoubledPos(-1)
        size = 0
    }

    operator fun set(key: K, value: V) {
        put(key, value)
    }

    /**
     * Inserts key, value into the cache. Returns any previous value associated with the given key,
     * otherwise `null` is returned.
     */
    @IgnorableReturnValue
    fun put(key: K, value: V): V? {
        ifAssertions {
            check(size <= cacheSize) { "Cache size exceeded expected bounds!" }
        }
        val position = posFromHash(key)
        var currentPosition = position
        do {
            val currentKey = getKey(currentPosition)
            if (key == currentKey) {
                @Suppress("UNCHECKED_CAST")
                val previousValue = getValue(currentPosition)
                setValue(currentPosition, value)

                markEntryAsNewest(currentPosition)

                return previousValue
            } else if (currentKey == null) {
                if (size >= cacheSize) {
                    removeOldestEntry()
                    break // uses break so the later loop will not check the key
                } else {
                    setKeyValue(currentPosition, key, value)
                    addEntryToOrderListAsNewest(currentPosition)
                    ++size
                    return null
                }
            }
            currentPosition = currentPosition.next()
        } while (true)

        currentPosition = position
        do {
            val currentKey = getKey(currentPosition)
            if (currentKey == null) {
                setKeyValue(currentPosition, key, value)

                addEntryToOrderListAsNewest(currentPosition)
                ++size
                return null
            }
            currentPosition = currentPosition.next()
        } while (currentPosition != position)
        return null
    }

    /**
     * Inserts key, value into the cache. Returns any previous value associated with the given key,
     * otherwise `null` is returned.
     */
    fun getOrPut(key: K, defaultValue: () -> V): V {
        ifAssertions {
            check(size <= cacheSize) { "Cache size exceeded expected bounds!" }
        }
        val position = posFromHash(key)
        var currentPosition = position
        do {
            val currentKey = getKey(currentPosition)
            if (key == currentKey) {
                @Suppress("UNCHECKED_CAST")
                val value = getValue(currentPosition) as V
                markEntryAsNewest(currentPosition) // these move the value in the LRU

                return value
            } else if (currentKey == null) {
                if (size >= cacheSize) { // remove the oldest entry
                    removeOldestEntry()
                    break
                } else {
                    val value = defaultValue()
                    setKeyValue(currentPosition, key, value)
                    addEntryToOrderListAsNewest(currentPosition)
                    ++size
                    return value
                }

            }
            currentPosition = currentPosition.next()
        } while (true)

        currentPosition = position
        do {
            val currentKey = getKey(currentPosition)
            if (currentKey == null) {
                val value = defaultValue()
                setKeyValue(currentPosition, key, value)

                addEntryToOrderListAsNewest(currentPosition)
                ++size
                return value
            }
            currentPosition = currentPosition.next()
        } while (currentPosition != position)
        error("This code should not be reachable")
    }

    fun putAll(other: LRUCache<out K, out V>) {
        if (other.size == 0) return

        var pos = other.oldestPosition
        ifAssertions { check(!other.getOlder(pos).isSet) }
        while (pos.isSet) {
            put(other.getKey(pos)!!, other.getValue(pos)!!)
            pos = other.getNewer(pos)
        }

    }

    /**
     * Check whether the cache contains the given key, without updating the access order.
     */
    @ExperimentalXmlUtilApi
    operator fun contains(key: K): Boolean {
        val position = posFromHash(key)
        var currentPosition = position
        do {
            val currentKey = getKey(currentPosition) ?: return false

            if (key == currentKey) return true
            currentPosition = currentPosition.next()
        } while (currentPosition != position)
        return false
    }

    /**
     * Returns the value associated with the given key, otherwise `null` is returned.
     */
    operator fun get(key: K): V? {
        val position = posFromHash(key)
        var currentPosition = position
        do {
            val currentKey = getKey(currentPosition) ?: return null

            if (key == currentKey) {
                markEntryAsNewest(currentPosition)

                return getValue(currentPosition)
            }
            currentPosition = currentPosition.next()
        } while (currentPosition != position)
        return null
    }

    /**
     * Removes the given key from the cache. Returns the value associated with key if it is
     * removed, otherwise `null` is returned.
     */
    fun remove(key: K): V? {
        val position = posFromHash(key)
        var currentPosition = position
        do {
            val currentKey = getKey(currentPosition) ?: return null

            if (key == currentKey) return removeAtPosition(currentPosition)

            currentPosition = currentPosition.next()
        } while (currentPosition != position)
        return null
    }

    private fun removeAtPosition(position: DoubledPos): V {
        checkNotNull(getKey(position)) { "No key at the position: $position" }
        val removedValue = getValue(position)

        val oldLeft = getOlder(position)
        val oldRight = getNewer(position)

        when {
            oldLeft.isSet -> oldLeft.setNewer(oldRight)
            else -> oldestPosition = oldRight
        }
        when {
            oldRight.isSet -> oldRight.setOlder(oldLeft)
            else -> newestPosition = oldLeft
        }

        removeKeyShiftingSpilledHashcodes(position)

        --size
        return removedValue as V
    }

    private fun removeOldestEntry() {
        if (! oldestPosition.isSet) return

        val oldOldest = oldestPosition

        val nextOldestPos = getNewer(oldOldest)
        nextOldestPos.setOlder(DoubledPos(-1))
        oldOldest.setNewer(DoubledPos(-1))

        oldestPosition = nextOldestPos

        removeKeyShiftingSpilledHashcodes(oldOldest)
        size -= 1
    }

    /**
     * Removal of the entry at the current position such that hash key spills are consecutive,
     * starting at the entry.
     * @param currentPosition The position to be discarded/removed
     */
    private fun removeKeyShiftingSpilledHashcodes(currentPosition: DoubledPos) {
        if (!currentPosition.isSet) return

        setKeyValue(currentPosition, null, null)

        var currentPosition = currentPosition
        var freeSlot: DoubledPos
        var currentKeySlot: DoubledPos
        do {
            freeSlot = currentPosition
            currentPosition = currentPosition.next()
            while (true) {
                val currentKey = getKey(currentPosition)
                if (currentKey == null) { // the current item to set null can be emptied
                    setKeyValue(freeSlot, null, null)
                    freeSlot.setOlder(DoubledPos(-1))
                    freeSlot.setNewer(DoubledPos(-1))
                    return // exits the outer loop if we found an empty position
                }
                currentKeySlot = posFromHash(currentKey)
                if (freeSlot <= currentPosition) {
                    if (freeSlot >= currentKeySlot || currentKeySlot > currentPosition) {
                        break // found an item to move to the left
                    }
                } else { // wrapped around the array
                    if (currentPosition < currentKeySlot && currentKeySlot <= freeSlot) {
                        break
                    }
                }
                currentPosition = currentPosition.next()
            }
            val leftOfCurrent = getOlder(currentPosition)
            val rightOfCurrent = getNewer(currentPosition)
            when {
                leftOfCurrent.isSet -> leftOfCurrent.setNewer(freeSlot)
                else -> oldestPosition = freeSlot
            }
            when {
                rightOfCurrent.isSet -> rightOfCurrent.setOlder(freeSlot)
                else -> newestPosition = freeSlot
            }
            freeSlot.setOlder(leftOfCurrent)
            freeSlot.setNewer(rightOfCurrent)
            setKeyValue(freeSlot, getKey(currentPosition), getValue(currentPosition))

            ifAssertions {
                // resets (should not be required)
                setKeyValue(currentPosition, null, null)
                currentPosition.setOlder(DoubledPos(-1))
                currentPosition.setNewer(DoubledPos(-1))
            }
        } while (true)
    }

    private fun markEntryAsNewest(position: DoubledPos) {
        val oldLeft = getOlder(position)
        val oldRight = getNewer(position)
        when {
            oldLeft.isSet -> oldLeft.setNewer(oldRight)
            else -> oldestPosition = oldRight
        }
        when {
            oldRight.isSet -> oldRight.setOlder(oldLeft)
            else -> newestPosition = oldLeft
        }

        if (newestPosition.isSet) {
            newestPosition.setNewer(position)
        }
        position.setOlder(newestPosition) // works if there is nothing older
        position.setNewer(DoubledPos(-1))

        newestPosition = position
        if (!oldestPosition.isSet) {
            oldestPosition = newestPosition
        }

    }

    private fun addEntryToOrderListAsNewest(position: DoubledPos) {

        if (newestPosition.isSet) {
            newestPosition.setNewer(position)
        }
        position.setOlder(newestPosition)
        position.setNewer(DoubledPos(-1))

        newestPosition = position
        if (!oldestPosition.isSet) {
            oldestPosition = newestPosition
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun getKey(pos: DoubledPos): K? {
        @Suppress("UNCHECKED_CAST")
        return hashMapData[pos.value + KEY_OFFSET] as K?
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun setKey(pos: DoubledPos, value: K?) {
        hashMapData[pos.value + KEY_OFFSET] = value
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun getValue(pos: DoubledPos): V? {
        @Suppress("UNCHECKED_CAST")
        return hashMapData[pos.value + VALUE_OFFSET] as V?
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun setValue(pos: DoubledPos, value: V?) {
        hashMapData[pos.value + VALUE_OFFSET] = value
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun setKeyValue(pos: DoubledPos, key: K?, value: V?) {
        hashMapData[pos.value + KEY_OFFSET] = key
        hashMapData[pos.value + VALUE_OFFSET] = value
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun getOlder(pos: DoubledPos): DoubledPos {
        return DoubledPos(orderLinks[pos.value + OLDER_OFFSET])
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun DoubledPos.setOlder(older: DoubledPos) {
        orderLinks[value + OLDER_OFFSET] = older.value
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun getNewer(pos: DoubledPos): DoubledPos {
        return DoubledPos(orderLinks[pos.value + NEWER_OFFSET])
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun DoubledPos.setNewer(newer: DoubledPos) {
        orderLinks[value + NEWER_OFFSET] = newer.value
    }

    private fun posFromHash(key: K): DoubledPos {
        val h = key.hashCode()
        /** -0x61c88647*/ // phiMix(x) taken from FastUtil
        return DoubledPos(((h xor (h shr 16)) and cacheMask) * NUM_INTEGERS_TO_HOLD_ENTRY)
    }


    private fun DoubledPos.next(): DoubledPos {
        return DoubledPos((value + NUM_INTEGERS_TO_HOLD_ENTRY) and orderMask)
    }

    @JvmInline
    value class DoubledPos(val value: Int) {
        val isSet: Boolean get() = value >= 0

        @Suppress("NOTHING_TO_INLINE")
        inline operator fun compareTo(other: DoubledPos): Int {
            return value.compareTo(other.value)
        }
    }

    override fun toString(): String {
        return buildString {
            append("LRUCache { ")
            var cur = oldestPosition
            while (cur.isSet) {
                val key = hashMapData[cur.value + KEY_OFFSET]
                val value = hashMapData[cur.value + VALUE_OFFSET]
                if (length > 12) append(", ")
                append("$key -> $value")
                val next = orderLinks[cur.value + NEWER_OFFSET]
                ifAssertions {
                    if (next >= 0) {
                        check(orderLinks[next + OLDER_OFFSET] == cur.value) {
                            "Linked list previous is incorrect"
                        }

                    }
                }
                cur = DoubledPos(next)
            }

            append(" }")
        }
    }

    companion object {
        private const val KEY_OFFSET = 0
        private const val VALUE_OFFSET = 1
        private const val OLDER_OFFSET = 0
        private const val NEWER_OFFSET = 1
        private const val NUM_INTEGERS_TO_HOLD_ENTRY = 2

        /**
         * Returns the least power of two larger than or equal to `Math.ceil( expected / f
         * )`.
         *
         * @param expectedSize the expected number of elements in a hash table.
         * @param f the load factor.
         * @return the minimum possible size for a backing array.
         * @throws IllegalArgumentException if the necessary size is larger than 2<sup>30</sup>.
         */
        private fun calculateArraySize(expectedSize: Int, f: Float): Int {
            var desiredCapacity = ceil((expectedSize / f).toDouble()).toLong()
            require(desiredCapacity <= Int.MAX_VALUE) {
                "Storage gets too large with expected size $expectedSize, load factor $f"
            }
            // find next closest power of 2.
            if (desiredCapacity <= 2) {
                return 2
            }

            // effectively round up to the nearest power of 2 and then left shifted
            val leftShift = 64 - (desiredCapacity - 1).countLeadingZeroBits()
            desiredCapacity = 1L shl leftShift
            return desiredCapacity.toInt()
        }
    }
}
