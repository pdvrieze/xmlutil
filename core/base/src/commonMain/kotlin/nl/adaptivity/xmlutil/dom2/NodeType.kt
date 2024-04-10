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

package nl.adaptivity.xmlutil.dom2

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
