/*
 * Copyright (c) 2023.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.github.pdvrieze.formats.xmlschema.resolved

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VDecimal
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VDouble
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VFloat
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.*
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnnotation
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.*
import io.github.pdvrieze.formats.xmlschema.model.SimpleTypeModel.Variety

sealed class ResolvedFacet(override val schema: ResolvedSchemaLike) : ResolvedPart {
    abstract override val rawPart: XSFacet

    val fixed get() = rawPart.fixed

    val id: VID? get() = rawPart.id
    val annotation: XSAnnotation? get() = rawPart.annotation

    open fun check(type: ResolvedSimpleType) {}

    open fun validate(type: PrimitiveDatatype, decimal: VDecimal) {}

    open fun validate(float: VFloat) {}

    open fun validate(float: VDouble) {}

    companion object {
        operator fun invoke(rawPart: XSFacet, schema: ResolvedSchemaLike): ResolvedFacet = when (rawPart) {
            is XSAssertionFacet -> ResolvedAssertionFacet(rawPart, schema)
            is XSEnumeration -> ResolvedEnumeration(rawPart, schema)
            is XSExplicitTimezone -> ResolvedExplicitTimezone(rawPart, schema)
            is XSFractionDigits -> ResolvedFractionDigits(rawPart, schema)
            is XSLength -> ResolvedLength(rawPart, schema)
            is XSMaxExclusive -> ResolvedMaxExclusive(rawPart, schema)
            is XSMaxInclusive -> ResolvedMaxInclusive(rawPart, schema)
            is XSMaxLength -> ResolvedMaxLength(rawPart, schema)
            is XSMinExclusive -> ResolvedMinExclusive(rawPart, schema)
            is XSMinInclusive -> ResolvedMinInclusive(rawPart, schema)
            is XSMinLength -> ResolvedMinLength(rawPart, schema)
            is XSPattern -> ResolvedPattern(rawPart, schema)
            is XSTotalDigits -> ResolvedTotalDigits(rawPart, schema)
            is XSWhiteSpace -> ResolvedWhiteSpace(rawPart, schema)
        }
    }
}

class ResolvedAssertionFacet(override val rawPart: XSAssertionFacet, schema: ResolvedSchemaLike) :
    ResolvedFacet(schema) {
    val test get() = rawPart.test
    val xPathDefaultNamespace get() = rawPart.xPathDefaultNamespace
}

class ResolvedEnumeration(override val rawPart: XSEnumeration, schema: ResolvedSchemaLike) : ResolvedFacet(schema) {
    val value: String get() = rawPart.value.xmlString
}

class ResolvedExplicitTimezone(override val rawPart: XSExplicitTimezone, schema: ResolvedSchemaLike) :
    ResolvedFacet(schema) {

}

class ResolvedFractionDigits(override val rawPart: XSFractionDigits, schema: ResolvedSchemaLike) :
    ResolvedFacet(schema) {

    val value: ULong get() = rawPart.value
}

sealed class ResolvedLengthBase(schema: ResolvedSchemaLike) : ResolvedFacet(schema) {
    abstract val value: ULong
    override fun check(type: ResolvedSimpleType) {
        when (type.mdlVariety) {
            Variety.ATOMIC -> when (val primitive = type.mdlPrimitiveTypeDefinition) {
                null -> error("Length is not supported on simple types not deriving from a primitive")
                else -> error("Length is not supported for type ${primitive.qName}")
            }

            Variety.LIST -> {} // fine
            Variety.UNION,
            Variety.NIL -> error("Variety does not support length facet")
        }
    }

    fun validate(type: ResolvedSimpleType, representation: String): Result<Unit> = runCatching {
        when (type.mdlVariety) {
            Variety.ATOMIC -> {
                when (val primitive = type.mdlPrimitiveTypeDefinition) {
                    is AnyURIType,
                    is StringType -> checkLength(representation.length, representation)

                    is HexBinaryType -> checkLength(primitive.length(representation), "hex value")
                    is Base64BinaryType -> checkLength(primitive.length(representation), "base64 value")
                    is QNameType,
                    is NotationType -> Unit

                    else -> error("Unsupported primitive type ${primitive?.qName} for simple type restriction")
                }
            }

            Variety.LIST -> {
                val len = representation.split(' ').size
                checkLength(len, "list[$len]")
            }

            Variety.UNION,
            Variety.NIL -> error("Length Facet not supported in this variety")
        }
    }


    abstract fun checkLength(resolvedLength: Int, repr: String)
}

interface IResolvedMinLength : ResolvedPart {
    val value: ULong
    val fixed: Boolean?
    fun checkLength(resolvedLength: Int, repr: String)
    fun validate(type: ResolvedSimpleType, representation: String): Result<Unit>
    fun check(type: ResolvedSimpleType)
}

interface IResolvedMaxLength : ResolvedPart {
    val value: ULong
    val fixed: Boolean?
    fun checkLength(resolvedLength: Int, repr: String)
    fun validate(type: ResolvedSimpleType, representation: String): Result<Unit>
    fun check(type: ResolvedSimpleType)
}

class ResolvedLength(override val rawPart: XSLength, schema: ResolvedSchemaLike) : ResolvedLengthBase(schema),
    IResolvedMinLength, IResolvedMaxLength {

    override val value: ULong get() = rawPart.value
    override fun checkLength(resolvedLength: Int, repr: String) {
        check(resolvedLength == value.toInt()) { "length($resolvedLength) of ${repr} is not $value" }
    }

}

class ResolvedMinLength(override val rawPart: XSMinLength, schema: ResolvedSchemaLike) : ResolvedLengthBase(schema),
    IResolvedMinLength {

    override val value: ULong get() = rawPart.value
    override fun checkLength(resolvedLength: Int, repr: String) {
        check(resolvedLength >= value.toInt()) { "length($resolvedLength) of ${repr} is not at least $value" }
    }

}


class ResolvedMaxLength(override val rawPart: XSMaxLength, schema: ResolvedSchemaLike) : ResolvedLengthBase(schema),
    IResolvedMaxLength {

    override val value: ULong get() = rawPart.value
    override fun checkLength(resolvedLength: Int, repr: String) {
        check(resolvedLength <= value.toInt()) { "length($resolvedLength) of ${repr} is not at most $value" }
    }
}

sealed class ResolvedBoundBaseFacet(schema: ResolvedSchemaLike) : ResolvedFacet(schema) {
    abstract val isInclusive: Boolean

}

sealed class ResolvedMaxBoundFacet(schema: ResolvedSchemaLike) : ResolvedBoundBaseFacet(schema)

sealed class ResolvedMinBoundFacet(schema: ResolvedSchemaLike) : ResolvedBoundBaseFacet(schema)

class ResolvedMaxExclusive(override val rawPart: XSMaxExclusive, schema: ResolvedSchemaLike) :
    ResolvedMaxBoundFacet(schema) {
    override val isInclusive: Boolean get() = false

    override fun validate(type: PrimitiveDatatype, decimal: VDecimal) {
        check(decimal.toLong() < rawPart.value.xmlString.toLong())
    }
}


class ResolvedMaxInclusive(override val rawPart: XSMaxInclusive, schema: ResolvedSchemaLike) :
    ResolvedMaxBoundFacet(schema) {
    override val isInclusive: Boolean get() = true

    override fun validate(type: PrimitiveDatatype, decimal: VDecimal) {
        check(decimal.toLong() <= rawPart.value.xmlString.toLong())
    }
}


class ResolvedMinExclusive(override val rawPart: XSMinExclusive, schema: ResolvedSchemaLike) :
    ResolvedMinBoundFacet(schema) {
    override val isInclusive: Boolean get() = false

    override fun validate(type: PrimitiveDatatype, decimal: VDecimal) {
        check(decimal.toLong() > rawPart.value.xmlString.toLong())
    }
}

class ResolvedMinInclusive(override val rawPart: XSMinInclusive, schema: ResolvedSchemaLike) :
    ResolvedMinBoundFacet(schema) {
    override val isInclusive: Boolean get() = true

    override fun validate(type: PrimitiveDatatype, decimal: VDecimal) {
        check(decimal.toLong() >= rawPart.value.xmlString.toLong())
    }
}

class ResolvedPattern(override val rawPart: XSPattern, schema: ResolvedSchemaLike) : ResolvedFacet(schema) {
    val value: String get() = rawPart.value
    val regex: Regex by lazy { Regex(rawPart.value) }

    override fun toString(): String = "Pattern('$value')"
}

class ResolvedTotalDigits(override val rawPart: XSTotalDigits, schema: ResolvedSchemaLike) : ResolvedFacet(schema) {

    val value: ULong = rawPart.value
}

class ResolvedWhiteSpace(override val rawPart: XSWhiteSpace, schema: ResolvedSchemaLike) : ResolvedFacet(schema) {
    val value: XSWhiteSpace.Values get() = rawPart.value


}

