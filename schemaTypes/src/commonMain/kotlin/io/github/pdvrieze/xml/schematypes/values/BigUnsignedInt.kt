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

package io.github.pdvrieze.xml.schematypes.values

import io.github.pdvrieze.xml.schematypes.types.NonNegativeIntegerType
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
import nl.adaptivity.xmlutil.core.impl.multiplatform.ifAssertions

@OptIn(ExperimentalUnsignedTypes::class)
class BigUnsignedInt private constructor(private val ints: UIntArray, private val exp: ULong): XsdNonNegativeInteger {
    init {
        require(ints.isNotEmpty()) { "At least one integer must be present" }
        if (ints.size == 1 && ints[0] == 0u) {
            require(exp == 0uL) { "The value is zero, but an exponent is present" }
        }
    }

    private constructor(r: ParseResult): this(r.ints, r.exp)

    constructor(value: UInt, exp: ULong = 0uL) : this(
        uintArrayOf(value),
        exp
    )

    constructor(value: ULong, exp: ULong = 0uL) : this(
        uintArrayOf(value.toUInt(), (value shr 32).toUInt()),
        exp
    )

    constructor(str: String): this(parse(str))

    constructor(orginal: XsdNonNegativeInteger): this(convert(orginal))

    override fun countTrailingZeroBits(): ULong {
        for (i in ints.indices) {
            if (ints[i] != 0u) return ((i.toULong() * 32uL) + ints[i].countTrailingZeroBits().toULong())
        }
        throw IllegalStateException("The value is zero, ")
    }

    override val size: ULong
        get() = ints.size.toULong() + ((31u + exp) shr 32)

    override fun get(index: ULong): UInt {
        if ((index + 1uL) * 32uL < exp) return 0u // not visible

        val tmp = ((index.toLong() shl 5) - exp.toLong())
        val byteShift = tmp.shr(5).toInt()
        val bitShift = tmp.and(0x1f).toInt()

        if (bitShift == 0) return ints[byteShift]

        if (byteShift >= ints.size) return 0u

        val lsi = if (byteShift < 0) 0u else (ints[byteShift] shr bitShift)
        val msi = if (byteShift+1 >=ints.size) 0u else (ints[byteShift + 1] shl (32 - bitShift))

        return lsi or msi
    }

    override operator fun get(index: Int): UInt {
        if (index < 0) return 0u

        if ((index.toULong() + 1u) shl 5 < exp) return 0u // not visible

        val tmp = ((index.toLong() shl 5) - exp.toLong())
        val byteShift = tmp.shr(5).toInt()

        val bitShift = tmp.and(0x1f).toInt()

        if (bitShift == 0) return ints[byteShift]

        if (byteShift >= ints.size) return 0u

        val lsi = if (byteShift < 0) 0u else (ints[byteShift] shr bitShift)
        val msi = if (byteShift + 1 >= ints.size) 0u else (ints[byteShift + 1] shl (32 - bitShift))

        return lsi or msi
    }

    fun getBitIndex(bitIndex: ULong): UInt {
        if (bitIndex + 32uL < exp) return 0u // not visible

        val tmp = (bitIndex.toLong()-exp.toLong())
        val byteShift = tmp.shr(5).toInt()
        val bitShift = tmp.and(0x1f).toInt()

        if (bitShift == 0) return ints[byteShift]

        if (byteShift >= ints.size) return 0u

        val lsi = if (byteShift < 0) 0u else (ints[byteShift] shr bitShift)
        val msi = if (byteShift+1 >=ints.size) 0u else (ints[byteShift + 1] shl (32 - bitShift))

        return lsi or msi
    }

    operator fun div(divider: UInt): BigUnsignedInt = divRem(divider).quotient

    operator fun div(divider: ULong): BigUnsignedInt = divRem(BigUnsignedInt(divider)).quotient

    operator fun div(divider: XsdNonNegativeInteger) = when (divider) {
        is BigUnsignedInt -> divRem(divider)
        is XsdUnsignedLong -> divRem(BigUnsignedInt(divider.toULong()))
        is XsdUnsignedInt -> divRem(divider.toUInt())
        else -> divRem(BigUnsignedInt(divider))
    }.quotient

    operator fun div(divider: BigUnsignedInt): BigUnsignedInt = divRem(divider).quotient

    override fun plus(other: XsdNonNegativeInteger): BigUnsignedInt {
        if (other is BigUnsignedInt) { return plus(other) }
        return plus(BigUnsignedInt(other))
    }

    override fun plus(other: ULong): XsdNonNegativeInteger {
        val newInts = UIntArray(maxOf(size.toInt() + 1, 3))

        var carry: UInt
        run {
            val sum = (other and 0xffffffffuL) + get(0)
            carry = sum.shr(32).toUInt()
            newInts[0] = sum.toUInt()
        }
        run {
            val sum = (other shr 32) + get(1) + carry
            carry = sum.shr(32).toUInt()
            newInts[0] = sum.toUInt()
        }

        if (newInts.size>=2) {

            for (i in 2 until newInts.size) {
                val sum = get(i).toULong() + carry
                carry = sum.shr(32).toUInt()
                newInts[i] = sum.toUInt()
            }
        }

        return when {
            newInts.last() == 0u -> BigUnsignedInt(newInts.copyOfRange(0, newInts.size - 1), 0uL)
            else -> BigUnsignedInt(newInts, 0uL)
        }
    }

    override fun times(other: XsdNonNegativeInteger): BigUnsignedInt {
        return times(other as? BigUnsignedInt ?: BigUnsignedInt(other))
    }

    operator fun times(other: BigUnsignedInt): BigUnsignedInt {
        if (other.ints.size> ints.size) return other.times(this)

        val newInts = UIntArray(size.toInt() + other.size.toInt() + 1)
        val newExp = exp + other.exp

        for (otherIdx in other.ints.indices) {
            var carry = 0u
            for (idx in ints.indices) {
                val newIdx = idx + otherIdx
                val m = ints[idx].toULong() * other.ints[otherIdx].toULong() + carry + newInts[newIdx]
                newInts[newIdx] = m.toUInt()
                carry = m.shr(32).toUInt()
            }
            for (idx in ints.size until newInts.size) {
                val newIdx = idx + otherIdx
                val m = carry.toULong() + newInts[newIdx]
                newInts[newIdx] = m.toUInt()
                carry = m.shr(32).toUInt()
                if (carry == 0u) break // no carry, we are done
            }
        }

        var lastByteToKeep = newInts.lastIndex
        while (lastByteToKeep > 0 && newInts[lastByteToKeep] == 0u) lastByteToKeep-=1

        val array = if (lastByteToKeep + 1 == newInts.size) newInts else newInts.copyOf(lastByteToKeep + 1)

        return BigUnsignedInt(array, newExp)
    }

    operator fun plus(other: BigUnsignedInt): BigUnsignedInt {
        val newExponent = minOf(countTrailingZeroBits(), other.countTrailingZeroBits())
        val newInts = UIntArray(maxOf(ints.size, other.ints.size) + 1 - (newExponent shr 5).toInt())

/*
        val intShift = (newExponent - exp) shr 5
        val bitShiftRight = (newExponent - exp) and 0x1fu

        val otherIntShift = (newExponent - other.exp) shr 5
        val otherBitShiftRight = (newExponent - other.exp) and 0x1fu
*/

        // TODO, internalize int calculation as the current solution duplicates a lot

        var carry = 0u
        for (i in newInts.indices) {
            val a = getBitIndex(newExponent + i.toULong() * 32uL)
            val b = other.getBitIndex(newExponent + i.toULong() * 32uL)
            val sum = a + b + carry
            newInts[i] = sum
            carry = sum.shr(32)
        }

        return BigUnsignedInt(newInts, newExponent)
    }

    infix fun shl(shift: Int): BigUnsignedInt {
        require(shift >= 0) { "Shift must be non-negative" }
        if (shift == 0) return this
        return BigUnsignedInt(ints, exp + shift.toULong())
    }

    infix fun shl(shift: ULong): BigUnsignedInt {
        if (shift == 0uL) return this
        return BigUnsignedInt(ints, exp + shift)
    }

    infix fun shr(shift: Int): BigUnsignedInt {
        if (shift == 0) return this
        return shr(shift.toULong())
    }

    infix fun shr(shift: ULong): BigUnsignedInt {
        require(shift >= 0uL) { "Shift must be non-negative" }
        if (shift == 0uL) return this
        // we can just adjust the exponent
        if (shift <= exp) return BigUnsignedInt(ints, exp - shift)

        val lsbBitsToDrop = shift - exp
        val droppedInts = (lsbBitsToDrop shr 5).toInt()

        if (lsbBitsToDrop and 0x1fuL == 0uL) { // shift multiple of 32 means no array changes are needed
            return BigUnsignedInt(ints.copyOfRange(droppedInts, ints.size), exp - shift)
        }

        val shiftRight = (lsbBitsToDrop and 0x1fu).toInt() // only the bits, not the word shifts
        val newInts = UIntArray(ints.size - droppedInts) { i ->
            val idx = i + droppedInts
            when {
                idx + 1 >= ints.size -> (ints[idx] shr shiftRight)
                else -> (ints[idx] shr shiftRight) or (ints[idx + 1] shl (32 - shiftRight))
            }
        }

        // Actually going to trim bits.

        return BigUnsignedInt(newInts, 0uL)
    }

    override val schemaType: NonNegativeIntegerType<XsdNonNegativeInteger>
        get() = NonNegativeIntegerType.Instance

    override fun toLong(): Long = toULong().toLong()

    fun expandExp(): BigUnsignedInt {
        val leadingZeroBits = ints.last().countLeadingZeroBits().toUInt()
        val intsToAdd = ((exp + 31u - leadingZeroBits) shr 5).toInt()

        val newInts = UIntArray(ints.size + intsToAdd)

        val shift = exp.rem(32u).toInt()
        var carry  = 0u
        for (idx in ints.indices) {
            val mult = ints[idx].toULong().shl(shift)+carry

            newInts[idx + intsToAdd] += mult.toUInt()

            carry = mult.shr(32).toUInt()
        }

        var lastByteToKeep = newInts.lastIndex
        while (lastByteToKeep > 0 && newInts[lastByteToKeep] == 0u) lastByteToKeep-=1

        val array = if (lastByteToKeep + 1 == newInts.size) newInts else newInts.copyOf(lastByteToKeep + 1)

        return BigUnsignedInt(array, 0u)
    }

    override fun toULong(): ULong {
        if (exp > 0u) { // optimize, we don't need to do most msb stuff
            return expandExp().toULong()
        }
        return when (ints.size) {
            0 -> 0u
            1 -> ints[0].toULong()
            else -> ints[0].toULong() + (ints[1].toULong() shl 32)
        }
    }

    override fun toUInt(): UInt {
        // TODO optimize this
        return toULong().toUInt()
    }

    override fun toInt(): Int {
        return toUInt().toInt()
    }

    override fun compareTo(other: XsdInteger): Int {
        TODO("not implemented")
    }



    override val xmlString: String
        get() {
            when {
                exp != 0uL -> return expandExp().xmlString
                ints.size <= 2 -> return toULong().toString()
                else -> return buildString { appendTo(this) }
            }
        }

    internal fun appendTo(appendable: Appendable) {
        when {
            exp != 0uL -> expandExp().appendTo(appendable)
            ints.size <= 2 -> when (appendable) {
                is StringBuilder -> appendable.append(toULong())
                else -> appendable.append(toULong().toString())
            }

            else -> {
                val d: UIntDivRem = divRem(1_000_000_000u)
                d.quotient.appendTo(appendable)
                appendable.append(d.remainder.toString().padStart(9, '0'))
            }
        }
    }

    private fun divRem(divider: UInt): UIntDivRem { // will (initially) expand exponents
        val leadingZeroBits = ints.last().countLeadingZeroBits().toUInt()
        val intsToAdd = ((exp + 31u - leadingZeroBits) shr 5).toInt()

        when (ints.size + intsToAdd) {
            1 -> {
                val u = toUInt()
                val div: UInt = u.div(divider)
                val rem: UInt = u - (div * divider)
                return UIntDivRem(BigUnsignedInt(div), rem)
            }

            2 -> {
                val u = toULong()
                val div: ULong = u.div(divider.toULong())
                val rem: UInt = (u - (div * divider)).toUInt()
                return UIntDivRem(BigUnsignedInt(div), rem)
            }
        }

        val newInts = UIntArray(ints.size + intsToAdd)

        var rem: ULong = 0uL
        for (i in (ints.size - 1) downTo 0) {
            val baseInt = ints[i].toULong() + (rem shl 32)
            val div: ULong = baseInt.div(divider)
            newInts[i+intsToAdd] = div.toUInt()
            rem = baseInt - (div * divider)
        }
        for (i in (intsToAdd-1) downTo 0) {
            val baseInt = (rem shl 32)
            val div: ULong = baseInt.div(divider)
            newInts[i+intsToAdd] = div.toUInt()
            rem = baseInt - (div * divider)
        }

        var lastByteToKeep = newInts.lastIndex
        while (lastByteToKeep > 0 && newInts[lastByteToKeep] == 0u) lastByteToKeep-=1

        val array = if (lastByteToKeep + 1 == newInts.size) newInts else newInts.copyOf(lastByteToKeep + 1)
        return UIntDivRem(BigUnsignedInt(array, 0uL), rem.toUInt())

    }

    private fun divRem(divider: BigUnsignedInt): UIntDivRem { // will (initially) expand exponents
        val normalizedDivider = divider.expandExp()
        ifAssertions {
            assert(normalizedDivider.exp == 0uL) { "Divider must not have an exponent" }
            assert(normalizedDivider.ints.isNotEmpty()) { "Divider must have at least one integer" }
        }

        if (normalizedDivider.ints.size == 1) return divRem(normalizedDivider.ints[0])

        val leadingZeroBits = ints.last().countLeadingZeroBits().toUInt()
        val intsToAdd = ((exp + 31u - leadingZeroBits) shr 5).toInt()

        TODO("Full division not yet implemented")
/*
        val newInts = UIntArray(ints.size + intsToAdd)

        var rem: ULong = 0uL
        for (i in (ints.size - 1) downTo 0) {
            val baseInt = ints[i].toULong() + (rem shl 32)
            val div: ULong = baseInt.div(divider)
            newInts[i+intsToAdd] = div.toUInt()
            rem = baseInt - (div * divider)
        }
        for (i in (intsToAdd-1) downTo 0) {
            val baseInt = (rem shl 32)
            val div: ULong = baseInt.div(divider)
            newInts[i+intsToAdd] = div.toUInt()
            rem = baseInt - (div * divider)
        }

        var lastByteToKeep = newInts.lastIndex
        while (lastByteToKeep > 0 && newInts[lastByteToKeep] == 0u) lastByteToKeep-=1

        val array = if (lastByteToKeep + 1 == newInts.size) newInts else newInts.copyOf(lastByteToKeep + 1)
        return UIntDivRem(BigUnsignedInt(array, 0uL), rem.toUInt())
*/

    }

    override fun toString(): String = buildString {
        append("BigUnsignedInt(")
        for (i in ints.reversed()) {
            append(i.toString(16).padStart(8, '0'))
        }
        if (exp != 0uL) append(" × 2^$exp")
        append(')')
        return super.toString()
    }

    @ExperimentalXmlUtilApi
    data class UIntDivRem(val quotient: BigUnsignedInt, val remainder: UInt)

    @ExperimentalXmlUtilApi
    data class DivRem(val quotient: BigUnsignedInt, val remainder: BigUnsignedInt)

    private class ParseResult(val ints: UIntArray, val exp: ULong)

    companion object {

        private fun convert(original : XsdNonNegativeInteger): ParseResult {
            val exp = original.countTrailingZeroBits()

            val ints = UIntArray(original.size.toInt() - (exp shr 5).toInt()) { i ->
                (original[i] shr (exp and 0x1fu).toInt()) or (original[i + 1] shl (32 - (exp and 0x1fu).toInt()))
            }
            return ParseResult(ints, exp)
        }

        private fun parse(s: String): ParseResult {
            var normalised = s.trim()
            if (normalised.isEmpty()) throw NumberFormatException("Empty string")
            if (normalised[0] == '+') normalised = normalised.substring(1)

            val intsNeeded = 1 + s.length / 9 // not very accurate but good enough for now

            val last = normalised.length

            var numbers = UIntArray(intsNeeded)
            var tmp = UIntArray(intsNeeded)
            var intsUsed = 1


            var first = s.length.rem(9) // actually initialise it after the first substring

            if (first > 0) {
                numbers[0] = normalised.substring(0, minOf(first, last)).toUInt()
            }

            while (first < last) {
                val nextInt = normalised.substring(first, minOf(first+9, last)).toULong()

                val m = numbers[0].toULong() * 1_000_000_000uL + nextInt
                tmp[0] = m.toUInt()
                var carry: UInt = m.shr(32).toUInt()

                for (i in 1 until intsUsed) {
                    val m = numbers[i].toULong() * 1_000_000_000uL + carry

                    tmp[i] = m.toUInt()
                    carry = m.shr(32).toUInt()
                }

                if (carry != 0u) {
                    tmp[intsUsed] = carry
                    intsUsed += 1
                }

                for (i in intsUsed until tmp.size) { tmp[i] = 0u }

                val x = numbers
                numbers = tmp
                tmp = x

                first += 9
            }

            var exponentToUse = 0uL
//            if (intsUsed > 2) {
                for (i in 0 until intsUsed) {
                    val part = numbers[i]
                    if (part != 0u) {
                        exponentToUse = (i * 32 + part.countTrailingZeroBits()).toULong()
                        break
                    }
                }
//            }
            if (exponentToUse > 0uL) {
                val rightShift = exponentToUse.and(0x1fu).toInt()
                val intShift = (exponentToUse + 31u).shr(5).toInt() // add 31 to ensure a shift for at least 1 bit
                val leftShift = 32 - rightShift

                var previous: ULong = (numbers[0] shr rightShift).toULong()

                for (i in 0 until (numbers.size - intShift)) {
                    val x: ULong = previous + (numbers[i+intShift].toULong() shl leftShift)
                    tmp[i] = x.toUInt()
                    previous = x.shr(32)
                }

                numbers = tmp
            }

            var lastByteToKeep = numbers.lastIndex
            while (lastByteToKeep > 0 && numbers[lastByteToKeep] == 0u) lastByteToKeep-=1

            val array = if (lastByteToKeep + 1 == numbers.size) numbers else numbers.copyOf(lastByteToKeep + 1)
            return ParseResult(array, exponentToUse)
        }
    }
}
