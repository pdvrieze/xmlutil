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

package nl.adaptivity.xmlutil.dom

import nl.adaptivity.xmlutil.core.impl.wrappingDom.wrap
import nl.adaptivity.xmlutil.dom2.Document
import nl.adaptivity.xmlutil.dom2.Node as Node2
import org.w3c.dom.Document as DomDocument

@JsName("Document")
public actual external interface PlatformDocument : PlatformNode {

    @JsName("createElement")
    public fun createElement(localName: String): PlatformElement

    @JsName("createElementNS")
    public fun createElementNS(namespaceURI: String, qualifiedName: String): PlatformElement

    @JsName("createDocumentFragment")
    public fun createDocumentFragment(): PlatformDocumentFragment

    @JsName("createTextNode")
    public fun createTextNode(data: String): PlatformText

    @JsName("createCDATASection")
    public fun createCDATASection(data: String): PlatformCDATASection

    @JsName("createComment")
    public fun createComment(data: String): PlatformComment

    @JsName("createProcessingInstruction")
    public fun createProcessingInstruction(target: String, data: String): PlatformProcessingInstruction

    @JsName("importNode")
    public fun importNode(node: PlatformNode, deep: Boolean): PlatformNode

    @JsName("adoptNode")
    public fun adoptNode(node: PlatformNode): PlatformNode?

    @JsName("createAttribute")
    public fun createAttribute(localName: String): PlatformAttr

    @JsName("createAttributeNS")
    public fun createAttributeNS(namespace: String?, qualifiedName: String): PlatformAttr
}

public actual val PlatformDocument.childNodes: PlatformNodeList
    get() = unsafeCast<DomDocument>().childNodes.asDynamic()

public actual fun Document.adoptNode(node: PlatformNode): Node2? = adoptNode(node.wrap())
