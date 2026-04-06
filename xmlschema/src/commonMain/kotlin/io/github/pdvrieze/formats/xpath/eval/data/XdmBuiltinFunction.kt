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
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmTypeTest
import io.github.pdvrieze.formats.xpath.functions.XFunction
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.Collation

@XPathInternal
class XdmBuiltinFunction(
    private val function: XFunction<XdmValue<*>>,
    override val staticType: XdmFunctionType = function.functionTypes.single()
) : XdmFunction<XdmBuiltinFunction>() {
    override fun asT(): XdmBuiltinFunction = this

    override val dynamicType: XdmFunctionType
        get() = function.functionTypes.single()

    context(ctx: ExprEvalContext)
    override fun treatAsNonEmpty(type: XdmTypeTest): XdmValue<*> {
        TODO("Function casting not yet implemented")
    }

    override fun isValEqual(expected: XdmValue<*>, collation: Collation?): Boolean {
        return expected is XdmBuiltinFunction && expected.function == function
    }

    context(ctx: ExprEvalContext)
    override fun isDeepEqual(
        other: XdmValue<*>,
        collation: Collation?
    ): Boolean {
        return isValEqual(other, collation)
    }

    @XPathInternal
    context(ctx: ExprEvalContext)
    override fun normalizeToArithmetic(): XdmValue<*> {
        throw EvaluationException(
            ErrorCodes.XPTY0004_TYPE_ERROR,
            "Built in functions are not compatible with an arithmetic operator"
        )
    }
}

