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
import io.github.pdvrieze.formats.xpath.eval.data.XdmSingleValue
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.eval.type.XdmFunctionType
import io.github.pdvrieze.formats.xpath.eval.typeTest.*
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.DoubleType
import io.github.pdvrieze.xml.schematypes.types.FloatType
import io.github.pdvrieze.xml.schematypes.types.StringType
import io.github.pdvrieze.xml.schematypes.types.UntypedAtomicType
import io.github.pdvrieze.xml.schematypes.values.*
import nl.adaptivity.xmlutil.QName

interface XFunction<out R : XdmValue<*>> {
    val functionName: QName

    val functionTypes: List<XdmFunctionType>

    @XPathInternal
    context(ctx: ExprEvalContext)
    fun getReturnType(): XdmSequenceTypeTest {
        return functionTypes.asSequence()
            .map { it.returnType }
            .reduce { acc, type -> acc.sharedBaseType(type) }
    }

    @XPathInternal
    context(ctx: ExprEvalContext)
    operator fun invoke(vararg args: XdmValue<*>): R = invoke(args.toList())

    /**
     * Actually invoke the function. The arguments must already be promoted and of the correct type.
     */
    @XPathInternal
    context(ctx: ExprEvalContext)
    operator fun invoke(args: List<XdmValue<*>>): R

    @XPathInternal
    context(ctx: ExprEvalContext)
    fun invokePromoting(vararg args: XdmValue<*>): R = invokePromoting(args.toList())

    /**
     * Invoke the function by applying promotion of arguments.
     */
    @XPathInternal
    context(ctx: ExprEvalContext)
    fun invokePromoting(args: List<XdmValue<*>>): R {
        val functionType = functionTypes.singleOrNull { it.isVarArg || it.argTypes.size == args.size }
            ?: throw EvaluationException(ErrorCodes.XPST0017_ARGS_MISMATCH, "Function with name $functionName has no matching signature")

        val evalArgs = promoteArguments(args, functionType, functionName)
        return invoke(evalArgs)
    }

    companion object {

        context(ctx: ExprEvalContext)
        private fun promoteArgumentSequence(arg: XdmValue<*>, type: XdmSequenceTypeTest, funName: QName): XdmValue<*> {
            when (type) {
                is XdmSequenceTypeTest.NONE -> throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "None cannot be instantiated")
                is XdmSequenceTypeTest.EMPTY -> when(arg.size) {
                    0 -> return XdmSequence.EMPTY
                    else -> throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Expected empty sequence, but got ${arg.size} items")
                }
                is XdmTypeTest -> if (!type.cardinality.matches(arg.size)) throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "In $funName expected sequence of cardinality ${type.cardinality}, but got ${arg.size} items")
            }

            val atomizedArg = if (type is XdmSchemaTypeTest) arg.atomize() else arg

            when (atomizedArg) {
                XdmSequence.EMPTY -> return XdmSequence.EMPTY
                is XdmSingleValue<*> -> return promoteArgument(atomizedArg, type, funName)
                else -> return XdmSequence.buildSingle {
                    for (a in atomizedArg) {
                        add(promoteArgument(a, type, funName))
                    }
                }
            }
        }

        context(ctx: ExprEvalContext)
        private fun promoteArgument(arg: XdmSingleValue<*>, type: XdmTypeTest, funName: QName): XdmValue<*> {
            when {
                type.isInstance(arg) -> return arg
                arg !is XdmAtomic<*> -> throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "In $funName expected type $type, but got ${arg.staticType}")
            }


            val neededSchemaType = when (type) {
                is XdmTypeTest.AnyItem -> return arg
                is XdmNodeKindTest -> throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "In $funName expected node of kind ${type.nodeKind} but got ${arg.staticType}")
                is XdmFunctionTypeTest -> throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "In $funName expected function type $type, but got $arg")
                is XdmSchemaTypeTest -> type.schemaType
            }
            val argType = arg.dynamicType.schemaType
            val argValue = arg.value
            when {
                argType is UntypedAtomicType -> return XdmAtomic(argType.castFrom(argValue))

                neededSchemaType.name isEquivalent FloatType.Instance.name &&
                        argValue is XsdDecimal -> return XdmAtomic(XsdFloat(argValue.toFloat()))

                neededSchemaType.name isEquivalent DoubleType.Instance.name &&
                        argValue is XsdNumeric<*> -> return XdmAtomic(XsdDouble(argValue.toDouble()))

                neededSchemaType.name isEquivalent StringType.Instance.name &&
                        (argValue is XsdAnyURI) -> return XdmAtomic(XsdString(argValue.value))

                else -> throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "In ${funName} Expected type $neededSchemaType, but got ${arg.staticType}")
            }
        }

        context(ctx: ExprEvalContext)
        internal fun promoteArguments(args: List<XdmValue<*>>, functionType: XdmFunctionType, funName: QName): List<XdmValue<*>> {
            val result = args.mapIndexed { idx, arg ->
                val argType = when {
                    idx < functionType.argTypes.size -> functionType.argTypes[idx]
                    functionType.isVarArg -> functionType.argTypes.last()
                    else -> throw EvaluationException(ErrorCodes.XPST0017_ARGS_MISMATCH, "Function with name $funName has no matching signature")
                }
                promoteArgumentSequence(arg, argType, funName)
            }
            return result
        }

    }
}

