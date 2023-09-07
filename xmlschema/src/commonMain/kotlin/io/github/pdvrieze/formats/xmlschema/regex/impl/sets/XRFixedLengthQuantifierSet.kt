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
 * Greedy quantifier over constructions that consume a fixed number of characters.
 */
open internal class XRFixedLengthQuantifierSet(
    val quantifier: XRQuantifier,
    innerSet: XRAbstractSet,
    next: XRAbstractSet,
    type: Int
) : XRQuantifierSet(innerSet, next, type) {

    init {
        require(innerSet.consumesFixedLength)
        innerSet.next = XRFSet.possessiveFSet
    }

    val min: Int get() = quantifier.min
    val max: Int get() = quantifier.max

    override val consumesFixedLength: Boolean
        get() = (min == max)

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        var index = startIndex
        val matches = mutableListOf<Int>()

        // Process occurrences between 0 and max.
        while (max == XRQuantifier.INF || matches.size < max) {
            val nextIndex = innerSet.matches(index, testString, matchResult)
            if (nextIndex < 0) {
                if (matches.size < min) {
                    return -1
                } else {
                    break
                }
            }
            matches.add(index)
            index = nextIndex
        }

        // Roll back if the next node doesn't match the remaining string.
        while (matches.size > min) {
            val nextIndex = next.matches(index, testString, matchResult)
            if (nextIndex >= 0) {
                return nextIndex
            }
            index = matches.removeLast()
        }

        return next.matches(index, testString, matchResult)
    }

    override fun toString(): String {
        return "${this::class}(innerSet = $innerSet, next = $next)"
    }
}
