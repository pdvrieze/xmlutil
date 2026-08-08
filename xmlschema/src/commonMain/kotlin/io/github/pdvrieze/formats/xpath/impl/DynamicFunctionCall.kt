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

import io.github.pdvrieze.formats.xpath.eval.data.XdmFunction
import io.github.pdvrieze.formats.xpath.eval.data.XdmPartialApplication
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.functions.XFunction
import nl.adaptivity.xmlutil.QName

@XPathInternal
internal class DynamicFunctionCall @NeedsXPath3_0 constructor(val expr: Expr, args: List<ExprSingleOrPlaceholder>): FunctionCall(args) {

    context(c: OutputContext)
    override fun appendToString(builder: Appendable) {
        expr.appendToString(builder)
        builder.appendParams()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as DynamicFunctionCall

        return expr == other.expr
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + expr.hashCode()
        return result
    }

    companion object {

        @OptIn(NeedsXPath3_1::class)
        context(ctx: ExprEvalContext)
        fun eval(function: XdmFunction<*>, args: List<ExprSingleOrPlaceholder>): XdmValue<*> {
            var hasPlaceholder = false
            val evaluatedArgs = args.map {
                when (it) {
                    is ExprSingle -> it.eval()
                    ParamPlaceholder -> {
                        hasPlaceholder = true; null
                    }
                }
            }

            if (hasPlaceholder) return XdmPartialApplication(function, evaluatedArgs)
            val notNullArgs = evaluatedArgs.requireNoNulls()

            val realArgs =
                XFunction.promoteArguments(notNullArgs, function.dynamicType, function.maybeName ?: QName("<unknown>"))

            return function.invoke(realArgs)
        }

    }

}
