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

package io.github.pdvrieze.xml.schematypes.values.instances

import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
import kotlin.jvm.JvmInline
import kotlin.math.sign

internal object FloatToDecimalConverter {
    val format get() = this

    const val EXPONENT_BIAS = -127
    const val DECIMAL_SIGNIFICAND_DIGITS = 9
    const val DECIMAL_EXPONENT_DIGITS = 2
    const val CACHE_BITS = 64
    const val MAX_OUTPUT_STRING_LENGTH = 1 + DECIMAL_SIGNIFICAND_DIGITS + 1 + 1 + 1 + DECIMAL_EXPONENT_DIGITS


    fun compute_mul(u: UInt, cache: ULong): compute_mul_resultUInt {
        val r = umul96_upper64(u, cache)
        return compute_mul_resultUInt((r shr 32).toUInt(), r == 0uL)
    }

    fun compute_delta(cache: ULong, beta: Int): UInt {
        return (cache shr (CACHE_BITS - 1 - beta)).toUInt()
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
        return ((cache - (cache shr (SIGNIFICAND_BITS + 2))) shr
                (CACHE_BITS - SIGNIFICAND_BITS - 1 - beta)).toUInt()
    }

    fun compute_right_endpoint_for_shorter_interval_case(cache: ULong, beta: Int): UInt {
        return ((cache + (cache shr (SIGNIFICAND_BITS + 1))) shr
                (CACHE_BITS - SIGNIFICAND_BITS - 1 - beta)).toUInt()
    }

    fun compute_round_up_for_shorter_interval_case(cache: ULong, beta: Int): UInt {
        return ((cache shr (CACHE_BITS - SIGNIFICAND_BITS - 2 - beta)).toUInt() + 1u) shr 1
    }

    fun divide_by_pow10(n: UInt, N: UInt): UInt {
        return n / computePower10(N)
    }

    //    template <int N, n_max: UInt>
    fun divide_by_pow10_orig(n: UInt, N: UInt, n_max: UInt): UInt {

        // Specialize for 32-bit division by 10.
        // Without the bound on n_max (which compilers these days never leverage), the
        // minimum needed amount of shift is larger than 32. Hence, this may generate better
        // code for 32-bit or smaller architectures. Even for 64-bit architectures, it seems
        // compilers tend to generate mov + mul instead of a single imul for an unknown
        // reason if we just write n / 10.
        if (N == 1u && n_max <= 0x4000_0004u) {
            return ((n.toULong() * 0x1999_999AuL) shr 32).toUInt()
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





    fun carrier_uint(i: UInt): UInt = i

    //    static_assert(sizeof(carrier_uint) == sizeof(Float))

    const val MIN_EXPONENT = -126
    const val MAX_EXPONENT = 127
    const val SIGNIFICAND_BITS = 23
    const val CARRIER_BITS = 32
    val KAPPA = (((CARRIER_BITS - SIGNIFICAND_BITS - 2) * 315653) shr 20) - 1

    val MIN_K = minOf(-(MAX_EXPONENT - SIGNIFICAND_BITS).floorLog10Pow2MinusLog10_4Over3(),
        -(MAX_EXPONENT - SIGNIFICAND_BITS).floorLog10Pow2() + KAPPA)

    // We do invoke shorter_interval_case for exponent == min_exponent case;
    // so we should not add 1 here.
    val MAX_K = maxOf(-(MIN_EXPONENT - SIGNIFICAND_BITS /*+ 1*/).floorLog10Pow2MinusLog10_4Over3(),
        -(MIN_EXPONENT - SIGNIFICAND_BITS).floorLog10Pow2() + KAPPA)

    const val CASE_SHORTER_INTERVAL_LEFT_ENDPOINT_LOWER_THRESHOLD = 2

    val CASE_SHORTER_INTERVAL_LEFT_ENDPOINT_UPPER_THRESHOLD =
        2 + (computePower10(
            k = count_factors(1u.shl(SIGNIFICAND_BITS+2)-1u, 5u)
        ) / 3u).floorLog2()

    val CASE_SHORTER_INTERVAL_LEFT_RANGE = CASE_SHORTER_INTERVAL_LEFT_ENDPOINT_LOWER_THRESHOLD..
            CASE_SHORTER_INTERVAL_LEFT_ENDPOINT_UPPER_THRESHOLD

    const val CASE_SHORTER_INTERVAL_RIGHT_ENDPOINT_LOWER_THRESHOLD = 0

    val CASE_SHORTER_INTERVAL_RIGHT_ENDPOINT_UPPER_THRESHOLD = //11
        2 + (computePower10(
            k = count_factors(1u.shl(SIGNIFICAND_BITS + 1) + 1u, 10u)
        ) / 3u).floorLog2()

    val CASE_SHORTER_INTERVAL_RIGHT_RANGE = CASE_SHORTER_INTERVAL_RIGHT_ENDPOINT_LOWER_THRESHOLD..CASE_SHORTER_INTERVAL_RIGHT_ENDPOINT_UPPER_THRESHOLD

    val SHORTER_INTERVAL_TIE_LOWER_THRESHOLD = -(SIGNIFICAND_BITS + 4).floorLog5Pow2MinusLog5_3() - 2 - SIGNIFICAND_BITS

    val SHORTER_INTERVAL_TIE_UPPER_THRESHOLD = -(SIGNIFICAND_BITS + 2).floorLog5Pow2() - 2 - SIGNIFICAND_BITS

    val SHORTER_INTERNVAL_TIE_RANGE = SHORTER_INTERVAL_TIE_LOWER_THRESHOLD..SHORTER_INTERVAL_TIE_UPPER_THRESHOLD

//    static_assert(kappa >= 1)
//    static_assert(carrier_bits >= significand_bits + 2 + floor_log2_pow10(kappa + 1))
//    static_assert(min_k >= format::min_k && max_k <= format::max_k)

    fun check_divisibility_and_divide_by_pow10(n: MutableUInt): Boolean {
        val N = 1
        // Make sure the computation for max_n does not overflow.
//        static_assert(N + 1 <= floor_log10_pow2(carrier_bits), "")
//        assert(n.value <= compute_power(carrier_uint(10), N + 1u))

        val magicNumber = DIVIDE_MAGIC_NUMBER[0]
        val prod = (n * magicNumber)

        val result = (prod and 0xFFFFu) < magicNumber

        n.value = prod shr 16
        return result
    }

    @JvmInline
    value class FloatBits(private val bits: Int) {
        val significand: UInt get() = (bits and 0x007fffff).toUInt()
        val binary_exponent: Int get() = ((bits ushr 23) and 0xff)
        val exponent: Int get() = ((bits ushr 23) and 0xff)+format.EXPONENT_BIAS
        val isNegative: Boolean get() = bits.ushr(31) != 0

        fun isFinite(): Boolean = (bits and 0x7f800000) != 0x7f800000

        constructor(f: Float): this(f.toRawBits())
    }

    // The main algorithm assumes the input is a normal/subnormal finite number.
    fun to_decimal(number: Float): DecimalFP32 {
        val numberBits = FloatBits(number)
        var rawExponent = numberBits.binary_exponent
        val isEven = numberBits.significand.isEven()
        var twoFc = numberBits.significand shl 1
        val isNegative = numberBits.isNegative

        // Is the input a normal number?
        if (rawExponent != 0) {
            rawExponent -= 150 //-format.exponent_bias - format.significand_bits

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
            if (twoFc == 0u) {
                // Compute k and beta.
                val minus_k = rawExponent.floorLog10Pow2MinusLog10_4Over3()
                val beta = rawExponent + (-minus_k).floorLog2Pow10()

                // Compute xi and zi.
                val cache = format.cache[-minus_k - MIN_K]

                var xi = format.compute_left_endpoint_for_shorter_interval_case(cache, beta)
                val zi = format.compute_right_endpoint_for_shorter_interval_case(cache, beta)

                // If the left endpoint is not an integer, increase it.
                // (Both endpoints are always included since the significand is even.)
                if (rawExponent !in CASE_SHORTER_INTERVAL_LEFT_RANGE) {
                    ++xi
                }

                // Try bigger divisor.
                // zi is at most floor((f_c + 1/2) * 2^e * 10^k0).
                // Substituting f_c = 2^p and k0 = -floor(log10(3 * 2^(e-2))), we get
                // zi <= floor((2^(p+1) + 1) * 20/3) <= ceil((2^(p+1) + 1)/3) * 20.
                // This computation does not overflow for any of the formats I care about.
                var decimal_significand = zi / 10u

                // If succeed, remove trailing zeros if necessary and return.
                if (decimal_significand * 10u >= xi) {
                    val decimal_exponent = minus_k + 1
//                    format.remove_trailing_zeros(decimal_significand, decimal_exponent)
                    return DecimalFP32.withoutTrailingZeros(decimal_significand, decimal_exponent, isNegative)
                }

                // Otherwise, compute the round-up of y.
                decimal_significand = format.compute_round_up_for_shorter_interval_case(cache, beta)

                // When tie occurs, choose the even one.
                if (decimal_significand.isEven() && rawExponent in SHORTER_INTERNVAL_TIE_RANGE) {
                    --decimal_significand
                } else if (decimal_significand < xi) {
                    ++decimal_significand
                }
                return DecimalFP32(decimal_significand, minus_k, isNegative)
            }

            // Normal interval case.
            twoFc = twoFc or (1.toUInt() shl (SIGNIFICAND_BITS + 1))
        } else {
            // Is the input a subnormal number?
            // Normal interval case.
            rawExponent = MIN_EXPONENT - SIGNIFICAND_BITS
        }

        //////////////////////////////////////////////////////////////////////
        // Step 1: Schubfach multiplier calculation.
        //////////////////////////////////////////////////////////////////////

        // Compute k and beta.
        val minus_k = rawExponent.floorLog10Pow2() - KAPPA
        val cache = format.cache[-minus_k - MIN_K]
        val beta = rawExponent + (-minus_k).floorLog2Pow10()

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
            format.compute_mul(carrier_uint((twoFc or 1u) shl beta), cache)

        //////////////////////////////////////////////////////////////////////
        // Step 2: Try larger divisor; remove trailing zeros if necessary.
        //////////////////////////////////////////////////////////////////////

        val BIG_DIVISOR = computePower10( KAPPA.toUInt() + 1u)
        val SMALL_DIVISOR = computePower10(KAPPA.toUInt())

        // Using an upper bound on zi, we might be able to optimize the division
        // better than the compiler; we are computing zi / big_divisor here.
        var decimal_significand = format.divide_by_pow10(z_result.integer_part, KAPPA.toUInt() + 1u)
        var r = carrier_uint(z_result.integer_part - BIG_DIVISOR * decimal_significand)

        do {
            if (r < deltai) {
                // Exclude the right endpoint if necessary.
                if (!(r != 0u || !z_result.is_integer || isEven)) {
                    --decimal_significand
                    r = BIG_DIVISOR
                    break
                }
            } else if (r > deltai) {
                break
            } else {
                // r == deltai; compare fractional parts.
                val x_result =
                    format.compute_mul_parity(twoFc - 1u, cache, beta)

                if (!(x_result.parity or (x_result.is_integer and isEven))) {
                    break
                }
            }

            val decimal_exponent = minus_k + KAPPA + 1
            return DecimalFP32.withoutTrailingZeros(decimal_significand, decimal_exponent, isNegative)
        } while (false)


        //////////////////////////////////////////////////////////////////////
        // Step 3: Find the significand with the smaller divisor.
        //////////////////////////////////////////////////////////////////////

        decimal_significand *= 10u

        // delta is equal to 10^(kappa + elog10(2) - floor(elog10(2))), so dist cannot
        // be larger than r.
        val dist = MutableUInt(carrier_uint(r - (deltai / 2u) + (SMALL_DIVISOR / 2u)))
        val approx_y_parity = ((dist.value xor (SMALL_DIVISOR shr 1)) and 1u) != 0u

        // Is dist divisible by 10^kappa?
        val divisible_by_small_divisor = check_divisibility_and_divide_by_pow10(dist)

        // Add dist / 10^kappa to the significand.
        decimal_significand += dist.value

        if (divisible_by_small_divisor) {
            // Check z^(f) >= epsilon^(f).
            // We have either yi == zi - epsiloni or yi == (zi - epsiloni) - 1,
            // where yi == zi - epsiloni if and only if z^(f) >= epsilon^(f).
            // Since there are only 2 possibilities, we only need to care about the
            // parity. Also, zi and r should have the same parity since the divisor
            // is an even number.
            val y_result = format.compute_mul_parity(twoFc, cache, beta)
            if (y_result.parity != approx_y_parity) {
                --decimal_significand
            }
            else {
                // If z^(f) >= epsilon^(f), we might have a tie
                // when z^(f) == epsilon^(f), or equivalently, when y is an integer.
                // When tie happens, always choose the even one.
                if ((decimal_significand % 2u != 0u) and y_result.is_integer) {
                    --decimal_significand
                }
            }
        }

        return DecimalFP32(decimal_significand, minus_k + KAPPA, isNegative)
    }

    fun to_chars_n(number: Float, buffer: StringBuilder): StringBuilder  {

        if (!number.isFinite()) {
            if (number.isNaN()) {
                buffer.append("NaN")
                return buffer
            } else {
                if (number.sign < 0) {
                    buffer.append('-')
                }
                buffer.append("Infinity")
                return buffer
            }
        }

        if (number.sign < 0) {
            buffer.append('-')
        }

        if (number == 0.0f) {
            buffer.append("0E0")
            return buffer
        }

        var (dec_sig, dec_exp, dec_sign) =
            to_decimal(number)

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

    data class DecimalFP32(val significand: UInt, val exponent: Int, val is_negative: Boolean) {

        fun toBigDecimal(): BigDecimal {
            val l = if(is_negative) -(significand.toLong()) else significand.toLong()
            return BigDecimal(l).exp10(exponent)
        }

        companion object {

            private fun UInt.rotr(r: Int): UInt {
                val r = r and 0x1f
                return (this shr r) or (this shl ((32 - r) and 0x1f))
            }


            fun withoutTrailingZeros(significand: UInt, exponent: Int, isNegative: Boolean): DecimalFP32 {
                var significand = significand
                var exponent = exponent

                // See https://github.com/jk-jeon/rtz_benchmark.
                // The idea of branchless search below is by reddit users r/pigeon768 and
                // r/TheoreticalDumbass.

                var r = (significand * (184254097u)).rotr(4)
                var b = r < (429497u)
                var s = if(b) 1u else 0u
                if (b) significand = r

                r = (significand * (42949673u)).rotr(2)
                b = r < (42949673u)
                s = s.shl(1) + if(b) 1u else 0u
                if (b) significand = r

                r = (significand * (1288490189u)).rotr(1)
                b = r < (429496730u)
                s = (s shl 1) + if (b) 1u else 0u
                if (b) significand = r

                exponent += s.toInt()

                return DecimalFP32(significand, exponent, isNegative)
            }
        }
    }

}

