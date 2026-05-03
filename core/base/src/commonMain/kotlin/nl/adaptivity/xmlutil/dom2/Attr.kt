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

package nl.adaptivity.xmlutil.dom2

import nl.adaptivity.xmlutil.dom.PlatformAttr
import nl.adaptivity.xmlutil.dom.PlatformNode

public expect interface Attr : Node, PlatformAttr {
    public override fun getOwnerDocument(): Document

    public fun getNamespaceURI(): String?

    public fun getPrefix(): String?

    public fun getLocalName(): String?

    public fun getName(): String

    public fun getValue(): String

    public fun setValue(value: String)

    public fun getOwnerElement(): Element?

    public override fun appendChild(node: PlatformNode): Nothing
    public override fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): Nothing
    public override fun removeChild(node: PlatformNode): Nothing
    public override fun getFirstChild(): Nothing?
    public override fun getLastChild(): Nothing?

    override fun getNodeValue(): String
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public val Attr.namespaceURI: String? get() = getNamespaceURI()

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public val Attr.prefix: String? get() = getPrefix()

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public val Attr.localName: String? get() = getLocalName()

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public val Attr.name: String get() = getName()

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public var Attr.value: String
    get() = getValue()
    set(value) { setValue(value) }

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public val Attr.ownerElement: Element? get() = getOwnerElement()
