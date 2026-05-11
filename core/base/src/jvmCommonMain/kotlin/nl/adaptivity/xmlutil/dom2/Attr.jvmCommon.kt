/*
 * Copyright (c) 2025-2026.
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

package nl.adaptivity.xmlutil.dom2

import nl.adaptivity.xmlutil.dom.PlatformAttr
import nl.adaptivity.xmlutil.dom.PlatformNode
import org.w3c.dom.TypeInfo

public actual interface Attr : Node, PlatformAttr {
    public actual override fun getNamespaceURI(): String?
    public actual override fun getPrefix(): String?
    public actual override fun getLocalName(): String?
    public actual override fun getName(): String
    public actual override fun getValue(): String
    public actual override fun setValue(value: String)
    public actual override fun getOwnerElement(): Element?

    override fun getAttributes(): Nothing? = null

    override fun getParentNode(): Node?

    override fun getParentElement(): Element?

    @IgnorableReturnValue
    public actual override fun appendChild(node: PlatformNode): Nothing

    @IgnorableReturnValue
    public actual override fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): Nothing

    @IgnorableReturnValue
    public actual override fun removeChild(node: PlatformNode): Nothing

    public actual override fun getFirstChild(): Nothing?
    public actual override fun getLastChild(): Nothing?

    actual override fun getNodeValue(): String
    override fun setNodeValue(nodeValue: String?) {
        setValue(nodeValue ?: "")
    }

    override fun normalize() {}

    override fun cloneNode(deep: Boolean): Attr {
        return getOwnerDocument().createAttributeNS(getNamespaceURI(), getName()).also {
            it.setValue(getValue())
        }
    }

    /**
     * Always true for processing that does not have defaults
     */
    override fun getSpecified(): Boolean = true

    override fun getSchemaTypeInfo(): TypeInfo? {
        return null
    }

    override fun isId(): Boolean = false

    actual override fun getOwnerDocument(): Document
}
