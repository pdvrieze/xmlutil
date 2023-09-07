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

package io.github.pdvrieze.formats.xmlschema.regex.impl

/**
 * This class represents low surrogate character.
 *
 * Note that we can use high and low surrogate characters
 * that don't combine into supplementary code point.
 * See http://www.unicode.org/reports/tr18/#Supplementary_Characters
 */
internal class XRLowSurrogateCharSet(low: Char) : XRCharSet(low) {

    override fun accepts(startIndex: Int, testString: CharSequence): Int {
        val result = super.accepts(startIndex, testString)
        if (result < 0 || testString.isHighSurrogate(startIndex - 1)) {
            return -1
        }
        return result
    }

    private fun CharSequence.isHighSurrogate(index: Int, leftBound: Int = 0, rightBound: Int = length)
         = (index in leftBound until rightBound && this[index].isHighSurrogate())

    override fun find(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        var index = startIndex
        while (index < testString.length) {
            index = testString.indexOf(char, index, ignoreCase)
            if (index < 0) {
                return -1
            }
            if (!testString.isHighSurrogate(index - 1)
                &&  next.matches(index + charCount, testString, matchResult) >= 0) {
                return index
            }
            index++
        }
        return -1
    }

    override fun findBack(leftLimit: Int, rightLimit: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        var index = rightLimit
        while (index >= leftLimit) {
            index = testString.lastIndexOf(char, index, ignoreCase)
            if (index < 0) {
                return -1
            }
            if (!testString.isHighSurrogate(index - 1, leftLimit, rightLimit)
                && next.matches(index + charCount, testString, matchResult) >= 0) {
                return index
            }
            index--
        }
        return -1
    }

    override fun first(set: XRAbstractSet): Boolean {
        return when(set) {
            is XRLowSurrogateCharSet -> set.char == this.char
            is XRCharSet,
            is XRRangeSet,
            is XRSupplementaryRangeSet -> false
            else -> true
        }
    }

    override fun hasConsumed(matchResult: XRMatchResultImpl): Boolean = true
}

/**
 * This class represents high surrogate character.
 */
internal class XRHighSurrogateCharSet(high: Char) : XRCharSet(high) {

    override fun accepts(startIndex: Int, testString: CharSequence): Int {
        val result = super.accepts(startIndex, testString)
        if (result < 0 || testString.isLowSurrogate(startIndex + 1)) {
            return -1
        }
        return result
    }

    private fun CharSequence.isLowSurrogate(index: Int, leftBound: Int = 0, rightBound: Int = length)
            = (index in leftBound until rightBound && this[index].isLowSurrogate())

    // TODO: We have a similar code here, in LowSurrogateCharSet and in CharSet. Reuse it somehow.
    override fun find(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        var index = startIndex
        while (index < testString.length) {
            index = testString.indexOf(char, index, ignoreCase)
            if (index < 0) {
                return -1
            }
            // Remove params.
            if (!testString.isLowSurrogate(index + 1)
                &&  next.matches(index + charCount, testString, matchResult) >= 0) {
                return index
            }
            index++
        }
        return -1
    }

    override fun findBack(leftLimit: Int, rightLimit: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        var index = rightLimit
        while (index >= leftLimit) {
            index = testString.lastIndexOf(char, index, ignoreCase)
            if (index < 0) {
                return -1
            }
            if (!testString.isLowSurrogate(index + 1, leftLimit, rightLimit)
                && next.matches(index + charCount, testString, matchResult) >= 0) {
                return index
            }
            index--
        }
        return -1
    }

    override fun first(set: XRAbstractSet): Boolean {
        return when (set) {
            is XRHighSurrogateCharSet -> set.char == this.char
            is XRCharSet,
            is XRRangeSet,
            is XRSupplementaryRangeSet -> false
            else -> true
        }
    }

    override fun hasConsumed(matchResult: XRMatchResultImpl): Boolean = true
}

