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
import io.github.pdvrieze.formats.xpath.data.XdmSequence
import io.github.pdvrieze.formats.xpath.data.XdmType
import io.github.pdvrieze.formats.xpath.data.XdmValue
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunctionImpl
import io.github.pdvrieze.formats.xpath.functions.singleArg
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.DoubleType
import io.github.pdvrieze.xml.schematypes.values.XsdAtomic
import io.github.pdvrieze.xml.schematypes.values.XsdDecimal
import io.github.pdvrieze.xml.schematypes.values.XsdDouble
import io.github.pdvrieze.xml.schematypes.values.XsdFloat
import io.github.pdvrieze.xml.schematypes.values.XsdInteger
import io.github.pdvrieze.xml.schematypes.values.XsdNumeric

@XPathInternal
object NumericFunctions: AbstractFunctionObject() {

    val fnAbs = BuiltinFunctionImpl("abs", functionType(XdmType.NUMERIC.opt, XdmType.NUMERIC.opt)) { args ->
        val n = toSingleAtomic<XsdNumeric<*>>(args) ?: return@BuiltinFunctionImpl XdmSequence.empty(XdmType.NUMERIC)
        XdmAtomic(n.abs() as XsdAtomic)
    }

    val fnCeiling = BuiltinFunctionImpl("ceiling", functionType(XdmType.NUMERIC.opt, XdmType.NUMERIC.opt)) { args ->
        val n = toSingleAtomic<XsdNumeric<*>>(args) ?: return@BuiltinFunctionImpl XdmSequence.empty(XdmType.NUMERIC)
        XdmAtomic(n.ceiling() as XsdAtomic)
    }

    val fnFloor = BuiltinFunctionImpl("floor", functionType(XdmType.NUMERIC.opt, XdmType.NUMERIC.opt)) { args ->
        val n = toSingleAtomic<XsdNumeric<*>>(args) ?: return@BuiltinFunctionImpl XdmSequence.empty(XdmType.NUMERIC)
        XdmAtomic(n.floor() as XsdAtomic)
    }

    val fnRound = BuiltinFunctionImpl("round", functionType(XdmType.NUMERIC.opt, XdmType.NUMERIC.opt)) { args ->
        val value = toAtomic<XsdNumeric<*>>(0, args) ?: return@BuiltinFunctionImpl XdmSequence.empty(XdmType.NUMERIC)
        val r = when (args.size) {
            1 -> value.round()
            2 -> value.round(toAtomic<XsdInteger>(1, args) ?: throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR))
            else -> throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT)
        }
        XdmAtomic(r as XsdAtomic)
    }

    val fnRoundHalfToEven = BuiltinFunctionImpl("round-half-to-even", functionType(XdmType.NUMERIC.opt, XdmType.NUMERIC.opt)) { args ->
        val value = toAtomic<XsdNumeric<*>>(0, args) ?: return@BuiltinFunctionImpl XdmSequence.empty(XdmType.NUMERIC)
        val r = when (args.size) {
            1 -> value.roundToHalfEven()
            2 -> value.roundToHalfEven(toAtomic<XsdInteger>(1, args) ?: throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR))
            else -> throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT)
        }
        XdmAtomic(r as XsdAtomic)
    }

    val fnNumber = BuiltinFunctionImpl("number", contextFunctionTypes(t(DoubleType.Instance), XdmType.ATOMIC.opt)) { args ->
        val arg = if (args.isEmpty()) contextOf<ExprEvalContext>().contextItem else args.singleArg<XdmValue>()
        if(arg !is XdmAtomic<*>) return@BuiltinFunctionImpl XdmAtomic.NaN

        @Suppress("UNCHECKED_CAST")
        when (val value = arg.value) {
            is XsdDouble -> arg as XdmAtomic<XsdDouble>
            is XsdFloat -> XdmAtomic(XsdDouble(value.value.toDouble()))
            is XsdDecimal -> XdmAtomic(XsdDouble(value.toDouble()))
            else -> XdmAtomic(XsdDouble(value.xmlString.toDoubleOrNull()?: Double.NaN))
        }
    }

}
