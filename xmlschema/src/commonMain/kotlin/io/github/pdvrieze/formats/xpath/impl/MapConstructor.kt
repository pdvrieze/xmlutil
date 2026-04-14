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
import io.github.pdvrieze.formats.xpath.eval.data.XdmMap
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.eval.type.XdmEmptySequenceType
import io.github.pdvrieze.formats.xpath.eval.type.XdmMapType
import io.github.pdvrieze.formats.xpath.eval.type.XdmSchemaType
import io.github.pdvrieze.formats.xpath.eval.type.XdmType
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmSchemaTypeTest
import io.github.pdvrieze.xml.schematypes.types.UntypedAtomicType

@XPathInternal
internal class MapConstructor @NeedsXPath3_1 constructor(val entries: List<Entry>): AbstractExprSingle() {

    context(c: OutputContext)
    override fun appendToString(builder: Appendable) {
        builder.append("map{")
        builder.joinHelper(entries) {
            it.key.appendToString(builder)
            append(" : ")
            it.value.appendToString(builder)
        }
    }

    data class Entry(val key: ExprSingle, val value: ExprSingle)

    @XPathInternal
    context(ctx: EvalContext)
    override fun eval(): XdmValue<*> = ctx.withExprContext(this) {
        val content = mutableMapOf<XdmAtomic<*>, XdmValue<*>>()
        var keyType: XdmSchemaType? = null
        var valueType: XdmType = XdmEmptySequenceType

        for ((kExpr, vExpr) in entries) {
            val key = kExpr.eval()
            if (key !is XdmAtomic<*>) throw EvaluationException(
                ErrorCodes.XPTY0004_TYPE_ERROR,
                "Key is not a single atomic value: $key"
            )

            val value = vExpr.eval()

            keyType = keyType?.sharedBaseType(key.staticType) ?: key.staticType
            valueType = valueType.sharedBaseType(value.staticType)

            content[key] = value
        }


        val keyTypeTest = keyType?.toTypeTest()
            ?: XdmSchemaTypeTest(UntypedAtomicType.Instance, SequenceType.OccurrenceType.SINGLE)

        val type = XdmMapType(keyTypeTest, valueType.toTypeTest())
        return XdmMap.invoke(content, type)
    }
}
