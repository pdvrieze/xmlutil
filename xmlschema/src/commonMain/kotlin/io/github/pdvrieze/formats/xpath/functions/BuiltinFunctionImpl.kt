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

import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.data.XdmAtomic
import io.github.pdvrieze.formats.xpath.eval.data.XdmSequence
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.eval.type.XdmFunctionType
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmTypeTest
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunction.Companion.FN_NAMESPACE
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.Collation
import io.github.pdvrieze.xml.schematypes.types.AnyType
import io.github.pdvrieze.xml.schematypes.values.XsdAtomic
import io.github.pdvrieze.xml.schematypes.values.XsdString
import nl.adaptivity.xmlutil.QName

@XPathInternal
class BuiltinFunctionImpl<out R: XdmValue<*>>(
    override val functionName: QName,
    override val functionTypes: List<XdmFunctionType>,
    val evalFunction: context(ExprEvalContext) (List<XdmValue<*>>) -> R
): BuiltinFunction<R> {

    constructor(
        functionName: String,
        functionTypes: List<XdmFunctionType>,
        evalFunction: context(ExprEvalContext) (List<XdmValue<*>>) -> R
    ): this(QName(FN_NAMESPACE, functionName), functionTypes, evalFunction)

    constructor(
        functionName: String,
        functionType: XdmFunctionType,
        evalFunction: context(ExprEvalContext) (List<XdmValue<*>>) -> R
    ): this(QName(FN_NAMESPACE, functionName), listOf(functionType), evalFunction)

    constructor(
        name: QName,
        returnType: AnyType,
        vararg argumentTypes: AnyType,
        evalFunction: context(ExprEvalContext) (List<XdmValue<*>>) -> R
    ) : this(
        name,
        listOf(XdmFunctionType(returnType, *argumentTypes)),
        evalFunction
    )

    constructor(
        name: QName,
        returnType: XdmTypeTest,
        vararg argumentTypes: XdmTypeTest,
        evalFunction: context(ExprEvalContext) (List<XdmValue<*>>) -> R
    ) : this(
        name,
        listOf(XdmFunctionType(returnType, *argumentTypes)),
        evalFunction
    )

    constructor(
        name: String,
        returnType: XdmTypeTest,
        vararg argumentTypes: XdmTypeTest,
        evalFunction: context(ExprEvalContext) (List<XdmValue<*>>) -> R
    ) : this(
        name,
        listOf(XdmFunctionType(returnType, *argumentTypes)),
        evalFunction
    )

    constructor(
        name: String,
        returnType: AnyType,
        vararg argumentTypes: AnyType,
        evalFunction: context(ExprEvalContext) (List<XdmValue<*>>) -> R
    ) : this(
        name,
        listOf(XdmFunctionType(returnType, *argumentTypes)),
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
    override fun invoke(args: List<XdmValue<*>>): R = evalFunction(args)
}

context(ctx: ExprEvalContext)
internal inline fun <reified T : XdmValue<*>> List<XdmValue<*>>.singleArg(): T {
    checkArgCount(1)
    return get(0) as? T
        ?: throw EvaluationException(
            "Argument not of expected type ${T::class.simpleName}, found: '${get(0)}'"
        )
}

context(ctx: ExprEvalContext)
internal fun List<XdmValue<*>>.checkArgCount(expected: Int) {
    if (size != expected)
        throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT, "Expected $expected arguments, found ${size}")
}


context(ctx: ExprEvalContext)
internal inline fun <reified T: XsdAtomic> List<XdmValue<*>>.atomicArgOrEmpty(idx: Int): T? {
    val arg: XdmAtomic<*> = when (val a = get(idx)) {
        is XdmSequence.EMPTY -> return null
        is XdmAtomic<*> -> a
        is XdmSequence<*> -> when (a.size) {
            0 -> return null
            1 -> a[0] as? XdmAtomic<*> ?: throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Expected atomic, found sequence: ${a[0].staticType}")
            else -> throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Expected atomic, found sequence: ${a.staticType}")
        }
        else -> throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Expected atomic, found: ${a.staticType}")
    }
    if (arg.value !is T) throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Expected atomic of type ${T::class.simpleName}, found: ${arg.staticType}")
    return arg.value
}

context(ctx: ExprEvalContext)
internal inline fun <reified T: XdmValue<*>> List<XdmValue<*>>.xdmArg(idx: Int): T {
    val r = get(idx) as? T ?: throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Expected ${T::class.simpleName}, found: ${get(idx)}")
    return r
}


context(ctx: ExprEvalContext)
internal inline fun <reified T: XsdAtomic> List<XdmValue<*>>.singleAtomicArg(): T {
    checkArgCount(1)
    return atomicArgN(0)
}

context(ctx: ExprEvalContext)
internal inline fun <reified T: XdmValue<*>> List<XdmValue<*>>.argN(arg: Int): T {
    return this[arg] as? T
        ?: throw EvaluationException("Argument not of expected type ${T::class.simpleName}")
}

context(ctx: ExprEvalContext)
internal fun List<XdmValue<*>>.maybeCollation(pos: Int): Collation {
    return when {
        pos < size -> {
            val cName = atomicArgN<XsdString>(pos).xmlString
            ctx.collation(cName)
                ?: throw EvaluationException(ErrorCodes.FOCH0002, "Unsupported collation: $cName")
        }

        else -> ctx.defaultCollation
    }
}

context(ctx: ExprEvalContext)
internal inline fun <reified T: XsdAtomic> List<XdmValue<*>>.atomicArgN(arg: Int): T {
    return atomicArgOrEmpty<T>(arg) ?: throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Argument ${arg+1} is empty sequence, but should be atomic")
}
