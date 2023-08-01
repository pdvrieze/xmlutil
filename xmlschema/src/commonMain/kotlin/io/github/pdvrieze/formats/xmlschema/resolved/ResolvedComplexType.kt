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
    override val mdlAttributeWildcard: ResolvedAny? get() = model.mdlAttributeWildcard
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
                check(ct.mdlParticle.mdlIsEmptiable()) { "($rawPart) Defaults ($representation) are only valid for mixed content if the particle is emptiable" }
            }

            else -> error("The value ${representation} is not valid in an element-only complex type")
        }
    }


    /** 3.3.4.2 for complex types */
    override fun isValidSubtitutionFor(other: ResolvedType): Boolean {
        return when (other) {
            is ResolvedComplexType -> return isValidlyDerivedFrom(other)
            is ResolvedSimpleType -> return isValidlyDerivedFrom(other)
            else -> error("Unreachable")
        }
    }

    /**
     * 3.4.6.5 Type derivation ok (complex)
     */
    override fun isValidlyDerivedFrom(simpleBase: ResolvedSimpleType): Boolean {
        if(this == simpleBase) return true // 2.1
        // check derivation method is not in blocking
        if (this == simpleBase) return true // 2.2
        val btd = mdlBaseTypeDefinition
        if (btd==AnyType) return false // 2.3.1
        return when(btd) {
            is ResolvedComplexType -> btd.isValidlyDerivedFrom(simpleBase)
            is ResolvedSimpleType -> btd.isValidlyDerivedFrom(simpleBase)
        }
    }

    /**
     * 3.4.6.5 Type derivation ok (complex)
     */
    fun isValidlyDerivedFrom(complexBase: ResolvedComplexType): Boolean {
        if(this == complexBase) return true // 2.1
        // check derivation method is not in blocking
        if (this == complexBase) return true // 2.2
        val btd = mdlBaseTypeDefinition
        if (btd==AnyType) return false // 2.3.1
        return when(btd) {
            is ResolvedComplexType -> btd.isValidlyDerivedFrom(complexBase)
            is ResolvedSimpleType -> btd.isValidlyDerivedFrom(complexBase)
        }
    }

    override fun check(checkedTypes: MutableSet<QName>, inheritedTypes: SingleLinkedList<QName>) {
        checkNotNull(model)
        mdlContentType.check()
        if (mdlDerivationMethod == T_DerivationControl.EXTENSION) {
            val baseType = mdlBaseTypeDefinition
            if (baseType is ResolvedComplexType) {
                require(T_DerivationControl.EXTENSION !in baseType.mdlFinal) { "Final types cannot be extended" }
                require(mdlAttributeUses.containsAll(baseType.mdlAttributeUses)) { "Base attribute uses must be a subset of the extension" }
                val baseWc = baseType.mdlAttributeWildcard
                if (baseWc != null) {
                    val wc = requireNotNull(mdlAttributeWildcard)
                    // TODO apply 3.10.6.2 rules
                    require(wc.mdlNamespaceConstraint.containsAll(baseWc.mdlNamespaceConstraint))
                }

                when (val baseCType = baseType.mdlContentType) {
                    is ResolvedSimpleContentType ->
                        require(baseCType.mdlSimpleTypeDefinition == (mdlContentType as ResolvedSimpleContentType).mdlSimpleTypeDefinition) {
                            "3.4.6.2 - 1.4.1 - Simple content types must have the same simple type definition"
                        }

                    is EmptyContentType -> {}//require(mdlContentType is EmptyContentType)

                    is MixedContentType -> {
                        require(mdlContentType is MixedContentType)
                        // Ensure chcking particle extensions
                        val bot = baseCType.mdlOpenContent
                        val eot = (mdlContentType as MixedContentType).mdlOpenContent
                        require(bot == null || eot?.mdlMode == OpenContentModel.Mode.INTERLEAVE || (bot.mdlMode == OpenContentModel.Mode.SUFFIX && eot?.mdlMode == OpenContentModel.Mode.SUFFIX))
                        if (bot != null && eot != null) {
                            require(eot.mdlWildCard!!.mdlNamespaceConstraint.containsAll(bot.mdlWildCard!!.mdlNamespaceConstraint))
                        }
                    }

                    is ElementOnlyContentType -> {
                        require(mdlContentType is ElementOnlyContentType) { "Content type for complex extension must match: ${mdlContentType.mdlVariety}!= ${baseCType.mdlVariety}" }
                        // Ensure chcking particle extensions

                    }
                }
            } else { // extension of simple type
            }
        } else { // restriction
            val b = mdlBaseTypeDefinition
            val ct: ResolvedContentType = mdlContentType
//            check (b is ResolvedComplexType) { "Restriction must be based on a complex type" }
            val bt = (b as? ResolvedComplexType)?.mdlContentType
            when {
                b == AnyType -> {} // Derivation is fine

                ct is ResolvedSimpleContentType -> {
                    when(bt) {
                        is ResolvedSimpleContentType -> {
                            val sb = bt.mdlSimpleTypeDefinition
                            val st = ct.mdlSimpleTypeDefinition
                            check(st.isValidlyDerivedFrom(sb)) { "For derivation, simple content models must validly derive" }
                        }

                        is MixedContentType ->
                            check(bt.mdlParticle.mdlIsEmptiable()) { "Simple variety can only restrict emptiable mixed content type" }

                        else -> error("Invalid derivation of ${bt?.mdlVariety} by simple")
                    }
                }

                ct is EmptyContentType -> {
                    when (bt) {
                        is ResolvedElementBase -> {
                            check(bt.mdlParticle.mdlIsEmptiable())
                        }
                        !is EmptyContentType -> error("Invalid derivation of ${bt?.mdlVariety} by empty")
                    }
                }

                ct is ElementOnlyContentType -> {
                    check(bt is ResolvedElementBase) { "ElementOnly content type can only derive elementOnly or mixed" }
                    check(ct.restricts(bt))
                }

                ct is MixedContentType -> {
                    check(bt is MixedContentType) { "Mixed content type can only derive from mixed content" }
                    check(ct.restricts(bt))
                }
            }

            // check attributes : 3.4.6.3, item 3
            // check attributes : 3.4.6.3, item 4
            // check attributes : 3.4.6.3, item 5 assertions is an extension of b.extensions
        }
    }

    fun collectConstraints(collector: MutableList<ResolvedIdentityConstraint>) {
        content.collectConstraints(collector)
    }


    sealed interface ResolvedDirectParticle<out T : ResolvedTerm> : ResolvedParticle<T>,
        T_Particle {
        fun collectConstraints(collector: MutableList<ResolvedIdentityConstraint>)
        override fun check(checkedTypes: MutableSet<QName>) {

            super.check(checkedTypes)
            check(mdlMinOccurs <= mdlMaxOccurs) { "MinOccurs should be <= than maxOccurs" }

        }
    }

    interface Model : ComplexTypeModel {
        override val mdlBaseTypeDefinition: ResolvedType
        override val mdlFinal: Set<T_DerivationControl.ComplexBase>
        override val mdlContentType: ResolvedContentType
        override val mdlDerivationMethod: T_DerivationControl.ComplexBase
        override val mdlAttributeWildcard: ResolvedAny?
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

        override val mdlAttributeWildcard: ResolvedAny? // TODO do more
            get() = null

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
                            val b2 =
                                (lastB.rawPart.content.derivation as? XSComplexContent.XSComplexDerivationBase)?.base
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


            val effectiveMixed = (content as? XSComplexContent)?.mixed?.also { require(rawPart.mixed==null || rawPart.mixed==it) } ?: rawPart.mixed ?: false
            val term = derivation.term

            val explicitContent: ResolvedGroupParticle<ResolvedGroupLikeTerm>? = when {
                (term == null) ||
                        (term.maxOccurs == T_AllNNI(0)) ||
                        ((term is XSAll && ! term.hasChildren()) || (term is XSSequence && !term.hasChildren())) ||
                        (term is XSChoice && term.minOccurs?.toUInt() == 0u && !term.hasChildren()) -> null


                else -> ResolvedGroupParticle.invoke(parent, term, schema)
            }

            val effectiveContent: ResolvedParticle<ResolvedGroupLikeTerm>? = explicitContent ?: when {
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
                    val part: ResolvedDirectParticle<IResolvedGroupMember> = when {
                        baseParticle is ResolvedAll && explicitContent == null -> baseParticle
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
                            require(baseParticle.mdlTerm !is ResolvedAll) { "All can not be part of a sequence" }

                            val p: List<ResolvedParticle<ResolvedChoiceSeqMember>> =
                                (listOf(baseParticle) + listOfNotNull(effectiveContent))
                                    .map {
                                        check(it.mdlTerm is ResolvedChoiceSeqMember) { "${it.mdlTerm} is not valid inside a sequence" }
                                        it as ResolvedParticle<ResolvedChoiceSeqMember>
                                    }

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
                val particle: ResolvedParticle<ResolvedGroupLikeTerm> = (explicitContentType as? ResolvedElementBase)?.mdlParticle
                    ?: SyntheticSequence(
                        mdlMinOccurs = VNonNegativeInteger.ONE,
                        mdlMaxOccurs = T_AllNNI.ONE,
                        mdlParticles = emptyList(),
                        schema = schema
                    )

                // TODO Add wildcard union
                val w = wildcardElement.any ?: XSAny()
                val openContent = ResolvedOpenContent(
                    XSOpenContent(
                        mode = wildcardElement.mode ?: T_ContentMode.INTERLEAVE,
                        any = w
                    ), schema
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
                        FacetList(derivation.facets, schema, b.mdlPrimitiveTypeDefinition),
                        b.mdlFundamentalFacets,
                        b.mdlVariety.notNil(),
                        b.mdlPrimitiveTypeDefinition,
                        b.mdlItemTypeDefinition,
                        b.mdlMemberTypeDefinitions,
                        schema
                    )
                }

                baseType is ResolvedComplexType &&
                        complexBaseContentType is MixedContentType &&
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

        override fun check() {}
    }

    interface ResolvedContentType : ComplexTypeModel.ContentType {
        fun check()
    }

    object EmptyContentType : ComplexTypeModel.ContentType.Empty, ResolvedContentType {
        override fun check() {}
    }

    interface ResolvedElementBase : ResolvedContentType, ComplexTypeModel.ContentType.ElementBase {
        override val mdlParticle: ResolvedParticle<ResolvedGroupLikeTerm>
        val mdlOpenContent: ResolvedOpenContent?

        /** Implementation of 3.4.6.4 */
        fun restricts(base: ResolvedElementBase): Boolean {
            // 1. every sequence of elements valid in this is also (locally -3.4.4.2) valid in B
            // 2. for sequences es that are valid, for elements e in es b's default binding subsumes r
            val normalized = mdlParticle.normalizeTerm()
            val normalizedBase = base.mdlParticle.normalizeTerm()
            if(normalizedBase.mdlMinOccurs > normalized.mdlMinOccurs) return false
            if(normalizedBase.mdlMaxOccurs < normalized.mdlMaxOccurs) return false
            if(! normalized.mdlTerm.restricts(normalizedBase.mdlTerm)) return false
            return true
        }

        override fun check() {
            fun collectElements(
                term: ResolvedTerm,
                target: MutableList<ResolvedElement> = mutableListOf()
            ): List<ResolvedElement> {
                when (term) {
                    is ResolvedElement -> target.add(term)
                    is ResolvedGroupLikeTerm -> for (p in term.mdlParticles) {
                        collectElements(p.mdlTerm, target)
                    }
                }
                return target
            }

            val elements = mutableMapOf<QName, ResolvedType>()
            for (particle in collectElements(mdlParticle.mdlTerm)) {
                val qName = particle.mdlQName
                val old = elements.put(qName, particle.mdlTypeDefinition)
                if (old != null) require(particle.mdlTypeDefinition == old) {
                    "If an element is repeated in a group it must have identical types"
                }
            }

        }
    }

    class MixedContentType(
        override val mdlParticle: ResolvedParticle<ResolvedGroupLikeTerm>,
        override val mdlOpenContent: ResolvedOpenContent? = null
    ) : ComplexTypeModel.ContentType.Mixed, ResolvedElementBase {
        override val openContent: OpenContentModel? get() = null
    }

    class ElementOnlyContentType(
        override val mdlParticle: ResolvedParticle<ResolvedGroupLikeTerm>,
        override val mdlOpenContent: ResolvedOpenContent? = null
    ) : ComplexTypeModel.ContentType.ElementOnly, ResolvedElementBase {
        override val openContent: OpenContentModel? get() = null
    }

    interface ResolvedSimpleContentType : ResolvedContentType, ComplexTypeModel.SimpleContent,
        ComplexTypeModel.ContentType.Simple {
        override val mdlAttributeWildcard: ResolvedAny?

        override val mdlContentType: ResolvedSimpleContentType

        override val mdlSimpleTypeDefinition: ResolvedSimpleType

        override val mdlBaseTypeDefinition: ResolvedType

    }

    companion object {
        internal fun contentType(
            effectiveMixed: Boolean,
            particle: ResolvedParticle<ResolvedGroupLikeTerm>?
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
                    check(interSection.isEmpty()) { "Duplicate attributes ($interSection) in attribute group" }
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
