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

import io.github.pdvrieze.xml.schematypes.RangeException

private fun handleNumberFormatException(s: CharSequence, e: NumberFormatException, rangeDigitCheckStart: Int): Nothing =
    when (s.length) {
        in rangeDigitCheckStart..Int.MAX_VALUE if (s.removePrefix("-").all { c -> c.isDigit() }) ->
            throw RangeException("Value $s is out of range")

        else -> throw e
    }

private fun handleDecNumberFormatException(s: CharSequence, e: NumberFormatException, rangeDigitCheckStart: Int): Nothing {
    when (s.length) {
        in rangeDigitCheckStart..Int.MAX_VALUE if (s.all { c -> c.isDigit() }) ->
            throw RangeException("Value $s is out of range")

        else -> {
            var seenDot = false
            val tooLong = s.removePrefix("-").all {
                (! seenDot && it == '.' && run { seenDot = true; true }) || it.isDigit()
            }
            if (tooLong) throw RangeException("Value $s is out of range")
            else throw e
        }
    }
}

/**
 * Helper function to convert a string to an int that throws a range exception if the value
 * is out of range
 */
internal fun CharSequence.xsToInt(): Int {
    val s = toString()
    try {
        return s.toInt()
    } catch (e: NumberFormatException) {
        handleNumberFormatException(s, e, 10)
    }
}

/**
 * Helper function to convert a string to an int that throws a range exception if the value
 * is out of range
 */
internal fun CharSequence.xsToUInt(): UInt {
    val s = toString()
    try {
        return s.toUInt()
    } catch (e: NumberFormatException) {
        handleNumberFormatException(s, e, 10)
    }
}

/**
 * Helper function to convert a string to an int that throws a range exception if the value
 * is out of range
 */
internal fun CharSequence.xsToLong(): Long {
    val s = toString()
    try {
        return s.toLong()
    } catch (e: NumberFormatException) {
        handleNumberFormatException(s, e, 20)
    }
}

/**
 * Helper function to convert a string to an int that throws a range exception if the value
 * is out of range
 */
internal fun CharSequence.xsToULong(): ULong {
    val s = toString()
    try {
        return s.toULong()
    } catch (e: NumberFormatException) {
        handleNumberFormatException(s, e, 20)
    }
}

/**
 * Helper function to convert a string to an int that throws a range exception if the value
 * is out of range
 */
internal fun CharSequence.xsToFloat(): Float {
    val s = toString()
    try {
        return s.toFloat()
    } catch (e: NumberFormatException) {
        handleDecNumberFormatException(s, e, 6)
    }
}

/**
 * Helper function to convert a string to an int that throws a range exception if the value
 * is out of range
 */
internal fun CharSequence.xsToDouble(): Double {
    val s = toString()
    try {
        return s.toDouble()
    } catch (e: NumberFormatException) {
        handleDecNumberFormatException(s, e, 14)
    }
}
