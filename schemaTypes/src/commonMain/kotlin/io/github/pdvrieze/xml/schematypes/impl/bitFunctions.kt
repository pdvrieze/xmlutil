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

package io.github.pdvrieze.xml.schematypes.impl

import io.github.pdvrieze.xml.schematypes.requireRange

internal fun Int.toLBits(bitCount: Int, shift: Int): ULong = toLBits(bitCount) shl shift

internal fun Int.toLBits(bitCount: Int): ULong {
    val exMask = ((1uL shl bitCount-1) - 1uL).toUInt().inv().toInt()
    val expected = this.shr(31).and(exMask) //sign extends
    requireRange(this and exMask == expected) { "Value $this is out of range for $bitCount bits" }

    val ulValue = toULong()
    return (ulValue and (1uL shl (bitCount - 1)) - 1uL) or ((ulValue shr 63) shl (bitCount - 1))
}

internal fun Long.toLBits(bitCount: Int, shift: Int): ULong = toLBits(bitCount) shl shift

internal fun Long.toLBits(bitCount: Int): ULong {
    val exMask = ((1uL shl bitCount-1) - 1uL).inv().toLong() // include the sign bit in match
    val expected = this.shr(31).and(exMask) //sign extends so msb target bit should match sign
    requireRange(this and exMask == expected) { "Value $this is out of range for $bitCount bits" }

    val ulValue = toULong()
    return (ulValue and (1uL shl (bitCount - 1)) - 1uL) or ((ulValue shr 63) shl (bitCount - 1))
}

internal fun ULong.toLBits(bitCount: Int, shift: Int): ULong = toLBits(bitCount) shl shift

internal fun ULong.toLBits(bitCount: Int): ULong {
    val exMask = ((1uL shl bitCount) - 1uL).inv()
    requireRange(this and exMask == 0uL) { "Value $this is out of range for $bitCount bits" }

    return (this and (1uL shl (bitCount - 1)) - 1uL) or ((this shr 63) shl (bitCount - 1))
}

internal fun Int.toIBits(bitCount: Int, shift: Int): UInt = toIBits(bitCount) shl shift

internal fun Int.toIBits(bitCount: Int): UInt {
    val exMask = ((1uL shl bitCount-1) - 1uL).toUInt().inv().toInt()
    val expected = this.shr(31).and(exMask) //sign extends
    requireRange(this and exMask ==expected) { "Value $this is out of range for $bitCount bits" }

    val uValue = toUInt()
    return (uValue and (1u shl (bitCount - 1)) - 1u) or ((uValue shr 31) shl (bitCount - 1))
}

internal fun UInt.toIBits(bitCount: Int, shift: Int): UInt = toIBits(bitCount) shl shift

internal fun UInt.toIBits(bitCount: Int): UInt {
    val exMask = ((1uL shl bitCount) - 1uL).toUInt().inv()
    requireRange(this and exMask ==0u) { "Value $this is out of range for $bitCount bits" }

    return this and (1u shl (bitCount)) - 1u
}

internal fun UInt.toLBits(bitCount: Int, shift: Int): ULong = toLBits(bitCount) shl shift

internal fun UInt.toLBits(bitCount: Int): ULong {
    val exMask = ((1uL shl bitCount) - 1uL).toUInt().inv()
    requireRange(this and exMask ==0u) { "Value $this is out of range for $bitCount bits" }

    return toULong() and (1uL shl (bitCount)) - 1uL
}

internal fun UInt.intFromBits(bitCount: Int): Int {
    val signMask: UInt = 1u shl (bitCount - 1)
    val mask = signMask - 1u
    return when {
        this and signMask == signMask ->  // negative (mask.inv gives everything else 1's)
            (mask.inv() or (this and mask)).toInt()

        else -> (this and mask).toInt()
    }
}

internal fun ULong.intFromBits(bitCount: Int): Int {
    val signMask: ULong = 1uL shl (bitCount - 1)
    val mask = signMask - 1uL
    return when {
        this and signMask == signMask ->  // negative
            (mask.inv() or (this and mask)).toInt()

        else -> (this and mask).toInt()
    }
}

internal fun UInt.uintFromBits(bitCount: Int): UInt {
    return this and ((1u shl bitCount) - 1u)
}

internal fun ULong.uintFromBits(bitCount: Int): UInt {
    return (this and ((1uL shl bitCount) - 1uL)).toUInt()
}

internal fun ULong.longFromBits(bitCount: Int): Long {
    val signMask: ULong = 1uL shl (bitCount - 1)
    val mask = signMask - 1uL
    return when {
        this and signMask == signMask ->  // negative
            (((-1).toULong() xor mask) or
                    (this and mask)).toLong()

        else -> (this and mask).toLong()
    }
}

internal fun ULong.uLongFromBits(bitCount: Int): ULong {
    return this and ((1uL shl bitCount) - 1uL)
}
