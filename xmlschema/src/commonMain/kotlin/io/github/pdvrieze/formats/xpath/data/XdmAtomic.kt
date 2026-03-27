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

import io.github.pdvrieze.formats.xpath.data.EvaluationException.ErrorCodes.FORG0006_INVALID_ARGUMENT_TYPE
import io.github.pdvrieze.formats.xpath.impl.*
import io.github.pdvrieze.xml.schematypes.values.*

@OptIn(XPathInternal::class)
class XdmAtomic<T: XsdAtomic>(val value: T) : XdmSingleValue<XdmAtomic<T>>() {
    override fun asT(): XdmAtomic<T> = this

    context(ctx: ExprEvalContext)
    override fun atomize(): XdmAtomic<T> = this

    context(ctx: ExprEvalContext)
    override fun atomizeTo(receiver: MutableList<in XdmSingleValue<*>>) {
        receiver.add(this)
    }

    override val type: XdmType
        get() = XdmSchemaType(value.schemaType)

    override fun isValEqual(expected: XdmValue): Boolean {
        return expected is XdmAtomic<*> && value == expected.value
    }

    context(ctx: ExprEvalContext)
    override fun toBoolean(): Boolean = when (value) {
        is XsdBoolean -> value.value
        is XsdAnyURI -> value.value.isNotEmpty()
        is XsdString -> value.isNotEmpty()
        is XsdFloat -> value.value != 0.0f && !value.value.isNaN()
        is XsdDouble -> value.value != 0.0 && !value.value.isNaN()
        is XsdInteger -> value != XsdInteger.ZERO
        else -> throw EvaluationException(
            FORG0006_INVALID_ARGUMENT_TYPE,
            contextOf<ExprEvalContext>().expr,
            "Cannot cast to boolean"
        )
    }

    context(ctx: ExprEvalContext)
    override fun withType(type: XdmType): XdmValue {
        TODO("Xsd coercion not yet implemented")
    }

    override fun toString(): String = value.toString()
}

