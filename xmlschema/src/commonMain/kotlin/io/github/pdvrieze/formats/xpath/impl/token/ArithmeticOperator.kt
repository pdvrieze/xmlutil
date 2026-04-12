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

package io.github.pdvrieze.formats.xpath.impl.token

import io.github.pdvrieze.formats.xpath.XPathVersion
import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.data.XdmAtomic
import io.github.pdvrieze.formats.xpath.eval.data.XdmSequence
import io.github.pdvrieze.formats.xpath.eval.data.XdmSingleValue
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.functions.Fn
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.*
import io.github.pdvrieze.xml.schematypes.values.*

abstract class ArithmeticOperator(
    literal: String,
    priority: Int,
    minVersion: XPathVersion = XPathVersion.XPath3_1,
    isDelimiting: Boolean
) : Operator(literal, priority, minVersion, isDelimiting) {
    @XPathInternal
    context(ctx: ExprEvalContext)
    fun normalizeToArithmetic(value: XdmValue<*>): XdmValue<*> {
        val v1 = value.atomize()
        // Handle sequences
        val v2: XdmSingleValue<*> = when (v1.size) {
            0 -> return if (ctx.isXPath1Compat) XdmAtomic.NaN else XdmSequence.EMPTY
            1 -> v1[0]
            else if ctx.isXPath1Compat -> v1[0]
            else -> throw EvaluationException.Companion(
                ErrorCodes.XPTY0004_TYPE_ERROR,
                "Sequence as arithmatic operand"
            )
        }
        if (v2 !is XdmAtomic<*>) throw EvaluationException.Companion(
            ErrorCodes.XPTY0004_TYPE_ERROR,
            "Value type ${value.staticType} is not compatible with an arithmetic operator"
        )
        when {
            v2.value is XsdDouble -> return v2
            !ctx.isXPath1Compat -> {
                if (v2.staticType == UntypedAtomicType.Instance) {
                    return v2.toXdmDouble()
                } else return v2
            }

            v2.value is XsdBoolean ||
                    v2.value is XsdDecimal ||
                    v2.value is XsdFloat ||
                    v2.staticType == UntypedAtomicType.Instance -> return Fn.number(v2)
        }
        return v2
    }

    @XPathInternal
    context(ctx: ExprEvalContext)
    override fun eval(left: XdmValue<*>, right: XdmValue<*>): XdmValue<*> {
        val l = when (val n = normalizeToArithmetic(left)) {
            is XdmAtomic<*> -> n
            XdmSequence.EMPTY -> return XdmSequence.EMPTY
            else -> throw EvaluationException("Implementation in number normalization")
        }
        val r = when (val n = normalizeToArithmetic(right)) {
            is XdmAtomic<*> -> n
            XdmSequence.EMPTY -> return XdmSequence.EMPTY
            else -> throw EvaluationException("Implementation in number normalization")
        }
        val lType = l.value.schemaType
        val rType = r.value.schemaType
        val requiredType = operatorMapping(lType, rType)
        val value = when (requiredType) {
            is DoubleType<*> -> XsdDouble.Companion(
                evalDouble(
                    l.toXdmDouble().value.value,
                    r.toXdmDouble().value.value
                )
            )

            is FloatType<*> -> XsdFloat.Companion(evalFloat(l.toXdmFloat().value.value, r.toXdmFloat().value.value))
            is IntegerType<*> -> evalInteger(l.toXdmInteger().value, r.toXdmInteger().value)
            is DecimalType<*> -> evalDecimal(l.toXdmDecimal().value, r.toXdmDecimal().value)
            else -> evalCustom(l.value, r.value)
        }
        return XdmAtomic(value)
    }

    context(ctx: ExprEvalContext) @XPathInternal
    protected open fun operatorMapping(
        lType: AnyAtomicType<XsdAtomic>,
        rType: AnyAtomicType<XsdAtomic>
    ): AnyAtomicType<XsdAtomic> = when {
        lType.name isEquivalent rType.name -> lType
        lType.isBaseOf(rType) -> lType
        rType.isBaseOf(lType) -> rType
        ctx.isXPath1Compat && lType is NumericType<*> && rType is NumericType<*> -> DoubleType.Instance

        else -> disjointOperatorMapping(lType, rType)
    }

    @XPathInternal
    context(ctx: ExprEvalContext)
    protected open fun disjointOperatorMapping(
        leftType: AnyAtomicType<*>,
        rightType: AnyAtomicType<*>
    ): AnyAtomicType<*> {
        return when {
            leftType is DoubleType || rightType is DoubleType -> DoubleType.Instance
            leftType is FloatType || rightType is FloatType -> FloatType.Instance
            leftType is IntegerType && rightType is IntegerType -> IntegerType.Instance
            leftType is DecimalType || rightType is DecimalType -> DecimalType.Instance
            else -> DoubleType.Instance
        }
    }

    @XPathInternal
    context(ctx: ExprEvalContext)
    open fun evalFloat(left: Float, right: Float): Float =
        TODO("Operation ${literal} is not defined on floats")

    @XPathInternal
    context(ctx: ExprEvalContext)
    open fun evalDouble(left: Double, right: Double): Double =
        TODO("Operation ${literal} is not defined on doubles")

    @XPathInternal
    context(ctx: ExprEvalContext)
    open fun evalInteger(left: XsdInteger, right: XsdInteger): XsdInteger =
        TODO("Operation ${literal} is not defined on xsd:integer")

    @XPathInternal
    context(ctx: ExprEvalContext)
    open fun evalDecimal(left: XsdDecimal, right: XsdDecimal): XsdDecimal =
        TODO("Operation ${literal} is not defined on xsd:decimal")

    @XPathInternal
    context(ctx: ExprEvalContext)
    open fun evalCustom(left: XsdAtomic, right: XsdAtomic): XsdAtomic {
        TODO("Custom evaluation of operator '$literal' not yet implemented for types ${left.schemaType.name} and ${right.schemaType.name}")
    }

//    fun evalNormalized(left: XdmAtomic<*>, right: XdmValue<*>)
}
