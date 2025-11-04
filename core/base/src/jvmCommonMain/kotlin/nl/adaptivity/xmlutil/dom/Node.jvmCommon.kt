/*
 * Copyright (c) 2025.
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

internal actual fun PlatformNode.asAttr(): PlatformAttr = this as PlatformAttr

internal actual fun PlatformNode.asElement(): PlatformElement = this as PlatformElement

@Suppress("NOTHING_TO_INLINE")
public actual inline fun PlatformNode.appendChild(node: PlatformNode): PlatformNode = appendChild(node)

public actual fun PlatformNode.replaceChild(oldChild: PlatformNode, newChild: PlatformNode): PlatformNode =
    replaceChild(oldChild, newChild)

public actual fun PlatformNode.removeChild(node: PlatformNode): PlatformNode = removeChild(node)

public actual fun PlatformNode.lookupPrefix(namespace: String): String? =
    lookupPrefix(namespace)

public actual fun PlatformNode.lookupNamespaceURI(prefix: String): String? =
    lookupNamespaceURI(prefix)
