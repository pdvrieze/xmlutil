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

import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.data.*
import io.github.pdvrieze.formats.xpath.eval.type.XdmSchemaType
import io.github.pdvrieze.formats.xpath.eval.type.XdmSingleType
import io.github.pdvrieze.formats.xpath.functions.impl.Accessors
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.AnyAtomicType
import io.github.pdvrieze.xml.schematypes.types.AnySimpleType
import io.github.pdvrieze.xml.schematypes.types.UntypedAtomicType
import io.github.pdvrieze.xml.schematypes.types.UntypedType
import io.github.pdvrieze.xml.schematypes.values.XsdAtomic
import io.github.pdvrieze.xml.schematypes.values.XsdString
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.dom.*
import nl.adaptivity.xmlutil.dom2.NamedNodeMap
import nl.adaptivity.xmlutil.dom2.impl.AbstractElement
import nl.adaptivity.xmlutil.dom2.impl.LinearAttrStorage
import nl.adaptivity.xmlutil.dom2.impl.LinearNodeStorage

@XPathInternal
public class XdmElement internal constructor(
    ownerDocument: XdmDocument,
    namespaceURI: String?,
    localName: String,
    prefix: String?,
    parentNode: XdmParentNode<*>? = null,
) : AbstractElement<XdmNode<*>, XdmParentNode<*>>(
    ownerDocument = ownerDocument,
    namespaceURI = namespaceURI,
    localName = localName,
    prefix = prefix,
    nodeStorage = { LinearNodeStorage(ownerDocument.storageAdapter) },
    attrStorage = { LinearAttrStorage(ownerDocument.storageAdapter, it as AbstractElement<*, *>) },
    parentNode = parentNode
), XdmParentNode<XdmElement> {
    internal constructor(ownerDocument: XdmDocument, original: PlatformElement) : this(
        ownerDocument,
        original.namespaceURI,
        original.localName,
        original.prefix
    )

    override val self: XdmElement get() = this
    override fun asT(): XdmElement = this

    override var posInParent: Int = -1
        @XdmNodeFriend set

    // TODO this is not necessarily valid. Probably just maps to the document element.
    final override var staticType: XdmSingleType = XdmSchemaType(UntypedType.Instance)
        private set

    private var _dynamicType: XdmSingleType? = null
    final override val dynamicType: XdmSingleType
        get() = _dynamicType ?: staticType

    override fun getOwnerDocument(): XdmDocument = super.getOwnerDocument() as XdmDocument

    @XdmNodeFriend
    override fun setOwnerDocument(ownerDocument: XdmDocument) {
        super.setOwnerDocument(ownerDocument)
    }

    override fun getParentElement(): XdmElement? {
        return getParentNode() as XdmElement?
    }

    override fun getTagName(): String = when (prefix) {
        null, "" -> localName
        else -> "$prefix:$localName"
    }

    context(ctx: ExprEvalContext)
    override fun atomize(): XdmAtomicOrSequence<XdmAtomic<XsdAtomic>> {
        return when {
            // 2.5.3 part 4 - sub b (maybe should be later
            isNil -> XdmSequence.EMPTY

            // Is this even possible?
            staticType.isAssignableTo(UntypedAtomicType.Instance) -> {
                val s: XdmAtomic<XsdString> = Accessors.fnString(this)
                XdmAtomic(UntypedAtomicType.Instance.fromString(s.value))
            }

            // 2.5.3 part 4 - sub a
            staticType == XdmSchemaType.ANY_SIMPLE ||
            ! staticType.isAssignableTo(AnySimpleType.Instance) ||
            staticType.isAssignableTo(AnyAtomicType.Instance) ->
                XdmAtomic(UntypedAtomicType.Instance.fromString(getTextContent()))

            dynamicType == XdmSchemaType.UNTYPED -> {
                val s = Accessors.fnString(this)
                XdmAtomic(UntypedAtomicType.Instance.fromString(s.value))
            }

            else -> throw EvaluationException(ErrorCodes.FOTY0012,"Cannot atomize a typed element to non-atomic type yet")
        }
    }

    @IgnorableReturnValue
    override fun appendChild(node: PlatformNode): XdmNode<*> {
        if (node !is XdmNodeAlias<*>) return super<AbstractElement>.appendChild(node)
        val newNode = super<AbstractElement>.appendChild(node.base)
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
        if (newChild !is XdmNodeAlias<*>) return super<AbstractElement>.replaceChild(newChild, oldChild)
        val newNode = super<AbstractElement>.replaceChild(newChild.base, oldChild)
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
        if (newChild !is XdmNodeAlias<*>) return super<AbstractElement>.insertBefore(newChild, refChild)

        val newBase = super<AbstractElement>.insertBefore(newChild.base, refChild)
        return when {
            newBase === newChild.base -> newChild
            else -> XdmNodeAlias(newBase, newChild.dynamicType)
        }
    }

    override fun getAttributes(): NamedNodeMap<XdmAttr> {
        @Suppress("UNCHECKED_CAST")
        return super.getAttributes() as NamedNodeMap<XdmAttr>
    }

    override fun cloneNode(deep: Boolean): XdmElement {
        val e = XdmElement(getOwnerDocument(), namespaceURI, localName, prefix)
        for (a in getAttributes()) {
            when (val nsUri = a.getNamespaceURI()) {
                null, "" -> e.setAttribute(a.getName(), a.getValue())
                else -> e.setAttributeNS(nsUri, a.getName(), a.getValue())
            }
        }
        if (deep) {
            for (c in super.getChildNodes()) e.appendChild(c.cloneNode(true))
        }
        return e
    }

    override fun toString(): String {
        return buildString {
            append('<')
            val tagName = when {
                getPrefix().isNullOrEmpty() -> getLocalName()
                else -> "${getPrefix()}:${getLocalName()}"
            }
            append(tagName)
            for (a in getAttributes()) {
                append(' ').append(a)
            }

            val _childNodes = getChildNodes()

            if (_childNodes.isEmpty()) {
                append(" />")
            } else {
                append(">")
                _childNodes.joinTo(this, "")
                append("</").append(tagName).append('>')
            }
        }
    }

}
