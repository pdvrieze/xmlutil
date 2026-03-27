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

@file:OptIn(XPathInternal::class)

package io.github.pdvrieze.formats.xpath.functions

import io.github.pdvrieze.formats.xpath.data.*
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.AnyType
import io.github.pdvrieze.xml.schematypes.values.XsdAtomic

@XPathInternal
class BuiltinFunctionImpl<R: XdmValue>(
    val name: String,
    override val returnType: XdmType,
    override val argumentTypes: List<XdmType>,
    val evalFunction: context(ExprEvalContext) (List<XdmValue>) -> R
): BuiltinFunction<R> {

    constructor(
        name: String,
        returnType: AnyType,
        vararg argumentTypes: AnyType,
        evalFunction: context(ExprEvalContext) (List<XdmValue>) -> R
    ) : this(
        name,
        XdmSchemaType(returnType),
        argumentTypes.map { XdmSchemaType(it) },
        evalFunction
    )

/*
    constructor(
        returnType: AnyType,
        vararg argumentTypes: AnyType,
        evalFunction: context(ExprEvalContext) (List<XdmValue>) -> XsdAtomic
    ) : this(
        XdmSequenceType.Schema(returnType),
        argumentTypes.map { XdmSequenceType.Schema(it) },
        { XdmAtomic<XsdAtomic>(evalFunction(it)) }
    )
*/

    context(ctx: ExprEvalContext)
    override fun eval(args: List<XdmValue>): R = evalFunction(args)
}

context(ctx: ExprEvalContext)
inline fun <reified T : XdmValue> Collection<XdmValue>.singleArg(): T {
    return (singleOrNull() ?: throw EvaluationException(EvaluationException.ErrorCodes.FOAP0001_WRONG_ARG_CNT, ctx.expr)) as? T
        ?: throw EvaluationException(ctx.expr, "Argument not of expected type ${T::class.simpleName}")
}

context(ctx: ExprEvalContext)
inline fun <reified T: XsdAtomic> Collection<XdmValue>.singleAtomicArg(): T {
    val arg = singleOrNull() ?: throw EvaluationException(EvaluationException.ErrorCodes.FOAP0001_WRONG_ARG_CNT, ctx.expr)
    return (arg as? XdmAtomic<*>)?.value as? T
        ?: throw EvaluationException(ctx.expr, "Argument not of expected type ${T::class.simpleName}")
}

context(ctx: ExprEvalContext)
inline fun <reified T: XdmValue> List<XdmValue>.argN(arg: Int): T {
    return this[arg] as? T
        ?: throw EvaluationException(ctx.expr, "Argument not of expected type ${T::class.simpleName}")
}

context(ctx: ExprEvalContext)
inline fun <reified T: XsdAtomic> List<XdmValue>.atomicArgN(arg: Int): T {
    return (this[arg] as? XdmAtomic<*>)?.value as? T
        ?: throw EvaluationException(ctx.expr, "Argument not of expected type ${T::class.simpleName}")
}
