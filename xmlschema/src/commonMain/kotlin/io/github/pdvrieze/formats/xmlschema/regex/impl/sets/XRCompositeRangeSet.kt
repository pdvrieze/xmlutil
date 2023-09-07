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
 * This class is used to split the range that contains surrogate characters into two ranges:
 * the first consisting of these surrogate characters and the second consisting of all others characters
 * from the parent range. This class represents the parent range split in such a manner.
 */
internal class XRCompositeRangeSet(/* range without surrogates */ val withoutSurrogates: XRAbstractSet,
                                   /* range containing surrogates only */ val surrogates: XRSurrogateRangeSet) : XRSimpleSet() {

    override var next: XRAbstractSet = dummyNext
        get() = field
        set(next) {
            field = next
            surrogates.next = next
            withoutSurrogates.next = next
        }

    // This node consumes a single character starting at `startIndex`.
    // A single surrogate char is consumed only if it is unpaired, see [SurrogateRangeSet].
    // Thus, for a given [testString] and [startIndex] a fixed amount of chars are consumed.
    override val consumesFixedLength: Boolean
        get() = true

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        var result = withoutSurrogates.matches(startIndex, testString, matchResult)
        if (result < 0) {
            result = surrogates.matches(startIndex, testString, matchResult)
        }
        return result
    }

    override val name: String
            get() = "CompositeRangeSet: " + " <nonsurrogate> " + withoutSurrogates + " <surrogate> " + surrogates

    override fun hasConsumed(matchResult: XRMatchResultImpl): Boolean = true

    override fun first(set: XRAbstractSet): Boolean {
        return true
    }
}
