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
import nl.adaptivity.xmlutil.dom.PlatformDocumentFragment
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom.childNodes
import nl.adaptivity.xmlutil.dom.nodeType
import nl.adaptivity.xmlutil.dom2.DocumentFragment
import nl.adaptivity.xmlutil.dom2.NodeType
import nl.adaptivity.xmlutil.dom2.nodeType

@ExperimentalXmlUtilApi
public abstract class AbstractDocumentFragment<out N : IAbstractNode<N, P>, out P : IAbstractParentNode<N, P>>(
    ownerDocument: AbstractDocument<N, P>,
    nodeStorage: (P) -> MutableAbstractNodeStorage<N, P>,
    parentNode: P? = null
) : AbstractParentNode<N, P>(ownerDocument, nodeStorage, parentNode,), DocumentFragment {

    final override fun getPreviousSibling(): Nothing? = null

    final override fun getNextSibling(): Nothing? = null

    final override fun getTextContent(): String = getTextContentImpl()

    final override fun getNodetype(): NodeType = NodeType.DOCUMENT_FRAGMENT_NODE
    final override fun getNodeValue(): Nothing? = null

    final override fun getNamespaceURI(): Nothing? = null

    final override fun getPrefix(): Nothing? = null

    final override fun getLocalName(): Nothing? = null

    @ExperimentalXmlUtilApi
    final override fun setNodeValue(value: String?) {
        /** defined as NO-OP */
    }

    final override fun getNodeName(): String = "#document-fragment"
    final override fun getAttributes(): Nothing? = null
    final override fun hasAttributes(): Boolean = false

    override fun getOwnerDocument(): AbstractDocument<N, P> {
        return checkNotNull(super.getOwnerDocument()) { "Document fragments cannot have a null owner document" }
    }

    final override fun lookupPrefix(namespace: String): Nothing? = null

    final override fun lookupNamespaceURI(prefix: String): Nothing? = null

    override fun cloneNode(deep: Boolean): AbstractDocumentFragment<N, P> {
        return getOwnerDocument().createDocumentFragment().also { f ->
            if (deep) {
                for (c in getChildNodes()) f.appendChild(c.cloneNode(deep))
            }
        }
    }

    override fun isEqualNode(other: PlatformNode): Boolean {
        return when {
            this === other -> true
            nodeType != other.nodeType -> false //handle javascript instance check issues
            other !is AbstractDocumentFragment<*,*> -> false

            else -> nodeStorage.isEqualNodes(other.childNodes)
        }
    }
}
