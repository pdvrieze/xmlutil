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
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.dom.PlatformNode

@Serializable(NodeSerializer::class)
public expect interface Node : PlatformNode {
    public fun getParentElement(): Element?// = parentNode as? Element
    public fun getNodetype(): NodeType

    //region dom 1
    /** @since DOM Level 1 */
    public fun getNodeName(): String

    /** @since DOM Level 1 */
    @ExperimentalXmlUtilApi
    public fun getNodeValue(): String?

    /** @since DOM Level 1 */
    @ExperimentalXmlUtilApi
    public fun setNodeValue(value: String?)

    /** @since DOM Level 1 */
    public fun getParentNode(): Node?

    /** @since DOM Level 1 */
    public fun getChildNodes(): NodeList

    /** @since DOM Level 1 */
    public fun getFirstChild(): Node?

    /** @since DOM Level 1 */
    public fun getLastChild(): Node?

    /** @since DOM Level 1 */
    public fun getPreviousSibling(): Node?

    /** @since DOM Level 1 */
    public fun getNextSibling(): Node?

    /** @since DOM Level 1 */
    @ExperimentalXmlUtilApi
    public fun getAttributes(): NamedNodeMap<Attr>?

    /** @since DOM Level 1 */
    public fun getOwnerDocument(): Document?

    /** @since DOM Level 1 */
    @ExperimentalXmlUtilApi
    @IgnorableReturnValue
    public fun insertBefore(newChild: PlatformNode, refChild: PlatformNode?): Node?

    /** @since DOM Level 1 */
    @IgnorableReturnValue
    public fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): Node

    /** @since DOM Level 1 */
    @IgnorableReturnValue
    public fun removeChild(node: PlatformNode): Node

    /** @since DOM Level 1 */
    @IgnorableReturnValue
    public fun appendChild(node: PlatformNode): Node

    /** @since DOM Level 1 */
    @ExperimentalXmlUtilApi
    public fun hasChildNodes(): Boolean

    /** @since DOM Level 1 */
    @ExperimentalXmlUtilApi
    public fun cloneNode(deep: Boolean): Node

    //endregion

    //region dom 2

    /** @since DOM Level 2 */
    public fun hasAttributes(): Boolean
    // public fun isSupported(feature: String, version: String): Boolean

    /** @since DOM Level 2 */
    public fun getNamespaceURI(): String?
    /** @since DOM Level 2 */
    public fun getPrefix(): String?

    /** @since DOM Level 2 */
    public fun getLocalName(): String?

    //endregion

    //region dom 3

    @ExperimentalXmlUtilApi
    public fun normalize()

    @ExperimentalXmlUtilApi
    public fun isSameNode(other: PlatformNode): Boolean

    @ExperimentalXmlUtilApi
    public fun isEqualNode(other: PlatformNode): Boolean

    /** @since DOM Level 3 */
    @ExperimentalXmlUtilApi
    public fun getBaseURI(): String?

    /** @since DOM Level 3 */
    public fun getTextContent(): String?
    /** @since DOM Level 3 */
    public fun setTextContent(value: String?)

    @ExperimentalXmlUtilApi
    public fun lookupPrefix(namespace: String): String?

    @ExperimentalXmlUtilApi
    public fun lookupNamespaceURI(prefix: String): String?

    @ExperimentalXmlUtilApi
    public fun isDefaultNamespace(namespaceURI: String): Boolean
    //TODO public fun compareDocumentPosition(other: Node): Int
    //TODO public fun getFeature(feature: String, version: String): Any?

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
