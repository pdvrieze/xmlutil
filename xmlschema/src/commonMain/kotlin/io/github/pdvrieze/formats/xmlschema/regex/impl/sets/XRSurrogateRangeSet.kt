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
 * This class is a range that contains only surrogate characters.
 */
internal class XRSurrogateRangeSet(surrChars: XRAbstractCharClass) : XRRangeSet(surrChars) {

    override fun accepts(startIndex: Int, testString: CharSequence): Int {
        val result = super.accepts(startIndex, testString)
        when {
            result < 0 ||
            testString.isHighSurrogate(startIndex - 1) && testString.isLowSurrogate(startIndex) ||
            testString.isHighSurrogate(startIndex) && testString.isLowSurrogate(startIndex + 1) ->
                return -1
        }
        return result
    }

    private fun CharSequence.isHighSurrogate(index: Int, leftBound: Int = 0, rightBound: Int = length)
            = (index in leftBound until rightBound && this[index].isHighSurrogate())

    private fun CharSequence.isLowSurrogate(index: Int, leftBound: Int = 0, rightBound: Int = length)
            = (index in leftBound until rightBound && this[index].isLowSurrogate())

    override fun first(set: XRAbstractSet): Boolean {
        return when (set) {
            is XRSurrogateRangeSet -> true
            is XRCharSet,
            is XRRangeSet,
            is XRSupplementaryRangeSet -> false
            else -> true
        }
    }

    override fun hasConsumed(matchResult: XRMatchResultImpl): Boolean = true
}
