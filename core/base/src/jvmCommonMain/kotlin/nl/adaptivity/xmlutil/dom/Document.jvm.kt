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

package nl.adaptivity.xmlutil.dom

import nl.adaptivity.xmlutil.core.impl.dom.wrap
import nl.adaptivity.xmlutil.dom2.Document as Document2
import nl.adaptivity.xmlutil.dom2.Node as Node2

@Suppress(
    "ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER_WARNING",
    "ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_SUPERTYPES_AS_NON_FINAL_EXPECT_CLASSIFIER_WARNING"
)
public actual typealias PlatformDocument = org.w3c.dom.Document


public actual fun PlatformDocument.importNode(node: PlatformNode, deep: Boolean): PlatformNode =
    importNode(node, deep)

public actual fun PlatformDocument.adoptNode(node: PlatformNode): PlatformNode =
    adoptNode(node)

public actual fun PlatformDocument.createAttribute(localName: String): PlatformAttr =
    createAttribute(localName)

public actual fun PlatformDocument.createAttributeNS(namespace: String?, qualifiedName: String): PlatformAttr =
    createAttributeNS(namespace, qualifiedName)

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
