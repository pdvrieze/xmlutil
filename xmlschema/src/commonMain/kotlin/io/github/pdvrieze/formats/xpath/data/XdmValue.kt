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
sealed interface XdmValue {
    val size: Int get() = 1
    operator fun get(index: Int): XdmSingleValue<*>

    @XPathInternal
    context(ctx: ExprEvalContext)
    fun atomizeTo(receiver: MutableList<in XdmAtomic<*>>)

    context(ctx: ExprEvalContext)
    fun atomize(): XdmAtomicOrSequence {
        val newElems = mutableListOf<XdmAtomic<*>>()
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

    @XPathInternal
    context(ctx: ExprEvalContext)
    open fun normalizeToArithmetic(): XdmValue {
        val v = atomize()
        // Handle sequences
        return when (v.size) {
            0 -> return if (ctx.isXPath1Compat) XdmAtomic.NaN else XdmSequence.EMPTY
            1 -> v[0]
            else if ctx.isXPath1Compat -> v[0]
            else -> throw EvaluationException(
                ErrorCodes.XPTY0004_TYPE_ERROR,
                "Sequence as arithmatic operand"
            )
        }.normalizeToArithmetic()
    }

}

@XPathInternal
object XdmNil /*: XdmValue()*/ {
    /*override*/ fun get(index: Int): Nothing {
        throw IndexOutOfBoundsException("Index out of bounds")
    }

    context(ctx: ExprEvalContext)
    /*override*/ fun atomizeTo(receiver: MutableList<in XdmSingleValue<*>>): Nothing {
        throw UnsupportedOperationException("Nil cannot be atomized")
    }

    context(ctx: ExprEvalContext)
    /*override*/ fun withType(type: XdmType): Nothing {
        throw UnsupportedOperationException("Nil cannot be cast")
    }

    context(ctx: ExprEvalContext)
    /*override*/ fun evalPredicates(predicates: Iterable<Expr>): Nothing {
        throw UnsupportedOperationException("Nil cannot be evaluated")
    }

    /*override*/ fun isValEqual(expected: XdmValue): Nothing {
        throw UnsupportedOperationException("Nil cannot be evaluated")
    }

    context(ctx: ExprEvalContext)
    /*override*/ fun toBoolean(): Nothing{
        throw UnsupportedOperationException("Nil cannot be evaluated")
    }

    /*override*/ val type: XdmErrorType get() = XdmErrorType
}
