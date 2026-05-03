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
import nl.adaptivity.xmlutil.dom2.DocumentFragment
import nl.adaptivity.xmlutil.dom2.Node

@XmlUtilInternal
public interface AbstractNodeStorage<out N : IAbstractNode<N, P>, out P : IAbstractParentNode<N, P>> {

    public fun getNodeList(): AbstractNodeList<N, P>

    @Suppress("UNCHECKED_CAST")
    public fun getFirstChild(): N? = getNodeList().asSequence().firstOrNull() as N?

    @Suppress("UNCHECKED_CAST")
    public fun getLastChild(): N? = getNodeList().asSequence().lastOrNull() as N?

    @XmlUtilInternal
    public fun checkTypeAndOwner(parent: @UnsafeVariance P, node: PlatformNode): N

    public fun appendChild(parent: @UnsafeVariance P, node: PlatformNode): N = checkTypeAndOwner(parent, node).also {
        when (it) {
            is DocumentFragment -> for (child in it.getChildNodes()) {
                val _ = appendChild(parent, child)
            }
            else -> appendChild(parent, it)
        }
    }
    public fun appendChild(parent: @UnsafeVariance P, node: @UnsafeVariance N)

    public fun removeChild(parent: @UnsafeVariance P, node: PlatformNode): N = removeChild(parent, checkTypeAndOwner(parent, node))
    public fun removeChild(parent: @UnsafeVariance P, node: @UnsafeVariance N): N

    public fun replaceChild(parent: @UnsafeVariance P, newChild: PlatformNode, oldChild: PlatformNode): N {
        return replaceChild(parent, checkTypeAndOwner(parent, newChild), checkTypeAndOwner(parent, oldChild))
    }

    public fun replaceChild(parent: @UnsafeVariance P, newChild: @UnsafeVariance N, oldChild: @UnsafeVariance N): N

    public fun getSiblingBefore(ref: Node): N? {
        var cur: N? = getFirstChild() ?: return null
        while (cur != null) {
            @Suppress("UNCHECKED_CAST")
            val n = cur.getNextSibling() as N?

            if (n == ref) return cur
            cur = n
        }
        return null
    }

    public fun getSiblingAfter(ref: Node): N? {
        var cur: N? = getFirstChild() ?: return null
        @Suppress("UNCHECKED_CAST")
        while (cur != null) {
            if (cur == ref) return cur.getNextSibling() as N?
            cur = cur.getNextSibling() as N?
        }
        return null
    }
}

@ExperimentalXmlUtilApi
public class LinearNodeStorage<N : IAbstractNode<N, P>, P : IAbstractParentNode<N, P>>(
    private val adapter: Adapter<N, P>
): AbstractNodeStorage<N, P>, AbstractNodeList<N, P> {
    private val elements = mutableListOf<N>()

    @XmlUtilInternal
    override fun checkTypeAndOwner(parent: P, node: PlatformNode): N = adapter.checkType(parent, node)

    override fun getNodeList(): AbstractNodeList<N, P> = this

    override fun appendChild(parent: @UnsafeVariance P, node: N) {
        elements.add(node)
        adapter.updateChildPos(parent, node, elements.size-1)
    }

    override fun removeChild(parent: P, node: N): N {
        if (!elements.remove(node)) throw IllegalArgumentException("Node not in list")
        adapter.updateChildPos(parent, node, -1)
        return node
    }

    override fun replaceChild(parent: P, newChild: N, oldChild: N): N {
        val idx = elements.indexOf(oldChild)
        if (idx < 0) throw IllegalArgumentException("Node not in list")
        adapter.updateChildPos(parent,oldChild, -1)
        elements[idx] = newChild
        adapter.updateChildPos(parent,newChild, idx)
        return oldChild
    }

    override fun iterator(): Iterator<N> = elements.iterator()

    public interface Adapter<N: IAbstractNode<N, P>, P: IAbstractParentNode<N, P>> {
        public fun checkType(parent: P, node: PlatformNode): N
        public fun updateChildPos(parent: P, node: N, newPos: Int) {}
    }
}
