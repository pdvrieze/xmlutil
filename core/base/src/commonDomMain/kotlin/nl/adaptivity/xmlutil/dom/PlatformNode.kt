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

@file:Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")

package nl.adaptivity.xmlutil.dom

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.dom2.NodeList
import nl.adaptivity.xmlutil.dom2.NodeSerializer
import nl.adaptivity.xmlutil.dom2.NodeType


@Suppress(
    "ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER_WARNING",
    "NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION_WARNING"
)
@Serializable(with = NodeSerializer::class)
public actual interface PlatformNode {
    public fun getNodetype(): NodeType

    public fun getNodeName(): String

    public fun getOwnerDocument(): PlatformDocument

    public fun getParentNode(): PlatformNode?

    public fun getParentElement(): PlatformElement?
        //= getParentNode() as? PlatformElement?

    public fun getChildNodes(): NodeList

    public fun getFirstChild(): PlatformNode?

    public fun getLastChild(): PlatformNode?

    public fun getPreviousSibling(): PlatformNode?

    public fun getNextSibling(): PlatformNode?

    public fun getTextContent(): String?
    public fun setTextContent(value: String)

    public fun lookupPrefix(namespace: String): String?

    public fun lookupNamespaceURI(prefix: String): String?

    public fun appendChild(node: PlatformNode): PlatformNode

    public fun replaceChild(oldChild: PlatformNode, newChild: PlatformNode): PlatformNode

    public fun removeChild(node: PlatformNode): PlatformNode

//    public fun getNodeType(): Short = nodeType.value

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

