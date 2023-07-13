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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VString
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.model.*
import io.github.pdvrieze.formats.xmlschema.resolved.facets.FacetList
import io.github.pdvrieze.formats.xmlschema.resolved.particles.ResolvedParticle
import io.github.pdvrieze.formats.xmlschema.types.*
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.qname

sealed class ResolvedComplexType(
    final override val schema: ResolvedSchemaLike
) : ResolvedType,
    T_ComplexType,
    ResolvedLocalAttribute.Parent,
    ResolvedLocalElement.Parent,
    ResolvedParticleParent,
    ComplexTypeModel {
    abstract override val rawPart: XSComplexType

    abstract override val content: ResolvedComplexTypeContent

    protected abstract val model: Model

    override val mdlAbstract: Boolean get() = model.mdlAbstract
    override val mdlProhibitedSubstitutions: Set<ComplexTypeModel.Derivation> get() = model.mdlProhibitedSubstitutions
    override val mdlFinal: Set<ComplexTypeModel.Derivation> get() = model.mdlFinal
    override val mdlContentType: ResolvedContentType get() = model.mdlContentType
    override val mdlAttributeUses: Set<ResolvedAttribute> get() = model.mdlAttributeUses
    override val mdlAttributeWildcard: AnyModel get() = model.mdlAttributeWildcard
    override val mdlBaseTypeDefinition: ResolvedType get() = model.mdlBaseTypeDefinition
    override val mdlDerivationMethod: T_DerivationControl.ComplexBase get() = model.mdlDerivationMethod
    override val mdlAnnotations: AnnotationModel? get() = model.mdlAnnotations

    override fun validate(representation: VString) {
        when (val ct = mdlContentType) {
            is ResolvedSimpleContentType -> ct.mdlSimpleTypeDefinition.let { st ->
                st.mdlFacets.validate(
                    st.mdlPrimitiveTypeDefinition,
                    representation
                ); st.validate(representation)
            }

            is MixedContentType -> {
                check(ct.mdlParticle.mdlIsEmptiable()) { "Defaults are only valid for mixed content if the particle is emptiable" }
            }

            else -> error("The value ${representation} is not valid in an element-only complex type")
        }
    }

    override fun check(checkedTypes: MutableSet<QName>, inheritedTypes: SingleLinkedList<QName>) {
        checkNotNull(model)
    }

    fun collectConstraints(collector: MutableList<ResolvedIdentityConstraint>) {
        content.collectConstraints(collector)
    }


    sealed interface ResolvedDirectParticle<out T : ResolvedTerm> : ResolvedParticle<T>, T_ComplexType.DirectParticle {
        fun collectConstraints(collector: MutableList<ResolvedIdentityConstraint>)
    }

    interface Model : ComplexTypeModel {
        override val mdlBaseTypeDefinition: ResolvedType
        override val mdlFinal: Set<T_DerivationControl.ComplexBase>
        override val mdlContentType: ResolvedContentType
        override val mdlDerivationMethod: T_DerivationControl.ComplexBase
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

        override val mdlAttributeWildcard: AnyModel
            get() = TODO("not implemented")

    }

    protected abstract class ComplexModelBase(
        parent: ResolvedComplexType,
        rawPart: XSComplexType.ComplexBase,
        schema: ResolvedSchemaLike,
    ) : ModelBase(parent, rawPart, schema) {

        final override val mdlContentType: ResolvedContentType

        final override val mdlBaseTypeDefinition: ResolvedType

        final override val mdlDerivationMethod: T_DerivationControl.ComplexBase

        init {
            val baseTypeDefinition: ResolvedType
            val content: XSI_ComplexContent.Complex = rawPart.content
            val derivation: XSI_ComplexDerivation

            when (content) {
                is XSComplexContent -> {
                    derivation = content.derivation
                    val base = requireNotNull(derivation.base) { "Missing base attribute for complex type derivation" }
                    if ((parent as? ResolvedGlobalType)?.qName == base) {
                        require(schema is CollatedSchema.RedefineWrapper) { "Self-reference of type names can only happen in redefine" }

                        baseTypeDefinition = schema.nestedComplexType(base)
                    } else {

                        val seenTypes = mutableSetOf<QName>()
                        seenTypes.add(base)
                        val baseType = schema.type(base)

                        var b: ResolvedGlobalComplexType? = baseType as? ResolvedGlobalComplexType
                        while (b != null) {
                            val lastB = b
                            val b2 = (lastB.rawPart.content.derivation as? XSComplexContent.XSComplexDerivationBase)?.base
                            b = b2?.let { b2Name ->
                                if (lastB.qName == b2Name && lastB.schema is CollatedSchema.RedefineWrapper) {
//                                    val b3 = lastB.schema.originalSchema.complexTypes.single { it.name.xmlString == b2Name.localPart }
//                                    val sa = SchemaAssociatedElement(lastB.schema.originalLocation, b3)
//                                    ResolvedGlobalComplexType(sa, lastB.schema)
                                    lastB.schema.nestedComplexType(b2Name)
                                } else {
                                    require(seenTypes.add(b2)) { "Recursive type use in complex content: ${seenTypes.joinToString()}" }
                                    schema.type(b2) as? ResolvedGlobalComplexType
                                }
                            }
                        }

                        // Do this after recursion test (otherwise it causes a stack overflow)
                        when (derivation) {
                            is XSComplexContent.XSExtension ->
                                require(T_DerivationControl.EXTENSION !in baseType.mdlFinal) { "Type $base is final for extension" }

                            is XSComplexContent.XSRestriction ->
                                require(T_DerivationControl.RESTRICTION !in baseType.mdlFinal) { "Type $base is final for restriction" }
                        }

                        baseTypeDefinition = baseType
                    }

                }

                is XSComplexType.Shorthand -> {
                    derivation = content
                    check(derivation.base == null) { " Shorthand has no base" }
                    baseTypeDefinition = AnyType
                }

                else -> error("Should be unreachable (sealed type)")
            }

            mdlBaseTypeDefinition = baseTypeDefinition

            mdlDerivationMethod = when (derivation) {
                is XSComplexContent.XSExtension -> T_DerivationControl.EXTENSION
                else -> T_DerivationControl.RESTRICTION
            }


            val effectiveMixed = (content as? XSComplexContent)?.mixed ?: rawPart.mixed ?: false
            val term = derivation.term

            val explicitContent: ResolvedGroupParticle<*>? = when {
                (term == null) ||
                        (term.maxOccurs == T_AllNNI(0)) ||
                        (term is XSAll || term is XSSequence && !term.hasChildren()) ||
                        (term is XSChoice && term.minOccurs?.toUInt() == 0u && !term.hasChildren()) -> null


                else -> ResolvedGroupParticle.invoke(parent, term, schema)
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
                        derivation is XSComplexType.Shorthand ->
                    contentType(effectiveMixed, effectiveContent)


                baseTypeDefinition !is ResolvedComplexType -> // simple type
                    contentType(effectiveMixed, effectiveContent)

                baseTypeDefinition.mdlContentType.mdlVariety.let {
                    it == ComplexTypeModel.Variety.SIMPLE || it == ComplexTypeModel.Variety.EMPTY
                } -> contentType(effectiveMixed, effectiveContent)

                effectiveContent == null -> baseTypeDefinition.mdlContentType
                else -> {
                    val baseParticle = (baseTypeDefinition.mdlContentType as ResolvedElementBase).mdlParticle
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
                            val p: List<ResolvedParticle<ResolvedChoiceSeqMember>> =
                                (listOf(baseParticle) + listOfNotNull(effectiveContent).filterIsInstance<ResolvedChoiceSeqMember>())
                                    .filterIsInstance<ResolvedParticle<ResolvedChoiceSeqMember>>()

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

            val wildcardElement: XSI_OpenContent? =
                (rawPart as? XSComplexType.Shorthand)?.openContent
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
                is XSSimpleContentExtension -> require(T_DerivationControl.EXTENSION !in baseType.mdlFinal) { "${derivation.base} is final for extension" }
                is XSSimpleContentRestriction -> require(T_DerivationControl.RESTRICTION !in baseType.mdlFinal) { "${derivation.base} is final for extension" }
                else -> error("Unsupported derivation child.")
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
                        FacetList(derivation.facets, schema),
                        b.mdlFundamentalFacets,
                        b.mdlVariety.notNil(),
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

        override val mdlDerivationMethod: T_DerivationControl.ComplexBase =
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
        override val mdlAttributeWildcard: AnyModel

        override val mdlContentType: ResolvedSimpleContentType

        override val mdlSimpleTypeDefinition: ResolvedSimpleType

        override val mdlBaseTypeDefinition: ResolvedType

    }

    companion object {
        internal fun contentType(
            effectiveMixed: Boolean,
            particle: ResolvedParticle<*>?
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
                    T_DerivationControl.EXTENSION ->
                        for (a in t.mdlAttributeUses) {
                            require(put(a.mdlQName, a) == null) { "Duplicate attribute $a" }
                        }

                    T_DerivationControl.RESTRICTION ->
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
