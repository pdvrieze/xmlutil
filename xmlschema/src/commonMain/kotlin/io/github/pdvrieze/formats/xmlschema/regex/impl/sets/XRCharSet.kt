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
 * Represents node accepting single character.
 */
open internal class XRCharSet(char: Char, val ignoreCase: Boolean = false) : XRLeafSet() {

    // We use only low case characters when working in case insensitive mode.
    val char: Char = if (ignoreCase) char.lowercaseChar() else char

    // Overrides =======================================================================================================

    override fun accepts(startIndex: Int, testString: CharSequence): Int {
        if (ignoreCase) {
            return if (this.char == testString[startIndex].lowercaseChar()) 1 else -1
        } else {
            return if (this.char == testString[startIndex]) 1 else -1
        }
    }

    override fun find(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        var index = startIndex
        while (index < testString.length) {
            index = testString.indexOf(char, index, ignoreCase)
            if (index < 0) {
                return -1
            }
            if (next.matches(index + charCount, testString, matchResult) >= 0) {
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
            if (next.matches(index + charCount, testString, matchResult) >= 0) {
                return index
            }
            index--
        }
        return -1
    }

    override val name: String
            get()= char.toString()

    override fun first(set: XRAbstractSet): Boolean {
        if (ignoreCase) {
            return super.first(set)
        }
        return when (set) {
            is XRCharSet -> set.char == char
            is XRRangeSet -> set.accepts(0, char.toString()) > 0
            is XRSupplementaryRangeSet -> set.contains(char)
            else -> true
        }
    }
}
