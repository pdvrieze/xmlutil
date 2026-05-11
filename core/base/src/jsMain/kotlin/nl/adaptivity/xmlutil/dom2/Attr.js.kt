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

public actual interface Attr : Node, PlatformAttr {
    override val parentElement: Element? get() = getParentElement()
    override val ownerDocument: Document get() = getOwnerDocument()

    override val namespaceURI: String? get() = getNamespaceURI()
    override val prefix: String? get() = getPrefix()
    override val localName: String get() = getLocalName() ?: getName()
    override val name: String get() = getName()
    override var value: String
        get() = getValue()
        set(value) { setValue(value) }
    override val ownerElement: Element?
        get() = getOwnerElement()

    public actual fun getNamespaceURI(): String?
    public actual fun getPrefix(): String?
    public actual fun getLocalName(): String?
    public actual fun getName(): String
    public actual fun getValue(): String
    public actual fun setValue(value: String)
    public actual fun getOwnerElement(): Element?

    actual override fun getOwnerDocument(): Document

    @IgnorableReturnValue
    public actual override fun appendChild(node: PlatformNode): Nothing

    @IgnorableReturnValue
    public actual override fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): Nothing

    @IgnorableReturnValue
    public actual override fun removeChild(node: PlatformNode): Nothing

    public actual override fun getFirstChild(): Nothing?
    public actual override fun getLastChild(): Nothing?
    actual override fun getNodeValue(): String
}
