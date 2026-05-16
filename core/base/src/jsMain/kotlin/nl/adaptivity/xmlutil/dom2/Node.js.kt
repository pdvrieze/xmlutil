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
    override val ownerDocument: Document? get() = getOwnerDocument()
    override val parentNode: Node? get() = getParentNode()
    override val parentElement: Element? get() = getParentElement()
    override val childNodes: NodeList get() = getChildNodes()
    override val firstChild: Node? get() = getFirstChild()
    override val lastChild: Node? get() = getLastChild()
    override val previousSibling: Node? get() = getPreviousSibling()
    override val nextSibling: Node? get() = getNextSibling()

    override val nodeType: Short get() = getNodetype().value
    override val nodeName: String get() = getNodeName()
    override val nodeValue: String? get() = getNodeValue()
    override var textContent: String?
        get() = getTextContent()
        set(value) {
            setTextContent(value)
        }

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
    public actual fun getAttributes(): NamedNodeMap?

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
