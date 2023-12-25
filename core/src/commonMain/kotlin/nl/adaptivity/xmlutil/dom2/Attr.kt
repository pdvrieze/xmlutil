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

package nl.adaptivity.xmlutil.dom2

public interface Attr : Node {
    public fun getNamespaceURI(): String?

    public fun getPrefix(): String?

    public fun getLocalName(): String?

    public fun getName(): String

    public fun getValue(): String

    public fun setValue(value: String)

    public fun getOwnerElement(): Element?
}

public val Attr.namespaceURI: String? get() = getNamespaceURI()
public val Attr.prefix: String? get() = getPrefix()
public val Attr.localName: String? get() = getLocalName()
public val Attr.name: String get() = getName()
public var Attr.value: String
    get() = getValue()
    set(value) { setValue(value) }
public val Attr.ownerElement: Element? get() = getOwnerElement()
