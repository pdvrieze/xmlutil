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
import nl.adaptivity.xmlutil.dom2.Attr
import nl.adaptivity.xmlutil.dom2.Element
import org.w3c.dom.Attr as DomAttr
import org.w3c.dom.Node as DomNode

internal class AttrImpl(delegate: DomAttr) : NodeImpl<DomAttr>(delegate), Attr {
    override var value: String
        get() = delegate.value
        set(value) {
            delegate.value = value
        }

    override val ownerDocument: DocumentImpl get() = checkNotNull(super<NodeImpl>.ownerDocument)

    override val parentElement: Element? get() = ownerElement

    override fun getOwnerDocument(): DocumentImpl = checkNotNull(super.getOwnerDocument())

    override fun getOwnerElement(): Element? = ownerElement

    override fun getValue(): String = value

    override fun setValue(value: String) {
        this.value = value
    }

    override fun getNodeValue(): String {
        return getValue()
    }

    override fun getPrefix(): String? = prefix

    override fun getNamespaceURI(): String? = namespaceURI

    override fun getLocalName(): String = localName

    override fun getName(): String = name


    override val namespaceURI: String? get() = delegate.namespaceURI

    override val prefix: String? get() = delegate.prefix

    override val localName: String
        get() = delegate.localName

    override val name: String get() = delegate.name

    override val ownerElement: ElementImpl?
        get() = delegate.ownerElement?.wrap()

    override fun appendChild(node: PlatformNode): Nothing =
        throw UnsupportedOperationException("No children in attributes")

    override fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): Nothing =
        throw UnsupportedOperationException("No children in attributes")

    override fun removeChild(node: PlatformNode): Nothing =
        throw UnsupportedOperationException("No children in attributes")

    override fun getFirstChild(): Nothing? = null
    override fun getLastChild(): Nothing? = null

}

internal fun DomNode.wrapAttr(): AttrImpl {
    return (this as DomAttr).wrap()
}
