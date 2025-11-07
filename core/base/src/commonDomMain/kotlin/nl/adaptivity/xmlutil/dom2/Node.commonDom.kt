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

@file:Suppress("ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT")

package nl.adaptivity.xmlutil.dom2

import nl.adaptivity.xmlutil.dom.PlatformNode

public actual typealias Node = PlatformNode

public actual fun Node.appendChild(node: PlatformNode): Node {
    return appendChild(node) // child member
}

public actual fun Node.replaceChild(
    newChild: PlatformNode,
    oldChild: Node
): Node {
    return replaceChild(newChild, oldChild) // child member
}

public actual fun Node.removeChild(node: PlatformNode): Node {
    return removeChild(node)
}
