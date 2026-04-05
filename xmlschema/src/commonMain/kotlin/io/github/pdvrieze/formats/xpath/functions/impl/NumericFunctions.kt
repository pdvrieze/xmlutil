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
import io.github.pdvrieze.formats.xpath.eval.data.*
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunctionImpl
import io.github.pdvrieze.formats.xpath.functions.singleArg
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.DoubleType
import io.github.pdvrieze.xml.schematypes.values.*

@XPathInternal
object NumericFunctions: AbstractFunctionObject() {

    val fnAbs: BuiltinFunctionImpl<XdmAtomicOrEmpty<XdmNumeric>> = BuiltinFunctionImpl(
        "abs",
        functionType(NUMERIC.opt, NUMERIC.opt)
    ) { args ->
        val n = toSingleAtomic<XsdNumeric<*>>(args, false) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        XdmAtomic(n.abs())
    }

    val fnCeiling: BuiltinFunctionImpl<XdmAtomicOrEmpty<XdmNumeric>> = BuiltinFunctionImpl(
        "ceiling",
        functionType(NUMERIC.opt, NUMERIC.opt)
    ) { args ->
        val n = toSingleAtomic<XsdNumeric<*>>(args, false) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        XdmAtomic(n.ceiling())
    }

    val fnFloor: BuiltinFunctionImpl<XdmAtomicOrEmpty<XdmNumeric>> = BuiltinFunctionImpl(
        "floor",
        functionType(NUMERIC.opt, NUMERIC.opt)
    ) { args ->
        val n = toSingleAtomic<XsdNumeric<*>>(args, false) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        XdmAtomic(n.floor())
    }

    val fnRound: BuiltinFunctionImpl<XdmAtomicOrEmpty<XdmNumeric>> = BuiltinFunctionImpl(
        "round",
        functionType(NUMERIC.opt, NUMERIC.opt)
    ) { args ->
        val value = toAtomic<XsdNumeric<*>>(0, args) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        val r = when (args.size) {
            1 -> value.round()
            2 -> value.round(toAtomic<XsdInteger>(1, args) ?: throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR))
            else -> throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT)
        }
        XdmAtomic(r)
    }

    val fnRoundHalfToEven: BuiltinFunctionImpl<XdmAtomicOrEmpty<XdmNumeric>> = BuiltinFunctionImpl(
        "round-half-to-even",
        functionType(NUMERIC.opt, NUMERIC.opt)
    ) { args ->
        val value = toAtomic<XsdNumeric<*>>(0, args) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        val r = when (args.size) {
            1 -> value.roundToHalfEven()
            2 -> value.roundToHalfEven(
                toAtomic<XsdInteger>(1, args) ?: throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR)
            )

            else -> throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT)
        }
        XdmAtomic(r)
    }

    val fnNumber: BuiltinFunctionImpl<XdmAtomicOrEmpty<XdmNumeric>> = BuiltinFunctionImpl(
        "number",
        contextFunctionTypes(t(DoubleType.Instance), ATOMIC.opt)
    ) { args ->
        val arg = if (args.isEmpty()) contextOf<ExprEvalContext>().contextItem else args.singleArg<XdmValue<*>>()
        if (arg !is XdmAtomic<*>) return@BuiltinFunctionImpl XdmAtomic.NaN

        @Suppress("UNCHECKED_CAST")
        when (val value = arg.value) {
            is XsdDouble -> arg as XdmAtomic<XsdDouble>
            is XsdFloat -> XdmAtomic(XsdDouble(value.value.toDouble()))
            is XsdDecimal -> XdmAtomic(XsdDouble(value.toDouble()))
            else -> XdmAtomic(XsdDouble(value.xmlString.toDoubleOrNull() ?: Double.NaN))
        }
    }

}
