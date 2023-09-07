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

import io.github.pdvrieze.formats.xmlschema.regex.XRegex
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert

/**
 * The node which marks end of the particular group.
 */
open internal class XRFSet(val groupIndex: Int) : XRSimpleSet() {

    var isBackReferenced = false

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        val oldEnd = matchResult.getEnd(groupIndex)
        matchResult.setEnd(groupIndex, startIndex)
        val shift = next.matches(startIndex, testString, matchResult)
        if (shift < 0) {
            matchResult.setEnd(groupIndex, oldEnd)
        }
        return shift
    }

    override fun hasConsumed(matchResult: XRMatchResultImpl): Boolean = false
    override val name: String
            get() = "fSet"

    override fun processSecondPass(): XRFSet {
        val result = super.processSecondPass()
        @OptIn(XmlUtilInternal::class)
        assert(result == this)
        return this
    }

    /**
     * Marks the end of the particular group and not take into account possible
     * kickbacks (required for atomic groups, for instance)
     */
    internal class PossessiveFSet : XRSimpleSet() {
        override fun matches(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
            return startIndex
        }

        override fun hasConsumed(matchResult: XRMatchResultImpl): Boolean {
            return false
        }

        override val name: String
                get() = "possessiveFSet"
    }

    companion object {
        val possessiveFSet = PossessiveFSet()
    }
}

/**
 * Special construction which marks end of pattern.
 */
internal class XRFinalSet : XRFSet(0) {

    override fun matches(startIndex: Int, testString: CharSequence,
                         matchResult: XRMatchResultImpl): Int {
        if (matchResult.mode == XRegex.Mode.FIND || startIndex == testString.length) {
            matchResult.setEnd(0, startIndex)
            return startIndex
        }
        return -1
    }

    override val name: String
        get() = "FinalSet"
}

/**
 * Non-capturing group closing node.
 */
internal class XRNonCapFSet(groupIndex: Int) : XRFSet(groupIndex) {

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        matchResult.setConsumed(groupIndex, startIndex - matchResult.getConsumed(groupIndex))
        return next.matches(startIndex, testString, matchResult)
    }

    override val name: String
        get() = "NonCapFSet"

    override fun hasConsumed(matchResult: XRMatchResultImpl): Boolean {
        return false
    }
}

/**
 * LookAhead FSet, always returns true
 */
internal class XRAheadFSet : XRFSet(-1) {

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        return startIndex
    }

    override val name: String
        get() = "AheadFSet"
}

/**
 * FSet for lookbehind constructs. Checks if string index saved by corresponding
 * jointSet in "consumers" equals to current index and return current string
 * index, return -1 otherwise.

 */
internal class XRBehindFSet(groupIndex: Int) : XRFSet(groupIndex) {

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        val rightBound = matchResult.getConsumed(groupIndex)
        return if (rightBound == startIndex) startIndex else -1
    }

    override val name: String
        get() = "BehindFSet"
}

/**
 * Represents an end of an atomic group.
 */
internal class XRAtomicFSet(groupIndex: Int) : XRFSet(groupIndex) {

    var index: Int = 0

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        matchResult.setConsumed(groupIndex, startIndex - matchResult.getConsumed(groupIndex))
        index = startIndex
        return startIndex
    }

    override val name: String
        get() = "AtomicFSet"

    override fun hasConsumed(matchResult: XRMatchResultImpl): Boolean {
        return false
    }
}
