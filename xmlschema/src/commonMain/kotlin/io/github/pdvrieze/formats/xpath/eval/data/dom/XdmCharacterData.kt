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

package io.github.pdvrieze.formats.xpath.eval.data.dom

import io.github.pdvrieze.formats.xpath.eval.data.XdmNode
import io.github.pdvrieze.formats.xpath.eval.data.XdmNodeFriend
import io.github.pdvrieze.formats.xpath.eval.type.XdmSchemaType
import io.github.pdvrieze.formats.xpath.eval.type.XdmSingleType
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.UntypedAtomicType
import nl.adaptivity.xmlutil.dom2.impl.AbstractCharacterData

@OptIn(XPathInternal::class)
internal abstract class XdmCharacterData internal constructor(
    ownerDocument: XdmDocument,
    data: String,
    parentNode: XdmParentNode<*>? = null
) : AbstractCharacterData<XdmNode<*>, XdmParentNode<*>>(ownerDocument, parentNode), XdmNode<XdmCharacterData> {

    private var _data = data

    final override var posInParent: Int = -1
        @XdmNodeFriend set

    final override var staticType: XdmSingleType = XdmSchemaType(UntypedAtomicType.Instance)
        @XdmNodeFriend set

    private var _dynamicType: XdmSingleType? = null
    final override val dynamicType: XdmSingleType
        get() = _dynamicType ?: staticType

    override fun getOwnerDocument(): XdmDocument = super.getOwnerDocument() as XdmDocument

    @XdmNodeFriend
    override fun setOwnerDocument(ownerDocument: XdmDocument) {
        super.setOwnerDocument(ownerDocument)
    }

    override fun getParentElement(): XdmElement? = super.getParentElement() as XdmElement?

    override fun getData(): String = _data

    override fun setData(data: String) {
        this._data = data
    }

    final override fun substringData(offset: Int, count: Int): String {
        return _data.substring(offset, offset + count)
    }

    final override fun appendData(data: String) {
        this._data += data
    }

    final override fun insertData(offset: Int, data: String) {
        this._data = this._data.replaceRange(offset, offset, data)
    }

    final override fun deleteData(offset: Int, count: Int) {
        this._data = _data.removeRange(offset, offset + count)
    }

    final override fun replaceData(offset: Int, count: Int, data: String) {
        this._data = this._data.replaceRange(offset, offset + count, data)
    }

    abstract override fun cloneNode(deep: Boolean): XdmCharacterData
}

