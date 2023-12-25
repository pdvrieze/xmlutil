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

import nl.adaptivity.xmlutil.core.impl.idom.ICharacterData
import nl.adaptivity.xmlutil.core.impl.idom.INode
import nl.adaptivity.xmlutil.core.impl.idom.INodeList
import nl.adaptivity.xmlutil.dom.*

internal abstract class CharacterDataImpl(
    ownerDocument: DocumentImpl,
    final override var data: String
) : NodeImpl(ownerDocument), ICharacterData {

    final override var parentNode: INode? = null
        internal set

    override fun getData(): String {
        return data
    }

    override fun setData(value: String) {
        data = value
    }

    final override fun getFirstChild(): Nothing? = null
    final override fun getLastChild(): Nothing? = null
    final override fun getChildNodes(): INodeList = EmptyNodeList

    override fun getTextContent(): String? = getData()

    final override fun substringData(offset: Int, count: Int): String {
        return getData().substring(offset, offset + count)
    }

    final override fun appendData(data: String) {
        this.data += data
    }

    final override fun insertData(offset: Int, data: String) {
        this.data = this.getData().replaceRange(offset, offset, data)
    }

    final override fun deleteData(offset: Int, count: Int) {
        this.data = getData().removeRange(offset, offset + count)
    }

    final override fun replaceData(offset: Int, count: Int, data: String) {
        this.data = this.getData().replaceRange(offset, offset + count, data)
    }

    final override fun appendChild(node: INode): Nothing {
        throw DOMException("Character nodes have no children")
    }

    final override fun replaceChild(oldChild: INode, newChild: INode): Nothing {
        throw DOMException("Character nodes have no children")
    }

    final override fun removeChild(node: INode): Nothing {
        throw DOMException("Character nodes have no children")
    }

    override fun lookupPrefix(namespace: String): String? {
        return getParentNode()?.lookupPrefix(namespace)
    }

    override fun lookupNamespaceURI(prefix: String): String? {
        return getParentNode()?.lookupNamespaceURI(prefix)
    }
}

internal object EmptyNodeList : INodeList {
    override val size: Int get() = 0

    override fun item(index: Int): Nothing? = null
}
