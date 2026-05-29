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
import nl.adaptivity.xmlutil.dom.DOMException
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom.PlatformNodeList
import nl.adaptivity.xmlutil.dom.length
import nl.adaptivity.xmlutil.dom2.length

@ExperimentalXmlUtilApi
public open class LinearNodeStorage<N : IAbstractNode<N, P>, P : IAbstractParentNode<N, P>>(
    internal val adapter: Adapter<N, P>
): MutableAbstractNodeStorage<N, P>, AbstractNodeList<N, P> {
    private val elements = mutableListOf<N>()

    @XmlUtilInternal
    override fun checkTypeAndOwner(node: PlatformNode): N = adapter.checkTypeAndOwner(node)

    final override fun getNodeList(): AbstractNodeList<N, P> = this

    override fun appendChild(parent: @UnsafeVariance P, node: N) {
        if (isAncestor(parent, node)) throw DOMException.hierarchyRequestErr("Cannot insert node into itself")
        if (node is AbstractDocumentFragment<*, *>) {
            for (c in node.getChildNodes()) appendChild(parent, c)
        } else {
            elements.add(node)
            adapter.setParentAndUpdateChildPos(parent, node, elements.size - 1)
        }
    }

    override fun removeChild(parent: P, node: N): N {
        if (!elements.remove(node)) throw DOMException.notFoundErr("Node not in list")
        adapter.setParentAndUpdateChildPos(parent, node, -1)
        return node
    }

    private fun elementIndex(parent: P, node: N): Int {
        if (parent != node.getParentNode()) throw DOMException.notFoundErr("Node not in list")
        val hint = adapter.getChildPosHint(node)
        return when {
            hint >= 0 -> hint

            else -> elements.indexOf(node).also {
                if (it < 0) throw DOMException.notFoundErr("Node not in list")
            }
        }
    }

    override fun replaceChild(parent: P, newChild: N, oldChild: N): N {
        if (isAncestor(parent, newChild)) throw DOMException.hierarchyRequestErr("Cannot insert node into itself")
        val idx = elementIndex(parent, oldChild)

        adapter.setParentAndUpdateChildPos(parent,oldChild, -1)

        if (newChild !is AbstractDocumentFragment<*, *>) {
            elements[idx] = newChild
            adapter.setParentAndUpdateChildPos(parent,newChild, idx)
        } else {
            @Suppress("UNCHECKED_CAST")
            val childrenToAdd: List<N> = newChild.getChildNodes().toList() as List<N>

            elements.addAll(idx, childrenToAdd)
            for (i in idx until elements.size) {
                adapter.setParentAndUpdateChildPos(parent, elements[i], i)
            }
        }

        return oldChild
    }

    override fun clear() {
        // defensive copy to handle update checking the parent.
        val oldElements = elements.toList()
        elements.clear()
        for (e in oldElements) adapter.setParentAndUpdateChildPos(null, e, -1)
    }

    private fun isAncestor(parent: P?, node: N): Boolean {
        var cur: P? = parent
        while (cur != null) {
            if (cur == node) return true
            @Suppress("UNCHECKED_CAST")
            cur = cur.getParentNode()
        }
        return false
    }

    override fun insertBefore(newChild: N, refChild: N) {
        if (newChild == refChild || isAncestor(refChild.getParentNode(), newChild)) throw DOMException.hierarchyRequestErr("Cannot insert node into itself")
        val parent = refChild.getParentNode() as P
        val idx = elementIndex(parent, refChild)

        if (newChild !is AbstractDocumentFragment<*, *>) {
            elements.add(idx, newChild)
        } else {
            @Suppress("UNCHECKED_CAST")
            val childrenToAdd: List<N> = newChild.getChildNodes().toList() as List<N>

            elements.addAll(idx, childrenToAdd)
        }
        for (i in idx until elements.size) {
            adapter.setParentAndUpdateChildPos(parent, elements[i], i)
        }
    }

    override fun iterator(): MutableIterator<N> = NodeIterator()

    override fun isEqualNodes(otherList: PlatformNodeList): Boolean {
        return length == otherList.length && super.isEqualNodes(otherList)
    }

    private inner class NodeIterator() : MutableIterator<N> {
        var nextPos: Int = 0

        override fun remove() {
            check(nextPos > 0) { "Iterator not started yet" }
            val parent = elements[nextPos - 1].getParentNode() as P
            adapter.setParentAndUpdateChildPos(null, elements[nextPos - 1], -1)
            elements.removeAt(nextPos - 1)
            for (i in (nextPos - 1) until elements.size) {
                adapter.setParentAndUpdateChildPos(parent, elements[i], i)
            }
        }

        override fun next(): N {
            return elements[nextPos++]
        }

        override fun hasNext(): Boolean = nextPos < elements.size

    }

    public interface Adapter<N: IAbstractNode<N, P>, P: IAbstractParentNode<N, P>> {
        public fun checkTypeAndOwner(node: PlatformNode): N

        public fun setParentAndUpdateChildPos(parent: P?, node: N, newPos: Int) {
            val newParent = if (newPos >= 0) parent else null
            @Suppress("UNCHECKED_CAST")
            (node as AbstractNode<N, P>).setParentNode(newParent)
        }

        public fun getChildPosHint(node: N): Int = POS_UNKNOWN

        public companion object {
            public const val POS_NO_PARENT: Int = -1
            public const val POS_UNKNOWN: Int = -2
        }
    }
}
