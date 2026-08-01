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

package io.github.pdvrieze.formats.xpath.impl

import io.github.pdvrieze.formats.xpath.XPathVersion
import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.data.XdmFunction
import io.github.pdvrieze.formats.xpath.eval.data.XdmSequence
import io.github.pdvrieze.formats.xpath.eval.data.XdmSingleValue
import nl.adaptivity.xmlutil.QName

@XPathInternal
internal sealed class ArrowFunctionSpecifier @NeedsXPath3_1 constructor() {


    abstract fun collectUnsupportedExprs(
        xPathVersion: XPathVersion,
        isXQuery: Boolean,
        collector: MutableList<Any>
    )


    context(c: OutputContext)
    abstract fun appendToString(builder: Appendable)

    context(ctx: ExprEvalContext)
    abstract fun resolve(arity: Int): XdmFunction<*>

    internal class QNameFunc @NeedsXPath3_1 constructor(val qname: QName) : ArrowFunctionSpecifier() {
        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.appendQName(qname)
        }

        override fun collectUnsupportedExprs(
            xPathVersion: XPathVersion,
            isXQuery: Boolean,
            collector: MutableList<Any>
        ) {}

        context(ctx: ExprEvalContext)
        override fun resolve(arity: Int): XdmFunction<*> {
            return ctx.resolveFunction(qname, arity)
                ?: throw EvaluationException(ErrorCodes.XPST0008_INVALID_NAME, "Function $qname not found")
        }
    }

    internal class SeqFunc @NeedsXPath3_1 internal constructor(val elements: List<ExprSingle>): ArrowFunctionSpecifier() {
        init {
            require(elements.isNotEmpty()) {"SeqFunc must have at least one element"}
        }
        @NeedsXPath3_1
        internal constructor(p: ParenExpr): this(
            when(val c = p.expr) {
                is SequenceExpr -> c.elements
                is ExprSingle -> listOf(c)
            }
        )

        override fun collectUnsupportedExprs(
            xPathVersion: XPathVersion,
            isXQuery: Boolean,
            collector: MutableList<Any>
        ) {
            elements.forEach { it.collectUnsupportedExprs(xPathVersion, isXQuery, collector) }
        }

        context(ctx: ExprEvalContext)
        override fun resolve(arity: Int): XdmFunction<*> {
            val maybeFunction = XdmSequence.build<XdmSingleValue<*>> {
                for (e in elements) add(e.eval())
            }
            if (maybeFunction.size != 1) throw EvaluationException("Sequence not of single element is not a function")
            val shouldBeFunction = maybeFunction[0] as? XdmFunction<*> ?: throw EvaluationException("${maybeFunction[0]} is not a function")
            return shouldBeFunction
        }

        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.append('(')
            builder.appendExprs(elements)
            builder.append(')')
        }
    }

    class VarRefFunc @NeedsXPath3_1 internal constructor(val varName: QName): ArrowFunctionSpecifier() {
        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.append('$').appendQName(varName)
        }

        override fun collectUnsupportedExprs(
            xPathVersion: XPathVersion,
            isXQuery: Boolean,
            collector: MutableList<Any>
        ) {}

        context(ctx: ExprEvalContext)
        override fun resolve(arity: Int): XdmFunction<*> {
            val varValue = ctx.resolveVar(varName) ?: throw EvaluationException(ErrorCodes.XPST0008_INVALID_NAME, "Undeclared variable: $varName")
            return varValue as? XdmFunction<*> ?: throw EvaluationException("${varValue} is not a function")
        }
    }
}

