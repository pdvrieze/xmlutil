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
 * Positive lookahead node.
 */
internal class XRPositiveLookAheadSet(children: List<XRAbstractSet>, fSet: XRFSet) : XRLookAroundSet(children, fSet) {

    /** Returns startIndex+shift, the next position to match */
    override fun tryToMatch(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        children.forEach {
            val shift = it.matches(startIndex, testString, matchResult)
            if (shift >= 0) {
                // PosLookaheadFset always returns true, position remains the same next.match() from;
                return next.matches(startIndex, testString, matchResult)
            }
        }
        return -1
    }

    override val name: String
        get() = "PositiveLookaheadJointSet"
}

/**
 * Negative look ahead node.
 */
internal class XRNegativeLookAheadSet(children: List<XRAbstractSet>, fSet: XRFSet) : XRLookAroundSet(children, fSet) {

    /** Returns startIndex+shift, the next position to match */
    override fun tryToMatch(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        children.forEach {
            if (it.matches(startIndex, testString, matchResult) >= 0) {
                return -1
            }
        }

        return next.matches(startIndex, testString, matchResult)
    }

    override val name: String
        get() = "NegativeLookaheadJointSet"
}
