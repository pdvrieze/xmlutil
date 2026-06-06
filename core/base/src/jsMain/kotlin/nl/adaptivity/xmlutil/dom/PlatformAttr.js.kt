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
import org.w3c.dom.Attr as DomAttr

@JsName("Attr")
public actual external interface PlatformAttr : PlatformNode

@ExperimentalXmlUtilApi
public actual fun PlatformAttr.getNamespaceURI(): String? = unsafeCast<DomAttr>().namespaceURI

@ExperimentalXmlUtilApi
public actual fun PlatformAttr.getName(): String = unsafeCast<DomAttr>().name

@ExperimentalXmlUtilApi
public actual fun PlatformAttr.getLocalName(): String? = unsafeCast<DomAttr>().localName

@ExperimentalXmlUtilApi
public actual fun PlatformAttr.getPrefix(): String? = unsafeCast<DomAttr>().prefix

@ExperimentalXmlUtilApi
public actual fun PlatformAttr.getValue(): String = unsafeCast<DomAttr>().value

public fun DomAttr.isId(): Boolean = when {
    asDynamic().isId === undefined -> false
    else -> asDynamic().isId
}
