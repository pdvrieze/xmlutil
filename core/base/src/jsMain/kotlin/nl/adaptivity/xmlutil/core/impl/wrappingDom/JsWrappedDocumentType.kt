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

package nl.adaptivity.xmlutil.core.impl.wrappingDom

import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom2.DocumentType
import nl.adaptivity.xmlutil.dom2.Element
import org.w3c.dom.DocumentType as DOMDocumentType

internal class JsWrappedDocumentType(delegate: DOMDocumentType) : JsWrappedNode<DOMDocumentType>(delegate), DocumentType {
    override val parentElement: Element?
        get() = getParentElement()

    override fun getNodeValue(): Nothing? = null

    override fun getName(): String = delegate.name

    override fun getPublicId(): String = delegate.publicId

    override fun getSystemId(): String = delegate.systemId

    override fun appendChild(node: PlatformNode): Nothing =
        throw UnsupportedOperationException("No children in document type")

    override fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): Nothing =
        throw UnsupportedOperationException("No children in document type")

    override fun removeChild(node: PlatformNode): Nothing =
        throw UnsupportedOperationException("No children in document type")

    public override fun getFirstChild(): Nothing? = null

    public override fun getLastChild(): Nothing? = null

    override fun getAttributes(): Nothing? = null

}
