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

package io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes

import io.github.pdvrieze.formats.xmlschema.datatypes.ResAnySimpleType
import io.github.pdvrieze.formats.xmlschema.datatypes.ResAnyType
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedBuiltinSimpleType
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedBuiltinType
import io.github.pdvrieze.xml.schematypes.values.XsdAnySimple
import io.github.pdvrieze.xml.schematypes.values.XsdDecimal
import io.github.pdvrieze.xml.schematypes.values.XsdString
import nl.adaptivity.xmlutil.XMLConstants.XSD_NS_URI

fun builtinType(localName: String, targetNamespace: String): ResolvedBuiltinType? {
    if (targetNamespace != XSD_NS_URI) return null
    return when (localName) {
        "anyType" -> ResAnyType
        "anySimpleType" -> ResAnySimpleType
        "anyAtomicType" -> ResAnyAtomicType
        "anyURI" -> ResAnyURIType
        "base64Binary" -> ResBase64BinaryType
        "boolean" -> ResBooleanType
        "date" -> ResDateType
        "dateTime" -> ResDateTimeType
        "dateTimeStamp" -> ResDateTimeStampType
        "decimal" -> ResDecimalType
        "integer" -> ResIntegerType
        "long" -> ResLongType
        "int" -> ResIntType
        "short" -> ResShortType
        "byte" -> ResByteType
        "nonNegativeInteger" -> ResNonNegativeIntegerType
        "positiveInteger" -> ResPositiveIntegerType
        "unsignedLong" -> ResUnsignedLongType
        "unsignedInt" -> ResUnsignedIntType
        "unsignedShort" -> ResUnsignedShortType
        "unsignedByte" -> ResUnsignedByteType
        "nonPositiveInteger" -> ResNonPositiveIntegerType
        "negativeInteger" -> ResNegativeIntegerType
        "double" -> ResDoubleType
        "duration" -> ResDurationType
        "dayTimeDuration" -> ResDayTimeDurationType
        "yearMonthDuration" -> ResYearMonthDurationType
        "float" -> ResFloatType
        "gDay" -> ResGDayType
        "gMonth" -> ResGMonthType
        "gMonthDay" -> ResGMonthDayType
        "gYear" -> ResGYearType
        "gYearMonth" -> ResGYearMonthType
        "hexBinary" -> ResHexBinaryType
        "NOTATION" -> ResNotationType
        "QName" -> ResQNameType
        "string" -> ResStringType
        "normalizedString" -> ResNormalizedStringType
        "token" -> ResTokenType
        "language" -> ResLanguageType
        "Name" -> ResNameType
        "NCName" -> ResNCNameType
        "ENTITY" -> ResEntityType
        "ID" -> ResIDType
        "IDREF" -> ResIDRefType
        "NMTOKEN" -> ResNMTokenType
        "time" -> ResTimeType
        "ENTITIES" -> ResEntitiesType
        "IDREFS" -> ResIDRefsType
        "NMTOKENS" -> ResNMTokensType
        "precisionDecimal" -> ResPrecisionDecimalType
        else -> null
    }
}

sealed interface ResIStringType<T: XsdString> : ResolvedBuiltinSimpleType<T> {
    override fun value(representation: XsdString): XsdString
}

interface ResFiniteDateType<T: XsdAnySimple> : ResolvedBuiltinSimpleType<T>

sealed interface ResIIntegerType<out T: XsdDecimal> : IResolvedDecimalType<T>

