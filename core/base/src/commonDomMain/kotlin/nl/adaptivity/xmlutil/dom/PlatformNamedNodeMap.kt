/*
 * Copyright (c) 2024-2025.
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

@file:Suppress("NOTHING_TO_INLINE")

package nl.adaptivity.xmlutil.dom

public actual interface PlatformNamedNodeMap {
    public fun item(index: Int): PlatformNode?

    public fun getNamedItem(qualifiedName: String): PlatformNode?

    public fun getNamedItemNS(namespace: String?, localName: String): PlatformNode?

    public fun setNamedItem(attr: PlatformNode): PlatformNode?

    public fun setNamedItemNS(attr: PlatformNode): PlatformNode?

    public fun removeNamedItem(qualifiedName: String): PlatformNode?

    public fun removeNamedItemNS(namespace: String?, localName: String): PlatformNode?
}

public inline operator fun PlatformNamedNodeMap.get(index:Int): PlatformNode? = item(index)


