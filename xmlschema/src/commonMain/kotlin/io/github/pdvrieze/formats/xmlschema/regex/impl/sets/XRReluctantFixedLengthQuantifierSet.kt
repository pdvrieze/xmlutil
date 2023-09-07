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
 * Reluctant version of the fixed length quantifier set.
 */
internal class XRReluctantFixedLengthQuantifierSet(
    quantifier: XRQuantifier,
    innerSet: XRAbstractSet,
    next: XRAbstractSet,
    type: Int
) : XRFixedLengthQuantifierSet(quantifier, innerSet, next, type) {

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        var index = startIndex

        // Process first min occurrences.
        repeat(min) {
            val nextIndex = innerSet.matches(index, testString, matchResult)
            if (nextIndex < 0) {
                return -1
            }
            index = nextIndex
        }

        // Process occurrences between min and max.
        var occurrences = min
        while (max == XRQuantifier.INF || occurrences < max) {
            var nextIndex = next.matches(index, testString, matchResult)
            if (nextIndex < 0) {
                nextIndex = innerSet.matches(index, testString, matchResult)
                if (nextIndex < 0) {
                    return -1
                }
                index = nextIndex
                occurrences += 1
            } else {
                return nextIndex
            }
        }

        return next.matches(index, testString, matchResult)
    }
}
