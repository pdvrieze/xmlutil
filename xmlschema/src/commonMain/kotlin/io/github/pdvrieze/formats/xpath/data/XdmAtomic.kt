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

import io.github.pdvrieze.formats.xpath.data.ErrorCodes.FORG0006_INVALID_ARGUMENT_TYPE
import io.github.pdvrieze.formats.xpath.functions.Fn
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.UntypedAtomicType
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
        if (expected !is XdmAtomic<*>) return false
        val expectedValue = expected.value

        when (value) {
            is XsdQName -> return expectedValue is XsdQName && value.isEquivalent(expectedValue)
            is XsdDouble if (expectedValue is XsdNumeric<*>) -> return value.value == expectedValue.toDouble()
            is XsdFloat if (expectedValue is XsdNumeric<*>) -> return value.toDouble() == expectedValue.toDouble()
            is XsdDecimal -> when (expectedValue) {
                is XsdDouble,
                is XsdFloat -> return value.toDouble() == expectedValue.toDouble()
                is XsdDecimal -> return value == expectedValue
            }
        }
        return value.xmlString == expectedValue.xmlString
    }

    context(ctx: ExprEvalContext)
    override fun toBoolean(): Boolean = when (value) {
        is XsdBoolean -> value.value
        is XsdAnyURI -> value.value.isNotEmpty()
        is XsdString -> value.isNotEmpty()
        is XsdFloat -> value.value != 0.0f && !value.value.isNaN()
        is XsdDouble -> value.value != 0.0 && !value.value.isNaN()
        is XsdInteger -> value != XsdInteger.ZERO
        else -> throw EvaluationException(FORG0006_INVALID_ARGUMENT_TYPE, "Cannot cast to boolean")
    }

    context(ctx: ExprEvalContext)
    fun toXdmFloat(): XdmAtomic<XsdFloat> =
        @Suppress("UNCHECKED_CAST") // for the cast where this is a float
        when (value){
            is XsdFloat -> this as XdmAtomic<XsdFloat>
            is XsdDecimal -> XdmAtomic(XsdFloat(value.toDouble().toFloat()))
            else -> throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Value of type ${value.schemaType} cannot be cast to double")
        }

    context(ctx: ExprEvalContext)
    fun toXdmDouble(): XdmAtomic<XsdDouble> =
        @Suppress("UNCHECKED_CAST") // for the cast where this is a double
        when (value){
            is XsdDouble -> this as XdmAtomic<XsdDouble>
            is XsdFloat -> XdmAtomic(XsdDouble(value.value.toDouble()))
            is XsdDecimal -> XdmAtomic(XsdDouble(value.toDouble()))
            else -> throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Value of type ${value.schemaType} cannot be cast to double")
        }

    context(ctx: ExprEvalContext)
    fun toXdmInteger(): XdmAtomic<XsdInteger> =
        @Suppress("UNCHECKED_CAST") // for the cast where this is a double
        when (value){
            is XsdInteger -> this as XdmAtomic<XsdInteger>
            else -> XdmAtomic(XsdInteger(value.xmlString))
//            else -> throw EvaluationException(EvaluationException.ErrorCodes.XPTY0004_TYPE_ERROR, "Value of type ${value.schemaType} cannot be cast to double")
        }

    context(ctx: ExprEvalContext)
    fun toXdmDecimal(): XdmAtomic<XsdDecimal> =
        @Suppress("UNCHECKED_CAST") // for the cast where this is a double
        when (value){
            is XsdDecimal -> this as XdmAtomic<XsdDecimal>
            else -> XdmAtomic(XsdDecimal(value.xmlString))
        }


    context(ctx: ExprEvalContext)
    override fun withType(type: XdmType): XdmValue {
        TODO("Xsd coercion not yet implemented")
    }

    @XPathInternal
    context(ctx: ExprEvalContext)
    override fun normalizeToArithmetic(): XdmAtomic<*> {
        // Handle sequences
        return when {
            value is XsdDouble -> this

            !ctx.isXPath1Compat -> when (type) {
                UntypedAtomicType.Instance -> toXdmDouble()
                else -> this
            }

            this.value is XsdBoolean ||
                    this.value is XsdDecimal ||
                    this.value is XsdFloat ||
                    this.type == UntypedAtomicType.Instance -> return Fn.number(this) as XdmAtomic<*>

            else -> this
        }
    }

    override fun toString(): String = value.toString()

    companion object {
        val NaN = XdmAtomic(XsdDouble(Double.NaN))

        fun unTyped(value: String): XdmAtomic<XsdAtomic> = XdmAtomic(UntypedAtomicType.Instance.fromString(value))
    }

}

