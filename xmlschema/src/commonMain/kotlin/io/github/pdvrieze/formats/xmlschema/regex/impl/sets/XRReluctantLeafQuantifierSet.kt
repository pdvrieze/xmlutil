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
 * Reluctant quantifier node over a leaf node.
 *  - a{n,m}?;
 *  - a*? == a{0, <inf>}?;
 *  - a?? == a{0, 1}?;
 *  - a+? == a{1, <inf>}?;
 */
internal class XRReluctantLeafQuantifierSet(
    quant: XRQuantifier,
    innerSet: XRLeafSet,
    next: XRAbstractSet,
    type: Int
) : XRLeafQuantifierSet(quant, innerSet, next, type) {

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        var index = startIndex
        var occurrences = 0

        while (occurrences < min) {
            if (index + leaf.charCount > testString.length) {
                return -1
            }

            val shift = leaf.accepts(index, testString)
            if (shift < 1) {
                return -1
            }
            index += shift
            occurrences++
        }

        do {
            var shift = next.matches(index, testString, matchResult)
            if (shift >= 0) {
                return shift
            }

            if (index + leaf.charCount <= testString.length) {
                shift = leaf.accepts(index, testString)
                index += shift
                occurrences++
            }

        } while (shift >= 1 && (max == XRQuantifier.INF || occurrences <= max))

        return -1
    }
}
