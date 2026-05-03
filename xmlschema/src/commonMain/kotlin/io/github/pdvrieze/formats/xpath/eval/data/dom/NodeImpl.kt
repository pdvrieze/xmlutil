/*
 * Copyright (c) 2024-2026.
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

package io.github.pdvrieze.formats.xpath.eval.data.dom

import nl.adaptivity.xmlutil.dom2.Node

internal abstract class NodeImpl() : Node {
    abstract override fun getOwnerDocument(): DocumentImpl
    abstract fun setOwnerDocument(ownerDocument: DocumentImpl)

    private var parentNode: NodeImpl? = null

    abstract override fun getChildNodes(): INodeListImpl

    override fun getParentNode(): NodeImpl? = parentNode

    override fun getParentElement(): ElementImpl? {
        return parentNode as? ElementImpl
    }

    fun setParentNode(node: Node?) {
        parentNode = node?.let { checkNode(it) }
    }

    override fun getPreviousSibling(): NodeImpl? {
        val siblings = (getParentNode() ?: return null).getChildNodes()
        if (siblings.item(0) == this || siblings.size <= 1) return null
        for (idx in 1 until siblings.size) {
            if (siblings.item(idx) == this) {
                return siblings.item(idx - 1)
            }
        }
        return null
    }

    override fun getNextSibling(): NodeImpl? {
        val siblings = (getParentNode() ?: return null).getChildNodes()
        if (siblings.item(siblings.size - 1) == this || siblings.size <= 1) return null
        for (idx in 0 until (siblings.size - 1)) {
            if (siblings.item(idx) == this) {
                return siblings.item(idx + 1)
            }
        }
        return null
    }

}
