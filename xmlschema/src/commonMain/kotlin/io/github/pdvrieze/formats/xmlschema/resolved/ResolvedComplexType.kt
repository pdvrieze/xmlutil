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
import io.github.pdvrieze.formats.xmlschema.datatypes.impl.SingleLinkedList
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.PrimitiveDatatype
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.model.*
import io.github.pdvrieze.formats.xmlschema.types.*
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.qname

sealed class ResolvedComplexType(
    final override val schema: ResolvedSchemaLike
) : ResolvedType, T_ComplexType, ResolvedLocalAttribute.Parent, ComplexTypeModel {
    abstract override val rawPart: XSComplexType

    abstract override val content: ResolvedComplexTypeContent

    protected abstract val model: Model

    override val mdlAbstract: Boolean get() = model.mdlAbstract
    override val mdlProhibitedSubstitutions: Set<out ComplexTypeModel.Derivation> get() = model.mdlProhibitedSubstitutions
    override val mdlFinal: Set<out ComplexTypeModel.Derivation> get() = model.mdlFinal
    override val mdlContentType: ResolvedContentType get() = model.mdlContentType
    override val mdlAttributeUses: Set<ResolvedAttribute> get() = model.mdlAttributeUses
    override val mdlAttributeWildcard: WildcardModel get() = model.mdlAttributeWildcard
    override val mdlBaseTypeDefinition: ResolvedType get() = model.mdlBaseTypeDefinition
    override val mdlDerivationMethod: T_TypeDerivationControl.ComplexBase get() = model.mdlDerivationMethod
    override val mdlAnnotations: AnnotationModel? get() = model.mdlAnnotations

    override fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>) {
        checkNotNull(model)
    }

    sealed interface ResolvedDirectParticle<T : ResolvedTerm> : ResolvedParticle<T>, T_ComplexType.DirectParticle

    interface Model : ComplexTypeModel {
        override val mdlBaseTypeDefinition: ResolvedType
        override val mdlFinal: Set<out T_TypeDerivationControl.ComplexBase>
        override val mdlContentType: ResolvedContentType
        override val mdlDerivationMethod: T_TypeDerivationControl.ComplexBase
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

        final override val mdlDerivationMethod: T_TypeDerivationControl.ComplexBase

        init {

            val content = rawPart.content
            val derivation: XSI_ComplexDerivation

            when (content) {
                is XSComplexContent -> {
                    derivation = content.derivation
                    if ((parent as? ResolvedGlobalType)?.qName == derivation.base) {
                        require(schema is CollatedSchema.RedefineWrapper) { "Self-reference of type names can only happen in redefine" }
                        val b =
                            schema.relativeBase.complexTypes.single { it.name.xmlString == derivation.base?.localPart }
                        mdlBaseTypeDefinition = ResolvedGlobalComplexType(b, schema)
                    } else {
                        val base =
                            requireNotNull(derivation.base) { "Missing base attribute for complex type derivation" }

                        val seenTypes = mutableSetOf<QName>()
                        seenTypes.add(base)
                        val baseType = schema.type(base)

                        var b: ResolvedGlobalComplexType? = baseType as? ResolvedGlobalComplexType
                        while (b != null) {
                            val b2 = (b.rawPart.content.derivation as? XSComplexContent.XSComplexDerivationBase)?.base
                            b = b2?.let {
                                require(seenTypes.add(b2)) { "Recursive type use in complex content: ${seenTypes.joinToString()}" }
                                schema.type(b2) as? ResolvedGlobalComplexType
                            }
                        }

                        // Do this after recursion test (otherwise it causes a stack overflow)
                        when (derivation) {
                            is XSComplexContent.XSExtension ->
                                require(T_TypeDerivationControl.EXTENSION !in baseType.mdlFinal) { "Type $base is final for extension" }
                            is XSComplexContent.XSRestriction ->
                                require(T_TypeDerivationControl.RESTRICTION !in baseType.mdlFinal) { "Type $base is final for restriction"}
                        }

                        mdlBaseTypeDefinition = baseType
                    }

                }

                is IXSComplexTypeShorthand -> {
                    derivation = content
                    check(derivation.base == null) { " Shorthand has no base" }
                    mdlBaseTypeDefinition = AnyType
                }
            }


            mdlDerivationMethod = when (derivation) {
                is XSComplexContent.XSExtension -> T_TypeDerivationControl.EXTENSION
                else -> T_TypeDerivationControl.RESTRICTION
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
                    contentType(effectiveMixed, effectiveContent, parent)


                mdlBaseTypeDefinition !is ResolvedComplexType -> // simple type
                    contentType(effectiveMixed, effectiveContent, parent)

                mdlBaseTypeDefinition.mdlContentType.mdlVariety.let {
                    it == ComplexTypeModel.Variety.SIMPLE || it == ComplexTypeModel.Variety.EMPTY
                } -> contentType(effectiveMixed, effectiveContent, parent)

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
                val w = wildcardElement.any ?: XSAny()
                val openContent = XSOpenContent(
                    mode = wildcardElement.mode ?: T_ContentMode.INTERLEAVE,
                    any = w
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

        final override val mdlSimpleTypeDefinition: ResolvedSimpleType

        init {
            val qname =
                (rawPart as? XSGlobalComplexType)?.let { qname(schema.targetNamespace?.value, it.name.xmlString) }

            val derivation = rawPart.content.derivation


            val baseType: ResolvedType = derivation.base?.let { schema.type(it) } ?: AnyType

            when (derivation) {
                is XSSimpleContentExtension -> require(T_TypeDerivationControl.EXTENSION !in baseType.mdlFinal) { "${derivation.base} is final for extension" }
                is XSSimpleContentRestriction -> require(T_TypeDerivationControl.RESTRICTION !in baseType.mdlFinal) { "${derivation.base} is final for extension" }
                else -> error("Compilation doesn't see exhaustion")
            }

            mdlBaseTypeDefinition = baseType


            val complexBaseDerivation = (baseType as? ResolvedComplexType)?.mdlDerivationMethod
            val baseTypeComplexBase = (baseType as? ResolvedComplexType)?.mdlBaseTypeDefinition
            val complexBaseContentType: ResolvedContentType? = (baseType as? ResolvedComplexType)?.mdlContentType

            when {
                baseType is ResolvedComplexType &&
                        complexBaseContentType is SimpleModelBase &&
                        derivation is XSSimpleContentRestriction -> {
                    val b: ResolvedSimpleType =
                        derivation.simpleType?.let { ResolvedLocalSimpleType(it, schema, parent) }
                            ?: complexBaseContentType.mdlSimpleTypeDefinition

                    mdlSimpleTypeDefinition = SyntheticSimpleType(
                        parent,
                        b,
                        derivation.facets,
                        b.mdlFundamentalFacets,
                        b.mdlVariety,
                        b.mdlPrimitiveTypeDefinition,
                        b.mdlItemTypeDefinition,
                        b.mdlMemberTypeDefinitions,
                        schema
                    )
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

                baseType is ResolvedComplexType &&
                        complexBaseContentType is ResolvedSimpleContentType &&
                        derivation is XSSimpleContentExtension ->
                    mdlSimpleTypeDefinition = requireNotNull(complexBaseContentType.mdlSimpleTypeDefinition)

                else -> mdlSimpleTypeDefinition = requireNotNull(baseType as? ResolvedSimpleType) {
                    "Simple content base types must be effectively simple ($baseType)"
                }
            }


        }

        override val mdlDerivationMethod: T_TypeDerivationControl.ComplexBase =
            rawPart.content.derivation.derivationMethod
    }

    interface ResolvedContentType : ComplexTypeModel.ContentType {

    }

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
        ComplexTypeModel.ContentType.Simple {
        override val mdlAttributeWildcard: WildcardModel

        override val mdlContentType: ResolvedSimpleContentType

        override val mdlSimpleTypeDefinition: ResolvedSimpleType

        override val mdlBaseTypeDefinition: ResolvedType

    }

    companion object {
        internal fun contentType(
            effectiveMixed: Boolean,
            particle: ResolvedParticle<*>?,
            parent: ResolvedComplexType
        ): ResolvedContentType {
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
                    T_TypeDerivationControl.EXTENSION ->
                        for (a in t.mdlAttributeUses) {
                            require(put(a.mdlQName, a) == null) { "Duplicate attribute $a" }
                        }

                    T_TypeDerivationControl.RESTRICTION ->
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

class SyntheticSimpleType(
    override val mdlContext: SimpleTypeContext,
    override val mdlBaseTypeDefinition: TypeModel,
    override val mdlFacets: List<XSFacet>,
    override val mdlFundamentalFacets: FundamentalFacets,
    override val mdlVariety: SimpleTypeModel.Variety,
    override val mdlPrimitiveTypeDefinition: PrimitiveDatatype?,
    override val mdlItemTypeDefinition: ResolvedSimpleType?,
    override val mdlMemberTypeDefinitions: List<ResolvedSimpleType>,
    override val schema: ResolvedSchemaLike,
) : ResolvedSimpleType, SimpleTypeModel.Local, ResolvedSimpleType.Model {
    override val mdlAnnotations: Nothing? get() = null
    override val mdlFinal: Set<Nothing> get() = emptySet()
    override val simpleDerivation: Nothing get() = error("Not supported")
    override val model: SyntheticSimpleType get() = this
    override val rawPart: Nothing get() = error("Not supported")
}

internal fun calcProhibitedSubstitutions(
    rawPart: XSGlobalComplexType,
    schema: ResolvedSchemaLike
): Set<out ComplexTypeModel.Derivation> {
    return rawPart.block ?: schema.blockDefault.toDerivationSet()
}

internal fun calcFinalSubstitutions(
    rawPart: XSGlobalComplexType,
    schema: ResolvedSchemaLike
): Set<T_TypeDerivationControl.ComplexBase> {
    return rawPart.final ?: schema.finalDefault.toDerivationSet()
}
