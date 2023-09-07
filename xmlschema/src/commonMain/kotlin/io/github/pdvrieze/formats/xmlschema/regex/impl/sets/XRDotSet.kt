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
 * Node accepting any character except line terminators.
 */
internal class XRDotSet(val lt: XRAbstractLineTerminator, val matchLineTerminator: Boolean)
    : XRSimpleSet(XRAbstractSet.TYPE_DOTSET) {

    // This node consumes any character. If the character is supplementary, this node consumes both surrogate chars representing it.
    // Otherwise, consumes a single char.
    // Thus, for a given [testString] and [startIndex] a fixed amount of chars are consumed.
    override val consumesFixedLength: Boolean
        get() = true

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        val rightBound = testString.length
        if (startIndex >= rightBound) {
            return -1
        }

        val high = testString[startIndex]
        if (high.isHighSurrogate() && startIndex + 2 <= rightBound) {
            val low = testString[startIndex + 1]
            if (Char.isSurrogatePair(high, low)) {
                if (!matchLineTerminator && lt.isLineTerminator(Char.toCodePoint(high, low))) {
                    return -1
                } else {
                    return next.matches(startIndex + 2, testString, matchResult)
                }
            }
        }
        if (!matchLineTerminator && lt.isLineTerminator(high)) {
            return -1
        } else {
            return next.matches(startIndex + 1, testString, matchResult)
        }
    }

    override fun hasConsumed(matchResult: XRMatchResultImpl): Boolean = true
    override val name: String
        get() = "."
}
