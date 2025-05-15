/*
 * Copyright (c) 2025.
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

internal actual fun Node.asAttr(): Attr = this as Attr

internal actual fun Node.asElement(): Element = this as Element

@Suppress("NOTHING_TO_INLINE")
public actual inline fun Node.appendChild(node: Node): Node = appendChild(node)

public actual fun Node.replaceChild(oldChild: Node, newChild: Node): Node =
    replaceChild(oldChild, newChild)

public actual fun Node.removeChild(node: Node): Node = removeChild(node)
