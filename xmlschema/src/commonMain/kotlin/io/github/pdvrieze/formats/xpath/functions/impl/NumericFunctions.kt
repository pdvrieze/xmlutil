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

import io.github.pdvrieze.formats.xpath.XPathVersion
import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.data.*
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunctionImpl
import io.github.pdvrieze.formats.xpath.functions.atomicArgN
import io.github.pdvrieze.formats.xpath.functions.atomicArgOrEmpty
import io.github.pdvrieze.formats.xpath.functions.singleArg
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.DoubleType
import io.github.pdvrieze.xml.schematypes.values.*
import io.github.pdvrieze.xml.schematypes.values.formatters.IntegerFormatter
import io.github.pdvrieze.xml.schematypes.values.formatters.NumberFormatter
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.xmlTrimWhitespace

@XPathInternal
object NumericFunctions: AbstractFunctionObject() {

    //region functions on numeric values 4.4
    val fnAbs: BuiltinFunctionImpl<XdmAtomicOrEmpty<XdmNumeric>> = BuiltinFunctionImpl.Fn(
        "abs",
        functionType(NUMERIC.opt, NUMERIC.opt)
    ) Fn@{ args ->
        val n = args.toSingleAtomic<XsdNumeric<*>>() ?: return@Fn XdmSequence.EMPTY
        XdmAtomic(n.abs())
    }

    val fnCeiling: BuiltinFunctionImpl<XdmAtomicOrEmpty<XdmNumeric>> = BuiltinFunctionImpl.Fn(
        "ceiling",
        functionType(NUMERIC.opt, NUMERIC.opt)
    ) Fn@{ args ->
        val n = args.toSingleAtomic<XsdNumeric<*>>() ?: return@Fn XdmSequence.EMPTY
        XdmAtomic(n.ceiling())
    }

    val fnFloor: BuiltinFunctionImpl<XdmAtomicOrEmpty<XdmNumeric>> = BuiltinFunctionImpl.Fn(
        "floor",
        functionType(NUMERIC.opt, NUMERIC.opt)
    ) Fn@{ args ->
        val n = args.toSingleAtomic<XsdNumeric<*>>() ?: return@Fn XdmSequence.EMPTY
        XdmAtomic(n.floor())
    }

    val fnRound: BuiltinFunctionImpl<XdmAtomicOrEmpty<XdmNumeric>> = BuiltinFunctionImpl.Fn(
        "round",
        functionType(NUMERIC.opt, NUMERIC.opt)
    ) Fn@{ args ->
        val value = args.atomicOrEmpty<XsdNumeric<*>>(0) ?: return@Fn XdmSequence.EMPTY
        val r = when (args.size) {
            1 -> value.round()
            2 -> value.round(args.atomicOrEmpty<XsdInteger>(1) ?: throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR))
            else -> throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT)
        }
        XdmAtomic(r)
    }

    val fnRoundHalfToEven: BuiltinFunctionImpl<XdmAtomicOrEmpty<XdmNumeric>> = BuiltinFunctionImpl.Fn(
        "round-half-to-even",
        functionType(NUMERIC.opt, NUMERIC.opt)
    ) Fn@{ args ->
        val value = args.atomicOrEmpty<XsdNumeric<*>>(0) ?: return@Fn XdmSequence.EMPTY
        val r = when (args.size) {
            1 -> value.roundToHalfEven()
            2 -> value.roundToHalfEven(
                args.atomicOrEmpty<XsdInteger>(1) ?: throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR)
            )

            else -> throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT)
        }
        XdmAtomic(r)
    }
    //endregion

    //region Parsing numbers
    val fnNumber: BuiltinFunctionImpl<XdmAtomic<XsdNumeric<*>>> = BuiltinFunctionImpl.Fn(
        "number",
        contextFunctionTypes(t(DoubleType.Instance), ATOMIC.opt)
    ) Fn@{ args ->
        val arg = if (args.isEmpty()) contextOf<ExprEvalContext>().contextValue else args.singleArg<XdmValue<*>>()
        if (arg !is XdmAtomic<*>) return@Fn XdmAtomic.NaN

        @Suppress("UNCHECKED_CAST")
        when (val value = arg.value) {
            is XsdDouble -> arg as XdmAtomic<XsdDouble>
            is XsdFloat -> atomic(value.value.toDouble())
            is XsdDecimal -> atomic(value.toDouble())
            else -> atomic(value.xmlString.toDoubleOrNull() ?: Double.NaN)
        }
    }
    //endregion

    //region Formatting integers 4.5
    val fnFormatInteger = BuiltinFunctionImpl.Fn("format-integer", listOf(
        functionType(STRING.opt, INTEGER.opt, STRING),
        functionType(STRING.opt, INTEGER.opt, STRING, STRING.opt),
    )) Fn@{ args ->
        val ctx = contextOf<ExprEvalContext>()
        val value = args.atomicArgOrEmpty<XsdInteger>(0) ?: return@Fn XdmAtomic(XsdString(""))
        val picture = args.atomicArgN<XsdString>(1).xmlString
        val language: XsdLanguage = (if (args.size==2) null else args.atomicArgOrEmpty<XsdString>(2))?.let { XsdLanguage(it.xmlString) }
            ?: ctx.defaultLanguage

        val formatter = try {
            IntegerFormatter(picture, language)
        } catch (e: IllegalArgumentException) {
            throw EvaluationException(ErrorCodes.FODF1310, "Invalid picture for format-integer: '$picture'", e)
        }
        atomic(formatter.format(value))
    }
    //endregion

    //region Formatting numbers 4.7
    val fnFormatNumber = BuiltinFunctionImpl.Fn("format-number", listOf(
        functionType(STRING.opt, NUMERIC.opt, STRING),
        functionType(STRING.opt, NUMERIC.opt, STRING, STRING.opt),
    )) Fn@{ args ->
        val ctx = contextOf<ExprEvalContext>()
        val value = args.atomicArgOrEmpty<XsdNumeric<*>>(0) ?: XsdDouble(Double.NaN)
        val picture = args.atomicArgN<XsdString>(1).xmlString

        val formatName = if (args.size==2) null else args.atomicArgOrEmpty<XsdString>(2)?.let {
            val x = xmlTrimWhitespace(it.xmlString)
            if (x.startsWith("Q{")) {
                val nsEndIdx = x.indexOf('}', 2)
                if (nsEndIdx == -1) throw EvaluationException(ErrorCodes.FODF1280, "Invalid format name: '$x'")
                val ns = x.substring(2, nsEndIdx)
                val localName = x.substring(nsEndIdx+1)
                QName(ns, localName)
            } else if (':' in x) {
                val prefix = x.substringBefore(':')
                val localPart = x.substringAfter(':')
                val ns = ctx.namepaceContext.getNamespaceURI(prefix)
                    ?: throw EvaluationException(ErrorCodes.FODF1280, "Namespace prefix '$prefix' not bound to namespace")

                QName(ns, localPart, prefix)
            } else {
                QName(x)
            }
        }
        val decimalFormat = formatName?.let {
            ctx.resolveDecimalFormat(it)
                ?: throw EvaluationException(ErrorCodes.FODF1280, "Decimal format '$it' not known")
        } ?: ctx.defaultDecimalFormat


        val format = try { NumberFormatter(picture, decimalFormat) } catch (e: IllegalArgumentException) {
            throw EvaluationException(ErrorCodes.FODF1310, "Invalid picture for format-number: '$picture'", e)
        }
        if (! ctx.specVersion.includes(XPathVersion.XPath3_1) && format.hasExponent()) {
            throw EvaluationException(ErrorCodes.FODF1310, "XPath 3.0 and earlier do not support exponents in format-number: '$picture' (format: $formatName)")
        }

        atomic(format.format(value))

    }
    //endregion
}
