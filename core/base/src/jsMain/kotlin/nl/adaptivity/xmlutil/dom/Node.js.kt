/*
 * Copyright (c) 2024.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

@file:Suppress("NOTHING_TO_INLINE")

package nl.adaptivity.xmlutil.dom

import nl.adaptivity.xmlutil.core.impl.dom.unWrap

@Suppress(
    "ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER_WARNING",
    "NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION_WARNING",
)
public actual external interface Node {
    public val nodeType: Short
    public val nodeName: String
    public val baseURI: String
    public val ownerDocument: Document?
    public val parentNode: Node?
    public val parentElement: Element?
    public val childNodes: NodeList
    public val firstChild: Node?
    public val lastChild: Node?
    public val previousSibling: Node?
    public val nextSibling: Node?
    public var nodeValue: String?
    public var textContent: String?

    public actual fun lookupPrefix(namespace: String): String?
    public actual fun lookupNamespaceURI(prefix: String): String?

}

public actual inline fun Node.getNodeType(): Short = nodeType
public actual inline fun Node.getNodeName(): String = nodeName
public actual inline fun Node.getOwnerDocument(): Document = ownerDocument!!
public actual inline fun Node.getParentNode(): Node? = parentNode
public actual inline fun Node.getTextContent(): String? = textContent
public actual inline fun Node.getChildNodes(): NodeList = childNodes
public actual inline fun Node.getFirstChild(): Node? = firstChild
public actual inline fun Node.getLastChild(): Node? = lastChild
public actual inline fun Node.getPreviousSibling(): Node? = previousSibling
public actual inline fun Node.getNextSibling(): Node? = nextSibling


internal actual fun Node.asAttr(): Attr {
    check(getNodeType() == NodeConsts.ATTRIBUTE_NODE)
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    return this as Attr
}

internal actual fun Node.asElement(): Element {
    check(getNodeType() == NodeConsts.ELEMENT_NODE)
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    return this as Element
}

public actual fun Node.appendChild(node: Node): Node {
    return asDynamic().appendChild(node.unWrap())
}

public actual fun Node.replaceChild(oldChild: Node, newChild: Node): Node =
    asDynamic().replaceChild(oldChild.unWrap(), newChild.unWrap())

public actual fun Node.removeChild(node: Node): Node =
    asDynamic().removeChild(node.unWrap())
