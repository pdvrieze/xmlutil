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
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.*
import io.github.pdvrieze.xml.schematypes.values.XsdAtomic

@XPathInternal
class XdmSchemaType(
    val schemaType: AnyType
) : XdmSingleType() {

/*
    init {
        val n = schemaType.name
        if (n != null) {
            require(!n.isEquivalent(AnyType.Instance.name)) { "AnyType cannot be instantiated" }
            require(!n.isEquivalent(AnySimpleType.Instance.name)) { "AnySimpleType cannot be instantiated" }
            require(!n.isEquivalent(AnyAtomicType.Instance.name)) { "AnyAtomicType cannot be instantiated" }
        }
    }
*/

    context(ctx: ExprEvalContext)
    override fun isAssignableTo(expectedType: XdmSequenceTypeTest): Boolean {
        return when (expectedType) {
            is XdmTypeTest.Any -> true
            !is XdmSchemaTypeTest -> false
            else -> schemaType.derivesFrom(expectedType.schemaType)
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
    override fun fromString(value: String): XdmAtomic<*> {
        if (schemaType !is AnyAtomicType<*>) throw EvaluationException(
            ctx.expr,
            "Cannot convert string to non-atomic type"
        )
        val xsdValue: XsdAtomic = schemaType.fromString(value)
        return XdmAtomic(xsdValue)
    }

    override fun toString(): String {
        return schemaType.name.toString()
    }

    companion object {
        val UNTYPED = XdmSchemaType(UntypedType.Instance)
        val UNTYPED_ATOMIC = XdmSchemaType(UntypedAtomicType.Instance)
        val ANY = XdmSchemaType(AnyType.Instance)
    }
}
