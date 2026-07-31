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

import io.github.pdvrieze.formats.xpath.eval.data.XdmFunction
import io.github.pdvrieze.formats.xpath.eval.data.XdmSingleValue
import io.github.pdvrieze.formats.xpath.eval.type.XdmSingleType
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmFunctionTypeTest
import io.github.pdvrieze.xml.schematypes.types.AnyType

@OptIn(NeedsXPath3_0::class)
@XPathInternal
sealed class FunctionTypeTest @NeedsXPath3_0 constructor(): ItemTypeTest {
    context(ctx: ExprEvalContext)
    private fun isAssignableFrom(child: AnyType): Boolean = false


    context(ctx: ExprEvalContext)
    abstract override fun toTypeTest(occurrence: SequenceType.OccurrenceType): XdmFunctionTypeTest


    @NeedsXPath3_0
    object ANY: FunctionTypeTest() {
        context(ctx: ExprEvalContext)
        private fun isInstance(value: XdmSingleValue<*>): Boolean {
            return value is XdmFunction<*>
        }

        context(ctx: ExprEvalContext)
        override fun toTypeTest(occurrence: SequenceType.OccurrenceType): XdmFunctionTypeTest.AnyFunction {
            return XdmFunctionTypeTest.AnyFunction(occurrence)
        }

        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.append("function(*)")
        }

        context(ctx: ExprEvalContext)
        private fun isAssignableTo(baseType: XdmSingleType): Boolean {
            if (baseType == NodeKindTest.AnyNode) return true
            return baseType == ANY
        }
    }

    class Typed @NeedsXPath3_0 constructor(val returnType: SequenceType, val paramTypes: List<SequenceType>): FunctionTypeTest() {
        context(ctx: ExprEvalContext)
        override fun toTypeTest(occurrence: SequenceType.OccurrenceType): XdmFunctionTypeTest.Typed {
            return XdmFunctionTypeTest.Typed(paramTypes.map { it.eval() }, returnType.eval(), occurrence)
        }

        context(ctx: ExprEvalContext)
        private fun isInstance(value: XdmSingleValue<*>): Boolean {
            if (value !is XdmFunction<*>) return false

            val staticType = value.staticType
            return when {
                staticType.argTypes.size != paramTypes.size -> false
                staticType.argTypes.indices.any {
                    !paramTypes[it].eval().isAssignableTo(staticType.argTypes[it])
                } -> false
                //            !value.type.returnType.isAssignableTo(returnType.eval()) -> false
                else -> true
            }
        }

        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.append("function(")
            builder.joinHelper(paramTypes) {
                it.appendToString(builder)
            }
            builder.append(") as ")
            returnType.appendToString(builder)
        }

        context(ctx: ExprEvalContext)
        private fun isAssignableTo(baseType: XdmSingleType): Boolean {
            if (baseType == NodeKindTest.AnyNode || baseType == ANY) return true
            TODO()
/*
            if (baseType !is Typed) return false
            if (baseType.paramTypes.size != paramTypes.size) return false
            if (!returnType.eval().isAssignableTo(baseType.returnType.eval())) return false
            for (i in paramTypes.indices) {
                if (!baseType.paramTypes[i].eval().isAssignableTo(paramTypes[i].eval())) return false
            }
            return true
*/
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Typed

            if (returnType != other.returnType) return false
            if (paramTypes != other.paramTypes) return false

            return true
        }

        override fun hashCode(): Int {
            var result = returnType.hashCode()
            result = 31 * result + paramTypes.hashCode()
            return result
        }


    }
}

