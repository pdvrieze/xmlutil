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
import io.github.pdvrieze.formats.xmlschema.datatypes.impl.SingleLinkedList
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VPrefixString
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VString
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.FiniteDateType
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.NotationType
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.PrimitiveDatatype
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSWhiteSpace
import io.github.pdvrieze.formats.xmlschema.model.AnnotationModel
import io.github.pdvrieze.formats.xmlschema.model.SimpleTypeModel
import io.github.pdvrieze.formats.xmlschema.model.SimpleTypeModel.Variety
import io.github.pdvrieze.formats.xmlschema.model.TypeModel
import io.github.pdvrieze.formats.xmlschema.resolved.facets.FacetList
import io.github.pdvrieze.formats.xmlschema.resolved.facets.ResolvedWhiteSpace
import io.github.pdvrieze.formats.xmlschema.types.*
import io.github.pdvrieze.formats.xmlschema.types.CardinalityFacet.Cardinality
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.isEquivalent
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.qname

sealed interface ResolvedSimpleType : ResolvedType, T_SimpleType, SimpleTypeModel {
    override val simpleDerivation: Derivation

    val model: Model

    override val mdlAnnotations: AnnotationModel? get() = model.mdlAnnotations

    override val mdlBaseTypeDefinition: ResolvedSimpleType get() = model.mdlBaseTypeDefinition

    override val mdlFacets: FacetList get() = model.mdlFacets

    override val mdlFundamentalFacets: FundamentalFacets get() = model.mdlFundamentalFacets

    override val mdlVariety: Variety get() = model.mdlVariety

    override val mdlPrimitiveTypeDefinition: PrimitiveDatatype? get() = model.mdlPrimitiveTypeDefinition

    override val mdlItemTypeDefinition: ResolvedSimpleType? get() = model.mdlItemTypeDefinition

    override val mdlMemberTypeDefinitions: List<ResolvedSimpleType>
        get() = model.mdlMemberTypeDefinitions

    override val mdlFinal: Set<TypeModel.Derivation>
        get() = model.mdlFinal

    override fun check(
        checkedTypes: MutableSet<QName>,
        inheritedTypes: SingleLinkedList<QName>
    ) { // TODO maybe move to toplevel

        when (val n = (this as? OptNamedPart)?.name) {
            null -> simpleDerivation.check(checkedTypes, inheritedTypes)
            else -> {
                val qName = n.toQname(schema.targetNamespace)
                checkedTypes.add(qName)
                simpleDerivation.check(checkedTypes, inheritedTypes + qName)
            }
        }

        if (mdlPrimitiveTypeDefinition == NotationType) {
            for (enum in mdlFacets.enumeration) {
                schema.notation((enum.value as? VPrefixString)?.toQName() ?: QName(enum.value.xmlString))
            }
        }

        if (mdlVariety == Variety.LIST) {
            mdlFacets.checkList()
            if (mdlBaseTypeDefinition != AnySimpleType) {
                check(mdlBaseTypeDefinition.mdlVariety == Variety.LIST)
                check(T_DerivationControl.RESTRICTION !in mdlBaseTypeDefinition.mdlFinal)
            }
        }
    }

    override fun validate(representation: VString) {
        check(this != mdlPrimitiveTypeDefinition) { "$mdlPrimitiveTypeDefinition fails to override validate" }
        mdlPrimitiveTypeDefinition?.validate(representation)
        mdlFacets.validate(mdlPrimitiveTypeDefinition, representation)
    }

    sealed class Derivation(final override val schema: ResolvedSchemaLike) : T_SimpleType.Derivation, ResolvedPart {
        final override val annotation: XSAnnotation? get() = rawPart.annotation
        final override val id: VID? get() = rawPart.id

        abstract override val rawPart: T_SimpleType.Derivation
        abstract val baseType: ResolvedSimpleType
        abstract fun check(checkedTypes: MutableSet<QName>, inheritedTypes: SingleLinkedList<QName>)
    }

    interface Model : SimpleTypeModel {
        override val mdlFinal: Set<TypeModel.Derivation>
        override val mdlItemTypeDefinition: ResolvedSimpleType?
        override val mdlMemberTypeDefinitions: List<ResolvedSimpleType>
        override val mdlBaseTypeDefinition: ResolvedSimpleType
    }

    sealed class ModelBase(
        rawPart: XSISimpleType,
        protected val schema: ResolvedSchemaLike,
        context: ResolvedSimpleType
    ) : SimpleTypeModel, Model {

        final override val mdlAnnotations: AnnotationModel? = rawPart.annotation.models()
        final override val mdlBaseTypeDefinition: ResolvedSimpleType
        final override val mdlItemTypeDefinition: ResolvedSimpleType?
        final override val mdlMemberTypeDefinitions: List<ResolvedSimpleType>
        final override val mdlVariety: Variety
        final override val mdlPrimitiveTypeDefinition: PrimitiveDatatype?

        init {
            val typeName =
                (rawPart as? XSGlobalSimpleType)?.let { qname(schema.targetNamespace?.value, it.name.xmlString) }
            val simpleDerivation = rawPart.simpleDerivation

            mdlBaseTypeDefinition = when {
                simpleDerivation !is XSSimpleRestriction -> AnySimpleType

                rawPart is XSGlobalSimpleType &&
                        typeName != null && simpleDerivation.base != null && typeName.isEquivalent(simpleDerivation.base) -> {
                    require(schema is CollatedSchema.RedefineWrapper) { "Only redefines can have 'self-referencing types'" }
                    ResolvedGlobalSimpleType(
                        schema.originalSchema.simpleTypes.single { it.name.xmlString == typeName.localPart },
                        schema
                    )
                }

                simpleDerivation.base?.isEquivalent(AnySimpleType.qName) == true -> AnySimpleType

                else -> simpleDerivation.base?.let {
                    require(typeName == null || !it.isEquivalent(typeName))
                    schema.simpleType(it)
                } ?: ResolvedLocalSimpleType(simpleDerivation.simpleType!!, schema, context)
            }


            mdlVariety = when (simpleDerivation) {
                is XSSimpleList -> Variety.LIST
                is XSSimpleRestriction -> recurseBaseType(mdlBaseTypeDefinition) {
                    when (it) {
                        AnySimpleType -> {
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
                    AnySimpleType -> (simpleDerivation as XSSimpleList).itemTypeName?.let { schema.simpleType(it) }
                        ?: ResolvedLocalSimpleType(simpleDerivation.simpleType!!, schema, context)

                    else -> recurseBaseType(mdlBaseTypeDefinition) { it.mdlItemTypeDefinition }
                }

                else -> null
            }

            mdlMemberTypeDefinitions = when {
                simpleDerivation !is XSSimpleUnion -> emptyList()
                mdlBaseTypeDefinition == AnySimpleType -> simpleDerivation.memberTypes?.map { schema.simpleType(it) }
                    ?: simpleDerivation.simpleTypes.map { ResolvedLocalSimpleType(it, schema, this@ModelBase) }

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
                mdlBaseTypeDefinition.mdlFacets.override(FacetList(d.facets, schema))
            }

            is XSSimpleList -> FacetList(
                whiteSpace =
                ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), schema)
            )

            is XSSimpleUnion -> FacetList.EMPTY
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

        }

        protected companion object {

            fun <R> recurseBaseType(
                startType: ResolvedSimpleType,
                seenTypes: SingleLinkedList<ResolvedSimpleType> = SingleLinkedList(),
                valueFun: (ResolvedSimpleType) -> R
            ): R {
                when {
                    startType is ResolvedBuiltinType -> return valueFun(startType)
                    startType in seenTypes -> throw IllegalArgumentException("Loop in base type definition")
                    startType.simpleDerivation is ResolvedUnionDerivation -> return valueFun(startType)
                    startType.simpleDerivation is ResolvedListDerivationBase -> return valueFun(startType)
                    else -> {}
                }

                val newSeen: SingleLinkedList<ResolvedSimpleType> = seenTypes + startType

                return when (val base = startType.mdlBaseTypeDefinition) {
                    AnySimpleType -> return valueFun(base)
                    !is ResolvedSimpleType -> error("Recursing should not go to anytype")
                    else -> {
                        recurseBaseType(base, newSeen, valueFun)
                    }
                }
            }
        }
    }


}
