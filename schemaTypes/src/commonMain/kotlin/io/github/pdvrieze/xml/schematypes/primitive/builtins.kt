/*
 * Copyright (c) 2023-2026.
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

package io.github.pdvrieze.xml.schematypes.primitive

import io.github.pdvrieze.formats.xmlschema.datatypes.*
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.*
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSExplicitTimezone
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSFractionDigits
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSPattern
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSWhiteSpace
import io.github.pdvrieze.formats.xmlschema.regex.XRegex
import io.github.pdvrieze.formats.xmlschema.resolved.*
import io.github.pdvrieze.formats.xmlschema.resolved.facets.*
import io.github.pdvrieze.formats.xmlschema.types.CardinalityFacet.Cardinality
import io.github.pdvrieze.formats.xmlschema.types.FundamentalFacets
import io.github.pdvrieze.formats.xmlschema.types.OrderedFacet.Order
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import io.github.pdvrieze.xml.schematypes.ISimpleType
import io.github.pdvrieze.xml.schematypes.WhitespaceValue
import io.github.pdvrieze.xml.schematypes.values.*
import io.github.pdvrieze.xml.schematypes.values.instances.*
import nl.adaptivity.xmlutil.XMLConstants.XSD_NS_URI
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.xmlCollapseWhitespace
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

fun builtinType(localName: String, targetNamespace: String): ResolvedBuiltinType? {
    if (targetNamespace != XSD_NS_URI) return null
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
    override fun value(representation: XSString): XSString
}

sealed interface IDecimalType : ResolvedBuiltinSimpleType {
    override fun value(representation: XSString): XSDecimal
}

sealed class AtomicDatatype(name: String, targetNamespace: String) :
    ISimpleType.Atomic {

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

sealed class PrimitiveDatatype<out T : XSAnySimple>(name: String, targetNamespace: String) :
    AtomicDatatype(name, targetNamespace) {
    final override val isSpecial: Boolean get() = false

    final override fun value(representation: XSString): T {
        val normalized = mdlFacets.whiteSpace?.value?.normalize(representation) ?: representation
        return valueFromNormalized(normalized)
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        mdlFacets.validateValue(value)
    }

    protected abstract fun valueFromNormalized(normalized: XSString): T

    abstract fun value(maybeValue: XSAnySimple): XSAnySimple

    abstract override val baseType: ResolvedBuiltinSimpleType
    override val simpleDerivation: ResolvedSimpleRestrictionBase
        get() = SimpleBuiltinRestriction(baseType, schema = BuiltinSchemaXmlschema)

    final override val mdlBaseTypeDefinition: ResolvedBuiltinType get() = baseType
    override val mdlPrimitiveTypeDefinition: PrimitiveDatatype<T>? get() = this
}

object AnyAtomicType : AtomicDatatype("anyAtomicType", XSD_NS_URI) {
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

    override fun validateValue(value: Any, version: SchemaVersion) {
        error("Atomic is not directly usable")
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        error("Atomic is not directly usable")
    }
}

object AnyURIType : PrimitiveDatatype<XSAnyURI>("anyURI", XSD_NS_URI) {
    override val baseType: AnyAtomicType get() = AnyAtomicType
    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true))
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XSString): XSAnyURI = normalized.toString().toAnyUri()

    override fun value(maybeValue: XSAnySimple): XSAnyURI {
        return maybeValue as? XSAnyURI
            ?: value(XSString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        when (version) {
            SchemaVersion.V1_0 if (value is XSParsedUri) -> {}
            SchemaVersion.V1_0 if (value is XSAnyURI) -> {
                val _ = XSParsedUri(value)
            }

            else -> check(value is XSAnyURI)
        }
        mdlFacets.validateValue(value)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        mdlFacets.validate(mdlPrimitiveTypeDefinition, representation)
    }
}

@OptIn(ExperimentalEncodingApi::class)
object Base64BinaryType : PrimitiveDatatype<XSByteArrayImpl>("base64Binary", XSD_NS_URI) {
    fun length(representation: String): Int {
        // TODO don't actually decode just for length.
        return Base64.decode(representation).size
    }

    val regex = XRegex("((([A-Za-z0-9+/] ?){4})*(([A-Za-z0-9+/] ?){3}[A-Za-z0-9+/]|([A-Za-z0-9+/] ?){2}[AEIMQUYcgkosw048] ?=|[A-Za-z0-9+/] ?[AQgw] ?= ?=))?", SchemaVersion.V1_0)

    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XSString): XSByteArrayImpl {
        check(regex.matches(normalized))
        return VByteArray(
            Base64.decode(
                normalized
            )
        )
    }

    override fun value(maybeValue: XSAnySimple): XSByteArrayImpl {
        return maybeValue as? XSByteArrayImpl ?: value(
            XSString(maybeValue.xmlString)
        )
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSByteArrayImpl)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        val _ = value(representation)
    }
}

object BooleanType : PrimitiveDatatype<XSBooleanImpl>("boolean", XSD_NS_URI) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.FINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XSString): XSBooleanImpl = when (normalized.toString()) {
        "true", "1" -> XSBooleanImpl.TRUE
        "false", "0" -> XSBooleanImpl.FALSE
        else -> error("$normalized is not a boolean")
    }

    override fun value(maybeValue: XSAnySimple): XSBooleanImpl {
        return maybeValue as? XSBooleanImpl
            ?: value(XSString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSBooleanImpl)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        val _ = value(representation)
    }
}

interface FiniteDateType : ResolvedBuiltinSimpleType

object DateType : PrimitiveDatatype<XSDateImpl>("date", XSD_NS_URI), FiniteDateType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
        explicitTimezone = ResolvedExplicitTimezone(
            XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL)
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XSString): XSDateImpl {
        val s = normalized.xmlString
        val monthIdx = s.indexOf('-', 1) // sign can be start
        val year = s.substring(0, monthIdx).toInt()
        val month = s.substring(monthIdx + 1, monthIdx + 3).toInt()
        if (s[monthIdx + 3] != '-') throw NumberFormatException("Missing - between month and day")
        val day = s.substring(monthIdx + 4, monthIdx + 6).toInt()

        return when {
            normalized.length >= monthIdx + 6 ->
                VDate(
                    year,
                    month,
                    day,
                    XSDateTime.timezoneFragValue(
                        s.substring(monthIdx + 6)
                    )
                )

            else ->
                VDate(year, month, day)
        }
    }

    override fun value(maybeValue: XSAnySimple): XSDateImpl {
        return maybeValue as? XSDateImpl
            ?: value(XSString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSDateImpl)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        val _ = value(representation)
    }
}

object DateTimeType : PrimitiveDatatype<XSDateTimeImpl>("dateTime", XSD_NS_URI) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
        explicitTimezone = ResolvedExplicitTimezone(
            XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL)
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XSString): XSDateTimeImpl {
        val s = normalized.xmlString
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
        val seconds = DecimalType.value(
            XSString(
                s.substring(tIndex + 7, secEnd ?: s.length)
            )
        )
        return when (secEnd) {
            null -> XSDateTimeImpl(
                year,
                month.toUInt(),
                day.toUInt(),
                hour,
                minutes,
                seconds
            )
            else -> {
                val timezoneOffset = XSDateTime.timezoneFragValue(s.substring(secEnd))
                XSDateTimeImpl(
                    year,
                    month.toUInt(),
                    day.toUInt(),
                    hour,
                    minutes,
                    seconds,
                    timezoneOffset
                )
            }
        }
    }

    override fun value(maybeValue: XSAnySimple): XSDateTimeImpl {
        return maybeValue as? XSDateTimeImpl ?: value(
            XSString(maybeValue.xmlString)
        )
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        validateValue(value(representation), version)
    }
}

object DateTimeStampType : PrimitiveDatatype<XSDateTimeImpl>("dateTimeStamp", XSD_NS_URI) {
    override val baseType: DateTimeType get() = DateTimeType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
        explicitTimezone = ResolvedExplicitTimezone(
            XSExplicitTimezone(XSExplicitTimezone.Value.REQUIRED, true)
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XSString): XSDateTimeImpl {
        return DateTimeType.value(normalized).also {
            requireNotNull(it.timezoneOffset) { "DateTimestamps must have a timestamp" }
        }
    }

    override fun value(maybeValue: XSAnySimple): XSDateTimeImpl {
        return (maybeValue as? XSDateTimeImpl)?.also {
            requireNotNull(it.timezoneOffset) { "DateTimestamps must have a timestamp" }
        } ?: value(XSString(maybeValue.xmlString))
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        validateValue(value(representation), version)
    }
}

object DecimalType : PrimitiveDatatype<XSDecimal>("decimal", XSD_NS_URI), IDecimalType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

    override fun valueFromNormalized(normalized: XSString): XSDecimal {
        return try {
            when (normalized.toLong()) {
                in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() ->
                    VInteger(
                        normalized.toLong().toInt()
                    )

                else -> VInteger(normalized.toLong())
            }
        } catch (e: NumberFormatException) {
            VBigDecimalImpl(
                xmlCollapseWhitespace(normalized.xmlString)
            )
        }
    }

    override fun value(maybeValue: XSAnySimple): XSDecimal {
        return maybeValue as? XSDecimal
            ?: value(XSString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSDecimal)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        validateValue(value(representation), version)
    }

}

sealed interface IIntegerType : IDecimalType

object IntegerType : PrimitiveDatatype<XSInteger>("integer", XSD_NS_URI), IIntegerType {
    override val baseType: DecimalType get() = DecimalType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u)),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), SchemaVersion.V1_1)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

    override fun valueFromNormalized(normalized: XSString): XSInteger {
        return VInteger(normalized.toLong())
    }

    override fun value(maybeValue: XSAnySimple): XSInteger {
        return maybeValue as? XSInteger
            ?: value(XSString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSInteger)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        validateValue(value(representation), version)
        //TODO("not implemented")
    }
}

object LongType : PrimitiveDatatype<XSInteger>("long", XSD_NS_URI), IIntegerType {
    override val baseType: IntegerType get() = IntegerType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u)),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), SchemaVersion.V1_1,)),
        maxConstraint = // pass null as this has an initialization loop. The value is pre-normalized.
        ResolvedMaxInclusive.createUnverified(
            VInteger(
                Long.MAX_VALUE
            )
        ),
        minConstraint = ResolvedMinInclusive.createUnverified(
            VInteger(
                Long.MIN_VALUE
            )
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun valueFromNormalized(normalized: XSString): XSInteger {
        return VInteger(normalized.toLong())
    }

    override fun value(maybeValue: XSAnySimple): XSInteger {
        return maybeValue as? XSInteger
            ?: value(XSString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSInteger)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        val _ = value(representation)
    }
}

object IntType : PrimitiveDatatype<XSInteger>("int", XSD_NS_URI), IIntegerType {
    override val baseType: LongType get() = LongType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u)),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), SchemaVersion.V1_1,)),
        maxConstraint = ResolvedMaxInclusive.createUnverified(
            VInteger(
                Int.MAX_VALUE
            )
        ),
        minConstraint = ResolvedMinInclusive.createUnverified(
            VInteger(
                Int.MIN_VALUE
            )
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun valueFromNormalized(normalized: XSString): XSInteger {
        return VInteger(
            WhitespaceValue.COLLAPSE.normalize(
                normalized
            ).toLong()
        )
    }

    override fun value(maybeValue: XSAnySimple): XSInteger {
        return maybeValue as? XSInteger
            ?: value(XSString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSInteger)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        val _ = value(representation)
    }

}

object ShortType : PrimitiveDatatype<XSInteger>("short", XSD_NS_URI), IIntegerType {
    override val baseType: IntType get() = IntType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u)),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), SchemaVersion.V1_1,)),
        maxConstraint = ResolvedMaxInclusive.createUnverified(
            VInteger(
                32767
            )
        ),
        minConstraint = ResolvedMinInclusive.createUnverified(
            VInteger(
                -32768
            )
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun valueFromNormalized(normalized: XSString): XSInteger {
        return VInteger(normalized.toInt())
    }

    override fun value(maybeValue: XSAnySimple): XSInteger {
        return maybeValue as? XSInteger
            ?: value(XSString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSInteger)
        check(value.toInt() in Short.MIN_VALUE..Short.MAX_VALUE)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        validateValue(value(representation), version)
    }

}

object ByteType : PrimitiveDatatype<XSInteger>("byte", XSD_NS_URI), IIntegerType {
    override val baseType: ShortType get() = ShortType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u)),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), SchemaVersion.V1_1,)),
        maxConstraint = ResolvedMaxInclusive.createUnverified(
            VInteger(
                127
            )
        ),
        minConstraint = ResolvedMinInclusive.createUnverified(
            VInteger(
                -128
            )
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun valueFromNormalized(normalized: XSString): XSInteger {
        return VInteger(normalized.toInt())
    }

    override fun value(maybeValue: XSAnySimple): XSInteger {
        return maybeValue as? XSInteger
            ?: value(XSString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSInteger)
        check(value.toInt() in Byte.MIN_VALUE..Byte.MAX_VALUE)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        validateValue(value(representation), version)
    }

    override fun toString(): String = "Builtin:Byte"
}

object NonNegativeIntegerType : PrimitiveDatatype<XSNonNegativeInteger>("nonNegativeInteger", XSD_NS_URI), IIntegerType {
    override val baseType: IntegerType get() = IntegerType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u)),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), SchemaVersion.V1_1,)),
        minConstraint = ResolvedMinInclusive.createUnverified(XSNonNegativeInteger(0)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

    override fun valueFromNormalized(normalized: XSString): XSNonNegativeInteger {
        return XSNonNegativeInteger(normalized)
    }

    override fun value(maybeValue: XSAnySimple): XSNonNegativeInteger {
        return maybeValue as? XSNonNegativeInteger ?: value(
            XSString(
                maybeValue.xmlString
            )
        )
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSNonNegativeInteger)  { "Value $value is not non-negative"}
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        val _ = value(representation)
    }

}

object PositiveIntegerType : PrimitiveDatatype<XSNonNegativeInteger>("positiveInteger", XSD_NS_URI), IIntegerType {
    override val baseType: NonNegativeIntegerType get() = NonNegativeIntegerType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u)),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), SchemaVersion.V1_1,)),
        minConstraint = ResolvedMinInclusive.createUnverified(XSNonNegativeInteger(1)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

    override fun valueFromNormalized(normalized: XSString): XSNonNegativeInteger {
        return XSNonNegativeInteger(normalized)
    }

    override fun value(maybeValue: XSAnySimple): XSNonNegativeInteger {
        return maybeValue as? XSNonNegativeInteger ?: value(
            XSString(
                maybeValue.xmlString
            )
        )
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSNonNegativeInteger)  { "Value $value is not positive"}
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        val _ = value(representation)
    }

}

object UnsignedLongType : PrimitiveDatatype<XSUnsignedLong>("unsignedLong", XSD_NS_URI), IIntegerType {
    override val baseType: NonNegativeIntegerType get() = NonNegativeIntegerType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u)),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), SchemaVersion.V1_1,)),
        maxConstraint = ResolvedMaxInclusive.createUnverified(XSUnsignedLong(ULong.MAX_VALUE)),
        minConstraint = ResolvedMinInclusive.createUnverified(XSUnsignedLong(0uL)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun valueFromNormalized(normalized: XSString): XSUnsignedLong {
        return XSUnsignedLong(normalized.toULong())
    }

    override fun value(maybeValue: XSAnySimple): XSUnsignedLong {
        return maybeValue as? XSUnsignedLong ?: value(
            XSString(
                maybeValue.xmlString
            )
        )
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSUnsignedLong)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        val _ = value(representation)
    }

}

object UnsignedIntType : PrimitiveDatatype<XSUnsignedInt>("unsignedInt", XSD_NS_URI), IIntegerType {
    override val baseType: UnsignedLongType get() = UnsignedLongType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u)),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), SchemaVersion.V1_1,)),
        maxConstraint = ResolvedMaxInclusive.createUnverified(XSUnsignedInt(UInt.MAX_VALUE)),
        minConstraint = ResolvedMinInclusive.createUnverified(XSUnsignedInt(0u)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun valueFromNormalized(normalized: XSString): XSUnsignedInt {
        return XSUnsignedInt(normalized.toUInt())
    }

    override fun value(maybeValue: XSAnySimple): XSUnsignedInt {
        return maybeValue as? XSUnsignedInt ?: value(
            XSString(
                maybeValue.xmlString
            )
        )
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSUnsignedInt)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        validateValue(value(representation), version)
    }

}

object UnsignedShortType : PrimitiveDatatype<XSUnsignedInt>("unsignedShort", XSD_NS_URI), IIntegerType {
    override val baseType: UnsignedIntType get() = UnsignedIntType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u)),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), SchemaVersion.V1_1,)),
        maxConstraint = ResolvedMaxInclusive.createUnverified(XSUnsignedInt(65535u)),
        minConstraint = ResolvedMinInclusive.createUnverified(XSUnsignedInt(0u)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun valueFromNormalized(normalized: XSString): XSUnsignedInt {
        return XSUnsignedInt(normalized.toUInt())
    }

    override fun value(maybeValue: XSAnySimple): XSUnsignedInt {
        return maybeValue as? XSUnsignedInt ?: value(
            XSString(
                maybeValue.xmlString
            )
        )
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSUnsignedInt)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        validateValue(value(representation), version)
    }

}

object UnsignedByteType : PrimitiveDatatype<XSUnsignedInt>("unsignedByte", XSD_NS_URI), IIntegerType {
    override val baseType: UnsignedShortType get() = UnsignedShortType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u)),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), SchemaVersion.V1_1,)),
        maxConstraint = ResolvedMaxInclusive.createUnverified(
            XSUnsignedInt(255u)
        ),
        minConstraint = ResolvedMinInclusive.createUnverified(
            XSUnsignedInt(0u)
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun valueFromNormalized(normalized: XSString): XSUnsignedInt {
        return XSUnsignedInt(normalized.toUInt())
    }

    override fun value(maybeValue: XSAnySimple): XSUnsignedInt {
        return maybeValue as? XSUnsignedInt ?: value(
            XSString(
                maybeValue.xmlString
            )
        )
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSUnsignedInt)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        validateValue(value(representation), version)
    }

}

object NonPositiveIntegerType : PrimitiveDatatype<XSDecimal>("nonPositiveInteger", XSD_NS_URI), IIntegerType {
    override val baseType: IntegerType get() = IntegerType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u)),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), SchemaVersion.V1_1,)),
        maxConstraint = ResolvedMaxInclusive.createUnverified(
            VInteger(
                0
            )
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

    override fun valueFromNormalized(normalized: XSString): XSDecimal {
        return when (normalized.toLong()) {
            in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() ->
                VInteger(
                    normalized.toLong().toInt()
                )

            else -> VInteger(normalized.toLong())
        }
    }

    override fun value(maybeValue: XSAnySimple): XSDecimal {
        return maybeValue as? XSDecimal
            ?: value(XSString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSDecimal) { "Value $value is not a decimal"}
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        check(value(representation).toLong() <= 0L)
    }

}

object NegativeIntegerType : PrimitiveDatatype<XSDecimal>("negativeInteger", XSD_NS_URI), IIntegerType {
    override val baseType: NonPositiveIntegerType get() = NonPositiveIntegerType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u)),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), SchemaVersion.V1_1,)),
        maxConstraint = ResolvedMaxInclusive.createUnverified(
            VInteger(
                -1
            )
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

    override fun valueFromNormalized(normalized: XSString): XSDecimal {
        return when (normalized.toLong()) {
            in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() ->
                VInteger(
                    normalized.toLong().toInt()
                )

            else -> VInteger(normalized.toLong())
        }
    }

    override fun value(maybeValue: XSAnySimple): XSDecimal {
        return maybeValue as? XSDecimal
            ?: value(XSString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSDecimal)  { "Value $value is not a decimal"}
        check(value.toLong() < 0L) { "Value $value is not negative"}
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        check(value(representation).toLong() < 0L)
    }

}

object DoubleType : PrimitiveDatatype<XSDoubleImpl>("double", XSD_NS_URI) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun valueFromNormalized(normalized: XSString): XSDoubleImpl {
        return VDouble(normalized.toDouble())
    }

    override fun value(maybeValue: XSAnySimple): XSDoubleImpl {
        return maybeValue as? XSDoubleImpl
            ?: value(XSString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSDoubleImpl)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        val _ = value(representation)
    }

}

object DurationType : PrimitiveDatatype<XSDurationInst>("duration", XSD_NS_URI) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XSString): XSDurationInst {
        return XSDurationInst(normalized.xmlString)
    }

    override fun value(maybeValue: XSAnySimple): XSDurationInst {
        return maybeValue as? XSDurationInst ?: value(
            XSString(
                maybeValue.xmlString
            )
        )
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        val _ = value(representation)
    }
}

object DayTimeDurationType : PrimitiveDatatype<XSDurationInst>("dayTimeDuration", XSD_NS_URI) {
    override val baseType: DurationType get() = DurationType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
        patterns = listOf(ResolvedPattern(XSPattern("[^YM]*(T.*)?"), SchemaVersion.V1_1,)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XSString): XSDurationInst {
        return XSDurationInst(normalized.xmlString)
    }

    override fun value(maybeValue: XSAnySimple): XSDurationInst {
        return maybeValue as? XSDurationInst ?: value(
            XSString(
                maybeValue.xmlString
            )
        )
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        require(value is XSDurationInst)
        require(value.months == 0L)
        val days = value.millis / (24 * 3600_000)
        require(days == 0L)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        validateValue(value(representation), version)
    }
}

object YearMonthDurationType : PrimitiveDatatype<XSDurationInst>("yearMonthDuration", XSD_NS_URI) {
    override val baseType: DurationType get() = DurationType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
        patterns = listOf(ResolvedPattern(XSPattern("[^DT]*"), SchemaVersion.V1_1,)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XSString): XSDurationInst {
        return XSDurationInst(normalized.xmlString)
    }

    override fun value(maybeValue: XSAnySimple): XSDurationInst {
        return maybeValue as? XSDurationInst ?: value(
            XSString(
                maybeValue.xmlString
            )
        )
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        require(value is XSDurationInst)
        val seconds = value.millis % (24 * 3600_000)
        require(seconds == 0L)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        validateValue(value(representation), version)
    }
}

object FloatType : PrimitiveDatatype<XSFloatImpl>("float", XSD_NS_URI) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun valueFromNormalized(normalized: XSString): XSFloatImpl {
        return VFloat(normalized.toFloat())
    }

    override fun value(maybeValue: XSAnySimple): XSFloatImpl {
        return maybeValue as? XSFloatImpl
            ?: value(XSString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSFloatImpl)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        val _ = value(representation)
    }

}

object GDayType : PrimitiveDatatype<XSGDayImpl>("gDay", XSD_NS_URI), FiniteDateType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
        explicitTimezone = ResolvedExplicitTimezone(
            XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL)
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XSString): XSGDayImpl {
        val s = normalized.xmlString
        require(s.startsWith("---"))
        val tzIndex = s.indexOf('Z', 3)
        return when {
            tzIndex < 0 -> XSGDayImpl(
                s.substring(3).toInt()
            )
            else -> XSGDayImpl(
                s.substring(
                    3,
                    tzIndex
                ).toInt(),
                XSDateTime.timezoneFragValue(
                    s.substring(tzIndex)
                )
            )
        }
    }

    override fun value(maybeValue: XSAnySimple): XSGDayImpl {
        return maybeValue as? XSGDayImpl
            ?: value(XSString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSGDayImpl)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        val _ = value(representation)
    }
}

object GMonthType : PrimitiveDatatype<XSGMonthImpl>("gMonth", XSD_NS_URI), FiniteDateType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
        explicitTimezone = ResolvedExplicitTimezone(
            XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL)
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XSString): XSGMonthImpl {
        val s = normalized.xmlString
        require(s.startsWith("--"))
        val month = s.substring(2, 4).toInt()

        if (s.length == 4) {
            return XSGMonthImpl(month)
        } else { // Handle bogus month format with trailing dashes (non-standard compliant, error in xmlschema older versions).
            if (s.length > 5 && s[4] == '-' && s[5] == '-') error("Trailing slashes in month format (not compliant)")
            val tz = s.substring(4)
            val tzOffset = XSDateTime.timezoneFragValue(tz)
            return XSGMonthImpl(month, tzOffset)
        }
    }

    override fun value(maybeValue: XSAnySimple): XSGMonthImpl {
        return maybeValue as? XSGMonthImpl
            ?: value(XSString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSGMonthImpl)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        val _ = value(representation)
    }
}

object GMonthDayType : PrimitiveDatatype<XSMonthDayImpl>("gMonthDay", XSD_NS_URI), FiniteDateType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
        explicitTimezone = ResolvedExplicitTimezone(
            XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL)
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XSString): XSMonthDayImpl {
        val s = normalized.xmlString
        require(s.startsWith("--"))
        val tzIndex = s.indexOf('Z', 2)
        return when {
            tzIndex < 0 -> {
                val (month, day) = s.substring(2).split('-').map { it.toUInt() }
                XSMonthDayImpl(month, day)
            }

            else -> {
                val tz = XSDateTime.timezoneFragValue(s.substring(tzIndex))
                val (month, day) = s.substring(2, tzIndex).split('-').map { it.toUInt() }
                XSMonthDayImpl(month, day, tz)
            }
        }
    }

    override fun value(maybeValue: XSAnySimple): XSMonthDayImpl {
        return maybeValue as? XSMonthDayImpl ?: value(
            XSString(maybeValue.xmlString)
        )
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSMonthDayImpl)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        val _ = value(representation)
    }
}

object GYearType : PrimitiveDatatype<XSGYear>("gYear", XSD_NS_URI), FiniteDateType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
        explicitTimezone = ResolvedExplicitTimezone(
            XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL)
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XSString): XSGYear {
        val s = normalized.xmlString
        val yearEnd = s.substring(1).indexOfFirst { it !in '0'..'9' }.let { if (it >= 0) it + 1 else s.length }
        val year = s.substring(0, yearEnd).toInt()
        val tzOffset = XSDateTime.timezoneFragValue(s.substring(yearEnd))
        return XSGYear(year, tzOffset)
    }

    override fun value(maybeValue: XSAnySimple): XSGYear {
        return maybeValue as? XSGYear
            ?: value(XSString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSGYear)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        val _ = value(representation)
    }
}

object GYearMonthType : PrimitiveDatatype<XSGYearMonth>("gYearMonth", XSD_NS_URI), FiniteDateType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
        explicitTimezone = ResolvedExplicitTimezone(
            XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL)
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XSString): XSGYearMonth {
        val (year, month) = normalized.split('-').map { it.toInt() }
        return XSGYearMonth(
            year,
            month.toUInt()
        )
    }

    override fun value(maybeValue: XSAnySimple): XSGYearMonth {
        return maybeValue as? XSGYearMonth ?: value(
            XSString(maybeValue.xmlString)
        )
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSGYearMonth)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        val _ = value(representation)
    }

}

object HexBinaryType : PrimitiveDatatype<XSByteArrayImpl>("hexBinary", XSD_NS_URI) {
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
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XSString): XSByteArrayImpl {
        require(normalized.length % 2 == 0) { "Hex must have even amount of characters" }
        val b = ByteArray(normalized.length / 2) { normalized.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
        return VByteArray(b)
    }

    override fun value(maybeValue: XSAnySimple): XSByteArrayImpl {
        return maybeValue as? XSByteArrayImpl ?: value(
            XSString(maybeValue.xmlString)
        )
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSByteArrayImpl) { "Value for hex binary is not a ByteArray" }
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        validateValue(value(representation), version)
    }
}

object NotationType : PrimitiveDatatype<VNotation>("NOTATION", XSD_NS_URI) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XSString): VNotation {
        return VNotation(normalized)
    }

    override fun value(maybeValue: XSAnySimple): VNotation {
        return maybeValue as? VNotation ?: value(
            XSString(
                maybeValue.xmlString
            )
        )
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is VNotation)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {

    }
}

object QNameType : PrimitiveDatatype<VQName>("QName", XSD_NS_URI) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XSString): VQName {
        return (normalized as? XSPrefixString)?.toVQName() ?: VQName(
            normalized.xmlString
        )
    }

    override fun value(maybeValue: XSAnySimple): VQName {
        return maybeValue as? VQName
            ?: value(XSString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is VQName)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        val localName = when (representation) {
            is XSPrefixString -> representation.localname
            else -> representation.xmlString
        }
        check(localName.indexOf(':') < 0) { "local names cannot contain : characters" }
    }
}

object StringType : PrimitiveDatatype<XSString>("string", XSD_NS_URI), IStringType {
    override val baseType: AnyAtomicType get() = AnyAtomicType
    override val simpleDerivation: ResolvedSimpleRestrictionBase
        get() = SimpleBuiltinRestriction(
            baseType,
            BuiltinSchemaXmlschema,
            listOf(XSWhiteSpace(WhitespaceValue.PRESERVE, fixed = false))
        )

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.PRESERVE)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XSString): XSString {
        return normalized
    }

    override fun value(maybeValue: XSAnySimple): XSString {
        return maybeValue as? XSString
            ?: XSString(maybeValue.xmlString)
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSString)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {}
}

object NormalizedStringType : PrimitiveDatatype<XSNormalizedString>("normalizedString", XSD_NS_URI), IStringType {
    override val baseType: StringType get() = StringType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.REPLACE)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XSString): XSNormalizedString {
        return normalized as? XSNormalizedString
            ?: XSNormalizedString(normalized.xmlString)
    }

    override fun value(maybeValue: XSAnySimple): XSNormalizedString {
        return maybeValue as? XSNormalizedString ?: value(
            XSString(maybeValue.xmlString)
        )
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSNormalizedString)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        // all representations are valid
    }
}

object TokenType : PrimitiveDatatype<XSToken>("token", XSD_NS_URI), IStringType {
    override val baseType: NormalizedStringType get() = NormalizedStringType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XSString): XSToken {
        return normalized as? XSToken
            ?: XSToken(normalized.xmlString)
    }

    override fun value(maybeValue: XSAnySimple): XSToken {
        return maybeValue as? XSToken
            ?: value(XSString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSToken)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        mdlFacets.validate(this, representation)
    }
}

object LanguageType : PrimitiveDatatype<XSLanguageImpl>("language", XSD_NS_URI), IStringType {
    override val baseType: TokenType get() = TokenType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE)),
        patterns = listOf(ResolvedPattern(XSPattern("[a-zA-Z]{1,8}(-[a-zA-Z0-9]{1,8})*"), SchemaVersion.V1_1,)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XSString): XSLanguageImpl {
        return XSLanguageImpl(normalized.xmlString)
    }

    override fun value(maybeValue: XSAnySimple): XSLanguageImpl {
        return maybeValue as? XSLanguageImpl ?: XSLanguageImpl(maybeValue.xmlString)
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        mdlFacets.validateValue(value as XSLanguageImpl)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        val _ = value(representation) // triggers validation in constructor
    }
}

object NameType : PrimitiveDatatype<XSName>("Name", XSD_NS_URI), IStringType {
    override val baseType: TokenType get() = TokenType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE)),
        patterns = listOf(ResolvedPattern(XSPattern("\\i\\c*"), SchemaVersion.V1_1,)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XSString): XSName {
        return normalized as? XSName
            ?: XSName(normalized.xmlString)
    }

    override fun value(maybeValue: XSAnySimple): XSName {
        return maybeValue as? XSName
            ?: value(XSString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSName)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        val _ = value(representation)
    }
}

object NCNameType : PrimitiveDatatype<XSNCName>("NCName", XSD_NS_URI), IStringType {
    override val baseType: NameType get() = NameType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE)),
        patterns = listOf(
            ResolvedPattern(XSPattern("\\i\\c*"), SchemaVersion.V1_1,),
            ResolvedPattern(XSPattern("[\\i-[:]][\\c-[:]]*"), SchemaVersion.V1_1,)
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XSString): XSNCName {
        return normalized as? XSNCName
            ?: XSNCName(normalized.xmlString)
    }

    override fun value(maybeValue: XSAnySimple): XSNCName {
        return maybeValue as? XSNCName
            ?: value(XSString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSNCName)
        mdlFacets.validate(mdlPrimitiveTypeDefinition, value)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        mdlFacets.validate(mdlPrimitiveTypeDefinition, representation)
    }

}

object EntityType : PrimitiveDatatype<XSString>("ENTITY", XSD_NS_URI), IStringType {
    override val baseType: NCNameType get() = NCNameType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE)),
        patterns = listOf(
            ResolvedPattern(XSPattern("\\i\\c*"), SchemaVersion.V1_1,),
            ResolvedPattern(XSPattern("[\\i-[:]][\\c-[:]]*"), SchemaVersion.V1_1,)
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XSString): XSString {
        return normalized
    }

    override fun value(maybeValue: XSAnySimple): XSString {
        return maybeValue as? XSString
            ?: XSString(maybeValue.xmlString)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        mdlFacets.validate(this, representation)
    }
}

object IDType : PrimitiveDatatype<XSIDImpl>("ID", XSD_NS_URI), IStringType {
    override val baseType: NCNameType get() = NCNameType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE)),
        patterns = listOf(
            ResolvedPattern(XSPattern("\\i\\c*"), SchemaVersion.V1_1,),
            ResolvedPattern(XSPattern("[\\i-[:]][\\c-[:]]*"), SchemaVersion.V1_1,)
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XSString): XSIDImpl {
        return normalized as? XSIDImpl
            ?: XSIDImpl(normalized.xmlString)
    }

    override fun value(maybeValue: XSAnySimple): XSIDImpl {
        return maybeValue as? XSIDImpl
            ?: value(XSString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSIDImpl)
        mdlFacets.validateValue(value)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        validateValue(value(representation), version)
    }
}

object IDRefType : PrimitiveDatatype<XSIDRefImpl>("IDREF", XSD_NS_URI), IStringType {
    override val baseType: NCNameType get() = NCNameType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE)),
        patterns = listOf(
            ResolvedPattern(XSPattern("\\i\\c*"), SchemaVersion.V1_1,),
            ResolvedPattern(XSPattern("[\\i-[:]][\\c-[:]]*"), SchemaVersion.V1_1,)
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XSString): XSIDRefImpl {
        return normalized as? XSIDRefImpl
            ?: XSIDRefImpl(normalized.xmlString)
    }

    override fun value(maybeValue: XSAnySimple): XSIDRefImpl {
        return maybeValue as? XSIDRefImpl
            ?: value(XSString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XSIDRefImpl) {"$value is not an IDRef"}
        check(value.isNotEmpty()) { "IDRef may not be empty" }
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        val _ = value(representation)
    }
}

object NMTokenType : PrimitiveDatatype<VNMToken>("NMTOKEN", XSD_NS_URI), IStringType {
    override val baseType: TokenType get() = TokenType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE)),
        patterns = listOf(ResolvedPattern(XSPattern("\\c+"), SchemaVersion.V1_1,)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XSString): VNMToken {
        return normalized as? VNMToken
            ?: VNMToken(normalized.xmlString)
    }

    override fun value(maybeValue: XSAnySimple): VNMToken {
        return maybeValue as? VNMToken
            ?: value(XSString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is VNMToken)
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        val _ = value(representation)
    }
}

object TimeType : PrimitiveDatatype<VTime>("time", XSD_NS_URI) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
        explicitTimezone = ResolvedExplicitTimezone(
            XSExplicitTimezone(XSExplicitTimezone.Value.OPTIONAL)
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XSString): VTime {
        return VTime(normalized.xmlString)
    }

    override fun value(maybeValue: XSAnySimple): VTime {
        return maybeValue as? VTime
            ?: value(XSString(maybeValue.xmlString))
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        validateValue(value(representation), version)
    }
}

object EntitiesType :
    ConstructedListDatatype(
        "ENTITIES",
        XSD_NS_URI,
        EntityType,
        BuiltinSchemaXmlschema,
    ) {
    override val mdlItemTypeDefinition: ResolvedSimpleType
        get() = EntityType

    override fun validate(representation: XSString, version: SchemaVersion) {}
}

object IDRefsType : ConstructedListDatatype(
    "IDREFS",
    XSD_NS_URI,
    EntityType,
    BuiltinSchemaXmlschema
) {
    override val mdlItemTypeDefinition: ResolvedSimpleType
        get() = IDRefType

    override fun validate(representation: XSString, version: SchemaVersion) {}
}

object NMTokensType : ConstructedListDatatype(
    "NMTOKENS",
    XSD_NS_URI,
    EntityType,
    BuiltinSchemaXmlschema
) {
    override val mdlItemTypeDefinition: ResolvedSimpleType
        get() = NMTokenType

    override fun validate(representation: XSString, version: SchemaVersion) {}
}

object PrecisionDecimalType : PrimitiveDatatype<XSAnySimple>("precisionDecimal", XSD_NS_URI) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

    override fun valueFromNormalized(normalized: XSString): XSAnySimple {
        TODO("NOT IMPLEMENTED")
    }

    override fun value(maybeValue: XSAnySimple): XSAnySimple {
        TODO("not implemented")
    }

    override fun validate(representation: XSString, version: SchemaVersion) {
        mdlFacets.validate(this, representation)
//        TODO("not implemented")
    }
}
