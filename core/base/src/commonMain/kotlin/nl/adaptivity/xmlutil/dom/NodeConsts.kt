/*
 * Copyright (c) 2025.
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

package nl.adaptivity.xmlutil.dom

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
