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

@file:Suppress("ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT")
@file:MustUseReturnValues

package nl.adaptivity.xmlutil.dom2

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.dom.PlatformNode

@Serializable(NodeSerializer::class)
public actual interface Node : PlatformNode {
    actual override fun getFirstChild(): Node?
    actual override fun getParentNode(): Node?
    actual override fun getLastChild(): Node?
    actual override fun getPreviousSibling(): Node?
    actual override fun getNextSibling(): Node?
    actual override fun getOwnerDocument(): Document?
    actual override fun getParentElement(): Element?

    @IgnorableReturnValue
    public actual override fun appendChild(node: PlatformNode): Node

    @IgnorableReturnValue
    public actual override fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): Node

    @IgnorableReturnValue
    public actual override fun removeChild(node: PlatformNode): Node

    @ExperimentalXmlUtilApi
    public actual fun hasChildNodes(): Boolean

    @ExperimentalXmlUtilApi
    public actual fun getAttributes(): NamedNodeMap<Attr>?

    @ExperimentalXmlUtilApi
    public actual fun cloneNode(deep: Boolean): Node

    @ExperimentalXmlUtilApi
    public actual fun normalize()

    @ExperimentalXmlUtilApi
    @IgnorableReturnValue
    public actual fun insertBefore(newChild: PlatformNode, refChild: PlatformNode?): Node?

    @ExperimentalXmlUtilApi
    public actual fun isSameNode(other: PlatformNode): Boolean

    @ExperimentalXmlUtilApi
    public actual fun isEqualNode(other: PlatformNode): Boolean

    @ExperimentalXmlUtilApi
    public actual fun getBaseURI(): String?

    @ExperimentalXmlUtilApi
    public actual override fun getNodeValue(): String?
    @ExperimentalXmlUtilApi
    public actual fun setNodeValue(value: String?)
    public actual fun getNamespaceURI(): String?
    public actual fun getPrefix(): String?
    public actual fun getLocalName(): String?
    @ExperimentalXmlUtilApi
    public actual fun isDefaultNamespace(namespaceURI: String): Boolean

}

@IgnorableReturnValue
public actual fun Node.appendChild(node: PlatformNode): Node {
    return appendChild(node) // child member
}

@IgnorableReturnValue
public actual fun Node.replaceChild(
    newChild: PlatformNode,
    oldChild: Node
): Node {
    return replaceChild(newChild, oldChild) // child member
}

@IgnorableReturnValue
public actual fun Node.removeChild(node: PlatformNode): Node {
    return removeChild(node)
}
