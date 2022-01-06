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

package nl.adaptivity.xmlutil.core.impl.dom

import org.w3c.dom.*

internal abstract class CharacterDataImpl(
    ownerDocument: Document,
    data: String
): NodeImpl(ownerDocument), CharacterData {


    final override var data = data
        private set

    final override var parentNode: Node? = null

    final override val firstChild: Nothing? get() = null
    final override val lastChild: Nothing? get() = null
    final override val childNodes: NodeList get() = EmptyNodeList

    final override fun substringData(offset: Int, count: Int): String {
        return data.substring(offset, offset+count)
    }

    final override fun appendData(data: String) {
        this.data += data
    }

    final override fun insertData(offset: Int, data: String) {
        this.data = this.data.replaceRange(offset, offset, data)
    }

    final override fun deleteData(offset: Int, count: Int) {
        this.data = data.removeRange(offset, offset + count)
    }

    final override fun replaceData(offset: Int, count: Int, data: String) {
        this.data = this.data.replaceRange(offset, offset+count, data)
    }

    final override fun appendChild(node: Node): Node {
        throw DOMException("Character nodes have no children")
    }

    final override fun replaceChild(oldChild: Node, newChild: Node): Node {
        throw DOMException("Character nodes have no children")
    }

    final override fun removeChild(node: Node): Node {
        throw DOMException("Character nodes have no children")
    }

    override fun lookupPrefix(namespace: String?): String? {
        return parentNode?.lookupPrefix(namespace)
    }

    override fun lookupNamespaceURI(prefix: String?): String? {
        return parentNode?.lookupNamespaceURI(prefix)
    }
}

internal object EmptyNodeList: NodeList {
    override val length: Int get() = 0

    override fun item(index: Int): Nothing? = null
}
