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

package nl.adaptivity.xmlutil.core.impl.dom

import nl.adaptivity.xmlutil.core.impl.idom.IAttr
import nl.adaptivity.xmlutil.core.impl.idom.IElement
import nl.adaptivity.xmlutil.core.impl.idom.INode
import nl.adaptivity.xmlutil.core.impl.idom.INodeList
import nl.adaptivity.xmlutil.dom.DOMException
import nl.adaptivity.xmlutil.dom2.NodeType
import nl.adaptivity.xmlutil.dom.Attr as Attr1
import nl.adaptivity.xmlutil.dom2.Attr as Attr2

internal class AttrImpl(
    ownerDocument: DocumentImpl,
    override val namespaceURI: String?,
    override val localName: String,
    override val prefix: String?,
    override var value: String
) : NodeImpl(ownerDocument), IAttr {

    constructor(ownerDocument: DocumentImpl, original: Attr1) : this(
        ownerDocument,
        original.namespaceURI,
        original.localName,
        original.prefix,
        original.value
    )

    constructor(ownerDocument: DocumentImpl, original: Attr2) : this(
        ownerDocument,
        original.getNamespaceURI(),
        original.getLocalName() ?: original.getName(),
        original.getPrefix(),
        original.getValue()
    )

    override val nodeType: Short
        get() = nodetype.value

    override val name: String get() = getName()

    override fun getName(): String = when {
        getPrefix().isNullOrEmpty() -> localName
        else -> "${getPrefix()}:${getLocalName()}"
    }

    override fun getNamespaceURI(): String? = namespaceURI

    override fun getPrefix(): String? = prefix

    override fun getLocalName(): String = localName

    override fun getValue(): String = value

    override fun setValue(value: String) {
        this.value = value
    }

    override fun getNodeName(): String = getName()

    override val nodetype: NodeType
        get() = NodeType.ATTRIBUTE_NODE

    override fun getChildNodes(): INodeList = EmptyNodeList

    override fun getFirstChild(): Nothing? = null

    override fun getLastChild(): Nothing? = null

    override var ownerElement: IElement? = null
        internal set

    override fun getOwnerElement(): IElement? = ownerElement

    override var parentNode: INode?
        get() = null
        set(_) {
            throw UnsupportedOperationException()
        }

    override fun getTextContent(): String = value

    override fun setTextContent(value: String) {
        this.value = value
    }

    override fun appendChild(node: INode): Nothing {
        throw DOMException("Attributes have no children")
    }

    override fun replaceChild(oldChild: INode, newChild: INode): Nothing {
        throw DOMException("Attributes have no children")
    }

    override fun removeChild(node: INode): Nothing {
        throw DOMException("Attributes have no children")
    }

    override fun lookupPrefix(namespace: String): String? {
        return getOwnerElement()?.lookupPrefix(namespace)
    }

    override fun lookupNamespaceURI(prefix: String): String? {
        return getOwnerElement()?.lookupNamespaceURI(prefix)
    }

    override fun toString(): String {
        val attrName = when (getPrefix().isNullOrBlank()) {
            true -> getLocalName()
            else -> "${getPrefix()}:${getLocalName()}"
        }
        return "$attrName=\"${getValue()}\""
    }
}
