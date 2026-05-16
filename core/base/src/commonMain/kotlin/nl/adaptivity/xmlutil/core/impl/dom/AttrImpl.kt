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

package nl.adaptivity.xmlutil.core.impl.dom

import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.dom.*
import nl.adaptivity.xmlutil.dom2.impl.AbstractAttr
import nl.adaptivity.xmlutil.dom2.impl.AbstractDocument

public class AttrImpl internal constructor(
    ownerDocument: DocumentImpl,
    namespaceURI: String?,
    localName: String,
    prefix: String?,
    value: String,
    parentNode: ElementImpl? = null,
) : AbstractAttr<NodeImpl, ParentNodeImpl>(ownerDocument, parentNode), NodeImpl {

    private val _namespaceURI = namespaceURI
    private val _localName = localName
    private val _prefix = prefix
    private var _value = value

    internal constructor(ownerDocument: DocumentImpl, original: PlatformAttr) : this(
        ownerDocument,
        original.getNamespaceURI(),
        original.getLocalName() ?: throw DOMException.invalidCharacterErr("Local name not set for attribute"),
        original.getPrefix(),
        original.getValue()
    )

    override fun isId(): Boolean = false

    override fun getOwnerDocument(): DocumentImpl = super.getOwnerDocument() as DocumentImpl

    @XmlUtilInternal
    override fun setOwnerDocument(ownerDocument: AbstractDocument<NodeImpl, ParentNodeImpl>) {
        super.setOwnerDocument(ownerDocument)
    }

    override fun getNamespaceURI(): String? = _namespaceURI

    override fun getPrefix(): String? = _prefix

    override fun getLocalName(): String = _localName

    override fun getValue(): String = _value

    override fun setValue(value: String) {
        this._value = value
    }

    override fun getOwnerElement(): ElementImpl? = getParentElement() as ElementImpl?

    override fun toString(): String {
        val attrName = when (getPrefix().isNullOrBlank()) {
            true -> getLocalName()
            else -> "${getPrefix()}:${getLocalName()}"
        }
        return "$attrName=\"${getValue()}\""
    }
}
