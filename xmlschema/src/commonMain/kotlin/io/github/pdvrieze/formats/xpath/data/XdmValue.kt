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

package io.github.pdvrieze.formats.xpath.data

import io.github.pdvrieze.formats.xpath.impl.Expr
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal

@OptIn(XPathInternal::class)
sealed class XdmValue {
    open val size: Int get() = 1
    abstract operator fun get(index: Int): XdmValue

    context(ctx: ExprEvalContext)
    internal abstract fun atomizeTo(receiver: MutableList<in XdmSingleValue<*>>)

    context(ctx: ExprEvalContext)
    open fun atomize(): XdmValue {
        val newElems = mutableListOf<XdmSingleValue<*>>()
        atomizeTo(newElems)
        return newElems.singleOrNull() ?: XdmSequence(newElems)
    }

    context(ctx: ExprEvalContext)
    abstract fun withType(type: XdmType): XdmValue

    context(ctx: ExprEvalContext)
    abstract fun evalPredicates(predicates: Iterable<Expr>): XdmValue

    /**
     * Implement the VAL_EQ operator
     */
    abstract fun isValEqual(expected: XdmValue): Boolean

    context(ctx: ExprEvalContext)
    abstract fun toBoolean(): Boolean

    abstract val type: XdmType

}

