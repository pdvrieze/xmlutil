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
import io.github.pdvrieze.formats.xpath.eval.data.*
import io.github.pdvrieze.formats.xpath.functions.Fn
import io.github.pdvrieze.formats.xpath.functions.impl.BooleanFunctions
import io.github.pdvrieze.formats.xpath.impl.*
import io.github.pdvrieze.xml.schematypes.types.*
import io.github.pdvrieze.xml.schematypes.values.*

sealed class Operator(
    override val literal: String,
    val priority: Int,
    override val minVersion: XPathVersion = XPathVersion.XPath3_1,
    override val isDelimiting: Boolean,
): WordToken {
//    @NeedsXPath2
    object COMMA: Operator(",", 1, XPathVersion.XPath2_0, true)

    // FOR|LET|SOME|EVERY|IF -> 2, isDelimiting = false

    @NeedsXPath1
    object OR: Operator("or", 3, XPathVersion.XPath1_0, false)

    @NeedsXPath1
    object AND: Operator("and", 4, XPathVersion.XPath1_0, false)

    @NeedsXPath1
    object EQ: Operator("=", 5, XPathVersion.XPath1_0, true) {
        @OptIn(NeedsXPath3_1::class)
        override val longer: List<Operator> get() = listOf(ARROW)

        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun eval(left: io.github.pdvrieze.formats.xpath.eval.data.XdmValue, right: io.github.pdvrieze.formats.xpath.eval.data.XdmValue): XdmAtomic<XsdBoolean> {
            when {
                left.staticType.isAssignableTo(BooleanType.Instance) -> {
                    return BooleanFunctions.opBooleanEqual(listOf(left, right))
                }
                else -> TODO("Equality operator not yet supported for type ${left.staticType} and ${right.staticType}")
            }
        }
    }
    @NeedsXPath1
    object NEQ: Operator("!=", 5, XPathVersion.XPath1_0, true) {
        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun eval(left: io.github.pdvrieze.formats.xpath.eval.data.XdmValue, right: io.github.pdvrieze.formats.xpath.eval.data.XdmValue): XdmAtomic<XsdBoolean> {
            val eval = (EQ.eval(left, right) as XdmAtomic<*>).value as XsdBoolean
            return XdmAtomic(XsdBoolean(! eval.value))
        }
    }

    @NeedsXPath1
    object LT: Operator("<", 5, XPathVersion.XPath1_0, true) {
        @OptIn(NeedsXPath2::class)
        override val longer: List<Operator> get() = listOf(LE, PRECEDES)
    }
    @NeedsXPath1
    object LE : Operator("<=", 5, XPathVersion.XPath1_0, true)
    @NeedsXPath1
    object GT : Operator(">", 5, XPathVersion.XPath1_0, true){
        @OptIn(NeedsXPath2::class)
        override val longer: List<Operator> get() = listOf(GE, FOLLOWS)
    }
    @NeedsXPath1
    object GE : Operator(">=", 5, XPathVersion.XPath1_0, true)
    @NeedsXPath2
    object VAL_EQ : Operator("eq", 5, XPathVersion.XPath2_0, false), ComparisonImpl {

        override fun defaultCmp(left: XsdAtomic, right: XsdAtomic): Boolean =
            left == right

        override fun cmp(left: Double, right: Double): Boolean = left == right

        override fun cmp(left: Float, right: Float): Boolean = left == right

        override fun cmp(left: Boolean, right: Boolean): Boolean = left == right

        override fun cmp(left: String, right: String): Boolean = left == right

        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun eval(left: io.github.pdvrieze.formats.xpath.eval.data.XdmValue, right: io.github.pdvrieze.formats.xpath.eval.data.XdmValue): io.github.pdvrieze.formats.xpath.eval.data.XdmValue {
            val leftVal = when (val a = left.atomize()) {
                is XdmSequence.Empty -> return XdmSequence.EMPTY
                is XdmAtomic<*> -> a.value
                is XdmSequence<*> -> throw EvaluationException(
                    ErrorCodes.XPTY0004_TYPE_ERROR,
                    "Sequence as value comparison operand"
                )
            }
            val rightVal = when (val a = right.atomize()) {
                is XdmSequence.Empty -> return XdmSequence.EMPTY
                is XdmAtomic<*> -> a.value
                is XdmSequence<*> -> throw EvaluationException(
                    ErrorCodes.XPTY0004_TYPE_ERROR,
                    "Sequence as value comparison operand"
                )
            }

            val result: Boolean = when (leftVal) {
                is XsdFloat if rightVal is XsdFloat -> leftVal.value == rightVal.value
                is XsdDouble if rightVal is XsdDouble -> leftVal.value == rightVal.value
                is XsdDecimal if rightVal is XsdDecimal -> leftVal == rightVal
                is XsdNumeric<*> if rightVal is XsdNumeric<*> -> leftVal.toDouble() == rightVal.toDouble()

                is XsdBoolean if rightVal is XsdBoolean -> leftVal.value == rightVal.value


                else -> TODO()
            }
            return XdmAtomic(XsdBoolean(result))
        }

    }
    @NeedsXPath2
    object VAL_NEQ: Operator("ne", 5, XPathVersion.XPath2_0, false)
    @NeedsXPath2
    object VAL_LT: Operator("lt", 5, XPathVersion.XPath2_0, false)
    @NeedsXPath2
    object VAL_LE: Operator("le", 5, XPathVersion.XPath2_0, false)
    @NeedsXPath2
    object VAL_GT: Operator("gt", 5, XPathVersion.XPath2_0, false)
    @NeedsXPath2
    object VAL_GE: Operator("ge", 5, XPathVersion.XPath2_0, false)
    @NeedsXPath2
    object PRECEDES: Operator("<<", 5, XPathVersion.XPath2_0, true)
    @NeedsXPath2
    object FOLLOWS: Operator(">>", 5, XPathVersion.XPath2_0, true)
    @NeedsXPath2
    object IS: Operator("is", 5, XPathVersion.XPath2_0, false)

    @NeedsXPath3_0
    object CONCAT: Operator("||", 6, XPathVersion.XPath3_0, true)
    @NeedsXPath2
    object TO: Operator("to", 7, XPathVersion.XPath2_0, false)

    @NeedsXPath1
    object ADD: Operator("+", 8, XPathVersion.XPath1_0, true)

    @NeedsXPath1
    object SUB: Operator("-", 8, XPathVersion.XPath1_0, true)

    @NeedsXPath1
    object MUL: Operator("*", 9, XPathVersion.XPath1_0, true)
    @NeedsXPath1
    object DIV: Operator("div", 9, XPathVersion.XPath1_0, false)
    @NeedsXPath2
    object IDIV: Operator("idiv", 9, XPathVersion.XPath2_0, false)
    @NeedsXPath1
    object MOD: Operator("mod", 9, XPathVersion.XPath1_0, false)

    @NeedsXPath1
    object UNION: Operator("union", 10, XPathVersion.XPath1_0, false)
    @NeedsXPath1
    object PIPEUNION: Operator("|", 10, XPathVersion.XPath1_0, true){
        @OptIn(NeedsXPath3_0::class)
        override val longer: List<Operator> = listOf(CONCAT)
    }

    @NeedsXPath2
    object INTERSECT: Operator("intersect", 11, XPathVersion.XPath2_0, false)
    @NeedsXPath2
    object EXCEPT: Operator("except", 11, XPathVersion.XPath2_0, false)

    // INSTANC_EOF -> 12, isDelimiting = false
    // TREAT_AS -> 13, isDelimiting = false
    // CASTABLE_AS -> 14, isDelimiting = false
    // CAST_AS -> 15, isDelimiting = false

    @NeedsXPath3_1 object ARROW: Operator("=>", 16, XPathVersion.XPath3_1, true)

    @NeedsXPath1 object UNARY_MINUS: Operator("-", 17, XPathVersion.XPath1_0, true)
    @NeedsXPath2 object UNARY_PLUS: Operator("+", 17, XPathVersion.XPath2_0, true)

    @NeedsXPath3_0 object MAP: Operator("!", 18, XPathVersion.XPath3_0, true) {
        @OptIn(NeedsXPath3_1::class)
        override val longer: List<Operator> get() = listOf(NEQ)
    }


    // '/', '//' (path separators) -> 19, isDelimiting = true
    // '[', '?' (binary lookup) -> 20, isDelimiting = true
    // '?' (unary lookup) -> 21, isDelimiting = true
    ;

    open val longer: List<Operator> get() = emptyList()

    context(ctx: ExprEvalContext)
    @XPathInternal
    open fun eval(left: io.github.pdvrieze.formats.xpath.eval.data.XdmValue, right: io.github.pdvrieze.formats.xpath.eval.data.XdmValue): io.github.pdvrieze.formats.xpath.eval.data.XdmValue =
        TODO("Evaluation of operator '$literal' not yet implemented")

    context(ctx: ExprEvalContext)
    @XPathInternal
    fun eval(param: io.github.pdvrieze.formats.xpath.eval.data.XdmValue): io.github.pdvrieze.formats.xpath.eval.data.XdmValue = eval(listOf(param))

    @XPathInternal
    context(ctx: ExprEvalContext)
    fun eval(params: List<io.github.pdvrieze.formats.xpath.eval.data.XdmValue>): io.github.pdvrieze.formats.xpath.eval.data.XdmValue =
        params.reduce { acc, param -> eval(acc, param) }

    @XPathInternal
    context(ctx: ExprEvalContext)
    private fun evalComparison(
        left: io.github.pdvrieze.formats.xpath.eval.data.XdmValue,
        right: io.github.pdvrieze.formats.xpath.eval.data.XdmValue,
        operator : ComparisonImpl,
    ): XdmAtomicOrEmpty {
        val leftVal = when (val a = left.atomize()) {
            is XdmSequence.Empty -> return XdmSequence.EMPTY
            is XdmAtomic<*> if (a.staticType is UntypedAtomicType) -> XsdString(a.value.xmlString)
            is XdmAtomic<*> -> a.value.let { if (it is XsdAnyURI) XsdString(it.xmlString) else it }
            is XdmSequence<*> -> throw EvaluationException(
                ErrorCodes.XPTY0004_TYPE_ERROR,
                "Sequence as value comparison operand"
            )
        }
        val rightVal = when (val a = right.atomize()) {
            is XdmSequence.Empty -> return XdmSequence.EMPTY
            is XdmAtomic<*> if (a.staticType is UntypedAtomicType) -> XsdString(a.value.xmlString)
            is XdmAtomic<*> -> a.value.let { if (it is XsdAnyURI) XsdString(it.xmlString) else it }
            is XdmSequence<*> -> throw EvaluationException(
                ErrorCodes.XPTY0004_TYPE_ERROR,
                "Sequence as value comparison operand"
            )
        }

        val result: Boolean = when (leftVal) {
            is XsdFloat if rightVal is XsdFloat -> operator.cmp(leftVal.value, rightVal.value)
            is XsdDouble if rightVal is XsdDouble -> operator.cmp(leftVal.value, rightVal.value)
            is XsdDecimal if rightVal is XsdDecimal -> operator.cmp(leftVal, rightVal)
            is XsdNumeric<*> if rightVal is XsdNumeric<*> -> operator.cmp(leftVal.toDouble(), rightVal.toDouble())
            is XsdBoolean if rightVal is XsdBoolean -> operator.cmp(leftVal.value, rightVal.value)

            is XsdString if rightVal is XsdString -> operator.cmp(leftVal.xmlString, rightVal.xmlString)
            is XsdDateTime if rightVal is XsdDateTime -> operator.cmp(leftVal, rightVal)
            is XsdDate if rightVal is XsdDate -> operator.cmp(leftVal, rightVal)
            is XsdDuration if rightVal is XsdDuration -> operator.cmp(leftVal, rightVal)
            is XsdGDay if rightVal is XsdGDay -> operator.cmp(leftVal, rightVal)

            is XsdGMonthDay if rightVal is XsdGMonthDay -> operator.cmp(leftVal, rightVal)
            is XsdGMonth if rightVal is XsdGMonth -> operator.cmp(leftVal, rightVal)
            is XsdGYearMonth if rightVal is XsdGYearMonth -> operator.cmp(leftVal, rightVal)
            is XsdGYear if rightVal is XsdGYear -> operator.cmp(leftVal, rightVal)
            is XsdHexBinary if rightVal is XsdHexBinary -> operator.cmp(leftVal, rightVal)

            is XsdNotation if rightVal is XsdNotation -> operator.cmp(leftVal, rightVal)
            is XsdQName if rightVal is XsdQName -> operator.cmp(leftVal, rightVal)
            is XsdTime if rightVal is XsdTime -> operator.cmp(leftVal, rightVal)

            else -> throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Type mismatch")
        }
        return XdmAtomic(XsdBoolean(result))
    }


}

internal interface ComparisonImpl {
    context(ctx: ExprEvalContext)
    @XPathInternal
    fun eval(left: io.github.pdvrieze.formats.xpath.eval.data.XdmValue, right: io.github.pdvrieze.formats.xpath.eval.data.XdmValue): io.github.pdvrieze.formats.xpath.eval.data.XdmValue {
        val leftVal = when (val a = left.atomize()) {
            is XdmSequence.Empty -> return XdmSequence.EMPTY
            is XdmAtomic<*> if (a.staticType is UntypedAtomicType) -> XsdString(a.value.xmlString)
            is XdmAtomic<*> -> a.value.let { if (it is XsdAnyURI) XsdString(it.xmlString) else it }
            is XdmSequence<*> -> throw EvaluationException(
                ErrorCodes.XPTY0004_TYPE_ERROR,
                "Sequence as value comparison operand"
            )
        }
        val rightVal = when (val a = right.atomize()) {
            is XdmSequence.Empty -> return XdmSequence.EMPTY
            is XdmAtomic<*> if (a.staticType is UntypedAtomicType) -> XsdString(a.value.xmlString)
            is XdmAtomic<*> -> a.value.let { if (it is XsdAnyURI) XsdString(it.xmlString) else it }
            is XdmSequence<*> -> throw EvaluationException(
                ErrorCodes.XPTY0004_TYPE_ERROR,
                "Sequence as value comparison operand"
            )
        }

        val result: Boolean = when (leftVal) {
            is XsdFloat if rightVal is XsdFloat -> cmp(leftVal.value, rightVal.value)
            is XsdDouble if rightVal is XsdDouble -> cmp(leftVal.value, rightVal.value)
            is XsdDecimal if rightVal is XsdDecimal -> cmp(leftVal, rightVal)
            is XsdNumeric<*> if rightVal is XsdNumeric<*> -> cmp(leftVal.toDouble(), rightVal.toDouble())
            is XsdBoolean if rightVal is XsdBoolean -> cmp(leftVal.value, rightVal.value)

            is XsdString if rightVal is XsdString -> cmp(leftVal.xmlString, rightVal.xmlString)
            is XsdDateTime if rightVal is XsdDateTime -> cmp(leftVal, rightVal)
            is XsdDate if rightVal is XsdDate -> cmp(leftVal, rightVal)
            is XsdDuration if rightVal is XsdDuration -> cmp(leftVal, rightVal)
            is XsdGDay if rightVal is XsdGDay -> cmp(leftVal, rightVal)

            is XsdGMonthDay if rightVal is XsdGMonthDay -> cmp(leftVal, rightVal)
            is XsdGMonth if rightVal is XsdGMonth -> cmp(leftVal, rightVal)
            is XsdGYearMonth if rightVal is XsdGYearMonth -> cmp(leftVal, rightVal)
            is XsdGYear if rightVal is XsdGYear -> cmp(leftVal, rightVal)
            is XsdHexBinary if rightVal is XsdHexBinary -> cmp(leftVal, rightVal)

            is XsdNotation if rightVal is XsdNotation -> cmp(leftVal, rightVal)
            is XsdQName if rightVal is XsdQName -> cmp(leftVal, rightVal)
            is XsdTime if rightVal is XsdTime -> cmp(leftVal, rightVal)

            else -> throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Type mismatch")
        }
        return XdmAtomic(XsdBoolean(result))
    }

    fun defaultCmp(left: XsdAtomic, right: XsdAtomic): Boolean
    fun cmp(left: XsdDecimal, right: XsdDecimal): Boolean = defaultCmp(left, right)
    fun cmp(left: Double, right: Double): Boolean
    fun cmp(left: Float, right: Float): Boolean
    fun cmp(left: Boolean, right: Boolean): Boolean
    fun cmp(left: String, right: String): Boolean
    fun cmp(left: XsdDateTime, right: XsdDateTime): Boolean = defaultCmp(left, right)
    fun cmp(left: XsdDate, right: XsdDate): Boolean = defaultCmp(left, right)
    fun cmp(left: XsdDuration, right: XsdDuration): Boolean = defaultCmp(left, right)
    fun cmp(left: XsdGDay, right: XsdGDay): Boolean = defaultCmp(left, right)
    fun cmp(left: XsdGMonthDay, right: XsdGMonthDay): Boolean = defaultCmp(left, right)
    fun cmp(left: XsdGMonth, right: XsdGMonth): Boolean = defaultCmp(left, right)
    fun cmp(left: XsdGYearMonth, right: XsdGYearMonth): Boolean = defaultCmp(left, right)
    fun cmp(left: XsdGYear, right: XsdGYear): Boolean = defaultCmp(left, right)
    fun cmp(left: XsdHexBinary, right: XsdHexBinary): Boolean = defaultCmp(left, right)
    fun cmp(left: XsdNotation, right: XsdNotation): Boolean = defaultCmp(left, right)
    fun cmp(left: XsdQName, right: XsdQName): Boolean = defaultCmp(left, right)
    fun cmp(left: XsdTime, right: XsdTime): Boolean = defaultCmp(left, right)
}

interface ArithmeticOperator {
    @XPathInternal
    context(ctx: ExprEvalContext)
    fun normalizeToArithmetic(value: io.github.pdvrieze.formats.xpath.eval.data.XdmValue): io.github.pdvrieze.formats.xpath.eval.data.XdmValue {
        val v1 = value.atomize()
        // Handle sequences
        val v2: XdmSingleValue<*> = when (v1.size) {
            0 -> return if (ctx.isXPath1Compat) XdmAtomic.NaN else XdmSequence.EMPTY
            1 -> v1[0]
            else if ctx.isXPath1Compat -> v1[0]
            else -> throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Sequence as arithmatic operand")
        }
        if (v2 !is XdmAtomic<*>) throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Value type ${value.staticType} is not compatible with an arithmetic operator")
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
    fun eval(left: io.github.pdvrieze.formats.xpath.eval.data.XdmValue, right: io.github.pdvrieze.formats.xpath.eval.data.XdmValue): io.github.pdvrieze.formats.xpath.eval.data.XdmValue {
        val l = when (val n = normalizeToArithmetic(left)) {
            is XdmAtomic<*> -> n
            XdmSequence.EMPTY -> return XdmSequence.EMPTY
            else -> throw EvaluationException(ctx.expr, "Implementation in number normalization")
        }
        val r = when (val n = normalizeToArithmetic(right)) {
            is XdmAtomic<*> -> n
            XdmSequence.EMPTY -> return XdmSequence.EMPTY
            else -> throw EvaluationException(ctx.expr, "Implementation in number normalization")
        }
        val requiredType = operatorMapping(l.value.schemaType, r.value.schemaType)
        return when (requiredType) {
            is DoubleType<*> -> evalDouble(l.toXdmDouble().value.value, r.toXdmDouble().value.value)
            is FloatType<*> -> evalFloat(l.toXdmFloat().value.value, r.toXdmFloat().value.value)
            is IntegerType<*> -> evalInteger(l.toXdmInteger().value, r.toXdmInteger().value)
            is DecimalType<*> -> evalDecimal(l.toXdmDecimal().value, r.toXdmDecimal().value)
            else -> throw EvaluationException(ctx.expr, "Implementation in number normalization")
        }
    }

    @XPathInternal
    context(ctx: ExprEvalContext)
    fun operatorMapping(leftType: AnyAtomicType<*>, rightType: AnyAtomicType<*>): AnyAtomicType<*>

    @XPathInternal
    context(ctx: ExprEvalContext)
    fun evalFloat(left: Float, right: Float): io.github.pdvrieze.formats.xpath.eval.data.XdmValue

    @XPathInternal
    context(ctx: ExprEvalContext)
    fun evalDouble(left: Double, right: Double): io.github.pdvrieze.formats.xpath.eval.data.XdmValue

    @XPathInternal
    context(ctx: ExprEvalContext)
    fun evalInteger(left: XsdInteger, right: XsdInteger): io.github.pdvrieze.formats.xpath.eval.data.XdmValue

    @XPathInternal
    context(ctx: ExprEvalContext)
    fun evalDecimal(left: XsdDecimal, right: XsdDecimal): XdmValue

//    fun evalNormalized(left: XdmAtomic<*>, right: XdmValue<*>)
}
