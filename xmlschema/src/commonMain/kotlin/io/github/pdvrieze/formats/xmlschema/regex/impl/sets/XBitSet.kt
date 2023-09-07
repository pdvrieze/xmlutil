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

@file:OptIn(XmlUtilInternal::class)

package io.github.pdvrieze.formats.xmlschema.regex.impl.sets

import nl.adaptivity.xmlutil.XmlUtilInternal

@XmlUtilInternal
public expect class XBitSet(size: Int) {

    /**
     * Returns an index of a next bit which value is `true` after [startIndex] (inclusive).
     * Returns -1 if there is no such bits after [startIndex].
     * @throws IndexOutOfBoundException if [startIndex] < 0.
     */
    fun nextSetBit(startIndex: Int): Int

    /**
     * Returns an index of a next bit which value is `false` after [startIndex] (inclusive).
     * Returns [size] if there is no such bits between [startIndex] and [size] - 1 assuming that the set has an infinite
     * sequence of `false` bits after (size - 1)-th.
     * @throws IndexOutOfBoundException if [startIndex] < 0.
     */
    fun nextClearBit(startIndex: Int): Int

    /** Returns a value of a bit with the [index] specified. */
    operator fun get(index: Int): Boolean

    /** Set the bit specified to the specified value. */
    fun set(index: Int, value: Boolean)

    /** Sets the bits with indices between [from] (inclusive) and [to] (exclusive) to the specified value. */
    fun set(from: Int, to: Int, value: Boolean): Unit

    /** Returns true if the specified BitSet has any bits set to true that are also set to true in this BitSet. */
    fun intersects(another: XBitSet): Boolean

    /** Performs a logical xor operation over corresponding bits of this and [another] BitSets. The result is saved in this BitSet. */
    fun xor(another: XBitSet)

    /** Performs a logical and operation over corresponding bits of this and [another] BitSets. The result is saved in this BitSet. */
    fun and(another: XBitSet)

    /** Performs a logical and + not operations over corresponding bits of this and [another] BitSets. The result is saved in this BitSet. */
    fun andNot(another: XBitSet)

    /** Performs a logical or operation over corresponding bits of this and [another] BitSets. The result is saved in this BitSet. */
    fun or(another: XBitSet)

    /** True if this BitSet contains no bits set to true. */
    fun isEmpty(): Boolean

}

internal fun DefaultBitSet() = XBitSet(ELEMENT_SIZE)

/** Sets the bits from the range specified to the specified value. */
internal fun XBitSet.set(range: IntRange, value: Boolean) = set(range.start, range.endInclusive+1, value)

internal inline fun XBitSet.nextSetBit() = nextSetBit(0)

internal inline fun XBitSet.nextClearBit() = nextClearBit(0)

internal inline fun XBitSet.set(index:Int) = set(index, true)

internal inline fun XBitSet.set(from: Int, to: Int) = set(from, to, true)

internal inline fun XBitSet.set(range: IntRange) = set(range = range, true)

expect internal val XBitSet.isEmpty: Boolean

// Default size of one element in the array used to store bits.
private const val ELEMENT_SIZE = 64
