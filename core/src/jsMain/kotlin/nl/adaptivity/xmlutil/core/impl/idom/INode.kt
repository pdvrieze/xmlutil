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

package nl.adaptivity.xmlutil.core.impl.idom

import nl.adaptivity.xmlutil.core.impl.dom.unWrap
import nl.adaptivity.xmlutil.dom2.NodeType
import nl.adaptivity.xmlutil.dom.Node as Node1
import nl.adaptivity.xmlutil.dom2.Node as Node2
import org.w3c.dom.Node as DomNode

public interface INode : Node1, Node2 {
    public val delegate: DomNode
    override val nodetype: NodeType get() = NodeType(nodeType)
    override val ownerDocument: IDocument
    override val parentNode: INode?
    override val parentElement: IElement? get() = parentNode as? IElement
    override val firstChild: INode?
    override val lastChild: INode?
    override val previousSibling: INode?
    override val nextSibling: INode?
    override val childNodes: INodeList


    override fun getNodeName(): String = nodeName
    override fun getOwnerDocument(): IDocument = ownerDocument
    override fun getParentNode(): INode? = parentNode
    override fun getParentElement(): IElement? = parentElement
    override fun getFirstChild(): INode? = firstChild
    override fun getLastChild(): INode? = lastChild
    override fun getPreviousSibling(): INode? = previousSibling
    override fun getNextSibling(): INode? = nextSibling
    override fun getNodeType(): Short = nodeType
    override fun getChildNodes(): INodeList = childNodes


    override fun appendChild(node: Node1): INode = appendChild(node.unWrap())
    override fun appendChild(node: Node2): INode = appendChild(node.unWrap())
    public fun appendChild(node: INode): INode = appendChild(node.delegate)
    public fun appendChild(newChild: DomNode): INode

    override fun replaceChild(oldChild: Node1, newChild: Node1): INode =
        replaceChild(oldChild.unWrap(), newChild.unWrap())
    override fun replaceChild(oldChild: Node2, newChild: Node2): INode =
        replaceChild(oldChild.unWrap(), newChild.unWrap())
    public fun replaceChild(oldChild: INode, newChild: INode): INode =
        replaceChild(oldChild.delegate, newChild.delegate)
    public fun replaceChild(newChild: DomNode, oldChild: DomNode): INode

    override fun removeChild(node: Node2): INode = removeChild(node.unWrap())
    override fun removeChild(node: Node1): INode = removeChild(node.unWrap())
    public fun removeChild(node: INode): INode = removeChild(node.delegate)
    public fun removeChild(oldChild: DomNode): INode

    public override fun getTextContent(): String? = textContent
    public override fun setTextContent(value: String) { textContent = value }
    public fun insertBefore(newChild: DomNode?, refChild: DomNode?): INode
    public fun getNodeValue(): String? = nodeValue
    public fun setNodeValue(nodeValue: String?) { this.nodeValue = nodeValue }
    public fun hasChildNodes(): Boolean
    public fun cloneNode(deep: Boolean): INode
    public fun normalize()
    public fun getBaseURI(): String = baseURI
    public fun compareDocumentPosition(other: DomNode): Short
    public fun isSameNode(other: DomNode?): Boolean
    public fun isDefaultNamespace(namespaceURI: String): Boolean
    public fun isEqualNode(arg: DomNode): Boolean
}

