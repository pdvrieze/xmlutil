/*
 * Copyright (c) 2024-2025.
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

@file:Suppress("NOTHING_TO_INLINE")

package nl.adaptivity.xmlutil.dom

import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.core.impl.dom.unWrap

@Suppress(
    "ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER_WARNING",
    "NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION_WARNING",
)
public actual external interface PlatformNode {
    public val nodeType: Short
    public val nodeName: String
    public val baseURI: String
    public val ownerDocument: PlatformDocument?
    public val parentNode: PlatformNode?
    public val parentElement: PlatformElement?
    public val childNodes: PlatformNodeList
    public val firstChild: PlatformNode?
    public val lastChild: PlatformNode?
    public val previousSibling: PlatformNode?
    public val nextSibling: PlatformNode?
    public var nodeValue: String?
    public var textContent: String?

    public fun lookupPrefix(namespace: String): String?
    public fun lookupNamespaceURI(prefix: String): String?

}

public actual inline fun PlatformNode.getNodeType(): Short = nodeType
public actual inline fun PlatformNode.getNodeName(): String = nodeName
public actual inline fun PlatformNode.getOwnerDocument(): PlatformDocument = ownerDocument!!
public actual inline fun PlatformNode.getParentNode(): PlatformNode? = parentNode
public actual inline fun PlatformNode.getTextContent(): String? = textContent
public actual inline fun PlatformNode.getChildNodes(): PlatformNodeList = childNodes
public actual inline fun PlatformNode.getFirstChild(): PlatformNode? = firstChild
public actual inline fun PlatformNode.getLastChild(): PlatformNode? = lastChild
public actual inline fun PlatformNode.getPreviousSibling(): PlatformNode? = previousSibling
public actual inline fun PlatformNode.getNextSibling(): PlatformNode? = nextSibling


internal actual fun PlatformNode.asAttr(): PlatformAttr {
    check(getNodeType() == NodeConsts.ATTRIBUTE_NODE)
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    return this as PlatformAttr
}

internal actual fun PlatformNode.asElement(): PlatformElement {
    check(getNodeType() == NodeConsts.ELEMENT_NODE)
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    return this as PlatformElement
}

@XmlUtilInternal
public actual fun PlatformNode.appendChild(node: PlatformNode): PlatformNode {
    return asDynamic().appendChild(node.unWrap())
}

@XmlUtilInternal
public actual fun PlatformNode.replaceChild(oldChild: PlatformNode, newChild: PlatformNode): PlatformNode =
    asDynamic().replaceChild(oldChild.unWrap(), newChild.unWrap())

@XmlUtilInternal
public actual fun PlatformNode.removeChild(node: PlatformNode): PlatformNode =
    asDynamic().removeChild(node.unWrap())

@XmlUtilInternal
public actual inline fun PlatformNode.lookupPrefix(namespace: String): String? =
    lookupPrefix(namespace)

@XmlUtilInternal
public actual inline fun PlatformNode.lookupNamespaceURI(prefix: String): String? =
    lookupNamespaceURI(prefix)
