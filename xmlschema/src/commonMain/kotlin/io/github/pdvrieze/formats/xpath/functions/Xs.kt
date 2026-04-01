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

package io.github.pdvrieze.formats.xpath.functions

import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.data.XdmAtomic
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.eval.type.XdmFunctionType
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmTypeTest
import io.github.pdvrieze.formats.xpath.functions.impl.AbstractFunctionObject
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.AnyAtomicType
import nl.adaptivity.xmlutil.QName

@XPathInternal
object Xs: AbstractFunctionObject() {

    context(ctx: ExprEvalContext)
    fun createFromSchemaType(name: QName): BuiltinFunction<*> {
        val type = ctx.resolveTypeOrNull(name) as AnyAtomicType<*>

        val signatures: List<XdmFunctionType> = listOf<XdmFunctionType>(functionType(XdmTypeTest.STRING, t(type)))
        return BuiltinFunctionImpl(name, signatures) { args: List<XdmValue> ->
            val arg = toAnySingleAtomic(args) ?: throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR)
            val value = runCatching { type.fromString(arg.value.xmlString) }
                .getOrElse { e ->
                    when (e) {
                        is IllegalArgumentException,
                        is NumberFormatException -> throw EvaluationException(ErrorCodes.FORG0001, e)

                        else -> throw EvaluationException(ctx.expr, e)
                    }
                }

            XdmAtomic(value)
        }
    }

}
