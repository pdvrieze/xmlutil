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
 * This class represent atomic group (?>X), once X matches, this match become unchangeable till the end of the match.
 */
open internal class XRAtomicJointSet(children: List<XRAbstractSet>, fSet: XRFSet) : XRNonCapturingJointSet(children, fSet) {

    /** Returns startIndex+shift, the next position to match */
    override fun matches(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        val start = matchResult.getConsumed(groupIndex)
        matchResult.setConsumed(groupIndex, startIndex)
        children.forEach {
            val shift = it.matches(startIndex, testString, matchResult)
            if (shift >= 0) {
                // AtomicFset always returns true, but saves the index to run this next.match() from;
                return next.matches((fSet as XRAtomicFSet).index, testString, matchResult)
            }
        }

        matchResult.setConsumed(groupIndex, start)
        return -1
    }

    override val name: String
        get() = "AtomicJointSet"

}
