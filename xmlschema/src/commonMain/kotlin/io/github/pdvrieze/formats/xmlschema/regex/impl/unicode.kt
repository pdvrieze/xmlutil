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
@file:OptIn(ExperimentalNativeApi::class) // for Companion extension

package io.github.pdvrieze.formats.xmlschema.regex.impl

import kotlin.experimental.ExperimentalNativeApi

internal fun Char.Companion.isSupplementaryCodePoint(codepoint: Int): Boolean =
    codepoint in MIN_SUPPLEMENTARY_CODE_POINT..MAX_CODE_POINT

/**
 * Converts the codepoint specified to a char array. If the codepoint is not supplementary, the method will
 * return an array with one element otherwise it will return an array A with a high surrogate in A[0] and
 * a low surrogate in A[1].
 *
 *
 * Note that this function is unstable.
 * In the future it could be deprecated in favour of an overload that would accept a `CodePoint` type.
 */
// TODO: Consider removing from public API
@Suppress("DEPRECATION")
internal fun Char.Companion.toChars(codePoint: Int): CharArray =
    when {
        codePoint in 0 until MIN_SUPPLEMENTARY_CODE_POINT -> charArrayOf(codePoint.toChar())
        codePoint in MIN_SUPPLEMENTARY_CODE_POINT..MAX_CODE_POINT -> {
            val low = ((codePoint - 0x10000) and 0x3FF) + MIN_LOW_SURROGATE.toInt()
            val high = (((codePoint - 0x10000) ushr 10) and 0x3FF) + MIN_HIGH_SURROGATE.toInt()
            charArrayOf(high.toChar(), low.toChar())
        }
        else -> throw IllegalArgumentException()
    }

fun Char.Companion.toCodePoint(highSurrogate: Char, lowSurrogate: Char): Int {
    return ((highSurrogate.code - 0xd800 shl 10)
            + (lowSurrogate.code - 0xdc00)
            + 0x10000)
}

fun Char.Companion.isSurrogatePair(high: Char, low: Char): Boolean {
    return high.isHighSurrogate() && low.isLowSurrogate()
}

/**
 * The minimum value of a supplementary code point, `\u0x10000`.
 *
 * Note that this constant is experimental.
 * In the future it could be deprecated in favour of another constant of a `CodePoint` type.
 */
public const val MIN_SUPPLEMENTARY_CODE_POINT: Int = 0x10000
public const val MAX_CODE_POINT = 0X10FFFF
