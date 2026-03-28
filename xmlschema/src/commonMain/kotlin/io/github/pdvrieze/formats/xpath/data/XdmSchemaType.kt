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

package io.github.pdvrieze.formats.xpath.data

import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.*
import io.github.pdvrieze.xml.schematypes.values.XsdAtomic

@XPathInternal
class XdmSchemaType(
    val schemaType: AnyType
) : XdmSingleType() {

    context(ctx: ExprEvalContext)
    override fun isSubtypeOf(other: XdmSingleType): Boolean {
        when {
            other !is XdmSchemaType -> return false

            // 2.5.6.2 #1 union type derivation
            other.schemaType is AnySimpleType.AtomicOrUnion<*> &&
                    schemaType.derivesFrom(other.schemaType) -> return true

            // 2.5.6.2 #2 all union elements
            schemaType is AnySimpleUnion<*> && schemaType.members.all { XdmSchemaType(it).isSubtypeOf(other) } ->
                return true

        }

        return false
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
        if (schemaType !is AnyAtomicType<*>) throw EvaluationException(ctx.expr, "Cannot convert string to non-atomic type")
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
