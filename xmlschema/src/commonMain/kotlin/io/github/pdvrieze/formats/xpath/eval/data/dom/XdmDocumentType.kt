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
import nl.adaptivity.xmlutil.dom.*
import nl.adaptivity.xmlutil.dom2.*
import nl.adaptivity.xmlutil.dom2.impl.AbstractDocumentType

@XPathInternal
public class XdmDocumentType internal constructor(
    maybeOwnerDocument: XdmDocument?,
    name: String,
    publicId: String,
    systemId: String
) : AbstractDocumentType<XdmNode<*>, XdmParentNode<*>>(maybeOwnerDocument), XdmNode<XdmDocumentType>, DocumentType {
    private val _name = name
    private val _publicId = publicId
    private val _systemId = systemId

    internal constructor(original: PlatformDocumentType) : this(
        original.getOwnerDocument()?.let { XdmDocument.coerce(it) },
        original.getName(),
        original.getPublicId(),
        original.getSystemId()
    )

    override fun asT(): XdmDocumentType = this

    override var posInParent: Int get() = -1
        @XdmNodeFriend set(value) {
            if (value >= 0) throw DOMException.hierarchyRequestErr("Cannot set position of a document")
        }

    // TODO this is not necessarily valid. Probably just maps to the document element.
    final override var staticType: XdmSingleType = XdmSchemaType(UntypedType.Instance)
        private set

    private var _dynamicType: XdmSingleType? = null
    final override val dynamicType: XdmSingleType
        get() = _dynamicType ?: staticType

    override fun getOwnerDocument(): XdmDocument? {
        return super.getOwnerDocument() as XdmDocument?
    }

    @XdmNodeFriend
    override fun setOwnerDocument(ownerDocument: XdmDocument) {
        super.setOwnerDocument(ownerDocument)
    }

    override fun getName(): String = _name

    override fun getPublicId(): String = _publicId

    override fun getSystemId(): String = _systemId

    override fun getEntities(): NamedNodeMap<Entity> {
        return EmptyNamedNodeMap
    }

    override fun getNotations(): NamedNodeMap<Notation> {
        return EmptyNamedNodeMap
    }

    override fun cloneNode(deep: Boolean): XdmDocumentType {
        return XdmDocumentType(null, getName(), getPublicId(), getSystemId())
    }

    context(ctx: ExprEvalContext)
    override fun atomize(): XdmAtomicOrSequence<XdmAtomic<XsdAtomic>> {
        throw UnsupportedOperationException("Unsupported node type: ${NodeType.DOCUMENT_TYPE_NODE}")
    }

    public companion object {
        internal fun coerce(doctype: PlatformDocumentType): XdmDocumentType {
            return (doctype as? XdmDocumentType)?.takeIf { it.getOwnerDocument() == null } ?: XdmDocumentType(doctype)
        }

    }
}
