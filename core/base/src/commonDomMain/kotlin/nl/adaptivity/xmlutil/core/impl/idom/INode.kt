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

package nl.adaptivity.xmlutil.core.impl.idom

import nl.adaptivity.xmlutil.dom.PlatformNode as Node1
import nl.adaptivity.xmlutil.dom2.Node as Node2

public interface INode : Node1, Node2 {
    override val ownerDocument: IDocument get() = getOwnerDocument()
    override val parentNode: INode? get() = getParentNode()
    override val nodeName: String get() = getNodeName()
    override val childNodes: INodeList get() = getChildNodes()
    override val firstChild: INode? get() = getFirstChild()
    override val lastChild: INode? get() = getLastChild()
    override val previousSibling: INode? get() = getPreviousSibling()
    override val nextSibling: INode? get() = getNextSibling()
    override val nodeType: Short get() = getNodeType()
    override val textContent: String? get() = getTextContent()

    override fun getOwnerDocument(): IDocument
    override fun getParentNode(): INode? = parentNode
    override fun getParentElement(): IElement? = getParentNode() as? IElement
    override fun getFirstChild(): INode?
    override fun getLastChild(): INode?
    override fun getPreviousSibling(): INode?
    override fun getNextSibling(): INode?
    override fun getNodeType(): Short = super.getNodeType()
    override fun getChildNodes(): INodeList

    override fun appendChild(node: Node1): INode = appendChild(node as INode)

    override fun appendChild(node: Node2): INode = appendChild(node as INode)

    public fun appendChild(node: INode): INode

    override fun replaceChild(oldChild: Node1, newChild: Node1): INode =
        replaceChild(oldChild as INode, newChild as INode)

    override fun replaceChild(oldChild: Node2, newChild: Node2): INode =
        replaceChild(oldChild as INode, newChild as INode)

    public fun replaceChild(oldChild: INode, newChild: INode): INode

    override fun removeChild(node: Node1): INode = removeChild(node as INode)

    override fun removeChild(node: Node2): INode = removeChild(node as INode)

    public fun removeChild(node: INode): INode

    public fun getTextContext(): String? = textContent
}

