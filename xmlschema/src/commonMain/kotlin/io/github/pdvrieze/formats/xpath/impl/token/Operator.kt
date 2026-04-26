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

package io.github.pdvrieze.formats.xpath.impl.token

import io.github.pdvrieze.formats.xpath.XPathVersion
import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.data.*
import io.github.pdvrieze.formats.xpath.functions.impl.BooleanFunctions
import io.github.pdvrieze.formats.xpath.functions.impl.NumericFunctions
import io.github.pdvrieze.formats.xpath.functions.impl.StringFunctions
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
    object OR: LogicOperator("or", 3, XPathVersion.XPath1_0, false) {
        override fun invoke(left: Boolean, right: Boolean): Boolean {
            return left || right
        }
    }

    @NeedsXPath1
    object AND: LogicOperator("and", 4, XPathVersion.XPath1_0, false) {
        override fun invoke(left: Boolean, right: Boolean): Boolean {
            return left && right
        }
    }

    @NeedsXPath1
    object EQ: Operator("=", 5, XPathVersion.XPath1_0, true) {
        @OptIn(NeedsXPath3_1::class)
        override val longer: List<Operator> get() = listOf(ARROW)

        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun eval(left: XdmValue<*>, right: XdmValue<*>): XdmAtomic<XsdBoolean> {
            return when {
                left.staticType.isAssignableTo(BooleanType.Instance) ->
                    BooleanFunctions.opBooleanEqual(listOf(left, right))

                else -> XdmBoolean(XsdBoolean(left.isValEqual(right)))
            }
        }
    }
    @NeedsXPath1
    object NEQ: Operator("!=", 5, XPathVersion.XPath1_0, true) {
        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun eval(left: XdmValue<*>, right: XdmValue<*>): XdmAtomic<XsdBoolean> {
            val eval = (EQ.eval(left, right) as XdmAtomic<*>).value as XsdBoolean
            return XdmAtomic((! eval.value))
        }
    }

    @NeedsXPath1
    object LT: ComparisonOperator("<", 5, XPathVersion.XPath1_0, true) {
        @OptIn(NeedsXPath2::class)
        override val longer: List<Operator> get() = listOf(LE, PRECEDES)

        context(ctx: ExprEvalContext)
        override fun numericCompare(cmp: Int): Boolean = cmp < 0
    }

    @NeedsXPath1
    object LE : ComparisonOperator("<=", 5, XPathVersion.XPath1_0, true) {
        context(ctx: ExprEvalContext)
        override fun numericCompare(cmp: Int): Boolean = cmp <= 0
    }
    @NeedsXPath1
    object GT : ComparisonOperator(">", 5, XPathVersion.XPath1_0, true){
        @OptIn(NeedsXPath2::class)
        override val longer: List<Operator> get() = listOf(GE, FOLLOWS)

        context(ctx: ExprEvalContext)
        override fun numericCompare(cmp: Int): Boolean = cmp > 0
    }
    @NeedsXPath1
    object GE : ComparisonOperator(">=", 5, XPathVersion.XPath1_0, true) {

        context(ctx: ExprEvalContext)
        override fun numericCompare(cmp: Int): Boolean = cmp >= 0
    }

    @NeedsXPath2
    object VAL_EQ : ComparisonOperator("eq", 5, XPathVersion.XPath2_0, false) {

        context(ctx: ExprEvalContext)
        override fun numericCompare(cmp: Int): Boolean = cmp == 0

        context(ctx: ExprEvalContext)
        override fun cmpAtomic(leftVal: XsdAtomic, rightVal: XsdAtomic): Boolean {
            return leftVal.equals(rightVal)
        }



/*

        context(ctx: ExprEvalContext)
        override fun cmpNumbers(left: XsdNumeric<*>, right: XsdNumeric<*>): Boolean {
            return left == right
        }

        context(ctx: ExprEvalContext)
        override fun cmp(left: Double, right: Double): Boolean = left == right

        context(ctx: ExprEvalContext)
        override fun cmp(left: Float, right: Float): Boolean = left == right

        context(ctx: ExprEvalContext)
        override fun cmp(left: Boolean, right: Boolean): Boolean = left == right

        context(ctx: ExprEvalContext)
        override fun cmp(left: String, right: String): Boolean = left == right

        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun eval(left: XdmValue<*>, right: XdmValue<*>): XdmAtomicOrEmpty<XdmBoolean> {
            val leftVal = when (val a = left.atomize()) {
                is XdmSequence.EMPTY -> return a
                is XdmAtomic<*> -> a.value
                is XdmSequence<*> -> throw EvaluationException(
                    ErrorCodes.XPTY0004_TYPE_ERROR,
                    "Sequence as value comparison operand"
                )
            }
            val rightVal = when (val a = right.atomize()) {
                is XdmSequence.EMPTY -> return XdmSequence.EMPTY
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

                else -> return XdmAtomic((leftVal.equals(rightVal)))
            }
            return XdmAtomic((result))
        }
*/

    }
    @NeedsXPath2
    object VAL_NEQ: Operator("ne", 5, XPathVersion.XPath2_0, false) {
        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun eval(left: XdmValue<*>, right: XdmValue<*>): XdmValue<*> {
            val isEq = (VAL_EQ.eval(left, right) as? XdmAtomic<XsdBoolean>)?.value ?: return XdmSequence.EMPTY
            return XdmBoolean(XsdBoolean(!isEq.value))
        }
    }
    @NeedsXPath2
    object VAL_LT: ComparisonOperator("lt", 5, XPathVersion.XPath2_0, false) {
                @OptIn(NeedsXPath2::class)
        override val longer: List<Operator> get() = listOf(LE, PRECEDES)

        context(ctx: ExprEvalContext)
        override fun numericCompare(cmp: Int): Boolean = cmp < 0

    }
    @NeedsXPath2
    object VAL_LE: ComparisonOperator("le", 5, XPathVersion.XPath2_0, false) {
                @OptIn(NeedsXPath2::class)
        override val longer: List<Operator> get() = listOf(LE, PRECEDES)

        context(ctx: ExprEvalContext)
        override fun numericCompare(cmp: Int): Boolean = cmp <= 0

    }
    @NeedsXPath2
    object VAL_GT: ComparisonOperator("gt", 5, XPathVersion.XPath2_0, false) {
                @OptIn(NeedsXPath2::class)
        override val longer: List<Operator> get() = listOf(LE, PRECEDES)

        context(ctx: ExprEvalContext)
        override fun numericCompare(cmp: Int): Boolean = cmp > 0

    }
    @NeedsXPath2
    object VAL_GE: ComparisonOperator("ge", 5, XPathVersion.XPath2_0, false) {
                @OptIn(NeedsXPath2::class)
        override val longer: List<Operator> get() = listOf(LE, PRECEDES)

        context(ctx: ExprEvalContext)
        override fun numericCompare(cmp: Int): Boolean = cmp >= 0
    }
    @NeedsXPath2
    object PRECEDES: Operator("<<", 5, XPathVersion.XPath2_0, true)
    @NeedsXPath2
    object FOLLOWS: Operator(">>", 5, XPathVersion.XPath2_0, true)
    @NeedsXPath2
    object IS: Operator("is", 5, XPathVersion.XPath2_0, false)

    @NeedsXPath3_0
    object CONCAT: Operator("||", 6, XPathVersion.XPath3_0, true) {
        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun eval(left: XdmValue<*>, right: XdmValue<*>): XdmValue<*> {
            // defined as equivalent to the function call
            return StringFunctions.fnConcat(left, right)
        }
    }
    @NeedsXPath2
    object TO: Operator("to", 7, XPathVersion.XPath2_0, false)

    @NeedsXPath1
    object ADD: ArithmeticOperator("+", 8, XPathVersion.XPath1_0, true) {
        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun disjointOperatorMapping(leftType: AnyAtomicType<*>, rightType: AnyAtomicType<*>): AnyAtomicType<*> {
            return when (leftType) {
                is DateType<*> if (rightType is DurationType<*>) -> DateType.Instance
                is DurationType<*> if (rightType is DateType<*>) -> DateType.Instance
                is TimeType<*> if (rightType is DayTimeDurationType<*>) -> TimeType.Instance
                is DayTimeDurationType<*> if (rightType is TimeType<*>) -> TimeType.Instance
                is DateTimeType<*> if (rightType is DurationType<*>) -> DateTimeType.Instance
                is DurationType<*> if (rightType is DateTimeType<*>) -> DateTimeType.Instance

                else -> super.disjointOperatorMapping(leftType, rightType)
            }
        }

        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun evalFloat(left: Float, right: Float): Float = left + right

        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun evalDouble(left: Double, right: Double): Double = left + right

        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun evalInteger(left: XsdInteger, right: XsdInteger): XsdInteger =
            left + right

        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun evalDecimal(left: XsdDecimal, right: XsdDecimal): XsdDecimal = left + right

        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun evalCustom(left: XsdAtomic, right: XsdAtomic): XsdAtomic = when {
            left is XsdTime -> left + (right as XsdDayTimeDuration)
            left is XsdDate -> left + (right as XsdDuration)
            left is XsdDateTime -> left + (right as XsdDuration)
            right is XsdTime -> right + (left as XsdDayTimeDuration)
            right is XsdDate -> right + (left as XsdDuration)
            right is XsdDateTime -> right + (left as XsdDuration)
            else -> throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Unsupported parameters: ${left.schemaType} + ${right.schemaType}")
        }
    }

    @NeedsXPath1
    object SUB: ArithmeticOperator("-", 8, XPathVersion.XPath1_0, true) {
        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun disjointOperatorMapping(leftType: AnyAtomicType<*>, rightType: AnyAtomicType<*>): AnyAtomicType<*> {
            return when (leftType) {
                is DateType<*> if (rightType is DurationType<*>) -> DateType.Instance
                is DurationType<*> if (rightType is DateType<*>) -> DateType.Instance
                is TimeType<*> if (rightType is DayTimeDurationType<*>) -> TimeType.Instance
                is DayTimeDurationType<*> if (rightType is TimeType<*>) -> TimeType.Instance
                is DateTimeType<*> if (rightType is DurationType<*>) -> DateTimeType.Instance
                is DurationType<*> if (rightType is DateTimeType<*>) -> DateTimeType.Instance

                else -> super.disjointOperatorMapping(leftType, rightType)
            }
        }

        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun evalFloat(left: Float, right: Float): Float = left - right

        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun evalDouble(left: Double, right: Double): Double = left - right

        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun evalInteger(left: XsdInteger, right: XsdInteger): XsdInteger =
            left - right

        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun evalDecimal(left: XsdDecimal, right: XsdDecimal): XsdDecimal = left - right

        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun evalCustom(left: XsdAtomic, right: XsdAtomic): XsdAtomic {
            when {
                left is XsdTime -> when (right) {
                    is XsdDayTimeDuration -> return left - right
                    is XsdTime -> return left - right
                }

                left is XsdDate -> when (right){
                    is XsdDate -> return left - right
                    is XsdDuration -> return left - right

                }

                left is XsdDateTime -> when (right) {
                    is XsdDateTime -> return left - right
                    is XsdDuration -> return left - right
                }

                left is XsdDayTimeDuration && right is XsdDayTimeDuration -> return left - right

                left is XsdYearMonthDuration && right is XsdYearMonthDuration -> return left - right
            }
            throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Unsupported parameters: ${left.schemaType.name} - ${right.schemaType.name}")
        }
    }

    @NeedsXPath1
    object MUL: ArithmeticOperator("*", 9, XPathVersion.XPath1_0, true) {
        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun disjointOperatorMapping(
            leftType: AnyAtomicType<*>,
            rightType: AnyAtomicType<*>
        ): AnyAtomicType<*> {
            // note that we need to check due to subtypes
            return when (leftType) {
                is YearMonthDurationType if rightType is NumericType<*> -> YearMonthDurationType.Instance
                is NumericType<*> if rightType is YearMonthDurationType -> YearMonthDurationType.Instance
                is DayTimeDurationType if rightType is NumericType<*> -> DayTimeDurationType.Instance
                is NumericType<*> if rightType is DayTimeDurationType -> DayTimeDurationType.Instance

                else -> super.disjointOperatorMapping(leftType, rightType)
            }
        }

        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun evalFloat(left: Float, right: Float): Float = left * right

        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun evalDouble(left: Double, right: Double): Double = left * right

        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun evalInteger(left: XsdInteger, right: XsdInteger): XsdInteger =
            left * right

        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun evalDecimal(left: XsdDecimal, right: XsdDecimal): XsdDecimal = left * right

        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun evalCustom(
            left: XsdAtomic,
            right: XsdAtomic
        ): XsdAtomic = when {
            left is XsdDayTimeDuration && right is XsdNumeric<*> -> left * right
            left is XsdNumeric<*> && right is XsdDayTimeDuration -> right * left
            left is XsdYearMonthDuration && right is XsdNumeric<*> -> left * right
            left is XsdNumeric<*> && right is XsdYearMonthDuration -> right * left

            else -> super.evalCustom(left, right)
        }
    }

    @NeedsXPath1
    object DIV: ArithmeticOperator("div", 9, XPathVersion.XPath1_0, false) {
        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun operatorMapping(lType: AnyAtomicType<XsdAtomic>, rType: AnyAtomicType<XsdAtomic>): AnyAtomicType<XsdAtomic> {
            //override integer returns
            return when (val t = super.operatorMapping(lType, rType)) {
                is IntegerType<*> -> return DecimalType.Instance
                else -> t
            }
        }

        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun disjointOperatorMapping(
            leftType: AnyAtomicType<*>,
            rightType: AnyAtomicType<*>
        ): AnyAtomicType<*> {
            if (leftType == rightType) return leftType
            if (ctx.isXPath1Compat) return DoubleType.Instance
            // note that we need to check due to subtypes
            return when (leftType) {
                is YearMonthDurationType if rightType is NumericType<*> -> YearMonthDurationType.Instance
                is YearMonthDurationType if rightType is YearMonthDurationType -> DecimalType.Instance
                is DayTimeDurationType if rightType is NumericType<*> -> DayTimeDurationType.Instance
                is DayTimeDurationType if rightType is DayTimeDurationType -> DecimalType.Instance
                is IntegerType if (rightType is IntegerType) -> DecimalType.Instance
                else -> super.disjointOperatorMapping(leftType, rightType)
            }
        }


        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun evalFloat(left: Float, right: Float): Float {
            return left/right
        }

        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun evalDouble(left: Double, right: Double): Double {
            return left/right
        }

        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun evalDecimal(
            left: XsdDecimal,
            right: XsdDecimal
        ): XsdDecimal {
            return left.toBigDecimal() / right.toBigDecimal()
        }
    }
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

    open val longer: List<Operator> get() = emptyList()

    @XPathInternal
    context(ctx: ExprEvalContext)
    open fun eval(left: XdmValue<*>, right: XdmValue<*>): XdmValue<*> =
        TODO("Evaluation of operator '$literal' not yet implemented")

    context(ctx: ExprEvalContext)
    @XPathInternal
    fun eval(param: XdmValue<*>): XdmValue<*> = eval(listOf(param))

    @XPathInternal
    context(ctx: ExprEvalContext)
    fun eval(params: List<XdmValue<*>>): XdmValue<*> =
        params.reduce { acc, param -> eval(acc, param) }

    @XPathInternal
    context(ctx: ExprEvalContext)
    private fun evalComparison(
        left: XdmValue<*>,
        right: XdmValue<*>,
        operator : ComparisonOperator,
    ): XdmAtomicOrEmpty<XdmBoolean> {
        val leftVal = when (val a = left.atomize()) {
            is XdmSequence.EMPTY -> return XdmSequence.EMPTY
            is XdmAtomic<*> if (a.staticType.schemaType is UntypedAtomicType) -> XsdString(a.value.xmlString)
            is XdmAtomic<*> -> a.value.let { if (it is XsdAnyURI) XsdString(it.xmlString) else it }
            is XdmSequence<*> -> throw EvaluationException(
                ErrorCodes.XPTY0004_TYPE_ERROR,
                "Sequence as value comparison operand"
            )
        }
        val rightVal = when (val a = right.atomize()) {
            is XdmSequence.EMPTY -> return XdmSequence.EMPTY
            is XdmAtomic<*> if (a.staticType.schemaType is UntypedAtomicType) -> XsdString(a.value.xmlString)
            is XdmAtomic<*> -> a.value.let { if (it is XsdAnyURI) XsdString(it.xmlString) else it }
            is XdmSequence<*> -> throw EvaluationException(
                ErrorCodes.XPTY0004_TYPE_ERROR,
                "Sequence as value comparison operand"
            )
        }

        val result: Boolean = when (leftVal) {
            is XsdFloat if rightVal is XsdFloat -> operator.cmp(leftVal.value, rightVal.value)
            is XsdDouble if rightVal is XsdDouble -> operator.cmp(leftVal.value, rightVal.value,)
            is XsdDecimal if rightVal is XsdDecimal -> operator.cmp(leftVal, rightVal)
            is XsdNumeric<*> if rightVal is XsdNumeric<*> -> operator.cmp(leftVal.toDouble(), rightVal.toDouble(),)
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
        return XdmAtomic((result))
    }


}

abstract class SequenceComparisonOperator(
    private val base: Operator,
    literal: String,
    priority: Int,
    minVersion: XPathVersion = XPathVersion.XPath3_1,
    isDelimiting: Boolean,
    val isInequality: Boolean = false,
) : Operator(literal, priority, minVersion, isDelimiting) {
    context(ctx: ExprEvalContext)
    @XPathInternal
    override fun eval(left: XdmValue<*>, right: XdmValue<*>): XdmValue<*> {
        if (left is XdmAtomic<*> && left.value is XsdBoolean) {
            val rightBool = XsdBoolean(right.toBoolean())
            return base.eval(left, XdmAtomic(rightBool))
        } else if (right is XdmAtomic<*> && right.value is XsdBoolean) {
            val leftBool = XsdBoolean(left.toBoolean())
            return base.eval(XdmAtomic(leftBool), right)
        }

        var leftAtoms: Collection<XdmAtomic<XsdAtomic>> = left.atomize()
        var rightAtoms: Collection<XdmAtomic<XsdAtomic>> = right.atomize()

        if (isInequality) {
            leftAtoms = leftAtoms.map { NumericFunctions.fnNumber(it) }
            rightAtoms = rightAtoms.map { NumericFunctions.fnNumber(it) }
        }


        if (leftAtoms.isEmpty() || rightAtoms.isEmpty()) return XdmSequence.EMPTY

        if (left !is XdmAtomic<*> || right !is XdmAtomic<*>) {
            throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Sequence as value comparison operand")
        }

        return base.eval(left, right)
    }
}

abstract class ComparisonOperator(
    literal: String,
    priority: Int,
    minVersion: XPathVersion = XPathVersion.XPath3_1,
    isDelimiting: Boolean,
) : Operator(literal, priority, minVersion, isDelimiting) {
    context(ctx: ExprEvalContext)
    @XPathInternal
    override fun eval(left: XdmValue<*>, right: XdmValue<*>): XdmAtomicOrEmpty<XdmBoolean> {
        val leftVal = when (val a = left.atomize()) {
            is XdmSequence.EMPTY -> return XdmSequence.EMPTY
            is XdmAtomic<*> if (a.staticType.schemaType is UntypedAtomicType) -> XsdString(a.value.xmlString)
            is XdmAtomic<*> -> a.value.let { if (it is XsdAnyURI) XsdString(it.xmlString) else it }
            is XdmSequence<*> -> throw EvaluationException(
                ErrorCodes.XPTY0004_TYPE_ERROR,
                "Sequence as value comparison operand"
            )
        }
        val rightVal = when (val a = right.atomize()) {
            is XdmSequence.EMPTY -> return XdmSequence.EMPTY
            is XdmAtomic<*> if (a.staticType.schemaType is UntypedAtomicType) -> XsdString(a.value.xmlString)
            is XdmAtomic<*> -> a.value.let { if (it is XsdAnyURI) XsdString(it.xmlString) else it }
            is XdmSequence<*> -> throw EvaluationException(
                ErrorCodes.XPTY0004_TYPE_ERROR,
                "Sequence as value comparison operand"
            )
        }

        return XdmAtomic((cmpAtomic(leftVal, rightVal)))
    }

    context(ctx: ExprEvalContext)
    open protected fun cmpAtomic(leftVal: XsdAtomic, rightVal: XsdAtomic): Boolean {
        val result: Boolean = when (leftVal) {
            is XsdFloat if rightVal is XsdFloat -> cmp(leftVal.value, rightVal.value)
            is XsdDouble if rightVal is XsdDouble -> cmp(leftVal.value, rightVal.value)
            is XsdDecimal if rightVal is XsdDecimal -> cmp(leftVal, rightVal)
            is XsdNumeric<*> if rightVal is XsdNumeric<*> -> numericCompare(numericCompare(leftVal, rightVal))
            is XsdBoolean if rightVal is XsdBoolean -> cmp(leftVal.value, rightVal.value)

            is XsdString if rightVal is XsdString -> cmp(leftVal.xmlString, rightVal.xmlString)
            is XsdDateTime if rightVal is XsdDateTime -> {
                val tz = ctx.defaultTimeZone
                cmp(leftVal.ensureTimezone(tz), rightVal.ensureTimezone(tz))
            }
            is XsdDate if rightVal is XsdDate -> {
                val tz = ctx.defaultTimeZone
                cmp(leftVal.ensureTimezone(tz), rightVal.ensureTimezone(tz))
            }
            is XsdDuration if rightVal is XsdDuration -> cmp(leftVal, rightVal)
            is XsdGDay if rightVal is XsdGDay -> cmp(leftVal, rightVal)

            is XsdGMonthDay if rightVal is XsdGMonthDay -> cmp(leftVal, rightVal)
            is XsdGMonth if rightVal is XsdGMonth -> cmp(leftVal, rightVal)
            is XsdGYearMonth if rightVal is XsdGYearMonth -> cmp(leftVal, rightVal)
            is XsdGYear if rightVal is XsdGYear -> cmp(leftVal, rightVal)
            is XsdHexBinary if rightVal is XsdHexBinary -> cmp(leftVal, rightVal)

            is XsdNotation if rightVal is XsdNotation -> cmp(leftVal, rightVal)
            is XsdQName if rightVal is XsdQName -> cmp(leftVal, rightVal)
            is XsdTime if rightVal is XsdTime -> {
                val tz = ctx.defaultTimeZone
                cmp(leftVal.ensureTimezone(tz), rightVal.ensureTimezone(tz))
            }

            else -> throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Type mismatch")
        }
        return result
    }

    context(ctx: ExprEvalContext)
    open fun defaultCmp(left: XsdAtomic, right: XsdAtomic): Int {
        return when (left) {
            is XsdDate if (right is XsdDate) -> left.compareTo(right)
            is XsdDateTime if (right is XsdDateTime) -> left.compareTo(right)
            is XsdTime if (right is XsdTime) -> left.compareTo(right)
            is XsdDayTimeDuration if (right is XsdDayTimeDuration) -> left.compareTo(right)
            is XsdYearMonthDuration if (right is XsdYearMonthDuration) -> left.compareTo(right)
            is XsdByteArray if (right is XsdByteArray) -> left.compareTo(right)

            else -> throw EvaluationException(
                ErrorCodes.XPTY0004_TYPE_ERROR,
                "Comparison of '$left' and '$right' is not supported"
            )

        }
    }


    context(ctx: ExprEvalContext)
    open fun defaultCmpXXX(left: XsdAtomic, right: XsdAtomic): Boolean = numericCompare(defaultCmp(left, right))

    context(ctx: ExprEvalContext)
    abstract fun numericCompare(cmp: Int) : Boolean

    context(ctx: ExprEvalContext)
    open fun numericCompare(left: XsdNumeric<*>, right: XsdNumeric<*>): Int = left.compareTo(right)

    context(ctx: ExprEvalContext)
    open fun cmp(left: XsdInteger, right: XsdInteger): Boolean = cmpNumbers(left, right)

    context(ctx: ExprEvalContext)
    open fun cmp(left: XsdDecimal, right: XsdDecimal): Boolean = cmpNumbers(left, right)

    context(ctx: ExprEvalContext)
    open fun cmp(left: XsdFloat, right: XsdFloat): Boolean = cmpNumbers(left, right)

    context(ctx: ExprEvalContext)
    open fun cmpNumbers(left: XsdNumeric<*>, right: XsdNumeric<*>): Boolean = numericCompare(numericCompare(left, right))

    context(ctx: ExprEvalContext)
    open fun cmp(left: Double, right: Double): Boolean = numericCompare(left.compareTo(right))

    context(ctx: ExprEvalContext)
    open fun cmp(left: Float, right: Float): Boolean = numericCompare(left.compareTo(right))

    context(ctx: ExprEvalContext)
    open fun cmp(left: Boolean, right: Boolean): Boolean = numericCompare(left.compareTo(right))

    context(ctx: ExprEvalContext)
    open fun cmp(left: String, right: String): Boolean = numericCompare(left.compareTo(right))

    context(ctx: ExprEvalContext)
    open fun <T : IXsdDateTime> cmpDateTime(left: T, right: T): Boolean = numericCompare(defaultCmp(left, right))

    context(ctx: ExprEvalContext)
    open fun cmp(left: XsdDateTime, right: XsdDateTime): Boolean = cmpDateTime(left, right)

    context(ctx: ExprEvalContext)
    open fun cmp(left: XsdDate, right: XsdDate): Boolean = cmpDateTime(left, right)

    context(ctx: ExprEvalContext)
    open fun cmp(left: XsdDuration, right: XsdDuration): Boolean = defaultCmpXXX(left, right)

    context(ctx: ExprEvalContext)
    open fun cmp(left: XsdGDay, right: XsdGDay): Boolean = cmpDateTime(left, right)

    context(ctx: ExprEvalContext)
    open fun cmp(left: XsdGMonthDay, right: XsdGMonthDay): Boolean = cmpDateTime(left, right)

    context(ctx: ExprEvalContext)
    open fun cmp(left: XsdGMonth, right: XsdGMonth): Boolean = cmpDateTime(left, right)

    context(ctx: ExprEvalContext)
    open fun cmp(left: XsdGYearMonth, right: XsdGYearMonth): Boolean = cmpDateTime(left, right)

    context(ctx: ExprEvalContext)
    open fun cmp(left: XsdGYear, right: XsdGYear): Boolean = cmpDateTime(left, right)

    context(ctx: ExprEvalContext)
    open fun cmp(left: XsdHexBinary, right: XsdHexBinary): Boolean = defaultCmpXXX(left, right)

    context(ctx: ExprEvalContext)
    open fun cmp(left: XsdNotation, right: XsdNotation): Boolean = defaultCmpXXX(left, right)

    context(ctx: ExprEvalContext)
    open fun cmp(left: XsdQName, right: XsdQName): Boolean = defaultCmpXXX(left, right)

    context(ctx: ExprEvalContext)
    open fun cmp(left: XsdTime, right: XsdTime): Boolean = cmpDateTime(left, right)
}

