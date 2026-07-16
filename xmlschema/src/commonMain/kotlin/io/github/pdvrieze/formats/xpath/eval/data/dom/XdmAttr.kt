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

import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.EvaluationException.Companion.invoke
import io.github.pdvrieze.formats.xpath.eval.data.XdmAtomic
import io.github.pdvrieze.formats.xpath.eval.data.XdmAtomicOrSequence
import io.github.pdvrieze.formats.xpath.eval.data.XdmNode
import io.github.pdvrieze.formats.xpath.eval.data.XdmNodeFriend
import io.github.pdvrieze.formats.xpath.eval.data.XdmNodeOld
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.eval.type.XdmSchemaType
import io.github.pdvrieze.formats.xpath.eval.type.XdmSingleType
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmNodeKindTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmTypeTest
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.UntypedAtomicType
import io.github.pdvrieze.xml.schematypes.values.XsdAtomic
import nl.adaptivity.xmlutil.dom.DOMException
import nl.adaptivity.xmlutil.dom.PlatformAttr
import nl.adaptivity.xmlutil.dom.getLocalName
import nl.adaptivity.xmlutil.dom.getNamespaceURI
import nl.adaptivity.xmlutil.dom.getPrefix
import nl.adaptivity.xmlutil.dom.getValue
import nl.adaptivity.xmlutil.dom2.impl.AbstractAttr
import nl.adaptivity.xmlutil.dom2.value

@OptIn(XPathInternal::class)
internal class XdmAttr internal constructor(
    ownerDocument: XdmDocument,
    namespaceURI: String?,
    localName: String,
    prefix: String?,
    value: String,
    parentNode: XdmElement? = null,
): AbstractAttr<XdmNode<*>, XdmParentNode<*>>(ownerDocument, parentNode),
    XdmNode<XdmAttr> {

    private val _namespaceURI = namespaceURI
    private val _localName = localName
    private val _prefix = prefix
    private var _value = value

    override var staticType: XdmSingleType = XdmSchemaType(UntypedAtomicType.Instance)
        @XdmNodeFriend set

    private var _dynamicType: XdmSingleType? = null
    override val dynamicType: XdmSingleType
        get() = _dynamicType ?: staticType

    internal constructor(ownerDocument: XdmDocument, original: PlatformAttr) : this(
        ownerDocument,
        original.getNamespaceURI(),
        original.getLocalName() ?: throw DOMException.invalidCharacterErr("Local name not set for attribute") ,
        original.getPrefix(),
        original.getValue()
    )

    override fun asT(): XdmAttr = this

    override fun isId(): Boolean = false

    override var posInParent: Int = -1
        @XdmNodeFriend set

    override fun getOwnerDocument(): XdmDocument = super.getOwnerDocument() as XdmDocument

    @XdmNodeFriend
    override fun setOwnerDocument(ownerDocument: XdmDocument) {
        super.setOwnerDocument(ownerDocument)
    }

    override fun getNamespaceURI(): String? = _namespaceURI

    override fun getPrefix(): String? = _prefix

    override fun getLocalName(): String = _localName

    override fun getValue(): String = _value

    override fun setValue(value: String) {
        this._value = value
    }

    override fun getOwnerElement(): XdmElement? = super.getOwnerElement() as XdmElement?

    context(ctx: ExprEvalContext)
    override fun atomize(): XdmAtomicOrSequence<XdmAtomic<XsdAtomic>> {
        return staticType.fromString(value) as XdmAtomic<*>
    }

    override fun cloneNode(deep: Boolean): XdmAttr {
        return XdmAttr(getOwnerDocument(), _namespaceURI, _localName, _prefix, _value)
    }

    override fun toString(): String {
        val attrName = when (getPrefix().isNullOrBlank()) {
            true -> getLocalName()
            else -> "${getPrefix()}:${getLocalName()}"
        }
        return "$attrName=\"${getValue()}\""
    }
}
