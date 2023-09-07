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
 * Optimized greedy quantifier node ('*') for the case where there is no intersection with
 * next node and normal quantifiers could be treated as greedy and possessive.
 */
internal class XRUnifiedQuantifierSet(quant: XRLeafQuantifierSet) : XRLeafQuantifierSet(XRQuantifier.starQuantifier, quant.leaf, quant.next, quant.type) {

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        var index = startIndex
        while (index + leaf.charCount <= testString.length && leaf.accepts(index, testString) > 0) {
            index += leaf.charCount
        }

        return next.matches(index, testString, matchResult)
    }

    override fun find(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        var startSearch = next.find(startIndex, testString, matchResult)
        if (startSearch < 0)
            return -1

        var result = startSearch
        var index = startSearch - leaf.charCount
        while (index >= startIndex && leaf.accepts(index, testString) > 0) {
            result = index
            index -= leaf.charCount
        }
        return result
    }

    init {
        innerSet.next = this
    }
}
