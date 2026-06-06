/*
 * Copyright (c) 2026.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package nl.adaptivity.xmlutil.dom2.impl

import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom.PlatformNodeList
import nl.adaptivity.xmlutil.dom.iterator
import nl.adaptivity.xmlutil.dom2.Node

@ExperimentalXmlUtilApi
public interface AbstractNodeStorage <out N : IAbstractNode<N, P>, out P : IAbstractParentNode<N, P>> {

    public fun getNodeList(): AbstractNodeList<N, P>

    public fun iterator(): Iterator<N>

    @Suppress("UNCHECKED_CAST")
    public fun getFirstChild(): N? = getNodeList().asSequence().firstOrNull() as N?

    @Suppress("UNCHECKED_CAST")
    public fun getLastChild(): N? = getNodeList().asSequence().lastOrNull() as N?

    @XmlUtilInternal
    public fun checkTypeAndOwner(node: PlatformNode): N

    public fun getSiblingBefore(ref: Node): N? {
        val it = iterator()
        if (!it.hasNext()) return null
        var cur: N? = null
        do {
            val next = it.next()
            if (next == ref) return cur
            cur = next
        } while (it.hasNext())

        return null
    }

    public fun getSiblingAfter(ref: Node): N? {
        val it = iterator()
        while (it.hasNext()) {
            val cur = it.next()
            if (cur == ref) return if (it.hasNext()) it.next() else null
        }
        return null
    }

    public fun isEqualNodes(otherList: PlatformNodeList): Boolean {
        val left = iterator()
        val right = otherList.iterator()
        while (left.hasNext() && right.hasNext()) {
            if (! left.next().isEqualNode(right.next())) return false
        }
        return !left.hasNext() && !right.hasNext()
    }

}

