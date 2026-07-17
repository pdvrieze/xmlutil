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

import Axis
import io.github.pdvrieze.formats.xpath.XPathVersion
import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.data.XdmAtomic
import io.github.pdvrieze.formats.xpath.eval.data.XdmSequence
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.impl.token.Axis
import io.github.pdvrieze.xml.schematypes.values.XsdInt
import io.github.pdvrieze.xml.schematypes.values.XsdNumeric
import kotlin.text.Appendable
import kotlin.text.append

@XPathInternal
open class AxisStep(
    val axis: Axis,
    val test: NodeTest,
    val predicates: List<Expr>
) : PrimaryOrStep() {
    constructor(test: NodeTest) : this(Axis.CHILD, test, emptyList())

    constructor(axis: Axis, test: NodeTest) : this(axis, test, emptyList())

    context(ctx: ExprEvalContext)
    override fun eval(context: XdmValue<*>?): XdmValue<*> {
        if (context == null) throw EvaluationException(ErrorCodes.XPDY0002_ABSENT_DYNAMIC_CONTEXT, "Missing context for path evaluation")

        var current = axis.eval(context, test)
        if (predicates.isEmpty()) return current

        for (predicate in predicates) {
            when (current) {
                is XdmSequence<*> -> {
                    val newElems = current.filterIndexed { index, value ->
                        val evalResult = ctx.withValueContext(value, index, current.size) { predicate.eval() }

                        when ((evalResult as? XdmAtomic<*>)?.value) {
                            is XsdNumeric<*> ->
                                XdmAtomic(XsdInt(index)).isValEqual(evalResult)

                            else -> evalResult.toBoolean()
                        }
                    }

                    current = XdmSequence.fromList (newElems, current.staticType)
                }

                else -> ctx.withValueContext(current, 1, 1) {
                    if (!predicate.eval().toBoolean()) return XdmSequence.EMPTY
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

        predicates.forEach { it.collectUnsupportedExprs(xPathVersion, isXQuery, collector) }
    }

    context(c: OutputContext)
    override fun appendToString(builder: Appendable) {
        when (axis) {
            Axis.ATTRIBUTE -> builder.append('@')
            Axis.CHILD -> {}
            else -> builder.append(axis.literal).append("::")
        }
        test.appendToString(builder)
        for(p in predicates) {
            builder.append('[')
            p.appendToString(builder)
            builder.append(']')
        }
    }
}

