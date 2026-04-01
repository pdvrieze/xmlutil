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

import io.github.pdvrieze.formats.xpath.eval.data.XdmArray
import io.github.pdvrieze.formats.xpath.eval.data.XdmSingleValue
import io.github.pdvrieze.formats.xpath.eval.type.XdmSingleType
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmArrayTypeTest
import io.github.pdvrieze.xml.schematypes.types.AnyType

@XPathInternal
sealed class ArrayTypeTest: ItemTypeTest {
    context(ctx: ExprEvalContext)
    private fun isAssignableFrom(child: AnyType): Boolean = false

    context(ctx: ExprEvalContext)
    abstract override fun toTypeTest(occurrence: SequenceType.OccurrenceType): XdmArrayTypeTest

    @NeedsXPath3_1
    object ANY: ArrayTypeTest() {
        context(ctx: ExprEvalContext)
        private fun isInstance(value: XdmSingleValue<*>): Boolean {
            return value is XdmArray
        }

        context(ctx: ExprEvalContext)
        override fun toTypeTest(occurrence: SequenceType.OccurrenceType): XdmArrayTypeTest.Any {
            return XdmArrayTypeTest.Any(occurrence)
        }

        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.append("array(*)")
        }

        @OptIn(NeedsXPath3_0::class)
        context(ctx: ExprEvalContext)
        private fun isAssignableTo(baseType: XdmSingleType): Boolean {
            // 2.5.6.2 #33
            if (baseType == FunctionTypeTest.ANY) return true

            TODO()
/*
            // 2.5.6.2 #34
            if (baseType is FunctionTypeTest.Typed) {
                val paramType: SequenceType = (baseType.paramTypes.singleOrNull()) ?: return false
                val evalType = paramType.eval() as? XdmSchemaType ?: return false
                return evalType.schemaType.name?.isEquivalent(QName(XMLConstants.XSD_NS_URI, "integer")) == true
            }

            return baseType == ANY
*/
        }
    }

    class Typed @NeedsXPath3_1 constructor(val elemType: SequenceType): ArrayTypeTest() {
        context(ctx: ExprEvalContext)
        private fun isInstance(value: XdmSingleValue<*>): Boolean {
            TODO()
/*
            return value is XdmArray &&
                    (value.type.isAssignableTo(elemType.eval()) || value.content.all { elemType.isInstance(it) })
*/
        }

        context(ctx: ExprEvalContext)
        override fun toTypeTest(occurrence: SequenceType.OccurrenceType): XdmArrayTypeTest.Typed {
            return XdmArrayTypeTest.Typed(elemType.eval(), occurrence)
        }

        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.append("array(")
            elemType.appendToString(builder)
            builder.append(")")
        }
    }
}
