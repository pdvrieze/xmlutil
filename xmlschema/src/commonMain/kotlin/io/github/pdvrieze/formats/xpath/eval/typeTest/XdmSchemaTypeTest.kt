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

package io.github.pdvrieze.formats.xpath.eval.typeTest

import io.github.pdvrieze.formats.xpath.eval.data.XdmSingleValue
import io.github.pdvrieze.formats.xpath.eval.type.XdmSchemaType
import io.github.pdvrieze.formats.xpath.eval.type.XdmSingleType
import io.github.pdvrieze.formats.xpath.eval.type.XdmType
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.NodeKindTest
import io.github.pdvrieze.formats.xpath.impl.SequenceType.OccurrenceType
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.AnyAtomicType
import io.github.pdvrieze.xml.schematypes.types.AnySimpleType
import io.github.pdvrieze.xml.schematypes.types.AnyType

class XdmSchemaTypeTest(val schemaType: AnyType, cardinality: OccurrenceType) : XdmTypeTest(cardinality) {

    @XPathInternal
    context(ctxt: ExprEvalContext)
    override fun isAssignableFromSingle(source: XdmSequenceTypeTest): Boolean {
        return source is XdmSchemaTypeTest && source.schemaType.derivesFrom(schemaType)
    }

    context(ctx: ExprEvalContext)
    @XPathInternal
    override fun sharedBaseType(
        other: XdmTypeTest,
        neededCardinality: OccurrenceType
    ): XdmSequenceTypeTest {
        // TODO handle further possibility of element test
        when {
            other is XdmNodeKindTest && schemaType !is AnySimpleType<*> -> return XdmNodeKindTest(
                NodeKindTest.AnyNode,
                neededCardinality
            )

            other !is XdmSchemaTypeTest -> return AnyItem(neededCardinality)
        }

        return XdmSchemaTypeTest(schemaType.sharedBaseType(other.schemaType), neededCardinality)
    }

    @OptIn(XPathInternal::class)
    override fun toValueType(fallbackType: XdmSingleType): XdmType {
        return XdmSchemaType(schemaType).cardinality(cardinality)
    }

    context(ctx: ExprEvalContext)
    @XPathInternal
    override fun isSingleInstance(value: XdmSingleValue<*>): Boolean {
        return value.dynamicType.isAssignableTo(this)
    }

    override val opt: XdmSchemaTypeTest get() = XdmSchemaTypeTest(schemaType, OccurrenceType.OPTIONAL)
    override val single: XdmSchemaTypeTest get() = XdmSchemaTypeTest(schemaType, OccurrenceType.SINGLE)
    override val any: XdmSchemaTypeTest get() = XdmSchemaTypeTest(schemaType, OccurrenceType.ANY)
    override val atLeastOne: XdmSchemaTypeTest get() = XdmSchemaTypeTest(schemaType, OccurrenceType.AT_LEAST_ONE)

    override fun toString(): String = buildString {
        append(schemaType.name)
        if (cardinality != OccurrenceType.SINGLE) append(cardinality.literal)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as XdmSchemaTypeTest

        return schemaType == other.schemaType
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + schemaType.hashCode()
        return result
    }


    companion object {
        val ANY_ATOMIC = XdmSchemaTypeTest(AnyAtomicType.Instance, OccurrenceType.SINGLE)
    }

}
