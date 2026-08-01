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
import io.github.pdvrieze.formats.xpath.eval.data.XdmPartialApplication
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue

@XPathInternal
internal class ArrowFunction @NeedsXPath3_1 constructor(val expr: ExprSingle, val functionSpecifier: ArrowFunctionSpecifier, val params: List<ExprSingleOrPlaceholder>): AbstractExprSingle() {


    override fun collectUnsupportedExprs(
        xPathVersion: XPathVersion,
        isXQuery: Boolean,
        collector: MutableList<Any>
    ) {
        if (xPathVersion < XPathVersion.XPath3_1) collector.add(this)
        expr.collectUnsupportedExprs(xPathVersion, isXQuery, collector)
        functionSpecifier.collectUnsupportedExprs(xPathVersion, isXQuery, collector)
        params.forEach { it.collectUnsupportedExprs(xPathVersion, isXQuery, collector) }
    }

    @OptIn(NeedsXPath3_1::class)
    @XPathInternal
    context(ctx: EvalContext)
    override fun eval(): XdmValue<*> {
        ctx.withExprContext(this) {
            val function = functionSpecifier.resolve(params.size + 1)

            var isPartial = false
            val newArgs = buildList<XdmValue<*>?> {
                add(expr.eval())
                params.mapTo(this) { p ->
                    when (p) {
                        ParamPlaceholder -> {
                            isPartial = true
                            null
                        }

                        is ExprSingle -> p.eval()
                    }
                }
            }
            if (isPartial) return XdmPartialApplication(function, newArgs)

            return function.invoke(newArgs.filterNotNull())
        }
    }

    context(c: OutputContext)
    override fun appendToString(builder: Appendable) {
        expr.appendToString(builder)
        builder.append(" => ")
        functionSpecifier.appendToString(builder)
        builder.append('(')
        builder.appendExprs(params)
        builder.append(')')
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as ArrowFunction

        if (expr != other.expr) return false
        if (functionSpecifier != other.functionSpecifier) return false
        if (params != other.params) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + expr.hashCode()
        result = 31 * result + functionSpecifier.hashCode()
        result = 31 * result + params.hashCode()
        return result
    }


}
