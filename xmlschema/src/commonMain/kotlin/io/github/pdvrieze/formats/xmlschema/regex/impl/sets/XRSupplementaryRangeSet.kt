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
 * Represents node accepting single character from the given char class.
 * This character can be supplementary (2 chars needed to represent) or from
 * basic multilingual pane (1 needed char to represent it).
 */
open internal class XRSupplementaryRangeSet(charClass: XRAbstractCharClass, val ignoreCase: Boolean = false): XRSimpleSet() {

    val chars = charClass.instance

    // This node can consume a single char or a supplementary code point consisting of two surrogate chars.
    // But this node can't match an unpaired surrogate char.
    // Thus, for a given [testString] and [startIndex] a fixed amount of chars are consumed.
    override val consumesFixedLength: Boolean
        get() = true

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        val rightBound = testString.length
        if (startIndex >= rightBound) {
            return -1
        }

        var index = startIndex

        val high = testString[index++]
        if (contains(high)) {
            val result = next.matches(index, testString, matchResult)
            if (result >= 0) return result
        }

        if (index < rightBound) {
            val low = testString[index++]
            if (Char.isSurrogatePair(high, low) && contains(Char.toCodePoint(high, low))) {
                return next.matches(index, testString, matchResult)
            }
        }

        return -1
    }

    fun contains(char: Char): Boolean {
        if (ignoreCase) {
            return chars.contains(char.uppercaseChar()) || chars.contains(char.lowercaseChar())
        } else {
            return chars.contains(char)
        }
    }

    fun contains(char: Int): Boolean {
        return chars.contains(char)
    }

    override val name: String
        get() = "range:" + (if (chars.alt) "^ " else " ") + chars.toString()


    override fun first(set: XRAbstractSet): Boolean {
        @Suppress("DEPRECATION")
        return when(set) {
            is XRCharSet -> XRAbstractCharClass.intersects(chars, set.char.toInt())
            is XRSupplementaryRangeSet -> XRAbstractCharClass.intersects(chars, set.chars)
            is XRRangeSet -> XRAbstractCharClass.intersects(chars, set.chars)
            else -> true
        }
    }

    override fun hasConsumed(matchResult: XRMatchResultImpl): Boolean = true
}
