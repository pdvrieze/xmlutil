/*
 * Copyright (c) 2026.
 *
 * This file is part of xmlutil. It is a derivation/port of
 * https://github.com/jk-jeon/dragonbox/blob/master/subproject/simple/include/simple_dragonbox.h
 *
 * The contents of this file may be used under the terms of
 * the Apache License v2.0 with LLVM Exceptions.
 * (See accompanying file LICENSE-Apache or copy at
 *  https://llvm.org/foundation/relicensing/LICENSE.txt)
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

@file:OptIn(ExperimentalUnsignedTypes::class)

package io.github.pdvrieze.xml.schematypes.values.instances

import io.github.pdvrieze.xml.schematypes.requireRange
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
import kotlin.jvm.JvmInline

/* Implementation of dragonbox based on
 * https://github.com/jk-jeon/dragonbox/blob/master/subproject/simple/include/simple_dragonbox.h
 * and https://onlinelibrary.wiley.com/doi/epdf/10.1002/spe.70056
 */

private interface DecimalFP<SignificandType> {
    val significand: SignificandType
    val exponent: Int
    val is_negative: Boolean
}


data class DecimalFP64(override val significand: ULong, override val exponent: Int, override val is_negative: Boolean): DecimalFP<ULong> {


    fun toBigDecimal(): BigDecimal {
        val l = if(is_negative) -(significand.toLong()) else significand.toLong()
        return BigDecimal(l).exp10(exponent)
    }


    companion object {

    }
}

private fun ULong.rotr(r: Int): ULong {
    val r = r and 0x3f
    return (this shr r) or (this shl ((64 - r) and 63))
}

internal fun UInt.isEven() = this % 2u == 0u


internal fun UInt.floorLog2(): Int = 31 - countLeadingZeroBits()

private fun ULong.floorLog2(): Int = 63 - countLeadingZeroBits()

internal fun Int.floorLog10Pow2(): Int {
    requireRange(this in -2620..2620) { "Exponent out of range: ${this}"}
    return (this * 315653) shr 20
}

internal fun Int.floorLog2Pow10():Int {
    // Formula itself holds on [-4003,4003]; [-1233,1233] is to ensure no overflow.
    requireRange(this in -1233..1233) { "Exponent out of range: ${this}"}
    return (this * 1741647) shr 19
}

internal fun Int.floorLog10Pow2MinusLog10_4Over3(): Int {
    requireRange(this in -2985..2936)
    return (this * 631305 - 261663) shr 21
}

internal fun Int.floorLog5Pow2(): Int {
    assert(this in -1831..1831)
    return (this * 225799) shr 19
}

internal fun Int.floorLog5Pow2MinusLog5_3(): Int {
    assert(this in -3543..2427)
    return (this * 451597 - 715764) shr 20
}

private class UInt128(val high: ULong, val low: ULong) {
    operator fun plus(otherLow: ULong): UInt128 {
        val newLow = low + otherLow
        val newHigh = if (newLow < low) high + 1u else high
        return UInt128(newHigh, newLow)
    }
}

/**
 * Get 128-bit result of multiplication of two 64-bit unsigned integers.
 */
private fun umul128(x: ULong, y: ULong): UInt128 {
    val a = x shr 32
    val b = x and 0xFFFF_FFFFu
    val c = y shr 32
    val d = y and 0xFFFF_FFFFu

    val ac = a * c
    val bc = b * c
    val ad = a * d
    val bd = b * d

    val intermediate = (bd shr 32) + ((ad + bc) and 0xFFFF_FFFFu)

    return UInt128(
        high = ac + (intermediate shr 32) + (ad shr 32) + (bc shr 32),
        low = (intermediate shl 32) + (bd and 0xFFFF_FFFFu)
    )
}

/**
 * Get high half of the 128-bit result of multiplication of two 64-bit unsigned integers.
 */
private fun umul128_upper64(x: ULong, y: ULong): ULong {
    val a = x shr 32
    val b = x and 0xFFFF_FFFFu
    val c = y shr 32
    val d = y and 0xFFFF_FFFFu

    val ac = a * c
    val bc = b * c
    val ad = a * d
    val bd = b * d

    val intermediate = (bd shr 32) + (ad and 0xFFFF_FFFFu) + (bc and 0xFFFF_FFFFu)

    return ac + ((intermediate + ad + bc) shr 32)
}


// Get upper 128-bits of multiplication of a 64-bit unsigned integer and a 128-bit
// unsigned integer.
private fun umul192_upper128(x: ULong, y: UInt128): UInt128 {
    var r = umul128(x, y.high)
    r += umul128_upper64(x, y.low)
    return r
}

// Get upper 64-bits of multiplication of a 32-bit unsigned integer and a 64-bit
// unsigned integer.
internal fun umul96_upper64(x: UInt, y: ULong): ULong {
    val yh = (y shr 32)
    val yl = (y and 0xffff_ffffu)

    val xyh = x * yh
    val xyl = x * yl

    return xyh + (xyl shr 32)
}

// Get lower 128-bits of multiplication of a 64-bit unsigned integer and a 128-bit
// unsigned integer.
private fun umul192_lower128(x: ULong, y: UInt128): UInt128 {
    val high = x * y.high
    val high_low = umul128(x, y.low)
    return UInt128(
        high = (high + high_low.high) and (0xffffffffffffffffuL),
        low = high_low.low
    )
}

// Get lower 64-bits of multiplication of a 32-bit unsigned integer and a 64-bit
// unsigned integer.
internal fun umul96_lower64(x: UInt, y: ULong): ULong {
    return x.toULong() * y
}

//template <int k, class Int>

/** 10 to the power k */
internal fun computePower10(k: UInt): UInt {
    //static_assert(k >= 0);
    var a = 10u
    var e = k
    var p = 1u
    while (e != 0u) {
        if (e and 1u != 0u) p *= a
        e = e shr 1
        a *= a
    }
    return p
}

/**
 * 10 to the power k.
 */
private fun computePower10Long(k: UInt): ULong {
    //static_assert(k >= 0);
    var a = 10uL
    var e = k
    var p = 1uL
    while (e!=0u) {
        if (e and 1u != 0u) p *= a
        e = e shr 1
        a *= a
    }
    return p
}

//template <int k, class Int>

interface ComputeMulResult<T> {
    val integer_part: T
    val is_integer: Boolean
}

class compute_mul_resultUInt(override val integer_part: UInt, override val is_integer: Boolean): ComputeMulResult<UInt>
class compute_mul_resultULong(override val integer_part: ULong, override val is_integer: Boolean): ComputeMulResult<ULong>

@JvmInline
value class compute_mul_parity_result private constructor(private val packed: Int) {
    val parity get() = packed and 1 != 0
    val is_integer get() = packed and 2 != 0
    constructor(parity: Boolean, is_integer: Boolean): this (if (parity) 1 else 0 or if (is_integer) 2 else 0)
}

interface FloatFormat<out T> {
    val total_bits: Int
    val significand_bits: Int
    val exponent_bits: Int
    val min_exponent: Int
    val max_exponent: Int
    val exponent_bias: Int
    val decimal_significand_digits: Int
    val decimal_exponent_digits: Int
    val cache_bits: Int
    val min_k: Int
    val max_k: Int
    val max_output_string_length: Int
}

class MutableUInt(var value: UInt) {
    operator fun times(other: UInt): UInt = value * other
}

class MutableULong(var value: ULong) {
    operator fun times(other: ULong): ULong = value * other
    operator fun times(other: UInt): ULong = value * other
}

class MutableInt(var value: Int) {
    operator fun plusAssign(other: Int) {
        value += other
    }
}


private object float_format_float: FloatFormat<Float> {
    override val total_bits = 32
    override val significand_bits = 23
    override val exponent_bits = 8
    override val min_exponent = -126
    override val max_exponent = 127
    override val exponent_bias = -127
    override val decimal_significand_digits = 9
    override val decimal_exponent_digits = 2
    override val cache_bits = 64
    override val min_k = -31
    override val max_k = 46
    override val max_output_string_length = 1 + decimal_significand_digits + 1 + 1 + 1 + decimal_exponent_digits


    fun compute_mul(u: UInt, cache: ULong): compute_mul_resultUInt {
        val r = umul96_upper64(u, cache)
        return compute_mul_resultUInt((r shr 32).toUInt(), r == 0uL)
    }

    fun compute_delta(cache: ULong, beta: Int): UInt {
        return (cache shr (cache_bits - 1 - beta)).toUInt()
    }

    fun compute_mul_parity(two_f: UInt, cache: ULong, beta: Int): compute_mul_parity_result {
        assert(beta >= 1)
        assert(beta <= 32)
        val r = umul96_lower64(two_f, cache)
        return compute_mul_parity_result(
            parity = ((r shr (64 - beta)) and 1uL) != 0uL,
            is_integer = (0xFFFFFFFFuL and (r shr (32 - beta))) == 0uL
        )
    }

    fun compute_left_endpoint_for_shorter_interval_case(cache: ULong, beta: Int): UInt {
        return ((cache - (cache shr (significand_bits + 2))) shr
            (cache_bits - significand_bits - 1 - beta)).toUInt()
    }

    fun compute_right_endpoint_for_shorter_interval_case(cache: ULong, beta: Int): UInt {
        return ((cache + (cache shr (significand_bits + 1))) shr
            (cache_bits - significand_bits - 1 - beta)).toUInt()
    }

    fun compute_round_up_for_shorter_interval_case(cache: ULong, beta: Int): UInt {
        return ((cache shr (cache_bits - significand_bits - 2 - beta)).toUInt() + 1u) shr 1
    }

//    template <int N, n_max: UInt>
    fun divide_by_pow10(n: UInt, N: UInt, n_max: UInt): UInt {

        // Specialize for 32-bit division by 10.
        // Without the bound on n_max (which compilers these days never leverage), the
        // minimum needed amount of shift is larger than 32. Hence, this may generate better
        // code for 32-bit or smaller architectures. Even for 64-bit architectures, it seems
        // compilers tend to generate mov + mul instead of a single imul for an unknown
        // reason if we just write n / 10.
        if (N == 1u && n_max <= 1073741828u) {
            return ((n.toULong() * 429496730uL) shr 32).toUInt()
        }
        // Specialize for 32-bit division by 100.
        // It seems compilers tend to generate mov + mul instead of a single imul for an
        // unknown reason if we just write n / 100.
        else if (N == 2u) {
            return ((n.toULong() * 1374389535uL) shr 37).toUInt()
        }
        else {
            return n / computePower10( N)
        }
    }

    val cache = arrayOf(
        0x81CEB32C4B43FCF5uL, 0xA2425FF75E14FC32uL,
        0xCAD2F7F5359A3B3FuL, 0xFD87B5F28300CA0EuL,
        0x9E74D1B791E07E49uL, 0xC612062576589DDBuL,
        0xF79687AED3EEC552uL, 0x9ABE14CD44753B53uL,
        0xC16D9A0095928A28uL, 0xF1C90080BAF72CB2uL,
        0x971DA05074DA7BEFuL, 0xBCE5086492111AEBuL,
        0xEC1E4A7DB69561A6uL, 0x9392EE8E921D5D08uL,
        0xB877AA3236A4B44AuL, 0xE69594BEC44DE15CuL,
        0x901D7CF73AB0ACDAuL, 0xB424DC35095CD810uL,
        0xE12E13424BB40E14uL, 0x8CBCCC096F5088CCuL,
        0xAFEBFF0BCB24AAFFuL, 0xDBE6FECEBDEDD5BFuL,
        0x89705F4136B4A598uL, 0xABCC77118461CEFDuL,
        0xD6BF94D5E57A42BDuL, 0x8637BD05AF6C69B6uL,
        0xA7C5AC471B478424uL, 0xD1B71758E219652CuL,
        0x83126E978D4FDF3CuL, 0xA3D70A3D70A3D70BuL,
        0xCCCCCCCCCCCCCCCDuL, 0x8000000000000000uL,
        0xA000000000000000uL, 0xC800000000000000uL,
        0xFA00000000000000uL, 0x9C40000000000000uL,
        0xC350000000000000uL, 0xF424000000000000uL,
        0x9896800000000000uL, 0xBEBC200000000000uL,
        0xEE6B280000000000uL, 0x9502F90000000000uL,
        0xBA43B74000000000uL, 0xE8D4A51000000000uL,
        0x9184E72A00000000uL, 0xB5E620F480000000uL,
        0xE35FA931A0000000uL, 0x8E1BC9BF04000000uL,
        0xB1A2BC2EC5000000uL, 0xDE0B6B3A76400000uL,
        0x8AC7230489E80000uL, 0xAD78EBC5AC620000uL,
        0xD8D726B7177A8000uL, 0x878678326EAC9000uL,
        0xA968163F0A57B400uL, 0xD3C21BCECCEDA100uL,
        0x84595161401484A0uL, 0xA56FA5B99019A5C8uL,
        0xCECB8F27F4200F3AuL, 0x813F3978F8940985uL,
        0xA18F07D736B90BE6uL, 0xC9F2C9CD04674EDFuL,
        0xFC6F7C4045812297uL, 0x9DC5ADA82B70B59EuL,
        0xC5371912364CE306uL, 0xF684DF56C3E01BC7uL,
        0x9A130B963A6C115DuL, 0xC097CE7BC90715B4uL,
        0xF0BDC21ABB48DB21uL, 0x96769950B50D88F5uL,
        0xBC143FA4E250EB32uL, 0xEB194F8E1AE525FEuL,
        0x92EFD1B8D0CF37BFuL, 0xB7ABC627050305AEuL,
        0xE596B7B0C643C71AuL, 0x8F7E32CE7BEA5C70uL,
        0xB35DBF821AE4F38CuL, 0xE0352F62A19E306FuL,
    )

}

// XXX remove these
private fun int(v: ULong): Int = v.toInt()
private fun uint64_t(v: ULong): ULong = v
private fun uint64_t(v: UInt): ULong = v.toULong()
private fun uint32_t(v: Int): UInt = v.toUInt()
private fun uint32_t(v: UInt): UInt = v
private fun size_t(v: Boolean): ULong = if(v) 1uL else 0uL


//template <>
private object float_format_double: FloatFormat<Double> {
    typealias carrier_uint = ULong
    override val total_bits = 64
    override val significand_bits = 52
    override val exponent_bits = 11
    override val min_exponent = -1022
    override val max_exponent = 1023
    override val exponent_bias = -1023
    override val decimal_significand_digits = 17
    override val decimal_exponent_digits = 3
    override val cache_bits = 128
    override val min_k = -292
    override val max_k = 326
    override val max_output_string_length = 1 + float_format_float.decimal_significand_digits + 1 + 1 + 1 + float_format_float.decimal_exponent_digits

    fun remove_trailing_zeros(significand: MutableULong, exponent: MutableInt) {
        // See https://github.com/jk-jeon/rtz_benchmark.
        // The idea of branchless search below is by reddit users r/pigeon768 and
        // r/TheoreticalDumbass.

        var r = uint64_t(significand * 28999941890838049uL).rotr(8)
        var b = r < 184467440738uL
        var s = size_t(b)
        if(b) significand.value = r

        r = uint64_t(significand * 182622766329724561uL).rotr(4)
        b = r < 1844674407370956uL
        s = s.shl(1) + if(b) 1uL else 0uL
        if(b) significand.value = r

        r = uint64_t(significand * 10330176681277348905uL).rotr(2)
        b = r < 184467440737095517uL
        s = s.shl(1) + if(b) 1uL else 0uL
        if(b) significand.value = r

        r = uint64_t(significand * 14757395258967641293uL).rotr(1)
        b = r < 1844674407370955162uL
        s = s.shl(1) + if(b) 1uL else 0uL
        if(b) significand.value = r

        exponent += int(s)
    }

    fun compute_mul(u: ULong, cache: UInt128): compute_mul_resultULong {
        val r = umul192_upper128(u, cache)
        return compute_mul_resultULong(r.high, r.low == 0uL)
    }

    fun compute_delta(cache: UInt128, beta: Int): ULong {
        return cache.high shr (total_bits - 1 - beta)
    }

    fun compute_mul_parity(two_f: ULong, cache: UInt128, beta: Int): compute_mul_parity_result {
        assert(beta >= 1)
        assert(beta < 64)
        val r = umul192_lower128(two_f, cache)
        return compute_mul_parity_result(
            parity = ((r.high shr (64 - beta)) and 1uL) != 0uL,
            is_integer = ((r.high shl beta) or (r.low shr (64 - beta))) == 0uL
        )
    }

    fun compute_left_endpoint_for_shorter_interval_case(cache: UInt128, beta: Int): ULong {
        return (cache.high - (cache.high shr (significand_bits + 2))) shr
        (total_bits - significand_bits - 1 - beta)
    }

    fun compute_right_endpoint_for_shorter_interval_case(cache: UInt128, beta: Int): ULong {
        return (cache.high + (cache.high shr (significand_bits + 1))) shr
        (total_bits - significand_bits - 1 - beta)
    }

    fun compute_round_up_for_shorter_interval_case(cache: UInt128, beta: Int): ULong {
        return ((cache.high shr (total_bits - significand_bits - 2 - beta)) + 1uL).shr(1)
    }

//    template <int N, n_max: ULong>
    fun divide_by_pow10(n: ULong, N: UInt, n_max: ULong): ULong {
//        static_assert(N >= 0, "")

        // Specialize for 64-bit division by 10.
        // Without the bound on n_max (which compilers these days never leverage), the
        // minimum needed amount of shift is larger than 64.
        if (N == 1u && n_max <= 4611686018427387908uL) {
            return umul128_upper64(n, 1844674407370955162uL)
        }
        // Specialize for 64-bit division by 1000.
        // Without the bound on n_max (which compilers these days never leverage), the
        // smallest magic number for this computation does not fit into 64-bits.
        else if (N == 3u && n_max <= 15534100272597517998uL) {
            return umul128_upper64(n, 4722366482869645214uL) shr 8
        }
        else {
            return n / computePower10Long(N)
        }
    }

    val cache = arrayOf(
        UInt128(0xFF77B1FCBEBCDC4FuL, 0x25E8E89C13BB0F7BuL),
        UInt128(0x9FAACF3DF73609B1uL, 0x77B191618C54E9ADuL),
        UInt128(0xC795830D75038C1DuL, 0xD59DF5B9EF6A2418uL),
        UInt128(0xF97AE3D0D2446F25uL, 0x4B0573286B44AD1EuL),
        UInt128(0x9BECCE62836AC577uL, 0x4EE367F9430AEC33uL),
        UInt128(0xC2E801FB244576D5uL, 0x229C41F793CDA740uL),
        UInt128(0xF3A20279ED56D48AuL, 0x6B43527578C11110uL),
        UInt128(0x9845418C345644D6uL, 0x830A13896B78AAAAuL),
        UInt128(0xBE5691EF416BD60CuL, 0x23CC986BC656D554uL),
        UInt128(0xEDEC366B11C6CB8FuL, 0x2CBFBE86B7EC8AA9uL),
        UInt128(0x94B3A202EB1C3F39uL, 0x7BF7D71432F3D6AAuL),
        UInt128(0xB9E08A83A5E34F07uL, 0xDAF5CCD93FB0CC54uL),
        UInt128(0xE858AD248F5C22C9uL, 0xD1B3400F8F9CFF69uL),
        UInt128(0x91376C36D99995BEuL, 0x23100809B9C21FA2uL),
        UInt128(0xB58547448FFFFB2DuL, 0xABD40A0C2832A78BuL),
        UInt128(0xE2E69915B3FFF9F9uL, 0x16C90C8F323F516DuL),
        UInt128(0x8DD01FAD907FFC3BuL, 0xAE3DA7D97F6792E4uL),
        UInt128(0xB1442798F49FFB4AuL, 0x99CD11CFDF41779DuL),
        UInt128(0xDD95317F31C7FA1DuL, 0x40405643D711D584uL),
        UInt128(0x8A7D3EEF7F1CFC52uL, 0x482835EA666B2573uL),
        UInt128(0xAD1C8EAB5EE43B66uL, 0xDA3243650005EED0uL),
        UInt128(0xD863B256369D4A40uL, 0x90BED43E40076A83uL),
        UInt128(0x873E4F75E2224E68uL, 0x5A7744A6E804A292uL),
        UInt128(0xA90DE3535AAAE202uL, 0x711515D0A205CB37uL),
        UInt128(0xD3515C2831559A83uL, 0x0D5A5B44CA873E04uL),
        UInt128(0x8412D9991ED58091uL, 0xE858790AFE9486C3uL),
        UInt128(0xA5178FFF668AE0B6uL, 0x626E974DBE39A873uL),
        UInt128(0xCE5D73FF402D98E3uL, 0xFB0A3D212DC81290uL),
        UInt128(0x80FA687F881C7F8EuL, 0x7CE66634BC9D0B9AuL),
        UInt128(0xA139029F6A239F72uL, 0x1C1FFFC1EBC44E81uL),
        UInt128(0xC987434744AC874EuL, 0xA327FFB266B56221uL),
        UInt128(0xFBE9141915D7A922uL, 0x4BF1FF9F0062BAA9uL),
        UInt128(0x9D71AC8FADA6C9B5uL, 0x6F773FC3603DB4AAuL),
        UInt128(0xC4CE17B399107C22uL, 0xCB550FB4384D21D4uL),
        UInt128(0xF6019DA07F549B2BuL, 0x7E2A53A146606A49uL),
        UInt128(0x99C102844F94E0FBuL, 0x2EDA7444CBFC426EuL),
        UInt128(0xC0314325637A1939uL, 0xFA911155FEFB5309uL),
        UInt128(0xF03D93EEBC589F88uL, 0x793555AB7EBA27CBuL),
        UInt128(0x96267C7535B763B5uL, 0x4BC1558B2F3458DFuL),
        UInt128(0xBBB01B9283253CA2uL, 0x9EB1AAEDFB016F17uL),
        UInt128(0xEA9C227723EE8BCBuL, 0x465E15A979C1CADDuL),
        UInt128(0x92A1958A7675175FuL, 0x0BFACD89EC191ECAuL),
        UInt128(0xB749FAED14125D36uL, 0xCEF980EC671F667CuL),
        UInt128(0xE51C79A85916F484uL, 0x82B7E12780E7401BuL),
        UInt128(0x8F31CC0937AE58D2uL, 0xD1B2ECB8B0908811uL),
        UInt128(0xB2FE3F0B8599EF07uL, 0x861FA7E6DCB4AA16uL),
        UInt128(0xDFBDCECE67006AC9uL, 0x67A791E093E1D49BuL),
        UInt128(0x8BD6A141006042BDuL, 0xE0C8BB2C5C6D24E1uL),
        UInt128(0xAECC49914078536DuL, 0x58FAE9F773886E19uL),
        UInt128(0xDA7F5BF590966848uL, 0xAF39A475506A899FuL),
        UInt128(0x888F99797A5E012DuL, 0x6D8406C952429604uL),
        UInt128(0xAAB37FD7D8F58178uL, 0xC8E5087BA6D33B84uL),
        UInt128(0xD5605FCDCF32E1D6uL, 0xFB1E4A9A90880A65uL),
        UInt128(0x855C3BE0A17FCD26uL, 0x5CF2EEA09A550680uL),
        UInt128(0xA6B34AD8C9DFC06FuL, 0xF42FAA48C0EA481FuL),
        UInt128(0xD0601D8EFC57B08BuL, 0xF13B94DAF124DA27uL),
        UInt128(0x823C12795DB6CE57uL, 0x76C53D08D6B70859uL),
        UInt128(0xA2CB1717B52481EDuL, 0x54768C4B0C64CA6FuL),
        UInt128(0xCB7DDCDDA26DA268uL, 0xA9942F5DCF7DFD0AuL),
        UInt128(0xFE5D54150B090B02uL, 0xD3F93B35435D7C4DuL),
        UInt128(0x9EFA548D26E5A6E1uL, 0xC47BC5014A1A6DB0uL),
        UInt128(0xC6B8E9B0709F109AuL, 0x359AB6419CA1091CuL),
        UInt128(0xF867241C8CC6D4C0uL, 0xC30163D203C94B63uL),
        UInt128(0x9B407691D7FC44F8uL, 0x79E0DE63425DCF1EuL),
        UInt128(0xC21094364DFB5636uL, 0x985915FC12F542E5uL),
        UInt128(0xF294B943E17A2BC4uL, 0x3E6F5B7B17B2939EuL),
        UInt128(0x979CF3CA6CEC5B5AuL, 0xA705992CEECF9C43uL),
        UInt128(0xBD8430BD08277231uL, 0x50C6FF782A838354uL),
        UInt128(0xECE53CEC4A314EBDuL, 0xA4F8BF5635246429uL),
        UInt128(0x940F4613AE5ED136uL, 0x871B7795E136BE9AuL),
        UInt128(0xB913179899F68584uL, 0x28E2557B59846E40uL),
        UInt128(0xE757DD7EC07426E5uL, 0x331AEADA2FE589D0uL),
        UInt128(0x9096EA6F3848984FuL, 0x3FF0D2C85DEF7622uL),
        UInt128(0xB4BCA50B065ABE63uL, 0x0FED077A756B53AAuL),
        UInt128(0xE1EBCE4DC7F16DFBuL, 0xD3E8495912C62895uL),
        UInt128(0x8D3360F09CF6E4BDuL, 0x64712DD7ABBBD95DuL),
        UInt128(0xB080392CC4349DECuL, 0xBD8D794D96AACFB4uL),
        UInt128(0xDCA04777F541C567uL, 0xECF0D7A0FC5583A1uL),
        UInt128(0x89E42CAAF9491B60uL, 0xF41686C49DB57245uL),
        UInt128(0xAC5D37D5B79B6239uL, 0x311C2875C522CED6uL),
        UInt128(0xD77485CB25823AC7uL, 0x7D633293366B828CuL),
        UInt128(0x86A8D39EF77164BCuL, 0xAE5DFF9C02033198uL),
        UInt128(0xA8530886B54DBDEBuL, 0xD9F57F830283FDFDuL),
        UInt128(0xD267CAA862A12D66uL, 0xD072DF63C324FD7CuL),
        UInt128(0x8380DEA93DA4BC60uL, 0x4247CB9E59F71E6EuL),
        UInt128(0xA46116538D0DEB78uL, 0x52D9BE85F074E609uL),
        UInt128(0xCD795BE870516656uL, 0x67902E276C921F8CuL),
        UInt128(0x806BD9714632DFF6uL, 0x00BA1CD8A3DB53B7uL),
        UInt128(0xA086CFCD97BF97F3uL, 0x80E8A40ECCD228A5uL),
        UInt128(0xC8A883C0FDAF7DF0uL, 0x6122CD128006B2CEuL),
        UInt128(0xFAD2A4B13D1B5D6CuL, 0x796B805720085F82uL),
        UInt128(0x9CC3A6EEC6311A63uL, 0xCBE3303674053BB1uL),
        UInt128(0xC3F490AA77BD60FCuL, 0xBEDBFC4411068A9DuL),
        UInt128(0xF4F1B4D515ACB93BuL, 0xEE92FB5515482D45uL),
        UInt128(0x991711052D8BF3C5uL, 0x751BDD152D4D1C4BuL),
        UInt128(0xBF5CD54678EEF0B6uL, 0xD262D45A78A0635EuL),
        UInt128(0xEF340A98172AACE4uL, 0x86FB897116C87C35uL),
        UInt128(0x9580869F0E7AAC0EuL, 0xD45D35E6AE3D4DA1uL),
        UInt128(0xBAE0A846D2195712uL, 0x8974836059CCA10AuL),
        UInt128(0xE998D258869FACD7uL, 0x2BD1A438703FC94CuL),
        UInt128(0x91FF83775423CC06uL, 0x7B6306A34627DDD0uL),
        UInt128(0xB67F6455292CBF08uL, 0x1A3BC84C17B1D543uL),
        UInt128(0xE41F3D6A7377EECAuL, 0x20CABA5F1D9E4A94uL),
        UInt128(0x8E938662882AF53EuL, 0x547EB47B7282EE9DuL),
        UInt128(0xB23867FB2A35B28DuL, 0xE99E619A4F23AA44uL),
        UInt128(0xDEC681F9F4C31F31uL, 0x6405FA00E2EC94D5uL),
        UInt128(0x8B3C113C38F9F37EuL, 0xDE83BC408DD3DD05uL),
        UInt128(0xAE0B158B4738705EuL, 0x9624AB50B148D446uL),
        UInt128(0xD98DDAEE19068C76uL, 0x3BADD624DD9B0958uL),
        UInt128(0x87F8A8D4CFA417C9uL, 0xE54CA5D70A80E5D7uL),
        UInt128(0xA9F6D30A038D1DBCuL, 0x5E9FCF4CCD211F4DuL),
        UInt128(0xD47487CC8470652BuL, 0x7647C32000696720uL),
        UInt128(0x84C8D4DFD2C63F3BuL, 0x29ECD9F40041E074uL),
        UInt128(0xA5FB0A17C777CF09uL, 0xF468107100525891uL),
        UInt128(0xCF79CC9DB955C2CCuL, 0x7182148D4066EEB5uL),
        UInt128(0x81AC1FE293D599BFuL, 0xC6F14CD848405531uL),
        UInt128(0xA21727DB38CB002FuL, 0xB8ADA00E5A506A7DuL),
        UInt128(0xCA9CF1D206FDC03BuL, 0xA6D90811F0E4851DuL),
        UInt128(0xFD442E4688BD304AuL, 0x908F4A166D1DA664uL),
        UInt128(0x9E4A9CEC15763E2EuL, 0x9A598E4E043287FFuL),
        UInt128(0xC5DD44271AD3CDBAuL, 0x40EFF1E1853F29FEuL),
        UInt128(0xF7549530E188C128uL, 0xD12BEE59E68EF47DuL),
        UInt128(0x9A94DD3E8CF578B9uL, 0x82BB74F8301958CFuL),
        UInt128(0xC13A148E3032D6E7uL, 0xE36A52363C1FAF02uL),
        UInt128(0xF18899B1BC3F8CA1uL, 0xDC44E6C3CB279AC2uL),
        UInt128(0x96F5600F15A7B7E5uL, 0x29AB103A5EF8C0BAuL),
        UInt128(0xBCB2B812DB11A5DEuL, 0x7415D448F6B6F0E8uL),
        UInt128(0xEBDF661791D60F56uL, 0x111B495B3464AD22uL),
        UInt128(0x936B9FCEBB25C995uL, 0xCAB10DD900BEEC35uL),
        UInt128(0xB84687C269EF3BFBuL, 0x3D5D514F40EEA743uL),
        UInt128(0xE65829B3046B0AFAuL, 0x0CB4A5A3112A5113uL),
        UInt128(0x8FF71A0FE2C2E6DCuL, 0x47F0E785EABA72ACuL),
        UInt128(0xB3F4E093DB73A093uL, 0x59ED216765690F57uL),
        UInt128(0xE0F218B8D25088B8uL, 0x306869C13EC3532DuL),
        UInt128(0x8C974F7383725573uL, 0x1E414218C73A13FCuL),
        UInt128(0xAFBD2350644EEACFuL, 0xE5D1929EF90898FBuL),
        UInt128(0xDBAC6C247D62A583uL, 0xDF45F746B74ABF3AuL),
        UInt128(0x894BC396CE5DA772uL, 0x6B8BBA8C328EB784uL),
        UInt128(0xAB9EB47C81F5114FuL, 0x066EA92F3F326565uL),
        UInt128(0xD686619BA27255A2uL, 0xC80A537B0EFEFEBEuL),
        UInt128(0x8613FD0145877585uL, 0xBD06742CE95F5F37uL),
        UInt128(0xA798FC4196E952E7uL, 0x2C48113823B73705uL),
        UInt128(0xD17F3B51FCA3A7A0uL, 0xF75A15862CA504C6uL),
        UInt128(0x82EF85133DE648C4uL, 0x9A984D73DBE722FCuL),
        UInt128(0xA3AB66580D5FDAF5uL, 0xC13E60D0D2E0EBBBuL),
        UInt128(0xCC963FEE10B7D1B3uL, 0x318DF905079926A9uL),
        UInt128(0xFFBBCFE994E5C61FuL, 0xFDF17746497F7053uL),
        UInt128(0x9FD561F1FD0F9BD3uL, 0xFEB6EA8BEDEFA634uL),
        UInt128(0xC7CABA6E7C5382C8uL, 0xFE64A52EE96B8FC1uL),
        UInt128(0xF9BD690A1B68637BuL, 0x3DFDCE7AA3C673B1uL),
        UInt128(0x9C1661A651213E2DuL, 0x06BEA10CA65C084FuL),
        UInt128(0xC31BFA0FE5698DB8uL, 0x486E494FCFF30A63uL),
        UInt128(0xF3E2F893DEC3F126uL, 0x5A89DBA3C3EFCCFBuL),
        UInt128(0x986DDB5C6B3A76B7uL, 0xF89629465A75E01DuL),
        UInt128(0xBE89523386091465uL, 0xF6BBB397F1135824uL),
        UInt128(0xEE2BA6C0678B597FuL, 0x746AA07DED582E2DuL),
        UInt128(0x94DB483840B717EFuL, 0xA8C2A44EB4571CDDuL),
        UInt128(0xBA121A4650E4DDEBuL, 0x92F34D62616CE414uL),
        UInt128(0xE896A0D7E51E1566uL, 0x77B020BAF9C81D18uL),
        UInt128(0x915E2486EF32CD60uL, 0x0ACE1474DC1D122FuL),
        UInt128(0xB5B5ADA8AAFF80B8uL, 0x0D819992132456BBuL),
        UInt128(0xE3231912D5BF60E6uL, 0x10E1FFF697ED6C6AuL),
        UInt128(0x8DF5EFABC5979C8FuL, 0xCA8D3FFA1EF463C2uL),
        UInt128(0xB1736B96B6FD83B3uL, 0xBD308FF8A6B17CB3uL),
        UInt128(0xDDD0467C64BCE4A0uL, 0xAC7CB3F6D05DDBDFuL),
        UInt128(0x8AA22C0DBEF60EE4uL, 0x6BCDF07A423AA96CuL),
        UInt128(0xAD4AB7112EB3929DuL, 0x86C16C98D2C953C7uL),
        UInt128(0xD89D64D57A607744uL, 0xE871C7BF077BA8B8uL),
        UInt128(0x87625F056C7C4A8BuL, 0x11471CD764AD4973uL),
        UInt128(0xA93AF6C6C79B5D2DuL, 0xD598E40D3DD89BD0uL),
        UInt128(0xD389B47879823479uL, 0x4AFF1D108D4EC2C4uL),
        UInt128(0x843610CB4BF160CBuL, 0xCEDF722A585139BBuL),
        UInt128(0xA54394FE1EEDB8FEuL, 0xC2974EB4EE658829uL),
        UInt128(0xCE947A3DA6A9273EuL, 0x733D226229FEEA33uL),
        UInt128(0x811CCC668829B887uL, 0x0806357D5A3F5260uL),
        UInt128(0xA163FF802A3426A8uL, 0xCA07C2DCB0CF26F8uL),
        UInt128(0xC9BCFF6034C13052uL, 0xFC89B393DD02F0B6uL),
        UInt128(0xFC2C3F3841F17C67uL, 0xBBAC2078D443ACE3uL),
        UInt128(0x9D9BA7832936EDC0uL, 0xD54B944B84AA4C0EuL),
        UInt128(0xC5029163F384A931uL, 0x0A9E795E65D4DF12uL),
        UInt128(0xF64335BCF065D37DuL, 0x4D4617B5FF4A16D6uL),
        UInt128(0x99EA0196163FA42EuL, 0x504BCED1BF8E4E46uL),
        UInt128(0xC06481FB9BCF8D39uL, 0xE45EC2862F71E1D7uL),
        UInt128(0xF07DA27A82C37088uL, 0x5D767327BB4E5A4DuL),
        UInt128(0x964E858C91BA2655uL, 0x3A6A07F8D510F870uL),
        UInt128(0xBBE226EFB628AFEAuL, 0x890489F70A55368CuL),
        UInt128(0xEADAB0ABA3B2DBE5uL, 0x2B45AC74CCEA842FuL),
        UInt128(0x92C8AE6B464FC96FuL, 0x3B0B8BC90012929EuL),
        UInt128(0xB77ADA0617E3BBCBuL, 0x09CE6EBB40173745uL),
        UInt128(0xE55990879DDCAABDuL, 0xCC420A6A101D0516uL),
        UInt128(0x8F57FA54C2A9EAB6uL, 0x9FA946824A12232EuL),
        UInt128(0xB32DF8E9F3546564uL, 0x47939822DC96ABFAuL),
        UInt128(0xDFF9772470297EBDuL, 0x59787E2B93BC56F8uL),
        UInt128(0x8BFBEA76C619EF36uL, 0x57EB4EDB3C55B65BuL),
        UInt128(0xAEFAE51477A06B03uL, 0xEDE622920B6B23F2uL),
        UInt128(0xDAB99E59958885C4uL, 0xE95FAB368E45ECEEuL),
        UInt128(0x88B402F7FD75539BuL, 0x11DBCB0218EBB415uL),
        UInt128(0xAAE103B5FCD2A881uL, 0xD652BDC29F26A11AuL),
        UInt128(0xD59944A37C0752A2uL, 0x4BE76D3346F04960uL),
        UInt128(0x857FCAE62D8493A5uL, 0x6F70A4400C562DDCuL),
        UInt128(0xA6DFBD9FB8E5B88EuL, 0xCB4CCD500F6BB953uL),
        UInt128(0xD097AD07A71F26B2uL, 0x7E2000A41346A7A8uL),
        UInt128(0x825ECC24C873782FuL, 0x8ED400668C0C28C9uL),
        UInt128(0xA2F67F2DFA90563BuL, 0x728900802F0F32FBuL),
        UInt128(0xCBB41EF979346BCAuL, 0x4F2B40A03AD2FFBAuL),
        UInt128(0xFEA126B7D78186BCuL, 0xE2F610C84987BFA9uL),
        UInt128(0x9F24B832E6B0F436uL, 0x0DD9CA7D2DF4D7CAuL),
        UInt128(0xC6EDE63FA05D3143uL, 0x91503D1C79720DBCuL),
        UInt128(0xF8A95FCF88747D94uL, 0x75A44C6397CE912BuL),
        UInt128(0x9B69DBE1B548CE7CuL, 0xC986AFBE3EE11ABBuL),
        UInt128(0xC24452DA229B021BuL, 0xFBE85BADCE996169uL),
        UInt128(0xF2D56790AB41C2A2uL, 0xFAE27299423FB9C4uL),
        UInt128(0x97C560BA6B0919A5uL, 0xDCCD879FC967D41BuL),
        UInt128(0xBDB6B8E905CB600FuL, 0x5400E987BBC1C921uL),
        UInt128(0xED246723473E3813uL, 0x290123E9AAB23B69uL),
        UInt128(0x9436C0760C86E30BuL, 0xF9A0B6720AAF6522uL),
        UInt128(0xB94470938FA89BCEuL, 0xF808E40E8D5B3E6AuL),
        UInt128(0xE7958CB87392C2C2uL, 0xB60B1D1230B20E05uL),
        UInt128(0x90BD77F3483BB9B9uL, 0xB1C6F22B5E6F48C3uL),
        UInt128(0xB4ECD5F01A4AA828uL, 0x1E38AEB6360B1AF4uL),
        UInt128(0xE2280B6C20DD5232uL, 0x25C6DA63C38DE1B1uL),
        UInt128(0x8D590723948A535FuL, 0x579C487E5A38AD0FuL),
        UInt128(0xB0AF48EC79ACE837uL, 0x2D835A9DF0C6D852uL),
        UInt128(0xDCDB1B2798182244uL, 0xF8E431456CF88E66uL),
        UInt128(0x8A08F0F8BF0F156BuL, 0x1B8E9ECB641B5900uL),
        UInt128(0xAC8B2D36EED2DAC5uL, 0xE272467E3D222F40uL),
        UInt128(0xD7ADF884AA879177uL, 0x5B0ED81DCC6ABB10uL),
        UInt128(0x86CCBB52EA94BAEAuL, 0x98E947129FC2B4EAuL),
        UInt128(0xA87FEA27A539E9A5uL, 0x3F2398D747B36225uL),
        UInt128(0xD29FE4B18E88640EuL, 0x8EEC7F0D19A03AAEuL),
        UInt128(0x83A3EEEEF9153E89uL, 0x1953CF68300424ADuL),
        UInt128(0xA48CEAAAB75A8E2BuL, 0x5FA8C3423C052DD8uL),
        UInt128(0xCDB02555653131B6uL, 0x3792F412CB06794EuL),
        UInt128(0x808E17555F3EBF11uL, 0xE2BBD88BBEE40BD1uL),
        UInt128(0xA0B19D2AB70E6ED6uL, 0x5B6ACEAEAE9D0EC5uL),
        UInt128(0xC8DE047564D20A8BuL, 0xF245825A5A445276uL),
        UInt128(0xFB158592BE068D2EuL, 0xEED6E2F0F0D56713uL),
        UInt128(0x9CED737BB6C4183DuL, 0x55464DD69685606CuL),
        UInt128(0xC428D05AA4751E4CuL, 0xAA97E14C3C26B887uL),
        UInt128(0xF53304714D9265DFuL, 0xD53DD99F4B3066A9uL),
        UInt128(0x993FE2C6D07B7FABuL, 0xE546A8038EFE402AuL),
        UInt128(0xBF8FDB78849A5F96uL, 0xDE98520472BDD034uL),
        UInt128(0xEF73D256A5C0F77CuL, 0x963E66858F6D4441uL),
        UInt128(0x95A8637627989AADuL, 0xDDE7001379A44AA9uL),
        UInt128(0xBB127C53B17EC159uL, 0x5560C018580D5D53uL),
        UInt128(0xE9D71B689DDE71AFuL, 0xAAB8F01E6E10B4A7uL),
        UInt128(0x9226712162AB070DuL, 0xCAB3961304CA70E9uL),
        UInt128(0xB6B00D69BB55C8D1uL, 0x3D607B97C5FD0D23uL),
        UInt128(0xE45C10C42A2B3B05uL, 0x8CB89A7DB77C506BuL),
        UInt128(0x8EB98A7A9A5B04E3uL, 0x77F3608E92ADB243uL),
        UInt128(0xB267ED1940F1C61CuL, 0x55F038B237591ED4uL),
        UInt128(0xDF01E85F912E37A3uL, 0x6B6C46DEC52F6689uL),
        UInt128(0x8B61313BBABCE2C6uL, 0x2323AC4B3B3DA016uL),
        UInt128(0xAE397D8AA96C1B77uL, 0xABEC975E0A0D081BuL),
        UInt128(0xD9C7DCED53C72255uL, 0x96E7BD358C904A22uL),
        UInt128(0x881CEA14545C7575uL, 0x7E50D64177DA2E55uL),
        UInt128(0xAA242499697392D2uL, 0xDDE50BD1D5D0B9EAuL),
        UInt128(0xD4AD2DBFC3D07787uL, 0x955E4EC64B44E865uL),
        UInt128(0x84EC3C97DA624AB4uL, 0xBD5AF13BEF0B113FuL),
        UInt128(0xA6274BBDD0FADD61uL, 0xECB1AD8AEACDD58FuL),
        UInt128(0xCFB11EAD453994BAuL, 0x67DE18EDA5814AF3uL),
        UInt128(0x81CEB32C4B43FCF4uL, 0x80EACF948770CED8uL),
        UInt128(0xA2425FF75E14FC31uL, 0xA1258379A94D028EuL),
        UInt128(0xCAD2F7F5359A3B3EuL, 0x096EE45813A04331uL),
        UInt128(0xFD87B5F28300CA0DuL, 0x8BCA9D6E188853FDuL),
        UInt128(0x9E74D1B791E07E48uL, 0x775EA264CF55347EuL),
        UInt128(0xC612062576589DDAuL, 0x95364AFE032A819EuL),
        UInt128(0xF79687AED3EEC551uL, 0x3A83DDBD83F52205uL),
        UInt128(0x9ABE14CD44753B52uL, 0xC4926A9672793543uL),
        UInt128(0xC16D9A0095928A27uL, 0x75B7053C0F178294uL),
        UInt128(0xF1C90080BAF72CB1uL, 0x5324C68B12DD6339uL),
        UInt128(0x971DA05074DA7BEEuL, 0xD3F6FC16EBCA5E04uL),
        UInt128(0xBCE5086492111AEAuL, 0x88F4BB1CA6BCF585uL),
        UInt128(0xEC1E4A7DB69561A5uL, 0x2B31E9E3D06C32E6uL),
        UInt128(0x9392EE8E921D5D07uL, 0x3AFF322E62439FD0uL),
        UInt128(0xB877AA3236A4B449uL, 0x09BEFEB9FAD487C3uL),
        UInt128(0xE69594BEC44DE15BuL, 0x4C2EBE687989A9B4uL),
        UInt128(0x901D7CF73AB0ACD9uL, 0x0F9D37014BF60A11uL),
        UInt128(0xB424DC35095CD80FuL, 0x538484C19EF38C95uL),
        UInt128(0xE12E13424BB40E13uL, 0x2865A5F206B06FBAuL),
        UInt128(0x8CBCCC096F5088CBuL, 0xF93F87B7442E45D4uL),
        UInt128(0xAFEBFF0BCB24AAFEuL, 0xF78F69A51539D749uL),
        UInt128(0xDBE6FECEBDEDD5BEuL, 0xB573440E5A884D1CuL),
        UInt128(0x89705F4136B4A597uL, 0x31680A88F8953031uL),
        UInt128(0xABCC77118461CEFCuL, 0xFDC20D2B36BA7C3EuL),
        UInt128(0xD6BF94D5E57A42BCuL, 0x3D32907604691B4DuL),
        UInt128(0x8637BD05AF6C69B5uL, 0xA63F9A49C2C1B110uL),
        UInt128(0xA7C5AC471B478423uL, 0x0FCF80DC33721D54uL),
        UInt128(0xD1B71758E219652BuL, 0xD3C36113404EA4A9uL),
        UInt128(0x83126E978D4FDF3BuL, 0x645A1CAC083126EAuL),
        UInt128(0xA3D70A3D70A3D70AuL, 0x3D70A3D70A3D70A4uL),
        UInt128(0xCCCCCCCCCCCCCCCCuL, 0xCCCCCCCCCCCCCCCDuL),
        UInt128(0x8000000000000000uL, 0x0000000000000000uL),
        UInt128(0xA000000000000000uL, 0x0000000000000000uL),
        UInt128(0xC800000000000000uL, 0x0000000000000000uL),
        UInt128(0xFA00000000000000uL, 0x0000000000000000uL),
        UInt128(0x9C40000000000000uL, 0x0000000000000000uL),
        UInt128(0xC350000000000000uL, 0x0000000000000000uL),
        UInt128(0xF424000000000000uL, 0x0000000000000000uL),
        UInt128(0x9896800000000000uL, 0x0000000000000000uL),
        UInt128(0xBEBC200000000000uL, 0x0000000000000000uL),
        UInt128(0xEE6B280000000000uL, 0x0000000000000000uL),
        UInt128(0x9502F90000000000uL, 0x0000000000000000uL),
        UInt128(0xBA43B74000000000uL, 0x0000000000000000uL),
        UInt128(0xE8D4A51000000000uL, 0x0000000000000000uL),
        UInt128(0x9184E72A00000000uL, 0x0000000000000000uL),
        UInt128(0xB5E620F480000000uL, 0x0000000000000000uL),
        UInt128(0xE35FA931A0000000uL, 0x0000000000000000uL),
        UInt128(0x8E1BC9BF04000000uL, 0x0000000000000000uL),
        UInt128(0xB1A2BC2EC5000000uL, 0x0000000000000000uL),
        UInt128(0xDE0B6B3A76400000uL, 0x0000000000000000uL),
        UInt128(0x8AC7230489E80000uL, 0x0000000000000000uL),
        UInt128(0xAD78EBC5AC620000uL, 0x0000000000000000uL),
        UInt128(0xD8D726B7177A8000uL, 0x0000000000000000uL),
        UInt128(0x878678326EAC9000uL, 0x0000000000000000uL),
        UInt128(0xA968163F0A57B400uL, 0x0000000000000000uL),
        UInt128(0xD3C21BCECCEDA100uL, 0x0000000000000000uL),
        UInt128(0x84595161401484A0uL, 0x0000000000000000uL),
        UInt128(0xA56FA5B99019A5C8uL, 0x0000000000000000uL),
        UInt128(0xCECB8F27F4200F3AuL, 0x0000000000000000uL),
        UInt128(0x813F3978F8940984uL, 0x4000000000000000uL),
        UInt128(0xA18F07D736B90BE5uL, 0x5000000000000000uL),
        UInt128(0xC9F2C9CD04674EDEuL, 0xA400000000000000uL),
        UInt128(0xFC6F7C4045812296uL, 0x4D00000000000000uL),
        UInt128(0x9DC5ADA82B70B59DuL, 0xF020000000000000uL),
        UInt128(0xC5371912364CE305uL, 0x6C28000000000000uL),
        UInt128(0xF684DF56C3E01BC6uL, 0xC732000000000000uL),
        UInt128(0x9A130B963A6C115CuL, 0x3C7F400000000000uL),
        UInt128(0xC097CE7BC90715B3uL, 0x4B9F100000000000uL),
        UInt128(0xF0BDC21ABB48DB20uL, 0x1E86D40000000000uL),
        UInt128(0x96769950B50D88F4uL, 0x1314448000000000uL),
        UInt128(0xBC143FA4E250EB31uL, 0x17D955A000000000uL),
        UInt128(0xEB194F8E1AE525FDuL, 0x5DCFAB0800000000uL),
        UInt128(0x92EFD1B8D0CF37BEuL, 0x5AA1CAE500000000uL),
        UInt128(0xB7ABC627050305ADuL, 0xF14A3D9E40000000uL),
        UInt128(0xE596B7B0C643C719uL, 0x6D9CCD05D0000000uL),
        UInt128(0x8F7E32CE7BEA5C6FuL, 0xE4820023A2000000uL),
        UInt128(0xB35DBF821AE4F38BuL, 0xDDA2802C8A800000uL),
        UInt128(0xE0352F62A19E306EuL, 0xD50B2037AD200000uL),
        UInt128(0x8C213D9DA502DE45uL, 0x4526F422CC340000uL),
        UInt128(0xAF298D050E4395D6uL, 0x9670B12B7F410000uL),
        UInt128(0xDAF3F04651D47B4CuL, 0x3C0CDD765F114000uL),
        UInt128(0x88D8762BF324CD0FuL, 0xA5880A69FB6AC800uL),
        UInt128(0xAB0E93B6EFEE0053uL, 0x8EEA0D047A457A00uL),
        UInt128(0xD5D238A4ABE98068uL, 0x72A4904598D6D880uL),
        UInt128(0x85A36366EB71F041uL, 0x47A6DA2B7F864750uL),
        UInt128(0xA70C3C40A64E6C51uL, 0x999090B65F67D924uL),
        UInt128(0xD0CF4B50CFE20765uL, 0xFFF4B4E3F741CF6DuL),
        UInt128(0x82818F1281ED449FuL, 0xBFF8F10E7A8921A5uL),
        UInt128(0xA321F2D7226895C7uL, 0xAFF72D52192B6A0EuL),
        UInt128(0xCBEA6F8CEB02BB39uL, 0x9BF4F8A69F764491uL),
        UInt128(0xFEE50B7025C36A08uL, 0x02F236D04753D5B5uL),
        UInt128(0x9F4F2726179A2245uL, 0x01D762422C946591uL),
        UInt128(0xC722F0EF9D80AAD6uL, 0x424D3AD2B7B97EF6uL),
        UInt128(0xF8EBAD2B84E0D58BuL, 0xD2E0898765A7DEB3uL),
        UInt128(0x9B934C3B330C8577uL, 0x63CC55F49F88EB30uL),
        UInt128(0xC2781F49FFCFA6D5uL, 0x3CBF6B71C76B25FCuL),
        UInt128(0xF316271C7FC3908AuL, 0x8BEF464E3945EF7BuL),
        UInt128(0x97EDD871CFDA3A56uL, 0x97758BF0E3CBB5ADuL),
        UInt128(0xBDE94E8E43D0C8ECuL, 0x3D52EEED1CBEA318uL),
        UInt128(0xED63A231D4C4FB27uL, 0x4CA7AAA863EE4BDEuL),
        UInt128(0x945E455F24FB1CF8uL, 0x8FE8CAA93E74EF6BuL),
        UInt128(0xB975D6B6EE39E436uL, 0xB3E2FD538E122B45uL),
        UInt128(0xE7D34C64A9C85D44uL, 0x60DBBCA87196B617uL),
        UInt128(0x90E40FBEEA1D3A4AuL, 0xBC8955E946FE31CEuL),
        UInt128(0xB51D13AEA4A488DDuL, 0x6BABAB6398BDBE42uL),
        UInt128(0xE264589A4DCDAB14uL, 0xC696963C7EED2DD2uL),
        UInt128(0x8D7EB76070A08AECuL, 0xFC1E1DE5CF543CA3uL),
        UInt128(0xB0DE65388CC8ADA8uL, 0x3B25A55F43294BCCuL),
        UInt128(0xDD15FE86AFFAD912uL, 0x49EF0EB713F39EBFuL),
        UInt128(0x8A2DBF142DFCC7ABuL, 0x6E3569326C784338uL),
        UInt128(0xACB92ED9397BF996uL, 0x49C2C37F07965405uL),
        UInt128(0xD7E77A8F87DAF7FBuL, 0xDC33745EC97BE907uL),
        UInt128(0x86F0AC99B4E8DAFDuL, 0x69A028BB3DED71A4uL),
        UInt128(0xA8ACD7C0222311BCuL, 0xC40832EA0D68CE0DuL),
        UInt128(0xD2D80DB02AABD62BuL, 0xF50A3FA490C30191uL),
        UInt128(0x83C7088E1AAB65DBuL, 0x792667C6DA79E0FBuL),
        UInt128(0xA4B8CAB1A1563F52uL, 0x577001B891185939uL),
        UInt128(0xCDE6FD5E09ABCF26uL, 0xED4C0226B55E6F87uL),
        UInt128(0x80B05E5AC60B6178uL, 0x544F8158315B05B5uL),
        UInt128(0xA0DC75F1778E39D6uL, 0x696361AE3DB1C722uL),
        UInt128(0xC913936DD571C84CuL, 0x03BC3A19CD1E38EAuL),
        UInt128(0xFB5878494ACE3A5FuL, 0x04AB48A04065C724uL),
        UInt128(0x9D174B2DCEC0E47BuL, 0x62EB0D64283F9C77uL),
        UInt128(0xC45D1DF942711D9AuL, 0x3BA5D0BD324F8395uL),
        UInt128(0xF5746577930D6500uL, 0xCA8F44EC7EE3647AuL),
        UInt128(0x9968BF6ABBE85F20uL, 0x7E998B13CF4E1ECCuL),
        UInt128(0xBFC2EF456AE276E8uL, 0x9E3FEDD8C321A67FuL),
        UInt128(0xEFB3AB16C59B14A2uL, 0xC5CFE94EF3EA101FuL),
        UInt128(0x95D04AEE3B80ECE5uL, 0xBBA1F1D158724A13uL),
        UInt128(0xBB445DA9CA61281FuL, 0x2A8A6E45AE8EDC98uL),
        UInt128(0xEA1575143CF97226uL, 0xF52D09D71A3293BEuL),
        UInt128(0x924D692CA61BE758uL, 0x593C2626705F9C57uL),
        UInt128(0xB6E0C377CFA2E12EuL, 0x6F8B2FB00C77836DuL),
        UInt128(0xE498F455C38B997AuL, 0x0B6DFB9C0F956448uL),
        UInt128(0x8EDF98B59A373FECuL, 0x4724BD4189BD5EADuL),
        UInt128(0xB2977EE300C50FE7uL, 0x58EDEC91EC2CB658uL),
        UInt128(0xDF3D5E9BC0F653E1uL, 0x2F2967B66737E3EEuL),
        UInt128(0x8B865B215899F46CuL, 0xBD79E0D20082EE75uL),
        UInt128(0xAE67F1E9AEC07187uL, 0xECD8590680A3AA12uL),
        UInt128(0xDA01EE641A708DE9uL, 0xE80E6F4820CC9496uL),
        UInt128(0x884134FE908658B2uL, 0x3109058D147FDCDEuL),
        UInt128(0xAA51823E34A7EEDEuL, 0xBD4B46F0599FD416uL),
        UInt128(0xD4E5E2CDC1D1EA96uL, 0x6C9E18AC7007C91BuL),
        UInt128(0x850FADC09923329EuL, 0x03E2CF6BC604DDB1uL),
        UInt128(0xA6539930BF6BFF45uL, 0x84DB8346B786151DuL),
        UInt128(0xCFE87F7CEF46FF16uL, 0xE612641865679A64uL),
        UInt128(0x81F14FAE158C5F6EuL, 0x4FCB7E8F3F60C07FuL),
        UInt128(0xA26DA3999AEF7749uL, 0xE3BE5E330F38F09EuL),
        UInt128(0xCB090C8001AB551CuL, 0x5CADF5BFD3072CC6uL),
        UInt128(0xFDCB4FA002162A63uL, 0x73D9732FC7C8F7F7uL),
        UInt128(0x9E9F11C4014DDA7EuL, 0x2867E7FDDCDD9AFBuL),
        UInt128(0xC646D63501A1511DuL, 0xB281E1FD541501B9uL),
        UInt128(0xF7D88BC24209A565uL, 0x1F225A7CA91A4227uL),
        UInt128(0x9AE757596946075FuL, 0x3375788DE9B06959uL),
        UInt128(0xC1A12D2FC3978937uL, 0x0052D6B1641C83AFuL),
        UInt128(0xF209787BB47D6B84uL, 0xC0678C5DBD23A49BuL),
        UInt128(0x9745EB4D50CE6332uL, 0xF840B7BA963646E1uL),
        UInt128(0xBD176620A501FBFFuL, 0xB650E5A93BC3D899uL),
        UInt128(0xEC5D3FA8CE427AFFuL, 0xA3E51F138AB4CEBFuL),
        UInt128(0x93BA47C980E98CDFuL, 0xC66F336C36B10138uL),
        UInt128(0xB8A8D9BBE123F017uL, 0xB80B0047445D4185uL),
        UInt128(0xE6D3102AD96CEC1DuL, 0xA60DC059157491E6uL),
        UInt128(0x9043EA1AC7E41392uL, 0x87C89837AD68DB30uL),
        UInt128(0xB454E4A179DD1877uL, 0x29BABE4598C311FCuL),
        UInt128(0xE16A1DC9D8545E94uL, 0xF4296DD6FEF3D67BuL),
        UInt128(0x8CE2529E2734BB1DuL, 0x1899E4A65F58660DuL),
        UInt128(0xB01AE745B101E9E4uL, 0x5EC05DCFF72E7F90uL),
        UInt128(0xDC21A1171D42645DuL, 0x76707543F4FA1F74uL),
        UInt128(0x899504AE72497EBAuL, 0x6A06494A791C53A9uL),
        UInt128(0xABFA45DA0EDBDE69uL, 0x0487DB9D17636893uL),
        UInt128(0xD6F8D7509292D603uL, 0x45A9D2845D3C42B7uL),
        UInt128(0x865B86925B9BC5C2uL, 0x0B8A2392BA45A9B3uL),
        UInt128(0xA7F26836F282B732uL, 0x8E6CAC7768D7141FuL),
        UInt128(0xD1EF0244AF2364FFuL, 0x3207D795430CD927uL),
        UInt128(0x8335616AED761F1FuL, 0x7F44E6BD49E807B9uL),
        UInt128(0xA402B9C5A8D3A6E7uL, 0x5F16206C9C6209A7uL),
        UInt128(0xCD036837130890A1uL, 0x36DBA887C37A8C10uL),
        UInt128(0x802221226BE55A64uL, 0xC2494954DA2C978AuL),
        UInt128(0xA02AA96B06DEB0FDuL, 0xF2DB9BAA10B7BD6DuL),
        UInt128(0xC83553C5C8965D3DuL, 0x6F92829494E5ACC8uL),
        UInt128(0xFA42A8B73ABBF48CuL, 0xCB772339BA1F17FAuL),
        UInt128(0x9C69A97284B578D7uL, 0xFF2A760414536EFCuL),
        UInt128(0xC38413CF25E2D70DuL, 0xFEF5138519684ABBuL),
        UInt128(0xF46518C2EF5B8CD1uL, 0x7EB258665FC25D6AuL),
        UInt128(0x98BF2F79D5993802uL, 0xEF2F773FFBD97A62uL),
        UInt128(0xBEEEFB584AFF8603uL, 0xAAFB550FFACFD8FBuL),
        UInt128(0xEEAABA2E5DBF6784uL, 0x95BA2A53F983CF39uL),
        UInt128(0x952AB45CFA97A0B2uL, 0xDD945A747BF26184uL),
        UInt128(0xBA756174393D88DFuL, 0x94F971119AEEF9E5uL),
        UInt128(0xE912B9D1478CEB17uL, 0x7A37CD5601AAB85EuL),
        UInt128(0x91ABB422CCB812EEuL, 0xAC62E055C10AB33BuL),
        UInt128(0xB616A12B7FE617AAuL, 0x577B986B314D600AuL),
        UInt128(0xE39C49765FDF9D94uL, 0xED5A7E85FDA0B80CuL),
        UInt128(0x8E41ADE9FBEBC27DuL, 0x14588F13BE847308uL),
        UInt128(0xB1D219647AE6B31CuL, 0x596EB2D8AE258FC9uL),
        UInt128(0xDE469FBD99A05FE3uL, 0x6FCA5F8ED9AEF3BCuL),
        UInt128(0x8AEC23D680043BEEuL, 0x25DE7BB9480D5855uL),
        UInt128(0xADA72CCC20054AE9uL, 0xAF561AA79A10AE6BuL),
        UInt128(0xD910F7FF28069DA4uL, 0x1B2BA1518094DA05uL),
        UInt128(0x87AA9AFF79042286uL, 0x90FB44D2F05D0843uL),
        UInt128(0xA99541BF57452B28uL, 0x353A1607AC744A54uL),
        UInt128(0xD3FA922F2D1675F2uL, 0x42889B8997915CE9uL),
        UInt128(0x847C9B5D7C2E09B7uL, 0x69956135FEBADA12uL),
        UInt128(0xA59BC234DB398C25uL, 0x43FAB9837E699096uL),
        UInt128(0xCF02B2C21207EF2EuL, 0x94F967E45E03F4BCuL),
        UInt128(0x8161AFB94B44F57DuL, 0x1D1BE0EEBAC278F6uL),
        UInt128(0xA1BA1BA79E1632DCuL, 0x6462D92A69731733uL),
        UInt128(0xCA28A291859BBF93uL, 0x7D7B8F7503CFDCFFuL),
        UInt128(0xFCB2CB35E702AF78uL, 0x5CDA735244C3D43FuL),
        UInt128(0x9DEFBF01B061ADABuL, 0x3A0888136AFA64A8uL),
        UInt128(0xC56BAEC21C7A1916uL, 0x088AAA1845B8FDD1uL),
        UInt128(0xF6C69A72A3989F5BuL, 0x8AAD549E57273D46uL),
        UInt128(0x9A3C2087A63F6399uL, 0x36AC54E2F678864CuL),
        UInt128(0xC0CB28A98FCF3C7FuL, 0x84576A1BB416A7DEuL),
        UInt128(0xF0FDF2D3F3C30B9FuL, 0x656D44A2A11C51D6uL),
        UInt128(0x969EB7C47859E743uL, 0x9F644AE5A4B1B326uL),
        UInt128(0xBC4665B596706114uL, 0x873D5D9F0DDE1FEFuL),
        UInt128(0xEB57FF22FC0C7959uL, 0xA90CB506D155A7EBuL),
        UInt128(0x9316FF75DD87CBD8uL, 0x09A7F12442D588F3uL),
        UInt128(0xB7DCBF5354E9BECEuL, 0x0C11ED6D538AEB30uL),
        UInt128(0xE5D3EF282A242E81uL, 0x8F1668C8A86DA5FBuL),
        UInt128(0x8FA475791A569D10uL, 0xF96E017D694487BDuL),
        UInt128(0xB38D92D760EC4455uL, 0x37C981DCC395A9ADuL),
        UInt128(0xE070F78D3927556AuL, 0x85BBE253F47B1418uL),
        UInt128(0x8C469AB843B89562uL, 0x93956D7478CCEC8FuL),
        UInt128(0xAF58416654A6BABBuL, 0x387AC8D1970027B3uL),
        UInt128(0xDB2E51BFE9D0696AuL, 0x06997B05FCC0319FuL),
        UInt128(0x88FCF317F22241E2uL, 0x441FECE3BDF81F04uL),
        UInt128(0xAB3C2FDDEEAAD25AuL, 0xD527E81CAD7626C4uL),
        UInt128(0xD60B3BD56A5586F1uL, 0x8A71E223D8D3B075uL),
        UInt128(0x85C7056562757456uL, 0xF6872D5667844E4AuL),
        UInt128(0xA738C6BEBB12D16CuL, 0xB428F8AC016561DCuL),
        UInt128(0xD106F86E69D785C7uL, 0xE13336D701BEBA53uL),
        UInt128(0x82A45B450226B39CuL, 0xECC0024661173474uL),
        UInt128(0xA34D721642B06084uL, 0x27F002D7F95D0191uL),
        UInt128(0xCC20CE9BD35C78A5uL, 0x31EC038DF7B441F5uL),
        UInt128(0xFF290242C83396CEuL, 0x7E67047175A15272uL),
        UInt128(0x9F79A169BD203E41uL, 0x0F0062C6E984D387uL),
        UInt128(0xC75809C42C684DD1uL, 0x52C07B78A3E60869uL),
        UInt128(0xF92E0C3537826145uL, 0xA7709A56CCDF8A83uL),
        UInt128(0x9BBCC7A142B17CCBuL, 0x88A66076400BB692uL),
        UInt128(0xC2ABF989935DDBFEuL, 0x6ACFF893D00EA436uL),
        UInt128(0xF356F7EBF83552FEuL, 0x0583F6B8C4124D44uL),
        UInt128(0x98165AF37B2153DEuL, 0xC3727A337A8B704BuL),
        UInt128(0xBE1BF1B059E9A8D6uL, 0x744F18C0592E4C5DuL),
        UInt128(0xEDA2EE1C7064130CuL, 0x1162DEF06F79DF74uL),
        UInt128(0x9485D4D1C63E8BE7uL, 0x8ADDCB5645AC2BA9uL),
        UInt128(0xB9A74A0637CE2EE1uL, 0x6D953E2BD7173693uL),
        UInt128(0xE8111C87C5C1BA99uL, 0xC8FA8DB6CCDD0438uL),
        UInt128(0x910AB1D4DB9914A0uL, 0x1D9C9892400A22A3uL),
        UInt128(0xB54D5E4A127F59C8uL, 0x2503BEB6D00CAB4CuL),
        UInt128(0xE2A0B5DC971F303AuL, 0x2E44AE64840FD61EuL),
        UInt128(0x8DA471A9DE737E24uL, 0x5CEAECFED289E5D3uL),
        UInt128(0xB10D8E1456105DADuL, 0x7425A83E872C5F48uL),
        UInt128(0xDD50F1996B947518uL, 0xD12F124E28F7771AuL),
        UInt128(0x8A5296FFE33CC92FuL, 0x82BD6B70D99AAA70uL),
        UInt128(0xACE73CBFDC0BFB7BuL, 0x636CC64D1001550CuL),
        UInt128(0xD8210BEFD30EFA5AuL, 0x3C47F7E05401AA4FuL),
        UInt128(0x8714A775E3E95C78uL, 0x65ACFAEC34810A72uL),
        UInt128(0xA8D9D1535CE3B396uL, 0x7F1839A741A14D0EuL),
        UInt128(0xD31045A8341CA07CuL, 0x1EDE48111209A051uL),
        UInt128(0x83EA2B892091E44DuL, 0x934AED0AAB460433uL),
        UInt128(0xA4E4B66B68B65D60uL, 0xF81DA84D56178540uL),
        UInt128(0xCE1DE40642E3F4B9uL, 0x36251260AB9D668FuL),
        UInt128(0x80D2AE83E9CE78F3uL, 0xC1D72B7C6B42601AuL),
        UInt128(0xA1075A24E4421730uL, 0xB24CF65B8612F820uL),
        UInt128(0xC94930AE1D529CFCuL, 0xDEE033F26797B628uL),
        UInt128(0xFB9B7CD9A4A7443CuL, 0x169840EF017DA3B2uL),
        UInt128(0x9D412E0806E88AA5uL, 0x8E1F289560EE864FuL),
        UInt128(0xC491798A08A2AD4EuL, 0xF1A6F2BAB92A27E3uL),
        UInt128(0xF5B5D7EC8ACB58A2uL, 0xAE10AF696774B1DCuL),
        UInt128(0x9991A6F3D6BF1765uL, 0xACCA6DA1E0A8EF2AuL),
        UInt128(0xBFF610B0CC6EDD3FuL, 0x17FD090A58D32AF4uL),
        UInt128(0xEFF394DCFF8A948EuL, 0xDDFC4B4CEF07F5B1uL),
        UInt128(0x95F83D0A1FB69CD9uL, 0x4ABDAF101564F98FuL),
        UInt128(0xBB764C4CA7A4440FuL, 0x9D6D1AD41ABE37F2uL),
        UInt128(0xEA53DF5FD18D5513uL, 0x84C86189216DC5EEuL),
        UInt128(0x92746B9BE2F8552CuL, 0x32FD3CF5B4E49BB5uL),
        UInt128(0xB7118682DBB66A77uL, 0x3FBC8C33221DC2A2uL),
        UInt128(0xE4D5E82392A40515uL, 0x0FABAF3FEAA5334BuL),
        UInt128(0x8F05B1163BA6832DuL, 0x29CB4D87F2A7400FuL),
        UInt128(0xB2C71D5BCA9023F8uL, 0x743E20E9EF511013uL),
        UInt128(0xDF78E4B2BD342CF6uL, 0x914DA9246B255417uL),
        UInt128(0x8BAB8EEFB6409C1AuL, 0x1AD089B6C2F7548FuL),
        UInt128(0xAE9672ABA3D0C320uL, 0xA184AC2473B529B2uL),
        UInt128(0xDA3C0F568CC4F3E8uL, 0xC9E5D72D90A2741FuL),
        UInt128(0x8865899617FB1871uL, 0x7E2FA67C7A658893uL),
        UInt128(0xAA7EEBFB9DF9DE8DuL, 0xDDBB901B98FEEAB8uL),
        UInt128(0xD51EA6FA85785631uL, 0x552A74227F3EA566uL),
        UInt128(0x8533285C936B35DEuL, 0xD53A88958F872760uL),
        UInt128(0xA67FF273B8460356uL, 0x8A892ABAF368F138uL),
        UInt128(0xD01FEF10A657842CuL, 0x2D2B7569B0432D86uL),
        UInt128(0x8213F56A67F6B29BuL, 0x9C3B29620E29FC74uL),
        UInt128(0xA298F2C501F45F42uL, 0x8349F3BA91B47B90uL),
        UInt128(0xCB3F2F7642717713uL, 0x241C70A936219A74uL),
        UInt128(0xFE0EFB53D30DD4D7uL, 0xED238CD383AA0111uL),
        UInt128(0x9EC95D1463E8A506uL, 0xF4363804324A40ABuL),
        UInt128(0xC67BB4597CE2CE48uL, 0xB143C6053EDCD0D6uL),
        UInt128(0xF81AA16FDC1B81DAuL, 0xDD94B7868E94050BuL),
        UInt128(0x9B10A4E5E9913128uL, 0xCA7CF2B4191C8327uL),
        UInt128(0xC1D4CE1F63F57D72uL, 0xFD1C2F611F63A3F1uL),
        UInt128(0xF24A01A73CF2DCCFuL, 0xBC633B39673C8CEDuL),
        UInt128(0x976E41088617CA01uL, 0xD5BE0503E085D814uL),
        UInt128(0xBD49D14AA79DBC82uL, 0x4B2D8644D8A74E19uL),
        UInt128(0xEC9C459D51852BA2uL, 0xDDF8E7D60ED1219FuL),
        UInt128(0x93E1AB8252F33B45uL, 0xCABB90E5C942B504uL),
        UInt128(0xB8DA1662E7B00A17uL, 0x3D6A751F3B936244uL),
        UInt128(0xE7109BFBA19C0C9DuL, 0x0CC512670A783AD5uL),
        UInt128(0x906A617D450187E2uL, 0x27FB2B80668B24C6uL),
        UInt128(0xB484F9DC9641E9DAuL, 0xB1F9F660802DEDF7uL),
        UInt128(0xE1A63853BBD26451uL, 0x5E7873F8A0396974uL),
        UInt128(0x8D07E33455637EB2uL, 0xDB0B487B6423E1E9uL),
        UInt128(0xB049DC016ABC5E5FuL, 0x91CE1A9A3D2CDA63uL),
        UInt128(0xDC5C5301C56B75F7uL, 0x7641A140CC7810FCuL),
        UInt128(0x89B9B3E11B6329BAuL, 0xA9E904C87FCB0A9EuL),
        UInt128(0xAC2820D9623BF429uL, 0x546345FA9FBDCD45uL),
        UInt128(0xD732290FBACAF133uL, 0xA97C177947AD4096uL),
        UInt128(0x867F59A9D4BED6C0uL, 0x49ED8EABCCCC485EuL),
        UInt128(0xA81F301449EE8C70uL, 0x5C68F256BFFF5A75uL),
        UInt128(0xD226FC195C6A2F8CuL, 0x73832EEC6FFF3112uL),
        UInt128(0x83585D8FD9C25DB7uL, 0xC831FD53C5FF7EACuL),
        UInt128(0xA42E74F3D032F525uL, 0xBA3E7CA8B77F5E56uL),
        UInt128(0xCD3A1230C43FB26FuL, 0x28CE1BD2E55F35ECuL),
        UInt128(0x80444B5E7AA7CF85uL, 0x7980D163CF5B81B4uL),
        UInt128(0xA0555E361951C366uL, 0xD7E105BCC3326220uL),
        UInt128(0xC86AB5C39FA63440uL, 0x8DD9472BF3FEFAA8uL),
        UInt128(0xFA856334878FC150uL, 0xB14F98F6F0FEB952uL),
        UInt128(0x9C935E00D4B9D8D2uL, 0x6ED1BF9A569F33D4uL),
        UInt128(0xC3B8358109E84F07uL, 0x0A862F80EC4700C9uL),
        UInt128(0xF4A642E14C6262C8uL, 0xCD27BB612758C0FBuL),
        UInt128(0x98E7E9CCCFBD7DBDuL, 0x8038D51CB897789DuL),
        UInt128(0xBF21E44003ACDD2CuL, 0xE0470A63E6BD56C4uL),
        UInt128(0xEEEA5D5004981478uL, 0x1858CCFCE06CAC75uL),
        UInt128(0x95527A5202DF0CCBuL, 0x0F37801E0C43EBC9uL),
        UInt128(0xBAA718E68396CFFDuL, 0xD30560258F54E6BBuL),
        UInt128(0xE950DF20247C83FDuL, 0x47C6B82EF32A206AuL),
        UInt128(0x91D28B7416CDD27EuL, 0x4CDC331D57FA5442uL),
        UInt128(0xB6472E511C81471DuL, 0xE0133FE4ADF8E953uL),
        UInt128(0xE3D8F9E563A198E5uL, 0x58180FDDD97723A7uL),
        UInt128(0x8E679C2F5E44FF8FuL, 0x570F09EAA7EA7649uL),
        UInt128(0xB201833B35D63F73uL, 0x2CD2CC6551E513DBuL),
        UInt128(0xDE81E40A034BCF4FuL, 0xF8077F7EA65E58D2uL),
        UInt128(0x8B112E86420F6191uL, 0xFB04AFAF27FAF783uL),
        UInt128(0xADD57A27D29339F6uL, 0x79C5DB9AF1F9B564uL),
        UInt128(0xD94AD8B1C7380874uL, 0x18375281AE7822BDuL),
        UInt128(0x87CEC76F1C830548uL, 0x8F2293910D0B15B6uL),
        UInt128(0xA9C2794AE3A3C69AuL, 0xB2EB3875504DDB23uL),
        UInt128(0xD433179D9C8CB841uL, 0x5FA60692A46151ECuL),
        UInt128(0x849FEEC281D7F328uL, 0xDBC7C41BA6BCD334uL),
        UInt128(0xA5C7EA73224DEFF3uL, 0x12B9B522906C0801uL),
        UInt128(0xCF39E50FEAE16BEFuL, 0xD768226B34870A01uL),
        UInt128(0x81842F29F2CCE375uL, 0xE6A1158300D46641uL),
        UInt128(0xA1E53AF46F801C53uL, 0x60495AE3C1097FD1uL),
        UInt128(0xCA5E89B18B602368uL, 0x385BB19CB14BDFC5uL),
        UInt128(0xFCF62C1DEE382C42uL, 0x46729E03DD9ED7B6uL),
        UInt128(0x9E19DB92B4E31BA9uL, 0x6C07A2C26A8346D2uL),
        UInt128(0xC5A05277621BE293uL, 0xC7098B7305241886uL),
        UInt128(0xF70867153AA2DB38uL, 0xB8CBEE4FC66D1EA8uL)
    )
}

//template <int a, class UInt>
fun count_factors(n: UInt, a: UInt):UInt {
//    static_assert(a > 1)
    var c = 0u
    var n = n
    while (n % a == 0u) {
        n /= a
        ++c
    }
    return c
}

fun count_factors(n: ULong, a: UInt):UInt {
//    static_assert(a > 1)
    var c = 0u
    var n = n
    while (n % a == 0uL) {
        n /= a
        ++c
    }
    return c
}

val DIVIDE_MAGIC_NUMBER = uintArrayOf(6554u, 656u)

//template <class T, unsigned Size>
//static constexpr bool valid_float = std::numeric_limits<T>::is_iec559 &&
//std::numeric_limits<T>::radix == 2 && sizeof(T) == Size


//static_assert(valid_float<float, 4>,
//"simple_dragonbox: float may not be IEEE-754 binary32")


//static_assert(valid_float<double, 8>,
//"simple_dragonbox: double may not be IEEE-754 binary64")

/*
fun reverse(char* begin, char* end) {
    while (begin < --end) {
        char tmp = *begin
        *begin++ = *end
        *end = tmp
    }
}
*/

interface impl<F, I> {
    val format: FloatFormat<F>
}

private object DoubleToDecimalConverter: impl<Double, ULong> {
    override val format = float_format_double

    fun carrier_uint(i: Int): ULong = i.toULong()
    fun carrier_uint(i: UInt): ULong = i.toULong()
    fun carrier_uint(i: ULong): ULong = i

    //    static_assert(sizeof(carrier_uint) == sizeof(Float))

    val min_exponent = -1022
    val max_exponent = 1023
    val significand_bits = 52
    val carrier_bits = 64
    val kappa = (carrier_bits - significand_bits - 2).floorLog10Pow2() - 1

    val min_k = minOf(-(max_exponent - significand_bits).floorLog10Pow2MinusLog10_4Over3(),
        -(max_exponent - significand_bits).floorLog10Pow2() + kappa)

    // We do invoke shorter_interval_case for exponent == min_exponent case;
    // so we should not add 1 here.
    val max_k = maxOf(-(min_exponent - significand_bits /*+ 1*/).floorLog10Pow2MinusLog10_4Over3(),
        -(min_exponent - significand_bits).floorLog10Pow2() + kappa)

    val case_shorter_interval_left_endpoint_lower_threshold = 2

    val case_shorter_interval_left_endpoint_upper_threshold = 2 +
            (computePower10(
                k = count_factors(
                    (carrier_uint(1) shl (significand_bits + 2)) - 1uL,
                    5u
                ) + 1u
            ) / 3u).floorLog2()

    val case_shorter_interval_right_endpoint_lower_threshold = 0

    val case_shorter_interval_right_endpoint_upper_threshold = 2 +
            (computePower10(
                count_factors((carrier_uint(1) shl (significand_bits + 1)) + 1u, 5u) + 1u
            ) / 3u).floorLog2()

    val shorter_interval_tie_lower_threshold = -(significand_bits + 4).floorLog5Pow2MinusLog5_3() - 2 - significand_bits

    val shorter_interval_tie_upper_threshold = -(significand_bits + 2).floorLog5Pow2() - 2 - significand_bits

//    static_assert(kappa >= 1)
//    static_assert(carrier_bits >= significand_bits + 2 + floor_log2_pow10(kappa + 1))
//    static_assert(min_k >= format::min_k && max_k <= format::max_k)

    fun check_divisibility_and_divide_by_pow10(n: MutableULong, N: UInt): Boolean {
        // Make sure the computation for max_n does not overflow.
//        static_assert(N + 1 <= floor_log10_pow2(carrier_bits), "")
        assert(n.value <= computePower10Long(N + 1u))

        val magic_number = DIVIDE_MAGIC_NUMBER[N.toInt() - 1]
        val prod = (n * magic_number)

        val mask = 0xFFFFuL
        val result = ((prod and mask) < magic_number)

        val n = carrier_uint(prod shr 16)
        return result
    }

    // Compute floor(n / 10^N) for small n and N.
    // Precondition: n <= 10^(N+1)
    fun small_division_by_pow10(n: UInt, N: UInt): ULong {
        // Make sure the computation for max_n does not overflow.
        assert(N.toInt() + 1 <= carrier_bits.floorLog10Pow2())
        assert(n <= computePower10Long(N+1u))
        return carrier_uint((n * DIVIDE_MAGIC_NUMBER[N.toInt() - 1]) shr 16)
    }

    @JvmInline
    value class binary_fp(private val bits: Long) {
        val significand: ULong get() = bits.toULong() and 0xF_FFFF_FFFF_FFFFuL
        val exponent: Int get() = (((bits ushr 52) and 0x7ff).toInt() + -1023)
        val is_negative: Boolean get() = bits.ushr(63) != 0L

        fun isFinite(): Boolean = (bits ushr 52) and 0x7ffL != 0x7ffL

        constructor(f: Double): this(f.toRawBits())
    }

    fun decompose_float(x: Double): binary_fp {
        return binary_fp(x)
    }

    fun is_finite(binary_exponent: Int): Boolean {
        return binary_exponent != 0xff
    }

    fun to_decimal(d: Double): DecimalFP64 {
        val bin = binary_fp(d)
        return to_decimal(bin.significand, bin.exponent, bin.is_negative)
    }

    // The main algorithm assumes the input is a normal/subnormal finite number.
    fun to_decimal(binary_significand: ULong, binary_exponent: Int, is_negative: Boolean): DecimalFP64 {
        var binary_exponent = binary_exponent
        val is_even = binary_significand % 2uL == 0uL
        var two_fc = binary_significand shl 1

        // Is the input a normal number?
        if (binary_exponent != 0) {
            binary_exponent += format.exponent_bias - format.significand_bits

            // Shorter interval case; proceed like Schubfach.
            // One might think this condition is wrong, since when exponent_bits ==
            // 1 and two_fc == 0, the interval is actually regular. However, it
            // turns out that this seemingly wrong condition is actually fine,
            // because the end result is anyway the same.
            //
            // [binary32]
            // (fc-1/2) * 2^e = 1.175'494'28... * 10^-38
            // (fc-1/4) * 2^e = 1.175'494'31... * 10^-38
            //    fc    * 2^e = 1.175'494'35... * 10^-38
            // (fc+1/2) * 2^e = 1.175'494'42... * 10^-38
            //
            // Hence, shorter_interval_case will return 1.175'494'4 * 10^-38.
            // 1.175'494'3 * 10^-38 is also a correct shortest representation that
            // will be rejected if we assume shorter interval, but 1.175'494'4 *
            // 10^-38 is closer to the true value so it doesn't matter.
            //
            // [binary64]
            // (fc-1/2) * 2^e = 2.225'073'858'507'201'13... * 10^-308
            // (fc-1/4) * 2^e = 2.225'073'858'507'201'25... * 10^-308
            //    fc    * 2^e = 2.225'073'858'507'201'38... * 10^-308
            // (fc+1/2) * 2^e = 2.225'073'858'507'201'63... * 10^-308
            //
            // Hence, shorter_interval_case will return 2.225'073'858'507'201'4 *
            // 10^-308. This is indeed of the shortest length, and it is the unique
            // one closest to the true value among valid representations of the same
            // length.

            // Shorter interval case.
            if (two_fc == 0uL) {
                // Compute k and beta.
                val minus_k = binary_exponent.floorLog10Pow2MinusLog10_4Over3()
                val beta = binary_exponent + (-minus_k).floorLog2Pow10()

                // Compute xi and zi.
                val cache = format.cache[-minus_k - format.min_k]

                var xi = format.compute_left_endpoint_for_shorter_interval_case(cache, beta)
                val zi = format.compute_right_endpoint_for_shorter_interval_case(cache, beta)

                // If the left endpoint is not an integer, increase it.
                // (Both endpoints are always included since the significand is even.)
                if (!is_left_endpoint_integer_shorter_interval(binary_exponent)) {
                    ++xi
                }

                // Try bigger divisor.
                // zi is at most floor((f_c + 1/2) * 2^e * 10^k0).
                // Substituting f_c = 2^p and k0 = -floor(log10(3 * 2^(e-2))), we get
                // zi <= floor((2^(p+1) + 1) * 20/3) <= ceil((2^(p+1) + 1)/3) * 20.
                // This computation does not overflow for any of the formats I care about.
                val decimal_significand = MutableULong(format.divide_by_pow10(zi,1u,  (((carrier_uint(2) shl significand_bits) + 1u) / 3u + 1u) * 20u))

                // If succeed, remove trailing zeros if necessary and return.
                if (decimal_significand * 10u >= xi) {
                    val decimal_exponent = MutableInt(minus_k + 1)
                    format.remove_trailing_zeros(decimal_significand, decimal_exponent)
                    return DecimalFP64(decimal_significand.value, decimal_exponent.value, is_negative)
                }

                // Otherwise, compute the round-up of y.
                decimal_significand.value = format.compute_round_up_for_shorter_interval_case(cache, beta)

                // When tie occurs, choose the even one.
                if (decimal_significand.value % 2uL != 0uL &&
                    binary_exponent >= shorter_interval_tie_lower_threshold &&
                    binary_exponent <= shorter_interval_tie_upper_threshold) {
                    --decimal_significand.value
                } else if (decimal_significand.value < xi) {
                    ++decimal_significand.value
                }
                return DecimalFP64(decimal_significand.value, minus_k, is_negative)
            }

            // Normal interval case.
            two_fc = two_fc or (carrier_uint(1) shl (format.significand_bits + 1))
        }
        else {
            // Is the input a subnormal number?
            // Normal interval case.
            binary_exponent = min_exponent - format.significand_bits
        }

        //////////////////////////////////////////////////////////////////////
        // Step 1: Schubfach multiplier calculation.
        //////////////////////////////////////////////////////////////////////

        // Compute k and beta.
        val minus_k = binary_exponent.floorLog10Pow2() - kappa
        val cache = format.cache[-minus_k - format.min_k]
        val beta = binary_exponent + (-minus_k).floorLog2Pow10()

        // Compute zi and deltai.
        // 10^kappa <= deltai < 10^(kappa + 1)
        val deltai = format.compute_delta(cache, beta)
        // For the case of binary32, the result of integer check is not correct for
        // 29711844 * 2^-82
        // = 6.1442653300000000008655037797566933477355632930994033813476... * 10^-18
        // and 29711844 * 2^-81
        // = 1.2288530660000000001731007559513386695471126586198806762695... * 10^-17,
        // and they are the unique counterexamples. However, since 29711844 is even,
        // this does not cause any problem for the endpoints calculations; it can only
        // cause a problem when we need to perform integer check for the center.
        // Fortunately, with these inputs, that branch is never executed, so we are
        // fine.
        val z_result =
            format.compute_mul(carrier_uint((two_fc or 1u) shl beta), cache)

        //////////////////////////////////////////////////////////////////////
        // Step 2: Try larger divisor; remove trailing zeros if necessary.
        //////////////////////////////////////////////////////////////////////

        val big_divisor = computePower10Long(kappa.toUInt() + 1u)
        val small_divisor = computePower10Long(kappa.toUInt())

        // Using an upper bound on zi, we might be able to optimize the division
        // better than the compiler; we are computing zi / big_divisor here.
        var decimal_significand = format.divide_by_pow10(z_result.integer_part, kappa.toUInt() + 1u, (carrier_uint(2) shl significand_bits) * big_divisor - 1u)
        var r = carrier_uint(z_result.integer_part - big_divisor * decimal_significand)

        do {
            if (r < deltai) {
                // Exclude the right endpoint if necessary.
                if (!(r != 0uL || !z_result.is_integer || is_even)) {
                    --decimal_significand
                    r = big_divisor
                    break
                }
            } else if (r > deltai) {
                break
            } else {
                // r == deltai; compare fractional parts.
                val x_result =
                    format.compute_mul_parity(two_fc - 1u, cache, beta)

                if (!(x_result.parity or (x_result.is_integer and is_even))) {
                    break
                }
            }

            val decimal_exponent = MutableInt(minus_k + kappa + 1)
            val m = MutableULong(decimal_significand)
            format.remove_trailing_zeros(m, decimal_exponent)
            return DecimalFP64(m.value, decimal_exponent.value, is_negative)
        } while (false)


        //////////////////////////////////////////////////////////////////////
        // Step 3: Find the significand with the smaller divisor.
        //////////////////////////////////////////////////////////////////////

        decimal_significand *= 10u

        // delta is equal to 10^(kappa + elog10(2) - floor(elog10(2))), so dist cannot
        // be larger than r.
        val dist = MutableULong(carrier_uint(r - (deltai / 2uL) + (small_divisor / 2uL)))
        val approx_y_parity = ((dist.value xor (small_divisor shr 1)) and 1uL) != 0uL

        // Is dist divisible by 10^kappa?
        val divisible_by_small_divisor = check_divisibility_and_divide_by_pow10(dist, kappa.toUInt())

        // Add dist / 10^kappa to the significand.
        decimal_significand += dist.value

        if (divisible_by_small_divisor) {
            // Check z^(f) >= epsilon^(f).
            // We have either yi == zi - epsiloni or yi == (zi - epsiloni) - 1,
            // where yi == zi - epsiloni if and only if z^(f) >= epsilon^(f).
            // Since there are only 2 possibilities, we only need to care about the
            // parity. Also, zi and r should have the same parity since the divisor
            // is an even number.
            val y_result = format.compute_mul_parity(two_fc, cache, beta)
            if (y_result.parity != approx_y_parity) {
                --decimal_significand
            }
            else {
                // If z^(f) >= epsilon^(f), we might have a tie
                // when z^(f) == epsilon^(f), or equivalently, when y is an integer.
                // When tie happens, always choose the even one.
                if ((decimal_significand % 2uL != 0uL) and y_result.is_integer) {
                    --decimal_significand
                }
            }
        }

        return DecimalFP64(decimal_significand, minus_k + kappa, is_negative)
    }

    fun is_right_endpoint_integer_shorter_interval(binary_exponent: Int): Boolean {
        return binary_exponent >= case_shorter_interval_right_endpoint_lower_threshold &&
                binary_exponent <= case_shorter_interval_right_endpoint_upper_threshold
    }

    fun is_left_endpoint_integer_shorter_interval(binary_exponent: Int): Boolean {
        return binary_exponent >= case_shorter_interval_left_endpoint_lower_threshold &&
                binary_exponent <= case_shorter_interval_left_endpoint_upper_threshold
    }

    fun to_chars_n(x: Double, buffer: StringBuilder): StringBuilder  {
        val decomposed = decompose_float(x)

        if (!x.isFinite()) {
            if (decomposed.significand == 0uL) {
                if (decomposed.is_negative) buffer.append('-')
                buffer.append("Infinity")
            } else {
                buffer.append("NaN")
            }
            return buffer
        }

        if (decomposed.is_negative) {
            buffer.append('-')
        }

        if (decomposed.significand == 0uL && decomposed.exponent == 0) {
            buffer.append("0E0")
            return buffer
        }

        var (dec_sig, dec_exp, dec_sign) = to_decimal(x)

        if (dec_sig < 10u) {
            buffer.append('0' + dec_sig.toInt())
        } else {
            val reversed = StringBuilder()

            do {
                reversed.append('0'+ (dec_sig % 10u).toInt())

                dec_sig /= 10u
                ++dec_exp
            } while (dec_sig >= 10u)
            buffer.append('0' + dec_sig.toInt())
            buffer.append('.')
            buffer.append(reversed.reverse())
        }

        buffer.append('E')

        if (dec_exp < 0) {
            buffer.append('-')
            dec_exp = -dec_exp
        }

        val reversed = StringBuilder()
        do {
            reversed.append('0' + dec_exp % 10)
            dec_exp /= 10
        } while (dec_exp != 0)
        return buffer.append(reversed.reverse())
    }
}


//template <class Float>
fun toDecimal(x: Float): DecimalFP32 {
    assert(x.isFinite() && x != 0.0f)
    return FloatToDecimalConverter.to_decimal(x)
}

fun toDecimal(x: Double): DecimalFP64 {
    assert(x.isFinite() && x != 0.0)
    return DoubleToDecimalConverter.to_decimal(x)
}

// Null-terminate and bypass the return value of impl::to_chars_n.
//template <class Float>
fun to_chars(x: Float, buffer: StringBuilder): StringBuilder {
    return FloatToDecimalConverter.to_chars_n(x, buffer)
}

// Maximum required buffer size (excluding null-terminator)
//template <class Float>
private val max_output_string_length_float get() = float_format_float.max_output_string_length
