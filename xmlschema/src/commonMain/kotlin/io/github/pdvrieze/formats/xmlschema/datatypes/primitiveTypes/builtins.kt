/*
 * Copyright (c) 2021.
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

package io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.*
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.*
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.*
import io.github.pdvrieze.formats.xmlschema.model.SimpleTypeModel
import io.github.pdvrieze.formats.xmlschema.model.TypeModel
import io.github.pdvrieze.formats.xmlschema.resolved.*
import io.github.pdvrieze.formats.xmlschema.resolved.facets.*
import io.github.pdvrieze.formats.xmlschema.types.CardinalityFacet.Cardinality
import io.github.pdvrieze.formats.xmlschema.types.FundamentalFacets
import io.github.pdvrieze.formats.xmlschema.types.OrderedFacet.Order
import nl.adaptivity.xmlutil.QName
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

fun builtinType(localName: String, targetNamespace: String): Datatype? {
    if (targetNamespace != XmlSchemaConstants.XS_NAMESPACE) return null
    return when (localName) {
        "anyType" -> AnyType
        "anySimpleType" -> AnySimpleType
        "anyAtomicType" -> AnyAtomicType
        "anyURI" -> AnyURIType
        "base64Binary" -> Base64BinaryType
        "boolean" -> BooleanType
        "date" -> DateType
        "dateTime" -> DateTimeType
        "dateTimeStamp" -> DateTimeStampType
        "decimal" -> DecimalType
        "integer" -> IntegerType
        "long" -> LongType
        "int" -> IntType
        "short" -> ShortType
        "byte" -> ByteType
        "nonNegativeInteger" -> NonNegativeIntegerType
        "positiveInteger" -> PositiveIntegerType
        "unsignedLong" -> UnsignedLongType
        "unsignedInt" -> UnsignedIntType
        "unsignedShort" -> UnsignedShortType
        "unsignedByte" -> UnsignedByteType
        "nonPositiveInteger" -> NonPositiveIntegerType
        "negativeInteger" -> NegativeIntegerType
        "double" -> DoubleType
        "duration" -> DurationType
        "dayTimeDuration" -> DayTimeDurationType
        "yearMonthDuration" -> YearMonthDurationType
        "float" -> FloatType
        "gDay" -> GDayType
        "gMonth" -> GMonthType
        "gMonthDay" -> GMonthDayType
        "gYear" -> GYearType
        "gYearMonth" -> GYearMonthType
        "hexBinary" -> HexBinaryType
        "NOTATION" -> NotationType
        "QName" -> QNameType
        "string" -> StringType
        "normalizedString" -> NormalizedStringType
        "token" -> TokenType
        "language" -> LanguageType
        "Name" -> NameType
        "NCName" -> NCNameType
        "ENTITY" -> EntityType
        "ID" -> IDType
        "IDREF" -> IDRefType
        "NMTOKEN" -> NMTokenType
        "time" -> TimeType
        "ENTITIES" -> EntitiesType
        "IDREFS" -> IDRefsType
        "NMTOKENS" -> NMTokensType
        else -> null
    }
}

sealed interface IStringType: ResolvedBuiltinSimpleType {
    fun value(representation: String): VString
}

sealed interface IDecimalType: ResolvedBuiltinSimpleType {
    fun value(representation: String): VDecimal
}

sealed class AtomicDatatype(name: String, targetNamespace: String) : Datatype(name, targetNamespace),
    ResolvedBuiltinSimpleType {

    override val name: VNCName get() = super<Datatype>.name
    override val targetNamespace: VAnyURI
        get() = super<Datatype>.targetNamespace

    override val model: AtomicDatatype get() = this

    abstract override val mdlBaseTypeDefinition: ResolvedBuiltinType
    abstract override val mdlFacets: FacetList
    abstract override val mdlFundamentalFacets: FundamentalFacets
    override val mdlVariety: SimpleTypeModel.Variety get() = SimpleTypeModel.Variety.ATOMIC
    override val mdlPrimitiveTypeDefinition: PrimitiveDatatype? get() = null

    final override val mdlItemTypeDefinition: ResolvedSimpleType? get() = null
    final override val mdlMemberTypeDefinitions: List<ResolvedSimpleType> get() = emptyList()

    final override val mdlFinal: Set<TypeModel.Derivation> get() = emptySet()
}

sealed class PrimitiveDatatype(name: String, targetNamespace: String) : AtomicDatatype(name, targetNamespace) {
    abstract fun value(representation: String): Any

    abstract override val baseType: ResolvedBuiltinType
    override val simpleDerivation: ResolvedSimpleRestrictionBase
        get() = SimpleBuiltinRestriction(baseType)

    final override val mdlBaseTypeDefinition: ResolvedBuiltinType get() = baseType
    override val mdlPrimitiveTypeDefinition: PrimitiveDatatype? get() = this
}

object AnyAtomicType : AtomicDatatype("anyAtomicType", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnySimpleType get() = AnySimpleType
    override val simpleDerivation: ResolvedSimpleRestrictionBase =
        SimpleBuiltinRestriction(AnySimpleType)

    override val mdlBaseTypeDefinition: AnySimpleType get() = baseType

    override val mdlFacets: FacetList get() = FacetList.EMPTY

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun validateValue(representation: Any) {
        error("Atomic is not directly usable")
    }

    override fun validate(representation: String) {
        error("Atomic is not directly usable")
    }
}

object AnyURIType : PrimitiveDatatype("anyURI", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType
    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema)
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: String): VAnyURI = VAnyURI(representation)

    override fun validateValue(representation: Any) {
        check(representation is VAnyURI)
    }

    override fun validate(representation: String) {
        VAnyURI(representation)
    }
}

@OptIn(ExperimentalEncodingApi::class)
object Base64BinaryType : PrimitiveDatatype("base64Binary", XmlSchemaConstants.XS_NAMESPACE) {
    fun length(representation: String): Int {
        // TODO don't actually decode just for length.
        return Base64.decode(representation).size
    }

    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: String): ByteArray {
        return Base64.decode(representation)
    }

    override fun validateValue(representation: Any) {
        check(representation is ByteArray)
    }

    override fun validate(representation: String) {
        value(representation)
    }
}

object BooleanType : PrimitiveDatatype("boolean", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.FINITE,
        numeric = false,
    )

    override fun value(representation: String): VBoolean = when (representation) {
        "true", "1" -> VBoolean.TRUE
        "false", "0" -> VBoolean.FALSE
        else -> error("$representation is not a boolean")
    }

    override fun validateValue(representation: Any) {
        check(representation is VBoolean)
    }

    override fun validate(representation: String) {
        value(representation)
    }
}

interface FiniteDateType : ResolvedBuiltinType

object DateType : PrimitiveDatatype("date", XmlSchemaConstants.XS_NAMESPACE), FiniteDateType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        explicitTimezone = ResolvedExplicitTimezone(
            XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL),
            BuiltinXmlSchema
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: String): VDate {
        val (year, month, day) = representation.split('Z').first().split('-').map { it.toInt() }
        return VDate(year, month, day)
    }

    override fun validateValue(representation: Any) {
        check(representation is VDate)
    }

    override fun validate(representation: String) {
        value(representation)
    }
}

object DateTimeType : PrimitiveDatatype("dateTime", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        explicitTimezone = ResolvedExplicitTimezone(
            XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL),
            BuiltinXmlSchema
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: String): Any {
        TODO("not implemented")
    }

    override fun validate(representation: String) {
        // TODO: validate date time
        // value(representation)
    }
}

object DateTimeStampType : PrimitiveDatatype("dateTimeStamp", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: DateTimeType get() = DateTimeType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        explicitTimezone = ResolvedExplicitTimezone(
            XSExplicitTimezone(XSExplicitTimezone.Value.REQUIRED, true),
            BuiltinXmlSchema
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: String): Any {
        TODO("not implemented")
    }

    override fun validate(representation: String) {
        //TODO("not implemented")
    }
}

object DecimalType : PrimitiveDatatype("decimal", XmlSchemaConstants.XS_NAMESPACE), IDecimalType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

    override fun value(representation: String): VDecimal {
        return when (representation.toLong()) {
            in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() ->
                VInteger(representation.toLong().toInt())

            else -> VInteger(representation.toLong())
        }
    }

    override fun validateValue(representation: Any) {
        check(representation is VDecimal)
    }

    override fun validate(representation: String) {
        value(representation)
    }
}

object IntegerType : PrimitiveDatatype("integer", XmlSchemaConstants.XS_NAMESPACE), IDecimalType {
    override val baseType: DecimalType get() = DecimalType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinXmlSchema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinXmlSchema)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

    override fun value(representation: String): VInteger {
        return VInteger(representation.toInt())
    }

    override fun validateValue(representation: Any) {
        check(representation is VInteger)
    }

    override fun validate(representation: String) {
        //TODO("not implemented")
    }
}

object LongType : PrimitiveDatatype("long", XmlSchemaConstants.XS_NAMESPACE), IDecimalType {
    override val baseType: IntegerType get() = IntegerType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinXmlSchema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinXmlSchema)),
        maxConstraint = ResolvedMaxInclusive(XSMaxInclusive(VInteger(Long.MAX_VALUE)), BuiltinXmlSchema),
        minConstraint = ResolvedMinInclusive(XSMinInclusive(VInteger(Long.MIN_VALUE)), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun value(representation: String): VInteger {
        return VInteger(representation.toLong())
    }

    override fun validateValue(representation: Any) {
        check(representation is VInteger)
    }

    override fun validate(representation: String) {
        //TODO("not implemented")
    }
}

object IntType : PrimitiveDatatype("int", XmlSchemaConstants.XS_NAMESPACE), IDecimalType {
    override val baseType: LongType get() = LongType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinXmlSchema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinXmlSchema)),
        maxConstraint = ResolvedMaxInclusive(XSMaxInclusive(VInteger(Int.MAX_VALUE)), BuiltinXmlSchema),
        minConstraint = ResolvedMinInclusive(XSMinInclusive(VInteger(Int.MIN_VALUE)), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun value(representation: String): VInteger {
        return VInteger(representation.toLong())
    }

    override fun validateValue(representation: Any) {
        check(representation is VInteger)
    }

    override fun validate(representation: String) {
        //TODO("not implemented")
    }

}

object ShortType : PrimitiveDatatype("short", XmlSchemaConstants.XS_NAMESPACE), IDecimalType {
    override val baseType: IntType get() = IntType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinXmlSchema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinXmlSchema)),
        maxConstraint = ResolvedMaxInclusive(XSMaxInclusive(VInteger(32767)), BuiltinXmlSchema),
        minConstraint = ResolvedMinInclusive(XSMinInclusive(VInteger(-32768)), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun value(representation: String): VInteger {
        return VInteger(representation.toInt())
    }

    override fun validateValue(representation: Any) {
        check(representation is VInteger)
    }

    override fun validate(representation: String) {
        //TODO("not implemented")
    }

}

object ByteType : PrimitiveDatatype("byte", XmlSchemaConstants.XS_NAMESPACE), IDecimalType {
    override val baseType: ShortType get() = ShortType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinXmlSchema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinXmlSchema)),
        maxConstraint = ResolvedMaxInclusive(XSMaxInclusive(VInteger(127)), BuiltinXmlSchema),
        minConstraint = ResolvedMinInclusive(XSMinInclusive(VInteger(-128)), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun value(representation: String): VInteger {
        return VInteger(representation.toInt())
    }

    override fun validateValue(representation: Any) {
        check(representation is VInteger)
    }

    override fun validate(representation: String) {
        //TODO("not implemented")
    }

}

object NonNegativeIntegerType : PrimitiveDatatype("nonNegativeInteger", XmlSchemaConstants.XS_NAMESPACE), IDecimalType {
    override val baseType: IntegerType get() = IntegerType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinXmlSchema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinXmlSchema)),
        minConstraint = ResolvedMinInclusive(XSMinInclusive(VInteger.ZERO), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

    override fun value(representation: String): VNonNegativeInteger {
        return VNonNegativeInteger(representation)
    }

    override fun validateValue(representation: Any) {
        check(representation is VNonNegativeInteger)
    }

    override fun validate(representation: String) {
        //TODO("not implemented")
    }

}

object PositiveIntegerType : PrimitiveDatatype("positiveInteger", XmlSchemaConstants.XS_NAMESPACE), IDecimalType {
    override val baseType: NonNegativeIntegerType get() = NonNegativeIntegerType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinXmlSchema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinXmlSchema)),
        minConstraint = ResolvedMinInclusive(XSMinInclusive(VInteger(1)), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

    override fun value(representation: String): VNonNegativeInteger {
        return VNonNegativeInteger(representation)
    }

    override fun validateValue(representation: Any) {
        check(representation is VNonNegativeInteger)
    }

    override fun validate(representation: String) {
        //TODO("not implemented")
    }

}

object UnsignedLongType : PrimitiveDatatype("unsignedLong", XmlSchemaConstants.XS_NAMESPACE), IDecimalType {
    override val baseType: NonNegativeIntegerType get() = NonNegativeIntegerType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinXmlSchema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinXmlSchema)),
        maxConstraint = ResolvedMaxInclusive(XSMaxInclusive(VUnsignedLong(ULong.MAX_VALUE)), BuiltinXmlSchema),
        minConstraint = ResolvedMinInclusive(XSMinInclusive(VInteger.ZERO), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun value(representation: String): VUnsignedLong {
        return VUnsignedLong(representation.toULong())
    }

    override fun validateValue(representation: Any) {
        check(representation is VUnsignedLong)
    }

    override fun validate(representation: String) {
        //TODO("not implemented")
    }

}

object UnsignedIntType : PrimitiveDatatype("unsignedInt", XmlSchemaConstants.XS_NAMESPACE), IDecimalType {
    override val baseType: UnsignedLongType get() = UnsignedLongType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinXmlSchema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinXmlSchema)),
        maxConstraint = ResolvedMaxInclusive(XSMaxInclusive(VUnsignedInt(UInt.MAX_VALUE)), BuiltinXmlSchema),
        minConstraint = ResolvedMinInclusive(XSMinInclusive(VInteger.ZERO), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun value(representation: String): VUnsignedInt {
        return VUnsignedInt(representation.toUInt())
    }

    override fun validateValue(representation: Any) {
        check(representation is VUnsignedInt)
    }

    override fun validate(representation: String) {
        //TODO("not implemented")
    }

}

object UnsignedShortType : PrimitiveDatatype("unsignedShort", XmlSchemaConstants.XS_NAMESPACE), IDecimalType {
    override val baseType: UnsignedIntType get() = UnsignedIntType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinXmlSchema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinXmlSchema)),
        maxConstraint = ResolvedMaxInclusive(XSMaxInclusive(VUnsignedInt(65535u)), BuiltinXmlSchema),
        minConstraint = ResolvedMinInclusive(XSMinInclusive(VInteger.ZERO), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun value(representation: String): VUnsignedInt {
        return VUnsignedInt(representation.toUInt())
    }

    override fun validateValue(representation: Any) {
        check(representation is VUnsignedInt)
    }

    override fun validate(representation: String) {
        //TODO("not implemented")
    }

}

object UnsignedByteType : PrimitiveDatatype("unsignedByte", XmlSchemaConstants.XS_NAMESPACE), IDecimalType {
    override val baseType: UnsignedShortType get() = UnsignedShortType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinXmlSchema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinXmlSchema)),
        maxConstraint = ResolvedMaxInclusive(XSMaxInclusive(VUnsignedInt(255u)), BuiltinXmlSchema),
        minConstraint = ResolvedMinInclusive(XSMinInclusive(VInteger.ZERO), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun value(representation: String): VUnsignedInt {
        return VUnsignedInt(representation.toUInt())
    }

    override fun validateValue(representation: Any) {
        check(representation is VUnsignedInt)
    }

    override fun validate(representation: String) {
        //TODO("not implemented")
    }

}

object NonPositiveIntegerType : PrimitiveDatatype("nonPositiveInteger", XmlSchemaConstants.XS_NAMESPACE), IDecimalType {
    override val baseType: IntegerType get() = IntegerType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinXmlSchema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinXmlSchema)),
        maxConstraint = ResolvedMaxInclusive(XSMaxInclusive(VInteger.ZERO), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

    override fun value(representation: String): VDecimal {
        return when (representation.toLong()) {
            in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() ->
                VInteger(representation.toLong().toInt())

            else -> VInteger(representation.toLong())
        }
    }

    override fun validateValue(representation: Any) {
        check(representation is VDecimal)
    }

    override fun validate(representation: String) {
        check(value(representation).toLong() <= 0L)
    }

}

object NegativeIntegerType : PrimitiveDatatype("negativeInteger", XmlSchemaConstants.XS_NAMESPACE), IDecimalType {
    override val baseType: NonPositiveIntegerType get() = NonPositiveIntegerType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinXmlSchema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinXmlSchema)),
        maxConstraint = ResolvedMaxInclusive(XSMaxInclusive(VInteger(-1)), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

    override fun value(representation: String): VDecimal {
        return when (representation.toLong()) {
            in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() ->
                VInteger(representation.toLong().toInt())

            else -> VInteger(representation.toLong())
        }
    }

    override fun validateValue(representation: Any) {
        check(representation is VDecimal)
    }

    override fun validate(representation: String) {
        check(value(representation).toLong() < 0L)
    }

}

object DoubleType : PrimitiveDatatype("double", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun value(representation: String): VDouble {
        return VDouble(representation.toDouble())
    }

    override fun validateValue(representation: Any) {
        check(representation is VDouble)
    }

    override fun validate(representation: String) {
        value(representation)
    }

}

object DurationType : PrimitiveDatatype("duration", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: String): Any {
        TODO("not implemented")
    }

    override fun validate(representation: String) {
//        TODO("not implemented")
    }
}

object DayTimeDurationType : PrimitiveDatatype("dayTimeDuration", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: DurationType get() = DurationType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        patterns = listOf(ResolvedPattern(XSPattern("[^YM]*(T.*)?"), BuiltinXmlSchema)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: String): Any {
        TODO("not implemented")
    }

    override fun validate(representation: String) {
//        TODO("not implemented")
    }
}

object YearMonthDurationType : PrimitiveDatatype("yearMonthDuration", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: DurationType get() = DurationType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        patterns = listOf(ResolvedPattern(XSPattern("[^DT]*"), BuiltinXmlSchema)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: String): VGYearMonth {
        val (year, month) = representation.split('-').map { it.toInt() }
        return VGYearMonth(year, month)
    }

    override fun validateValue(representation: Any) {
        check(representation is VGYearMonth)
    }

    override fun validate(representation: String) {
        TODO("not implemented")
    }
}

object FloatType : PrimitiveDatatype("float", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun value(representation: String): VFloat {
        return VFloat(representation.toFloat())
    }

    override fun validateValue(representation: Any) {
        check(representation is VFloat)
    }

    override fun validate(representation: String) {
        value(representation)
    }

}

object GDayType : PrimitiveDatatype("gDay", XmlSchemaConstants.XS_NAMESPACE), FiniteDateType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        explicitTimezone = ResolvedExplicitTimezone(
            XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL),
            BuiltinXmlSchema
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: String): VGDay {
        return VGDay(representation.toInt())
    }

    override fun validateValue(representation: Any) {
        check(representation is VGDay)
    }

    override fun validate(representation: String) {
        value(representation)
    }
}

object GMonthType : PrimitiveDatatype("gMonth", XmlSchemaConstants.XS_NAMESPACE), FiniteDateType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        explicitTimezone = ResolvedExplicitTimezone(
            XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL),
            BuiltinXmlSchema
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: String): VGMonth {
        return VGMonth(representation.toInt())
    }

    override fun validateValue(representation: Any) {
        check(representation is VGMonth)
    }

    override fun validate(representation: String) {
        value(representation)
    }
}

object GMonthDayType : PrimitiveDatatype("gMonthDay", XmlSchemaConstants.XS_NAMESPACE), FiniteDateType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        explicitTimezone = ResolvedExplicitTimezone(
            XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL),
            BuiltinXmlSchema
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: String): VGMonthDay {
        val (month, day) = representation.split('-').map { it.toInt() }
        return VGMonthDay(month, day)
    }

    override fun validateValue(representation: Any) {
        check(representation is VGMonthDay)
    }

    override fun validate(representation: String) {
        TODO("not implemented")
    }
}

object GYearType : PrimitiveDatatype("gYear", XmlSchemaConstants.XS_NAMESPACE), FiniteDateType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        explicitTimezone = ResolvedExplicitTimezone(
            XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL),
            BuiltinXmlSchema
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: String): VGYear {
        TODO("not implemented")
    }

    override fun validateValue(representation: Any) {
        check(representation is VGYear)
    }

    override fun validate(representation: String) {
        TODO("not implemented")
    }
}

object GYearMonthType : PrimitiveDatatype("gYearMonth", XmlSchemaConstants.XS_NAMESPACE), FiniteDateType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        explicitTimezone = ResolvedExplicitTimezone(
            XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL),
            BuiltinXmlSchema
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: String): VGYearMonth {
        val (year, month) = representation.split('-').map { it.toInt() }
        return VGYearMonth(year, month)
    }

    override fun validateValue(representation: Any) {
        check(representation is VGYearMonth)
    }

    override fun validate(representation: String) {
        value(representation)
    }

}

object HexBinaryType : PrimitiveDatatype("hexBinary", XmlSchemaConstants.XS_NAMESPACE) {
    fun length(representation: String): Int {
        var acc = 0
        for (c in representation) {
            when {
                c in '0'..'9' -> acc++
                c in 'A'..'F' -> acc++
                c in 'a'..'b' -> acc++
                c == ' ' || c == '\t' || c == '\n' || c == '\r' -> {}
                else -> error("Unexpected character $c in hex binary value")
            }
        }
        return acc
    }

    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: String): ByteArray {
        require(representation.length %2 ==0) { "Hex must have even amount of characters" }
        return ByteArray(representation.length/2) { representation.substring(it*2, it*2+2).toInt(16).toByte() }
    }

    override fun validateValue(representation: Any) {
        check(representation is ByteArray)
    }

    override fun validate(representation: String) {
        TODO("not implemented")
    }
}

object NotationType : PrimitiveDatatype("NOTATION", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: String): VNotation {
        return VNotation(representation)
    }

    override fun validateValue(representation: Any) {
        check(representation is VNotation)
    }

    override fun validate(representation: String) {

    }
}

object QNameType : PrimitiveDatatype("QName", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: String): QName {
        error("QNames cannot be represented by string")
    }

    override fun validateValue(representation: Any) {
        check(representation is QName)
    }

    override fun validate(representation: String) {
        error("QNames cannot be represented by string")
    }
}

object StringType : PrimitiveDatatype("string", XmlSchemaConstants.XS_NAMESPACE), IStringType {
    override val baseType: AnyAtomicType get() = AnyAtomicType
    override val simpleDerivation: ResolvedSimpleRestrictionBase
        get() = SimpleBuiltinRestriction(baseType, listOf(XSWhiteSpace(XSWhiteSpace.Values.PRESERVE, fixed = false)))

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.PRESERVE), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: String): VString {
        return VString(representation)
    }

    override fun validateValue(representation: Any) {
        check(representation is VString)
    }

    override fun validate(representation: String) {}
}

object NormalizedStringType : PrimitiveDatatype("normalizedString", XmlSchemaConstants.XS_NAMESPACE), IStringType {
    override val baseType: StringType get() = StringType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.REPLACE), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: String): VNormalizedString {
        return VNormalizedString(representation)
    }

    override fun validateValue(representation: Any) {
        check(representation is VNormalizedString)
    }

    override fun validate(representation: String) {
        // TODO("not implemented")
    }
}

object TokenType : PrimitiveDatatype("token", XmlSchemaConstants.XS_NAMESPACE), IStringType {
    override val baseType: NormalizedStringType get() = NormalizedStringType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: String): VToken {
        return VToken(representation)
    }

    override fun validateValue(representation: Any) {
        check(representation is VToken)
    }

    override fun validate(representation: String) {
//        TODO("not implemented")
    }
}

object LanguageType : PrimitiveDatatype("language", XmlSchemaConstants.XS_NAMESPACE), IStringType {
    override val baseType: TokenType get() = TokenType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE), BuiltinXmlSchema),
        patterns = listOf(ResolvedPattern(XSPattern("[a-zA-Z]{1,8}(-[a-zA-Z0-9]{1,8})*"), BuiltinXmlSchema)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: String): VString {
        TODO("not implemented")
    }

    override fun validate(representation: String) {
//        TODO("not implemented")
    }
}

object NameType : PrimitiveDatatype("Name", XmlSchemaConstants.XS_NAMESPACE), IStringType {
    override val baseType: TokenType get() = TokenType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE), BuiltinXmlSchema),
        patterns = listOf(ResolvedPattern(XSPattern("\\i\\c*"), BuiltinXmlSchema)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: String): VName {
        return VName(representation)
    }

    override fun validateValue(representation: Any) {
        check(representation is VName)
    }

    override fun validate(representation: String) {
        value(representation)
    }
}

object NCNameType : PrimitiveDatatype("NCName", XmlSchemaConstants.XS_NAMESPACE), IStringType {
    override val baseType: NameType get() = NameType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE), BuiltinXmlSchema),
        patterns = listOf(
            ResolvedPattern(XSPattern("\\i\\c*"), BuiltinXmlSchema),
            ResolvedPattern(XSPattern("[\\i-[:]][\\c-[:]]*"), BuiltinXmlSchema)
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: String): VNCName {
        return VNCName(representation)
    }

    override fun validateValue(representation: Any) {
        check(representation is VNCName)
    }

    override fun validate(representation: String) {
        value(representation)
    }
}

object EntityType : PrimitiveDatatype("ENTITY", XmlSchemaConstants.XS_NAMESPACE), IStringType {
    override val baseType: NCNameType get() = NCNameType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE), BuiltinXmlSchema),
        patterns = listOf(
            ResolvedPattern(XSPattern("\\i\\c*"), BuiltinXmlSchema),
            ResolvedPattern(XSPattern("[\\i-[:]][\\c-[:]]*"), BuiltinXmlSchema)
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: String): VString {
        TODO("not implemented")
    }

    override fun validate(representation: String) {
//        TODO("not implemented")
    }
}

object IDType : PrimitiveDatatype("ID", XmlSchemaConstants.XS_NAMESPACE), IStringType {
    override val baseType: NCNameType get() = NCNameType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE), BuiltinXmlSchema),
        patterns = listOf(
            ResolvedPattern(XSPattern("\\i\\c*"), BuiltinXmlSchema),
            ResolvedPattern(XSPattern("[\\i-[:]][\\c-[:]]*"), BuiltinXmlSchema)
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: String): VID {
        return VID(representation)
    }

    override fun validateValue(representation: Any) {
        check(representation is VID)
    }

    override fun validate(representation: String) {
        value(representation)
    }
}

object IDRefType : PrimitiveDatatype("IDREF", XmlSchemaConstants.XS_NAMESPACE), IStringType {
    override val baseType: NCNameType get() = NCNameType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE), BuiltinXmlSchema),
        patterns = listOf(
            ResolvedPattern(XSPattern("\\i\\c*"), BuiltinXmlSchema),
            ResolvedPattern(XSPattern("[\\i-[:]][\\c-[:]]*"), BuiltinXmlSchema)
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: String): VIDRef {
        return VIDRef(representation)
    }

    override fun validateValue(representation: Any) {
        check(representation is VIDRef)
    }

    override fun validate(representation: String) {
        value(representation)
    }
}

object NMTokenType : PrimitiveDatatype("NMTOKEN", XmlSchemaConstants.XS_NAMESPACE), IStringType {
    override val baseType: TokenType get() = TokenType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE), BuiltinXmlSchema),
        patterns = listOf(ResolvedPattern(XSPattern("\\c+"), BuiltinXmlSchema)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: String): VNMToken {
        return VNMToken(representation)
    }

    override fun validateValue(representation: Any) {
        check(representation is VNMToken)
    }

    override fun validate(representation: String) {
        value(representation)
    }
}

object TimeType : PrimitiveDatatype("time", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        explicitTimezone = ResolvedExplicitTimezone(
            XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL),
            BuiltinXmlSchema
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: String): Any {
        TODO("not implemented")
    }

    override fun validate(representation: String) {
//        TODO("not implemented")
    }
}

object EntitiesType :
    ConstructedListDatatype("ENTITIES", XmlSchemaConstants.XS_NAMESPACE, EntityType, BuiltinXmlSchema) {
    override val mdlItemTypeDefinition: ResolvedSimpleType
        get() = EntityType

    override fun validateValue(representation: Any) {
        check(representation is List<*>)
    }

    override fun validate(representation: String) {}
}

object IDRefsType : ConstructedListDatatype("IDREFS", XmlSchemaConstants.XS_NAMESPACE, EntityType, BuiltinXmlSchema) {
    override val mdlItemTypeDefinition: ResolvedSimpleType
        get() = IDRefType

    override fun validateValue(representation: Any) {
        check(representation is List<*>)
    }

    override fun validate(representation: String) {}
}

object NMTokensType :
    ConstructedListDatatype("NMTOKENS", XmlSchemaConstants.XS_NAMESPACE, EntityType, BuiltinXmlSchema) {
    override val mdlItemTypeDefinition: ResolvedSimpleType
        get() = NMTokenType

    override fun validateValue(representation: Any) {
        check(representation is List<*>)
    }

    override fun validate(representation: String) {}
}

object PrecisionDecimalType : PrimitiveDatatype("precisionDecimal", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

    override fun value(representation: String): Any {
        TODO("NOT IMPLEMENTED")
    }

    override fun validate(representation: String) {
//        TODO("not implemented")
    }
}
