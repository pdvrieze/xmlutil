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
 * Represents group, which is alternation of other subexpression.
 * One should think about "group" in this model as JointSet opening group and corresponding FSet closing group.
 */
open internal class XRJointSet(children: List<XRAbstractSet>, fSet: XRFSet) : XRAbstractSet() {

    protected var children: MutableList<XRAbstractSet> = mutableListOf<XRAbstractSet>().apply { addAll(children) }

    var fSet: XRFSet = fSet
        protected set

    var groupIndex: Int = fSet.groupIndex
        protected set

    /**
     * Returns startIndex+shift, the next position to match
     */
    override fun matches(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        if (children.isEmpty()) {
            return -1
        }
        val oldStart = matchResult.getStart(groupIndex)
        matchResult.setStart(groupIndex, startIndex)
        children.forEach {
            val shift = it.matches(startIndex, testString, matchResult)
            if (shift >= 0) {
                return shift
            }
        }
        matchResult.setStart(groupIndex, oldStart)
        return -1
    }

    override var next: XRAbstractSet
        get() = fSet.next
        set(next) {
            fSet.next = next
        }

    override val name: String
            get() = "JointSet"
    override fun first(set: XRAbstractSet): Boolean = children.any { it.first(set) }

    override fun hasConsumed(matchResult: XRMatchResultImpl): Boolean {
        return !(matchResult.getEnd(groupIndex) >= 0 && matchResult.getStart(groupIndex) == matchResult.getEnd(groupIndex))
    }

    @OptIn(XmlUtilInternal::class)
    override fun processSecondPassInternal(): XRAbstractSet {
        val fSet = this.fSet
        if (!fSet.secondPassVisited) {
            val newFSet = fSet.processSecondPass()
            assert(newFSet == fSet)
        }

        for (i in children.indices) {
            val child = children[i]
            children[i] = if (!child.secondPassVisited) child.processSecondPass() else child
        }
        return super.processSecondPassInternal()
    }
}
