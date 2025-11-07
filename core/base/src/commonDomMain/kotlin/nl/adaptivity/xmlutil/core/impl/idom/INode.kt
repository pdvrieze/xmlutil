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

import nl.adaptivity.xmlutil.dom.PlatformNode

public interface INode : PlatformNode {
    override fun getOwnerDocument(): IDocument
    override fun getParentNode(): INode?
    override fun getChildNodes(): INodeList
    override fun getFirstChild(): INode?
    override fun getLastChild(): INode?
    override fun getPreviousSibling(): INode?
    override fun getNextSibling(): INode?

    override fun getParentElement(): IElement? = getParentNode() as? IElement

    override fun appendChild(node: PlatformNode): INode

    override fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): INode

    override fun removeChild(node: PlatformNode): INode
}

