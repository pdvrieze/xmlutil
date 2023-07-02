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
import io.github.pdvrieze.formats.xmlschema.types.CardinalityFacet.Cardinality
import io.github.pdvrieze.formats.xmlschema.types.FundamentalFacets
import io.github.pdvrieze.formats.xmlschema.types.OrderedFacet.Order
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

sealed class AtomicDatatype(name: String, targetNamespace: String) : Datatype(name, targetNamespace),
    ResolvedBuiltinSimpleType {

    override val name: VNCName get() = super<Datatype>.name
    override val targetNamespace: VAnyURI
        get() = super<Datatype>.targetNamespace

    override val model: AtomicDatatype get() = this

    abstract override val mdlBaseTypeDefinition: ResolvedBuiltinType
    abstract override val mdlFacets: List<ResolvedFacet>
    abstract override val mdlFundamentalFacets: FundamentalFacets
    override val mdlVariety: SimpleTypeModel.Variety get() = SimpleTypeModel.Variety.ATOMIC
    override val mdlPrimitiveTypeDefinition: PrimitiveDatatype? get() = null

    final override val mdlItemTypeDefinition: ResolvedSimpleType? get() = null
    final override val mdlMemberTypeDefinitions: List<ResolvedSimpleType> get() = emptyList()

    final override val mdlFinal: Set<TypeModel.Derivation> get() = emptySet()
}

sealed class PrimitiveDatatype(name: String, targetNamespace: String) : AtomicDatatype(name, targetNamespace) {
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

    override val mdlFacets: List<Nothing> get() = emptyList()
    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )
}

object AnyURIType : PrimitiveDatatype("anyURI", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema)
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

}

object Base64BinaryType : PrimitiveDatatype("base64Binary", XmlSchemaConstants.XS_NAMESPACE) {
    @OptIn(ExperimentalEncodingApi::class)
    fun length(representation: String): Int {
        // TODO don't actually decode just for length.
        return Base64.decode(representation).size
    }

    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

}

object BooleanType : PrimitiveDatatype("boolean", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.FINITE,
        numeric = false,
    )

}

interface FiniteDateType: ResolvedBuiltinType

object DateType : PrimitiveDatatype("date", XmlSchemaConstants.XS_NAMESPACE), FiniteDateType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        ResolvedExplicitTimezone(XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

}

object DateTimeType : PrimitiveDatatype("dateTime", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        ResolvedExplicitTimezone(XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

}

object DateTimeStampType : PrimitiveDatatype("dateTimeStamp", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: DateTimeType get() = DateTimeType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        ResolvedExplicitTimezone(XSExplicitTimezone(XSExplicitTimezone.Value.REQUIRED, true), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )


}

object DecimalType : PrimitiveDatatype("decimal", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

}

object IntegerType : PrimitiveDatatype("integer", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: DecimalType get() = DecimalType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        ResolvedFractionDigits(XSFractionDigits(0u), BuiltinXmlSchema),
        ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

}

object LongType : PrimitiveDatatype("long", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: IntegerType get() = IntegerType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        ResolvedFractionDigits(XSFractionDigits(0u), BuiltinXmlSchema),
        ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinXmlSchema),
        ResolvedMaxInclusive(XSMaxInclusive(VInteger(Long.MAX_VALUE)), BuiltinXmlSchema),
        ResolvedMinInclusive(XSMinInclusive(VInteger(Long.MIN_VALUE)), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

}

object IntType : PrimitiveDatatype("int", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: LongType get() = LongType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        ResolvedFractionDigits(XSFractionDigits(0u), BuiltinXmlSchema),
        ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinXmlSchema),
        ResolvedMaxInclusive(XSMaxInclusive(VInteger(Int.MAX_VALUE)), BuiltinXmlSchema),
        ResolvedMinInclusive(XSMinInclusive(VInteger(Int.MIN_VALUE)), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

}

object ShortType : PrimitiveDatatype("short", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: IntType get() = IntType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        ResolvedFractionDigits(XSFractionDigits(0u), BuiltinXmlSchema),
        ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinXmlSchema),
        ResolvedMaxInclusive(XSMaxInclusive(VInteger(32767)), BuiltinXmlSchema),
        ResolvedMinInclusive(XSMinInclusive(VInteger(-32768)), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

}

object ByteType : PrimitiveDatatype("byte", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: ShortType get() = ShortType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        ResolvedFractionDigits(XSFractionDigits(0u), BuiltinXmlSchema),
        ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinXmlSchema),
        ResolvedMaxInclusive(XSMaxInclusive(VInteger(127)), BuiltinXmlSchema),
        ResolvedMinInclusive(XSMinInclusive(VInteger(-128)), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

}

object NonNegativeIntegerType : PrimitiveDatatype("nonNegativeInteger", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: IntegerType get() = IntegerType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        ResolvedFractionDigits(XSFractionDigits(0u), BuiltinXmlSchema),
        ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinXmlSchema),
        ResolvedMinInclusive(XSMinInclusive(VInteger.ZERO), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

}

object PositiveIntegerType : PrimitiveDatatype("positiveInteger", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: NonNegativeIntegerType get() = NonNegativeIntegerType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        ResolvedFractionDigits(XSFractionDigits(0u), BuiltinXmlSchema),
        ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinXmlSchema),
        ResolvedMinInclusive(XSMinInclusive(VInteger(1)), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

}

object UnsignedLongType : PrimitiveDatatype("unsignedLong", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: NonNegativeIntegerType get() = NonNegativeIntegerType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        ResolvedFractionDigits(XSFractionDigits(0u), BuiltinXmlSchema),
        ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinXmlSchema),
        ResolvedMaxInclusive(XSMaxInclusive(VUnsignedLong(ULong.MAX_VALUE)), BuiltinXmlSchema),
        ResolvedMinInclusive(XSMinInclusive(VInteger.ZERO), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

}

object UnsignedIntType : PrimitiveDatatype("unsignedInt", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: UnsignedLongType get() = UnsignedLongType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        ResolvedFractionDigits(XSFractionDigits(0u), BuiltinXmlSchema),
        ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinXmlSchema),
        ResolvedMaxInclusive(XSMaxInclusive(VUnsignedInt(UInt.MAX_VALUE)), BuiltinXmlSchema),
        ResolvedMinInclusive(XSMinInclusive(VInteger.ZERO), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

}

object UnsignedShortType : PrimitiveDatatype("unsignedShort", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: UnsignedIntType get() = UnsignedIntType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        ResolvedFractionDigits(XSFractionDigits(0u), BuiltinXmlSchema),
        ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinXmlSchema),
        ResolvedMaxInclusive(XSMaxInclusive(VUnsignedInt(65535u)), BuiltinXmlSchema),
        ResolvedMinInclusive(XSMinInclusive(VInteger.ZERO), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

}

object UnsignedByteType : PrimitiveDatatype("unsignedByte", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: UnsignedShortType get() = UnsignedShortType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        ResolvedFractionDigits(XSFractionDigits(0u), BuiltinXmlSchema),
        ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinXmlSchema),
        ResolvedMaxInclusive(XSMaxInclusive(VUnsignedInt(255u)), BuiltinXmlSchema),
        ResolvedMinInclusive(XSMinInclusive(VInteger.ZERO), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

}

object NonPositiveIntegerType : PrimitiveDatatype("nonPositiveInteger", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: IntegerType get() = IntegerType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        ResolvedFractionDigits(XSFractionDigits(0u), BuiltinXmlSchema),
        ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinXmlSchema),
        ResolvedMaxInclusive(XSMaxInclusive(VInteger.ZERO), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

}

object NegativeIntegerType : PrimitiveDatatype("negativeInteger", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: NonPositiveIntegerType get() = NonPositiveIntegerType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        ResolvedFractionDigits(XSFractionDigits(0u), BuiltinXmlSchema),
        ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinXmlSchema),
        ResolvedMaxInclusive(XSMaxInclusive(VInteger(-1)), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

}

object DoubleType : PrimitiveDatatype("double", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

}

object DurationType : PrimitiveDatatype("duration", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

}

object DayTimeDurationType : PrimitiveDatatype("dayTimeDuration", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: DurationType get() = DurationType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        ResolvedPattern(XSPattern("[^YM]*(T.*)?"), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )
}

object YearMonthDurationType : PrimitiveDatatype("yearMonthDuration", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: DurationType get() = DurationType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        ResolvedPattern(XSPattern("[^DT]*"), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

}

object FloatType : PrimitiveDatatype("float", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

}

object GDayType : PrimitiveDatatype("gDay", XmlSchemaConstants.XS_NAMESPACE), FiniteDateType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        ResolvedExplicitTimezone(XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

}

object GMonthType : PrimitiveDatatype("gMonth", XmlSchemaConstants.XS_NAMESPACE), FiniteDateType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        ResolvedExplicitTimezone(XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

}

object GMonthDayType : PrimitiveDatatype("gMonthDay", XmlSchemaConstants.XS_NAMESPACE), FiniteDateType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        ResolvedExplicitTimezone(XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

}

object GYearType : PrimitiveDatatype("gYear", XmlSchemaConstants.XS_NAMESPACE), FiniteDateType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        ResolvedExplicitTimezone(XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

}

object GYearMonthType : PrimitiveDatatype("gYearMonth", XmlSchemaConstants.XS_NAMESPACE), FiniteDateType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        ResolvedExplicitTimezone(XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

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

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

}

object NotationType : PrimitiveDatatype("NOTATION", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

}

object QNameType : PrimitiveDatatype("QName", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

}

object StringType : PrimitiveDatatype("string", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType
    override val simpleDerivation: ResolvedSimpleRestrictionBase
        get() = SimpleBuiltinRestriction(baseType, listOf(XSWhiteSpace(XSWhiteSpace.Values.PRESERVE, fixed = false)))

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.PRESERVE), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

}

object NormalizedStringType : PrimitiveDatatype("normalizedString", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: StringType get() = StringType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.REPLACE), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

}

object TokenType : PrimitiveDatatype("token", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: NormalizedStringType get() = NormalizedStringType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

}

object LanguageType : PrimitiveDatatype("language", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: TokenType get() = TokenType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE), BuiltinXmlSchema),
        ResolvedPattern(XSPattern("[a-zA-Z]{1,8}(-[a-zA-Z0-9]{1,8})*"), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

}

object NameType : PrimitiveDatatype("Name", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: TokenType get() = TokenType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE), BuiltinXmlSchema),
        ResolvedPattern(XSPattern("\\i\\c*"), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

}

object NCNameType : PrimitiveDatatype("NCName", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: NameType get() = NameType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE), BuiltinXmlSchema),
        ResolvedPattern(XSPattern("\\i\\c*"), BuiltinXmlSchema),
        ResolvedPattern(XSPattern("[\\i-[:]][\\c-[:]]*"), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

}

object EntityType : PrimitiveDatatype("ENTITY", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: NCNameType get() = NCNameType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE), BuiltinXmlSchema),
        ResolvedPattern(XSPattern("\\i\\c*"), BuiltinXmlSchema),
        ResolvedPattern(XSPattern("[\\i-[:]][\\c-[:]]*"), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

}

object IDType : PrimitiveDatatype("ID", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: NCNameType get() = NCNameType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE), BuiltinXmlSchema),
        ResolvedPattern(XSPattern("\\i\\c*"), BuiltinXmlSchema),
        ResolvedPattern(XSPattern("[\\i-[:]][\\c-[:]]*"), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

}

object IDRefType : PrimitiveDatatype("IDREF", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: NCNameType get() = NCNameType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE), BuiltinXmlSchema),
        ResolvedPattern(XSPattern("\\i\\c*"), BuiltinXmlSchema),
        ResolvedPattern(XSPattern("[\\i-[:]][\\c-[:]]*"), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

}

object NMTokenType : PrimitiveDatatype("NMTOKEN", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: TokenType get() = TokenType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE), BuiltinXmlSchema),
        ResolvedPattern(XSPattern("\\c+"), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

}

object TimeType : PrimitiveDatatype("time", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
        ResolvedExplicitTimezone(XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

}

object EntitiesType : ConstructedListDatatype("ENTITIES", XmlSchemaConstants.XS_NAMESPACE, EntityType, BuiltinXmlSchema) {
    override val mdlItemTypeDefinition: ResolvedSimpleType
        get() = EntityType
}

object IDRefsType : ConstructedListDatatype("IDREFS", XmlSchemaConstants.XS_NAMESPACE, EntityType, BuiltinXmlSchema) {
    override val mdlItemTypeDefinition: ResolvedSimpleType
        get() = IDRefType
}

object NMTokensType : ConstructedListDatatype("NMTOKENS", XmlSchemaConstants.XS_NAMESPACE, EntityType, BuiltinXmlSchema) {
    override val mdlItemTypeDefinition: ResolvedSimpleType
        get() = NMTokenType


}

object PrecisionDecimalType : PrimitiveDatatype("precisionDecimal", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: List<ResolvedFacet> = listOf(
        ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinXmlSchema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

}
