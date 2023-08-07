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
import io.github.pdvrieze.formats.xmlschema.datatypes.impl.SingleLinkedList
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VString
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.resolved.facets.FacetList
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import io.github.pdvrieze.formats.xmlschema.types.VContentMode
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import nl.adaptivity.xmlutil.QName

sealed class ResolvedComplexType(
    val schema: ResolvedSchemaLike
) : ResolvedType,
    VAttributeScope.Member,
    ResolvedLocalElement.Parent,
    VElementScope.Member,
    VTypeScope.Member {

    abstract override val rawPart: XSComplexType

    abstract override val model: Model<*>

    // TODO use better way to determine this
    //name (provided in ResolvedGlobalType) for globals
    override val mdlBaseTypeDefinition: ResolvedType get() = model.mdlBaseTypeDefinition
    override val mdlFinal: Set<VDerivationControl.Complex> get() = model.mdlFinal

    abstract override val mdlScope: VComplexTypeScope

    // context (only for local, not for global)
    // TODO determine this on content type
    val mdlDerivationMethod: VDerivationControl.Complex get() = model.mdlDerivationMethod

    // abstract only in global types
    val mdlAttributeUses: Set<IResolvedAttributeUse> get() = model.mdlAttributeUses
    val mdlAttributeWildcard: ResolvedAny? get() = model.mdlAttributeWildcard
    val mdlContentType: ResolvedContentType get() = model.mdlContentType
    val mdlProhibitedSubstitutions: Set<VDerivationControl.Complex> get() = model.mdlProhibitedSubstitutions

    /** TODO tidy this one up */
    val mdlAssertions: List<XSIAssertCommon> get() = model.mdlAssertions

    override fun validate(representation: VString) {
        when (val ct = mdlContentType) {
            is ResolvedSimpleContentType -> ct.mdlSimpleTypeDefinition.let { st ->
                st.mdlFacets.validate(
                    st.mdlPrimitiveTypeDefinition,
                    representation
                ); st.validate(representation)
            }

            is MixedContentType -> {
                check(ct.mdlParticle.mdlIsEmptiable()) { "Defaults ($representation) are only valid for mixed content if the particle is emptiable" }
            }

            else -> error("The value ${representation} is not valid in an element-only complex type")
        }
    }


    /** 3.3.4.2 for complex types */
    override fun isValidSubtitutionFor(other: ResolvedType): Boolean {
        return when (other) {
            is ResolvedComplexType -> isValidlyDerivedFrom(other)
            is ResolvedSimpleType -> isValidlyDerivedFrom(other)
            else -> error("Unreachable")
        }
    }

    /**
     * 3.4.6.5 Type derivation ok (complex)
     */
    override fun isValidlyDerivedFrom(simpleBase: ResolvedSimpleType): Boolean {
        if (this == simpleBase) return true // 2.1
        // check derivation method is not in blocking
        if (this == simpleBase) return true // 2.2
        val btd = mdlBaseTypeDefinition
        if (btd == AnyType) return false // 2.3.1
        return when (btd) {
            is ResolvedComplexType -> btd.isValidlyDerivedFrom(simpleBase)
            is ResolvedSimpleType -> btd.isValidlyDerivedFrom(simpleBase)
            else -> error("Should be unreachable")
        }
    }

    /**
     * 3.4.6.5 Type derivation ok (complex)
     */
    fun isValidlyDerivedFrom(complexBase: ResolvedComplexType): Boolean {
        if (this == complexBase) return true // 2.1
        // check derivation method is not in blocking
        if (this == complexBase) return true // 2.2
        val btd = mdlBaseTypeDefinition
        if (btd == AnyType) return false // 2.3.1
        return when (btd) {
            is ResolvedComplexType -> btd.isValidlyDerivedFrom(complexBase)
            is ResolvedSimpleType -> btd.isValidlyDerivedFrom(complexBase)
            else -> error("Should be unreachable")
        }
    }

    override fun checkType(checkHelper: CheckHelper) {
        checkNotNull(model)
        for (attrUse in mdlAttributeUses) {
            attrUse.checkUse(checkHelper)
        }

        mdlContentType.check(this, checkHelper)

        if (mdlDerivationMethod == VDerivationControl.EXTENSION) {
            val baseType = mdlBaseTypeDefinition
            if (baseType is ResolvedComplexType) {
                require(VDerivationControl.EXTENSION !in baseType.mdlFinal) { "Type ${(baseType as ResolvedGlobalComplexType).mdlQName} is final for extension" }
                require(mdlAttributeUses.containsAll(baseType.mdlAttributeUses)) { "Base attribute uses must be a subset of the extension: extension: ${mdlAttributeUses} - base: ${baseType.mdlAttributeUses}" }
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
                        require(bot == null || eot?.mdlMode == ResolvedOpenContent.Mode.INTERLEAVE || (bot.mdlMode == ResolvedOpenContent.Mode.SUFFIX && eot?.mdlMode == ResolvedOpenContent.Mode.SUFFIX))
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

            require(VDerivationControl.RESTRICTION !in b.mdlFinal) { "Type ${(b as ResolvedGlobalComplexType).mdlQName} is final for restriction" }

            val ct: ResolvedContentType = mdlContentType
//            check (b is ResolvedComplexType) { "Restriction must be based on a complex type" }
            val bt = (b as? ResolvedComplexType)?.mdlContentType
            when {
                b == AnyType -> {} // Derivation is fine

                ct is ResolvedSimpleContentType -> {
                    when (bt) {
                        is ResolvedSimpleContentType -> {
                            val sb = bt.mdlSimpleTypeDefinition
                            val st = ct.mdlSimpleTypeDefinition
                            check(st.isValidlyDerivedFrom(sb)) { "For derivation, simple content models must validly derive" }
                        }

                        is MixedContentType ->
                            check(bt.mdlParticle.mdlIsEmptiable()) { "Simple variety can only restrict emptiable mixed content type" }

                        else -> throw IllegalArgumentException("Invalid derivation of ${bt?.mdlVariety} by simple")
                    }
                }

                ct is EmptyContentType -> {
                    when (bt) {
                        is ElementContentType -> {
                            check(bt.mdlParticle.mdlIsEmptiable())
                        }

                        !is EmptyContentType -> error("Invalid derivation of ${bt?.mdlVariety} by empty")
                    }
                }

                ct is ElementOnlyContentType -> {
                    check(bt is ElementContentType) { "ElementOnly content type can only derive elementOnly or mixed" }
                    check(ct.restricts(bt) || true) // TODO do check
                }

                ct is MixedContentType -> {
                    check(bt is MixedContentType) { "Mixed content type can only derive from mixed content" }
                    check(ct.restricts(bt) || true) // TODO do check
                }
            }

            // check attributes : 3.4.6.3, item 3
            // check attributes : 3.4.6.3, item 4
            // check attributes : 3.4.6.3, item 5 assertions is an extension of b.extensions
        }
    }

    fun collectConstraints(collector: MutableCollection<ResolvedIdentityConstraint>) {
        // a content type that can contain elements (not that it is an element only, this can be a group).
        (mdlContentType as? ElementContentType)?.run { mdlParticle.mdlTerm.collectConstraints(collector) }
    }


    sealed interface ResolvedDirectParticle<out T : ResolvedTerm>

    interface Model<R : XSIComplexType>: ResolvedAnnotated.IModel {
        fun calculateProhibitedSubstitutions(rawPart: R, schema: ResolvedSchemaLike): Set<VDerivationControl.Complex>

        val mdlAssertions: List<XSIAssertCommon>
        val mdlBaseTypeDefinition: ResolvedType
        val mdlFinal: Set<VDerivationControl.Complex>
        val mdlContentType: ResolvedContentType
        val mdlDerivationMethod: VDerivationControl.Complex
        val mdlAttributeWildcard: ResolvedAny?
        val mdlAbstract: Boolean
        val mdlProhibitedSubstitutions: Set<VDerivationControl.Complex>
        val mdlAttributeUses: Set<IResolvedAttributeUse>
        val mdlAnnotations: ResolvedAnnotation?
    }

    protected abstract class ModelBase<R : XSIComplexType>(
        context: ResolvedComplexType,
        rawPart: R,
        schema: ResolvedSchemaLike
    ) : ResolvedAnnotated.Model(rawPart), Model<R> {
        final override val mdlAnnotations: ResolvedAnnotation? = rawPart.annotation.models()

        final override val mdlAttributeUses: Set<IResolvedAttributeUse> by lazy {
            calculateAttributeUses(
                schema,
                rawPart,
                context
            )
        }

        override val mdlAttributeWildcard: ResolvedAny? // TODO do more
            get() = null

        override val mdlAssertions: List<XSIAssertCommon> = buildList {
            addAll(rawPart.content.derivation.asserts)
            addAll(rawPart.content.derivation.asserts)
        }


    }

    protected abstract class ComplexModelBase<R : XSComplexType.ComplexBase>(
        parent: ResolvedComplexType,
        rawPart: R,
        schema: ResolvedSchemaLike,
    ) : ModelBase<R>(parent, rawPart, schema) {

        final override val mdlContentType: ResolvedContentType

        final override val mdlBaseTypeDefinition: ResolvedType

        final override val mdlDerivationMethod: VDerivationControl.Complex

        init {
            val baseTypeDefinition: ResolvedType
            val content: XSI_ComplexContent = rawPart.content
            val derivation: XSI_ComplexDerivation

            when (content) {
                is XSComplexContent -> {
                    derivation = content.derivation
                    val base = requireNotNull(derivation.base) { "Missing base attribute for complex type derivation" }
                    if ((parent as? ResolvedGlobalType)?.mdlQName == base) {
                        require(schema is CollatedSchema.RedefineWrapper) { "Self-reference of type names can only happen in redefine" }

                        baseTypeDefinition = schema.nestedComplexType(base)
                    } else {

                        val seenTypes = mutableSetOf<QName>()
                        seenTypes.add(base)
                        val baseType = schema.type(base)

/*
                        var b: ResolvedGlobalComplexType? = baseType as? ResolvedGlobalComplexType
                        while (b != null) {
                            val lastB = b
                            val b2 =
                                (lastB.rawPart.simpleContent.derivation as? XSComplexContent.XSComplexDerivationBase)?.base
                            b = b2?.let { b2Name ->
                                if (lastB.mdlQName == b2Name && lastB.schema is CollatedSchema.RedefineWrapper) {

                                    lastB.schema.nestedComplexType(b2Name)
                                } else {
                                    require(seenTypes.add(b2)) { "Recursive type use in complex content: ${seenTypes.joinToString()}" }
                                    schema.type(b2) as? ResolvedGlobalComplexType
                                }
                            }
                        }
*/

                        // Do this after recursion test (otherwise it causes a stack overflow)

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
                is XSComplexContent.XSExtension -> VDerivationControl.EXTENSION
                else -> VDerivationControl.RESTRICTION
            }


            val effectiveMixed =
                (content as? XSComplexContent)?.mixed?.also { require(rawPart.mixed == null || rawPart.mixed == it) }
                    ?: rawPart.mixed ?: false
            val term = derivation.term

            val explicitContent: ResolvedGroupParticle<ResolvedModelGroup>? = when {
                (term == null) ||
                        (term.maxOccurs == VAllNNI(0)) ||
                        ((term is XSAll && !term.hasChildren()) || (term is XSSequence && !term.hasChildren())) ||
                        (term is XSChoice && term.minOccurs?.toUInt() == 0u && !term.hasChildren()) -> null


                else -> ResolvedGroupParticle.invoke(parent, term, schema)
            }

            val effectiveContent: ResolvedParticle<ResolvedModelGroup>? = explicitContent ?: when {
                !effectiveMixed -> null
                else -> ResolvedSequence(
                    parent,
                    XSSequence(minOccurs = VNonNegativeInteger(1), maxOccurs = VAllNNI(1)),
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
                    it == Variety.SIMPLE || it == Variety.EMPTY
                } -> contentType(effectiveMixed, effectiveContent)

                effectiveContent == null -> baseTypeDefinition.mdlContentType
                else -> {
                    val baseParticle = (baseTypeDefinition.mdlContentType as ElementContentType).mdlParticle
                    val baseTerm: ResolvedTerm = baseParticle.mdlTerm
                    val effectiveContentTerm = effectiveContent.mdlTerm
                    val part: ResolvedParticle<ResolvedModelGroup> = when {
                        baseParticle is ResolvedAll && explicitContent == null -> baseParticle
                        (baseTerm is ResolvedAll && effectiveContentTerm is ResolvedAll) -> {
                            val p = baseTerm.mdlParticles + effectiveContentTerm.mdlParticles
                            SyntheticAll(
                                mdlMinOccurs = effectiveContent.mdlMinOccurs,
                                mdlMaxOccurs = VAllNNI(1),
                                mdlParticles = p
                            )
                        }

                        else -> {
                            require(baseParticle.mdlTerm !is ResolvedAll) { "All can not be part of a sequence" }

                            val p: List<ResolvedParticle<ResolvedTerm>> =
                                (listOf(baseParticle) + listOfNotNull(effectiveContent))

                            SyntheticSequence(
                                mdlMinOccurs = VNonNegativeInteger(1),
                                mdlMaxOccurs = VAllNNI(1),
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

            val wildcardElement: XSOpenContentBase? =
                (rawPart as? XSComplexType.Shorthand)?.openContent
                    ?: (schema as? ResolvedSchema)?.defaultOpenContent?.takeIf {
                        explicitContentType.mdlVariety != Variety.EMPTY || it.appliesToEmpty
                    }

            if (wildcardElement == null || wildcardElement.mode == VContentMode.NONE) {
                mdlContentType = explicitContentType
            } else {
                val particle: ResolvedParticle<ResolvedModelGroup> =
                    (explicitContentType as? ElementContentType)?.mdlParticle
                        ?: SyntheticSequence(
                            mdlMinOccurs = VNonNegativeInteger.ONE,
                            mdlMaxOccurs = VAllNNI.ONE,
                            mdlParticles = emptyList(),
                            schema = schema
                        )

                // TODO Add wildcard union
                val w = wildcardElement.any ?: XSAny()
                val openContent = ResolvedOpenContent(
                    XSOpenContent(
                        mode = wildcardElement.mode,
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

    protected abstract class SimpleModelBase<R : XSComplexType.Simple>(
        context: ResolvedComplexType,
        rawPart: R,
        schema: ResolvedSchemaLike,
    ) : ModelBase<R>(context, rawPart, schema),
        ResolvedSimpleContentType {

        final override val mdlBaseTypeDefinition: ResolvedType =
            rawPart.content.derivation.base?.let { schema.type(it) } ?: AnyType

        override val mdlDerivationMethod: VDerivationControl.Complex =
            rawPart.content.derivation.derivationMethod

        override val mdlContentType: ResolvedSimpleContentType get() = this

        // mdlVariety is inherited from ResolvedSimpleContentType

        final override val mdlSimpleTypeDefinition: ResolvedSimpleType

        init {

            val derivation = rawPart.content.derivation

            val baseType: ResolvedType = mdlBaseTypeDefinition

            when (derivation) {
                is XSSimpleContentExtension -> require(VDerivationControl.EXTENSION !in mdlBaseTypeDefinition.mdlFinal) {
                    "${derivation.base} is final for extension"
                }

                is XSSimpleContentRestriction -> require(VDerivationControl.RESTRICTION !in baseType.mdlFinal) {
                    "${derivation.base} is final for extension"
                }
            }


            val complexBaseContentType: ResolvedContentType? = (baseType as? ResolvedComplexType)?.mdlContentType

            when {
                // 3.4.2.2 (mapping complex type with simple content)
                complexBaseContentType is ResolvedSimpleContentType &&
                        derivation is XSSimpleContentRestriction -> { // 1
                    val b: ResolvedSimpleType =
                        derivation.simpleType?.let { ResolvedLocalSimpleType(it, schema, context) } //1.1
                            ?: complexBaseContentType.mdlSimpleTypeDefinition // 1.2

                    mdlSimpleTypeDefinition = SyntheticSimpleType(
                        context,
                        b,
                        b.mdlFacets.overlay(FacetList(derivation.facets, schema, b.mdlPrimitiveTypeDefinition)),
                        b.mdlFundamentalFacets, // TODO may need further specialisation
                        b.mdlVariety.notNil(),
                        b.mdlPrimitiveTypeDefinition,
                        b.mdlItemTypeDefinition,
                        b.mdlMemberTypeDefinitions
                    )
                }

                complexBaseContentType is MixedContentType &&
                        derivation is XSSimpleContentRestriction &&
                        complexBaseContentType.mdlParticle.mdlIsEmptiable() -> { // 2

                    // while the simpleType would be anySimpleType this would violate the constraints
                    val sb =
                        derivation.simpleType
                            ?: error("Simple type definition constrained violated: 3.4.2.2 - step 2 (NOTE)")

                    mdlSimpleTypeDefinition = ResolvedLocalSimpleType( // simply add facets
                        XSLocalSimpleType(XSSimpleRestriction(sb, derivation.facets)),
                        schema,
                        context
                    )
                }

                complexBaseContentType is ResolvedSimpleContentType &&
                        derivation is XSSimpleContentExtension -> // 3
                    mdlSimpleTypeDefinition = requireNotNull(complexBaseContentType.mdlSimpleTypeDefinition)

                baseType is ResolvedSimpleType && //4
                        derivation is XSSimpleContentExtension -> mdlSimpleTypeDefinition = baseType

                else -> mdlSimpleTypeDefinition = AnySimpleType
            }

        }

        override fun check(
            complexType: ResolvedComplexType,
            checkHelper: CheckHelper
        ) {
            checkHelper.checkType(mdlBaseTypeDefinition)
        }
    }

    interface ResolvedContentType : VContentType {
        fun check(
            complexType: ResolvedComplexType,
            checkHelper: CheckHelper
        )
    }

    object EmptyContentType : VContentType.Empty, ResolvedContentType {
        override fun check(
            complexType: ResolvedComplexType,
            checkHelper: CheckHelper
        ) {
        }
    }

    interface ElementContentType : ResolvedContentType, VContentType.ElementBase {
        override val mdlParticle: ResolvedParticle<ResolvedModelGroup>
        val mdlOpenContent: ResolvedOpenContent?

        /** Implementation of 3.4.6.4 */
        fun restricts(base: ElementContentType): Boolean {
            // 1. every sequence of elements valid in this is also (locally -3.4.4.2) valid in B
            // 2. for sequences es that are valid, for elements e in es b's default binding subsumes r
            val normalized = mdlParticle.normalizeTerm()
            val normalizedBase = base.mdlParticle.normalizeTerm()
            if (normalizedBase.mdlMinOccurs > normalized.mdlMinOccurs) return false
            if (normalizedBase.mdlMaxOccurs < normalized.mdlMaxOccurs) return false
            if (!normalized.mdlTerm.restricts(normalizedBase.mdlTerm)) return false
            return true
        }

        override fun check(
            complexType: ResolvedComplexType,
            checkHelper: CheckHelper
        ) {
            fun collectElements(
                term: ResolvedTerm,
                target: MutableList<ResolvedElement> = mutableListOf()
            ): List<ResolvedElement> {
                when (term) {
                    is ResolvedElement -> target.add(term)
                    is ResolvedModelGroup -> for (p in term.mdlParticles) {
                        collectElements(p.mdlTerm, target)
                    }
                }
                return target
            }

            val elements = mutableMapOf<QName, ResolvedType>()
            mdlParticle.checkParticle(checkHelper)
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
        override val mdlParticle: ResolvedParticle<ResolvedModelGroup>,
        override val mdlOpenContent: ResolvedOpenContent? = null
    ) : VContentType.Mixed, ElementContentType {
        override val openContent: ResolvedOpenContent? get() = null
    }

    class ElementOnlyContentType(
        override val mdlParticle: ResolvedParticle<ResolvedModelGroup>,
        override val mdlOpenContent: ResolvedOpenContent? = null
    ) : VContentType.ElementOnly, ElementContentType {
        override val openContent: ResolvedOpenContent? get() = null
    }

    interface ResolvedSimpleContentType : ResolvedContentType,
        VSimpleTypeScope.Member,
        VContentType.Simple {
        val mdlAttributeWildcard: ResolvedAny?

        val mdlContentType: ResolvedSimpleContentType

        override val mdlSimpleTypeDefinition: ResolvedSimpleType

        val mdlBaseTypeDefinition: ResolvedType
        val mdlContext: VComplexTypeScope.Member
        val mdlAbstract: Boolean
        val mdlProhibitedSubstitutions: Set<VDerivationControl.Complex>
        val mdlFinal: Set<VDerivationControl.Complex>
        val mdlAttributeUses: Set<IResolvedAttributeUse>
        val mdlDerivationMethod: VDerivationControl.Complex

    }

    companion object {
        internal fun contentType(
            effectiveMixed: Boolean,
            particle: ResolvedParticle<ResolvedModelGroup>?
        ): ResolvedContentType {
            return when {
                particle == null -> EmptyContentType
                effectiveMixed -> MixedContentType(particle)
                else -> ElementOnlyContentType(particle)
            }
        }

        /**
         * This one isn't quire correct/ready
         */
        fun calculateAttributeUses(
            schema: ResolvedSchemaLike,
            rawPart: XSIComplexType,
            parent: ResolvedComplexType
        ): Set<IResolvedAttributeUse> {
            val defaultAttributeGroup = (schema as? ResolvedSchema)?.defaultAttributes
                ?.takeIf { rawPart.defaultAttributesApply != false }

            val prohibitedAttrs = mutableSetOf<QName>()

            val attributes = buildMap<QName, IResolvedAttributeUse> {
                // Defined attributes
                for (attr in rawPart.content.derivation.attributes) {
                    val resolvedAttribute = ResolvedLocalAttribute(parent, attr, schema)
                    when (resolvedAttribute) {
                        is ResolvedProhibitedAttribute -> prohibitedAttrs.add(resolvedAttribute.mdlQName)
                        else ->
                            put(resolvedAttribute.mdlAttributeDeclaration.mdlQName, resolvedAttribute)?.also {
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
                    // TODO follow the standard
                    val groupAttributeUses = group.getAttributeUses()
                    val interSection = groupAttributeUses.intersect(this.keys)
                    check(interSection.isEmpty()) { "Duplicate attributes ($interSection) in attribute group" }
                    groupAttributeUses.associateByTo(this) { it.mdlAttributeDeclaration.mdlQName }
                }

                // Extension/restriction. Only restriction can prohibit attributes.
                val t = parent.mdlBaseTypeDefinition as? ResolvedComplexType
                when (t?.mdlDerivationMethod) {
                    VDerivationControl.EXTENSION ->
                        for (a in t.mdlAttributeUses) {
                            val existingAttr = get(a.mdlAttributeDeclaration.mdlQName)
                            if (existingAttr!=null) {
                                require(existingAttr is ResolvedProhibitedAttribute || existingAttr.mdlAttributeDeclaration.mdlTypeDefinition == a.mdlAttributeDeclaration.mdlTypeDefinition) {
                                    "Invalid attribute extension $a of $existingAttr"
                                }
                            }
                        }

                    VDerivationControl.RESTRICTION ->
                        for (a in t.mdlAttributeUses) {
                            val qName = a.mdlAttributeDeclaration.mdlQName
                            if (qName !in prohibitedAttrs) {
                                getOrPut(qName) { a }
                            }
                        }

                    null -> Unit
                }

            }
            val attributeUses = attributes.values.toSet()
            return attributeUses
        }

    }

    enum class Variety { EMPTY, SIMPLE, ELEMENT_ONLY, MIXED }

}
