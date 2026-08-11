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
import io.github.pdvrieze.formats.xpath.eval.data.XdmSequence
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.xml.schematypes.values.XsdLong

@OptIn(XPathInternal::class)
@NeedsXPath1
internal class FilterExpr(val primaryExpr: ExprSingle, val predicates: List<Expr> = emptyList()): PrimaryOrStep() {
    context(ctx: ExprEvalContext)
    override fun eval(context: XdmValue<*>?): XdmValue<*> {
        val base = context(ctx.copyNoExpr(context?.let { ContextItem(it, 1, 1) })) { primaryExpr.eval() }
        if (predicates.isEmpty()) return base


        var current = base
        for (predicate in predicates) {
            if (predicate is LongLiteral) {
                val sequenceIdx = predicate.value.toInt() - 1
                if (sequenceIdx >= current.size) return XdmSequence.EMPTY
                current = current[sequenceIdx]
            } else {
                when (current) {
                    is XdmSequence<*> -> {
                        // note that the predicate needs repeated evaluation as it could use context
                        // position in evaluation
                        val newElems = current.filterIndexed { index, value ->
                            ctx.withValueContext(value, index + 1, current.size) { predicate.eval().toPredicateBoolean() }
                        }

                        current = XdmSequence.fromList (newElems, current.staticType)
                    }

                    else -> ctx.withValueContext(current, 1, 1) {
                        if (!predicate.eval().toPredicateBoolean()) return XdmSequence.EMPTY
                    }
                }
            }
        }
        return current
    }

    override fun collectUnsupportedExprs(
        xPathVersion: XPathVersion,
        isXQuery: Boolean,
        collector: MutableList<Any>
    ) {
        primaryExpr.collectUnsupportedExprs(xPathVersion, isXQuery, collector)
        predicates.forEach { it.collectUnsupportedExprs(xPathVersion, isXQuery, collector) }
    }

    context(c: OutputContext)
    override fun appendToString(builder: Appendable) {
        primaryExpr.appendToString(builder)
        for (p in predicates) {
            builder.append('[')
            p.appendToString(builder)
            builder.append(']')
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as FilterExpr

        if (primaryExpr != other.primaryExpr) return false
        if (predicates != other.predicates) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + primaryExpr.hashCode()
        result = 31 * result + predicates.hashCode()
        return result
    }


}

@OptIn(XPathInternal::class)
context(ctx: ExprEvalContext)
private fun XdmValue<*>.toPredicateBoolean(): Boolean {
    val itemIdx = ctx.contextItem?.position ?: return toBoolean()
    return when (val v = (this as? XdmAtomic<*>)?.value) {
        is XsdLong -> (v.toLong() == itemIdx.toLong())
        else -> this.toBoolean()
    }
}
