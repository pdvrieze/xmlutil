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

package nl.adaptivity.xmlutil.core.impl.dom

import nl.adaptivity.xmlutil.core.impl.idom.ICharacterData
import nl.adaptivity.xmlutil.core.impl.idom.INodeList

internal abstract class CharacterDataImpl(
    private var ownerDocument: DocumentImpl,
    private var data: String
) : NodeImpl(), ICharacterData {
    override fun getOwnerDocument(): DocumentImpl = ownerDocument

    override fun setOwnerDocument(ownerDocument: DocumentImpl) {
        if (this.ownerDocument !== ownerDocument) {
            setParentNode(null)
            this.ownerDocument = ownerDocument
        }
    }

    override fun getData(): String = data

    override fun setData(value: String) {
        data = value
    }

    final override fun getFirstChild(): Nothing? = null
    final override fun getLastChild(): Nothing? = null
    final override fun getChildNodes(): INodeList = EmptyNodeList

    override fun getTextContent(): String? = getData()

    override fun setTextContent(value: String) {
        data = value
    }

    final override fun substringData(offset: Int, count: Int): String {
        return data.substring(offset, offset + count)
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
        this.data = this.data.replaceRange(offset, offset + count, data)
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
