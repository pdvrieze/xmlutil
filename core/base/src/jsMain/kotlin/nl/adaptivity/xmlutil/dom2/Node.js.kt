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

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.core.impl.wrappingDom.wrap
import nl.adaptivity.xmlutil.dom.PlatformNode

@Serializable(with = NodeSerializer::class)
public actual interface Node : PlatformNode {

    @JsName("nodeType")
    public val nodeType: Short get() = getNodetype().value

    public actual fun getNodetype(): NodeType
    public actual fun getNodeName(): String
    public actual fun getNodeValue(): String?
    public actual fun getOwnerDocument(): Document?
    public actual fun getParentNode(): Node?
    public actual fun getTextContent(): String?
    public actual fun setTextContent(value: String?)
    public actual fun getChildNodes(): NodeList
    public actual fun getFirstChild(): Node?
    public actual fun getLastChild(): Node?
    public actual fun getPreviousSibling(): Node?
    public actual fun getNextSibling(): Node?
    public actual fun getParentElement(): Element?

    @ExperimentalXmlUtilApi
    public actual override fun lookupPrefix(namespace: String): String?

    @ExperimentalXmlUtilApi
    public actual override fun lookupNamespaceURI(prefix: String): String?

    @ExperimentalXmlUtilApi
    @IgnorableReturnValue
    public actual fun appendChild(node: PlatformNode): Node

    @ExperimentalXmlUtilApi
    @IgnorableReturnValue
    public actual fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): Node

    @ExperimentalXmlUtilApi
    @IgnorableReturnValue
    public actual fun removeChild(node: PlatformNode): Node

    @ExperimentalXmlUtilApi
    public actual fun hasChildNodes(): Boolean

    @ExperimentalXmlUtilApi
    public actual fun getAttributes(): NamedNodeMap<Attr>?

    @ExperimentalXmlUtilApi
    public actual fun hasAttributes(): Boolean

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
    public actual fun setNodeValue(value: String?)
    public actual fun getNamespaceURI(): String?
    public actual fun getPrefix(): String?
    public actual fun getLocalName(): String?
    @ExperimentalXmlUtilApi
    public actual fun isDefaultNamespace(namespaceURI: String): Boolean


    @IgnorableReturnValue
    @Deprecated("Binary only", level = DeprecationLevel.HIDDEN)
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    public fun appendChild(node: Node): Node = appendChild(node.unsafeCast<PlatformNode>())

    @IgnorableReturnValue
    @Deprecated("Binary only", level = DeprecationLevel.HIDDEN)
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    public fun insertBefore(newChild: Node, refChild: Node?): Node? =
        insertBefore(newChild.unsafeCast<PlatformNode>(), refChild?.unsafeCast<PlatformNode>())

    @IgnorableReturnValue
    @Deprecated("Binary only", level = DeprecationLevel.HIDDEN)
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    public fun replaceChild(newChild: Node, oldChild: Node): Node =
        replaceChild(newChild.unsafeCast<PlatformNode>(), oldChild.unsafeCast<PlatformNode>())

    @IgnorableReturnValue
    @Deprecated("Binary only", level = DeprecationLevel.HIDDEN)
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    public fun removeChild(node: Node): Node = removeChild(node.unsafeCast<PlatformNode>())

}

@IgnorableReturnValue
public actual fun Node.appendChild(node: PlatformNode): Node {
    val n = node as? Node ?: getOwnerDocument()!!.let { d -> d.adoptNode(node) ?: d.importNode(node, true) }
    return appendChild(n)
}

@IgnorableReturnValue
public actual fun Node.replaceChild(
    newChild: PlatformNode,
    oldChild: Node
): Node {
    val n = newChild as? Node ?: getOwnerDocument()!!.let { d -> d.adoptNode(newChild) ?: d.importNode(newChild, true) }
    return replaceChild(n, oldChild)
}

@IgnorableReturnValue
public actual fun Node.removeChild(node: PlatformNode): Node {
    val n = node as? Node ?: node.wrap()
    return removeChild(n)
}
