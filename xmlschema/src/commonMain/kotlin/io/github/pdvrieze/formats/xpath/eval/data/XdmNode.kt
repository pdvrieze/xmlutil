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
import io.github.pdvrieze.formats.xpath.eval.data.dom.XdmDocument
import io.github.pdvrieze.formats.xpath.eval.data.dom.XdmNodeAlias
import io.github.pdvrieze.formats.xpath.eval.data.dom.XdmParentNode
import io.github.pdvrieze.formats.xpath.eval.type.XdmSingleType
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmNodeKindTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmTypeTest
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.Collation
import io.github.pdvrieze.xml.schematypes.values.XsdAtomic
import nl.adaptivity.xmlutil.dom2.Node
import nl.adaptivity.xmlutil.dom2.impl.IAbstractNode

@OptIn(XPathInternal::class)
public interface XdmNode<out T: XdmNode<T>>: XdmSingleValue<T>, IAbstractNode<XdmNode<*>, XdmParentNode<*>>, Node {
    override val staticType: XdmSingleType// = XdmSchemaType(UntypedType.Instance)

    abstract var posInParent: Int
        @XdmNodeFriend set

    abstract override fun getOwnerDocument(): XdmDocument?

    @XdmNodeFriend
    abstract fun setOwnerDocument(ownerDocument: XdmDocument)

    context(ctx: ExprEvalContext)
    override fun toBoolean(): Boolean {
        // TODO this is overly simple
        return false
//        if (type.isSubtypeOf(BooleanType.Instance))
    }

    context(ctx: ExprEvalContext)
    @XPathInternal
    override fun treatAsNonEmpty(type: XdmTypeTest): XdmNodeAlias<@UnsafeVariance T> {
        if (type !is XdmNodeKindTest) throw EvaluationException(ErrorCodes.XPDY0050_INVALID_TYPE_IN_TREAT_AS, "Cannot cast node to $type")
        if (! type.nodeKind.matches(this)) throw EvaluationException(ErrorCodes.XPDY0050_INVALID_TYPE_IN_TREAT_AS, "Cannot cast $this to (${type.nodeKind})")
        // TODO do some checks
        return XdmNodeAlias(this, type.toValueType(staticType).single)
    }

    context(ctx: ExprEvalContext)
    @XPathInternal
    override fun atomizeTo(receiver: XdmSequence.XdmSequenceBuilder<XdmAtomic<XsdAtomic>>) {
        receiver.add(atomize())
    }

    override fun isValEqual(
        expected: XdmValue<*>,
        collation: Collation?
    ): Boolean {
        TODO("not implemented")
    }

    context(ctx: ExprEvalContext)
    override fun isDeepEqual(
        other: XdmValue<*>,
        collation: Collation?
    ): Boolean {
        TODO("not implemented")
    }

    override fun cloneNode(deep: Boolean): XdmNode<T>
}

@RequiresOptIn("Friend for XdmNode")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FIELD, AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.PROPERTY_GETTER)
internal annotation class XdmNodeFriend()
