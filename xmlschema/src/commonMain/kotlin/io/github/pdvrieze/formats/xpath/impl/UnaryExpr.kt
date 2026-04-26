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
import io.github.pdvrieze.formats.xpath.eval.data.XdmAtomic
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.functions.impl.NumericFunctions
import io.github.pdvrieze.xml.schematypes.values.XsdNumeric

@XPathInternal
sealed class UnaryExpr: AbstractExprSingle() {

    class Plus @NeedsXPath2 constructor(val expr: ExprSingle): UnaryExpr() {
        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.append('+')
            expr.appendToString(builder)
        }

        context(ctx: EvalContext)
        @XPathInternal
        override fun eval(): XdmValue<*> {
            val e = expr.eval()
            ctx.withExprContext(this) {
                return XdmAtomic(NumericFunctions.fnNumber(e).value.unaryPlus())
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            if (!super.equals(other)) return false

            other as Plus

            return expr == other.expr
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + expr.hashCode()
            return result
        }


    }

    @NeedsXPath1
    class Minus(val expr: ExprSingle): UnaryExpr() {
        override fun collectUnsupportedExprs(
            xPathVersion: XPathVersion,
            isXQuery: Boolean,
            collector: MutableList<Any>
        ) {
            expr.collectUnsupportedExprs(xPathVersion, isXQuery, collector)
        }

        context(ctx: EvalContext)
        @XPathInternal
        override fun eval(): XdmValue<*> {
            val e = expr.eval()
            ctx.withExprContext(this) {
                val r = when (val v = (e as? XdmAtomic<*>)?.value) {
                    is XsdNumeric<*> -> v.unaryMinus()
                    else -> NumericFunctions.fnNumber(e).value.unaryMinus()
                }

                return XdmAtomic(r)
            }
        }

        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.append('-')
            expr.appendToString(builder)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            if (!super.equals(other)) return false

            other as Minus

            return expr == other.expr
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + expr.hashCode()
            return result
        }

    }

}
