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
import io.github.pdvrieze.formats.xpath.eval.data.XdmSequence
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.eval.type.XdmSchemaType
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunction
import io.github.pdvrieze.xml.schematypes.types.AnySimpleType
import io.github.pdvrieze.xml.schematypes.values.XsdAtomic
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.namespaceURI

@XPathInternal
@NeedsXPath2
class CastExpr(val expr: Expr, val type: QName, val allowsEmpty: Boolean) : AbstractExprSingle() {
    context(c: OutputContext)
    override fun appendToString(builder: Appendable) {
        expr.appendToString(builder)
        builder.append(" cast as ")

        builder.appendQName(type)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as CastExpr

        if (expr != other.expr) return false
        if (type != other.type) return false

        return true
    }

    context(ctx: EvalContext)
    @XPathInternal
    override fun eval(): XdmValue<*> = ctx.withExprContext(this) {
        if (type.namespaceURI == BuiltinFunction.FN_NAMESPACE) when (type.localPart) {
            "NOTATION", "anySimpleType", "anyAtomicType" -> throw EvaluationException(ErrorCodes.XPST0080_INVALID_TARGET_TYPE, "The type $type cannot be a cast target")
        }

        val schemaType = ctx.resolveTypeOrNull(type)
            ?: throw EvaluationException(ErrorCodes.XQST0052_INVALID_TYPE_IN_CAST, "The type $type is not known")
        if (schemaType !is AnySimpleType.AtomicOrUnion<*>) throw EvaluationException(ErrorCodes.XQST0052_INVALID_TYPE_IN_CAST, "The type $type is not simple")

        // TODO deal with list types and sequences

        val value = expr.eval().atomize()
        if (value.isEmpty()) {
            if (!allowsEmpty) throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Cannot cast empty sequence to $type")
            return XdmSequence.EMPTY
        }

        val xdmType = XdmSchemaType(schemaType)

        val result = value.map {
            XdmAtomic(schemaType.castFrom(it.value) as XsdAtomic, xdmType)
        }

        return XdmSequence.fromList(result, xdmType)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + expr.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

}
