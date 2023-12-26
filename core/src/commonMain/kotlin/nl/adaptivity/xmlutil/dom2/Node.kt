/*
 * Copyright (c) 2022.
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

package nl.adaptivity.xmlutil.dom2

import kotlinx.serialization.Serializable

@Serializable(NodeSerializer::class)
public interface Node {
    public val nodetype : NodeType
    public fun getNodeType(): Short = nodetype.value
    public fun getNodeName(): String
    public fun getOwnerDocument(): Document
    public fun getParentNode(): Node?
    public fun getTextContent(): String?
    public fun setTextContent(value: String)
    public fun getChildNodes(): NodeList
    public fun getFirstChild(): Node?
    public fun getLastChild(): Node?
    public fun getPreviousSibling(): Node?
    public fun getNextSibling(): Node?
    public fun getParentElement(): Element? = parentNode as? Element

    public fun lookupPrefix(namespace: String): String?

    public fun lookupNamespaceURI(prefix: String): String?

    public fun appendChild(node: Node): Node

    public fun replaceChild(oldChild: Node, newChild: Node): Node

    public fun removeChild(node: Node): Node

}

public enum class NodeType(public val value: Short) {
    /**
     * The node is an `Element`.
     */
    ELEMENT_NODE(1),

    /**
     * The node is an `Attr`.
     */
    ATTRIBUTE_NODE(2),

    /**
     * The node is a `Text` node.
     */
    TEXT_NODE(3),

    /**
     * The node is a `CDATASection`.
     */
    CDATA_SECTION_NODE(4),

    /**
     * The node is an `EntityReference`.
     */
    @Deprecated("Legacy in the DOM standard")
    ENTITY_REFERENCE_NODE(5),

    /**
     * The node is an `Entity`.
     */
    @Deprecated("Legacy in the DOM standard")
    ENTITY_NODE(6),

    /**
     * The node is a `ProcessingInstruction`.
     */
    PROCESSING_INSTRUCTION_NODE(7),

    /**
     * The node is a `Comment`.
     */
    COMMENT_NODE(8),

    /**
     * The node is a `Document`.
     */
    DOCUMENT_NODE(9),

    /**
     * The node is a `DocumentType`.
     */
    DOCUMENT_TYPE_NODE(10),

    /**
     * The node is a `DocumentFragment`.
     */
    DOCUMENT_FRAGMENT_NODE(11),

    /**
     * The node is a `Notation`.
     */
    @Deprecated("Legacy in the DOM standard")
    NOTATION_NODE(12);

    public companion object {
        @Suppress("DEPRECATION")
        public operator fun invoke(v: Short): NodeType = when (v.toInt()) {
            1 -> ELEMENT_NODE
            2 -> ATTRIBUTE_NODE
            3 -> TEXT_NODE
            4 -> CDATA_SECTION_NODE
            5 -> ENTITY_REFERENCE_NODE
            6 -> ENTITY_NODE
            7 -> PROCESSING_INSTRUCTION_NODE
            8 -> COMMENT_NODE
            9 -> DOCUMENT_NODE
            10 -> DOCUMENT_TYPE_NODE
            11 -> DOCUMENT_FRAGMENT_NODE
            12 -> NOTATION_NODE

            else -> throw IllegalArgumentException("Unsupported node type: $v")
        }
    }

}

public inline val Node.nodeType: Short get() = getNodeType()
public inline val Node.nodeName: String get() = getNodeName()
public inline val Node.ownerDocument: Document get() = getOwnerDocument()
public inline val Node.parentNode: Node? get() = getParentNode()
public inline val Node.textContent: String? get() = getTextContent()
public inline val Node.childNodes: NodeList get() = getChildNodes()
public inline val Node.firstChild: Node? get() = getFirstChild()
public inline val Node.lastChild: Node? get() = getLastChild()
public inline val Node.previousSibling: Node? get() = getPreviousSibling()
public inline val Node.nextSibling: Node? get() = getNextSibling()
