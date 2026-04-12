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

import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.data.XdmAtomic
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmSchemaTypeTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmSequenceTypeTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmTypeTest
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.SequenceType.OccurrenceType
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.formats.xpath.impl.token.NodeType
import io.github.pdvrieze.xml.schematypes.types.*
import io.github.pdvrieze.xml.schematypes.values.XsdAtomic
import io.github.pdvrieze.xml.schematypes.values.isEquivalent

@XPathInternal
class XdmSchemaType(
    val schemaType: AnyType
) : XdmSingleType() {

    override fun toTypeTest(occurrence: OccurrenceType): XdmSchemaTypeTest {
        return XdmSchemaTypeTest(schemaType, occurrence)
    }

    override fun toTypeTest(): XdmSchemaTypeTest = toTypeTest(OccurrenceType.SINGLE)

    context(ctx: ExprEvalContext)
    override fun isAssignableTo(expectedType: XdmSequenceTypeTest): Boolean {
        return when (expectedType) {
            is XdmTypeTest.AnyItem -> true
            !is XdmSchemaTypeTest -> false
            else -> expectedType.schemaType.isBaseOf(schemaType)
        }
    }

    val isGeneralizedAtomic: Boolean by lazy {
        when (schemaType) {
            is AnyAtomicType<*> -> true
            is AnySimpleUnion<*> -> schemaType.isPureUnion()
            else -> false
        }
    }

    context(ctx: ExprEvalContext)
    fun canCastFrom(sourceType: XdmSingleType): Boolean {
        val sourceSchemaType = when (sourceType) {
            is XdmSchemaType -> sourceType.schemaType
            is XdmNodeType if(sourceType.nodeType == NodeType.TEXT) -> UNTYPED_ATOMIC
            else -> return false // not simple
        }
        if (schemaType !is AnySimpleType<*> || sourceSchemaType!is AnySimpleType<*>) return false

        return schemaType.canCastFrom(sourceSchemaType)
    }

    context(ctx: ExprEvalContext)
    override fun fromString(value: String): XdmAtomic<*> {
        if (schemaType !is AnyAtomicType<*>)
            throw EvaluationException("Cannot convert string to non-atomic type")
        val xsdValue: XsdAtomic = schemaType.fromString(value)
        return XdmAtomic(xsdValue)
    }

    override fun toString(): String {
        return schemaType.name.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as XdmSchemaType

        return schemaType.name.isEquivalent(other.schemaType.name)
    }

    override fun hashCode(): Int {
        return schemaType.hashCode()
    }


    companion object {
        val UNTYPED = XdmSchemaType(UntypedType.Instance)
        val UNTYPED_ATOMIC = XdmSchemaType(UntypedAtomicType.Instance)
        val ANY = XdmSchemaType(AnyType.Instance)
    }
}
