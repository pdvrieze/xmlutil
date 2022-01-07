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

import nl.adaptivity.xmlutil.XMLConstants
import org.w3c.dom.*

internal class AttrImpl(
    ownerDocument: Document,
    override val namespaceURI: String?,
    override val localName: String,
    override val prefix: String?,
    override var value: String
) : NodeImpl(ownerDocument), Attr {

    constructor(ownerDocument: DocumentImpl, original: Attr) : this(
        ownerDocument,
        original.namespaceURI,
        original.localName,
        original.prefix,
        original.value
    )

    init {
        if (prefix.isNullOrEmpty() && !namespaceURI.isNullOrEmpty() && namespaceURI!=XMLConstants.XMLNS_ATTRIBUTE_NS_URI) {
            throw IllegalArgumentException("Attributes without prefix are always in the default namespace ({$namespaceURI}$localName = \"$value\")")
        }
    }

    override val name: String
        get() = when(prefix) {
            null -> localName
            else -> "$prefix:$localName"
        }

    override val nodeName: String
        get() = name

    override val nodeType: Short
        get() = Node.ATTRIBUTE_NODE

    override val childNodes: NodeList get() = EmptyNodeList

    override val firstChild: Nothing? get() = null

    override val lastChild: Nothing? get() = null

    override var ownerElement: Element? = null
        internal set

    override var parentNode: Node?
        get() = null
        set(_) {
            throw UnsupportedOperationException()
        }

    override val textContent: String?
        get() = value

    override fun appendChild(node: Node): Node {
        throw DOMException("Attributes have no children")
    }

    override fun replaceChild(oldChild: Node, newChild: Node): Node {
        throw DOMException("Attributes have no children")
    }

    override fun removeChild(node: Node): Node {
        throw DOMException("Attributes have no children")
    }

    override fun lookupPrefix(namespace: String?): String? {
        return ownerElement?.lookupPrefix(namespace)
    }

    override fun lookupNamespaceURI(prefix: String?): String? {
        return ownerElement?.lookupNamespaceURI(prefix)
    }

    override fun toString(): String {
        val attrName = when (prefix.isNullOrBlank()) {
            true -> localName
            else -> "$prefix:$localName"
        }
        return "$attrName=\"$value\""
    }
}
