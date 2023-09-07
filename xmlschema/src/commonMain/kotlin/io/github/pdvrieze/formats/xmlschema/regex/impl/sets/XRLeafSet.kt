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
 * Base class for nodes representing leaf tokens of the RE, those who consumes fixed number of characters.
 */
internal abstract class XRLeafSet : XRSimpleSet(XRAbstractSet.TYPE_LEAF) {

    open val charCount = 1

    /** Returns "shift", the number of accepted chars. Commonly internal function, but called by quantifiers. */
    abstract fun accepts(startIndex: Int, testString: CharSequence): Int

    override val consumesFixedLength: Boolean
        get() = true

    /**
     * Checks if we can enter this state and pass the control to the next one.
     * Return positive value if match succeeds, negative otherwise.
     */
    override fun matches(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        if (startIndex + charCount > testString.length) {
            return -1
        }

        val shift = accepts(startIndex, testString) // TODO: may be move the check above in accept function.
        if (shift < 0) {
            return -1
        }

        return next.matches(startIndex + shift, testString, matchResult)
    }

    override fun hasConsumed(matchResult: XRMatchResultImpl): Boolean {
        return true
    }
}
