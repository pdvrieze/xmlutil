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

import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.data.XdmAtomic
import io.github.pdvrieze.formats.xpath.eval.data.XdmSingleValue
import io.github.pdvrieze.formats.xpath.eval.type.XdmSingleType
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmSchemaTypeTest
import io.github.pdvrieze.xml.schematypes.types.AnySimpleType
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.toCName

/**
 * Introduced in 3.0 to hold a QName in item type tests. However, this was just "AtomicType" in 2.0
 */
@XPathInternal
class AtomicOrUnionTypeTest(val name: QName): ItemTypeTest {
    init {
        require(name.localPart.isNotEmpty()) { "Atomic types must have non-empty local name" }
    }



    context(ctx: ExprEvalContext)
    private fun isInstance(value: XdmSingleValue<*>): Boolean {
        if (value !is XdmAtomic<*>) return false
        return value.staticType.isAssignableTo(eval())
    }

    context(c: OutputContext)
    override fun appendToString(builder: Appendable) {
        builder.appendQName(name)
    }

    override fun toString(): String = name.toCName()

    context(ctx: ExprEvalContext)
    fun eval(): AnySimpleType<*> = ctx.resolveType(name) as? AnySimpleType<*>
        ?: throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Type is not atomic or union")

    context(ctx: ExprEvalContext)
    override fun toTypeTest(occurrence: SequenceType.OccurrenceType): XdmSchemaTypeTest {
        return XdmSchemaTypeTest(ctx.resolveType(name), occurrence)
    }

    context(ctx: ExprEvalContext)
    private fun isAssignableTo(baseType: XdmSingleType): Boolean {
        TODO()
/*
        return when (baseType) {
            is XdmSchemaType -> eval().derivesFrom(baseType.schemaType)
            is XdmNodeKindTest -> baseType.isAssignableFrom(eval())
            else -> return false
        }
*/
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as AtomicOrUnionTypeTest

        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

}
