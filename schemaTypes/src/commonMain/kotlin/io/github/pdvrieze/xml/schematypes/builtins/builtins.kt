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

package io.github.pdvrieze.xml.schematypes.builtins

import io.github.pdvrieze.xml.schematypes.types.AnyAtomicType
import io.github.pdvrieze.xml.schematypes.types.AnySimpleType
import io.github.pdvrieze.xml.schematypes.types.AnyType
import io.github.pdvrieze.xml.schematypes.types.BuiltinType
import nl.adaptivity.xmlutil.XMLConstants.XSD_NS_URI

fun builtinType(localName: String, targetNamespace: String): BuiltinType? {
    if (targetNamespace != XSD_NS_URI) return null
    return when (localName) {
        "anyType" -> AnyType.Instance
        "anySimpleType" -> AnySimpleType.Instance
        "anyAtomicType" -> AnyAtomicType.Instance
        "anyURI" -> AnyURIType.Instance
        "base64Binary" -> Base64BinaryType.Instance
        "boolean" -> BooleanType.Instance
        "date" -> DateType.Instance
        "dateTime" -> DateTimeType.Instance
        "dateTimeStamp" -> DateTimeStampType.Instance
        "decimal" -> DecimalType.Instance
        "integer" -> IntegerType.Instance
        "long" -> LongType.Instance
        "int" -> IntType.Instance
        "short" -> ShortType.Instance
        "byte" -> ByteType.Instance
        "nonNegativeInteger" -> NonNegativeIntegerType.Instance
        "positiveInteger" -> PositiveIntegerType.Instance
        "unsignedLong" -> UnsignedLongType.Instance
        "unsignedInt" -> UnsignedIntType.Instance
        "unsignedShort" -> UnsignedShortType.Instance
        "unsignedByte" -> UnsignedByteType.Instance
        "nonPositiveInteger" -> NonPositiveIntegerType.Instance
        "negativeInteger" -> NegativeIntegerType.Instance
        "double" -> DoubleType.Instance
        "duration" -> DurationType.Instance
        "dayTimeDuration" -> DayTimeDurationType.Instance
        "yearMonthDuration" -> YearMonthDurationType.Instance
        "float" -> FloatType.Instance
        "gDay" -> GDayType.Instance
        "gMonth" -> GMonthType.Instance
        "gMonthDay" -> GMonthDayType.Instance
        "gYear" -> GYearType.Instance
        "gYearMonth" -> GYearMonthType.Instance
        "hexBinary" -> HexBinaryType.Instance
        "NOTATION" -> NotationType.Instance
        "QName" -> QNameType.Instance
        "string" -> StringType.Instance
        "normalizedString" -> NormalizedStringType.Instance
        "token" -> TokenType.Instance
        "language" -> LanguageType.Instance
        "Name" -> NameType.Instance
        "NCName" -> NCNameType.Instance
        "ENTITY" -> EntityType.Instance
        "ID" -> IDType.Instance
        "IDREF" -> IDRefType.Instance
        "NMTOKEN" -> NMTokenType.Instance
        "time" -> TimeType.Instance
        "ENTITIES" -> EntitiesType.Instance
        "IDREFS" -> IDRefsType.Instance
        "NMTOKENS" -> NMTokensType.Instance
        else -> null
    }
}
