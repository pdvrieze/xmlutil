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

import nl.adaptivity.xmlutil.dom.*

internal class DocumentTypeImpl(
    ownerDocument: Document,
    override val name: String,
    override val publicId: String,
    override val systemId: String
) : NodeImpl(ownerDocument), DocumentType {
    override var parentNode: Node? = null

    override val nodeType: Short get() = Node.DOCUMENT_TYPE_NODE

    override val nodeName: String get() = name

    override val childNodes: NodeList get() = EmptyNodeList

    override val firstChild: Nothing? get() = null

    override val lastChild: Nothing? get() = null

    override val textContent: String? get() = null

    override fun lookupPrefix(namespace: String?): String? {
        return parentNode?.lookupPrefix(namespace)
    }

    override fun lookupNamespaceURI(prefix: String?): String? {
        return parentNode?.lookupNamespaceURI(prefix)
    }

    override fun appendChild(node: Node): Node {
        throw DOMException("Document types have no children")
    }

    override fun replaceChild(oldChild: Node, newChild: Node): Node {
        throw DOMException("Document types have no children")
    }

    override fun removeChild(node: Node): Node {
        throw DOMException("Document types have no children")
    }
}
