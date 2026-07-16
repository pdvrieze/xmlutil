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

@file:OptIn(XPathInternal::class)

package io.github.pdvrieze.formats.xpath.eval.data.dom

import io.github.pdvrieze.formats.xpath.eval.data.XdmAtomic
import io.github.pdvrieze.formats.xpath.eval.data.XdmAtomicOrSequence
import io.github.pdvrieze.formats.xpath.eval.data.XdmNode
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.values.XsdAtomic
import io.github.pdvrieze.xml.schematypes.values.XsdString
import nl.adaptivity.xmlutil.dom.PlatformText
import nl.adaptivity.xmlutil.dom.getData
import nl.adaptivity.xmlutil.dom2.NodeType
import nl.adaptivity.xmlutil.dom2.impl.AbstractText

@XPathInternal
open class XdmText(ownerDocument: XdmDocument, data: String) :
    XdmCharacterData(ownerDocument, data), AbstractText<XdmNode<*>, XdmParentNode<*>> {

    constructor(ownerDocument: XdmDocument, original: PlatformText) : this(ownerDocument, original.getData())

    override fun asT(): XdmText = this

    override fun getNodetype(): NodeType = NodeType.TEXT_NODE

    override fun getNodeName(): String = "#text"

    override fun cloneNode(deep: Boolean): XdmText {
        return XdmText(getOwnerDocument(), getData())
    }

    context(ctx: ExprEvalContext)
    final override fun atomize(): XdmAtomicOrSequence<XdmAtomic<XsdAtomic>> {
        return XdmAtomic(XsdString(this.getData()))
    }

    override fun toString(): String {
        return getData()
    }
}
