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
import io.github.pdvrieze.formats.xpath.eval.type.XdmSchemaType
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunction
import io.github.pdvrieze.xml.schematypes.types.AnySimpleType
import io.github.pdvrieze.xml.schematypes.types.UntypedAtomicType
import io.github.pdvrieze.xml.schematypes.values.XsdBoolean
import io.github.pdvrieze.xml.schematypes.values.XsdString
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.namespaceURI

@XPathInternal
@NeedsXPath2
class CastableExpr(val expr: Expr, val type: QName, val allowsEmpty: Boolean) : AbstractExprSingle() {
    context(c: OutputContext)
    override fun appendToString(builder: Appendable) {
        expr.appendToString(builder)
        builder.append(" castable as ")

        builder.appendQName(type)
        if (allowsEmpty) builder.append('?')
    }

    @XPathInternal
    context(ctx: EvalContext)
    override fun eval(): XdmAtomic<XsdBoolean> = ctx.withExprContext(this) {
        if (type.namespaceURI == BuiltinFunction.FN_NAMESPACE) when (type.localPart) {
            "NOTATION", "anySimpleType", "anyAtomicType" -> throw EvaluationException(ErrorCodes.XPST0080_INVALID_TARGET_TYPE, "The type $type cannot be a cast target")
        }

        val value = try { expr.eval().atomize() } catch (_: EvaluationException) {
            // allow for capturing the error
            return XdmAtomic((false))
        }
        if (value.isEmpty()) return XdmAtomic(allowsEmpty)


        val (isList, schemaType) = castItemType(type)

        // handles error type
        if (schemaType !is AnySimpleType.AtomicOrUnion<*>) return XdmAtomic(false)

        // TODO deal with list types and sequences

        if (value.size > 1) {
            return XdmAtomic((false))
        }

        val r = ctx.withExprContext(this) { XdmSchemaType(schemaType).canCastFrom(value.staticType.single) }
        if (!r) return XdmAtomic((false))

        for (singleVal in value) {
            val s = when {
                singleVal.staticType.isAssignableTo(UntypedAtomicType.Instance) ->
                    XdmAtomic(XsdString(singleVal.value.xmlString))

                else -> singleVal
            }
            val runs = runCatching { schemaType.castFrom(s.value) }.isSuccess
            if (! runs) return XdmAtomic(false)
        }
        return XdmAtomic(true)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as CastableExpr

        if (allowsEmpty != other.allowsEmpty) return false
        if (expr != other.expr) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + allowsEmpty.hashCode()
        result = 31 * result + expr.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

}
