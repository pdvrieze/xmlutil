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

package io.github.pdvrieze.formats.xpath.functions.impl

import io.github.pdvrieze.formats.xpath.data.ErrorCodes
import io.github.pdvrieze.formats.xpath.data.EvaluationException
import io.github.pdvrieze.formats.xpath.data.XdmAtomic
import io.github.pdvrieze.formats.xpath.data.XdmFunctionType
import io.github.pdvrieze.formats.xpath.data.XdmNode
import io.github.pdvrieze.formats.xpath.data.XdmSchemaType
import io.github.pdvrieze.formats.xpath.data.XdmType
import io.github.pdvrieze.formats.xpath.data.XdmValue
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunction
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunctionImpl
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.AnyType
import io.github.pdvrieze.xml.schematypes.values.XsdAnySimple
import io.github.pdvrieze.xml.schematypes.values.XsdAtomic
import kotlin.reflect.typeOf

@XPathInternal
abstract class AbstractFunctionObject() {

    context(ctx: ExprEvalContext)
    protected fun argOrContext(index: Int, args: List<XdmValue>): XdmValue? = when (args.size - index){
        0 -> ctx.contextItem
        1 -> args[index]
        else -> throw EvaluationException.Companion(ErrorCodes.FOAP0001_WRONG_ARG_CNT)
    }

    context(ctx: ExprEvalContext)
    protected fun toSingleArg(args: List<XdmValue>): XdmValue? {
        return argOrContext(0, args)
    }

    context(ctx: ExprEvalContext)
    protected fun toAnySingleAtomic(args: List<XdmValue>): XdmAtomic<*>?{
        val arg = toSingleArg(args) ?: throw EvaluationException.Companion(ErrorCodes.XPDY0002_ABSENT_DYNAMIC_CONTEXT)
        if (arg.size == 0) return null
        if (arg !is XdmAtomic<*>) throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Expected atomic, found: ${arg.type}")
        @Suppress("UNCHECKED_CAST")
        return arg
    }

    context(ctx: ExprEvalContext)
    protected fun toAnyAtomic(pos: Int, args: List<XdmValue>): XdmAtomic<*>?{
        val arg = args.getOrNull(pos) ?: throw EvaluationException.Companion(ErrorCodes.FOAP0001_WRONG_ARG_CNT)
        if (arg.size == 0) return null
        if (arg !is XdmAtomic<*>) throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Expected atomic, found: ${arg.type}")
        @Suppress("UNCHECKED_CAST")
        return arg
    }

    context(ctx: ExprEvalContext)
    protected inline fun <reified T: XsdAnySimple> toSingleAtomic(args: List<XdmValue>): T? {
        val arg = toAnySingleAtomic(args) ?: return null
        return arg.value as? T
            ?: throw EvaluationException(
                ErrorCodes.XPTY0004_TYPE_ERROR,
                "Expected atomic of type ${typeOf<T>()}, found: ${arg.type}"
            )
    }

    context(ctx: ExprEvalContext)
    protected inline fun <reified T: XsdAnySimple> toAtomic(pos: Int, args: List<XdmValue>): T? {
        val arg = toAnyAtomic(pos, args) ?: return null
        return arg.value as? T
            ?: throw EvaluationException(
                ErrorCodes.XPTY0004_TYPE_ERROR,
                "Expected atomic of type ${typeOf<T>()}, found: ${arg.type}"
            )
    }

    context(ctx: ExprEvalContext)
    protected fun toSingleNode(args: List<XdmValue>): XdmNode? {
        val arg = toSingleArg(args) ?: throw EvaluationException.Companion(ErrorCodes.XPDY0002_ABSENT_DYNAMIC_CONTEXT)
        if (arg.size == 0) return null
        return arg as? XdmNode ?: throw EvaluationException.Companion(
            ErrorCodes.XPTY0004_TYPE_ERROR,
            "Expected node, found: ${arg.type}"
        )
    }


    protected fun functionType(returnType: XdmType, vararg argTypes: XdmType): XdmFunctionType =
        XdmFunctionType(argTypes.toList(), returnType)

    protected fun functionType(returnType: AnyType, vararg argTypes: AnyType): XdmFunctionType =
        XdmFunctionType(argTypes.map { XdmSchemaType(it) }, XdmSchemaType(returnType))

    protected fun functionType(returnType: XdmType, vararg argTypes: AnyType): XdmFunctionType =
        XdmFunctionType(argTypes.map { XdmSchemaType(it) }, returnType)

    protected fun functionType(returnType: AnyType, vararg argTypes: XdmType): XdmFunctionType =
        XdmFunctionType(argTypes.toList(), XdmSchemaType(returnType))

    protected fun contextFunctionTypes(returnType: AnyType, contextParam: AnyType, vararg argTypes: AnyType): List<XdmFunctionType> = listOf(
        functionType(returnType, *argTypes),
        functionType(returnType, contextParam, *argTypes)
    )

    protected fun contextFunctionTypes(returnType: XdmType, contextParam: XdmType, vararg argTypes: XdmType): List<XdmFunctionType> = listOf(
        XdmFunctionType(returnType, *argTypes),
        XdmFunctionType(returnType, *argTypes, contextParam)
    )

    protected fun contextFunctionTypes(returnType: XdmType, contextParam: AnyType, vararg argTypes: AnyType): List<XdmFunctionType> =listOf(
        functionType(returnType, *argTypes),
        functionType(returnType, contextParam, *argTypes)
    )

    protected fun contextFunctionTypes(returnType: AnyType, contextParam: XdmType, vararg argTypes: XdmType): List<XdmFunctionType> =listOf(
        functionType(returnType, *argTypes),
        functionType(returnType, contextParam, *argTypes)
    )

    protected fun t(type: AnyType): XdmSchemaType = XdmSchemaType(type)

}
