/*
 * Copyright (c) 2021.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package io.github.pdvrieze.formats.xmlschema.datatypes

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants

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

object NMTokenType : PrimitiveDatatype("NMToken", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: TokenType get() = TokenType
}

object TimeType : PrimitiveDatatype("time", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyAtomicType get() = AnyAtomicType
}

object EntitiesType: ConstructedListDatatype("ENTITIES", XmlSchemaConstants.XS_NAMESPACE, EntityType)

object IDRefsType: ConstructedListDatatype("IDREFS", XmlSchemaConstants.XS_NAMESPACE, EntityType)

object NMTokens: ConstructedListDatatype("NMTOKENS", XmlSchemaConstants.XS_NAMESPACE, EntityType)

object PrecisionDecimalType: PrimitiveDatatype("precisionDecimal", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: Datatype get() = AnyAtomicType
}
