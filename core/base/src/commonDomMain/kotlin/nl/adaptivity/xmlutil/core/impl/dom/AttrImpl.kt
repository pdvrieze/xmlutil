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

package nl.adaptivity.xmlutil.core.impl.dom

import nl.adaptivity.xmlutil.core.impl.idom.IAttr
import nl.adaptivity.xmlutil.core.impl.idom.IElement
import nl.adaptivity.xmlutil.core.impl.idom.INodeList
import nl.adaptivity.xmlutil.dom.DOMException
import nl.adaptivity.xmlutil.dom.PlatformAttr
import nl.adaptivity.xmlutil.dom2.NodeType

internal class AttrImpl(
    private var ownerDocument: DocumentImpl,
    private val namespaceURI: String?,
    private val localName: String,
    private val prefix: String?,
    private var value: String
) : NodeImpl(), IAttr {

    constructor(ownerDocument: DocumentImpl, original: PlatformAttr) : this(
        ownerDocument,
        original.getNamespaceURI(),
        original.getLocalName() ?: throw DOMException.invalidCharacterErr("Local name not set for attribute") ,
        original.getPrefix(),
        original.getValue()
    )

    override fun getOwnerDocument(): DocumentImpl = ownerDocument

    override fun setOwnerDocument(ownerDocument: DocumentImpl) {
        if (ownerDocument !== this.ownerDocument) {
            setOwnerElement(null)
            this.ownerDocument = ownerDocument
        }
    }

    override fun getNodetype(): NodeType = NodeType.ATTRIBUTE_NODE

    override fun getName(): String = when {
        prefix.isNullOrEmpty() -> localName
        else -> "${prefix}:${localName}"
    }

    override fun getNamespaceURI(): String? = namespaceURI

    override fun getPrefix(): String? = prefix

    override fun getLocalName(): String = localName

    override fun getValue(): String = value

    override fun setValue(value: String) {
        this.value = value
    }

    override fun getNodeName(): String = getName()

    override fun getChildNodes(): INodeList = EmptyNodeList

    private var ownerElement: IElement? = null

    override fun getOwnerElement(): IElement? = ownerElement
    internal fun setOwnerElement(ownerElement: IElement?) {
        this.ownerElement = ownerElement
    }

    override fun getTextContent(): String = value

    override fun setTextContent(value: String) {
        this.value = value
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
