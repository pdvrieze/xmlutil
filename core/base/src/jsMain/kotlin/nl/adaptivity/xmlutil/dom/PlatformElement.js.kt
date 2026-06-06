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

import org.w3c.dom.Element as DomElement

@JsName("Element")
public actual external interface PlatformElement : PlatformNode {

    @JsName("getAttribute")
    public fun getAttribute(qualifiedName: String): String?

    @JsName("getAttributeNS")
    public fun getAttributeNS(namespace: String?, localName: String): String?

    @JsName("setAttribute")
    public fun setAttribute(qualifiedName: String, value: String)

    @JsName("setAttributeNS")
    public fun setAttributeNS(namespace: String?, cName: String, value: String)

    @JsName("removeAttribute")
    public fun removeAttribute(qualifiedName: String)

    @JsName("removeAttributeNS")
    public fun removeAttributeNS(namespace: String?, localName: String)

    @JsName("hasAttribute")
    public fun hasAttribute(qualifiedName: String): Boolean

    @JsName("hasAttributeNS")
    public fun hasAttributeNS(namespace: String?, localName: String): Boolean

    @JsName("getAttributeNode")
    public fun getAttributeNode(qualifiedName: String): PlatformAttr?

    @JsName("getAttributeNodeNS")
    public fun getAttributeNodeNS(namespace: String?, localName: String): PlatformAttr?

    @JsName("setAttributeNode")
    public fun setAttributeNode(attr: PlatformAttr): PlatformAttr?

    @JsName("setAttributeNodeNS")
    public fun setAttributeNodeNS(attr: PlatformAttr): PlatformAttr?

    @JsName("removeAttributeNode")
    public fun removeAttributeNode(attr: PlatformAttr): PlatformAttr

    @JsName("getElementsByTagName")
    public fun getElementsByTagName(qualifiedName: String): PlatformNodeList

    @JsName("getElementsByTagNameNS")
    public fun getElementsByTagNameNS(namespace: String?, localName: String): PlatformNodeList
}


public actual val PlatformElement.namespaceURI: String?
    get() = unsafeCast<DomElement>().namespaceURI

public actual val PlatformElement.prefix: String?
    get() = unsafeCast<DomElement>().prefix

public actual val PlatformElement.name: String
    get() = unsafeCast<DomElement>().tagName

public actual val PlatformElement.localName: String
    get() = unsafeCast<DomElement>().localName

public actual val PlatformElement.attributes: PlatformNamedNodeMap
    get() = unsafeCast<DomElement>().attributes.asDynamic()

public actual val PlatformElement.childNodes: PlatformNodeList
    get() = unsafeCast<DomElement>().childNodes.asDynamic()
