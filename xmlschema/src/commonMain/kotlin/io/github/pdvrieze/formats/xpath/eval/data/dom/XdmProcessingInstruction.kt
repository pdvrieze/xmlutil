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

package io.github.pdvrieze.formats.xpath.eval.data.dom

import io.github.pdvrieze.formats.xpath.eval.data.XdmAtomic
import io.github.pdvrieze.formats.xpath.eval.data.XdmAtomicOrSequence
import io.github.pdvrieze.formats.xpath.eval.data.XdmNode
import io.github.pdvrieze.formats.xpath.eval.data.XdmNodeFriend
import io.github.pdvrieze.formats.xpath.eval.type.XdmSchemaType
import io.github.pdvrieze.formats.xpath.eval.type.XdmSingleType
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.UntypedType
import io.github.pdvrieze.xml.schematypes.values.XsdAtomic
import io.github.pdvrieze.xml.schematypes.values.XsdString
import nl.adaptivity.xmlutil.dom.PlatformProcessingInstruction
import nl.adaptivity.xmlutil.dom.getData
import nl.adaptivity.xmlutil.dom.getNodeName
import nl.adaptivity.xmlutil.dom2.impl.AbstractProcessingInstruction

@OptIn(XPathInternal::class)
internal class XdmProcessingInstruction(
    ownerDocument: XdmDocument,
    target: String,
    data: String,
    parentNode: XdmParentNode<*>? = null,
) : AbstractProcessingInstruction<XdmNode<*>, XdmParentNode<*>>(ownerDocument, parentNode),
    XdmNode<XdmProcessingInstruction> {
    private val _target = target
    private var _data = data

    override var posInParent: Int = -1
        @XdmNodeFriend set

    // TODO this is not necessarily valid. Probably just maps to the document element.
    final override val staticType: XdmSingleType get() = XdmSchemaType(UntypedType.Instance)

    private var _dynamicType: XdmSingleType? = null
    final override val dynamicType: XdmSingleType
        get() = _dynamicType ?: staticType

    constructor(ownerDocument: XdmDocument, original: PlatformProcessingInstruction) :
            this(ownerDocument, original.getNodeName(), original.getData())

    override fun asT(): XdmProcessingInstruction = this

    override fun getOwnerDocument(): XdmDocument {
        return super.getOwnerDocument() as XdmDocument
    }

    @XdmNodeFriend
    override fun setOwnerDocument(ownerDocument: XdmDocument) {
        super.setOwnerDocument(ownerDocument)
    }

    override fun getTarget(): String = _target

    override fun getData(): String = _data

    override fun setData(data: String) {
        this._data = data
    }

    context(ctx: ExprEvalContext)
    override fun atomize(): XdmAtomicOrSequence<XdmAtomic<XsdAtomic>> {
        return XdmAtomic(XsdString(_data))
    }

    override fun cloneNode(deep: Boolean): XdmProcessingInstruction {
        return XdmProcessingInstruction(getOwnerDocument(), getTarget(), getData())
    }
}
