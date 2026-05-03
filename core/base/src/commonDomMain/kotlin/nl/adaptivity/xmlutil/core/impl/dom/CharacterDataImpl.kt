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

@file:MustUseReturnValues

package nl.adaptivity.xmlutil.core.impl.dom

import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.dom2.CharacterData
import nl.adaptivity.xmlutil.dom2.impl.AbstractCharacterData
import nl.adaptivity.xmlutil.dom2.impl.AbstractDocument

@XmlUtilInternal
public abstract class CharacterDataImpl internal constructor(
    ownerDocument: DocumentImpl,
    private var data: String,
    parentNode: ParentNodeImpl? = null
) : AbstractCharacterData<NodeImpl, ParentNodeImpl>(ownerDocument, parentNode), CharacterData {
    override fun getOwnerDocument(): DocumentImpl = super.getOwnerDocument() as DocumentImpl

    override fun getData(): String = data

    override fun setData(data: String) {
        this.data = data
    }

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
}

