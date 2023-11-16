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

package nl.adaptivity.xmlutil.dom

public actual interface Attr : Node {
    public val namespaceURI: String?
    public val prefix: String?
    public val localName: String
    public val name: String
    public var value: String

    public val ownerElement: Element?
}

@Suppress("NOTHING_TO_INLINE")
public actual inline fun Attr.getNamespaceURI(): String? = namespaceURI
@Suppress("NOTHING_TO_INLINE")
public actual inline fun Attr.getPrefix(): String? = prefix
@Suppress("NOTHING_TO_INLINE")
public actual inline fun Attr.getLocalName(): String? = localName
@Suppress("NOTHING_TO_INLINE")
public actual inline fun Attr.getName(): String = name
@Suppress("NOTHING_TO_INLINE")
public actual inline fun Attr.getValue(): String = value
@Suppress("NOTHING_TO_INLINE")
public actual inline fun Attr.setValue(value: String) { this.value = value }
@Suppress("NOTHING_TO_INLINE")
public actual inline fun Attr.getOwnerElement(): Element? = ownerElement
