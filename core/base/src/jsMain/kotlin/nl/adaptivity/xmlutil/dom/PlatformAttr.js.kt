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

import org.w3c.dom.Attr as DomAttr

@JsName("Attr")
public actual external interface PlatformAttr : PlatformNode {
    public val namespaceURI: String?
    public val prefix: String?
    public val localName: String
    public val name: String
    public var value: String
    public val ownerElement: PlatformElement?
    override val ownerDocument: PlatformDocument
}

public actual fun PlatformAttr.getNamespaceURI(): String? = namespaceURI
public actual fun PlatformAttr.getName(): String = name
public actual fun PlatformAttr.getLocalName(): String? = localName
public actual fun PlatformAttr.getPrefix(): String? = prefix
public actual fun PlatformAttr.getValue(): String = value

public fun DomAttr.isId(): Boolean = when {
    asDynamic().isId === undefined -> false
    else -> asDynamic().isId
}
