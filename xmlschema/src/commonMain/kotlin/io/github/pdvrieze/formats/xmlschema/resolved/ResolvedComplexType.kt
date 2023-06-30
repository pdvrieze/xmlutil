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

import io.github.pdvrieze.formats.xmlschema.datatypes.AnyType
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.PrimitiveDatatype
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.model.*
import io.github.pdvrieze.formats.xmlschema.model.ComplexTypeModel.DerivationMethod
import io.github.pdvrieze.formats.xmlschema.types.*
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.qname

sealed class ResolvedComplexType(
    final override val schema: ResolvedSchemaLike
) : ResolvedType, T_ComplexType, ResolvedLocalAttribute.Parent, ComplexTypeModel {
    abstract override val rawPart: XSComplexType

    abstract override val content: ResolvedComplexTypeContent

    protected abstract val model: Model

    override val mdlAbstract: Boolean get() = model.mdlAbstract
    override val mdlProhibitedSubstitutions: T_DerivationSet get() = model.mdlProhibitedSubstitutions
    override val mdlFinal: T_DerivationSet get() = model.mdlFinal
    override val mdlContentType: ResolvedContentType get() = model.mdlContentType
    override val mdlAttributeUses: Set<ResolvedAttribute> get() = model.mdlAttributeUses
    override val mdlAttributeWildcard: WildcardModel get() = model.mdlAttributeWildcard
    override val mdlBaseTypeDefinition: ResolvedType get() = model.mdlBaseTypeDefinition
    override val mdlDerivationMethod: DerivationMethod get() = model.mdlDerivationMethod
    override val mdlAnnotations: AnnotationModel? get() = model.mdlAnnotations

    sealed interface ResolvedDirectParticle<T : ResolvedTerm> : ResolvedParticle<T>, T_ComplexType.DirectParticle

    interface Model : ComplexTypeModel {
        override val mdlBaseTypeDefinition: ResolvedType
        override val mdlFinal: T_DerivationSet
        override val mdlContentType: ResolvedContentType

    }

    protected abstract class ModelBase(
        parent: ResolvedComplexType,
        rawPart: XSIComplexType,
        schema: ResolvedSchemaLike
    ) : Model {
        override val mdlAnnotations: AnnotationModel? = rawPart.annotation.models()


        override val mdlAttributeUses: Set<ResolvedAttribute> by lazy {
            calculateAttributeUses(
                schema,
                rawPart,
                parent
            )
        }

        override val mdlAttributeWildcard: WildcardModel
            get() = TODO("not implemented")

    }

    protected abstract class ComplexModelBase(
        parent: ResolvedComplexType,
        rawPart: XSComplexType.ComplexBase,
        schema: ResolvedSchemaLike,
    ) : ModelBase(parent, rawPart, schema) {

        final override val mdlContentType: ResolvedContentType

        final override val mdlBaseTypeDefinition: ResolvedType

        final override val mdlDerivationMethod: DerivationMethod

        init {

            val content = rawPart.content as XSI_ComplexContent.Complex
            val derivation: XSI_ComplexDerivation

            when (content) {
                is XSComplexContent -> {
                    derivation = content.derivation
                    mdlBaseTypeDefinition =
                        schema.type(requireNotNull(derivation.base) { "Missing base attribute for complex type derivation" })
                }

                is IXSComplexTypeShorthand -> {
                    derivation = content
                    check(derivation.base == null) { " Shorthand has no base" }
                    mdlBaseTypeDefinition = AnyType
                }
            }


            mdlDerivationMethod = when (derivation) {
                is XSComplexContent.XSExtension -> DerivationMethod.EXTENSION
                else -> DerivationMethod.RESTRICION
            }


            val effectiveMixed = (content as? XSComplexContent)?.mixed ?: rawPart.mixed ?: false
            val term = derivation.term

            val explicitContent: ResolvedGroupParticle<*>? = when {
                (term == null) ||
                        (term.maxOccurs == T_AllNNI(0)) ||
                        (term is XSAll || term is XSSequence && !term.hasChildren()) ||
                        (term is XSChoice && term.minOccurs?.toUInt() == 0u && !term.hasChildren()) -> null


                else -> ResolvedGroupParticle(parent, term, schema)
            }

            val effectiveContent: ResolvedParticle<*>? = explicitContent ?: when {
                !effectiveMixed -> null
                else -> ResolvedSequence(
                    parent,
                    XSSequence(minOccurs = VNonNegativeInteger(1), maxOccurs = T_AllNNI(1)),
                    schema
                )
            }

            val explicitContentType: ResolvedContentType = when {
                derivation is XSComplexContent.XSRestriction ||
                        derivation is IXSComplexTypeShorthand ->
                    contentType(effectiveMixed, effectiveContent)


                mdlBaseTypeDefinition !is ResolvedComplexType -> // simple type
                    contentType(effectiveMixed, effectiveContent)

                mdlBaseTypeDefinition.mdlContentType.mdlVariety.let {
                    it == ComplexTypeModel.Variety.SIMPLE || it == ComplexTypeModel.Variety.EMPTY
                } -> contentType(effectiveMixed, effectiveContent)

                effectiveContent == null -> mdlBaseTypeDefinition.mdlContentType
                else -> {
                    val baseParticle = (mdlBaseTypeDefinition.mdlContentType as ResolvedElementBase).mdlParticle
                    val baseTerm: ResolvedTerm = baseParticle.mdlTerm
                    val effectiveContentTerm = effectiveContent.mdlTerm
                    val part: ResolvedParticle<*> = when {
                        baseTerm is ResolvedAll && explicitContent == null -> baseParticle
                        (baseTerm is ResolvedAll && effectiveContentTerm is ResolvedAll) -> {
                            val p = baseTerm.mdlParticles + effectiveContentTerm.mdlParticles
                            SyntheticAll(
                                mdlMinOccurs = effectiveContent.mdlMinOccurs,
                                mdlMaxOccurs = T_AllNNI(1),
                                mdlParticles = p,
                                schema = schema
                            )
                        }

                        else -> {
                            val p: List<ResolvedParticle<ResolvedChoiceSeqTerm>> =
                                (listOf(baseParticle) + listOfNotNull(effectiveContent).filterIsInstance<ResolvedChoiceSeqTerm>())
                                    .filterIsInstance<ResolvedParticle<ResolvedChoiceSeqTerm>>()

                            SyntheticSequence(
                                mdlMinOccurs = VNonNegativeInteger(1),
                                mdlMaxOccurs = T_AllNNI(1),
                                mdlParticles = p,
                                schema = schema
                            )
                        }
                    }
                    when {
                        effectiveMixed -> MixedContentType(part)
                        else -> ElementOnlyContentType(part)
                    }
                }
            }

            val wildcardElement: XSI_OpenContent? = (rawPart as? IXSComplexTypeShorthand)?.openContent
                ?: (schema as? ResolvedSchema)?.defaultOpenContent?.takeIf {
                    explicitContentType.mdlVariety != ComplexTypeModel.Variety.EMPTY || it.appliesToEmpty
                }

            if (wildcardElement == null || wildcardElement.mode == T_ContentMode.NONE) {
                mdlContentType = explicitContentType
            } else {
                val particle = (explicitContentType as? ResolvedElementBase)?.mdlParticle
                    ?: SyntheticSequence(
                        mdlMinOccurs = VNonNegativeInteger.ONE,
                        mdlMaxOccurs = T_AllNNI.ONE,
                        mdlParticles = emptyList(),
                        schema = schema
                    )

                // TODO Add wildcard union
                val w = wildcardElement.content ?: XSAny()
                val openContent = XSOpenContent(
                    mode = wildcardElement.mode ?: T_ContentMode.INTERLEAVE,
                    content = w
                )

                mdlContentType = when {
                    effectiveMixed -> MixedContentType(
                        mdlParticle = particle,
                        mdlOpenContent = openContent,
                    )

                    else -> ElementOnlyContentType(
                        mdlParticle = particle,
                        mdlOpenContent = openContent,
                    )
                }
            }
        }
    }

    protected abstract class SimpleModelBase(
        parent: ResolvedComplexType,
        rawPart: XSComplexType.Simple,
        schema: ResolvedSchemaLike
    ) : ModelBase(parent, rawPart, schema), ResolvedSimpleContentType {

        final override val mdlBaseTypeDefinition: ResolvedType

        override val mdlContentType: ResolvedSimpleContentType get() = this

        final override val mdlSimpleTypeDefinition: SimpleTypeModel

        init {
            val qname =
                (rawPart as? XSGlobalComplexType)?.let { qname(schema.targetNamespace?.value, it.name.xmlString) }

            val derivation = rawPart.content.derivation

            val baseType: ResolvedType = derivation.base?.let { schema.type(it) } ?: AnyType

            mdlBaseTypeDefinition = baseType


            val complexBaseDerivation = (baseType as? ResolvedComplexType)?.mdlDerivationMethod
            val baseTypeComplexBase = (baseType as? ResolvedComplexType)?.mdlBaseTypeDefinition
            val complexBaseContentType = (baseType as? ComplexTypeModel)?.mdlContentType

            when {
                baseType is ResolvedComplexType &&
                        complexBaseContentType is SimpleModelBase &&
                        derivation is XSSimpleContentRestriction -> {
                    val b = derivation.simpleType?.let { ResolvedLocalSimpleType(it, schema, parent) }
                        ?: complexBaseContentType.mdlSimpleTypeDefinition

                    mdlSimpleTypeDefinition = object : SimpleTypeModel {
                        //                            override val mdlTargetNamespace: VAnyURI? = schema.targetNamespace
                        override val mdlFinal: Set<Nothing> get() = emptySet()

                        //                            override val mdlContext: ResolvedComplexType = context
                        override val mdlBaseTypeDefinition: TypeModel = b
                        override val mdlFacets: List<T_Facet> get() = TODO("restriction children facets")
                        override val mdlFundamentalFacets: List<T_Facet> get() = TODO("unclear what this is")
                        override val mdlVariety: SimpleTypeModel.Variety get() = TODO("b.{variety}")
                        override val mdlPrimitiveTypeDefinition: PrimitiveDatatype get() = TODO("b.{primitive type definition}")
                        override val mdlItemTypeDefinition: SimpleTypeModel get() = TODO("b.{item type definition}")
                        override val mdlMemberTypeDefinitions: List<SimpleTypeModel> get() = TODO("b.{memberTypeDefinitions}")
                        override val mdlAnnotations: Nothing? get() = null

                    }
                }

                baseType is ResolvedComplexType &&
                        complexBaseContentType is ComplexTypeModel.ContentType.Mixed &&
                        complexBaseContentType.mdlParticle.mdlIsEmptiable() &&
                        derivation is XSSimpleContentRestriction -> {
                    val sb =
                        derivation.simpleType ?: error("Simple type definition constrained violated: 3.4.2.2 - step 2")

                    val st = ResolvedLocalSimpleType(
                        XSLocalSimpleType(
                            simpleDerivation = XSSimpleRestriction(
                                simpleType = sb,
                                facets = derivation.facets
                            )
                        ), schema, parent
                    )

                    mdlSimpleTypeDefinition = st
                }

                else -> mdlSimpleTypeDefinition = baseType as ResolvedSimpleType
            }


        }

        override val mdlDerivationMethod: DerivationMethod =
            rawPart.content.derivation.derivationMethod
    }

    interface ResolvedContentType : ComplexTypeModel.ContentType
    object EmptyContentType : ComplexTypeModel.ContentType.Empty, ResolvedContentType
    interface ResolvedElementBase : ResolvedContentType, ComplexTypeModel.ContentType.ElementBase {
        override val mdlParticle: ResolvedParticle<ResolvedTerm>
        val mdlOpenContent: XSOpenContent?
    }

    class MixedContentType(
        override val mdlParticle: ResolvedParticle<*>,
        override val mdlOpenContent: XSOpenContent? = null
    ) : ComplexTypeModel.ContentType.Mixed, ResolvedElementBase {
        override val openContent: OpenContentModel? get() = null
    }

    class ElementOnlyContentType(
        override val mdlParticle: ResolvedParticle<*>,
        override val mdlOpenContent: XSOpenContent? = null
    ) : ComplexTypeModel.ContentType.Mixed, ResolvedElementBase {
        override val openContent: OpenContentModel? get() = null
    }

    interface ResolvedSimpleContentType : ResolvedContentType, ComplexTypeModel.SimpleContent,
        ComplexTypeModel.ContentType.Simple

    companion object {
        internal fun contentType(effectiveMixed: Boolean, particle: ResolvedParticle<*>?): ResolvedContentType {
            return when {
                particle == null -> EmptyContentType
                effectiveMixed -> MixedContentType(particle)
                else -> ElementOnlyContentType(particle)
            }
        }

        fun calculateAttributeUses(
            schema: ResolvedSchemaLike,
            rawPart: XSIComplexType,
            parent: ResolvedComplexType
        ): Set<ResolvedAttribute> {
            val defaultAttributeGroup = (schema as? ResolvedSchema)?.defaultAttributes
                ?.takeIf { rawPart.defaultAttributesApply != false }

            val prohibitedAttrs = mutableSetOf<QName>()

            val attributes = buildMap<QName, ResolvedAttribute> {
                // Defined attributes
                for (attr in rawPart.content.derivation.attributes) {
                    val resolvedAttribute = ResolvedLocalAttribute(parent, attr, schema)
                    when (resolvedAttribute.use) {
                        XSAttrUse.PROHIBITED -> prohibitedAttrs.add(resolvedAttribute.mdlQName)
                        else ->
                            put(resolvedAttribute.mdlQName, resolvedAttribute)?.also {
                                error("Duplicate attribute $it on type $parent")
                            }
                    }
                }

                // Defined attribute group references (including the default one)
                val groups = buildList {
                    defaultAttributeGroup?.let { ag -> add(schema.attributeGroup(ag)) }
                    rawPart.content.derivation.attributeGroups.mapTo(this) { schema.attributeGroup(it.ref) }
                }
                for (group in groups) {
                    val groupAttributeUses = group.attributeUses
                    val interSection = groupAttributeUses.intersect(this.keys)
                    check(interSection.isNotEmpty()) { "Duplicate attributes ($interSection) in attribute group" }
                    groupAttributeUses.associateByTo(this) { it.mdlQName }
                }

                // Extension/restriction. Only restriction can prohibit attributes.
                val t = parent.mdlBaseTypeDefinition as? ResolvedComplexType
                when (t?.mdlDerivationMethod) {
                    DerivationMethod.EXTENSION ->
                        for (a in t.mdlAttributeUses) {
                            require(put(a.mdlQName, a) == null) { "Duplicate attribute $a" }
                        }

                    DerivationMethod.RESTRICION ->
                        for (a in t.mdlAttributeUses) {
                            if (a.mdlQName !in prohibitedAttrs) {
                                getOrPut(a.mdlQName) { a }
                            }
                        }

                    null -> Unit
                }

            }
            val attributeUses = attributes.values.toSet()
            return attributeUses
        }

    }

}

internal fun calcProhibitedSubstitutions(rawPart: XSGlobalComplexType, schema: ResolvedSchemaLike): T_DerivationSet {
    return rawPart.block ?: schema.blockDefault.toDerivationSet()
}

internal fun calcFinalSubstitutions(rawPart: XSGlobalComplexType, schema: ResolvedSchemaLike): T_DerivationSet {
    return rawPart.final ?: schema.finalDefault.toDerivationSet()
}
