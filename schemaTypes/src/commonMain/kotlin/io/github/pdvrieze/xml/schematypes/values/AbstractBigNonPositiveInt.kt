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

@OptIn(ExperimentalUnsignedTypes::class)
abstract class AbstractBigNonPositiveInt<T : AbstractBigNonPositiveInt<T>> protected constructor(
    ints: UIntArray,
    exp: ULong
) : AbstractBigInteger<T>(ints, exp), XsdNonPositiveInteger {

    init {
        require(ints.isNotEmpty()) { "At least one integer must be present" }
        if (ints.size == 1 && ints[0] == 0u) {
            require(exp == 0uL) { "The value is zero, but an exponent is present" }
        }
    }

    internal class ParseResult(val ints: UIntArray, val exp: ULong)

    override fun newInstance(value: Long): T {
        require(value >= 0) { "Value must be non-negative" }
        return newInstance(value)
    }

    override fun toULong(): ULong = when {
        exp >= 64uL -> 0uL

        exp >= 32uL -> get(1).toULong().shl(32)

        exp >0uL || ints.size > 1 -> get(1).toULong().shl(32) or get(0).toULong()

        else -> ints[0].toULong()
    }

    override fun toUInt(): UInt = when {
        exp >= 32uL -> 0u

        else -> get(0)
    }

    override fun toInt(): Int = toUInt().toInt()

    override fun toLong(): Long = toULong().toLong()

    operator fun div(divider: UInt): BigNonPositiveInt = divRem(divider).quotient

    operator fun div(divider: ULong): BigNonPositiveInt = divRem(BigUnsignedInt(divider)).quotient

    abstract fun divRem(divider: XsdNonNegativeInteger): DivRem<BigNonPositiveInt, AbstractBigNonPositiveInt<*>>

    abstract fun divRem(divider: XsdNonPositiveInteger): DivRem<AbstractBigUnsignedInt<*>, AbstractBigNonPositiveInt<*>>

    open operator fun div(divider: XsdNonPositiveInteger): XsdNonNegativeInteger {
        return divRem(divider).quotient
    }

    abstract fun divRem(divider: UInt): DivRem<BigNonPositiveInt, Int>

    override operator fun plus(other: XsdNonPositiveInteger): XsdNonPositiveInteger {
        if (other is BigNonPositiveInt) { return plus(other) }
        return plus(BigNonPositiveInt(other))
    }

    operator fun plus(other: T): T {
        val newExponent = minOf(countTrailingZeroBits(), other.countTrailingZeroBits())
        val newInts = UIntArray(maxOf(ints.size, other.ints.size) + 1 - (newExponent shr 5).toInt())

        // TODO, internalize int calculation as the current solution duplicates a lot

        var carry = 0u
        for (i in newInts.indices) {
            val a = getBitIndex(newExponent + i.toULong() * 32uL)
            val b = other.getBitIndex(newExponent + i.toULong() * 32uL)
            val sum = a + b + carry
            newInts[i] = sum
            carry = sum.shr(32)
        }

        return createOptimizedInstance(newInts, newExponent)
    }

    override fun plus(other: ULong): T {
        val newInts = when {
            exp == 0uL && ints.size == 1 -> ints.copyOf(2)
            else -> expandExp().ints // with exp>0 will be at least 2 ints big
        }

        var carry: UInt
        run {
            val sum = (other and 0xffffffffuL) + newInts[0]
            carry = sum.shr(32).toUInt()
            newInts[0] = sum.toUInt()
        }
        run {
            val sum = (other shr 32) + newInts[1] + carry
            carry = sum.shr(32).toUInt()
            newInts[1] = sum.toUInt()
        }

        if (newInts.size>=2 && carry>0u) {

            for (i in 2 until newInts.size) {
                val sum = get(i).toULong() + carry
                carry = sum.shr(32).toUInt()
                newInts[i] = sum.toUInt()
            }
        }

        return createOptimizedInstance( newInts, 0uL)
    }

    operator fun minus(other: T): T {
        val bitCount = significantBitsFromZero()/*
        var r = exp + ints.size.toULong() * 32uL
        for (i in ints.indices.reversed()) {
            when (val v = ints[i]) {
                0u -> r -= 32uL
                else -> return r - v.countLeadingZeroBits().toULong()
            }
        }
        return 0uL // no non-zero value found at all
*/
        val otherBitcount = other.significantBitsFromZero()/*
        var r = exp + ints.size.toULong() * 32uL
        for (i in ints.indices.reversed()) {
            when (val v = ints[i]) {
                0u -> r -= 32uL
                else -> return r - v.countLeadingZeroBits().toULong()
            }
        }
        return 0uL // no non-zero value found at all
*/
        if (otherBitcount > bitCount) throw ArithmeticException("Integer underflow in subtraction")
        val resultInts = UIntArray(((bitCount+31u) shr 5).toInt())
        val startIdx = (minOf(exp, other.exp) shr 5).toInt() // allows skipping full int skips

        var borrow: Long = 0L
        for (i in startIdx until resultInts.size) {
            val a = get(i).toLong() + borrow
            val b = other.get(i).toLong()
            // using bitwise and as mod to avoid branches
            val diff: Long = (a - b)
            resultInts[i] = diff.and(0xffff_ffff).toUInt()
            borrow = diff.shr(63) // effectively fill with sign bit (which is -1)
        }


        return createOptimizedInstance(resultInts, 0uL)
    }

    internal fun createOptimizedInstance(elems: UIntArray, exp: ULong): T {
        return super.createOptimizedInstance(1, elems, exp)
    }

    override fun compareTo(other: XsdInteger): Int = when {
        other.sign < 0 -> 1
        other.sign == 0 -> sign
        else -> compareTo(other.abs())
    }


    override fun compareTo(other: XsdNonPositiveInteger): Int {
        if (other is BigNonPositiveInt) return compareTo(other as AbstractBigInteger<*>)

        // optimize for 2 BigUnsignedInts
        val s = size
        val os = other.size
        when {
            s < os -> if ((s until os).any { other[it] != 0u }) return -1
            s > os -> if ((os until s).any { get(it) != 0u }) return 1
        }
        for (i in (s-1u) downTo 0u) {
            val v = get(i)
            val o = other[i]
            when {
                v < o -> return -1
                v > o -> return 1
            }
        }
        return 0
    }

    companion object {

        internal fun convert(original : XsdNonPositiveInteger): ParseResult {
            if (original is AbstractBigNonPositiveInt<*>) {
                return ParseResult(original.ints, original.exp)
            }

            val exp = original.countTrailingZeroBits()

            val ints = UIntArray(original.size.toInt() - (exp shr 5).toInt()) { i ->
                (original[i] shr (exp and 0x1fu).toInt()) or (original[i + 1] shl (32 - (exp and 0x1fu).toInt()))
            }
            return ParseResult(ints, exp)
        }

        internal fun parse(s: CharSequence): ParseResult {
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
