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

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.AnySimpleType
import io.github.pdvrieze.formats.xmlschema.datatypes.AnyType
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.*
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.namespaceURI

object BuiltinXmlSchema : ResolvedSchemaLike() {
    override val targetNamespace: VAnyURI = VAnyURI(XmlSchemaConstants.XS_NAMESPACE)
    override val defaultOpenContent: Nothing?
        get() = null

    override fun simpleType(typeName: QName): ResolvedGlobalSimpleType {
        require(typeName.namespaceURI == XmlSchemaConstants.XS_NAMESPACE) {
            "The type must be in the xmlschema namespace for the builtin schema"
        }

        return typeMap[typeName.localPart]
            ?: throw NoSuchElementException("No type with name $typeName found")
    }

    override fun type(typeName: QName): ResolvedGlobalType {
        require(typeName.namespaceURI == XmlSchemaConstants.XS_NAMESPACE) {
            "The type must be in the xmlschema namespace for the builtin schema"
        }
        if (typeName.localPart == "anyType") return AnyType
        return simpleType(typeName)
    }

    override val elements: List<ResolvedGlobalElement>
        get() = emptyList()

    override val attributes: List<ResolvedGlobalAttribute>
        get() = emptyList()

    override val simpleTypes: List<ResolvedGlobalSimpleType> by lazy {
        listOf(
            AnySimpleType, AnyAtomicType, AnyURIType, Base64BinaryType, BooleanType,
            DateType, DateTimeType, DateTimeStampType, DecimalType, IntegerType, LongType,
            IntType, ShortType, ByteType, NonNegativeIntegerType, PositiveIntegerType,
            UnsignedLongType, UnsignedIntType, UnsignedShortType, UnsignedByteType,
            NonPositiveIntegerType, NegativeIntegerType, DoubleType, DurationType,
            DayTimeDurationType, YearMonthDurationType, FloatType, GDayType, GMonthType,
            GMonthDayType, GYearType, GYearMonthType, HexBinaryType, NotationType, QNameType,
            StringType, NormalizedStringType, TokenType, LanguageType, NameType, NCNameType,
            EntityType, IDType, IDRefType, NMTokenType, TimeType, EntitiesType, IDRefsType,
            NMTokensType
        )
    }

    private val typeMap: Map<String, ResolvedGlobalSimpleType> by lazy {
        simpleTypes.associateBy { it.qName.localPart }
    }

    override val complexTypes: List<Nothing>
        get() = emptyList()

    override val groups: List<Nothing>
        get() = emptyList()

    override val attributeGroups: List<Nothing>
        get() = emptyList()

    override val blockDefault: Set<Nothing>
        get() = emptySet()

    override val finalDefault: Set<Nothing>
        get() = emptySet()
}
