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

@file:MustUseReturnValues

package nl.adaptivity.xmlutil.core.impl.dom

import nl.adaptivity.xmlutil.dom2.impl.AbstractElement
import nl.adaptivity.xmlutil.dom2.impl.LinearAttrStorage
import nl.adaptivity.xmlutil.dom2.impl.LinearNodeStorage

internal class ElementImpl internal constructor(
    ownerDocument: DocumentImpl,
    private val namespaceURI: String?,
    private val localName: String,
    private val prefix: String?,
    parentNode: ParentNodeImpl? = null,
) : AbstractElement<NodeImpl, ParentNodeImpl>(
    ownerDocument = ownerDocument,
    nodeStorage = { LinearNodeStorage(ownerDocument.storageAdapter) },
    attrStorage = { LinearAttrStorage(ownerDocument.storageAdapter, it as AbstractElement<*, *>) },
    parentNode = parentNode
), ParentNodeImpl {

    override val self: ElementImpl get() = this

    override fun getOwnerDocument(): DocumentImpl = super.getOwnerDocument() as DocumentImpl

    override fun getNamespaceURI(): String? = namespaceURI

    override fun getPrefix(): String? = prefix

    override fun getLocalName(): String = localName

    override fun getTagName(): String = when (prefix) {
        null, "" -> localName
        else -> "$prefix:$localName"
    }

    override fun toString(): String {
        return buildString {
            append('<')
            val tagName = when {
                getPrefix().isNullOrEmpty() -> getLocalName()
                else -> "${getPrefix()}:${getLocalName()}"
            }
            append(tagName)
            for (a in getAttributes()) {
                append(' ').append(a)
            }

            val _childNodes = getChildNodes()

            if (_childNodes.isEmpty()) {
                append(" />")
            } else {
                append(">")
                _childNodes.joinTo(this, "")
                append("</").append(tagName).append('>')
            }
        }
    }

}
