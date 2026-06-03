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

import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.dom2.Attr
import org.w3c.dom.Attr as DomAttr

@JsName("Attr")
public actual external interface PlatformAttr : PlatformNode {
/*
    public val namespaceURI: String?
    public val prefix: String?
    public val localName: String
    public val name: String
    public var value: String
    public val ownerElement: PlatformElement?
    override val ownerDocument: PlatformDocument
*/
}

@ExperimentalXmlUtilApi
public actual fun PlatformAttr.getNamespaceURI(): String? = when (this) {
    is Attr -> this.getNamespaceURI()
    else -> unsafeCast<DomAttr>().namespaceURI
}
@ExperimentalXmlUtilApi
public actual fun PlatformAttr.getName(): String = when (this) {
    is Attr -> this.getName()
    else -> unsafeCast<DomAttr>().name
}
@ExperimentalXmlUtilApi
public actual fun PlatformAttr.getLocalName(): String? = when (this) {
    is Attr -> this.getLocalName()
    else -> unsafeCast<DomAttr>().localName
}
@ExperimentalXmlUtilApi
public actual fun PlatformAttr.getPrefix(): String? = when (this) {
    is Attr -> this.getPrefix()
    else -> unsafeCast<DomAttr>().prefix
}
@ExperimentalXmlUtilApi
public actual fun PlatformAttr.getValue(): String = when (this) {
    is Attr -> this.getValue()
    else -> unsafeCast<DomAttr>().value
}

public fun DomAttr.isId(): Boolean = when (this) {
    is Attr -> isId()
    else if asDynamic().isId === undefined -> false
    else -> asDynamic().isId
}
