/*
 * Copyright (c) 2024.
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

import nl.adaptivity.xmlutil.dom2.Node as Node2
import org.w3c.dom.Node as DomNode

public interface INode : DomNode, Node2 {
    public val delegate: DomNode

    override fun getOwnerDocument(): IDocument
    override fun getParentNode(): INode?
    override fun getParentElement(): IElement? = getParentNode() as? IElement
    override fun getFirstChild(): INode?
    override fun getLastChild(): INode?
    override fun getPreviousSibling(): INode?
    override fun getNextSibling(): INode?
    override fun getNodeType(): Short
    override fun getAttributes(): INamedNodeMap? = null
    override fun getChildNodes(): INodeList

    override fun appendChild(node: Node2): INode = appendChild(node as INode)

    public fun appendChild(node: INode): INode

    override fun replaceChild(oldChild: Node2, newChild: Node2): INode =
        replaceChild(oldChild as INode, newChild as INode)

    public fun replaceChild(oldChild: INode, newChild: INode): INode

    override fun removeChild(node: Node2): INode = removeChild(node as INode)

    public fun removeChild(node: INode): INode
}

