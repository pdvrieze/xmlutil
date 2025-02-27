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

package nl.adaptivity.xmlutil.serialization.impl

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
internal class LRUCache<K : Any, V : Any>(private val cacheSize: Int, fillFactor: Float = 0.5f) {
    // An array to hold linked list positions
    private val linkedData: IntArray
    private val modulo: Int
    private val positionModulo: Int

    // A separate array to hold keys and values
    private val objData: Array<Any?>

    var size = 0
        private set

    private var oldestPosition = DoubledPos(-1)
    private var newestPosition = DoubledPos(-1)

    init {
        val capacity = calculateArraySize(cacheSize, fillFactor)
        val maxPosition = capacity * NUM_INTEGERS_TO_HOLD_ENTRY
        modulo = capacity - 1
        positionModulo = maxPosition - 1
        linkedData = IntArray(maxPosition)
        objData = arrayOfNulls(maxPosition)
        linkedData.fill(-1)
    }

    /**
     * Clears the cache for re-use.
     */
    fun clear() {
        linkedData.fill(-1)
        objData.fill(null)

        oldestPosition = DoubledPos(-1)
        newestPosition = DoubledPos(-1)
        size = 0
    }

    operator fun set(key: K, value: V) { put(key, value) }

    /**
     * Inserts key, value into the cache. Returns any previous value associated with the given key,
     * otherwise [this.NULL] is returned.
     */
    fun put(key: K, value: V): V? {
        check(size <= cacheSize) { "Cache size exceeded expected bounds!" }
        val position = posFromHash(key)
        var currentPosition = position
        do {
            val currentKey = getKey(currentPosition)
            if (key == currentKey) {
                @Suppress("UNCHECKED_CAST")
                val previousValue = getValue(currentPosition)
                setKeyValue(currentPosition, key, value)

                removeEntry(currentPosition)
                addEntry(currentPosition)
                return previousValue
            } else if (currentKey == null) {
                if (size >= cacheSize) {
                    val currentHeadPosition = oldestPosition
                    removeEntry(currentHeadPosition)
                    shiftKeys(currentHeadPosition)
                    setKeyValue(currentHeadPosition, null, null)
                    --size
                    break
                } else {
                    setKeyValue(currentPosition, key, value)
                    addEntry(currentPosition)
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

                addEntry(currentPosition)
                ++size
                return null
            }
            currentPosition = currentPosition.next()
        } while (currentPosition != position)
        return null
    }

    /**
     * Inserts key, value into the cache. Returns any previous value associated with the given key,
     * otherwise [this.NULL] is returned.
     */
    fun getOrPut(key: K, defaultValue: () -> V): V {
        check(size <= cacheSize) { "Cache size exceeded expected bounds!" }
        val position = posFromHash(key)
        var currentPosition = position
        do {
            val currentKey = getKey(currentPosition)
            if (key == currentKey) {
                @Suppress("UNCHECKED_CAST")
                val value = getValue(currentPosition) as V
                removeEntry(currentPosition) // these move the value in the LRU
                addEntry(currentPosition)
                return value
            } else if (currentKey == null) {
                if (size >= cacheSize) {
                    val currentHeadPosition = oldestPosition
                    removeEntry(currentHeadPosition)
                    shiftKeys(currentHeadPosition)
                    setKeyValue(currentHeadPosition, null, null)
                    --size
                    break
                } else {
                    val value = defaultValue()
                    setKeyValue(currentPosition, key, value)
                    addEntry(currentPosition)
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

                addEntry(currentPosition)
                ++size
                return value
            }
            currentPosition = currentPosition.next()
        } while (currentPosition != position)
        error("This code should not be reachable")
    }

    fun putAll(other: LRUCache<out K, out V>) {
        if (other.size ==0) return


        var pos = other.oldestPosition
        check(! other.getOlder(pos).isSet)
        while (pos.isSet) {
            put(other.getKey(pos)!!, other.getValue(pos)!!)
            pos = other.getNewer(pos)
        }

    }

    /**
     * Returns the value associated with the given key, otherwise [this.NULL] is returned.
     */
    operator fun get(key: K): V? {
        val position = posFromHash(key)
        var currentPosition = position
        do {
            val currentKey = getKey(currentPosition) ?: return null

            if (key == currentKey) {
                removeEntry(currentPosition)
                addEntry(currentPosition)
                return getValue(currentPosition)
            }
            currentPosition = currentPosition.next()
        } while (currentPosition != position)
        return null
    }

    /**
     * Removes the given key from the cache. Returns the value associated with key if it is
     * removed, otherwise [this.NULL] is returned.
     */
    fun remove(key: K): V? {
        val position = posFromHash(key)
        var currentPosition = position
        do {
            val currentKey = getKey(currentPosition) ?: return null

            if (key == currentKey) {
                @Suppress("UNCHECKED_CAST")
                val removedValue = getValue(currentPosition)
                removeEntry(currentPosition)
                shiftKeys(currentPosition)
                setKeyValue(currentPosition, null, null)
                --size
                return removedValue
            }
            currentPosition = currentPosition.next()
        } while (currentPosition != position)
        return null
    }

    private fun shiftKeys(currentPosition: DoubledPos) {
        var currentPosition = currentPosition
        var freeSlot: DoubledPos
        var currentKeySlot: DoubledPos
        do {
            freeSlot = currentPosition
            currentPosition = DoubledPos((currentPosition.value + NUM_INTEGERS_TO_HOLD_ENTRY) and positionModulo)
            while (true) {
                val currentKey = getKey(currentPosition)
                if (currentKey == null) {
                    setKey(freeSlot, null)
                    return
                }
                currentKeySlot = posFromHash(currentKey)
                if (freeSlot <= currentPosition) {
                    if (freeSlot >= currentKeySlot || currentKeySlot > currentPosition) {
                        break
                    }
                } else {
                    if (currentPosition < currentKeySlot && currentKeySlot <= freeSlot) {
                        break
                    }
                }
                currentPosition = currentPosition.next()
            }
            setKeyValue(freeSlot, getKey(currentPosition), getValue(currentPosition))
            val currentLeft = getOlder(currentPosition)
            setOlder(freeSlot, currentLeft)
            setNewer(freeSlot, getNewer(currentPosition))

            if (currentLeft.isSet) {
                setNewer(currentLeft, freeSlot)

                if (currentPosition == newestPosition) {
                    newestPosition = freeSlot
                }
            }

            val currentRight = getNewer(currentPosition)
            if (currentRight.isSet) {
                setOlder(currentRight, freeSlot)

                if (currentPosition == oldestPosition) {
                    oldestPosition = freeSlot
                }
            }
        } while (true)
    }

    private fun removeEntry(position: DoubledPos) {
        val oldLeft = getOlder(position)
        val oldRight = getNewer(position)
        setOlder(position, DoubledPos(-1))
        setNewer(position, DoubledPos(-1))
        if (oldLeft.isSet) {
            setNewer(oldLeft, oldRight)
        } else {
            oldestPosition = oldRight
        }
        if (oldRight.isSet) {
            setOlder(oldRight, oldLeft)
        } else {
            newestPosition = oldLeft
        }
    }

    private fun addEntry(position: DoubledPos) {

        if (newestPosition.isSet) {
            setNewer(newestPosition, position)
        }
        setOlder(position, newestPosition)
        setNewer(position, DoubledPos(-1))

        newestPosition = position
        if (!oldestPosition.isSet) {
            oldestPosition = newestPosition
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun getKey(pos: DoubledPos): K? {
        @Suppress("UNCHECKED_CAST")
        return objData[pos.value + KEY_OFFSET] as K?
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun setKey(pos: DoubledPos, value: K?) {
        objData[pos.value + KEY_OFFSET] = value
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun getValue(pos: DoubledPos): V? {
        @Suppress("UNCHECKED_CAST")
        return objData[pos.value + VALUE_OFFSET] as V?
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun setValue(pos: DoubledPos, value: V?) {
        objData[pos.value + VALUE_OFFSET] = value
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun setKeyValue(pos: DoubledPos, key: K?, value: V?) {
        objData[pos.value + KEY_OFFSET] = key
        objData[pos.value + VALUE_OFFSET] = value
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun getOlder(pos: DoubledPos): DoubledPos {
        return DoubledPos(linkedData[pos.value + LEFT_OFFSET])
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun setOlder(pos: DoubledPos, value: DoubledPos) {
        linkedData[pos.value + LEFT_OFFSET] = value.value
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun getNewer(pos: DoubledPos): DoubledPos {
        return DoubledPos(linkedData[pos.value + RIGHT_OFFSET])
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun setNewer(pos: DoubledPos, value: DoubledPos) {
        linkedData[pos.value + RIGHT_OFFSET] = value.value
    }

    private fun posFromHash(key: K): DoubledPos {
        val h = key.hashCode()
        /** -0x61c88647*/ // phiMix(x) taken from FastUtil
        return DoubledPos(((h xor (h shr 16)) and modulo) * NUM_INTEGERS_TO_HOLD_ENTRY)
    }


    private fun DoubledPos.next(): DoubledPos {
        return DoubledPos((value + NUM_INTEGERS_TO_HOLD_ENTRY) and positionModulo)
    }

    @JvmInline
    value class DoubledPos(val value: Int) {
        val isSet: Boolean get() = value >= 0

        @Suppress("NOTHING_TO_INLINE")
        inline operator fun compareTo(other: DoubledPos): Int {
            return value.compareTo(other.value)
        }
    }

    companion object {
        private const val KEY_OFFSET = 0
        private const val VALUE_OFFSET = 1
        private const val LEFT_OFFSET = 0
        private const val RIGHT_OFFSET = 1
        private const val NUM_INTEGERS_TO_HOLD_ENTRY = 2

        init {
            check((NUM_INTEGERS_TO_HOLD_ENTRY and NUM_INTEGERS_TO_HOLD_ENTRY - 1) == 0) { "Invalid entry size, should be power of 2!" }
        }

        /**
         * Returns the least power of two larger than or equal to `Math.ceil( expected / f
         * )`.
         *
         * @param expectedSize
         * the expected number of elements in a hash table.
         * @param f
         * the load factor.
         * @return the minimum possible size for a backing array.
         * @throws IllegalArgumentException
         * if the necessary size is larger than 2<sup>30</sup>.
         */
        private fun calculateArraySize(expectedSize: Int, f: Float): Int {
            var desiredCapacity = ceil((expectedSize / f).toDouble()).toLong()
            require(desiredCapacity <= Int.Companion.MAX_VALUE) {
                "Storage gets too large with expected size $expectedSize, load factor $f"
            }
            // find next closest power of 2.
            if (desiredCapacity <= 2) {
                return 2
            }
            desiredCapacity--
            desiredCapacity = desiredCapacity or (desiredCapacity shr 1)
            desiredCapacity = desiredCapacity or (desiredCapacity shr 2)
            desiredCapacity = desiredCapacity or (desiredCapacity shr 4)
            desiredCapacity = desiredCapacity or (desiredCapacity shr 8)
            desiredCapacity = desiredCapacity or (desiredCapacity shr 16)
            return ((desiredCapacity or (desiredCapacity shr 32)) + 1).toInt()
        }
    }
}
