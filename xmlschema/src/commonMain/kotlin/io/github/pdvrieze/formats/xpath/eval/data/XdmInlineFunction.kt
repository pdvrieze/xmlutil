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

package io.github.pdvrieze.formats.xpath.eval.data

import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.type.XdmFunctionType
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmSequenceTypeTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmTypeTest
import io.github.pdvrieze.formats.xpath.functions.XFunction
import io.github.pdvrieze.formats.xpath.impl.Expr
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.Collation
import nl.adaptivity.xmlutil.QName

@XPathInternal
class XdmInlineFunction(
    val params: List<Param>,
    val returnType: XdmSequenceTypeTest,
    val body: Expr
) : XdmFunction<XdmInlineFunction>() {
    override val maybeName: Nothing? get() = null

    override val staticType: XdmFunctionType
        get() = XdmFunctionType(params.map { it.type }, returnType)

    override val dynamicType: XdmFunctionType get() = staticType

    override fun asT(): XdmInlineFunction = this

    context(ctx: ExprEvalContext)
    override fun treatAsNonEmpty(type: XdmTypeTest): XdmValue<*> {
        TODO("Function casting not yet implemented")
    }

    context(ctx: ExprEvalContext)
    override fun isValEqual(expected: XdmValue<*>, collation: Collation?): Boolean {
        return expected is XdmInlineFunction &&
            params.size == expected.params.size &&
            body == expected.body &&
            params.indices.all { params[it] == expected.params[it] } &&
                returnType == expected.returnType
    }

    context(ctx: ExprEvalContext)
    override fun isDeepEqual(
        other: XdmValue<*>,
        collation: Collation?
    ): Boolean {
        return isValEqual(other, collation)
    }

    context(ctx: ExprEvalContext)
    override fun invoke(args: List<XdmValue<*>>): XdmValue<*> {
        if (args.size != params.size) throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT)

        val promotedArgs = XFunction.promoteArguments(args, staticType, QName("<inline>"))
        val values = promotedArgs.mapIndexed { index, value ->
            params[index].name to value
        }

        val innerCtx = ctx.newVarsScope(values)
        return context(innerCtx) {
            body.eval()
        }
    }

    data class Param(val name: QName, val type: XdmSequenceTypeTest)

}
