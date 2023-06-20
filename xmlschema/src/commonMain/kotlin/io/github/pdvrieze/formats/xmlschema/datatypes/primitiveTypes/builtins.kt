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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSWhiteSpace
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_SimpleDerivationSetElem
import io.github.pdvrieze.formats.xmlschema.resolved.*

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
}

sealed class PrimitiveDatatype(name: String, targetNamespace: String) : AtomicDatatype(name, targetNamespace) {
    abstract override val baseType: ResolvedBuiltinType
    override val simpleDerivation: ResolvedSimpleRestrictionBase
        get() = SimpleBuiltinRestriction(baseType)
}

class RestrictedAtomicDatatype(name: String, targetNamespace: String, override val baseType: AtomicDatatype) :
    AtomicDatatype(name, targetNamespace) {
    override val final: Set<T_SimpleDerivationSetElem>
        get() = TODO("not implemented")
    override val simpleDerivation: ResolvedSimpleRestrictionBase
        get() = TODO("not implemented")

    init {
        if (baseType == AnyAtomicType)
            throw IllegalArgumentException("Restricted types cannot derive directly from anyAtomicType")
    }

}

object AnyAtomicType : AtomicDatatype("anyAtomicType", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnySimpleType get() = AnySimpleType
    override val simpleDerivation: ResolvedSimpleRestrictionBase =
        SimpleBuiltinRestriction(AnySimpleType)
}

object AnyURIType : PrimitiveDatatype("anyURI", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType
}

object Base64BinaryType : PrimitiveDatatype("base64Binary", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType
}

object BooleanType : PrimitiveDatatype("boolean", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType
}

object DateType : PrimitiveDatatype("date", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType
}

object DateTimeType : PrimitiveDatatype("dateTime", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType
}

object DateTimeStampType : PrimitiveDatatype("dateTimeStamp", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: DateTimeType get() = DateTimeType
}

object DecimalType : PrimitiveDatatype("decimal", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType
}

object IntegerType : PrimitiveDatatype("integer", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: DecimalType get() = DecimalType
}

object LongType : PrimitiveDatatype("long", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: IntegerType get() = IntegerType
}

object IntType : PrimitiveDatatype("int", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: LongType get() = LongType
}

object ShortType : PrimitiveDatatype("short", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: IntType get() = IntType
}

object ByteType : PrimitiveDatatype("byte", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: ShortType get() = ShortType
}

object NonNegativeIntegerType : PrimitiveDatatype("nonNegativeInteger", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: IntegerType get() = IntegerType
}

object PositiveIntegerType : PrimitiveDatatype("positiveInteger", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: NonNegativeIntegerType get() = NonNegativeIntegerType
}

object UnsignedLongType : PrimitiveDatatype("unsignedLong", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: NonNegativeIntegerType get() = NonNegativeIntegerType
}

object UnsignedIntType : PrimitiveDatatype("unsignedInt", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: UnsignedLongType get() = UnsignedLongType
}

object UnsignedShortType : PrimitiveDatatype("unsignedShort", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: UnsignedIntType get() = UnsignedIntType
}

object UnsignedByteType : PrimitiveDatatype("unsignedByte", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: UnsignedShortType get() = UnsignedShortType
}

object NonPositiveIntegerType : PrimitiveDatatype("nonPositiveInteger", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: IntegerType get() = IntegerType
}

object NegativeIntegerType : PrimitiveDatatype("negativeInteger", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: NonPositiveIntegerType get() = NonPositiveIntegerType
}

object DoubleType : PrimitiveDatatype("double", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType
}

object DurationType : PrimitiveDatatype("duration", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType
}

object DayTimeDurationType : PrimitiveDatatype("dayTimeDuration", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: DurationType get() = DurationType
}

object YearMonthDurationType : PrimitiveDatatype("yearMonthDuration", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: DurationType get() = DurationType
}

object FloatType : PrimitiveDatatype("float", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType
}

object GDayType : PrimitiveDatatype("gDay", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType
}

object GMonthType : PrimitiveDatatype("gMonth", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType
}

object GMonthDayType : PrimitiveDatatype("gMonthDay", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType
}

object GYearType : PrimitiveDatatype("gYear", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType
}

object GYearMonthType : PrimitiveDatatype("gYearMonth", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType
}

object HexBinaryType : PrimitiveDatatype("hexBinary", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType
}

object NotationType : PrimitiveDatatype("NOTATION", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType
}

object QNameType : PrimitiveDatatype("QName", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType
}

object StringType : PrimitiveDatatype("string", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType
    override val simpleDerivation: ResolvedSimpleRestrictionBase
        get() = SimpleBuiltinRestriction(baseType, listOf(XSWhiteSpace(XSWhiteSpace.Values.PRESERVE, fixed = false)))
}

object NormalizedStringType : PrimitiveDatatype("normalizedString", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: StringType get() = StringType
}

object TokenType : PrimitiveDatatype("token", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: NormalizedStringType get() = NormalizedStringType
}

object LanguageType : PrimitiveDatatype("language", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: TokenType get() = TokenType
}

object NameType : PrimitiveDatatype("Name", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: TokenType get() = TokenType
}

object NCNameType : PrimitiveDatatype("NCName", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: NameType get() = NameType
}

object EntityType : PrimitiveDatatype("ENTITY", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: NCNameType get() = NCNameType
}

object IDType : PrimitiveDatatype("ID", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: NCNameType get() = NCNameType
}

object IDRefType : PrimitiveDatatype("IDREF", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: NCNameType get() = NCNameType
}

object NMTokenType : PrimitiveDatatype("NMTOKEN", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: TokenType get() = TokenType
}

object TimeType : PrimitiveDatatype("time", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType
}

object EntitiesType : ConstructedListDatatype("ENTITIES", XmlSchemaConstants.XS_NAMESPACE, EntityType)

object IDRefsType : ConstructedListDatatype("IDREFS", XmlSchemaConstants.XS_NAMESPACE, EntityType)

object NMTokensType : ConstructedListDatatype("NMTOKENS", XmlSchemaConstants.XS_NAMESPACE, EntityType)

object PrecisionDecimalType : PrimitiveDatatype("precisionDecimal", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType
}
