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

import nl.adaptivity.xmlutil.core.impl.dom.wrap
import nl.adaptivity.xmlutil.dom2.Document as Document2
import nl.adaptivity.xmlutil.dom2.Node as Node2

@Suppress(
    "ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER_WARNING",
    "NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION_WARNING",
)
public actual external interface PlatformDocument : PlatformNode {
    public val implementation: PlatformDOMImplementation
    public val doctype: PlatformDocumentType?
    public val documentElement: PlatformElement?
    public val inputEncoding: String

    public fun createElement(localName: String): PlatformElement

    public fun createElementNS(namespaceURI: String, qualifiedName: String): PlatformElement

    public fun createDocumentFragment(): PlatformDocumentFragment

    public fun createTextNode(data: String): PlatformText

    public fun createCDATASection(data: String): PlatformCDATASection

    public fun createComment(data: String): PlatformComment

    public fun createProcessingInstruction(target: String, data: String): PlatformProcessingInstruction
    public fun importNode(node: PlatformNode, deep: Boolean): PlatformNode

    public fun adoptNode(node: PlatformNode): PlatformNode

    public fun createAttribute(localName: String): PlatformAttr

    public fun createAttributeNS(namespace: String?, qualifiedName: String): PlatformAttr
}

public actual fun PlatformDocument.importNode(node: PlatformNode, deep: Boolean): PlatformNode =
    importNode(node, deep)

public actual fun PlatformDocument.adoptNode(node: PlatformNode): PlatformNode =
    adoptNode(node)

public actual fun PlatformDocument.createAttribute(localName: String): PlatformAttr =
    createAttribute(localName)

public actual fun PlatformDocument.createAttributeNS(namespace: String?, qualifiedName: String): PlatformAttr =
    createAttributeNS(namespace, qualifiedName)

public actual inline fun PlatformDocument.getImplementation(): PlatformDOMImplementation = implementation
public actual inline fun PlatformDocument.getDoctype(): PlatformDocumentType? = doctype
public actual inline fun PlatformDocument.getDocumentElement(): PlatformElement? = documentElement
public actual inline fun PlatformDocument.getInputEncoding(): String? = inputEncoding
public actual val PlatformDocument.supportsWhitespaceAtToplevel: Boolean get() = true

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public actual fun PlatformDocument.createElement(localName: String): PlatformElement = createElement(localName)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public actual fun PlatformDocument.createElementNS(namespaceURI: String, qualifiedName: String): PlatformElement =
    createElementNS(namespaceURI, qualifiedName)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public actual fun PlatformDocument.createDocumentFragment(): PlatformDocumentFragment =
    createDocumentFragment()

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public actual fun PlatformDocument.createTextNode(data: String): PlatformText =
    createTextNode(data)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public actual fun PlatformDocument.createCDATASection(data: String): PlatformCDATASection =
    createCDATASection(data)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public actual fun PlatformDocument.createComment(data: String): PlatformComment =
    createComment(data)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public actual fun PlatformDocument.createProcessingInstruction(target: String, data: String): PlatformProcessingInstruction =
    createProcessingInstruction(target, data)

public actual fun Document2.adoptNode(node: PlatformNode): Node2 = node.wrap()
