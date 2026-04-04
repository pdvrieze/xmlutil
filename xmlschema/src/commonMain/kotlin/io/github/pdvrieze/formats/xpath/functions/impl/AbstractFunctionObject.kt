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

import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.data.XdmAtomic
import io.github.pdvrieze.formats.xpath.eval.data.XdmNode
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.eval.type.XdmFunctionType
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmNodeKindTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmSchemaTypeTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmSequenceTypeTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmTypeTest
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.NodeKindTest
import io.github.pdvrieze.formats.xpath.impl.SequenceType
import io.github.pdvrieze.formats.xpath.impl.SequenceType.OccurrenceType.SINGLE
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.*
import io.github.pdvrieze.xml.schematypes.values.XsdAnySimple
import kotlin.reflect.typeOf

@XPathInternal
abstract class AbstractFunctionObject() {

    context(ctx: ExprEvalContext)
    protected fun argOrContext(
        index: Int,
        args: List<XdmValue>,
        allowContext: Boolean = false
    ): XdmValue? = when (args.size - index){
        0 if allowContext -> ctx.contextItem
        1 -> args[index]
        else -> throw EvaluationException(ErrorCodes.XPST0017_ARGS_MISMATCH)
    }

    context(ctx: ExprEvalContext)
    protected fun toSingleArg(args: List<XdmValue>, allowContext: Boolean = false): XdmValue? {
        return argOrContext(0, args, allowContext)
    }

    context(ctx: ExprEvalContext)
    protected fun toAnySingleAtomic(args: List<XdmValue>, allowContext: Boolean = false): XdmAtomic<*>? {
        val argOrContext = toSingleArg(args, allowContext) ?: throw EvaluationException(ErrorCodes.XPDY0002_ABSENT_DYNAMIC_CONTEXT)

        val atom = argOrContext.atomize()

        return when {
            atom.size == 0 -> null
            atom is XdmAtomic<*> -> atom
            else -> throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Expected atomic, found: ${atom.staticType} ($atom)")
        }
    }

    context(ctx: ExprEvalContext)
    protected fun toAnyAtomic(pos: Int, args: List<XdmValue>): XdmAtomic<*>? {
        val arg = args.getOrNull(pos) ?: throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT)
        return when {
            arg.size == 0 -> null
            arg is XdmAtomic<*> -> arg
            else -> throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Expected atomic, found: ${arg.staticType}")
        }
    }

    context(ctx: ExprEvalContext)
    protected inline fun <reified T: XsdAnySimple> toSingleAtomic(args: List<XdmValue>, allowContext: Boolean = false): T? {
        val arg = toAnySingleAtomic(args, allowContext) ?: return null
        return arg.value as? T
            ?: throw EvaluationException(
                ErrorCodes.XPTY0004_TYPE_ERROR,
                "Expected atomic of type ${typeOf<T>()}, found: ${arg.staticType}"
            )
    }

    context(ctx: ExprEvalContext)
    protected inline fun <reified T: XsdAnySimple> toAtomic(pos: Int, args: List<XdmValue>): T? {
        val arg = toAnyAtomic(pos, args) ?: return null
        return arg.value as? T
            ?: throw EvaluationException(
                ErrorCodes.XPTY0004_TYPE_ERROR,
                "Expected atomic of type ${typeOf<T>()}, found: ${arg.staticType}"
            )
    }

    context(ctx: ExprEvalContext)
    protected fun toSingleNode(args: List<XdmValue>, allowContext: Boolean = false): XdmNode? {
        val arg = toSingleArg(args, allowContext) ?: throw EvaluationException(ErrorCodes.XPDY0002_ABSENT_DYNAMIC_CONTEXT)
        if (arg.size == 0) return null
        return arg as? XdmNode ?: throw EvaluationException(
            ErrorCodes.XPTY0004_TYPE_ERROR,
            "Expected node, found: ${arg.staticType}"
        )
    }


    protected fun flexFunctionType(returnType: XdmSequenceTypeTest, vararg argTypes: XdmSequenceTypeTest): XdmFunctionType =
        XdmFunctionType(argTypes.toList(), returnType, true)

    protected fun flexFunctionType(returnType: AnyType, vararg argTypes: AnyType): XdmFunctionType =
        XdmFunctionType(argTypes.map { t(it) }, t(returnType), true)

    protected fun flexFunctionType(returnType: XdmTypeTest, vararg argTypes: AnyType): XdmFunctionType =
        XdmFunctionType(argTypes.map { t(it) }, returnType, true)

    protected fun flexFunctionType(returnType: AnyType, vararg argTypes: XdmTypeTest): XdmFunctionType =
        XdmFunctionType(argTypes.toList(), t(returnType), true)

    protected fun functionType(returnType: XdmSequenceTypeTest, vararg argTypes: XdmSequenceTypeTest): XdmFunctionType =
        XdmFunctionType(argTypes.toList(), returnType)

    protected fun functionType(returnType: XdmTypeTest): XdmFunctionType =
        XdmFunctionType(emptyList(), returnType)

    protected fun functionType(returnType: AnyType, vararg argTypes: AnyType): XdmFunctionType =
        XdmFunctionType(argTypes.map { t(it) }, t(returnType))

    protected fun functionType(returnType: XdmTypeTest, vararg argTypes: AnyType): XdmFunctionType =
        XdmFunctionType(argTypes.map { t(it) }, returnType)

    protected fun functionType(returnType: AnyType, vararg argTypes: XdmTypeTest): XdmFunctionType =
        XdmFunctionType(argTypes.toList(), t(returnType))

    protected fun contextFunctionTypes(returnType: AnyType, contextParam: AnyType, vararg argTypes: AnyType): List<XdmFunctionType> = listOf(
        functionType(returnType, *argTypes),
        functionType(returnType, contextParam, *argTypes)
    )

    protected fun contextFunctionTypes(returnType: XdmTypeTest, contextParam: XdmTypeTest, vararg argTypes: XdmTypeTest): List<XdmFunctionType> = listOf(
        XdmFunctionType(argTypes.toList(), returnType),
        XdmFunctionType(listOf(*argTypes, contextParam), returnType)
    )

    protected fun contextFunctionTypes(returnType: XdmTypeTest, contextParam: AnyType, vararg argTypes: AnyType): List<XdmFunctionType> =listOf(
        functionType(returnType, *argTypes),
        functionType(returnType, contextParam, *argTypes)
    )

    protected fun contextFunctionTypes(returnType: AnyType, contextParam: XdmTypeTest, vararg argTypes: XdmTypeTest): List<XdmFunctionType> =listOf(
        functionType(returnType, *argTypes),
        functionType(returnType, contextParam, *argTypes)
    )

    protected fun t(type: AnyType): XdmSchemaTypeTest = XdmSchemaTypeTest(type, SINGLE)

    val AnyAtomicType<*>.opt: XdmSchemaTypeTest
        get() = XdmSchemaTypeTest(this, SequenceType.OccurrenceType.OPTIONAL)

    val AnyAtomicType<*>.any: XdmSchemaTypeTest
        get() = XdmSchemaTypeTest(this, SequenceType.OccurrenceType.ANY)

    val AnyAtomicType<*>.atLeastOne: XdmSchemaTypeTest
        get() = XdmSchemaTypeTest(this, SequenceType.OccurrenceType.AT_LEAST_ONE)


    companion object {
        val BOOLEAN = XdmSchemaTypeTest(BooleanType.Instance, SINGLE)
        val STRING = XdmSchemaTypeTest(StringType.Instance, SINGLE)
        val NODE = XdmNodeKindTest(NodeKindTest.AnyNode, SINGLE)
        val ITEM = XdmTypeTest.ANY
        val ATOMIC = XdmSchemaTypeTest(AnyAtomicType.Instance, SINGLE)
        val NUMERIC = XdmSchemaTypeTest(NumericType.Instance, SINGLE)
        val INTEGER = XdmSchemaTypeTest(IntegerType.Instance, SINGLE)
    }
}
