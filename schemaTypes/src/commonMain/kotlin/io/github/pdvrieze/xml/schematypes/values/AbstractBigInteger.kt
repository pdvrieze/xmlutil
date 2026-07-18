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

import io.github.pdvrieze.xml.schematypes.values.instances.BigDecimal
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
import nl.adaptivity.xmlutil.core.impl.multiplatform.ifAssertions
import kotlin.jvm.JvmStatic

@OptIn(ExperimentalUnsignedTypes::class)
abstract class AbstractBigInteger<T : AbstractBigInteger<T>> protected constructor(
    internal val ints: UIntArray,
    internal val exp: ULong
) : XsdInteger {

    init {
        if (ints.size ==2) require(ints[1] != 0u) { "The second int must not be zero" }
    }

    abstract val self:T

    override fun toULong(): ULong {
        return when (size) {
            1uL -> get(0).toULong()
            else -> (get(0).toULong() or get(1).toULong().shl(32))
        }
    }

    override fun countTrailingZeroBits(): ULong {
        return ints.countTrailingZeroBits().toULong()
    }

    protected fun countLeadingZeroBits(): ULong {
        return ints.countLeadingZeroBits().toULong()
    }

    override fun significantBitsFromZero(): ULong {
        return ints.significantBits().toULong() + exp
    }

    override val size: ULong
        get() = ints.size.toULong() + ((31u + exp) shr 5)

    override fun toBigDecimal(): BigDecimal {
        return BigDecimal(this)
    }

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

        if (byteShift >= ints.size) return 0u

        if (bitShift == 0) return ints[byteShift]

        val lsi = if (byteShift < 0) 0u else (ints[byteShift] shr bitShift)
        val msi = if (byteShift+1 >=ints.size) 0u else (ints[byteShift + 1] shl (32 - bitShift))

        return lsi or msi
    }

    abstract operator fun div(divider: T): AbstractBigInteger<*>

    protected abstract fun newInstance(value: Long): T

    protected abstract fun newInstance(sign: Int, elems: UIntArray, exp: ULong): T

    /**
     * @param elems The base elements for the integer
     * @params exp The exponent of the base elements. This  may not be the final value
     */
    protected open fun createOptimizedInstance(sign: Int, elems: UIntArray, exp: ULong): T {
        val trailingBits = elems.countTrailingZeroBits()

        val originIntsToSkip = (trailingBits shr 5) // ints to remove at the ls side (rounds down)

        when ((elems.size.toLong() shl 5) - originIntsToSkip) {
            0L -> return newInstance(0L)
            1L -> return newInstance(sign, BigUnsignedInt.ONE.ints, exp + 31u) // single bit, still needs exp
        }

        val mostSigBit = elems.significantBits()

        val bitShiftToRight = when (val s = (trailingBits and 0x1f)) {
            0, 1, 2, 3, 4 -> 0 // Ignore 4 or fewer trailing zeros
            else -> s
        }

        if (bitShiftToRight == 0) { // optimize the case where no shifts are needed
            val newMax = (31 + mostSigBit).shr(5)
            val newElems = elems.copyOfRange(originIntsToSkip, newMax)
            val newExp: ULong = exp + (originIntsToSkip.toULong() shl 5)
            return newInstance(sign, newElems, newExp)

        } else {
            val newBitCount = mostSigBit - trailingBits

            val newElems = UIntArray((newBitCount + 31).shr(5))

            // we always move a bit of n+1 into n, using trailingInts as offset into elem
            if (newElems.size > 1) {
                for (i in 0..(newElems.size - 2)) {
                    val lsbits = elems[i + originIntsToSkip].shr(bitShiftToRight)
                    val hsbits = elems[i + originIntsToSkip + 1].shl(32 - bitShiftToRight)
                    newElems[i] = lsbits or hsbits
                }
            }
            val lastIndex = newElems.lastIndex

            val lsbits = elems[lastIndex + originIntsToSkip].shr(bitShiftToRight)
            val hsbits = when (val idx = lastIndex + originIntsToSkip + 1) {
                in elems.indices -> elems[idx].shl(32 - bitShiftToRight)
                else -> 0u
            }
            newElems[lastIndex] = lsbits or hsbits

            val newExp: ULong = exp + (originIntsToSkip.toULong() shl 5) + bitShiftToRight.toULong()
            return newInstance(sign, newElems.trimTrailingZeros(), newExp)
        }
    }

    override fun plus(other: XsdDecimal): XsdDecimal = when (other) {
        is XsdInteger -> plus(other)
        else -> other.plus(this)
    }

    override operator fun plus(other: XsdInteger): XsdInteger =
        BigInt(this).plus(other)

    override fun minus(other: XsdDecimal): XsdDecimal = when (other) {
        is XsdInteger -> minus(other)
        else -> plus(other.unaryMinus())
    }

    override operator fun minus(other: XsdInteger): XsdInteger =
        BigInt(this).minus(other)

    override fun times(other: XsdDecimal): XsdDecimal = when (other){
        is XsdInteger -> BigInt(this).times(other)
        else -> other.times(this)
    }

    operator fun times(other: T): T {
        @Suppress("UNCHECKED_CAST")
        if (other.ints.size> ints.size) return other.times(self)

        val lSign = sign
        val rSign = other.sign
        if (lSign == 0 || rSign == 0) return newInstance(0L)

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

        return createOptimizedInstance(sign * other.sign, newInts, newExp)
    }

    override operator fun times(multiplier: UInt): T {
        val newInts = UIntArray(size.toInt() + 1)

        var carry = 0u
        for (idx in ints.indices) {
            val m = ints[idx].toULong() * multiplier.toULong() + carry + newInts[idx]
            newInts[idx] = m.toUInt()
            carry = m.shr(32).toUInt()
        }

        val m = carry.toULong() + newInts[ints.size]
        newInts[ints.size] = m.toUInt()
        assert(m.shr(32).toUInt() == 0u)

        return createOptimizedInstance(sign, newInts, exp)
    }

    infix fun shl(shift: Int): T {
        require(shift >= 0) { "Shift must be non-negative" }
        if (shift == 0) return self
        return newInstance(sign, ints, exp + shift.toULong())
    }

    infix fun shl(shift: ULong): T {
        if (shift == 0uL) return self
        return newInstance(sign, ints, exp + shift)
    }

    infix fun shr(shift: Int): T {
        if (shift == 0) return self
        return shr(shift.toULong())
    }

    infix fun shr(shift: ULong): T {
        require(shift >= 0uL) { "Shift must be non-negative" }
        if (shift == 0uL) return self
        // we can just adjust the exponent
        if (shift <= exp) return newInstance(sign, ints, exp - shift)

        val lsbBitsToDrop = shift - exp
        val droppedInts = (lsbBitsToDrop shr 5).toInt()

        var significantInts = ints.size
        while (significantInts > 0 && ints[significantInts - 1] == 0u) significantInts -= 1

        if (lsbBitsToDrop and 0x1fuL == 0uL) { // shift multiple of 32 means no array changes are needed
            return newInstance(sign, ints.copyOfRange(droppedInts, significantInts), exp - shift)
        }

        val shiftRight = (lsbBitsToDrop and 0x1fu).toInt() // only the bits, not the word shifts
        if (significantInts<=droppedInts) return newInstance(0)

        val newInts = UIntArray(significantInts - droppedInts) { i ->
            val idx = i + droppedInts
            when {
                idx + 1 >= significantInts -> (ints[idx] shr shiftRight)
                else -> (ints[idx] shr shiftRight) or (ints[idx + 1] shl (32 - shiftRight))
            }
        }

        // Actually going to trim bits.

        return createOptimizedInstance(sign, newInts, 0uL)
    }

    /**
     * Get the absolute value of this value.
     */
    final override fun abs(): BigUnsignedInt {
        return when {
            sign == 0 -> BigUnsignedInt.ZERO
            else -> BigUnsignedInt(ints, exp)
        }
    }

    fun toSigned(): BigInt {
        return BigInt(sign, ints, exp)
    }

    fun expandExp(): T {
        return expandWithEffectiveExp(exp)
    }

    /**
     * Helper function that elides a copy in normalization for division. There we need to
     * shift left and then expand.
     */
    protected fun expandWithEffectiveExp(exp: ULong): T {
        val leadingZeroBits = ints.countLeadingZeroBits()
        val intsToAddX = ((exp.toLong() + 31 - leadingZeroBits) shr 5).toInt()

        val newInts = UIntArray(ints.size + intsToAddX)

        val bitShiftLeft = exp.and(0x1fu).toInt()
        val intShiftLeft = exp.shr(5).toInt()
        var carry = 0u
        for (idx in 0 until (ints.size - (leadingZeroBits shr 5))) {
            val mult = ints[idx].toULong().shl(bitShiftLeft) + carry

            newInts[idx + intShiftLeft] += mult.toUInt()

            carry = mult.shr(32).toUInt()
        }

        if (carry > 0u) newInts[newInts.lastIndex] = carry

        return newInstance(sign, newInts.trimTrailingZeros(), 0u)
    }

    override fun compareTo(other: XsdInteger): Int {
        if (sign != other.sign) return sign.compareTo(other.sign)
        // optimize for 2 BigUnsignedInts
        val s = significantBitsFromZero()
        val os = significantBitsFromZero()
        when {
            s < os -> return -1
            s > os -> return 1
        }

        // note that we must subtract a bit so that a 32-bit value only has one int.
        val start = (s - 1u) shr 5

        // TODO optimize to deal with exponents and int alignment
        for (i in start downTo 0u) {
            val v = get(i)
            val o = other[i]
            when {
                v < o -> return -1
                v > o -> return 1
            }
        }
        return 0
    }



    override val xmlString: String
        get() {
            return when {
                exp != 0uL -> expandExp().xmlString
                sign == 0 -> "0"
                else -> {
                    val sigBits = significantBitsFromZero()
                    when {
                        sigBits <= 63u -> toLong().toString()
                        else -> buildString { appendTo(this) }
                    }
                }
            }
        }

    internal fun appendTo(appendable: Appendable) {
        when {
            exp != 0uL -> expandExp().appendTo(appendable)
            ints.size <= 2 && ints.countLeadingZeroBits() > 0 -> when (appendable) {
                is StringBuilder -> appendable.append(toLong())
                else -> appendable.append(toLong().toString())
            }

            else -> {
                if (sign < 0) appendable.append('-')
                val d = unsignedDivRem(1_000_000_000u)
                d.quotient.appendTo(appendable)
                appendable.append(d.uintRemainder.toString().padStart(9, '0'))
            }
        }
    }

    /** Implementation of the division algorithm that ignores all signs. */
    protected fun unsignedDivRem(divider: UInt, targetExp: Long = -1L): UnsignedDivRemUInt { // will (initially) expand exponents
        if (divider == 0u) throw ArithmeticException("Division by zero")
        if (sign == 0) return UnsignedDivRemUInt(BigUnsignedInt.ZERO, 0u)

        val leadingZeroBits = ints.last().countLeadingZeroBits()
        val intsToAdd = ((exp.toInt() + 31 - leadingZeroBits) shr 5)

        when (ints.size + intsToAdd) {
            1 -> {
                val u = if (exp>=32uL) 0u else get(0)
                val div: UInt = u.div(divider)
                val rem: UInt = u - (div * divider)

                val rq = when (targetExp) {
                    -1L, 0L -> BigUnsignedInt(div)
                    else -> BigUnsignedInt(div).expandWithEffectiveExp(targetExp.toULong())
                }

                return UnsignedDivRemUInt(rq, rem)
            }

            2 -> {
                val u = when {
                    exp >= 64uL -> 0uL

                    exp >= 32uL -> get(1).toULong().shl(32)

                    exp > 0uL || ints.size > 1 -> get(1).toULong().shl(32) or get(0).toULong()

                    else -> ints[0].toULong()
                }
                val div: ULong = u.div(divider.toULong())
                val rem: UInt = (u - (div * divider)).toUInt()
                val rq = when (targetExp) {
                    -1L, 0L -> BigUnsignedInt(div)
                    else -> BigUnsignedInt(div).expandWithEffectiveExp(targetExp.toULong())
                }

                return UnsignedDivRemUInt(rq, rem)
            }
        }

        if (exp > 0u) return expandExp().unsignedDivRem(divider, targetExp)

        val newInts = UIntArray(ints.size + intsToAdd)

        var rem = 0uL
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

        val rq = when (targetExp) {
            -1L -> BigUnsignedInt(newInts.trimTrailingZeros(), 0uL).normalize()
            0L -> BigUnsignedInt(newInts.trimTrailingZeros(), 0uL)
            else -> BigUnsignedInt(newInts, 0uL).expandWithEffectiveExp(targetExp.toULong())
        }

        return UnsignedDivRemUInt(rq, rem.toUInt())
    }

    abstract override fun divRem(divider: UInt): IntDivRem<*, *>

    abstract fun divRem(divider: T): DivRem<AbstractBigInteger<*>, AbstractBigInteger<*>>

    fun unsignedDivRem(divider: T): UnsignedDivRem { // will (initially) expand exponents
        // Deal with single element division separately
        if (divider.size == 1uL) return unsignedDivRem(divider[0]).toDivRem()

        val leadingSignificantBits = significantBitsFromZero()
        val divSignificantBits = divider.significantBitsFromZero()

        // We are certain the divider is bigger than the dividend so we will have 0 quotient and
        // dividend as remainder.
        if (divSignificantBits > leadingSignificantBits) {
            return UnsignedDivRem(BigUnsignedInt.ZERO, abs().asBigUnsignedInt())
        }

        // For now needed for long multiplication
        // Note that d can be anything that makes MSI[quotient]*d leq to 0x8000_0000
        // this could
        val divSigBits = (divSignificantBits and 0x1fuL).toInt() // the less significant values are multiples of 32
        val shiftLeft_d = when {
            divSigBits > 16 -> 0 // no need to shift

            else -> { // we allow for choosing the shift that keeps int alignment (with exponent%32!=0)
                val shiftWithExp = (32 - (divider.exp and 0x1fuL).toInt())

                when (shiftWithExp + divSigBits) {
                    in 17..31 -> shiftWithExp

                    else -> 17 - divSigBits // make that the msb is bit 17
                }
            }
        }.toULong()



        // only normalize divider after we have decided no shortcuts apply
        // TODO optimize this into a single function that combines both
        val normalizedDivider = divider.expandWithEffectiveExp(divider.exp + shiftLeft_d).apply {
            ifAssertions {
                assert(exp == 0uL) { "Divider must not have an exponent" }
                assert(ints.size > 1) { "Divider must have at least two integers" }
            }
        }.ints


        val dividerSize_n = normalizedDivider.size


        val normalizedInts = expandWithEffectiveExp(exp + shiftLeft_d).ints
        ifAssertions {
            assert(normalizedInts.last() != 0u) { "Normalized ints must have non-zero most-significant-int" }
        }

        val growth_m = normalizedInts.size - dividerSize_n


        val divMSI = normalizedDivider.last() // we use this to determine the approximate quotient
        val divMSI2 = normalizedDivider[normalizedDivider.size-2] // we use this to determine the approximate quotient

        val quotient = UIntArray(growth_m + 1) // note this can be negative if there

        run {//
            val j = growth_m
            val divident = normalizedInts.last()
            var qX: ULong = divident.toULong() / divMSI // never too big
            assert(qX <= 0x1_0000_0000uL) { "Should always be an UInt" }
            var rX: ULong = divident - (qX * divMSI) // approximate remainder

            while (rX < 0x1_0000_0000uL && qX * divMSI2 > (rX shl 32) + normalizedInts[j + dividerSize_n - 2]) {
                qX -= 1u
                rX += 1u
            }

            if(multiplySubtractInPlace(normalizedInts, normalizedDivider, qX.toUInt(), j))
                qX-=1uL
            quotient[growth_m] = qX.toUInt()
        }

        for (j in (growth_m -1) downTo 0) {
            val divident = (normalizedInts[dividerSize_n+j].toULong().shl(32) + normalizedInts[dividerSize_n +j - 1].toULong())
            var qX: ULong = divident / divMSI
            var rX: ULong = divident - (qX * divMSI) // approximate remainder

            do {
                if (qX * divMSI2 > (rX shl 32) + normalizedInts[j + dividerSize_n - 2]) {
                    qX -= 1u
                    rX += 1u
                } else {
                    break
                }
            } while (rX < 0x1_0000_0000uL)

            if(multiplySubtractInPlace(normalizedInts, normalizedDivider, qX.toUInt(), j)) {
                qX -= 1uL
            }
            quotient[j] = qX.toUInt()
        }

        // Note that shr will optimize, no need here, but trailing zeros still need dropping
        val remainder = BigUnsignedInt(normalizedInts.trimTrailingZeros(), 0uL).shr(shiftLeft_d)

        return UnsignedDivRem(BigUnsignedInt(quotient, 0uL).normalize(), remainder)

    }

    /**
     * Perform an in place multiplySubtract modifying [target]
     * @param target The array to perform the multiplySubtract on
     * @param a The array to multiply. For division this is the divider
     * @param multiplier The single token multiplier the one "int" digit of the divider
     * @param leftOffset The offset of the first int in the target array.
     * @return If a "borrow" was needed
     */
    private fun multiplySubtractInPlace(target: UIntArray, a: UIntArray, multiplier: UInt, leftOffset: Int): Boolean {
        if (multiplier == 0u) return false
        var carry = 0u
        var borrow = 0u
        for (i in 0 until a.size) {
            val ti = leftOffset + i

            val mFull = (a[i].toULong() * multiplier.toULong() + carry)
            carry = mFull.shr(32).toUInt()
            val m = mFull and 0xffff_ffffuL
            val t = target[ti] - borrow
            if (t < m) {
                borrow = ((m-t) shr 32).toUInt()
                target[ti] = (0xffff_ffff_0000_0000uL+t-borrow).toUInt()
            } else {
                borrow = 0u
                target[ti] = (t-m).toUInt()
            }
        }
        val toReduce = borrow + carry

        if (toReduce == 0u) return false

        val tPos = a.size + leftOffset + 1
        val t = if (tPos<target.size) target[tPos] else 0u
        if (t >= toReduce) {
            target[a.size + leftOffset] = t - toReduce
            return false
        }
        // Else Step D6 - Add a again
        carry = 0u
        for(i in 0 until a.size) {
            val add = target[i+leftOffset].toULong() + a[i]
            target[i+leftOffset] = add.toUInt()
            carry = add.shr(32).toUInt()
        }
        target[a.size + leftOffset] = t + carry - toReduce
        return true
    }


    override fun equals(other: Any?): Boolean {
        if (other !is XsdInteger) return false
        return compareTo(other) == 0
    }

    /*
     * Note that this implementation
     */
    override fun hashCode(): Int {
        var result = sign.hashCode()
        if (ints.isEmpty() || sign == 0) return result
        result = 31 * result + exp.hashCode()
        var i = (size - 1uL)
        while (i >= 0uL && get(i) == 0u) {
            i -= 1uL
        }
        for (j in 0uL..i) {
            result = 31 * result + get(j).hashCode()
        }
        return result
    }


    override fun toString(): String = buildString {
        append("Big...Int(")
        ints.reversed().joinTo(this, "_") {
            it.toString(16).padStart(8, '0')
        }

        if (exp != 0uL) append(" × 2^$exp")
        append(')')
    }

    interface PosDivRem : DivRem<BigUnsignedInt, BigUnsignedInt> {
        override val quotient: BigUnsignedInt
        override val remainder: BigUnsignedInt
    }

    interface DivRem<out Q: AbstractBigInteger<out Q>, out R: AbstractBigInteger<out R>>: XsdInteger.DivRem {
        override val quotient: Q
        override val remainder: R

        override fun component1(): Q = quotient
        override fun component2(): R = remainder
    }

    interface IntDivRem<out Q: AbstractBigInteger<out Q>, out R: AbstractBigInteger<out R>>: DivRem<Q, R>, XsdInteger.IntDivRem
    interface UIntDivRem<out Q: AbstractBigInteger<out Q>, out R: AbstractBigInteger<out R>>: DivRem<Q, R>, XsdInteger.UIntDivRem

    @ExperimentalXmlUtilApi
    @XmlUtilInternal
    typealias UnsignedDivRemUInt = BigUnsignedInt.UIntDivRem

    @ExperimentalXmlUtilApi
    @XmlUtilInternal
    typealias UnsignedDivRem = BigUnsignedInt.PosDivRem

    companion object {
        @JvmStatic
        internal fun UIntArray.countTrailingZeroBits(): Int {
            for (i in this.indices) {
                val v = get(i)
                if (v != 0u) return (i * 32) + v.countTrailingZeroBits()
            }
            throw ArithmeticException("The value is zero, no trailing zero bits")
        }

        @JvmStatic
        protected fun UIntArray.countLeadingZeroBits(): Int {
            for (i in this.indices.reversed()) {
                val v = get(i)
                if (v != 0u) return ((size - 1 - i) shl 5) + v.countLeadingZeroBits()
            }
            return size shl 5
        }

        @JvmStatic
        protected fun UIntArray.significantBits(): Int {
            return (size shl 5) - countLeadingZeroBits()
        }

        @JvmStatic
        protected fun UIntArray.trimTrailingZeros(): UIntArray {
            var newSize = size
            while (newSize > 1 && this[newSize-1] == 0u) newSize -= 1
            return if (newSize != size) copyOfRange(0, newSize) else this
        }

    }
}
