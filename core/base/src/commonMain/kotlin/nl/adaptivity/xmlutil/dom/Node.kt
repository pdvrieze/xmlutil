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

@file:Suppress("DEPRECATION", "EXTENSION_SHADOWED_BY_MEMBER", "KotlinRedundantDiagnosticSuppress")

package nl.adaptivity.xmlutil.dom

import nl.adaptivity.xmlutil.util.isElement
import kotlin.jvm.JvmName

@Deprecated(
    "No longer supported, use dom2 instead",
    ReplaceWith("nl.adaptivity.xmlutil.dom2.Node", "nl.adaptivity.xmlutil.dom2")
)
public expect interface Node {

    public fun lookupPrefix(namespace: String): String?

    public fun lookupNamespaceURI(prefix: String): String?

    public fun appendChild(node: Node): Node

    public fun replaceChild(oldChild: Node, newChild: Node): Node

    public fun removeChild(node: Node): Node

}

public expect inline fun Node.getNodeType(): Short
public expect inline fun Node.getNodeName(): String
public expect inline fun Node.getOwnerDocument(): Document
public expect inline fun Node.getParentNode(): Node?
public expect inline fun Node.getTextContent(): String?
public expect inline fun Node.getChildNodes(): NodeList
public expect inline fun Node.getFirstChild(): Node?
public expect inline fun Node.getLastChild(): Node?
public expect inline fun Node.getPreviousSibling(): Node?
public expect inline fun Node.getNextSibling(): Node?

@Deprecated("Use accessor method", ReplaceWith("getNodeType()"))
public inline val Node.nodeType: Short get() = getNodeType()
@Deprecated("Use accessor method", ReplaceWith("getNodeName()"))
public inline val Node.nodeName: String get() = getNodeName()
@Deprecated("Use accessor method", ReplaceWith("getOwnerDocument()"))
public inline val Node.ownerDocument: Document get() = getOwnerDocument()
@Deprecated("Use accessor method", ReplaceWith("getParentNode()"))
public inline val Node.parentNode: Node? get() = getParentNode()
@Deprecated("Use accessor method", ReplaceWith("getTextContent()"))
public inline val Node.textContent: String? get() = getTextContent()
@Deprecated("Use accessor method", ReplaceWith("getChildNodes()"))
public inline val Node.childNodes: NodeList get() = getChildNodes()
@Deprecated("Use accessor method", ReplaceWith("getFirstChild()"))
public inline val Node.firstChild: Node? get() = getFirstChild()
@Deprecated("Use accessor method", ReplaceWith("getLastChild()"))
public inline val Node.lastChild: Node? get() = getLastChild()
@Deprecated("Use accessor method", ReplaceWith("getPreviousSibling()"))
public inline val Node.previousSibling: Node? get() = getPreviousSibling()
@Deprecated("Use accessor method", ReplaceWith("getNextSibling()"))
public inline val Node.nextSibling: Node? get() = getNextSibling()

internal expect fun Node.asAttr(): Attr
internal expect fun Node.asElement(): Element

public object NodeConsts {
    // NodeType
    /**
     * The node is an `Element`.
     */
    public const val ELEMENT_NODE: Short = 1

    /**
     * The node is an `Attr`.
     */
    public const val ATTRIBUTE_NODE: Short = 2

    /**
     * The node is a `Text` node.
     */
    public const val TEXT_NODE: Short = 3

    /**
     * The node is a `CDATASection`.
     */
    public const val CDATA_SECTION_NODE: Short = 4

    /**
     * The node is an `EntityReference`.
     */
    @Deprecated("Legacy in the DOM standard")
    public const val ENTITY_REFERENCE_NODE: Short = 5

    /**
     * The node is an `Entity`.
     */
    @Deprecated("Legacy in the DOM standard")
    public const val ENTITY_NODE: Short = 6

    /**
     * The node is a `ProcessingInstruction`.
     */
    public const val PROCESSING_INSTRUCTION_NODE: Short = 7

    /**
     * The node is a `Comment`.
     */
    public const val COMMENT_NODE: Short = 8

    /**
     * The node is a `Document`.
     */
    public const val DOCUMENT_NODE: Short = 9

    /**
     * The node is a `DocumentType`.
     */
    public const val DOCUMENT_TYPE_NODE: Short = 10

    /**
     * The node is a `DocumentFragment`.
     */
    public const val DOCUMENT_FRAGMENT_NODE: Short = 11

    /**
     * The node is a `Notation`.
     */
    @Deprecated("Legacy in the DOM standard")
    public const val NOTATION_NODE: Short = 12

}

public fun Node.getParentElement(): Element? = getParentNode()?.takeIf(Node::isElement)?.asElement()

@Deprecated("Use accessor methods for dom2 compatibility", ReplaceWith("getParentElement()"))
@get:JvmName("parentElement")
public val Node.parentElement: Element? get() = getParentElement()
