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

package io.github.pdvrieze.formats.xpath.eval.type

import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmFunctionTypeTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmMapTypeTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmSchemaTypeTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmSequenceTypeTest
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.SequenceType.OccurrenceType
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.AnySimpleType

@XPathInternal
class XdmMapType(
    keyType: XdmSchemaTypeTest,
    valueType: XdmSequenceTypeTest
) : XdmFunctionType(listOf(keyType), valueType) {
    init {
        require(keyType.schemaType is AnySimpleType.AtomicOrUnion<*>) { "Key type must be atomic or integer" }
    }

    val keyType: XdmSchemaTypeTest get() = argTypes.single() as XdmSchemaTypeTest
    val valueType: XdmSequenceTypeTest get() = returnType

    override fun toTypeTest(occurrence: OccurrenceType): XdmMapTypeTest.Typed {
        return XdmMapTypeTest.Typed(keyType, returnType, occurrence)
    }

    override fun toTypeTest(): XdmMapTypeTest.Typed = toTypeTest(OccurrenceType.SINGLE)

    context(ctx: ExprEvalContext)
    @XPathInternal
    override fun isAssignableTo(expectedType: XdmSequenceTypeTest): Boolean {
        val receiver = expectedType
        return when (receiver) {
            is XdmMapTypeTest -> receiver.keyType.isAssignableTo(keyType) &&
                    returnType.isAssignableTo(receiver.returnType)

            is XdmFunctionTypeTest -> super.isAssignableTo(receiver)

            else -> false
        }
    }
}
