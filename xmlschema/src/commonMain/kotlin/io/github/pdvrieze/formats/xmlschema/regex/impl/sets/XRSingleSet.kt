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

import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert

/**
 * Group node over subexpression without alternations.
 */
open internal class XRSingleSet(var kid: XRAbstractSet, fSet: XRFSet) : XRJointSet(listOf(), fSet) {

    var backReferencedSet: BackReferencedSingleSet? = null

    // Overrides (API) =================================================================================================

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        val start = matchResult.getStart(groupIndex)
        matchResult.setStart(groupIndex, startIndex)
        val shift = kid.matches(startIndex, testString, matchResult)
        if (shift >= 0) {
            return shift
        }
        matchResult.setStart(groupIndex, start)
        return -1
    }

    override fun find(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        val res = kid.find(startIndex, testString, matchResult)
        if (res >= 0)
            matchResult.setStart(groupIndex, res)
        return res
    }

    override fun findBack(leftLimit: Int, rightLimit: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        val res = kid.findBack(leftLimit, rightLimit, testString, matchResult)
        if (res >= 0)
            matchResult.setStart(groupIndex, res)
        return res
    }

    override fun first(set: XRAbstractSet): Boolean = kid.first(set)

    // Second pass processing ==========================================================================================

    override fun processBackRefReplacement(): XRJointSet? {
        /**
         * We will store a reference to created BackReferencedSingleSet
         * in [backReferencedSet] field. This is needed to process replacement
         * of sets correctly since sometimes we cannot renew all references to
         * detachable set in the current point of traverse. See
         * QuantifierSet and AbstractSet processSecondPass() methods for
         * more details.
         */
        val result = BackReferencedSingleSet(this)
        backReferencedSet = result
        return result
    }

    override fun processSecondPassInternal(): XRAbstractSet {
        fSet = fSet.processSecondPass()
        kid = kid.processSecondPass()
        return processBackRefReplacement() ?: this
    }

    /**
     * This method is used for traversing nodes after the first stage of compilation.
     */
    @OptIn(XmlUtilInternal::class)
    override fun processSecondPass(): XRAbstractSet {
        if (secondPassVisited) {
            if (fSet.isBackReferenced) {
                assert(backReferencedSet != null) // secondPassVisited
                return backReferencedSet!!
            }
        }
        secondPassVisited = true
        return processSecondPassInternal()
    }

    // Backreferenced version of the class =============================================================================

    /**
     * Group node over subexpression without alternations.
     * This node is used if current group is referenced via a backreference.
     */
    internal class BackReferencedSingleSet(node: XRSingleSet) : XRSingleSet(node.kid, node.fSet) {

        /*
         * This class is needed only for overwriting find() and findBack() methods of SingleSet class, which is being
         * back referenced. The following example explains the need for such substitution:
         *
         * Let's consider the pattern ".*(.)\\1".
         * Leading .* works as follows: finds line terminator and runs findBack from that point.
         * `findBack` method in its turn (in contrast to matches) sets group boundaries on the back trace.
         * Thus at the point we try to match back reference(\\1) groups are not yet set.
         *
         * To fix this problem we replace backreferenced groups with instances of this class,
         * which will use matches instead of find; this will affect performance, but ensure correctness of the match.
         */
        override fun find(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
            for (index in startIndex..testString.length) {
                val oldStart = matchResult.getStart(groupIndex)
                matchResult.setStart(groupIndex, index)

                val res = kid.matches(index, testString, matchResult)
                if (res >= 0) {
                    return index
                } else {
                    matchResult.setStart(groupIndex, oldStart)
                }
            }
            return -1
        }

        override fun findBack(leftLimit: Int, rightLimit: Int,
                              testString: CharSequence, matchResult: XRMatchResultImpl): Int {
            for (index in rightLimit downTo leftLimit) {
                val oldStart = matchResult.getStart(groupIndex)
                matchResult.setStart(groupIndex, index)

                val res = kid.matches(index, testString, matchResult)
                if (res >= 0) {
                    return index
                } else {
                    matchResult.setStart(groupIndex, oldStart)
                }
            }
            return -1
        }

        override fun processBackRefReplacement(): XRJointSet? = null
    }
}

