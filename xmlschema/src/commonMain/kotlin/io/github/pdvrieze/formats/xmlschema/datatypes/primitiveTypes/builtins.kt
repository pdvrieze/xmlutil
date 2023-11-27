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

package io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes

import io.github.pdvrieze.formats.xmlschema.datatypes.*
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.*
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSExplicitTimezone
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSFractionDigits
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSPattern
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSWhiteSpace
import io.github.pdvrieze.formats.xmlschema.impl.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.regex.XRegex
import io.github.pdvrieze.formats.xmlschema.resolved.*
import io.github.pdvrieze.formats.xmlschema.resolved.facets.*
import io.github.pdvrieze.formats.xmlschema.types.CardinalityFacet.Cardinality
import io.github.pdvrieze.formats.xmlschema.types.FundamentalFacets
import io.github.pdvrieze.formats.xmlschema.types.OrderedFacet.Order
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.xmlCollapseWhitespace
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

fun builtinType(localName: String, targetNamespace: String): ResolvedBuiltinType? {
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

sealed interface IStringType : ResolvedBuiltinSimpleType {
    override fun value(representation: VString): VString
}

sealed interface IDecimalType : ResolvedBuiltinSimpleType {
    override fun value(representation: VString): VDecimal
}

sealed class AtomicDatatype(name: String, targetNamespace: String) :
    Datatype(name, targetNamespace, BuiltinSchemaXmlschema),
    ResolvedBuiltinSimpleType, ResolvedSimpleType.Model {

    override val model: AtomicDatatype get() = this

    abstract override val mdlBaseTypeDefinition: ResolvedBuiltinType
    abstract override val mdlFacets: FacetList
    abstract override val mdlFundamentalFacets: FundamentalFacets
    override val mdlVariety: ResolvedSimpleType.Variety get() = ResolvedSimpleType.Variety.ATOMIC
    override val mdlPrimitiveTypeDefinition: PrimitiveDatatype<*>? get() = null

    final override val mdlItemTypeDefinition: ResolvedSimpleType? get() = null
    final override val mdlMemberTypeDefinitions: List<ResolvedSimpleType> get() = emptyList()

    final override val mdlFinal: Set<VDerivationControl.Type> get() = emptySet()

    override fun toString(): String = "Builtin:${mdlQName.localPart}"

}

typealias AnyPrimitiveDatatype = PrimitiveDatatype<*>

sealed class PrimitiveDatatype<out T: VAnySimpleType>(name: String, targetNamespace: String) : AtomicDatatype(name, targetNamespace) {
    final override val isSpecial: Boolean get() = false

    final override fun value(representation: VString): T {
        val normalized = mdlFacets.whiteSpace?.value?.normalize(representation) ?: representation
        return valueFromNormalized(normalized)
    }

    override fun validateValue(value: Any) {
        mdlFacets.validateValue(value)
    }

    protected abstract fun valueFromNormalized(normalized: VString): T

    abstract fun value(maybeValue: VAnySimpleType): VAnySimpleType

    abstract override val baseType: ResolvedBuiltinSimpleType
    override val simpleDerivation: ResolvedSimpleRestrictionBase
        get() = SimpleBuiltinRestriction(baseType, schema = BuiltinSchemaXmlschema)

    final override val mdlBaseTypeDefinition: ResolvedBuiltinType get() = baseType
    override val mdlPrimitiveTypeDefinition: PrimitiveDatatype<T>? get() = this
}

object AnyAtomicType : AtomicDatatype("anyAtomicType", XmlSchemaConstants.XS_NAMESPACE) {
    override val isSpecial: Boolean get() = true
    override val baseType: AnySimpleType get() = AnySimpleType
    override val simpleDerivation: ResolvedSimpleRestrictionBase =
        SimpleBuiltinRestriction(AnySimpleType, schema = BuiltinSchemaXmlschema)

    override val mdlBaseTypeDefinition: AnySimpleType get() = baseType

    override val mdlFacets: FacetList get() = FacetList.EMPTY

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun validateValue(value: Any) {
        error("Atomic is not directly usable")
    }

    override fun validate(representation: VString) {
        error("Atomic is not directly usable")
    }
}

object AnyURIType : PrimitiveDatatype<VAnyURI>("anyURI", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType
    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema)
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(representation: VString): VAnyURI = VAnyURI(representation)

    override fun value(maybeValue: VAnySimpleType): VAnyURI {
        return maybeValue as? VAnyURI ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VAnyURI)
        mdlFacets.validate(mdlPrimitiveTypeDefinition, VString(value.value))
    }

    override fun validate(representation: VString) {
        mdlFacets.validate(mdlPrimitiveTypeDefinition, representation)
    }
}

@OptIn(ExperimentalEncodingApi::class)
object Base64BinaryType : PrimitiveDatatype<VByteArray>("base64Binary", XmlSchemaConstants.XS_NAMESPACE) {
    fun length(representation: String): Int {
        // TODO don't actually decode just for length.
        return Base64.decode(representation).size
    }

    val regex = XRegex("((([A-Za-z0-9+/] ?){4})*(([A-Za-z0-9+/] ?){3}[A-Za-z0-9+/]|([A-Za-z0-9+/] ?){2}[AEIMQUYcgkosw048] ?=|[A-Za-z0-9+/] ?[AQgw] ?= ?=))?", ResolvedSchema.Version.V1_0)

    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: VString): VByteArray {
        check(regex.matches(normalized))
        return VByteArray(Base64.decode(normalized))
    }

    override fun value(maybeValue: VAnySimpleType): VByteArray {
        return maybeValue as? VByteArray ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VByteArray)
    }

    override fun validate(representation: VString) {
        value(representation)
    }
}

object BooleanType : PrimitiveDatatype<VBoolean>("boolean", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.FINITE,
        numeric = false,
    )

    override fun valueFromNormalized(representation: VString): VBoolean = when (representation.toString()) {
        "true", "1" -> VBoolean.TRUE
        "false", "0" -> VBoolean.FALSE
        else -> error("$representation is not a boolean")
    }

    override fun value(maybeValue: VAnySimpleType): VBoolean {
        return maybeValue as? VBoolean ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VBoolean)
    }

    override fun validate(representation: VString) {
        value(representation)
    }
}

interface FiniteDateType : ResolvedBuiltinSimpleType

object DateType : PrimitiveDatatype<VDate>("date", XmlSchemaConstants.XS_NAMESPACE), FiniteDateType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
        explicitTimezone = ResolvedExplicitTimezone(
            XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL),
            BuiltinSchemaXmlschema
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(representation: VString): VDate {
        val s = representation.xmlString
        val monthIdx = s.indexOf('-', 1) // sign can be start
        val year = s.substring(0, monthIdx).toInt()
        val month = s.substring(monthIdx + 1, monthIdx + 3).toInt()
        if (s[monthIdx + 3] != '-') throw NumberFormatException("Missing - between month and day")
        val day = s.substring(monthIdx + 4, monthIdx + 6).toInt()

        return when {
            representation.length >= monthIdx + 6 ->
                VDate(year, month, day, IDateTime.timezoneFragValue(s.substring(monthIdx + 6)))

            else ->
                VDate(year, month, day)
        }
    }

    override fun value(maybeValue: VAnySimpleType): VDate {
        return maybeValue as? VDate ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VDate)
    }

    override fun validate(representation: VString) {
        value(representation)
    }
}

object DateTimeType : PrimitiveDatatype<VDateTime>("dateTime", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
        explicitTimezone = ResolvedExplicitTimezone(
            XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL),
            BuiltinSchemaXmlschema
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(representation: VString): VDateTime {
        val s = representation.xmlString
        val tIndex = s.indexOf('T')

        require(tIndex >= 0)
        val (year, month, day) = s.substring(0, tIndex).split('-').map { it.toInt() }
        val hour = s.substring(tIndex + 1, tIndex + 3).toUInt()
        if (s[tIndex + 3] != ':') throw NumberFormatException("Missing : separtor between hours and minutes")
        val minutes = s.substring(tIndex + 4, tIndex + 6).toUInt()
        if (s[tIndex + 6] != ':') throw NumberFormatException("Missing : separtor between minutes and seconds")
        val secEnd = ((tIndex + 7)..<s.length).firstOrNull {
            s[it] != '.' && s[it] !in '0'..'9'
        }
        val seconds = DecimalType.value(VString(s.substring(tIndex + 7, secEnd ?: s.length)))
        return when (secEnd) {
            null -> VDateTime(year, month.toUInt(), day.toUInt(), hour, minutes, seconds)
            else -> {
                val timezoneOffset = IDateTime.timezoneFragValue(s.substring(secEnd))
                VDateTime(year, month.toUInt(), day.toUInt(), hour, minutes, seconds, timezoneOffset)
            }
        }
    }

    override fun value(maybeValue: VAnySimpleType): VDateTime {
        return maybeValue as? VDateTime ?: value(VString(maybeValue.xmlString))
    }

    override fun validate(representation: VString) {
        validateValue(value(representation))
    }
}

object DateTimeStampType : PrimitiveDatatype<VDateTime>("dateTimeStamp", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: DateTimeType get() = DateTimeType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
        explicitTimezone = ResolvedExplicitTimezone(
            XSExplicitTimezone(XSExplicitTimezone.Value.REQUIRED, true),
            BuiltinSchemaXmlschema
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(representation: VString): VDateTime {
        return DateTimeType.value(representation).also {
            requireNotNull(it.timezoneOffset) { "DateTimestamps must have a timestamp" }
        }
    }

    override fun value(maybeValue: VAnySimpleType): VDateTime {
        return (maybeValue as? VDateTime)?.also {
            requireNotNull(it.timezoneOffset) { "DateTimestamps must have a timestamp" }
        } ?: value(VString(maybeValue.xmlString))
    }

    override fun validate(representation: VString) {
        validateValue(value(representation))
    }
}

object DecimalType : PrimitiveDatatype<VDecimal>("decimal", XmlSchemaConstants.XS_NAMESPACE), IDecimalType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

    override fun valueFromNormalized(representation: VString): VDecimal {
        return try {
            when (representation.toLong()) {
                in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() ->
                    VInteger(representation.toLong().toInt())

                else -> VInteger(representation.toLong())
            }
        } catch (e: NumberFormatException) {
            VBigDecimalImpl(xmlCollapseWhitespace(representation.xmlString))
        }
    }

    override fun value(maybeValue: VAnySimpleType): VDecimal {
        return maybeValue as? VDecimal ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VDecimal)
    }

    override fun validate(representation: VString) {
        validateValue(value(representation))
    }

}

sealed interface IIntegerType : IDecimalType

object IntegerType : PrimitiveDatatype<VInteger>("integer", XmlSchemaConstants.XS_NAMESPACE), IIntegerType {
    override val baseType: DecimalType get() = DecimalType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinSchemaXmlschema)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

    override fun valueFromNormalized(representation: VString): VInteger {
        return VInteger(representation.toLong())
    }

    override fun value(maybeValue: VAnySimpleType): VInteger {
        return maybeValue as? VInteger ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VInteger)
    }

    override fun validate(representation: VString) {
        validateValue(value(representation))
        //TODO("not implemented")
    }
}

object LongType : PrimitiveDatatype<VInteger>("long", XmlSchemaConstants.XS_NAMESPACE), IIntegerType {
    override val baseType: IntegerType get() = IntegerType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinSchemaXmlschema)),
        maxConstraint = // pass null as this has an initialization loop. The value is pre-normalized.
        ResolvedMaxInclusive.createUnverified(VInteger(Long.MAX_VALUE)),
        minConstraint = ResolvedMinInclusive.createUnverified(VInteger(Long.MIN_VALUE)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun valueFromNormalized(representation: VString): VInteger {
        return VInteger(representation.toLong())
    }

    override fun value(maybeValue: VAnySimpleType): VInteger {
        return maybeValue as? VInteger ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VInteger)
    }

    override fun validate(representation: VString) {
        value(representation)
    }
}

object IntType : PrimitiveDatatype<VInteger>("int", XmlSchemaConstants.XS_NAMESPACE), IIntegerType {
    override val baseType: LongType get() = LongType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinSchemaXmlschema)),
        maxConstraint = ResolvedMaxInclusive.createUnverified(VInteger(Int.MAX_VALUE)),
        minConstraint = ResolvedMinInclusive.createUnverified(VInteger(Int.MIN_VALUE)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun valueFromNormalized(representation: VString): VInteger {
        return VInteger(WhitespaceValue.COLLAPSE.normalize(representation).toLong())
    }

    override fun value(maybeValue: VAnySimpleType): VInteger {
        return maybeValue as? VInteger ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VInteger)
    }

    override fun validate(representation: VString) {
        value(representation)
    }

}

object ShortType : PrimitiveDatatype<VInteger>("short", XmlSchemaConstants.XS_NAMESPACE), IIntegerType {
    override val baseType: IntType get() = IntType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinSchemaXmlschema)),
        maxConstraint = ResolvedMaxInclusive.createUnverified(VInteger(32767)),
        minConstraint = ResolvedMinInclusive.createUnverified(VInteger(-32768)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun valueFromNormalized(representation: VString): VInteger {
        return VInteger(representation.toInt())
    }

    override fun value(maybeValue: VAnySimpleType): VInteger {
        return maybeValue as? VInteger ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VInteger)
        check(value.toInt() in Short.MIN_VALUE..Short.MAX_VALUE)
    }

    override fun validate(representation: VString) {
        validateValue(value(representation))
    }

}

object ByteType : PrimitiveDatatype<VInteger>("byte", XmlSchemaConstants.XS_NAMESPACE), IIntegerType {
    override val baseType: ShortType get() = ShortType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinSchemaXmlschema)),
        maxConstraint = ResolvedMaxInclusive.createUnverified(VInteger(127)),
        minConstraint = ResolvedMinInclusive.createUnverified(VInteger(-128)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun valueFromNormalized(representation: VString): VInteger {
        return VInteger(representation.toInt())
    }

    override fun value(maybeValue: VAnySimpleType): VInteger {
        return maybeValue as? VInteger ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VInteger)
        check(value.toInt() in Byte.MIN_VALUE..Byte.MAX_VALUE)
    }

    override fun validate(representation: VString) {
        validateValue(value(representation))
    }

    override fun toString(): String = "Builtin:Byte"
}

object NonNegativeIntegerType : PrimitiveDatatype<VNonNegativeInteger>("nonNegativeInteger", XmlSchemaConstants.XS_NAMESPACE), IIntegerType {
    override val baseType: IntegerType get() = IntegerType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinSchemaXmlschema)),
        minConstraint = ResolvedMinInclusive.createUnverified(VNonNegativeInteger(0)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

    override fun valueFromNormalized(representation: VString): VNonNegativeInteger {
        return VNonNegativeInteger(representation)
    }

    override fun value(maybeValue: VAnySimpleType): VNonNegativeInteger {
        return maybeValue as? VNonNegativeInteger ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VNonNegativeInteger)  { "Value $value is not non-negative"}
    }

    override fun validate(representation: VString) {
        value(representation)
    }

}

object PositiveIntegerType : PrimitiveDatatype<VNonNegativeInteger>("positiveInteger", XmlSchemaConstants.XS_NAMESPACE), IIntegerType {
    override val baseType: NonNegativeIntegerType get() = NonNegativeIntegerType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinSchemaXmlschema)),
        minConstraint = ResolvedMinInclusive.createUnverified(VNonNegativeInteger(1)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

    override fun valueFromNormalized(representation: VString): VNonNegativeInteger {
        return VNonNegativeInteger(representation)
    }

    override fun value(maybeValue: VAnySimpleType): VNonNegativeInteger {
        return maybeValue as? VNonNegativeInteger ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VNonNegativeInteger)  { "Value $value is not positive"}
    }

    override fun validate(representation: VString) {
        value(representation)
    }

}

object UnsignedLongType : PrimitiveDatatype<VUnsignedLong>("unsignedLong", XmlSchemaConstants.XS_NAMESPACE), IIntegerType {
    override val baseType: NonNegativeIntegerType get() = NonNegativeIntegerType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinSchemaXmlschema)),
        maxConstraint = ResolvedMaxInclusive.createUnverified(VUnsignedLong(ULong.MAX_VALUE)),
        minConstraint = ResolvedMinInclusive.createUnverified(VUnsignedLong(0uL)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun valueFromNormalized(representation: VString): VUnsignedLong {
        return VUnsignedLong(representation.toULong())
    }

    override fun value(maybeValue: VAnySimpleType): VUnsignedLong {
        return maybeValue as? VUnsignedLong ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VUnsignedLong)
    }

    override fun validate(representation: VString) {
        value(representation)
    }

}

object UnsignedIntType : PrimitiveDatatype<VUnsignedInt>("unsignedInt", XmlSchemaConstants.XS_NAMESPACE), IIntegerType {
    override val baseType: UnsignedLongType get() = UnsignedLongType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinSchemaXmlschema)),
        maxConstraint = ResolvedMaxInclusive.createUnverified(VUnsignedInt(UInt.MAX_VALUE)),
        minConstraint = ResolvedMinInclusive.createUnverified(VUnsignedInt(0u)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun valueFromNormalized(representation: VString): VUnsignedInt {
        return VUnsignedInt(representation.toUInt())
    }

    override fun value(maybeValue: VAnySimpleType): VUnsignedInt {
        return maybeValue as? VUnsignedInt ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VUnsignedInt)
    }

    override fun validate(representation: VString) {
        validateValue(value(representation))
    }

}

object UnsignedShortType : PrimitiveDatatype<VUnsignedInt>("unsignedShort", XmlSchemaConstants.XS_NAMESPACE), IIntegerType {
    override val baseType: UnsignedIntType get() = UnsignedIntType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinSchemaXmlschema)),
        maxConstraint = ResolvedMaxInclusive.createUnverified(VUnsignedInt(65535u)),
        minConstraint = ResolvedMinInclusive.createUnverified(VUnsignedInt(0u)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun valueFromNormalized(representation: VString): VUnsignedInt {
        return VUnsignedInt(representation.toUInt())
    }

    override fun value(maybeValue: VAnySimpleType): VUnsignedInt {
        return maybeValue as? VUnsignedInt ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VUnsignedInt)
    }

    override fun validate(representation: VString) {
        validateValue(value(representation))
    }

}

object UnsignedByteType : PrimitiveDatatype<VUnsignedInt>("unsignedByte", XmlSchemaConstants.XS_NAMESPACE), IIntegerType {
    override val baseType: UnsignedShortType get() = UnsignedShortType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinSchemaXmlschema)),
        maxConstraint = ResolvedMaxInclusive.createUnverified(
            VUnsignedInt(255u)
        ),
        minConstraint = ResolvedMinInclusive.createUnverified(
            VUnsignedInt(0u)
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun valueFromNormalized(representation: VString): VUnsignedInt {
        return VUnsignedInt(representation.toUInt())
    }

    override fun value(maybeValue: VAnySimpleType): VUnsignedInt {
        return maybeValue as? VUnsignedInt ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VUnsignedInt)
    }

    override fun validate(representation: VString) {
        validateValue(value(representation))
    }

}

object NonPositiveIntegerType : PrimitiveDatatype<VDecimal>("nonPositiveInteger", XmlSchemaConstants.XS_NAMESPACE), IIntegerType {
    override val baseType: IntegerType get() = IntegerType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinSchemaXmlschema)),
        maxConstraint = ResolvedMaxInclusive.createUnverified(VInteger(0)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

    override fun valueFromNormalized(representation: VString): VDecimal {
        return when (representation.toLong()) {
            in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() ->
                VInteger(representation.toLong().toInt())

            else -> VInteger(representation.toLong())
        }
    }

    override fun value(maybeValue: VAnySimpleType): VDecimal {
        return maybeValue as? VDecimal ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VDecimal) { "Value $value is not a decimal"}
    }

    override fun validate(representation: VString) {
        check(value(representation).toLong() <= 0L)
    }

}

object NegativeIntegerType : PrimitiveDatatype<VDecimal>("negativeInteger", XmlSchemaConstants.XS_NAMESPACE), IIntegerType {
    override val baseType: NonPositiveIntegerType get() = NonPositiveIntegerType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinSchemaXmlschema)),
        maxConstraint = ResolvedMaxInclusive.createUnverified(VInteger(-1)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

    override fun valueFromNormalized(representation: VString): VDecimal {
        return when (representation.toLong()) {
            in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() ->
                VInteger(representation.toLong().toInt())

            else -> VInteger(representation.toLong())
        }
    }

    override fun value(maybeValue: VAnySimpleType): VDecimal {
        return maybeValue as? VDecimal ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VDecimal)  { "Value $value is not a decimal"}
        check(value.toLong() < 0L) { "Value $value is not negative"}
    }

    override fun validate(representation: VString) {
        check(value(representation).toLong() < 0L)
    }

}

object DoubleType : PrimitiveDatatype<VDouble>("double", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun valueFromNormalized(representation: VString): VDouble {
        return VDouble(representation.toDouble())
    }

    override fun value(maybeValue: VAnySimpleType): VDouble {
        return maybeValue as? VDouble ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VDouble)
    }

    override fun validate(representation: VString) {
        value(representation)
    }

}

object DurationType : PrimitiveDatatype<VDuration>("duration", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(representation: VString): VDuration {
        return VDuration(representation.xmlString)
    }

    override fun value(maybeValue: VAnySimpleType): VDuration {
        return maybeValue as? VDuration ?: value(VString(maybeValue.xmlString))
    }

    override fun validate(representation: VString) {
        value(representation)
    }
}

object DayTimeDurationType : PrimitiveDatatype<VDuration>("dayTimeDuration", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: DurationType get() = DurationType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[^YM]*(T.*)?"), BuiltinSchemaXmlschema)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(representation: VString): VDuration {
        return VDuration(representation.xmlString)
    }

    override fun value(maybeValue: VAnySimpleType): VDuration {
        return maybeValue as? VDuration ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        require(value is VDuration)
        require(value.months == 0L)
        val days = value.millis / (24 * 3600_000)
        require(days == 0L)
    }

    override fun validate(representation: VString) {
        validateValue(value(representation))
    }
}

object YearMonthDurationType : PrimitiveDatatype<VDuration>("yearMonthDuration", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: DurationType get() = DurationType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[^DT]*"), BuiltinSchemaXmlschema)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(representation: VString): VDuration {
        return VDuration(representation.xmlString)
    }

    override fun value(maybeValue: VAnySimpleType): VDuration {
        return maybeValue as? VDuration ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        require(value is VDuration)
        val seconds = value.millis % (24 * 3600_000)
        require(seconds == 0L)
    }

    override fun validate(representation: VString) {
        validateValue(value(representation))
    }
}

object FloatType : PrimitiveDatatype<VFloat>("float", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun valueFromNormalized(representation: VString): VFloat {
        return VFloat(representation.toFloat())
    }

    override fun value(maybeValue: VAnySimpleType): VFloat {
        return maybeValue as? VFloat ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VFloat)
    }

    override fun validate(representation: VString) {
        value(representation)
    }

}

object GDayType : PrimitiveDatatype<VGDay>("gDay", XmlSchemaConstants.XS_NAMESPACE), FiniteDateType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
        explicitTimezone = ResolvedExplicitTimezone(
            XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL),
            BuiltinSchemaXmlschema
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(representation: VString): VGDay {
        val s = representation.xmlString
        require(s.startsWith("---"))
        val tzIndex = s.indexOf('Z', 3)
        return when {
            tzIndex < 0 -> VGDay(s.substring(3).toInt())
            else -> VGDay(s.substring(3, tzIndex).toInt(), IDateTime.timezoneFragValue(s.substring(tzIndex)))
        }
    }

    override fun value(maybeValue: VAnySimpleType): VGDay {
        return maybeValue as? VGDay ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VGDay)
    }

    override fun validate(representation: VString) {
        value(representation)
    }
}

object GMonthType : PrimitiveDatatype<VGMonth>("gMonth", XmlSchemaConstants.XS_NAMESPACE), FiniteDateType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
        explicitTimezone = ResolvedExplicitTimezone(
            XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL),
            BuiltinSchemaXmlschema
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(representation: VString): VGMonth {
        val s = representation.xmlString
        require(s.startsWith("--"))
        val month = s.substring(2, 4).toInt()

        if (s.length == 4) {
            return VGMonth(month)
        } else { // Handle bogus month format with trailing dashes (non-standard compliant, error in xmlschema older versions).
            if (s.length > 5 && s[4] == '-' && s[5] == '-') error("Trailing slashes in month format (not compliant)")
            val tz = s.substring(4)
            val tzOffset = IDateTime.timezoneFragValue(tz)
            return VGMonth(month, tzOffset)
        }
    }

    override fun value(maybeValue: VAnySimpleType): VGMonth {
        return maybeValue as? VGMonth ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VGMonth)
    }

    override fun validate(representation: VString) {
        value(representation)
    }
}

object GMonthDayType : PrimitiveDatatype<VGMonthDay>("gMonthDay", XmlSchemaConstants.XS_NAMESPACE), FiniteDateType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
        explicitTimezone = ResolvedExplicitTimezone(
            XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL),
            BuiltinSchemaXmlschema
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(representation: VString): VGMonthDay {
        val s = representation.xmlString
        require(s.startsWith("--"))
        val tzIndex = s.indexOf('Z', 2)
        return when {
            tzIndex < 0 -> {
                val (month, day) = s.substring(2).split('-').map { it.toUInt() }
                VGMonthDay(month, day)
            }

            else -> {
                val tz = IDateTime.timezoneFragValue(s.substring(tzIndex))
                val (month, day) = s.substring(2, tzIndex).split('-').map { it.toUInt() }
                VGMonthDay(month, day, tz)
            }
        }
    }

    override fun value(maybeValue: VAnySimpleType): VGMonthDay {
        return maybeValue as? VGMonthDay ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VGMonthDay)
    }

    override fun validate(representation: VString) {
        value(representation)
    }
}

object GYearType : PrimitiveDatatype<VGYear>("gYear", XmlSchemaConstants.XS_NAMESPACE), FiniteDateType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
        explicitTimezone = ResolvedExplicitTimezone(
            XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL),
            BuiltinSchemaXmlschema
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(representation: VString): VGYear {
        val s = representation.xmlString
        val yearEnd = s.substring(1).indexOfFirst { it !in '0'..'9' }.let { if (it >= 0) it + 1 else s.length }
        val year = s.substring(0, yearEnd).toInt()
        val tzOffset = IDateTime.timezoneFragValue(s.substring(yearEnd))
        return VGYear(year, tzOffset)
    }

    override fun value(maybeValue: VAnySimpleType): VGYear {
        return maybeValue as? VGYear ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VGYear)
    }

    override fun validate(representation: VString) {
        value(representation)
    }
}

object GYearMonthType : PrimitiveDatatype<VGYearMonth>("gYearMonth", XmlSchemaConstants.XS_NAMESPACE), FiniteDateType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
        explicitTimezone = ResolvedExplicitTimezone(
            XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL),
            BuiltinSchemaXmlschema
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(representation: VString): VGYearMonth {
        val (year, month) = representation.split('-').map { it.toInt() }
        return VGYearMonth(year, month.toUInt())
    }

    override fun value(maybeValue: VAnySimpleType): VGYearMonth {
        return maybeValue as? VGYearMonth ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VGYearMonth)
    }

    override fun validate(representation: VString) {
        value(representation)
    }

}

object HexBinaryType : PrimitiveDatatype<VByteArray>("hexBinary", XmlSchemaConstants.XS_NAMESPACE) {
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
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: VString): VByteArray {
        require(normalized.length % 2 == 0) { "Hex must have even amount of characters" }
        val b = ByteArray(normalized.length / 2) { normalized.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
        return VByteArray(b)
    }

    override fun value(maybeValue: VAnySimpleType): VByteArray {
        return maybeValue as? VByteArray ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VByteArray) { "Value for hex binary is not a ByteArray" }
    }

    override fun validate(representation: VString) {
        validateValue(value(representation))
    }
}

object NotationType : PrimitiveDatatype<VNotation>("NOTATION", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(representation: VString): VNotation {
        return VNotation(representation)
    }

    override fun value(maybeValue: VAnySimpleType): VNotation {
        return maybeValue as? VNotation ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VNotation)
    }

    override fun validate(representation: VString) {

    }
}

object QNameType : PrimitiveDatatype<VQName>("QName", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(representation: VString): VQName {
        return (representation as? VPrefixString)?.toVQName() ?: VQName(representation.xmlString)
    }

    override fun value(maybeValue: VAnySimpleType): VQName {
        return maybeValue as? VQName ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VQName)
    }

    override fun validate(representation: VString) {
        val localName = when (representation) {
            is VPrefixString -> representation.localname
            else -> representation.xmlString
        }
        check(localName.indexOf(':') < 0) { "local names cannot contain : characters" }
    }
}

object StringType : PrimitiveDatatype<VString>("string", XmlSchemaConstants.XS_NAMESPACE), IStringType {
    override val baseType: AnyAtomicType get() = AnyAtomicType
    override val simpleDerivation: ResolvedSimpleRestrictionBase
        get() = SimpleBuiltinRestriction(
            baseType,
            BuiltinSchemaXmlschema,
            listOf(XSWhiteSpace(WhitespaceValue.PRESERVE, fixed = false))
        )

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.PRESERVE), BuiltinSchemaXmlschema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: VString): VString {
        return normalized
    }

    override fun value(maybeValue: VAnySimpleType): VString {
        return maybeValue as? VString ?: VString(maybeValue.xmlString)
    }

    override fun validateValue(value: Any) {
        check(value is VString)
    }

    override fun validate(representation: VString) {}
}

object NormalizedStringType : PrimitiveDatatype<VNormalizedString>("normalizedString", XmlSchemaConstants.XS_NAMESPACE), IStringType {
    override val baseType: StringType get() = StringType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.REPLACE), BuiltinSchemaXmlschema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: VString): VNormalizedString {
        return normalized as? VNormalizedString ?: VNormalizedString(normalized.xmlString)
    }

    override fun value(maybeValue: VAnySimpleType): VNormalizedString {
        return maybeValue as? VNormalizedString ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VNormalizedString)
    }

    override fun validate(representation: VString) {
        // all representations are valid
    }
}

object TokenType : PrimitiveDatatype<VToken>("token", XmlSchemaConstants.XS_NAMESPACE), IStringType {
    override val baseType: NormalizedStringType get() = NormalizedStringType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE), BuiltinSchemaXmlschema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(representation: VString): VToken {
        return representation as? VToken ?: VToken(representation.xmlString)
    }

    override fun value(maybeValue: VAnySimpleType): VToken {
        return maybeValue as? VToken ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VToken)
    }

    override fun validate(representation: VString) {
        mdlFacets.validate(this, representation)
    }
}

object LanguageType : PrimitiveDatatype<VLanguage>("language", XmlSchemaConstants.XS_NAMESPACE), IStringType {
    override val baseType: TokenType get() = TokenType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[a-zA-Z]{1,8}(-[a-zA-Z0-9]{1,8})*"), BuiltinSchemaXmlschema)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(representation: VString): VLanguage {
        return VLanguage(representation.xmlString)
    }

    override fun value(maybeValue: VAnySimpleType): VLanguage {
        return maybeValue as? VLanguage ?: VLanguage(maybeValue.xmlString)
    }

    override fun validateValue(value: Any) {
        mdlFacets.validateValue(value as VLanguage)
    }

    override fun validate(representation: VString) {
        value(representation) // triggers validation in constructor
    }
}

object NameType : PrimitiveDatatype<VName>("Name", XmlSchemaConstants.XS_NAMESPACE), IStringType {
    override val baseType: TokenType get() = TokenType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("\\i\\c*"), BuiltinSchemaXmlschema)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(representation: VString): VName {
        return representation as? VName ?: VName(representation.xmlString)
    }

    override fun value(maybeValue: VAnySimpleType): VName {
        return maybeValue as? VName ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VName)
    }

    override fun validate(representation: VString) {
        value(representation)
    }
}

object NCNameType : PrimitiveDatatype<VNCName>("NCName", XmlSchemaConstants.XS_NAMESPACE), IStringType {
    override val baseType: NameType get() = NameType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE), BuiltinSchemaXmlschema),
        patterns = listOf(
            ResolvedPattern(XSPattern("\\i\\c*"), BuiltinSchemaXmlschema),
            ResolvedPattern(XSPattern("[\\i-[:]][\\c-[:]]*"), BuiltinSchemaXmlschema)
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(representation: VString): VNCName {
        return representation as? VNCName ?: VNCName(representation.xmlString)
    }

    override fun value(maybeValue: VAnySimpleType): VNCName {
        return maybeValue as? VNCName ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VNCName)
        mdlFacets.validate(mdlPrimitiveTypeDefinition, value)
    }

    override fun validate(representation: VString) {
        mdlFacets.validate(mdlPrimitiveTypeDefinition, representation)
    }

}

object EntityType : PrimitiveDatatype<VString>("ENTITY", XmlSchemaConstants.XS_NAMESPACE), IStringType {
    override val baseType: NCNameType get() = NCNameType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE), BuiltinSchemaXmlschema),
        patterns = listOf(
            ResolvedPattern(XSPattern("\\i\\c*"), BuiltinSchemaXmlschema),
            ResolvedPattern(XSPattern("[\\i-[:]][\\c-[:]]*"), BuiltinSchemaXmlschema)
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(representation: VString): VString {
        return representation
    }

    override fun value(maybeValue: VAnySimpleType): VString {
        return maybeValue as? VString ?: VString(maybeValue.xmlString)
    }

    override fun validate(representation: VString) {
        mdlFacets.validate(this, representation)
    }
}

object IDType : PrimitiveDatatype<VID>("ID", XmlSchemaConstants.XS_NAMESPACE), IStringType {
    override val baseType: NCNameType get() = NCNameType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE), BuiltinSchemaXmlschema),
        patterns = listOf(
            ResolvedPattern(XSPattern("\\i\\c*"), BuiltinSchemaXmlschema),
            ResolvedPattern(XSPattern("[\\i-[:]][\\c-[:]]*"), BuiltinSchemaXmlschema)
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(representation: VString): VID {
        return representation as? VID ?: VID(representation.xmlString)
    }

    override fun value(maybeValue: VAnySimpleType): VID {
        return maybeValue as? VID ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VID)
        mdlFacets.validateValue(value)
    }

    override fun validate(representation: VString) {
        validateValue(value(representation))
    }
}

object IDRefType : PrimitiveDatatype<VIDRef>("IDREF", XmlSchemaConstants.XS_NAMESPACE), IStringType {
    override val baseType: NCNameType get() = NCNameType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE), BuiltinSchemaXmlschema),
        patterns = listOf(
            ResolvedPattern(XSPattern("\\i\\c*"), BuiltinSchemaXmlschema),
            ResolvedPattern(XSPattern("[\\i-[:]][\\c-[:]]*"), BuiltinSchemaXmlschema)
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(representation: VString): VIDRef {
        return representation as? VIDRef ?: VIDRef(representation.xmlString)
    }

    override fun value(maybeValue: VAnySimpleType): VIDRef {
        return maybeValue as? VIDRef ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VIDRef) {"$value is not an IDRef"}
        check(value.isNotEmpty()) { "IDRef may not be empty" }
    }

    override fun validate(representation: VString) {
        value(representation)
    }
}

object NMTokenType : PrimitiveDatatype<VNMToken>("NMTOKEN", XmlSchemaConstants.XS_NAMESPACE), IStringType {
    override val baseType: TokenType get() = TokenType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("\\c+"), BuiltinSchemaXmlschema)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(representation: VString): VNMToken {
        return representation as? VNMToken ?: VNMToken(representation.xmlString)
    }

    override fun value(maybeValue: VAnySimpleType): VNMToken {
        return maybeValue as? VNMToken ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any) {
        check(value is VNMToken)
    }

    override fun validate(representation: VString) {
        value(representation)
    }
}

object TimeType : PrimitiveDatatype<VTime>("time", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
        explicitTimezone = ResolvedExplicitTimezone(
            XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL),
            BuiltinSchemaXmlschema
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(representation: VString): VTime {
        return VTime(representation.xmlString)
    }

    override fun value(maybeValue: VAnySimpleType): VTime {
        return maybeValue as? VTime ?: value(VString(maybeValue.xmlString))
    }

    override fun validate(representation: VString) {
        validateValue(value(representation))
    }
}

object EntitiesType :
    ConstructedListDatatype(
        "ENTITIES",
        XmlSchemaConstants.XS_NAMESPACE,
        EntityType,
        BuiltinSchemaXmlschema,
    ) {
    override val mdlItemTypeDefinition: ResolvedSimpleType
        get() = EntityType

    override fun validate(representation: VString) {}
}

object IDRefsType : ConstructedListDatatype(
        "IDREFS",
        XmlSchemaConstants.XS_NAMESPACE,
        EntityType,
        BuiltinSchemaXmlschema
    ) {
    override val mdlItemTypeDefinition: ResolvedSimpleType
        get() = IDRefType

    override fun validate(representation: VString) {}
}

object NMTokensType : ConstructedListDatatype(
    "NMTOKENS",
    XmlSchemaConstants.XS_NAMESPACE,
    EntityType,
    BuiltinSchemaXmlschema
) {
    override val mdlItemTypeDefinition: ResolvedSimpleType
        get() = NMTokenType

    override fun validate(representation: VString) {}
}

object PrecisionDecimalType : PrimitiveDatatype<VAnySimpleType>("precisionDecimal", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), BuiltinSchemaXmlschema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

    override fun valueFromNormalized(representation: VString): VAnySimpleType {
        TODO("NOT IMPLEMENTED")
    }

    override fun value(maybeValue: VAnySimpleType): VAnySimpleType {
        TODO("not implemented")
    }

    override fun validate(representation: VString) {
        mdlFacets.validate(this, representation)
//        TODO("not implemented")
    }
}
