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

@file:Suppress("DEPRECATION", "EXTENSION_SHADOWED_BY_MEMBER", "KotlinRedundantDiagnosticSuppress")

package nl.adaptivity.xmlutil.dom

import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.util.isElement

@Deprecated(
    "No longer supported, use dom2 instead",
    ReplaceWith("nl.adaptivity.xmlutil.dom2.Node", "nl.adaptivity.xmlutil.dom2")
)
public expect interface PlatformNode

@XmlUtilInternal
public expect fun PlatformNode.appendChild(node: PlatformNode): PlatformNode
@XmlUtilInternal
public expect fun PlatformNode.replaceChild(oldChild: PlatformNode, newChild: PlatformNode): PlatformNode
@XmlUtilInternal
public expect fun PlatformNode.removeChild(node: PlatformNode): PlatformNode
@XmlUtilInternal
public expect fun PlatformNode.lookupPrefix(namespace: String): String?
@XmlUtilInternal
public expect fun PlatformNode.lookupNamespaceURI(prefix: String): String?

public expect inline fun PlatformNode.getNodeType(): Short
public expect inline fun PlatformNode.getNodeName(): String
public expect inline fun PlatformNode.getOwnerDocument(): PlatformDocument
public expect inline fun PlatformNode.getParentNode(): PlatformNode?
public expect inline fun PlatformNode.getTextContent(): String?
public expect inline fun PlatformNode.getChildNodes(): PlatformNodeList
public expect inline fun PlatformNode.getFirstChild(): PlatformNode?
public expect inline fun PlatformNode.getLastChild(): PlatformNode?
public expect inline fun PlatformNode.getPreviousSibling(): PlatformNode?
public expect inline fun PlatformNode.getNextSibling(): PlatformNode?

internal expect fun PlatformNode.asAttr(): PlatformAttr
internal expect fun PlatformNode.asElement(): PlatformElement

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

public fun PlatformNode.getParentElement(): PlatformElement? = getParentNode()?.takeIf(PlatformNode::isElement)?.asElement()

