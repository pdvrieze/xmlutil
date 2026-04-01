/*
 * Copyright (c) 2023-2026.
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

import io.github.pdvrieze.formats.xpath.eval.data.XdmSequence
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue

@XPathInternal
internal class ParenExpr(val expr: Expr): AbstractExprSingle() {
    context(ctx: EvalContext)
    @XPathInternal
    override fun eval(): io.github.pdvrieze.formats.xpath.eval.data.XdmValue {
        return expr.eval()
    }

    context(c: OutputContext)
    override fun appendToString(builder: Appendable) {
        builder.append('(')
        expr.appendToString(builder)
        builder.append(')')
    }

    @OptIn(NeedsXPath2::class)
    fun toExprList(): List<ExprSingle> = when (expr) {
        is SequenceExpr -> expr.elements
        is ExprSingle -> listOf(expr)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as ParenExpr

        return expr == other.expr
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + expr.hashCode()
        return result
    }

    @OptIn(NeedsXPath2::class)
    fun isEmptySequence(): Boolean {
        return expr is SequenceExpr && expr.isEmpty()
    }


}

object EmptySequenceExpr : AbstractExprSingle() {
    context(c: OutputContext)
    @XPathInternal
    override fun appendToString(builder: Appendable) {
        builder.append("()")
    }

    context(ctx: EvalContext)
    @XPathInternal
    override fun eval(): XdmValue = XdmSequence.EMPTY
}
