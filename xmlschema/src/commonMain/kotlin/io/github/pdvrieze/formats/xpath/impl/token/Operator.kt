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
import io.github.pdvrieze.formats.xpath.data.XdmAtomic
import io.github.pdvrieze.formats.xpath.data.XdmValue
import io.github.pdvrieze.formats.xpath.functions.OP_BOOLEAN_EQUAL
import io.github.pdvrieze.formats.xpath.impl.*
import io.github.pdvrieze.xml.schematypes.types.BooleanType
import io.github.pdvrieze.xml.schematypes.values.XsdBoolean

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

        context(ctx: EvalContext)
        @XPathInternal
        override fun eval(left: XdmValue, right: XdmValue): XdmAtomic<XsdBoolean> {
            when {
                left.type.isA(BooleanType.Instance) -> {
                    return OP_BOOLEAN_EQUAL.eval(listOf(left, right))
                }
                else -> TODO("Equality operator not yet supported for type ${left.type} and ${right.type}")
            }
        }
    },
    @NeedsXPath1
    NEQ("!=", 5, XPathVersion.XPath1_0, true) {
        context(ctx: EvalContext)
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
    VAL_EQ("eq", 5, XPathVersion.XPath2_0, false),
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
    ADD("+", 8, XPathVersion.XPath1_0, true),
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

    context(ctx: EvalContext)
    @XPathInternal
    open fun eval(left: XdmValue, right: XdmValue): XdmValue =
        TODO("Evaluation of operator $name not yet implemented")

    context(ctx: EvalContext)
    @XPathInternal
    fun eval(param: XdmValue): XdmValue = eval(listOf(param))

    @XPathInternal
    context(ctx: EvalContext)
    fun eval(params: List<XdmValue>): XdmValue =
        params.reduce { acc, param -> eval(acc, param) }


}
