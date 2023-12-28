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

import nl.adaptivity.xmlutil.core.impl.idom.*

internal abstract class NodeImpl(
    ownerDocument: DocumentImpl
) : INode {
    final override var ownerDocument = ownerDocument
        internal set

    abstract override var parentNode: INode?
        internal set

    final override fun getOwnerDocument(): DocumentImpl = ownerDocument

    override fun getPreviousSibling(): INode? {
        val siblings = (getParentNode() ?: return null).childNodes
        if (siblings.item(0) == this || siblings.size <= 1) return null
        for (idx in 1 until siblings.size) {
            if (siblings.item(idx) == this) {
                return siblings.item(idx - 1)
            }
        }
        return null
    }

    override fun getNextSibling(): INode? {
        val siblings = (getParentNode() ?: return null).childNodes
        if (siblings.item(siblings.size - 1) == this || siblings.size <= 1) return null
        for (idx in 0 until (siblings.size - 1)) {
            if (siblings.item(idx) == this) {
                return siblings.item(idx + 1)
            }
        }
        return null
    }

}

internal fun Appendable.appendTextContent(node: INode) {
    when (node) {
        is IDocumentFragment,
        is IElement -> for (n in node.getChildNodes()) {
            appendTextContent(n)
        }

        is IAttr -> append(node.getValue())

        is ICharacterData -> append(node.getData())
    }
}
