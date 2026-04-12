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
import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.data.*
import io.github.pdvrieze.formats.xpath.eval.type.XdmFunctionType
import io.github.pdvrieze.formats.xpath.eval.typeTest.*
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunction
import io.github.pdvrieze.formats.xpath.functions.Fn
import io.github.pdvrieze.formats.xpath.functions.Xs
import io.github.pdvrieze.xml.schematypes.types.DoubleType
import io.github.pdvrieze.xml.schematypes.types.FloatType
import io.github.pdvrieze.xml.schematypes.types.StringType
import io.github.pdvrieze.xml.schematypes.types.UntypedAtomicType
import io.github.pdvrieze.xml.schematypes.values.*
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

    context(ctx: ExprEvalContext)
    private fun promoteArgumentSequence(arg: XdmValue<*>, type: XdmSequenceTypeTest): XdmValue<*> {
        when (type) {
            is XdmSequenceTypeTest.NONE -> throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "None cannot be instantiated")
            is XdmSequenceTypeTest.EMPTY -> when(arg.size) {
                0 -> return XdmSequence.EMPTY
                else -> throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Expected empty sequence, but got ${arg.size} items")
            }
            is XdmTypeTest -> if (!type.cardinality.matches(arg.size)) throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "In $name expected sequence of cardinality ${type.cardinality}, but got ${arg.size} items")
        }

        val atomizedArg = if (type is XdmSchemaTypeTest) arg.atomize() else arg

        when (atomizedArg) {
            XdmSequence.EMPTY -> return XdmSequence.EMPTY
            is XdmSingleValue<*> -> return promoteArgument(atomizedArg, type)
            else -> return XdmSequence.buildSingle {
                for (a in atomizedArg) {
                    add(promoteArgument(a, type))
                }
            }
        }
    }

    context(ctx: ExprEvalContext)
    private fun promoteArgument(arg: XdmSingleValue<*>, type: XdmTypeTest): XdmValue<*> {
        when {
            type.isInstance(arg) -> return arg
            arg !is XdmAtomic<*> -> throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "In $name expected type $type, but got ${arg.staticType}")
        }


        val neededSchemaType = when (type) {
            is XdmTypeTest.AnyItem -> return arg
            is XdmNodeKindTest -> throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "In $name expected node of kind ${type.nodeKind} but got ${arg.staticType}")
            is XdmFunctionTypeTest -> throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "In $name expected function type $type, but got $arg")
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

            else -> throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "In ${name} Expected type $neededSchemaType, but got ${arg.staticType}")
        }
    }

    context(ctx: ExprEvalContext)
    private fun promoteArguments(args: List<XdmValue<*>>, functionType: XdmFunctionType): List<XdmValue<*>> {
        val result = args.mapIndexed { idx, arg ->
            val argType = when {
                idx < functionType.argTypes.size -> functionType.argTypes[idx]
                functionType.isVarArg -> functionType.argTypes.last()
                else -> throw EvaluationException(
                    ErrorCodes.XPST0017_ARGS_MISMATCH,
                    "Function with name ${name} has no matching signature"
                )
            }
            promoteArgumentSequence(arg, argType)
        }
        return result
    }

    @OptIn(NeedsXPath3_1::class)
    context(ctx: EvalContext)
    @XPathInternal
    override fun eval(): XdmValue<*> {
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
        val functionType = function.functionTypes.singleOrNull { it.isVarArg || it.argTypes.size == args.size }
            ?: throw EvaluationException(ErrorCodes.XPST0017_ARGS_MISMATCH, this, "Function with name ${name} has no matching signature")


        return withExprContext {
            val initialArgs = args.map { (it as ExprSingle).eval() }
            val evalArgs = promoteArguments(initialArgs, functionType)
            function.invoke(evalArgs)
        }
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

