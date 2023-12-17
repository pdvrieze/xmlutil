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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.*
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.AnyPrimitiveDatatype
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.FiniteDateType
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.NotationType
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSWhiteSpace
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.resolved.facets.FacetList
import io.github.pdvrieze.formats.xmlschema.resolved.facets.ResolvedWhiteSpace
import io.github.pdvrieze.formats.xmlschema.types.CardinalityFacet.Cardinality
import io.github.pdvrieze.formats.xmlschema.types.FundamentalFacets
import io.github.pdvrieze.formats.xmlschema.types.OrderedFacet
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XMLConstants.XSD_NS_URI
import nl.adaptivity.xmlutil.isEquivalent
import nl.adaptivity.xmlutil.qname

sealed interface ResolvedSimpleType : ResolvedType, VSimpleTypeScope.Member {

    override val mdlScope: VSimpleTypeScope

    val simpleDerivation: Derivation

    override val model: Model

    override val mdlBaseTypeDefinition: ResolvedType get() = model.mdlBaseTypeDefinition

    val mdlFacets: FacetList get() = model.mdlFacets

    val mdlFundamentalFacets: FundamentalFacets get() = model.mdlFundamentalFacets

    val mdlVariety: Variety get() = model.mdlVariety

    val mdlPrimitiveTypeDefinition: AnyPrimitiveDatatype? get() = model.mdlPrimitiveTypeDefinition

    val mdlItemTypeDefinition: ResolvedSimpleType? get() = model.mdlItemTypeDefinition

    val mdlMemberTypeDefinitions: List<ResolvedSimpleType>
        get() = model.mdlMemberTypeDefinitions

    override val mdlFinal: Set<VDerivationControl.Type>

    override fun checkType(
        checkHelper: CheckHelper
    ) { // TODO maybe move to toplevel
        checkAnnotated(checkHelper.version)
        simpleDerivation.checkDerivation(checkHelper)

        if (mdlPrimitiveTypeDefinition == NotationType) {
            for (enum in mdlFacets.enumeration) {
                val name = when (val v = enum.value) {
                    is VNotation -> v.value
                    is VPrefixString -> v.toQName()
                    is VString -> QName(v.xmlString)
                    else -> error("Value $v is not supported as notation")
                }
                // TODO (have notations resolved
                checkHelper.checkNotation(name)
            }
        }

        if (mdlVariety == Variety.LIST) {
            val baseTypeDef = mdlBaseTypeDefinition
            require(baseTypeDef is ResolvedSimpleType) { "Only AnySimpleType can inherit from (complex) AnyType" }
            if (baseTypeDef != AnySimpleType) {
                check(baseTypeDef.mdlVariety == Variety.LIST)
                check(VDerivationControl.RESTRICTION !in baseTypeDef.mdlFinal)
            }
            mdlFacets.checkList(this, checkHelper.version)
        } else {
            mdlFacets.check(this, checkHelper.version)
        }
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        when (mdlVariety) {
            Variety.ATOMIC -> {
                val pt = checkNotNull(mdlPrimitiveTypeDefinition)
                pt.validateValue(value, version)
                mdlFacets.validateValue(value)
            }
            Variety.LIST -> {
                val id = checkNotNull(mdlItemTypeDefinition)
                check(value is List<*>)
                mdlFacets.validateValue(value)
                for (i in value.requireNoNulls()) {
                    id.validateValue(i, version)
                }
            }
            Variety.UNION -> {
                check(mdlMemberTypeDefinitions.isNotEmpty()) { "Union type ${this} has no members" }
                check(mdlMemberTypeDefinitions.any {t ->
                    runCatching { t.validateValue(value, version); true }.getOrDefault(false)
                }) { "Value $value does not fit any members of union: ${mdlMemberTypeDefinitions}" }
            }
            Variety.NIL -> error("Nil variety cannot be validated")
        }
    }

    override fun validate(representation: VString, version: SchemaVersion) {
        check(this != mdlPrimitiveTypeDefinition) { "$mdlPrimitiveTypeDefinition fails to override validate" }
        val v = value(representation)
        if (v != null) validateValue(v, version)
        val pt = mdlPrimitiveTypeDefinition
        if (pt != null) {
            pt.validate(representation, version)
            mdlFacets.validate(pt, representation)
        }
    }

    override fun isValidSubtitutionFor(other: ResolvedType, asRestriction: Boolean): Boolean {
        return isValidlyDerivedFrom(other, asRestriction)
    }

    /**
     * 3.16.6.3
     */
    override fun isValidlyDerivedFrom(base: ResolvedType, asRestriction: Boolean): Boolean {
        if (this === base) return true // 3.16.6.3(1)

        if (VDerivationControl.RESTRICTION in base.mdlFinal) return false //3.16.6.3(2.1a)
        if (VDerivationControl.RESTRICTION in mdlBaseTypeDefinition.mdlFinal) return false //3.16.6.3(2.1.b)
        if (mdlBaseTypeDefinition == base) return true //3.16.6.3(2.2.1)
        if (mdlBaseTypeDefinition != AnyType && mdlBaseTypeDefinition.isValidlyDerivedFrom(base, asRestriction)) return true //3.16.6.3(2.2.2)
        if (mdlVariety != Variety.ATOMIC && base == AnySimpleType) return true //2.2.3
        if (base is ResolvedSimpleType && base.mdlVariety == Variety.UNION) { //2.2.4.1
            // Facets should be unassignable in union -- 2.2.4.3
            val members = base.transitiveUnionMembership() //2.2.4.2
            return members.any { m ->
                isValidlyDerivedFrom(m, asRestriction)
            }
        }
        return false //none of the 4 options is true
    }

    fun transitiveUnionMembership(collector: MutableSet<ResolvedSimpleType> = mutableSetOf()): Set<ResolvedSimpleType> {
        if(mdlVariety != Variety.UNION) return collector
        when(val d = simpleDerivation) {
            is ResolvedUnionDerivation -> {
                for (m in d.memberTypes) {
                    when (m.mdlVariety) {
                        Variety.UNION -> m.transitiveUnionMembership(collector)
                        else -> collector.add(m)
                    }
                }
            }
            is ResolvedListDerivationBase -> {
                throw IllegalStateException("list derivations should never have union variety")
            }
            is ResolvedSimpleRestrictionBase -> {
                (d.baseType as? ResolvedSimpleType)?.transitiveUnionMembership(collector)
            }
        }
        return collector
    }

    fun value(representation: VString): Any? {
        val normalized = mdlFacets.whiteSpace?.value?.normalize(representation) ?: representation
        return when (mdlVariety) {
            Variety.ATOMIC -> {
                val type = checkNotNull(mdlPrimitiveTypeDefinition) { "Missing primitive type for $this" }
                mdlFacets.validate(type, normalized)
                return type.value(normalized)
            }
            Variety.LIST -> {
                if (normalized.isEmpty()) return emptyList<Any>()
                val itemType = checkNotNull(mdlItemTypeDefinition)

                return when {
                    normalized.isEmpty() -> emptyList()
                    normalized is VPrefixStringList -> normalized.elems.map {
                        itemType.value(it)
                    }
                    else -> normalized.split(' ').map {
                        itemType.value(VString(it))
                    }
                }
            }
            Variety.UNION -> {
                for (m in mdlMemberTypeDefinitions) {
                    // TODO don't rely on exceptions here
                    runCatching { m.value(normalized) }.onSuccess { return it }
                }
            }
            Variety.NIL -> error("Nil variety is not allowed for actual types")
        }
    }

    sealed class Derivation() : ResolvedAnnotated {
        abstract override val model: ResolvedAnnotated.IModel

        /** Abstract as it is static for union/list. In those cases always AnySimpleType */
        abstract val baseType: ResolvedType

        open fun checkDerivation(checkHelper: CheckHelper) {}

    }

    interface Model : ResolvedAnnotated.IModel {
        val mdlItemTypeDefinition: ResolvedSimpleType?
        val mdlMemberTypeDefinitions: List<ResolvedSimpleType>
        val mdlBaseTypeDefinition: ResolvedType
        val mdlFacets: FacetList
        val mdlFundamentalFacets: FundamentalFacets
        val mdlVariety: Variety
        val mdlPrimitiveTypeDefinition: AnyPrimitiveDatatype?
    }

    sealed class ModelBase(
        rawPart: XSISimpleType,
        protected val schema: ResolvedSchemaLike,
        context: ResolvedSimpleType
    ) : ResolvedAnnotated.Model(rawPart), Model {

        final override val mdlBaseTypeDefinition: ResolvedSimpleType
        final override val mdlItemTypeDefinition: ResolvedSimpleType?
        final override val mdlMemberTypeDefinitions: List<ResolvedSimpleType>
        final override val mdlVariety: Variety
        final override val mdlPrimitiveTypeDefinition: AnyPrimitiveDatatype?

        init {

            val typeName =
                (rawPart as? XSGlobalSimpleType)?.let { qname(schema.targetNamespace?.value, it.name.xmlString) }

            val simpleDerivation = rawPart.simpleDerivation

            mdlBaseTypeDefinition = when {
                schema is RedefineSchema -> {
                    checkNotNull(typeName) { "Redefines are global and must have a name" }
                    check(simpleDerivation is XSSimpleRestriction) { "Redefines are restrictions" }
                    require(typeName.isEquivalent(checkNotNull(simpleDerivation.base))) { "Redefine of simple type ($typeName) must use original as base" }

                    schema.nestedType(typeName) as ResolvedGlobalSimpleType
                }

                simpleDerivation !is XSSimpleRestriction -> AnySimpleType

                rawPart is XSGlobalSimpleType &&
                        typeName != null && simpleDerivation.base != null && typeName.isEquivalent(simpleDerivation.base) -> {
                    throw IllegalArgumentException( "Only redefines can have 'self-referencing types' in $typeName" )
                }

                simpleDerivation.base?.isEquivalent(AnySimpleType.mdlQName) == true -> AnySimpleType

                else -> {
                    simpleDerivation.base?.let {
                        require(typeName == null || !it.isEquivalent(typeName))
                        schema.simpleType(it)
                    } ?: ResolvedLocalSimpleType(simpleDerivation.simpleType!!, schema, context)
                }
            }



            mdlVariety = when (simpleDerivation) {
                is XSSimpleList -> Variety.LIST
                is XSSimpleRestriction -> recurseBaseType(mdlBaseTypeDefinition) {
                    when {
                        it is AnySimpleType -> {
                            require(schema.targetNamespace?.value == XSD_NS_URI) {
                                "Direct inheritance of AnySimpleType is only allowed in the XMLSchema namespace"
                            }
                            Variety.ATOMIC
                        }

                        else -> it.mdlVariety.notNil()
                    }
                }

                is XSSimpleUnion -> Variety.UNION
                else -> error("Unreachable/unsupported derivation")
            }


            mdlItemTypeDefinition = when (mdlVariety) {
                Variety.LIST -> when {
                    mdlBaseTypeDefinition != AnySimpleType -> {
                        require(simpleDerivation !is XSSimpleList || (simpleDerivation.simpleType == null && simpleDerivation.itemTypeName == null)) {
                            "Lists by base type should have item types"
                        }

                        recurseBaseType(mdlBaseTypeDefinition) { it.mdlItemTypeDefinition }
                    }
                    else -> {
                        val d = simpleDerivation as XSSimpleList
                        when {
                            d.itemTypeName != null -> {
                                require(d.simpleType == null) { "List specifies simpleType and itemType ($rawPart)" }
                                schema.simpleType(d.itemTypeName)
                            }

                            d.simpleType != null -> {
                                ResolvedLocalSimpleType(d.simpleType, schema, context)
                            }

                            else -> throw IllegalArgumentException("List type without item type")
                        }
                    }
                }

                else -> null
            }

            mdlMemberTypeDefinitions = when {
                mdlVariety != Variety.UNION -> emptyList()
                simpleDerivation !is XSSimpleUnion -> recurseBaseType(mdlBaseTypeDefinition) {
                    it.mdlMemberTypeDefinitions
                }

                mdlBaseTypeDefinition == AnySimpleType -> simpleDerivation.memberTypes?.map {
                    schema.simpleType(it)
                } ?: simpleDerivation.simpleTypes.map {
                        ResolvedLocalSimpleType(it, schema, context)
                    }

                else -> recurseBaseType(mdlBaseTypeDefinition) {
                    it.mdlMemberTypeDefinitions
                }
            }


            mdlPrimitiveTypeDefinition = when (mdlVariety) {
                Variety.ATOMIC -> when (val b = mdlBaseTypeDefinition) {
                    is AnyPrimitiveDatatype -> b
                    else -> recurseBaseType(mdlBaseTypeDefinition) { it.mdlPrimitiveTypeDefinition }
                        ?: run { null }
                }

                else -> null
            }


        }


        final override val mdlFacets: FacetList = when (val d = rawPart.simpleDerivation) {
            is XSSimpleRestriction -> {
                when (mdlVariety) {
                    Variety.UNION ->
                        for (f in d.facets) {
                            require(f.isUnionFacet(schema.version)) { "Union variety types may not contain non-union facet: $f" }
                        }

                    Variety.LIST ->
                        for (f in d.facets) {
                            require(f.isListFacet(schema.version)) { "List variety types may not contain non-list facet: $f" }
                        }
                    else -> {}
                }
                mdlBaseTypeDefinition.mdlFacets.overlay(FacetList.safe(d.facets, schema, mdlBaseTypeDefinition))
            }

            is XSSimpleList -> FacetList(
                whiteSpace =
                ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true))
            )

            is XSSimpleUnion -> FacetList.EMPTY

            else -> error("Compiler issue")
        }


        final override val mdlFundamentalFacets: FundamentalFacets = when (rawPart.simpleDerivation) {

            is XSSimpleList -> {
                val cardinalty = when {
                    mdlItemTypeDefinition!!.mdlFundamentalFacets.cardinality == Cardinality.FINITE &&
                            mdlFacets.minLength != null && mdlFacets.maxLength != null -> Cardinality.FINITE

                    else -> Cardinality.COUNTABLY_INFINITE
                }

                FundamentalFacets(
                    ordered = OrderedFacet.Order.FALSE,
                    bounded = false,
                    cardinality = cardinalty,
                    numeric = false
                )
            }

            is XSSimpleRestriction -> {
                val ordered = mdlBaseTypeDefinition.mdlFundamentalFacets.ordered
                val bounded = mdlFacets.minConstraint != null || mdlFacets.maxConstraint != null
                val cardinality = when {
                    mdlBaseTypeDefinition.mdlFundamentalFacets.cardinality == Cardinality.FINITE -> Cardinality.FINITE
                    mdlFacets.maxLength != null || mdlFacets.totalDigits != null -> Cardinality.FINITE
                    mdlFacets.minConstraint != null &&
                            mdlFacets.maxConstraint != null &&
                            (mdlFacets.fractionDigits != null || mdlPrimitiveTypeDefinition is FiniteDateType) ->
                        Cardinality.FINITE

                    else -> Cardinality.COUNTABLY_INFINITE
                }

                val numeric = mdlBaseTypeDefinition.mdlFundamentalFacets.numeric

                FundamentalFacets(ordered, bounded, cardinality, numeric)
            }

            is XSSimpleUnion -> {
                val firstFacet = mdlMemberTypeDefinitions.firstOrNull()?.mdlFundamentalFacets
                val firstOrdered = firstFacet?.ordered

                val ordered = when {
                    mdlMemberTypeDefinitions.all {
                        it.mdlVariety == Variety.ATOMIC && it.mdlFundamentalFacets.ordered == firstOrdered
                    } && firstOrdered != null -> firstOrdered

                    mdlMemberTypeDefinitions.all { it.mdlFundamentalFacets.ordered == OrderedFacet.Order.FALSE } ->
                        OrderedFacet.Order.FALSE

                    else -> OrderedFacet.Order.PARTIAL
                }

                val isBounded = mdlMemberTypeDefinitions.all { it.mdlFundamentalFacets.bounded }
                val isInfinite =
                    mdlMemberTypeDefinitions.any { it.mdlFundamentalFacets.cardinality == Cardinality.COUNTABLY_INFINITE }
                FundamentalFacets(
                    ordered = ordered,
                    bounded = isBounded,
                    cardinality = if (isInfinite) Cardinality.COUNTABLY_INFINITE else Cardinality.FINITE,
                    numeric = mdlMemberTypeDefinitions.all { it.mdlFundamentalFacets.numeric }
                )
            }

            else -> error("Compiler issue")

        }

        protected companion object {

            fun <R> recurseBaseType(
                startType: ResolvedSimpleType,
                valueFun: (ResolvedSimpleType) -> R
            ): R = when {
                startType is ResolvedBuiltinType -> valueFun(startType)
                startType.simpleDerivation is ResolvedUnionDerivation -> valueFun(startType)
                startType.simpleDerivation is ResolvedListDerivationBase -> valueFun(startType)
                else -> when (val base = startType.mdlBaseTypeDefinition) {
                    AnySimpleType -> valueFun(AnySimpleType)
                    is ResolvedSimpleType -> recurseBaseType(base, valueFun)
                    else -> error("Recursing non-simple base type ($base) of simple: ${startType}")
                }
            }
        }
    }

    enum class Variety {
        ATOMIC, LIST, UNION, NIL;

        fun notNil(): Variety {
            check(this != NIL) {
                "Attempting to copy nil variety"
            }
            return this
        }
    }

    companion object


}
