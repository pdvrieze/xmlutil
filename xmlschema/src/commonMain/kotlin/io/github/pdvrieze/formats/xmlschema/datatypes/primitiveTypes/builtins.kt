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
import nl.adaptivity.xmlutil.xmlCollapseWhitespace
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

sealed interface IStringType : ResolvedBuiltinSimpleType {
    fun value(representation: VString): VString
}

sealed interface IDecimalType : ResolvedBuiltinSimpleType {
    fun value(representation: VString): VDecimal
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

    override fun toString(): String = "Builtin:$name"

}

sealed class PrimitiveDatatype(name: String, targetNamespace: String) : AtomicDatatype(name, targetNamespace) {
    abstract fun value(representation: VString): VAnySimpleType
    abstract fun value(maybeValue: VAnySimpleType): VAnySimpleType

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

    override fun validate(representation: VString) {
        error("Atomic is not directly usable")
    }
}

object AnyURIType : PrimitiveDatatype("anyURI", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType
    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema)
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: VString): VAnyURI = VAnyURI(representation)

    override fun value(maybeValue: VAnySimpleType): VAnyURI {
        return maybeValue as? VAnyURI ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VAnyURI)
    }

    override fun validate(representation: VString) {
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
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: VString): VByteArray {
        return VByteArray(Base64.decode(representation))
    }

    override fun value(maybeValue: VAnySimpleType): VByteArray {
        return maybeValue as? VByteArray ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VByteArray)
    }

    override fun validate(representation: VString) {
        value(representation)
    }
}

object BooleanType : PrimitiveDatatype("boolean", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.FINITE,
        numeric = false,
    )

    override fun value(representation: VString): VBoolean = when (representation.toString()) {
        "true", "1" -> VBoolean.TRUE
        "false", "0" -> VBoolean.FALSE
        else -> error("$representation is not a boolean")
    }

    override fun value(maybeValue: VAnySimpleType): VBoolean {
        return maybeValue as? VBoolean ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VBoolean)
    }

    override fun validate(representation: VString) {
        value(representation)
    }
}

interface FiniteDateType : ResolvedBuiltinType

object DateType : PrimitiveDatatype("date", XmlSchemaConstants.XS_NAMESPACE), FiniteDateType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
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

    override fun value(representation: VString): VDate {
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

    override fun validateValue(representation: Any) {
        check(representation is VDate)
    }

    override fun validate(representation: VString) {
        value(representation)
    }
}

object DateTimeType : PrimitiveDatatype("dateTime", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
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

    override fun value(representation: VString): VDateTime {
        val s = representation.xmlString
        val tIndex = s.indexOf('T')
        val zIndex = s.indexOf('Z', tIndex + 1)
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
        // TODO: validate date time
        value(representation)
    }
}

object DateTimeStampType : PrimitiveDatatype("dateTimeStamp", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: DateTimeType get() = DateTimeType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
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

    override fun value(representation: VString): VDateTime {
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
        value(representation)
    }
}

object DecimalType : PrimitiveDatatype("decimal", XmlSchemaConstants.XS_NAMESPACE), IDecimalType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

    override fun value(representation: VString): VDecimal {
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

    override fun validateValue(representation: Any) {
        check(representation is VDecimal)
    }

    override fun validate(representation: VString) {
        value(representation)
    }

}

sealed interface IIntegerType : IDecimalType

object IntegerType : PrimitiveDatatype("integer", XmlSchemaConstants.XS_NAMESPACE), IIntegerType {
    override val baseType: DecimalType get() = DecimalType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinSchemaXmlschema)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

    override fun value(representation: VString): VInteger {
        return VInteger(representation.toLong())
    }

    override fun value(maybeValue: VAnySimpleType): VInteger {
        return maybeValue as? VInteger ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VInteger)
    }

    override fun validate(representation: VString) {
        //TODO("not implemented")
    }
}

object LongType : PrimitiveDatatype("long", XmlSchemaConstants.XS_NAMESPACE), IIntegerType {
    override val baseType: IntegerType get() = IntegerType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinSchemaXmlschema)),
        maxConstraint = ResolvedMaxInclusive(
            XSMaxInclusive(VString(Long.MAX_VALUE.toString())),
            BuiltinSchemaXmlschema,
            LongType
        ),
        minConstraint = ResolvedMinInclusive(
            XSMinInclusive(VString(Long.MIN_VALUE.toString())),
            BuiltinSchemaXmlschema,
            LongType
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun value(representation: VString): VInteger {
        return VInteger(representation.toLong())
    }

    override fun value(maybeValue: VAnySimpleType): VInteger {
        return maybeValue as? VInteger ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VInteger)
    }

    override fun validate(representation: VString) {
        //TODO("not implemented")
    }
}

object IntType : PrimitiveDatatype("int", XmlSchemaConstants.XS_NAMESPACE), IIntegerType {
    override val baseType: LongType get() = LongType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinSchemaXmlschema)),
        maxConstraint = ResolvedMaxInclusive(
            XSMaxInclusive(VString(Int.MAX_VALUE.toString())),
            BuiltinSchemaXmlschema,
            IntType
        ),
        minConstraint = ResolvedMinInclusive(
            XSMinInclusive(VString(Int.MIN_VALUE.toString())),
            BuiltinSchemaXmlschema,
            IntType
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun value(representation: VString): VInteger {
        return VInteger(representation.toLong())
    }

    override fun value(maybeValue: VAnySimpleType): VInteger {
        return maybeValue as? VInteger ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VInteger)
    }

    override fun validate(representation: VString) {
        //TODO("not implemented")
    }

}

object ShortType : PrimitiveDatatype("short", XmlSchemaConstants.XS_NAMESPACE), IIntegerType {
    override val baseType: IntType get() = IntType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinSchemaXmlschema)),
        maxConstraint = ResolvedMaxInclusive(XSMaxInclusive(VString("32767")), BuiltinSchemaXmlschema, ShortType),
        minConstraint = ResolvedMinInclusive(
            XSMinInclusive(VString("-32768")),
            BuiltinSchemaXmlschema,
            ShortType
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun value(representation: VString): VInteger {
        return VInteger(representation.toInt())
    }

    override fun value(maybeValue: VAnySimpleType): VInteger {
        return maybeValue as? VInteger ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VInteger)
    }

    override fun validate(representation: VString) {
        //TODO("not implemented")
    }

}

object ByteType : PrimitiveDatatype("byte", XmlSchemaConstants.XS_NAMESPACE), IIntegerType {
    override val baseType: ShortType get() = ShortType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinSchemaXmlschema)),
        maxConstraint = ResolvedMaxInclusive(XSMaxInclusive(VString("127")), BuiltinSchemaXmlschema, ByteType),
        minConstraint = ResolvedMinInclusive(XSMinInclusive(VString("-128")), BuiltinSchemaXmlschema, ByteType),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun value(representation: VString): VInteger {
        return VInteger(representation.toInt())
    }

    override fun value(maybeValue: VAnySimpleType): VInteger {
        return maybeValue as? VInteger ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VInteger)
    }

    override fun validate(representation: VString) {
        //TODO("not implemented")
    }

    override fun toString(): String = "Builtin:Byte"
}

object NonNegativeIntegerType : PrimitiveDatatype("nonNegativeInteger", XmlSchemaConstants.XS_NAMESPACE), IIntegerType {
    override val baseType: IntegerType get() = IntegerType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinSchemaXmlschema)),
        minConstraint = ResolvedMinInclusive(
            XSMinInclusive(VString("0")),
            BuiltinSchemaXmlschema,
            NonNegativeIntegerType
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

    override fun value(representation: VString): VNonNegativeInteger {
        return VNonNegativeInteger(representation)
    }

    override fun value(maybeValue: VAnySimpleType): VNonNegativeInteger {
        return maybeValue as? VNonNegativeInteger ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VNonNegativeInteger)
    }

    override fun validate(representation: VString) {
        //TODO("not implemented")
    }

}

object PositiveIntegerType : PrimitiveDatatype("positiveInteger", XmlSchemaConstants.XS_NAMESPACE), IIntegerType {
    override val baseType: NonNegativeIntegerType get() = NonNegativeIntegerType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinSchemaXmlschema)),
        minConstraint = ResolvedMinInclusive(XSMinInclusive(VString("1")), BuiltinSchemaXmlschema, PositiveIntegerType),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

    override fun value(representation: VString): VNonNegativeInteger {
        return VNonNegativeInteger(representation)
    }

    override fun value(maybeValue: VAnySimpleType): VNonNegativeInteger {
        return maybeValue as? VNonNegativeInteger ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VNonNegativeInteger)
    }

    override fun validate(representation: VString) {
        //TODO("not implemented")
    }

}

object UnsignedLongType : PrimitiveDatatype("unsignedLong", XmlSchemaConstants.XS_NAMESPACE), IIntegerType {
    override val baseType: NonNegativeIntegerType get() = NonNegativeIntegerType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinSchemaXmlschema)),
        maxConstraint = ResolvedMaxInclusive(
            XSMaxInclusive(VString(ULong.MAX_VALUE.toString())),
            BuiltinSchemaXmlschema,
            UnsignedLongType
        ),
        minConstraint = ResolvedMinInclusive(XSMinInclusive(VString("0")), BuiltinSchemaXmlschema, UnsignedLongType),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun value(representation: VString): VUnsignedLong {
        return VUnsignedLong(representation.toULong())
    }

    override fun value(maybeValue: VAnySimpleType): VUnsignedLong {
        return maybeValue as? VUnsignedLong ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VUnsignedLong)
    }

    override fun validate(representation: VString) {
        //TODO("not implemented")
    }

}

object UnsignedIntType : PrimitiveDatatype("unsignedInt", XmlSchemaConstants.XS_NAMESPACE), IIntegerType {
    override val baseType: UnsignedLongType get() = UnsignedLongType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinSchemaXmlschema)),
        maxConstraint = ResolvedMaxInclusive(
            XSMaxInclusive(VString(UInt.MAX_VALUE.toString())),
            BuiltinSchemaXmlschema,
            UnsignedIntType
        ),
        minConstraint = ResolvedMinInclusive(XSMinInclusive(VString("0")), BuiltinSchemaXmlschema, UnsignedIntType),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun value(representation: VString): VUnsignedInt {
        return VUnsignedInt(representation.toUInt())
    }

    override fun value(maybeValue: VAnySimpleType): VUnsignedInt {
        return maybeValue as? VUnsignedInt ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VUnsignedInt)
    }

    override fun validate(representation: VString) {
        //TODO("not implemented")
        validateValue(value(representation))
    }

}

object UnsignedShortType : PrimitiveDatatype("unsignedShort", XmlSchemaConstants.XS_NAMESPACE), IIntegerType {
    override val baseType: UnsignedIntType get() = UnsignedIntType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinSchemaXmlschema)),
        maxConstraint = ResolvedMaxInclusive(
            XSMaxInclusive(VString("65535")),
            BuiltinSchemaXmlschema,
            UnsignedShortType
        ),
        minConstraint = ResolvedMinInclusive(XSMinInclusive(VString("0")), BuiltinSchemaXmlschema, UnsignedShortType),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun value(representation: VString): VUnsignedInt {
        return VUnsignedInt(representation.toUInt())
    }

    override fun value(maybeValue: VAnySimpleType): VUnsignedInt {
        return maybeValue as? VUnsignedInt ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VUnsignedInt)
    }

    override fun validate(representation: VString) {
        validateValue(value(representation))
    }

}

object UnsignedByteType : PrimitiveDatatype("unsignedByte", XmlSchemaConstants.XS_NAMESPACE), IIntegerType {
    override val baseType: UnsignedShortType get() = UnsignedShortType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinSchemaXmlschema)),
        maxConstraint = ResolvedMaxInclusive(
            XSMaxInclusive(VString("255")),
            BuiltinSchemaXmlschema,
            UnsignedByteType
        ),
        minConstraint = ResolvedMinInclusive(XSMinInclusive(VString("0")), BuiltinSchemaXmlschema, UnsignedByteType),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun value(representation: VString): VUnsignedInt {
        return VUnsignedInt(representation.toUInt())
    }

    override fun value(maybeValue: VAnySimpleType): VUnsignedInt {
        return maybeValue as? VUnsignedInt ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VUnsignedInt)
    }

    override fun validate(representation: VString) {
        validateValue(value(representation))
    }

}

object NonPositiveIntegerType : PrimitiveDatatype("nonPositiveInteger", XmlSchemaConstants.XS_NAMESPACE), IIntegerType {
    override val baseType: IntegerType get() = IntegerType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinSchemaXmlschema)),
        maxConstraint = ResolvedMaxInclusive(
            XSMaxInclusive(VString("0")),
            BuiltinSchemaXmlschema,
            NonPositiveIntegerType
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

    override fun value(representation: VString): VDecimal {
        return when (representation.toLong()) {
            in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() ->
                VInteger(representation.toLong().toInt())

            else -> VInteger(representation.toLong())
        }
    }

    override fun value(maybeValue: VAnySimpleType): VDecimal {
        return maybeValue as? VDecimal ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VDecimal)
    }

    override fun validate(representation: VString) {
        check(value(representation).toLong() <= 0L)
    }

}

object NegativeIntegerType : PrimitiveDatatype("negativeInteger", XmlSchemaConstants.XS_NAMESPACE), IIntegerType {
    override val baseType: NonPositiveIntegerType get() = NonPositiveIntegerType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), BuiltinSchemaXmlschema)),
        maxConstraint = ResolvedMaxInclusive(
            XSMaxInclusive(VString("-1")),
            BuiltinSchemaXmlschema,
            NegativeIntegerType
        ),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.TOTAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

    override fun value(representation: VString): VDecimal {
        return when (representation.toLong()) {
            in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() ->
                VInteger(representation.toLong().toInt())

            else -> VInteger(representation.toLong())
        }
    }

    override fun value(maybeValue: VAnySimpleType): VDecimal {
        return maybeValue as? VDecimal ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VDecimal)
        check(representation.toLong() < 0L)
    }

    override fun validate(representation: VString) {
        check(value(representation).toLong() < 0L)
    }

}

object DoubleType : PrimitiveDatatype("double", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun value(representation: VString): VDouble {
        return VDouble(representation.toDouble())
    }

    override fun value(maybeValue: VAnySimpleType): VDouble {
        return maybeValue as? VDouble ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VDouble)
    }

    override fun validate(representation: VString) {
        value(representation)
    }

}

object DurationType : PrimitiveDatatype("duration", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: VString): VDuration {
        return VDuration(representation.xmlString)
    }

    override fun value(maybeValue: VAnySimpleType): VDuration {
        return maybeValue as? VDuration ?: value(VString(maybeValue.xmlString))
    }

    override fun validate(representation: VString) {
        value(representation)
    }
}

object DayTimeDurationType : PrimitiveDatatype("dayTimeDuration", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: DurationType get() = DurationType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[^YM]*(T.*)?"), BuiltinSchemaXmlschema)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: VString): VAnySimpleType {
        TODO("not implemented")
    }

    override fun value(maybeValue: VAnySimpleType): VAnySimpleType {
        TODO("not implemented")
    }

    override fun validate(representation: VString) {
//        TODO("not implemented")
    }
}

object YearMonthDurationType : PrimitiveDatatype("yearMonthDuration", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: DurationType get() = DurationType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[^DT]*"), BuiltinSchemaXmlschema)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: VString): VGYearMonth {
        val s = representation.xmlString
        val tzIndex = s.indexOf('Z')
        if (tzIndex < 0) {
            val (year, month) = s.split('-').map { it.toInt() }
            return VGYearMonth(year, month)
        } else {
            val (year, month) = s.substring(0, tzIndex).split('-').map { it.toInt() }
            val tz = IDateTime.timezoneFragValue(s.substring(tzIndex))
            return VGYearMonth(year, month, tz)
        }
    }

    override fun value(maybeValue: VAnySimpleType): VGYearMonth {
        return maybeValue as? VGYearMonth ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VGYearMonth)
    }

    override fun validate(representation: VString) {
        TODO("not implemented")
    }
}

object FloatType : PrimitiveDatatype("float", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = true,
        cardinality = Cardinality.FINITE,
        numeric = true,
    )

    override fun value(representation: VString): VFloat {
        return VFloat(representation.toFloat())
    }

    override fun value(maybeValue: VAnySimpleType): VFloat {
        return maybeValue as? VFloat ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VFloat)
    }

    override fun validate(representation: VString) {
        value(representation)
    }

}

object GDayType : PrimitiveDatatype("gDay", XmlSchemaConstants.XS_NAMESPACE), FiniteDateType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
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

    override fun value(representation: VString): VGDay {
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

    override fun validateValue(representation: Any) {
        check(representation is VGDay)
    }

    override fun validate(representation: VString) {
        value(representation)
    }
}

object GMonthType : PrimitiveDatatype("gMonth", XmlSchemaConstants.XS_NAMESPACE), FiniteDateType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
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

    override fun value(representation: VString): VGMonth {
        val s = representation.xmlString
        require(s.startsWith("--"))
        val month = s.substring(2, 4).toInt()

        if (s.length == 4) {
            return VGMonth(month)
        } else { // Handle bogus month format with trailing dashes (non-standard compliant, error in xmlschema older versions).
            val tz = if (s.length > 5 && s[4] == '-' && s[5] == '-') s.substring(6) else s.substring(4)
            val tzOffset = IDateTime.timezoneFragValue(tz)
            return VGMonth(month, tzOffset)
        }
    }

    override fun value(maybeValue: VAnySimpleType): VGMonth {
        return maybeValue as? VGMonth ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VGMonth)
    }

    override fun validate(representation: VString) {
        value(representation)
    }
}

object GMonthDayType : PrimitiveDatatype("gMonthDay", XmlSchemaConstants.XS_NAMESPACE), FiniteDateType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
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

    override fun value(representation: VString): VGMonthDay {
        val s = representation.xmlString
        require(s.startsWith("--"))
        val tzIndex = s.indexOf('Z', 2)
        return when {
            tzIndex < 0 -> {
                val (month, day) = s.substring(2).split('-').map { it.toInt() }
                VGMonthDay(month, day)
            }

            else -> {
                val tz = IDateTime.timezoneFragValue(s.substring(tzIndex))
                val (month, day) = s.substring(2, tzIndex).split('-').map { it.toInt() }
                VGMonthDay(month, day, tz)
            }
        }
    }

    override fun value(maybeValue: VAnySimpleType): VGMonthDay {
        return maybeValue as? VGMonthDay ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VGMonthDay)
    }

    override fun validate(representation: VString) {
        value(representation)
    }
}

object GYearType : PrimitiveDatatype("gYear", XmlSchemaConstants.XS_NAMESPACE), FiniteDateType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
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

    override fun value(representation: VString): VGYear {
        val s = representation.xmlString
        val yearEnd = s.substring(1).indexOfFirst { it !in '0'..'9' }.let { if (it >= 0) it + 1 else s.length }
        val year = s.substring(0, yearEnd).toInt()
        val tzOffset = IDateTime.timezoneFragValue(s.substring(yearEnd))
        return VGYear(year, tzOffset)
    }

    override fun value(maybeValue: VAnySimpleType): VGYear {
        return maybeValue as? VGYear ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VGYear)
    }

    override fun validate(representation: VString) {
        value(representation)
    }
}

object GYearMonthType : PrimitiveDatatype("gYearMonth", XmlSchemaConstants.XS_NAMESPACE), FiniteDateType {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
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

    override fun value(representation: VString): VGYearMonth {
        val (year, month) = representation.split('-').map { it.toInt() }
        return VGYearMonth(year, month)
    }

    override fun value(maybeValue: VAnySimpleType): VGYearMonth {
        return maybeValue as? VGYearMonth ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VGYearMonth)
    }

    override fun validate(representation: VString) {
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
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: VString): VByteArray {
        require(representation.length % 2 == 0) { "Hex must have even amount of characters" }
        val b = ByteArray(representation.length / 2) { representation.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
        return VByteArray(b)
    }

    override fun value(maybeValue: VAnySimpleType): VByteArray {
        return maybeValue as? VByteArray ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is ByteArray)
    }

    override fun validate(representation: VString) {
        TODO("not implemented")
    }
}

object NotationType : PrimitiveDatatype("NOTATION", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: VString): VNotation {
        return VNotation(representation)
    }

    override fun value(maybeValue: VAnySimpleType): VNotation {
        return maybeValue as? VNotation ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VNotation)
    }

    override fun validate(representation: VString) {

    }
}

object QNameType : PrimitiveDatatype("QName", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: VString): VQName {
        return (representation as? VPrefixString)?.toVQName() ?: VQName(representation.xmlString)
    }

    override fun value(maybeValue: VAnySimpleType): VQName {
        return maybeValue as? VQName ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VQName)
    }

    override fun validate(representation: VString) {
        val localName = when (representation) {
            is VPrefixString -> representation.localname
            else -> representation.xmlString
        }
        check(localName.indexOf(':') < 0) { "local names cannot contain : characters" }
    }
}

object StringType : PrimitiveDatatype("string", XmlSchemaConstants.XS_NAMESPACE), IStringType {
    override val baseType: AnyAtomicType get() = AnyAtomicType
    override val simpleDerivation: ResolvedSimpleRestrictionBase
        get() = SimpleBuiltinRestriction(baseType, listOf(XSWhiteSpace(XSWhiteSpace.Values.PRESERVE, fixed = false)))

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.PRESERVE), BuiltinSchemaXmlschema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: VString): VString {
        return representation
    }

    override fun value(maybeValue: VAnySimpleType): VString {
        return maybeValue as? VString ?: VString(maybeValue.xmlString)
    }

    override fun validateValue(representation: Any) {
        check(representation is VString)
    }

    override fun validate(representation: VString) {}
}

object NormalizedStringType : PrimitiveDatatype("normalizedString", XmlSchemaConstants.XS_NAMESPACE), IStringType {
    override val baseType: StringType get() = StringType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.REPLACE), BuiltinSchemaXmlschema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: VString): VNormalizedString {
        return representation as? VNormalizedString ?: VNormalizedString(representation.xmlString)
    }

    override fun value(maybeValue: VAnySimpleType): VNormalizedString {
        return maybeValue as? VNormalizedString ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VNormalizedString)
    }

    override fun validate(representation: VString) {
        // TODO("not implemented")
    }
}

object TokenType : PrimitiveDatatype("token", XmlSchemaConstants.XS_NAMESPACE), IStringType {
    override val baseType: NormalizedStringType get() = NormalizedStringType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE), BuiltinSchemaXmlschema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: VString): VToken {
        return representation as? VToken ?: VToken(representation.xmlString)
    }

    override fun value(maybeValue: VAnySimpleType): VToken {
        return maybeValue as? VToken ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VToken)
    }

    override fun validate(representation: VString) {
//        TODO("not implemented")
    }
}

object LanguageType : PrimitiveDatatype("language", XmlSchemaConstants.XS_NAMESPACE), IStringType {
    override val baseType: TokenType get() = TokenType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("[a-zA-Z]{1,8}(-[a-zA-Z0-9]{1,8})*"), BuiltinSchemaXmlschema)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: VString): VString {
        return representation
    }

    override fun value(maybeValue: VAnySimpleType): VString {
        return maybeValue as? VString ?: VString(maybeValue.xmlString)
    }

    override fun validate(representation: VString) {
//        TODO("not implemented")
    }
}

object NameType : PrimitiveDatatype("Name", XmlSchemaConstants.XS_NAMESPACE), IStringType {
    override val baseType: TokenType get() = TokenType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("\\i\\c*"), BuiltinSchemaXmlschema)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: VString): VName {
        return representation as? VName ?: VName(representation.xmlString)
    }

    override fun value(maybeValue: VAnySimpleType): VName {
        return maybeValue as? VName ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VName)
    }

    override fun validate(representation: VString) {
        value(representation)
    }
}

object NCNameType : PrimitiveDatatype("NCName", XmlSchemaConstants.XS_NAMESPACE), IStringType {
    override val baseType: NameType get() = NameType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE), BuiltinSchemaXmlschema),
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

    override fun value(representation: VString): VNCName {
        return representation as? VNCName ?: VNCName(representation.xmlString)
    }

    override fun value(maybeValue: VAnySimpleType): VNCName {
        return maybeValue as? VNCName ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VNCName)
    }

    override fun validate(representation: VString) {
        value(representation)
    }

}

object EntityType : PrimitiveDatatype("ENTITY", XmlSchemaConstants.XS_NAMESPACE), IStringType {
    override val baseType: NCNameType get() = NCNameType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE), BuiltinSchemaXmlschema),
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

    override fun value(representation: VString): VString {
        return representation
    }

    override fun value(maybeValue: VAnySimpleType): VString {
        return maybeValue as? VString ?: VString(maybeValue.xmlString)
    }

    override fun validate(representation: VString) {
//        TODO("not implemented")
    }
}

object IDType : PrimitiveDatatype("ID", XmlSchemaConstants.XS_NAMESPACE), IStringType {
    override val baseType: NCNameType get() = NCNameType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE), BuiltinSchemaXmlschema),
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

    override fun value(representation: VString): VID {
        return representation as? VID ?: VID(representation.xmlString)
    }

    override fun value(maybeValue: VAnySimpleType): VID {
        return maybeValue as? VID ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VID)
    }

    override fun validate(representation: VString) {
        value(representation)
    }
}

object IDRefType : PrimitiveDatatype("IDREF", XmlSchemaConstants.XS_NAMESPACE), IStringType {
    override val baseType: NCNameType get() = NCNameType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE), BuiltinSchemaXmlschema),
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

    override fun value(representation: VString): VIDRef {
        return representation as? VIDRef ?: VIDRef(representation.xmlString)
    }

    override fun value(maybeValue: VAnySimpleType): VIDRef {
        return maybeValue as? VIDRef ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VIDRef)
    }

    override fun validate(representation: VString) {
        value(representation)
    }
}

object NMTokenType : PrimitiveDatatype("NMTOKEN", XmlSchemaConstants.XS_NAMESPACE), IStringType {
    override val baseType: TokenType get() = TokenType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE), BuiltinSchemaXmlschema),
        patterns = listOf(ResolvedPattern(XSPattern("\\c+"), BuiltinSchemaXmlschema)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(representation: VString): VNMToken {
        return representation as? VNMToken ?: VNMToken(representation.xmlString)
    }

    override fun value(maybeValue: VAnySimpleType): VNMToken {
        return maybeValue as? VNMToken ?: value(VString(maybeValue.xmlString))
    }

    override fun validateValue(representation: Any) {
        check(representation is VNMToken)
    }

    override fun validate(representation: VString) {
        value(representation)
    }
}

object TimeType : PrimitiveDatatype("time", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
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

    override fun value(representation: VString): VAnySimpleType {
        TODO("not implemented")
    }

    override fun value(maybeValue: VAnySimpleType): VAnySimpleType {
        TODO("not implemented")
    }

    override fun validate(representation: VString) {
//        TODO("not implemented")
    }
}

object EntitiesType :
    ConstructedListDatatype("ENTITIES", XmlSchemaConstants.XS_NAMESPACE, EntityType, BuiltinSchemaXmlschema) {
    override val mdlItemTypeDefinition: ResolvedSimpleType
        get() = EntityType

    override fun validateValue(representation: Any) {
        check(representation is List<*>)
    }

    override fun validate(representation: VString) {}
}

object IDRefsType :
    ConstructedListDatatype("IDREFS", XmlSchemaConstants.XS_NAMESPACE, EntityType, BuiltinSchemaXmlschema) {
    override val mdlItemTypeDefinition: ResolvedSimpleType
        get() = IDRefType

    override fun validateValue(representation: Any) {
        check(representation is List<*>)
    }

    override fun validate(representation: VString) {}
}

object NMTokensType :
    ConstructedListDatatype("NMTOKENS", XmlSchemaConstants.XS_NAMESPACE, EntityType, BuiltinSchemaXmlschema) {
    override val mdlItemTypeDefinition: ResolvedSimpleType
        get() = NMTokenType

    override fun validateValue(representation: Any) {
        check(representation is List<*>)
    }

    override fun validate(representation: VString) {}
}

object PrecisionDecimalType : PrimitiveDatatype("precisionDecimal", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), BuiltinSchemaXmlschema),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.PARTIAL,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = true,
    )

    override fun value(representation: VString): VAnySimpleType {
        TODO("NOT IMPLEMENTED")
    }

    override fun value(maybeValue: VAnySimpleType): VAnySimpleType {
        TODO("not implemented")
    }

    override fun validate(representation: VString) {
//        TODO("not implemented")
    }
}
