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

package nl.adaptivity.xmlutil.core.impl.wrappingDom

import nl.adaptivity.xmlutil.dom2.NodeList
import nl.adaptivity.xmlutil.dom2.NodeListIterator
import org.w3c.dom.Node as DomNode

internal class JsWrappedNodeList(val delegate: Any) : NodeList {
    override fun getLength(): Int = delegate.asDynamic().length

    override fun get(index: Int): JsWrappedNode<DomNode> {
        return item(index)
    }

    override fun iterator(): Iterator<JsWrappedNode<DomNode>> {
        return NodeListIterator(this)
    }

    override fun item(index: Int): JsWrappedNode<DomNode> {
        val node: DomNode = delegate.asDynamic().item(index) as DomNode? ?: throw IndexOutOfBoundsException("$index")
        return node.wrap()
    }
}
