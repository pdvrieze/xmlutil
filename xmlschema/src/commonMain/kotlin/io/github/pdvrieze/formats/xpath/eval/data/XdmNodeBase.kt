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

import io.github.pdvrieze.formats.xpath.eval.data.dom.XdmParentNode
import io.github.pdvrieze.formats.xpath.eval.type.XdmSingleType
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmTypeTest
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.Collation
import io.github.pdvrieze.xml.schematypes.values.XsdAtomic
import nl.adaptivity.xmlutil.dom2.impl.AbstractNodeList

@OptIn(XPathInternal::class)
sealed interface XdmNodeBase<out T: XdmNode<T>> : XdmSingleValue<T> {

    override val staticType: XdmSingleType// = XdmSchemaType(UntypedType.Instance)

    fun getChildNodes(): AbstractNodeList<XdmNode<*>, XdmParentNode<*>>

    context(ctx: ExprEvalContext)
    override fun toBoolean(): Boolean {
        // TODO this is overly simple
        return true // single element sequence with node
//        if (type.isSubtypeOf(BooleanType.Instance))
    }

    @XPathInternal
    context(ctx: ExprEvalContext)
    override fun treatAsNonEmpty(type: XdmTypeTest): XdmNodeAlias<@UnsafeVariance T>

    @XPathInternal
    context(ctx: ExprEvalContext)
    override fun atomizeTo(receiver: XdmSequence.XdmSequenceBuilder<XdmAtomic<XsdAtomic>>) {
        receiver.add(atomize())
    }

    context(ctx: ExprEvalContext)
    override fun atomize(): XdmAtomicOrSequence<XdmAtomic<XsdAtomic>>

    context(ctx: ExprEvalContext)
    override fun isValEqual(expected: XdmValue<*>, collation: Collation?): Boolean {
        return this.equals(expected)
    }

    context(ctx: ExprEvalContext)
    override fun isDeepEqual(other: XdmValue<*>, collation: Collation?): Boolean = when (other) {
        is XdmNodeAlias<*> -> isDeepEqual(other.base, collation ?: ctx.defaultCollation)
        is XdmNode<*> -> isDeepEqual(other, collation ?: ctx.defaultCollation)
        else -> false
    }

    context(ctx: ExprEvalContext)
    fun isDeepEqual(other: XdmNode<*>, collation: Collation): Boolean
}
