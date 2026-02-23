/*
 * Copyright (c) 2026.
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

package org.w3.dom

import nl.adaptivity.xmlutil.dom2.Element
import nl.adaptivity.xmlutil.dom2.Node

fun Node.nthElement(n: Int): Element {
    var pos = 0
    var node: Node? = getFirstChild()?: throw IndexOutOfBoundsException("No children in node")
    while (node != null) {
        if (node is Element) if (pos == n) return node else ++pos
        node = node.getNextSibling()
    }
    throw IndexOutOfBoundsException("No element found for index $n (only $pos elements found)")
}
