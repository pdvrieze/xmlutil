/*
 * Copyright (c) 2022.
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

@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER")

package nl.adaptivity.xmlutil.dom

public expect interface Attr : Node

public expect inline fun Attr.getNamespaceURI(): String?
public expect inline fun Attr.getPrefix(): String?
public expect inline fun Attr.getLocalName(): String?
public expect inline fun Attr.getName(): String
public expect inline fun Attr.getValue(): String
public expect inline fun Attr.setValue(value: String)
public expect inline fun Attr.getOwnerElement(): Element?

public inline val Attr.namespaceURI: String?
    get() = getNamespaceURI()

public inline val Attr.prefix: String?
    get() = getPrefix()

public inline val Attr.localName: String?
    get() = getLocalName()

public inline val Attr.name: String
    get() = getName()

public inline var Attr.value: String
    get() = getValue()
    set(value) = setValue(value)

public inline val Attr.ownerElement: Element?
    get() = getOwnerElement()
