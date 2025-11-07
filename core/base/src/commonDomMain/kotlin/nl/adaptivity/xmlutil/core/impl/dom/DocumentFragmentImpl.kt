/*
 * Copyright (c) 2024-2025.
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

package nl.adaptivity.xmlutil.core.impl.dom

import nl.adaptivity.xmlutil.core.impl.idom.IDocumentFragment
import nl.adaptivity.xmlutil.core.impl.idom.INode
import nl.adaptivity.xmlutil.core.impl.idom.INodeList
import nl.adaptivity.xmlutil.dom.DOMException
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom2.NodeType

internal class DocumentFragmentImpl(private var ownerDocument: DocumentImpl) : NodeImpl(), IDocumentFragment {
    override fun getOwnerDocument(): DocumentImpl = ownerDocument

    override fun setOwnerDocument(ownerDocument: DocumentImpl) {
        this.ownerDocument = ownerDocument
    }

    override fun getPreviousSibling(): Nothing? = null

    override fun getNextSibling(): Nothing? = null

    @Suppress("PropertyName")
    internal val _childNodes: NodeListImpl = NodeListImpl()

    override fun getChildNodes(): INodeList = _childNodes

    override fun getNodetype(): NodeType = NodeType.DOCUMENT_FRAGMENT_NODE

    override fun getNodeName(): String = "#document-fragment"

    override fun getFirstChild(): INode? = _childNodes.elements.firstOrNull()

    override fun getLastChild(): INode? = _childNodes.elements.lastOrNull()

    override fun getTextContent(): String = buildString {
        for (n in getChildNodes()) {
            appendTextContent(n)
        }
    }

    override fun setTextContent(value: String) {
        _childNodes.elements.clear()
        appendChild(getOwnerDocument().createTextNode(value))
    }

    override fun appendChild(node: PlatformNode): INode {
        val n = checkNode(node)
        _childNodes.elements.add(n)
        n.setParentNode(this)
        return n
    }

    override fun replaceChild(oldChild: PlatformNode, newChild: PlatformNode): INode {
        val old = checkNode(oldChild)
        val oldIdx = _childNodes.elements.indexOf(old)
        if (oldIdx < 0) throw DOMException("Old child not found")

        _childNodes.elements[oldIdx].setParentNode(null)
        val new = checkNode(newChild)
        _childNodes.elements[oldIdx] = new
        new.setParentNode(this)

        return old
    }

    override fun removeChild(node: PlatformNode): INode {
        val c = checkNode(node)

        if (!_childNodes.elements.remove(c)) throw DOMException("Node not found")
        c.setParentNode(null)

        return c
    }

    override fun lookupPrefix(namespace: String): String? = null

    override fun lookupNamespaceURI(prefix: String): String? = null
}
