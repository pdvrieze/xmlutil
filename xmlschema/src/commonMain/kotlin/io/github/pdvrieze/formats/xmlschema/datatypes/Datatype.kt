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

package io.github.pdvrieze.formats.xmlschema.datatypes

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VString
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.WhitespaceValue
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.AtomicDatatype
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.PrimitiveDatatype
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalSimpleType
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSFacet
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSMinLength
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSWhiteSpace
import io.github.pdvrieze.formats.xmlschema.resolved.*
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.resolved.facets.FacetList
import io.github.pdvrieze.formats.xmlschema.resolved.facets.ResolvedMinLength
import io.github.pdvrieze.formats.xmlschema.resolved.facets.ResolvedWhiteSpace
import io.github.pdvrieze.formats.xmlschema.types.CardinalityFacet.Cardinality
import io.github.pdvrieze.formats.xmlschema.types.FundamentalFacets
import io.github.pdvrieze.formats.xmlschema.types.OrderedFacet.Order
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.SerializableQName

abstract class Datatype(
    name: VNCName,
    schema: ResolvedSchemaLike,
    targetNamespace: VAnyURI? = schema.targetNamespace,
) : ResolvedBuiltinSimpleType {
    final override val mdlQName: QName = name.toQname(targetNamespace)

    abstract val baseType: ResolvedType

    constructor(name: String, targetNamespace: String, schema: ResolvedSchemaLike) :
            this(VNCName(name), schema, VAnyURI(targetNamespace))

    val dtFunctions: List<DataFunction> get() = emptyList()
    val identityFunction: DataFunction get() = TODO()
    val equalityFunction: DataFunction get() = TODO()
    val orderFunction: DataFunction? get() = null
}

class DataFunction

/**
 * Space separated for primitives. If the itemType is a Union the members of that union must be atomic.
 *
 * Can be derived using:
 * - length
 * - maxLength
 * - minLength
 * - enumeration
 * - pattern
 * - whiteSpace
 * - assertions
 */
sealed class ListDatatype(
    name: String,
    targetNamespace: String,
    val itemType: Datatype,
    schema: ResolvedSchemaLike,
) : Datatype(name, targetNamespace, schema), ResolvedBuiltinSimpleType, ResolvedGlobalSimpleType, ResolvedSimpleType.Model {

    abstract override val baseType: ResolvedType

    override val mdlScope: VSimpleTypeScope.Global get() = super<Datatype>.mdlScope

    val whiteSpace: WhitespaceValue get() = WhitespaceValue.COLLAPSE

    override fun checkType(checkHelper: CheckHelper) {
        checkHelper.checkType(baseType)
    }

    override val model: ListDatatype
        get() = this

    final override val mdlVariety: ResolvedSimpleType.Variety get() = ResolvedSimpleType.Variety.LIST

    final override val annotations: List<ResolvedAnnotation> get() = emptyList()
    final override val mdlBaseTypeDefinition: AnySimpleType get() = AnySimpleType
    final override val mdlFacets: FacetList = FacetList(
        minLength = ResolvedMinLength(XSMinLength(1u), schema),
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE), schema)
    )
    final override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false
    )
    final override val mdlPrimitiveTypeDefinition: PrimitiveDatatype?
        get() = AnySimpleType.mdlPrimitiveTypeDefinition

    abstract override val mdlItemTypeDefinition: ResolvedSimpleType

    final override val mdlMemberTypeDefinitions: List<ResolvedSimpleType>
        get() = emptyList()

    final override val mdlFinal: Set<VDerivationControl.Type> get() = emptySet()
    abstract val itemTypeName: SerializableQName?
    abstract val simpleType: XSLocalSimpleType?
}

abstract class ConstructedListDatatype : ListDatatype {
    constructor(
        name: String,
        targetNamespace: String,
        itemType: AtomicDatatype,
        schemaLike: ResolvedSchemaLike,
    ) : super(name, targetNamespace, itemType, schemaLike) {
        simpleDerivation = BuiltinListDerivation(itemType)
    }

    constructor(
        name: String,
        targetNamespace: String,
        itemType: UnionDatatype,
        schema: ResolvedSchemaLike,
    ) : super(name, targetNamespace, itemType, schema) {
        if (itemType.members.any { it !is AtomicDatatype }) {
            throw IllegalArgumentException("Union item types of a list must only have atomic members")
        }
        simpleDerivation = BuiltinListDerivation(itemType)
    }

    override val baseType: ResolvedType
        get() = AnySimpleType

    override val itemTypeName: QName?
        get() = itemType.mdlQName

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
sealed class UnionDatatype(name: String, targetNamespace: String, schema: ResolvedSchemaLike) : Datatype(name, targetNamespace, schema) {
    val members: List<Datatype> get() = TODO()
}

object ErrorType : Datatype("error", XmlSchemaConstants.XS_NAMESPACE, BuiltinSchemaXmlschema),
    ResolvedGlobalSimpleType,
    ResolvedBuiltinSimpleType,
    ResolvedSimpleType.Model {

    override val baseType: ErrorType get() = ErrorType

    val schema: ResolvedSchemaLike get() = BuiltinSchemaXmlschema
    override val simpleDerivation: ResolvedSimpleType.Derivation

        get() = ERRORDERIVATION
    override val mdlFacets: FacetList get() = FacetList.EMPTY
    override val mdlScope: VSimpleTypeScope.Global get() = super<Datatype>.mdlScope

    override val mdlBaseTypeDefinition: ErrorType get() = baseType
    override val mdlItemTypeDefinition: Nothing? get() = null
    override val mdlMemberTypeDefinitions: List<Nothing> get() = emptyList()

    override val mdlFinal: Set<VDerivationControl.Type>
        get() = super<ResolvedBuiltinSimpleType>.mdlFinal

    override val mdlFundamentalFacets: FundamentalFacets get() = super<ResolvedGlobalSimpleType>.mdlFundamentalFacets

    override val mdlVariety: ResolvedSimpleType.Variety get() = super<ResolvedBuiltinSimpleType>.mdlVariety

    override val mdlPrimitiveTypeDefinition: PrimitiveDatatype?
        get() = super<ResolvedGlobalSimpleType>.mdlPrimitiveTypeDefinition

    override val model: ErrorType get() = this
    override val annotations: List<ResolvedAnnotation> get() = emptyList()

    override fun validate(representation: VString) {
        TODO("not implemented")
    }

    private object ERRORDERIVATION : ResolvedSimpleRestrictionBase(rawPart) {
        override val model: IModel = Model(ErrorType)
    }
}

object AnyType : Datatype("anyType", XmlSchemaConstants.XS_NAMESPACE, BuiltinSchemaXmlschema), ResolvedBuiltinType, ResolvedSimpleType.Model {
    override val baseType: AnyType get() = AnyType // No actual base type

    override val annotations: List<ResolvedAnnotation> get() = emptyList()
    override val mdlVariety: ResolvedSimpleType.Variety get() = super<Datatype>.mdlVariety
    override val mdlFinal: Set<VDerivationControl.Type> get() = super<Datatype>.mdlFinal
    override val mdlFundamentalFacets: Nothing get() = throw UnsupportedOperationException("Any is not simple, and has no facets")

    override val simpleDerivation: ResolvedSimpleRestrictionBase
        get() = SimpleBuiltinRestriction(AnyType, schema = BuiltinSchemaXmlschema)

    override val mdlFacets: FacetList get() = FacetList.EMPTY

    //    override val final: Set<Nothing> get() = emptySet()
    override val model: AnyType get() = this

    override val mdlBaseTypeDefinition: AnyType get() = this

    override val mdlPrimitiveTypeDefinition: Nothing? get() = null

    override val mdlItemTypeDefinition: Nothing? get() = null

    override val mdlMemberTypeDefinitions: List<Nothing>
        get() = emptyList()

    override fun validate(representation: VString) {
//        error("anyType cannot be directly implemented")
    }

    override fun toString(): String = "xsd:anyType"


}

object AnySimpleType : Datatype("anySimpleType", XmlSchemaConstants.XS_NAMESPACE, BuiltinSchemaXmlschema), ResolvedBuiltinSimpleType, ResolvedSimpleType.Model {

    override val baseType: AnyType get() = AnyType

    override val simpleDerivation: ResolvedSimpleType.Derivation
        get() = SimpleBuiltinRestriction(baseType, schema = BuiltinSchemaXmlschema)

    override val mdlBaseTypeDefinition: AnyType get() = AnyType
    override val model: AnySimpleType get() = this
    override val mdlVariety: ResolvedSimpleType.Variety get() = ResolvedSimpleType.Variety.NIL
    override val mdlPrimitiveTypeDefinition: Nothing? get() = null
    override val mdlItemTypeDefinition: Nothing? get() = null
    override val mdlMemberTypeDefinitions: List<Nothing> get() = emptyList()
    override val mdlFinal: Set<VDerivationControl.Type> get() = super<Datatype>.mdlFinal
    override val mdlFacets: FacetList get() = FacetList.EMPTY
    override val annotations: List<ResolvedAnnotation> get() = emptyList()
    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = Order.FALSE,
        bounded = false,
        cardinality = Cardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun validate(representation: VString) {
//        TODO("not implemented")
    }
}

internal open class SimpleBuiltinRestriction(
    baseType: ResolvedBuiltinType,
    schema: ResolvedSchemaLike,
    facets: List<XSFacet> = listOf(XSWhiteSpace(WhitespaceValue.COLLAPSE, true))
) : ResolvedSimpleRestrictionBase(null) {
    override val model: IModel = Model(baseType, FacetList(facets, schema, baseType.mdlPrimitiveTypeDefinition))
}
