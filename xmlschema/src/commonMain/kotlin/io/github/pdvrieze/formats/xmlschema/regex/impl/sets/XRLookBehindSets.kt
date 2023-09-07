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
 * Positive lookbehind node.
 */
internal class XRPositiveLookBehindSet(children: List<XRAbstractSet>, fSet: XRFSet) : XRLookAroundSet(children, fSet) {

    /** Returns startIndex+shift, the next position to match */
    override fun tryToMatch(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        matchResult.setConsumed(groupIndex, startIndex)
        children.forEach {
            if (it.findBack(0, startIndex, testString, matchResult) >= 0) {
                matchResult.setConsumed(groupIndex, -1)
                return next.matches(startIndex, testString, matchResult)
            }
        }

         return -1
    }

    override val name: String
        get() = "PositiveBehindJointSet"
}

/**
 * Negative look behind node.
 */
internal class XRNegativeLookBehindSet(children: List<XRAbstractSet>, fSet: XRFSet) : XRLookAroundSet(children, fSet) {

    /** Returns startIndex+shift, the next position to match */
    override fun tryToMatch(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        matchResult.setConsumed(groupIndex, startIndex)

        children.forEach {
            val shift = it.findBack(0, startIndex, testString, matchResult)
            if (shift >= 0) {
                return -1
            }
        }

        return next.matches(startIndex, testString, matchResult)
    }

    override val name: String
        get() = "NegativeBehindJointSet"
}
