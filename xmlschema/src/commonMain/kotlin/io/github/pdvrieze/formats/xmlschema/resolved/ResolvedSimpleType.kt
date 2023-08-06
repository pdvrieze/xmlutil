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
import io.github.pdvrieze.formats.xmlschema.datatypes.impl.SingleLinkedList
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.*
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.FiniteDateType
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.NotationType
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.PrimitiveDatatype
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSWhiteSpace
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.resolved.facets.FacetList
import io.github.pdvrieze.formats.xmlschema.resolved.facets.ResolvedWhiteSpace
import io.github.pdvrieze.formats.xmlschema.types.CardinalityFacet.Cardinality
import io.github.pdvrieze.formats.xmlschema.types.FundamentalFacets
import io.github.pdvrieze.formats.xmlschema.types.OrderedFacet
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import nl.adaptivity.xmlutil.*

sealed interface ResolvedSimpleType : ResolvedType, ResolvedPart,
    VSimpleTypeScope.Member {
    override val rawPart: XSISimpleType

    override val mdlScope: VSimpleTypeScope

    val simpleDerivation: Derivation

    override val model: Model

    override val mdlBaseTypeDefinition: ResolvedSimpleType get() = model.mdlBaseTypeDefinition

    val mdlFacets: FacetList get() = model.mdlFacets

    val mdlFundamentalFacets: FundamentalFacets get() = model.mdlFundamentalFacets

    val mdlVariety: Variety get() = model.mdlVariety

    val mdlPrimitiveTypeDefinition: PrimitiveDatatype? get() = model.mdlPrimitiveTypeDefinition

    val mdlItemTypeDefinition: ResolvedSimpleType? get() = model.mdlItemTypeDefinition

    val mdlMemberTypeDefinitions: List<ResolvedSimpleType>
        get() = model.mdlMemberTypeDefinitions

    override val mdlFinal: Set<VDerivationControl.Type>

    override fun checkType(
        checkHelper: CheckHelper,
        inheritedTypes: SingleLinkedList<ResolvedType>
    ) { // TODO maybe move to toplevel
        val inherited = (this as? ResolvedGlobalType)?.run { inheritedTypes + this } ?: inheritedTypes
        simpleDerivation.checkDerivation(checkHelper, inherited)

        if (mdlPrimitiveTypeDefinition == NotationType) {
            for (enum in mdlFacets.enumeration) {
                val name = when (val v = enum.value) {
                    is VNotation -> v.value
                    is VPrefixString -> v.toQName()
                    else -> QName(enum.value.xmlString)
                }
                schema.notation(name)
            }
        }

        if (mdlVariety == Variety.LIST) {
            mdlFacets.checkList()
            if (mdlBaseTypeDefinition != AnySimpleType) {
                check(mdlBaseTypeDefinition.mdlVariety == Variety.LIST)
                check(VDerivationControl.RESTRICTION !in mdlBaseTypeDefinition.mdlFinal)
            }
        }
        mdlFacets.check(this.mdlPrimitiveTypeDefinition)
    }

    override fun validate(representation: VString) {
        check(this != mdlPrimitiveTypeDefinition) { "$mdlPrimitiveTypeDefinition fails to override validate" }
        mdlPrimitiveTypeDefinition?.validate(representation)
        mdlFacets.validate(mdlPrimitiveTypeDefinition, representation)
    }

    override fun isValidSubtitutionFor(other: ResolvedType): Boolean {
        when (other) {
            is ResolvedSimpleType -> return isValidlyDerivedFrom(other)
            is ResolvedComplexType -> return isValidlyDerivedFrom(other)
            else -> return false
        }
    }

    /**
     * 3.16.6.3
     */
    fun isValidlyDerivedFrom(complexBase: ResolvedComplexType): Boolean {
        // 1 - never true, as this is a simple type, the base is complex
        if (VDerivationControl.RESTRICTION in complexBase.mdlFinal) return false
        if (mdlBaseTypeDefinition == complexBase.mdlBaseTypeDefinition) return true //2.2.1
        if (VDerivationControl.RESTRICTION in mdlBaseTypeDefinition.mdlFinal) return false //2.1.b
        if (mdlVariety != Variety.ATOMIC && mdlBaseTypeDefinition == AnySimpleType) return true //2.2.3
        if (mdlBaseTypeDefinition != AnyType && mdlBaseTypeDefinition.isValidlyDerivedFrom(complexBase)) return true //2.2.2
        val sd = simpleDerivation
        if (sd is ResolvedUnionDerivation) { //2.2.4.1
            // Facets should be unassignable in union -- 2.2.4.3
            val members = sd.transitiveMembership()//2.2.4.2
            return !members.none { m -> isValidlyDerivedFrom(m) }
        }
        return false //none of the 4 options is true
    }

    /**
     * 3.4.6.5
     */
    override fun isValidlyDerivedFrom(simpleBase: ResolvedSimpleType): Boolean {
        if (this == simpleBase) return true
        if (VDerivationControl.RESTRICTION in simpleBase.mdlFinal) return false //2.1.a
        if (mdlBaseTypeDefinition == simpleBase) return true //2.2.1
        if (VDerivationControl.RESTRICTION in mdlBaseTypeDefinition.mdlFinal) return false //2.1.b
        if (mdlVariety != Variety.ATOMIC && mdlBaseTypeDefinition == AnySimpleType) return true //2.2.3
        if (mdlBaseTypeDefinition != AnyType && mdlBaseTypeDefinition.isValidlyDerivedFrom(simpleBase)) return true //2.2.2
        val sd = simpleDerivation
        if (sd is ResolvedUnionDerivation) { //2.2.4.1
            // Facets should be unassignable in union -- 2.2.4.3
            val members = sd.transitiveMembership()//2.2.4.2
            return !members.none { m -> isValidlyDerivedFrom(m) }
        }
        return false //none of the 4 options is true
    }

    sealed class Derivation(
        rawPart: XSSimpleDerivation?,
        final override val schema: ResolvedSchemaLike,
        inheritedTypes: SingleLinkedList<ResolvedType>
    ) :
        ResolvedAnnotated {
        abstract override val model: ResolvedAnnotated.IModel

        /** Abstract as it is static for union/list. In those cases always AnySimpleType */
        abstract val baseType: ResolvedSimpleType

        abstract fun checkDerivation(checkHelper: CheckHelper, inheritedTypes: SingleLinkedList<ResolvedType>)

    }

    interface Model : ResolvedAnnotated.IModel {
        val mdlItemTypeDefinition: ResolvedSimpleType?
        val mdlMemberTypeDefinitions: List<ResolvedSimpleType>
        val mdlBaseTypeDefinition: ResolvedSimpleType
        val mdlFacets: FacetList
        val mdlFundamentalFacets: FundamentalFacets
        val mdlVariety: Variety
        val mdlPrimitiveTypeDefinition: PrimitiveDatatype?
    }

    sealed class ModelBase(
        rawPart: XSISimpleType,
        protected val schema: ResolvedSchemaLike,
        context: ResolvedSimpleType,
        inheritedTypes: SingleLinkedList<ResolvedType>
    ) : ResolvedAnnotated.Model(rawPart), Model {

        final override val mdlBaseTypeDefinition: ResolvedSimpleType
        final override val mdlItemTypeDefinition: ResolvedSimpleType?
        final override val mdlMemberTypeDefinitions: List<ResolvedSimpleType>
        final override val mdlVariety: Variety
        final override val mdlPrimitiveTypeDefinition: PrimitiveDatatype?

        init {
            val newInheritedTypes = inheritedTypes + context

            val typeName =
                (rawPart as? XSGlobalSimpleType)?.let { qname(schema.targetNamespace?.value, it.name.xmlString) }

            checkRecursiveTypes(context, schema = schema)

            val simpleDerivation = rawPart.simpleDerivation

            mdlBaseTypeDefinition = when {
                simpleDerivation !is XSSimpleRestriction -> AnySimpleType

                rawPart is XSGlobalSimpleType &&
                        typeName != null && simpleDerivation.base != null && typeName.isEquivalent(simpleDerivation.base) -> {
                    require(schema is CollatedSchema.RedefineWrapper) { "Only redefines can have 'self-referencing types'" }
                    ResolvedGlobalSimpleType(
                        schema.originalSchema.simpleTypes.single { it.name.xmlString == typeName.localPart },
                        schema,
                        newInheritedTypes
                    )
                }

                simpleDerivation.base?.isEquivalent(AnySimpleType.mdlQName) == true -> AnySimpleType

                else -> simpleDerivation.base?.let {
                    require(typeName == null || !it.isEquivalent(typeName))
                    schema.simpleType(it, newInheritedTypes)
                } ?: ResolvedLocalSimpleType(simpleDerivation.simpleType!!, schema, context, newInheritedTypes)
            }


            mdlVariety = when (simpleDerivation) {
                is XSSimpleList -> Variety.LIST
                is XSSimpleRestriction -> recurseBaseType(mdlBaseTypeDefinition) {
                    when {
                        it is AnySimpleType -> {
                            require(schema.targetNamespace?.value == XmlSchemaConstants.XS_NAMESPACE) {
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
                Variety.LIST -> when (mdlBaseTypeDefinition) {
                    AnySimpleType -> (simpleDerivation as XSSimpleList).itemTypeName?.let { schema.simpleType(it, newInheritedTypes) }
                        ?: ResolvedLocalSimpleType(simpleDerivation.simpleType!!, schema, context, newInheritedTypes)

                    else -> recurseBaseType(mdlBaseTypeDefinition) { it.mdlItemTypeDefinition }
                }

                else -> null
            }

            mdlMemberTypeDefinitions = when {
                simpleDerivation !is XSSimpleUnion -> emptyList()
                mdlBaseTypeDefinition == AnySimpleType -> simpleDerivation.memberTypes?.map {
                    schema.simpleType(it, newInheritedTypes)
                } ?: simpleDerivation.simpleTypes.map {
                        ResolvedLocalSimpleType(it, schema, context, newInheritedTypes)
                    }

                else -> recurseBaseType(
                    mdlBaseTypeDefinition,
                ) { it.mdlMemberTypeDefinitions }
            }


            mdlPrimitiveTypeDefinition = when (mdlVariety) {
                Variety.ATOMIC -> when (val b = mdlBaseTypeDefinition) {
                    is PrimitiveDatatype -> b
                    else -> recurseBaseType(mdlBaseTypeDefinition) { it.mdlPrimitiveTypeDefinition }
                        ?: run { null }
                }

                else -> null
            }


        }


        final override val mdlFacets: FacetList = when (val d = rawPart.simpleDerivation) {
            is XSSimpleRestriction -> {
                mdlBaseTypeDefinition.mdlFacets.overlay(FacetList(d.facets, schema, mdlPrimitiveTypeDefinition))
            }

            is XSSimpleList -> FacetList(
                whiteSpace =
                ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true), schema)
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

            fun checkRecursiveTypes(
                startType: ResolvedSimpleType,
                seenTypes: SingleLinkedList<QName> = SingleLinkedList(),
                schema: ResolvedSchemaLike,
                container: QName? = null,
            ) {
                if (startType is ResolvedBuiltinType) return // ignore builtins

                val name: QName? = (startType as? ResolvedGlobalSimpleType)?.mdlQName

//                if (name in seenTypes) error("Indirect recursive use of simple base types: $name in $container")

                val simpleDerivation: XSSimpleDerivation = startType.rawPart.simpleDerivation
                when (simpleDerivation){
                    is XSSimpleUnion -> {
                        val newSeen = name?.let { seenTypes + it } ?: seenTypes
                        for(childName in simpleDerivation.memberTypes ?: emptyList()) {
                            // Check the members, but allow for redefine
                            require(childName !in seenTypes ||
                                        (schema is CollatedSchema.RedefineWrapper && schema.elementName == childName)) {
                                "Indirect recursive use of simple base types: $childName in $name"
                            }
                            checkRecursiveTypes(schema.simpleType(childName), newSeen, schema, name ?: container)
                        }
                        for (rawChild in simpleDerivation.simpleTypes) {
                            val child = ResolvedLocalSimpleType(rawChild, schema, startType, SingleLinkedList())
                            checkRecursiveTypes(child, newSeen, schema, name ?: container)
                        }
                    }

                    is XSSimpleList -> {
                        val newSeen = name?.let { seenTypes + it } ?: seenTypes
                        val itemName = simpleDerivation.itemTypeName
                        val itemType : ResolvedSimpleType = if (itemName!=null) {
                            require(simpleDerivation.simpleType==null) { "Lists have itemType xor simpleType" }
                            require (itemName !in newSeen || (schema is CollatedSchema.RedefineWrapper && schema.elementName == itemName)) {
                                "Indirect recursive use of simple base types: ${simpleDerivation.itemTypeName} in $name"
                            }
                            schema.simpleType(itemName)
                        } else {
                            val localItem = requireNotNull(simpleDerivation.simpleType) { "Lists must have itemType xor SimpleType" }
                            ResolvedLocalSimpleType(localItem, schema, startType, SingleLinkedList())
                        }
                        checkRecursiveTypes(itemType, newSeen, schema, name ?: container)
                    }

                    is XSSimpleRestriction -> {

                        val newSeen = name?.let { seenTypes + it } ?: seenTypes

                        val baseName = simpleDerivation.base
                        if (baseName != null) {
                            require(baseName !in seenTypes ||
                                    (schema is CollatedSchema.RedefineWrapper && schema.elementName == baseName)) {
                                "Indirect recursive use of simple base types: $baseName in $name"
                            }
                            checkRecursiveTypes(schema.simpleType(baseName), newSeen, schema, name ?: container)

                        } else {
                            val baseItem = requireNotNull(simpleDerivation.simpleType) { "Restrictions must have base xor SimpleType" }
                            val child = ResolvedLocalSimpleType(baseItem, schema, startType, SingleLinkedList())
                            checkRecursiveTypes(child, newSeen, schema, name ?: container)
                        }
                    }
                }
            }

            fun <R> recurseBaseType(
                startType: ResolvedSimpleType,
                valueFun: (ResolvedSimpleType) -> R
            ): R = when {
                startType is ResolvedBuiltinType -> valueFun(startType)
                startType.simpleDerivation is ResolvedUnionDerivation -> valueFun(startType)
                startType.simpleDerivation is ResolvedListDerivationBase -> valueFun(startType)
                else -> when (val base = startType.mdlBaseTypeDefinition) {
                    AnySimpleType -> valueFun(base)
                    else -> recurseBaseType(base, valueFun)
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
