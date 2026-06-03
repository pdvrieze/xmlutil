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
/*
    public val implementation: PlatformDOMImplementation
    public val doctype: PlatformDocumentType?
    public val documentElement: PlatformElement?
    public val inputEncoding: String?
*/

    public fun createElement(localName: String): PlatformElement

    public fun createElementNS(namespaceURI: String, qualifiedName: String): PlatformElement

    public fun createDocumentFragment(): PlatformDocumentFragment

    public fun createTextNode(data: String): PlatformText

    public fun createCDATASection(data: String): PlatformCDATASection

    public fun createComment(data: String): PlatformComment

    public fun createProcessingInstruction(target: String, data: String): PlatformProcessingInstruction
    public fun importNode(node: PlatformNode, deep: Boolean): PlatformNode

    public fun adoptNode(node: PlatformNode): PlatformNode?

    public fun createAttribute(localName: String): PlatformAttr

    public fun createAttributeNS(namespace: String?, qualifiedName: String): PlatformAttr
}

public actual val PlatformDocument.childNodes: PlatformNodeList
    get() = when (this) {
        is Document -> getChildNodes()
        else -> unsafeCast<DomDocument>().childNodes.asDynamic()
    }
public actual fun Document.adoptNode(node: PlatformNode): Node2? = adoptNode(node.wrap())
