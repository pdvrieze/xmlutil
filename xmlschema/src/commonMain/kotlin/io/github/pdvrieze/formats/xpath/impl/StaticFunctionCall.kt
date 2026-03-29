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
import io.github.pdvrieze.formats.xpath.data.EvaluationException
import io.github.pdvrieze.formats.xpath.data.XdmBuiltinFunction
import io.github.pdvrieze.formats.xpath.data.XdmPartialApplication
import io.github.pdvrieze.formats.xpath.data.XdmValue
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunction
import io.github.pdvrieze.formats.xpath.functions.Fn
import io.github.pdvrieze.formats.xpath.functions.Xs
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.namespaceURI

@XPathInternal
@NeedsXPath1
internal class StaticFunctionCall(val name: QName, args: List<ExprSingleOrPlaceholder>): FunctionCall(args) {
    constructor(name: QName, args: ParenExpr) : this(name, args.toExprList())

    constructor(name: QName, vararg args: ExprSingleOrPlaceholder) :
            this(name, args.asList())

    @OptIn(NeedsXPath3_1::class)
    context(ctx: EvalContext)
    @XPathInternal
    override fun eval(): XdmValue {
        val function = when (name.namespaceURI) {
            BuiltinFunction.FN_NAMESPACE, "" -> Fn.of(name.localPart)
            XMLConstants.XSD_NS_URI -> ctx.withExprContext(this) { Xs.createFromSchemaType(name) }
            else -> throw EvaluationException(this, "No builtin function from namespace: '${name.namespaceURI}'")
        }
        if (function == null) throw EvaluationException(this, "Function with name ${name} not found")

        if (args.any { it is ParamPlaceholder }) {
            val partialArgs = args.map {
                when (it) {
                    is ExprSingle -> it.eval()
                    ParamPlaceholder -> null
                }
            }
            return XdmPartialApplication(XdmBuiltinFunction(function), partialArgs)
        }
        val evalArgs = args.map { (it as ExprSingle).eval() }

        return withExprContext { function.invoke(evalArgs) }
    }

    override fun collectUnsupportedExprs(
        xPathVersion: XPathVersion,
        isXQuery: Boolean,
        collector: MutableList<Any>
    ) {
        args.forEach { it.collectUnsupportedExprs(xPathVersion, isXQuery, collector) }
    }

    context(c: OutputContext)
    override fun appendToString(builder: Appendable) {
        builder.appendQName(name)
        builder.appendParams()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as StaticFunctionCall

        return name == other.name
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }


}

