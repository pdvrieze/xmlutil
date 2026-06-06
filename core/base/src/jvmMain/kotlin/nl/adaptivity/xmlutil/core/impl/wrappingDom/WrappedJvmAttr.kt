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

import nl.adaptivity.xmlutil.dom.DOMException
import nl.adaptivity.xmlutil.dom.PlatformAttr
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom2.Attr
import org.w3c.dom.TypeInfo

internal class WrappedJvmAttr(delegate: PlatformAttr) : WrappedJvmNode<PlatformAttr>(delegate), Attr {
    override fun getLocalName(): String? = when {
        delegate.namespaceURI.isNullOrEmpty() -> delegate.name
        else -> delegate.localName
    }
    override fun getFirstChild(): Nothing? = null

    override fun getLastChild(): Nothing? = null

    override fun getOwnerElement(): WrappedJvmElement? = delegate.ownerElement?.wrap()

    override fun getName(): String = delegate.name

    override fun getSpecified(): Boolean = delegate.specified

    override fun getNodeValue(): String = delegate.nodeValue

    override fun setNodeValue(value: String?) {
        delegate.value = value
    }

    override fun normalize() {
        delegate.normalize()
    }

    override fun cloneNode(deep: Boolean): WrappedJvmAttr {
        return WrappedJvmAttr(delegate.cloneNode(deep) as PlatformAttr)
    }

    override fun getOwnerDocument(): WrappedJvmDocument {
        return checkNotNull(super.getOwnerDocument())
    }

    override fun getValue(): String = delegate.value

    override fun setValue(value: String) {
        delegate.value = value
    }

    override fun getSchemaTypeInfo(): TypeInfo = delegate.schemaTypeInfo

    override fun isId(): Boolean = delegate.isId

    @IgnorableReturnValue
    override fun appendChild(node: PlatformNode): Nothing =
        throw DOMException.hierarchyRequestErr("No children in attributes")

    override fun insertBefore(
        newChild: PlatformNode,
        refChild: PlatformNode?
    ): Nothing {
        throw DOMException.hierarchyRequestErr("No children in attributes")
    }

    @IgnorableReturnValue
    override fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): Nothing =
        throw DOMException.hierarchyRequestErr("No children in attributes")

    @IgnorableReturnValue
    override fun removeChild(node: PlatformNode): Nothing =
        throw DOMException.hierarchyRequestErr("No children in attributes")

    override fun getAttributes(): Nothing? = null
}
