/*
 * Copyright (c) 2024-2026.
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

@file:Suppress(
    "NOTHING_TO_INLINE", "EXTENSION_SHADOWED_BY_MEMBER",
    "ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER_WARNING",
    "ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_SUPERTYPES_AS_NON_FINAL_EXPECT_CLASSIFIER_WARNING"
)

package nl.adaptivity.xmlutil.dom

public actual typealias PlatformNode = org.w3c.dom.Node
public actual val PlatformNode.ownerDocument: PlatformDocument? get() = this.ownerDocument

public actual typealias PlatformAttr = org.w3c.dom.Attr

public actual fun PlatformAttr.getNamespaceURI(): String? = getNamespaceURI()
public actual fun PlatformAttr.getLocalName(): String? = getLocalName()
public actual fun PlatformAttr.getPrefix(): String? = getPrefix()
public actual fun PlatformAttr.getValue(): String = getValue()


public actual typealias PlatformDocumentFragment = org.w3c.dom.DocumentFragment

public actual typealias PlatformElement = org.w3c.dom.Element

public actual val PlatformElement.namespaceURI: String? get() = getNamespaceURI()
public actual val PlatformElement.prefix: String? get() = getPrefix()
public actual val PlatformElement.localName: String get() = getLocalName()

public actual typealias PlatformText = org.w3c.dom.Text

public actual typealias PlatformCharacterData = org.w3c.dom.CharacterData
public actual fun PlatformCharacterData.getData(): String = data

public actual typealias PlatformCDATASection = org.w3c.dom.CDATASection

public actual typealias PlatformComment = org.w3c.dom.Comment

public actual typealias PlatformProcessingInstruction = org.w3c.dom.ProcessingInstruction
public actual fun PlatformProcessingInstruction.getNodeName(): String = target
public actual fun PlatformProcessingInstruction.getData(): String = data

public actual typealias PlatformDOMImplementation = org.w3c.dom.DOMImplementation

public actual typealias PlatformDocumentType = org.w3c.dom.DocumentType
public actual fun PlatformDocumentType.getOwnerDocument(): PlatformDocument? = ownerDocument
public actual fun PlatformDocumentType.getName(): String = name
public actual fun PlatformDocumentType.getPublicId(): String = publicId
public actual fun PlatformDocumentType.getSystemId(): String = systemId

public actual typealias PlatformNamedNodeMap = org.w3c.dom.NamedNodeMap

public actual typealias PlatformNodeList = org.w3c.dom.NodeList


