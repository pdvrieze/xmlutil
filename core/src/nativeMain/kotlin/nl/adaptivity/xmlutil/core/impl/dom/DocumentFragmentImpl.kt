/*
 * Copyright (c) 2022. 
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

package nl.adaptivity.xmlutil.core.impl.dom

import org.w3c.dom.*

internal class DocumentFragmentImpl(ownerDocument: Document) : NodeImpl(ownerDocument), DocumentFragment {
    override val previousSibling: Nothing? get() = null
    override val nextSibling: Nothing? get() = null

    override var parentNode: Node?
        get() = null
        set(_) {
            throw UnsupportedOperationException()
        }

    internal val _childNodes: NodeListImpl = NodeListImpl()
    override val childNodes: NodeList get() = _childNodes

    override val nodeType: Short get() = Node.DOCUMENT_FRAGMENT_NODE

    override val nodeName: String get() = "#document-fragment"

    override val firstChild: Node?
        get() = _childNodes.elements.firstOrNull()

    override val lastChild: Node?
        get() = _childNodes.elements.lastOrNull()

    override fun appendChild(node: Node): Node {
        val n = checkNode(node)
        _childNodes.elements.add(n)
        n.parentNode = this
        return node
    }

    override fun replaceChild(oldChild: Node, newChild: Node): Node {
        val oldIdx = _childNodes.elements.indexOf(checkNode(oldChild))
        if (oldIdx < 0) throw DOMException("Old child not found")

        _childNodes.elements[oldIdx].parentNode = null
        val n = checkNode(newChild)
        _childNodes.elements[oldIdx] = n
        n.parentNode = this

        return oldChild
    }

    override fun removeChild(node: Node): Node {
        val c = checkNode(node)

        if (!_childNodes.elements.remove(c)) throw DOMException("Node not found")
        c.parentNode = null

        return c
    }

    override fun lookupPrefix(namespace: String?): String? = null

    override fun lookupNamespaceURI(prefix: String?): String? = null
}
