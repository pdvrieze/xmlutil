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

import io.github.pdvrieze.formats.xpath.eval.data.XdmMap
import io.github.pdvrieze.formats.xpath.eval.data.XdmSingleValue
import io.github.pdvrieze.formats.xpath.eval.type.XdmSingleType
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmMapTypeTest
import io.github.pdvrieze.formats.xpath.impl.SequenceType.OccurrenceType
import io.github.pdvrieze.xml.schematypes.types.AnyType

@OptIn(NeedsXPath3_0::class, NeedsXPath3_1::class)
@XPathInternal
sealed class MapTypeTest @NeedsXPath3_1 constructor(): ItemTypeTest {
    context(ctx: ExprEvalContext)
    private fun isAssignableFrom(child: AnyType): Boolean = false

    context(ctx: ExprEvalContext)
    abstract override fun toTypeTest(occurrence: OccurrenceType): XdmMapTypeTest

    @NeedsXPath3_1
    object ANY: MapTypeTest() {
        context(ctx: ExprEvalContext)
        override fun toTypeTest(occurrence: OccurrenceType): XdmMapTypeTest.AnyMap {
            return XdmMapTypeTest.AnyMap(occurrence)
        }

        context(ctx: ExprEvalContext)
        private fun isInstance(value: XdmSingleValue<*>): Boolean = value is XdmMap

        context(ctx: ExprEvalContext)
        private fun isAssignableTo(baseType: XdmSingleType): Boolean {
            // 2.5.6.2 #29
            if (baseType == FunctionTypeTest.ANY) return true

            // 2.5.6.2 #30
/*
            if (baseType is FunctionTypeTest.Typed) {
                val paramType: SequenceType = (baseType.paramTypes.singleOrNull()) ?: return false
                val evalType = paramType.eval() as? XdmSchemaType ?: return false
                if(evalType.schemaType.name?.isEquivalent(AnyAtomicType.Instance.name) == true) {
                    val otherReturnType = baseType.returnType.eval()
                    if (otherReturnType is XdmTypeTest && otherReturnType.itemType == ItemTypeTest.ItemTestTest) {
                        return true
                    }
                }
            }
*/

            return baseType == ANY
        }

        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.append("map(*)")
        }
    }

    class Typed @NeedsXPath3_1 constructor(val inputType: AtomicOrUnionTypeTest, val outputType: SequenceType) :
        MapTypeTest() {
        context(ctx: ExprEvalContext)
        override fun toTypeTest(occurrence: OccurrenceType): XdmMapTypeTest {
            return XdmMapTypeTest.Typed(inputType.toTypeTest(OccurrenceType.SINGLE), outputType.eval(), occurrence)
        }

        context(ctx: ExprEvalContext)
        private fun isInstance(value: XdmSingleValue<*>): Boolean {
            TODO()
/*
            return when {
                value !is XdmMap -> false
                ! value.type.valueType.isAssignableTo(outputType.eval()) -> false
                else -> inputType.isAssignableFrom(value.type.keyType)

//                else -> true
            }
*/
        }

        context(ctx: ExprEvalContext)
        private fun isAssignableTo(baseType: XdmSingleType): Boolean {
            TODO()
/*
            if (baseType == NodeKindTest.AnyNode || baseType == ANY) return true
            if (baseType is FunctionTypeTest) {
                when (baseType) {
                    FunctionTypeTest.ANY -> return true
                    is FunctionTypeTest.Typed -> {
                        val paramType = baseType.paramTypes.singleOrNull() ?: return false
                        paramType.eval()
                        baseType.paramTypes.singleOrNull()?.let {}

                    }
                }
            }
            if (baseType !is Typed) return false
            return inputType.isAssignableTo(baseType.inputType) &&
                    outputType.eval().isAssignableTo(baseType.outputType.eval())
*/
        }

        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.append("map(")
            builder.appendQName(inputType.name)
            builder.append(", ")
            outputType.appendToString(builder)
            builder.append(")")
        }
    }
}

