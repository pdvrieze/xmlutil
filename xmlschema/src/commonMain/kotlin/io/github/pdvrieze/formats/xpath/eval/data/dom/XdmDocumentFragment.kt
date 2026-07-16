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
@file:OptIn(XPathInternal::class)

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
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.dom.DOMException
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom2.NodeType
import nl.adaptivity.xmlutil.dom2.impl.AbstractDocument
import nl.adaptivity.xmlutil.dom2.impl.AbstractDocumentFragment
import nl.adaptivity.xmlutil.dom2.impl.LinearNodeStorage

@XmlUtilInternal
internal class XdmDocumentFragment(ownerDocument: XdmDocument) :
    AbstractDocumentFragment<XdmNode<*>, XdmParentNode<*>>(
        ownerDocument,
        { LinearNodeStorage(ownerDocument.storageAdapter) }), XdmParentNode<XdmDocumentFragment> {

    override val self: XdmDocumentFragment get() = this

    override fun asT(): XdmDocumentFragment = this

    // TODO this is not necessarily valid. Probably just maps to the document element.
    final override var staticType: XdmSingleType = XdmSchemaType(UntypedType.Instance)
        private set

    private var _dynamicType: XdmSingleType? = null
    final override val dynamicType: XdmSingleType
        get() = _dynamicType ?: staticType

    override var posInParent: Int get() = -1
        @XdmNodeFriend set(value) {
            if (value >= 0) throw DOMException.hierarchyRequestErr("Cannot set position of a document fragment")
        }

    override fun getOwnerDocument(): XdmDocument = super.getOwnerDocument() as XdmDocument

    @XdmNodeFriend
    override fun setOwnerDocument(ownerDocument: XdmDocument) {
        super.setOwnerDocument(ownerDocument)
    }

    /** Note that document fragments cannot have an owner element */
    override fun getParentElement(): Nothing? = null

    context(ctx: ExprEvalContext)
    override fun atomize(): XdmAtomicOrSequence<XdmAtomic<XsdAtomic>> {
        throw UnsupportedOperationException("Unsupported node type: ${NodeType.DOCUMENT_FRAGMENT_NODE}")
    }

    @IgnorableReturnValue
    override fun appendChild(node: PlatformNode): XdmNode<*> {
        if (node !is XdmNodeAlias<*>) return super<AbstractDocumentFragment>.appendChild(node)
        val newNode = super<AbstractDocumentFragment>.appendChild(node.base)
        return when {
            newNode === node.base -> node
            else -> XdmNodeAlias(newNode, node.dynamicType)
        }
    }

    @IgnorableReturnValue
    override fun replaceChild(
        newChild: PlatformNode,
        oldChild: PlatformNode
    ): XdmNode<*> {
        if (newChild !is XdmNodeAlias<*>) return super<AbstractDocumentFragment>.replaceChild(newChild, oldChild)
        val newNode = super<AbstractDocumentFragment>.replaceChild(newChild.base, oldChild)
        return when {
            newNode === newChild.base -> newChild
            else -> XdmNodeAlias(newNode, newChild.dynamicType)
        }
    }

    @IgnorableReturnValue
    @ExperimentalXmlUtilApi
    override fun insertBefore(
        newChild: PlatformNode,
        refChild: PlatformNode?
    ): XdmNode<*> {
        if (newChild !is XdmNodeAlias<*>) return super<AbstractDocumentFragment>.insertBefore(newChild, refChild)

        val newBase = super<AbstractDocumentFragment>.insertBefore(newChild.base, refChild)
        return when {
            newBase === newChild.base -> newChild
            else -> XdmNodeAlias(newBase, newChild.dynamicType)
        }
    }

    override fun cloneNode(deep: Boolean): XdmDocumentFragment {
        val f = XdmDocumentFragment(getOwnerDocument())
        if (deep) {
            for (c in getChildNodes()) f.appendChild(c.cloneNode(deep))
        }
        return f
    }
}
