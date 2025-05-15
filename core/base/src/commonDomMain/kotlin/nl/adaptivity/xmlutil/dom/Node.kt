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

@file:Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")

package nl.adaptivity.xmlutil.dom

import nl.adaptivity.xmlutil.core.impl.idom.INodeList


@Suppress(
    "ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER_WARNING",
    "NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION_WARNING"
)
public actual interface Node {
    public val nodeType: Short

    public val nodeName: String

    public val ownerDocument: Document

    public val parentNode: Node?

    public val parentElement: Element? get() = parentNode as? Element?

    public val childNodes: INodeList

    public val firstChild: Node?

    public val lastChild: Node?

    public val previousSibling: Node?

    public val nextSibling: Node?

    public val textContent: String?

    public actual fun lookupPrefix(namespace: String): String?

    public actual fun lookupNamespaceURI(prefix: String): String?

    public fun appendChild(node: Node): Node

    public fun replaceChild(oldChild: Node, newChild: Node): Node

    public fun removeChild(node: Node): Node

    public companion object {
        // NodeType
        /**
         * The node is an `Element`.
         */
        public const val ELEMENT_NODE: Short = NodeConsts.ELEMENT_NODE

        /**
         * The node is an `Attr`.
         */
        public const val ATTRIBUTE_NODE: Short = NodeConsts.ATTRIBUTE_NODE

        /**
         * The node is a `Text` node.
         */
        public const val TEXT_NODE: Short = NodeConsts.TEXT_NODE

        /**
         * The node is a `CDATASection`.
         */
        public const val CDATA_SECTION_NODE: Short = NodeConsts.CDATA_SECTION_NODE

        /**
         * The node is an `EntityReference`.
         */
        @Suppress("DEPRECATION")
        @Deprecated("Legacy in the DOM standard")
        public const val ENTITY_REFERENCE_NODE: Short = NodeConsts.ENTITY_REFERENCE_NODE

        /**
         * The node is an `Entity`.
         */
        @Suppress("DEPRECATION")
        @Deprecated("Legacy in the DOM standard")
        public const val ENTITY_NODE: Short = NodeConsts.ENTITY_NODE

        /**
         * The node is a `ProcessingInstruction`.
         */
        public const val PROCESSING_INSTRUCTION_NODE: Short = NodeConsts.PROCESSING_INSTRUCTION_NODE

        /**
         * The node is a `Comment`.
         */
        public const val COMMENT_NODE: Short = NodeConsts.COMMENT_NODE

        /**
         * The node is a `Document`.
         */
        public const val DOCUMENT_NODE: Short = NodeConsts.DOCUMENT_NODE

        /**
         * The node is a `DocumentType`.
         */
        public const val DOCUMENT_TYPE_NODE: Short = NodeConsts.DOCUMENT_TYPE_NODE

        /**
         * The node is a `DocumentFragment`.
         */
        public const val DOCUMENT_FRAGMENT_NODE: Short = NodeConsts.DOCUMENT_FRAGMENT_NODE

        /**
         * The node is a `Notation`.
         */
        @Suppress("DEPRECATION")
        @Deprecated("Legacy in the DOM standard")
        public const val NOTATION_NODE: Short = NodeConsts.NOTATION_NODE

    }

}

public actual inline fun Node.appendChild(node: Node): Node = appendChild(node)
public actual fun Node.replaceChild(oldChild: Node, newChild: Node): Node = replaceChild(oldChild, newChild)
public actual fun Node.removeChild(node: Node): Node = removeChild(node)

public actual inline fun Node.getNodeType(): Short = nodeType

public actual inline fun Node.getNodeName(): String = nodeName

public actual inline fun Node.getOwnerDocument(): Document = ownerDocument

public actual inline fun Node.getParentNode(): Node? = parentNode

public actual inline fun Node.getTextContent(): String? = textContent

@Suppress("DEPRECATION")
public actual inline fun Node.getChildNodes(): NodeList = childNodes

public actual inline fun Node.getFirstChild(): Node? = firstChild

public actual inline fun Node.getLastChild(): Node? = lastChild

public actual inline fun Node.getPreviousSibling(): Node? = previousSibling

public actual inline fun Node.getNextSibling(): Node? = nextSibling

