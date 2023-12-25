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

package nl.adaptivity.xmlutil.core.impl.dom

import nl.adaptivity.xmlutil.core.impl.idom.IDocument
import nl.adaptivity.xmlutil.core.impl.idom.INode
import nl.adaptivity.xmlutil.core.impl.idom.INodeList
import nl.adaptivity.xmlutil.dom2.NodeType
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.w3c.dom.UserDataHandler
import org.w3c.dom.Node as DomNode

public abstract class NodeImpl<N: DomNode>(public override val delegate: DomNode): INode {
    override fun getOwnerDocument(): IDocument = delegate.ownerDocument.wrap()

    override fun getParentNode(): INode? = delegate.parentNode.wrap()

    override fun getFirstChild(): INode? = delegate.firstChild.wrap()

    override fun getLastChild(): INode? = delegate.lastChild.wrap()

    override fun getPreviousSibling(): INode? = delegate.previousSibling.wrap()

    override fun getNextSibling(): INode? = delegate.nextSibling.wrap()

    override fun getNodeName(): String = delegate.nodeName

    override val nodetype: NodeType get() = NodeType(delegate.nodeType)

    override fun getTextContent(): String? = delegate.textContent

    override fun setTextContent(textContent: String?) {
        delegate.textContent = textContent
    }

    override fun getChildNodes(): INodeList = WrappingNodeList(delegate.childNodes)

    override fun getNodeValue(): String = delegate.nodeValue

    override fun setNodeValue(nodeValue: String?) {
        delegate.nodeValue = nodeValue
    }

    override fun insertBefore(newChild: DomNode?, refChild: DomNode?): INode {
        return delegate.insertBefore(newChild?.unWrap(), refChild?.unWrap()).wrap()
    }

    override fun hasChildNodes(): Boolean = delegate.hasChildNodes()

    override fun cloneNode(deep: Boolean): INode {
        return delegate.cloneNode(deep).wrap()
    }

    override fun normalize() {
        delegate.normalize()
    }

    override fun isSupported(feature: String?, version: String?): Boolean {
        return delegate.isSupported(feature, version)
    }

    override fun getNamespaceURI(): String? = delegate.namespaceURI

    override fun getPrefix(): String? = delegate.prefix

    override fun setPrefix(prefix: String?) {
        delegate.prefix = prefix
    }

    override fun getLocalName(): String? = delegate.localName

    override fun hasAttributes(): Boolean = delegate.hasAttributes()

    override fun getBaseURI(): String = delegate.baseURI

    override fun compareDocumentPosition(other: DomNode): Short {
        return delegate.compareDocumentPosition(other.unWrap())
    }

    override fun isSameNode(other: DomNode?): Boolean = delegate.isSameNode(other?.unWrap())

    override fun lookupPrefix(namespace: String): String? = delegate.lookupPrefix(namespace)

    override fun isDefaultNamespace(namespaceURI: String): Boolean = delegate.isDefaultNamespace(namespaceURI)

    override fun lookupNamespaceURI(prefix: String): String? = delegate.lookupNamespaceURI(prefix)

    override fun isEqualNode(arg: DomNode): Boolean {
        return delegate.isEqualNode(arg.unWrap())
    }

    override fun getFeature(feature: String, version: String?): Any? {
        return delegate.getFeature(feature, version)
    }

    override fun setUserData(key: String, data: Any?, handler: UserDataHandler?): Any? {
        return delegate.setUserData(key, data, handler)
    }

    override fun getUserData(key: String): Any? {
        return delegate.getUserData(key)
    }

    override fun appendChild(node: INode): INode {
        return delegate.appendChild(node.unWrap()).wrap()
    }

    override fun appendChild(newChild: DomNode): INode {
        return delegate.appendChild(newChild.unWrap()).wrap()
    }

    override fun replaceChild(oldChild: INode, newChild: INode): INode {
        return delegate.replaceChild(oldChild.unWrap(), newChild.unWrap()).wrap()
    }

    override fun replaceChild(newChild: Node, oldChild: Node): INode {
        return delegate.replaceChild(oldChild.unWrap(), newChild.unWrap()).wrap()
    }

    override fun removeChild(node: INode): INode {
        return delegate.removeChild(node.unWrap()).wrap()
    }

    override fun removeChild(oldChild: Node): INode {
        return delegate.removeChild(oldChild.unWrap()).wrap()
    }
}

internal fun DomNode.unWrap(): DomNode = when (this) {
    is INode -> delegate
    else -> this
}

internal fun DomNode.wrap(): INode = when (this) {
    is INode -> this
    else -> error("Node type $nodeType not supported")
}

internal fun Document.wrap(): IDocument = when (this) {
    is IDocument -> this
    else -> error("Node type $nodeType not supported")
}
