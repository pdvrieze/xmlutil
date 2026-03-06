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

package io.github.pdvrieze.formats.xmlschema.datatypes

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.ResAtomicDatatype
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.ResPrimitiveDatatype
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSFacet
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSWhiteSpace
import io.github.pdvrieze.formats.xmlschema.resolved.*
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.resolved.facets.FacetList
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import io.github.pdvrieze.xml.schematypes.WhitespaceValue
import io.github.pdvrieze.xml.schematypes.facets.*
import io.github.pdvrieze.xml.schematypes.types.AnySimpleType
import io.github.pdvrieze.xml.schematypes.values.*
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XMLConstants.XSD_NS_URI

abstract class AbstractDatatype(
    name: XsdNCName,
    schema: ResolvedSchemaLike,
    targetNamespace: XsdAnyURI? = schema.targetNamespace,
) : ResolvedBuiltinSimpleType<XsdAnySimple> {
    final override val mdlQName: XsdQName = name.toQname(targetNamespace)

    abstract override val baseType: ResolvedType

    constructor(name: String, targetNamespace: String, schema: ResolvedSchemaLike) :
            this(XsdNCName(name), schema, targetNamespace.toAnyUri())

    val dtFunctions: List<DataFunction> get() = emptyList()
    val identityFunction: DataFunction get() = TODO()
    val equalityFunction: DataFunction get() = TODO()
    val orderFunction: DataFunction? get() = null
}

class DataFunction

abstract class AbstractConstructedListDatatype<out T: XsdAnySimple, out E: XsdAnySimple> : ResListDatatype<T, E> {
    @Suppress("UNCHECKED_CAST")
    constructor(
        name: String,
        targetNamespace: String,
        itemType: ResAtomicDatatype<*>, // TODO should be atomic or union
        schemaLike: ResolvedSchemaLike,
    ) {
        simpleDerivation = BuiltinListDerivation(itemType)
    }

    @Suppress("UNCHECKED_CAST")
    constructor(
        name: String,
        targetNamespace: String,
        itemType: AbstractUnionDatatype,
        schema: ResolvedSchemaLike,
    ) {
        if (itemType.members.any { it !is ResAtomicDatatype<*> }) {
            throw IllegalArgumentException("Union item types of a list must only have atomic members")
        }
        simpleDerivation = BuiltinListDerivation(itemType)
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is List<*>)
    }

    override fun checkType(checkHelper: CheckHelper) {
        mdlFacets.checkList(this, checkHelper.version)
        checkHelper.checkType(baseType)
    }

    override val itemTypeName: QName?
        get() = itemType.name?.toQName()

    override val simpleType: Nothing? get() = null

    final override val simpleDerivation: BuiltinListDerivation
}

/**
 * Defined by construction or restriction
 *
 * Can be derived using:
 * - enumeration
 * - pattern
 * - assertions
 */
sealed class AbstractUnionDatatype(name: String, targetNamespace: String, schema: ResolvedSchemaLike) :
    AbstractDatatype(name, targetNamespace, schema), ResolvedSimpleType<XsdAnySimple> {
    abstract val members: List<ResolvedSimpleType<*>>
}

object ResErrorType : ResolvedBuiltinSimpleType<XsdAnySimple> {
    override val isSpecial: Boolean get() = false

    override val baseType: ResErrorType get() = ResErrorType
    override val name: XsdQName = XsdQName(XSD_NS_URI, "error")

    val schema: ResolvedSchemaLike get() = BuiltinSchemaXmlschema

    override val simpleDerivation: ResolvedSimpleType.Derivation
        get() = ERRORDERIVATION
    override val mdlFacets: FacetList get() = FacetList.EMPTY

    override val mdlBaseTypeDefinition: ResErrorType get() = baseType

    override val mdlItemTypeDefinition: Nothing? get() = null
    override val mdlMemberTypeDefinitions: List<Nothing> get() = emptyList()

    override val mdlFinal: Set<VDerivationControl.Type>
        get() = emptySet()

    override val ordered: FacetOrdered get() = AnySimpleType.Instance.ordered
    override val bounded: FacetBounded get() = AnySimpleType.Instance.bounded
    override val cardinality: FacetCardinality get() = AnySimpleType.Instance.cardinality
    override val numeric: FacetNumeric get() = AnySimpleType.Instance.numeric
    override val constrainingFacets: List<ConstrainingFacet> get() = emptyList()

    override val mdlVariety: ResolvedSimpleType.Variety get() = ResolvedSimpleType.Variety.ATOMIC
    override val mdlPrimitiveTypeDefinition: ResPrimitiveDatatype<*>?
        get() = TODO("not implemented")

    override val model: ResErrorType get() = this
    override val annotations: List<ResolvedAnnotation> get() = emptyList()

    override fun validate(representation: XsdString, version: SchemaVersion) {
        TODO("not implemented")
    }

    private object ERRORDERIVATION : ResolvedSimpleRestrictionBase(null) {
        override val model: IModel = Model(ResErrorType)
    }
}

internal object AnySimpleTypeRestriction : ResolvedSimpleRestrictionBase(null) {
    override val model: IModel = Model(ResAnyType, FacetList())
}

internal open class ResSimpleBuiltinRestriction(
    baseType: ResolvedBuiltinSimpleType<*>,
    schema: ResolvedSchemaLike,
    facets: List<XSFacet> = listOf(XSWhiteSpace(WhitespaceValue.COLLAPSE, true))
) : ResolvedSimpleRestrictionBase(null) {
    override val model: IModel = Model(baseType, FacetList(facets, schema, baseType, false))
}

