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

package nl.adaptivity.xmlutil.dom2

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.dom.PlatformNode

@Serializable(NodeSerializer::class)
public expect interface Node : PlatformNode {
    public fun getNodetype(): NodeType
    public fun getNodeName(): String
    public fun getNodeValue(): String?
    public fun getOwnerDocument(): Document?
    public fun getParentNode(): Node?
    public fun getTextContent(): String?
    public fun setTextContent(value: String?)
    public fun getChildNodes(): NodeList
    public fun getFirstChild(): Node?
    public fun getLastChild(): Node?
    public fun getPreviousSibling(): Node?
    public fun getNextSibling(): Node?
    public fun getParentElement(): Element?// = parentNode as? Element

    public fun lookupPrefix(namespace: String): String?

    public fun lookupNamespaceURI(prefix: String): String?

    @IgnorableReturnValue
    public fun appendChild(node: PlatformNode): Node

    @IgnorableReturnValue
    public fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): Node

    @IgnorableReturnValue
    public fun removeChild(node: PlatformNode): Node

    //region dom 3
    public fun hasChildNodes(): Boolean
    //endregion
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Deprecated("Use member instead", level = DeprecationLevel.HIDDEN)
@IgnorableReturnValue
public expect fun Node.appendChild(node: PlatformNode): Node
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Deprecated("Use member instead", level = DeprecationLevel.HIDDEN)
@IgnorableReturnValue
public expect fun Node.replaceChild(newChild: PlatformNode, oldChild: Node): Node
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Deprecated("Use member instead", level = DeprecationLevel.HIDDEN)
@IgnorableReturnValue
public expect fun Node.removeChild(node: PlatformNode): Node

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public inline val Node.nodeType: Short get() = getNodetype().value

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public inline val Node.nodeName: String get() = getNodeName()

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public inline val Node.ownerDocument: Document? get() = getOwnerDocument()

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public inline val Node.parentNode: Node? get() = getParentNode()

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public inline val Node.textContent: String? get() = getTextContent()

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public inline val Node.childNodes: NodeList get() = getChildNodes()

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public inline val Node.firstChild: Node? get() = getFirstChild()

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public inline val Node.lastChild: Node? get() = getLastChild()

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public inline val Node.previousSibling: Node? get() = getPreviousSibling()

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public inline val Node.nextSibling: Node? get() = getNextSibling()
