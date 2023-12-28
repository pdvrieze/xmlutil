/*
 * Copyright (c) 2023.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

@file:Suppress("DEPRECATION")

package nl.adaptivity.xmlutil.dom

@Deprecated(
    "No longer supported, use dom2 instead",
    ReplaceWith("nl.adaptivity.xmlutil.dom2.Attr", "nl.adaptivity.xmlutil.dom2")
)
public expect interface Attr : Node

public expect inline fun Attr.getNamespaceURI(): String?
public expect inline fun Attr.getPrefix(): String?
public expect inline fun Attr.getLocalName(): String?
public expect inline fun Attr.getName(): String
public expect inline fun Attr.getValue(): String
public expect inline fun Attr.setValue(value: String)
public expect inline fun Attr.getOwnerElement(): Element?

@Deprecated("Use accessor methods for dom2 compatibility", ReplaceWith("getNamespaceURI()"))
public inline val Attr.namespaceURI: String?
    get() = getNamespaceURI()

@Deprecated("Use accessor methods for dom2 compatibility", ReplaceWith("getPrefix()"))
public inline val Attr.prefix: String?
    get() = getPrefix()

@Deprecated("Use accessor methods for dom2 compatibility", ReplaceWith("getLocalName()"))
public inline val Attr.localName: String?
    get() = getLocalName()

@Deprecated("Use accessor methods for dom2 compatibility", ReplaceWith("getName()"))
public inline val Attr.name: String
    get() = getName()

@Deprecated("Use accessor methods for dom2 compatibility")
public inline var Attr.value: String
    get() = getValue()
    set(value) = setValue(value)

@Deprecated("Use accessor methods for dom2 compatibility", ReplaceWith("getOwnerElement()"))
public inline val Attr.ownerElement: Element?
    get() = getOwnerElement()
