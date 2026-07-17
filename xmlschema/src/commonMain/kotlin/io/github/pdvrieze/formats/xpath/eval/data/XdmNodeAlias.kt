/*
 * Copyright (c) 2026.
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

package io.github.pdvrieze.formats.xpath.eval.data

import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.data.dom.*
import io.github.pdvrieze.formats.xpath.eval.type.XdmSchemaType
import io.github.pdvrieze.formats.xpath.eval.type.XdmSingleType
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmTypeTest
import io.github.pdvrieze.formats.xpath.functions.impl.Accessors
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.Collation
import io.github.pdvrieze.xml.schematypes.types.AnyAtomicType
import io.github.pdvrieze.xml.schematypes.values.XsdAtomic
import io.github.pdvrieze.xml.schematypes.values.XsdString
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom2.Attr
import nl.adaptivity.xmlutil.dom2.NamedNodeMap
import nl.adaptivity.xmlutil.dom2.NodeType
import nl.adaptivity.xmlutil.dom2.impl.AbstractElement
import nl.adaptivity.xmlutil.dom2.impl.AbstractNodeList
import nl.adaptivity.xmlutil.dom2.value

@XPathInternal
class XdmNodeAlias<T: XdmNode<T>> internal constructor(
    val base: XdmNode<T>,
    override val dynamicType: XdmSingleType
) : XdmNodeBase<T> {

    override val staticType: XdmSingleType get() = dynamicType

    override fun asT(): T = base.asT()

    fun getOwnerDocument(): XdmDocument? = base.getOwnerDocument()

    fun getNodetype(): NodeType = base.getNodetype()

    fun getNodeName(): String = getNodeName()

    @ExperimentalXmlUtilApi
    fun getNodeValue(): String? = base.getNodeValue()

    @ExperimentalXmlUtilApi
    fun getAttributes(): NamedNodeMap<Attr>? = base.getAttributes()

    @ExperimentalXmlUtilApi
    fun hasChildNodes(): Boolean = base.hasChildNodes()

    fun hasAttributes(): Boolean = base.hasAttributes()

    fun getNamespaceURI(): String? = base.getNamespaceURI()

    fun getPrefix(): String? = base.getPrefix()

    fun getLocalName(): String? = base.getLocalName()

    @ExperimentalXmlUtilApi
    fun normalize() {
        base.normalize()
    }

    @ExperimentalXmlUtilApi
    fun isSameNode(other: PlatformNode): Boolean {
        return base.isSameNode(other)
    }

    @ExperimentalXmlUtilApi
    fun isEqualNode(other: PlatformNode): Boolean {
        return base.isEqualNode(other)
    }

    @ExperimentalXmlUtilApi
    fun getBaseURI(): String? = base.getBaseURI()

    fun getTextContent(): String? = base.getTextContent()

    @ExperimentalXmlUtilApi
    fun lookupPrefix(namespace: String): String? = base.lookupPrefix(namespace)

    @ExperimentalXmlUtilApi
    fun lookupNamespaceURI(prefix: String): String? {
        return lookupNamespaceURI(prefix)
    }

    @ExperimentalXmlUtilApi
    fun isDefaultNamespace(namespaceURI: String): Boolean {
        return base.isDefaultNamespace(namespaceURI)
    }

    fun getParentNode(): XdmParentNode<*>? = base.getParentNode()

    override fun getChildNodes(): AbstractNodeList<XdmNode<*>, XdmParentNode<*>> {
        return base.getChildNodes()
    }

    fun getFirstChild(): XdmNode<*>? {
        // TODO add type casting (if needed)
        return base.getFirstChild()
    }

    fun getLastChild(): XdmNode<*>? {
        // TODO add type casting (if needed)
        return base.getLastChild()
    }

    fun getPreviousSibling(): XdmNode<*>? = base.getPreviousSibling()

    fun getNextSibling(): XdmNode<*>? = base.getNextSibling()

    fun getParentElement(): AbstractElement<XdmNode<*>, XdmParentNode<*>>? {
        return base.getParentElement()
    }

    fun cloneNode(deep: Boolean): XdmNodeAlias<T> {
        return XdmNodeAlias(base.cloneNode(deep), dynamicType)
    }

    @XPathInternal
    context(ctx: ExprEvalContext)
    override fun normalizeToArithmetic(): XdmValue<*> = when (base) {
        is XdmAttr -> dynamicType.fromString(base.getValue())
        is XdmText -> dynamicType.fromString(base.getData())
        else -> super.normalizeToArithmetic()
    }

    @XPathInternal
    context(ctx: ExprEvalContext)
    override fun treatAsNonEmpty(type: XdmTypeTest): XdmNodeAlias<T> {
        return base.treatAsNonEmpty(type)
    }

    context(ctx: ExprEvalContext)
    override fun isDeepEqual(other: XdmNode<*>, collation: Collation): Boolean {
        return base.isDeepEqual(other, collation)
    }

    context(ctx: ExprEvalContext)
    override fun atomize(): XdmAtomicOrSequence<XdmAtomic<XsdAtomic>> {
        return when (base) {
            is XdmAttr -> (dynamicType).fromString(base.value) as XdmAtomic<*>
            is XdmProcessingInstruction -> XdmAtomic(XsdString.Companion(base.getData()))
            is XdmComment -> XdmAtomic(XsdString.Companion(base.getData()))
            is XdmText -> XdmAtomic(XsdString.Companion(base.getData()))
            is XdmElement if base.isNil -> XdmSequence.EMPTY

            is XdmDocument -> Accessors.fnString(this).atomize()

            is XdmElement if dynamicType.isAssignableTo(AnyAtomicType.Instance) ->
                dynamicType.fromString(base.getTextContent()) as XdmAtomic<*>

            is XdmElement if dynamicType == XdmSchemaType.UNTYPED -> Accessors.fnString(this).atomize()

            is XdmElement -> throw EvaluationException.Companion(
                ErrorCodes.FOTY0012,
                "Cannot atomize a typed element to non-atomic type yet"
            )
            else -> throw UnsupportedOperationException("Unsupported node type: ${base.getNodetype()}")
        }

        // TODO add check that the value is not "typed" (there is an actual value in the node)
        // otherwise throw FOTY0012
    }

}
