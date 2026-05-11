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
import nl.adaptivity.xmlutil.core.impl.wrappingDom.wrap
import nl.adaptivity.xmlutil.dom.PlatformNode
import org.w3c.dom.UserDataHandler

@Serializable(with = NodeSerializer::class)
public actual interface Node: PlatformNode {
    public actual fun getNodetype(): NodeType
    public actual override fun getNodeName(): String
    public actual override fun getNodeValue(): String?
    public actual override fun getOwnerDocument(): Document?
    public actual override fun getParentNode(): Node?
    public actual override fun getTextContent(): String?
    public actual override fun setTextContent(value: String?)
    public actual override fun getChildNodes(): NodeList
    public actual override fun getFirstChild(): Node?
    public actual override fun getLastChild(): Node?
    public actual override fun getPreviousSibling(): Node?
    public actual override fun getNextSibling(): Node?
    public actual fun getParentElement(): Element?
    public actual override fun lookupPrefix(namespace: String): String?
    public actual override fun lookupNamespaceURI(prefix: String): String?

    @IgnorableReturnValue
    public actual override fun appendChild(node: PlatformNode): Node

    @IgnorableReturnValue
    public actual override fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): Node

    override fun getNodeType(): Short = getNodetype().value

    @Deprecated("No-op for now")
    override fun normalize() {}

    actual override fun getAttributes(): NamedNodeMap?

    actual override fun hasChildNodes(): Boolean

    override fun isSupported(feature: String?, version: String?): Boolean = when (feature) {
        "Core", "XML" -> when (version) {
            "1.0", "2.0" -> true
            else -> false
        }

        else -> false
    }

    @IgnorableReturnValue
    override fun insertBefore(newChild: org.w3c.dom.Node, refChild: org.w3c.dom.Node?): org.w3c.dom.Node? = when (refChild) {
        null -> appendChild(newChild)
        else -> throw UnsupportedOperationException("insertBefore is not supported yet")
    }

    override fun setPrefix(prefix: String?) {
        throw UnsupportedOperationException("setPrefix is not supported yet")
    }

    override fun hasAttributes(): Boolean = false

    override fun getBaseURI(): String? = ownerDocument?.baseURI

    override fun compareDocumentPosition(other: org.w3c.dom.Node): Short {
        throw UnsupportedOperationException("compareDocumentPosition is not supported yet")
    }

    override fun isSameNode(other: org.w3c.dom.Node?): Boolean {
        return this == other
    }

    override fun isDefaultNamespace(namespaceURI: String?): Boolean {
        return lookupNamespaceURI("") == namespaceURI
    }

    override fun isEqualNode(arg: org.w3c.dom.Node): Boolean {
        TODO("not implemented")
    }

    override fun getFeature(feature: String?, version: String?): Any? {
        return null
    }

    override fun setUserData(
        key: String,
        data: Any?,
        handler: UserDataHandler?
    ): Any? {
        throw UnsupportedOperationException("UserData is not supported")
    }

    override fun getUserData(key: String): Any? {
        throw UnsupportedOperationException("UserData is not supported")
    }

    override fun getNamespaceURI(): String? = null

    override fun getPrefix(): String? = null

    override fun getLocalName(): String? = null

    @IgnorableReturnValue
    public actual override fun removeChild(node: PlatformNode): Node
}

@IgnorableReturnValue
@Deprecated("Use member", level = DeprecationLevel.HIDDEN)
public actual fun Node.appendChild(node: PlatformNode): Node {
    val n = node as? Node ?: getOwnerDocument()!!.let { d -> d.adoptNode(node = node) ?: d.importNode(node, true) }
    return appendChild(n)
}

@IgnorableReturnValue
@Deprecated("Use member", level = DeprecationLevel.HIDDEN)
public actual fun Node.replaceChild(
    newChild: PlatformNode,
    oldChild: Node
): Node {
    val n: Node = newChild as? Node ?: getOwnerDocument()!!.let { d -> d.adoptNode(node = newChild) ?: d.importNode(newChild, true) }
    return replaceChild(n, oldChild)
}

@IgnorableReturnValue
@Deprecated("Use member", level = DeprecationLevel.HIDDEN)
public actual fun Node.removeChild(node: PlatformNode): Node {
    val n = node as? Node ?: node.wrap()
    return removeChild(n)
}
