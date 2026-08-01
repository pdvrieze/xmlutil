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
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmArrayTypeTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmFunctionTypeTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmMapTypeTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmTypeTest
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.Collation

@OptIn(XPathInternal::class)
class XdmPartialApplication(
    val function: XdmFunction<*>,
    val args: List<XdmValue<*>?>,
    override val staticType: XdmFunctionType = function.partialStaticType(args)
) : XdmFunction<XdmPartialApplication>() {
    override fun asT(): XdmPartialApplication = this

    override val dynamicType: XdmFunctionType
        get() = function.partialDynType(args)

    private val paramIdxToOrigIdxMap = IntArray(staticType.argTypes.size)

    init {
        check(args.size == function.staticType.argTypes.size) { "partial application should have values for all arguments of the original" }
        var nextParamIdx = 0
        for (i in args.indices) {
            if (args[i] == null) paramIdxToOrigIdxMap[nextParamIdx++] = i
        }
    }

    override fun isValEqual(expected: XdmValue<*>, collation: Collation?): Boolean {
        return this == expected
    }

    context(ctx: ExprEvalContext)
    override fun isDeepEqual(
        other: XdmValue<*>,
        collation: Collation?
    ): Boolean {
        return isValEqual(other, collation)
    }

    context(ctx: ExprEvalContext)
    override fun treatAsNonEmpty(type: XdmTypeTest): XdmPartialApplication {
        when (type) {
            is XdmArrayTypeTest,
            is XdmMapTypeTest -> throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Cannot cast partial application to $type")
            is XdmTypeTest.AnyItem,
            is XdmFunctionTypeTest.AnyFunction -> return XdmPartialApplication(function, args, dynamicType)
            is XdmFunctionTypeTest.Typed -> if (type.argTypes.size != args.size) throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Cannot cast partial application to $type - invalid argument count")
            else -> throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Cannot cast partial application to $type")
        }

        if (! type.returnType.isAssignableTo(function.staticType.returnType)) throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Cannot cast partial application to $type - return type mismatch")
        for (i in type.argTypes.indices) {
            val newArgType = type.argTypes[i]
            val oldArgType = function.staticType.argTypes[paramIdxToOrigIdxMap[i]]
            if (! oldArgType.isAssignableTo(newArgType)) throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Cannot cast partial application to $type - argument type mismatch")
        }
        return XdmPartialApplication(function, args, type.toValueType(dynamicType).single as XdmFunctionType)
    }

    @XPathInternal
    context(ctx: ExprEvalContext)
    override fun normalizeToArithmetic(): XdmValue<*> {
        throw EvaluationException(
            ErrorCodes.XPTY0004_TYPE_ERROR,
            "Partial function applications not compatible with an arithmetic operator"
        )
    }

    context(ctx: ExprEvalContext)
    override fun invoke(args: List<XdmValue<*>>): XdmValue<*> {
        val newArgIterator = args.iterator()
        val oldArgs = this.args
        val newArgs = buildList {
            for (oldArgIdx in oldArgs.indices) {
                val oldArg = oldArgs[oldArgIdx]
                when(oldArg) {
                    null if !newArgIterator.hasNext() -> when {
                        !staticType.isVarArg || oldArgIdx + 1 < args.size ->
                            throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT)
                    }

                    null -> add(newArgIterator.next())

                    else -> add(oldArg)
                }
            }
            if (newArgIterator.hasNext()) {
                if (! staticType.isVarArg) throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT)
                do {
                    add(newArgIterator.next())
                } while (newArgIterator.hasNext())
            }
        }

        return function(newArgs)
    }
}
