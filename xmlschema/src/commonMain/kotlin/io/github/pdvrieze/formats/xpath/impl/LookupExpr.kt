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

import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.data.*
import io.github.pdvrieze.formats.xpath.functions.impl.Accessors
import io.github.pdvrieze.xml.schematypes.types.AnyAtomicType
import io.github.pdvrieze.xml.schematypes.values.XsdInt

@XPathInternal
internal class LookupExpr @NeedsXPath3_1 constructor(val context: Expr?, val key: KeySpecifier): AbstractExprSingle() {
    context(c: OutputContext)
    override fun appendToString(builder: Appendable) {
        context?.appendToString(builder)
        builder.append('?')
        key.appendToString(builder)
    }

    @OptIn(NeedsXPath3_1::class)
    @XPathInternal
    context(ctx: EvalContext)
    override fun eval(): XdmValue<*> {
        return ctx.withExprContext(this) {
            when (val c = context?.eval() ?: ctx.contextValue) {
                is XdmSequence.EMPTY -> XdmSequence.EMPTY
                is XdmMap -> {
                    val keyType = c.staticType.keyType.schemaType as AnyAtomicType<*>
                    val k = when (key) {
                        is NCNameKey -> XdmAtomic(keyType.fromString(key.value))
                        is IntegerKey -> XdmAtomic(keyType.castFrom(XsdInt(key.value)))

                        AnyKey -> return XdmSequence.build<XdmSingleValue<*>> {
                            for (v in c.values) {
                                addAll(v.atomize())
                            }
                        }

                        is ParenKey -> return XdmSequence.build<XdmSingleValue<*>> {
                            for (k in Accessors.fnData(key.params.map { it.eval() })) {
                                val v: XdmValue<*>? = c.get(k)
                                if (v != null) addAll(v.atomize())
                            }
                        }
                    }
                    c.get(k) ?: XdmSequence.EMPTY
                }

                is XdmArray -> when (key) {
                    is NCNameKey -> throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Non-integer array code")
                    is IntegerKey -> c.content[key.value]

                    AnyKey -> XdmSequence.build<XdmAtomic<*>> {
                        for (v in c.content) {
                            addAll(v.atomize())
                        }
                    }

                    is ParenKey -> XdmSequence.build<XdmAtomic<*>> {
                        for (k in Accessors.fnData(key.params.map { it.eval() })) {
                            val intKey = k.toXdmInteger().value.toInt()
                            if (intKey in c.content.indices) {
                                addAll(c.content[intKey].atomize())
                            }
                        }
                    }
                }

                else -> throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Context item not compatible with lookup")
            }

        }
    }

    sealed class KeySpecifier @NeedsXPath3_1 constructor() {
        context(c: OutputContext)
        abstract fun appendToString(builder: Appendable)
        override fun toString(): String = buildString {
            context(OutputContext.EMPTY) {
                appendToString(this)
            }
        }
    }

    class IntegerKey @NeedsXPath3_1 constructor(val value: Int) : KeySpecifier() {
        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            when (builder) {
                is StringBuilder -> builder.append(value)
                else -> builder.append(value.toString())
            }
        }
    }

    class NCNameKey @NeedsXPath3_1 constructor(val value: String) : KeySpecifier() {
        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.append(value)
        }
    }

    @NeedsXPath3_1
    object AnyKey : KeySpecifier() {
        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.append('*')
        }
    }

    class ParenKey @NeedsXPath3_1 constructor(val params: List<ExprSingle>) : KeySpecifier() {
        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.append('(')
            builder.joinHelper(params) { expr ->
                expr.appendToString(this)
            }
            builder.append(')')
        }
    }
}
