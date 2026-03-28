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
import io.github.pdvrieze.formats.xpath.data.*
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunction.FN
import io.github.pdvrieze.formats.xpath.functions.OP_BOOLEAN_EQUAL
import io.github.pdvrieze.formats.xpath.impl.*
import io.github.pdvrieze.xml.schematypes.types.*
import io.github.pdvrieze.xml.schematypes.values.*

enum class Operator(
    override val literal: String,
    val priority: Int,
    override val minVersion: XPathVersion = XPathVersion.XPath3_1,
    override val isDelimiting: Boolean,
): WordToken {
//    @NeedsXPath2
    COMMA(",", 1, XPathVersion.XPath2_0, true),

    // FOR|LET|SOME|EVERY|IF -> 2, isDelimiting = false

    @NeedsXPath1
    OR("or", 3, XPathVersion.XPath1_0, false),
    @NeedsXPath1
    AND("and", 4, XPathVersion.XPath1_0, false),

    @NeedsXPath1
    EQ("=", 5, XPathVersion.XPath1_0, true) {
        @OptIn(NeedsXPath3_1::class)
        override val longer: List<Operator> get() = listOf(ARROW)

        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun eval(left: XdmValue, right: XdmValue): XdmAtomic<XsdBoolean> {
            when {
                left.type.isSubtypeOf(BooleanType.Instance) -> {
                    return OP_BOOLEAN_EQUAL.eval(listOf(left, right))
                }
                else -> TODO("Equality operator not yet supported for type ${left.type} and ${right.type}")
            }
        }
    },
    @NeedsXPath1
    NEQ("!=", 5, XPathVersion.XPath1_0, true) {
        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun eval(left: XdmValue, right: XdmValue): XdmAtomic<XsdBoolean> {
            val eval = (EQ.eval(left, right) as XdmAtomic<*>).value as XsdBoolean
            return XdmAtomic(XsdBoolean(! eval.value))
        }
    },

    @NeedsXPath1
    LT("<", 5, XPathVersion.XPath1_0, true) {
        @OptIn(NeedsXPath2::class)
        override val longer: List<Operator> get() = listOf(LE, PRECEDES)
    },
    @NeedsXPath1
    LE("<=", 5, XPathVersion.XPath1_0, true),
    @NeedsXPath1
    GT(">", 5, XPathVersion.XPath1_0, true){
        @OptIn(NeedsXPath2::class)
        override val longer: List<Operator> get() = listOf(GE, FOLLOWS)
    },
    @NeedsXPath1
    GE(">=", 5, XPathVersion.XPath1_0, true),
    @NeedsXPath2
    VAL_EQ("eq", 5, XPathVersion.XPath2_0, false) {

        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun eval(left: XdmValue, right: XdmValue): XdmAtomic<XsdBoolean> {
            when {
                left.type.isSubtypeOf(BooleanType.Instance) -> {
                    return OP_BOOLEAN_EQUAL.eval(listOf(left, right))
                }
                else -> TODO("Equality operator not yet supported for type ${left.type} and ${right.type}")
            }
        }

    },
    @NeedsXPath2
    VAL_NEQ("ne", 5, XPathVersion.XPath2_0, false),
    @NeedsXPath2
    VAL_LT("lt", 5, XPathVersion.XPath2_0, false),
    @NeedsXPath2
    VAL_LE("le", 5, XPathVersion.XPath2_0, false),
    @NeedsXPath2
    VAL_GT("gt", 5, XPathVersion.XPath2_0, false),
    @NeedsXPath2
    VAL_GE("ge", 5, XPathVersion.XPath2_0, false),
    @NeedsXPath2
    PRECEDES("<<", 5, XPathVersion.XPath2_0, true),
    @NeedsXPath2
    FOLLOWS(">>", 5, XPathVersion.XPath2_0, true),
    @NeedsXPath2
    IS("is", 5, XPathVersion.XPath2_0, false),

    @NeedsXPath3_0
    CONCAT("||", 6, XPathVersion.XPath3_0, true),
    @NeedsXPath2
    TO("to", 7, XPathVersion.XPath2_0, false),

    @NeedsXPath1
    ADD("+", 8, XPathVersion.XPath1_0, true) {

    },
    @NeedsXPath1
    SUB("-", 8, XPathVersion.XPath1_0, true),

    @NeedsXPath1
    MUL("*", 9, XPathVersion.XPath1_0, true),
    @NeedsXPath1
    DIV("div", 9, XPathVersion.XPath1_0, false),
    @NeedsXPath2
    IDIV("idiv", 9, XPathVersion.XPath2_0, false),
    @NeedsXPath1
    MOD("mod", 9, XPathVersion.XPath1_0, false),

    @NeedsXPath1
    UNION("union", 10, XPathVersion.XPath1_0, false),
    @NeedsXPath1
    PIPEUNION("|", 10, XPathVersion.XPath1_0, true){
        @OptIn(NeedsXPath3_0::class)
        override val longer: List<Operator> = listOf(CONCAT)
    },

    @NeedsXPath2
    INTERSECT("intersect", 11, XPathVersion.XPath2_0, false),
    @NeedsXPath2
    EXCEPT("except", 11, XPathVersion.XPath2_0, false),

    // INSTANC_EOF -> 12, isDelimiting = false
    // TREAT_AS -> 13, isDelimiting = false
    // CASTABLE_AS -> 14, isDelimiting = false
    // CAST_AS -> 15, isDelimiting = false

    @NeedsXPath3_1 ARROW("=>", 16, XPathVersion.XPath3_1, true),

    @NeedsXPath1 UNARY_MINUS("-", 17, XPathVersion.XPath1_0, true),
    @NeedsXPath2 UNARY_PLUS("+", 17, XPathVersion.XPath2_0, true),

    @NeedsXPath3_0 MAP("!", 18, XPathVersion.XPath3_0, true) {
        @OptIn(NeedsXPath3_1::class)
        override val longer: List<Operator> get() = listOf(NEQ)
    },


    // '/', '//' (path separators) -> 19, isDelimiting = true
    // '[', '?' (binary lookup) -> 20, isDelimiting = true
    // '?' (unary lookup) -> 21, isDelimiting = true
    ;

    open val longer: List<Operator> get() = emptyList()

    context(ctx: ExprEvalContext)
    @XPathInternal
    open fun eval(left: XdmValue, right: XdmValue): XdmValue =
        TODO("Evaluation of operator $name not yet implemented")

    context(ctx: ExprEvalContext)
    @XPathInternal
    fun eval(param: XdmValue): XdmValue = eval(listOf(param))

    @XPathInternal
    context(ctx: ExprEvalContext)
    fun eval(params: List<XdmValue>): XdmValue =
        params.reduce { acc, param -> eval(acc, param) }


}

interface ArithmeticOperator {
    @XPathInternal
    context(ctx: ExprEvalContext)
    fun normalizeToArithmetic(value: XdmValue): XdmValue {
        val v1 = value.atomize()
        // Handle sequences
        val v2: XdmSingleValue<*> = when (v1.size) {
            0 -> return if (ctx.isXPath1Compat) XdmAtomic.NaN else XdmSequence.EMPTY
            1 -> v1[0]
            else if ctx.isXPath1Compat -> v1[0]
            else -> throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Sequence as arithmatic operand")
        }
        if (v2 !is XdmAtomic<*>) throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Value type ${value.type} is not compatible with an arithmetic operator")
        when {
            v2.value is XsdDouble -> return v2
            !ctx.isXPath1Compat -> {
                if (v2.type == UntypedAtomicType.Instance) {
                    return v2.toXdmDouble()
                } else return v2
            }
            v2.value is XsdBoolean ||
                    v2.value is XsdDecimal ||
                    v2.value is XsdFloat ||
                    v2.type == UntypedAtomicType.Instance -> return FN.NUMBER.eval(v2)
        }
        return v2
    }

    @XPathInternal
    context(ctx: ExprEvalContext)
    fun eval(left: XdmValue, right: XdmValue): XdmValue {
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
    fun evalFloat(left: Float, right: Float): XdmValue

    @XPathInternal
    context(ctx: ExprEvalContext)
    fun evalDouble(left: Double, right: Double): XdmValue

    @XPathInternal
    context(ctx: ExprEvalContext)
    fun evalInteger(left: XsdInteger, right: XsdInteger): XdmValue

    @XPathInternal
    context(ctx: ExprEvalContext)
    fun evalDecimal(left: XsdDecimal, right: XsdDecimal): XdmValue

//    fun evalNormalized(left: XdmAtomic<*>, right: XdmValue<*>)
}
