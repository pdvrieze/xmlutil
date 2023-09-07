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

import kotlin.RuntimeException

/**
 * Generalized greedy quantifier node over the leaf nodes.
 *  - a{n,m};
 *  - a* == a{0, <inf>};
 *  - a? == a{0, 1};
 *  - a+ == a{1, <inf>};
 */
open internal class XRLeafQuantifierSet(var quantifier: XRQuantifier,
                                        innerSet: XRLeafSet,
                                        next: XRAbstractSet,
                                        type: Int
) : XRQuantifierSet(innerSet, next, type) {

    init {
        innerSet.next = XRFSet.possessiveFSet
    }

    val leaf: XRLeafSet get() = super.innerSet as XRLeafSet
    val min: Int get() = quantifier.min
    val max: Int get() = quantifier.max

    override val consumesFixedLength: Boolean
        get() = (min == max)

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        var index = startIndex
        var occurrences = 0

        // Process first <min> occurrences of the sequence being looked for.
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

        // Process occurrences between min and max.
        while  ((max == XRQuantifier.INF || occurrences < max) && index + leaf.charCount <= testString.length) {
            val shift = leaf.accepts(index, testString)
            if (shift < 1) {
                break
            }
            index += shift
            occurrences++
        }

        // Roll back if the next node does't match the remaining string.
        while (occurrences >= min) {
            val shift = next.matches(index, testString, matchResult)
            if (shift >= 0) {
                return shift
            }
            index -= leaf.charCount
            occurrences--
        }
        return -1
    }

    override val name: String
        get() = quantifier.toString()

    override var innerSet: XRAbstractSet
        get() = super.innerSet
        set(innerSet) {
            if (innerSet !is XRLeafSet)
                throw RuntimeException("Internal Error")
            super.innerSet = innerSet
        }
}
