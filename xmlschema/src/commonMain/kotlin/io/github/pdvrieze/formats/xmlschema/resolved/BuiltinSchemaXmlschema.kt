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

import io.github.pdvrieze.formats.xmlschema.datatypes.AnySimpleType
import io.github.pdvrieze.formats.xmlschema.datatypes.AnyType
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.toAnyUri
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.*
import io.github.pdvrieze.formats.xmlschema.types.VFormChoice
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XMLConstants.XSD_NS_URI
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.namespaceURI

object BuiltinSchemaXmlschema : ResolvedSchemaLike() {
    override val version: SchemaVersion get() = SchemaVersion.V1_1
    override val targetNamespace: VAnyURI = XSD_NS_URI.toAnyUri()
    override val defaultOpenContent: Nothing? get() = null
    override val defaultAttributes: Nothing? get() = null

    override val attributeFormDefault: VFormChoice get() = VFormChoice.UNQUALIFIED
    override val elementFormDefault: VFormChoice get() = VFormChoice.QUALIFIED

    override fun maybeSimpleType(typeName: QName): ResolvedGlobalSimpleType? {
        require(typeName.namespaceURI == XSD_NS_URI) {
            "The type must be in the xmlschema namespace for the builtin schema"
        }

        return typeMap[typeName.localPart]
    }

    override fun maybeType(typeName: QName): ResolvedGlobalType? {
        if (typeName.namespaceURI == XSD_NS_URI && typeName.localPart == "anyType") return AnyType
        return maybeSimpleType(typeName)
    }

    private val simpleTypes: List<ResolvedGlobalSimpleType>
        get() = listOf(
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

    private val typeMap: Map<String, ResolvedGlobalSimpleType> by lazy {
        simpleTypes.associateBy { it.mdlQName.localPart }
    }

    override val blockDefault: Set<Nothing>
        get() = emptySet()

    override val finalDefault: Set<Nothing>
        get() = emptySet()

    override fun maybeAttributeGroup(attributeGroupName: QName): Nothing? {
        return null
    }

    override fun maybeGroup(groupName: QName): Nothing? = null

    override fun maybeElement(elementName: QName): Nothing? = null

    override fun maybeAttribute(attributeName: QName): Nothing? = null

    override fun maybeIdentityConstraint(constraintName: QName): Nothing? = null

    override fun maybeNotation(notationName: QName): Nothing? = null

    override fun substitutionGroupMembers(headName: QName): Set<Nothing> = emptySet()

    override fun getElements(): Set<ResolvedGlobalElement> = emptySet()

    internal val resolver = object : ResolvedSchema.SchemaElementResolver {
        override fun maybeSimpleType(typeName: String): ResolvedGlobalSimpleType? {
            return typeMap[typeName]
        }

        override fun maybeType(typeName: String): ResolvedGlobalType? {
            if (typeName == "anyType") return AnyType
            return maybeSimpleType(typeName)
        }

        override fun maybeAttributeGroup(attributeGroupName: String): Nothing? = null

        override fun maybeGroup(groupName: String): Nothing? = null

        override fun maybeElement(elementName: String): Nothing? = null

        override fun maybeAttribute(attributeName: String): Nothing? = null

        override fun maybeIdentityConstraint(constraintName: String): Nothing? = null

        override fun maybeNotation(notationName: String): Nothing? = null
    }
}
