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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VString
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.IDType
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSchema.Companion.STRICT_ALL_IN_EXTENSION
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.resolved.facets.FacetList
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import io.github.pdvrieze.formats.xmlschema.types.VContentMode
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import io.github.pdvrieze.formats.xmlschema.types.VFormChoice
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.namespaceURI

sealed class ResolvedComplexType(
    val schema: ResolvedSchemaLike
) : ResolvedType,
    VAttributeScope.Member,
    ResolvedLocalElement.Parent,
    VElementScope.Member,
    VTypeScope.Member {

    abstract override val model: Model

    // TODO use better way to determine this
    //name (provided in ResolvedGlobalType) for globals
    override val mdlBaseTypeDefinition: ResolvedType get() = model.mdlBaseTypeDefinition
    override val mdlFinal: Set<VDerivationControl.Complex> get() = model.mdlFinal

    abstract override val mdlScope: VComplexTypeScope

    // context (only for local, not for global)
    // TODO determine this on content type
    val mdlDerivationMethod: VDerivationControl.Complex get() = model.mdlDerivationMethod

    // abstract only in global types
    // TODO transform to map[QName,IResolvedAttributeUse]
    val mdlAttributeUses: Map<QName, IResolvedAttributeUse> get() = model.mdlAttributeUses
    val mdlAttributeWildcard: ResolvedAnyAttribute? get() = model.mdlAttributeWildcard
    val mdlContentType: ResolvedContentType get() = model.mdlContentType
    val mdlProhibitedSubstitutions: Set<VDerivationControl.Complex> get() = model.mdlProhibitedSubstitutions

    /** TODO tidy this one up */
    val mdlAssertions: List<XSIAssertCommon> get() = model.mdlAssertions

    override fun hasLocalNsInContext(): Boolean {
        return model.hasLocalNsInContext
    }

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
    override fun isValidSubtitutionFor(other: ResolvedType, asRestriction: Boolean): Boolean {
        return isValidlyDerivedFrom(other, asRestriction)
    }

    /**
     * 3.4.6.5 Type derivation ok (complex)
     */
    override fun isValidlyDerivedFrom(base: ResolvedType, asRestriction: Boolean): Boolean {
        if (this == base) return true // 2.1

        if (asRestriction && mdlDerivationMethod != VDerivationControl.RESTRICTION) return false
        if (base.mdlFinal.contains(mdlDerivationMethod)) return false

        val btd = mdlBaseTypeDefinition
        // check derivation method is not in blocking
        if (btd == base) return true
        if (btd == AnyType) return false // 2.3.1
        return when (btd) {
            is ResolvedComplexType -> btd.isValidlyDerivedFrom(base, asRestriction)
            is ResolvedSimpleType -> btd.isValidlyDerivedFrom(base, asRestriction)
            else -> error("Should be unreachable")
        }
    }

    override fun checkType(checkHelper: CheckHelper) {
        checkNotNull(model)
        for (attrUse in mdlAttributeUses.values) {
            attrUse.checkUse(checkHelper)
        }

        mdlContentType.check(this, checkHelper)

        if (mdlDerivationMethod == VDerivationControl.EXTENSION) {

            when (mdlBaseTypeDefinition) {
                is ResolvedComplexType -> checkExtensionOfComplex()

                else -> {} // extension of simple type
            }
        } else { // restriction
            checkRestriction()
        }
    }

    private fun checkRestriction() {
        val b = mdlBaseTypeDefinition

        require(VDerivationControl.RESTRICTION !in b.mdlFinal) { "Type ${(b as ResolvedGlobalComplexType).mdlQName} is final for restriction" }

        val contentType: ResolvedContentType = mdlContentType
        //            check (b is ResolvedComplexType) { "Restriction must be based on a complex type" }
        val baseContentType = (b as? ResolvedComplexType)?.mdlContentType
        when {
            b == AnyType -> {} // Derivation is fine

            contentType is ResolvedSimpleContentType -> {
                when (baseContentType) {
                    is ResolvedSimpleContentType -> {
                        val sb = baseContentType.mdlSimpleTypeDefinition
                        val st = contentType.mdlSimpleTypeDefinition
                        check(
                            st.isValidlyDerivedFrom(
                                sb,
                                true
                            )
                        ) { "For derivation, simple content models must validly derive" }
                    }

                    is MixedContentType ->
                        check(baseContentType.mdlParticle.mdlIsEmptiable()) { "Simple variety can only restrict emptiable mixed content type" }

                    else -> throw IllegalArgumentException("Invalid derivation of ${baseContentType?.mdlVariety} by simple")
                }
            }

            contentType is EmptyContentType -> {
                when (baseContentType) {
                    is ElementContentType -> {
                        check(baseContentType.mdlParticle.mdlIsEmptiable())
                    }

                    !is EmptyContentType -> error("Invalid derivation of ${baseContentType?.mdlVariety} by empty")
                }
            }

            contentType is ElementOnlyContentType -> {
                check(baseContentType is ElementContentType) { "ElementOnly content type can only derive elementOnly or mixed" }
                check(contentType.restricts(baseContentType, this, schema)) {
                    "Overriding element ${contentType.flattened} does not restrict base ${baseContentType.flattened}"
                }
            }

            contentType is MixedContentType -> {
                check(baseContentType is MixedContentType) { "Mixed content type can only derive from mixed content" }
                check(contentType.restricts(baseContentType, this, schema) || true) // TODO do check
            }
        }

        val dAttrs = mdlAttributeUses
        if (dAttrs.isNotEmpty()) {
            require(b is ResolvedComplexType) { "Restriction introduces attributes on a simple type" }
            val bAttrs = b.mdlAttributeUses
            for ((dName, dAttr) in dAttrs) {

                when (val bAttr = bAttrs[dName]) {
                    null -> {
                        val attrWildcard =
                            requireNotNull(b.mdlAttributeWildcard) { "No matching attribute or wildcard found for $dName" }
                        require(
                            attrWildcard.matches(dName, this, schema)
                        ) { "Attribute wildcard does not match $dName" }
                    }

                    else -> require(dAttr.isValidRestrictionOf(bAttr)) {
                        "3.4.6.3 - ${dAttr} doesn't restrict base attribute validly"
                    }
                }

            }
        }
        mdlAttributeWildcard?.let { wc ->
            require(b is ResolvedComplexType) { "Restriction with wilcard attributes must derive complex types" }
            val baseWC = requireNotNull(b.mdlAttributeWildcard) { "A wildcard must derive from a wildcard" }
            require(wc.restricts(baseWC)) { "Wildcard $wc does not restrict $baseWC" }

        }


        // check attributes : 3.4.6.3, item 3
        // check attributes : 3.4.6.3, item 4
        // check attributes : 3.4.6.3, item 5 assertions is an extension of b.extensions
    }

    private fun checkExtensionOfComplex() {
        val baseType = mdlBaseTypeDefinition as ResolvedComplexType
        require(VDerivationControl.EXTENSION !in baseType.mdlFinal) { "3.4.6.2(1.1) - Type ${(baseType as ResolvedGlobalComplexType).mdlQName} is final for extension" }
        for ((baseName, baseUse) in baseType.mdlAttributeUses) {
            val derived =
                requireNotNull(mdlAttributeUses[baseName]) { "3.4.6.2(1.2) - Base attribute uses must be a subset of the extension: extension: $baseName not found in $mdlAttributeUses" }
            if (baseUse.mdlRequired) {
                require(derived.mdlRequired) { "If the base attribute is required the child should also be" }
            }
            if (derived !is ResolvedProhibitedAttribute && baseUse !is ResolvedProhibitedAttribute) {
                require(
                    baseUse.mdlAttributeDeclaration.mdlTypeDefinition.isValidSubtitutionFor(
                        derived.mdlAttributeDeclaration.mdlTypeDefinition,
                        false
                    )
                ) { "Types must match" }
            }
        }

        // 1.3
        val baseWc = baseType.mdlAttributeWildcard
        if (baseWc != null) {
            val wc = requireNotNull(mdlAttributeWildcard) { "3.4.6.2(1.3) - extension must have also one" }
            require(wc.mdlNamespaceConstraint.isSupersetOf(baseWc.mdlNamespaceConstraint)) { "3.4.6.2(1.3) - base wildcard is subset of extension" }
        }

        when (val baseCType = baseType.mdlContentType) {
            is ResolvedSimpleContentType ->
                when (val ct = mdlContentType) {
                    is EmptyContentType -> {
                        require(baseType is ResolvedSimpleType || schema.version == ResolvedSchema.Version.V1_0) {
                            "From version 1.1 complexContent can not inherit simpleContent"
                        }
                        require(baseCType.mdlSimpleTypeDefinition.value(VString("")) != null) {
                            "The empty string must be a valid value"
                        }
                        // fine for now
                    }

                    is ResolvedSimpleContentType -> {
                        require(baseCType.mdlSimpleTypeDefinition == ct.mdlSimpleTypeDefinition) {
                            "3.4.6.2(1.4.1) - Simple content types must have the same simple type definition"
                        }

                    }

                    else -> {
                        throw IllegalArgumentException("simple base can only extend from simple content or empty")
                    }
                }

            is EmptyContentType -> {}//1.4.2 / 1.4.3.2.1 can extend from empty

            is MixedContentType -> {
                require(mdlContentType is MixedContentType) { "3.4.6.2(1.4.3.2.2.1) - mixed must be extended by mixed" }
                // Ensure chcking particle extensions
                val bot = baseCType.mdlOpenContent
                val eot = (mdlContentType as MixedContentType).mdlOpenContent
                require(bot == null || eot?.mdlMode == ResolvedOpenContent.Mode.INTERLEAVE || (bot.mdlMode == ResolvedOpenContent.Mode.SUFFIX && eot?.mdlMode == ResolvedOpenContent.Mode.SUFFIX)) {
                    "3.4.6.2(1.4.3.2.2.3) - open content not compatible"
                }
                if (bot != null && eot != null) {
                    require(eot.mdlWildCard!!.mdlNamespaceConstraint.isSupersetOf(bot.mdlWildCard!!.mdlNamespaceConstraint))
                }
            }

            is ElementOnlyContentType -> {
                require(mdlContentType is ElementOnlyContentType) { "Content type for complex extension must match: ${mdlContentType.mdlVariety}!= ${baseCType.mdlVariety}" }
                // Ensure chcking particle extensions

            }
        }
    }

    fun collectConstraints(collector: MutableCollection<ResolvedIdentityConstraint>) {
        // a content type that can contain elements (not that it is an element only, this can be a group).
        (mdlContentType as? ElementContentType)?.run { mdlParticle.mdlTerm.collectConstraints(collector) }
    }


    sealed interface ResolvedDirectParticle<out T : ResolvedTerm>

    interface Model : ResolvedAnnotated.IModel {
        fun calculateProhibitedSubstitutions(
            rawPart: XSIComplexType,
            schema: ResolvedSchemaLike
        ): Set<VDerivationControl.Complex>

        val mdlAssertions: List<XSIAssertCommon>
        val mdlBaseTypeDefinition: ResolvedType
        val mdlFinal: Set<VDerivationControl.Complex>
        val mdlContentType: ResolvedContentType
        val mdlDerivationMethod: VDerivationControl.Complex
        val mdlAttributeWildcard: ResolvedAnyAttribute?
        val mdlAbstract: Boolean
        val mdlProhibitedSubstitutions: Set<VDerivationControl.Complex>
        val mdlAttributeUses: Map<QName, IResolvedAttributeUse>
        val hasLocalNsInContext: Boolean
    }

    protected abstract class ModelBase<R : XSIComplexType> internal constructor(
        context: ResolvedComplexType,
        elem: SchemaElement<R>,
        schema: ResolvedSchemaLike
    ) : ResolvedAnnotated.Model(elem.elem), Model {

        final override val mdlAttributeUses: Map<QName, IResolvedAttributeUse> by lazy {
            calculateAttributeUses(schema, elem, context)
        }

        override val mdlAssertions: List<XSIAssertCommon> = buildList {
            addAll(elem.elem.content.derivation.asserts)
            addAll(elem.elem.content.derivation.asserts)
        }


        override val hasLocalNsInContext: Boolean by lazy {
            (mdlContentType as? ElementContentType)
                ?.run { mdlParticle.mdlTerm.hasLocalNsInContext() } ?: false
        }

    }

    protected abstract class ComplexModelBase<R : XSComplexType.ComplexBase> internal constructor(
        typeContext: ResolvedComplexType,
        elemPart: SchemaElement<R>,
        schema: ResolvedSchemaLike,
    ) : ModelBase<R>(typeContext, elemPart, schema) {

        final override val mdlContentType: ResolvedContentType

        final override val mdlBaseTypeDefinition: ResolvedType

        final override val mdlDerivationMethod: VDerivationControl.Complex

        final override val mdlAttributeWildcard: ResolvedAnyAttribute?


        init {
            val rawPart = elemPart.elem

            val baseTypeDefinition: ResolvedType
            val content: XSI_ComplexContent = rawPart.content
            val derivation: XSI_ComplexDerivation


            when (content) {
                is XSComplexContent -> {
                    derivation = content.derivation
                    val base = requireNotNull(derivation.base) { "Missing base attribute for complex type derivation" }
                    if ((typeContext as? ResolvedGlobalType)?.mdlQName == base) {
                        require(schema is RedefineSchema) { "Self-reference of type names can only happen in redefine" }

                        baseTypeDefinition = schema.nestedComplexType(base)
                    } else {

                        val seenTypes = mutableSetOf<QName>()
                        seenTypes.add(base)
                        val baseType = schema.type(base)

                        baseTypeDefinition = baseType
                    }
                    require(baseTypeDefinition is ResolvedComplexType) { "Complex types with complex content must have a complex base" }
                }

                is XSComplexType.Shorthand -> {
                    derivation = content
                    check(derivation.base == null) { " Shorthand has no base" }
                    baseTypeDefinition = AnyType
                }

                else -> error("Should be unreachable (sealed type)")
            }


            // attribute wildcards according to 3.4.2.5
            /* -- It is not clear what this does (attribute groups are outside the wildcard)
                        val defaultAttrGroupRef = when {
                            rawPart.defaultAttributesApply!=false -> schema.defaultAttributes
                            else -> null
                        }
            */

            val completeWildcard = content.derivation.anyAttribute?.let {
                ResolvedAnyAttribute(
                    it,
                    schema,
                    hasUnqualifiedAttrs = content.derivation.attributes.any {
                        when (it.form) {
                            VFormChoice.UNQUALIFIED -> true
                            VFormChoice.QUALIFIED -> false
                            null -> it.targetNamespace == null && schema.attributeFormDefault == VFormChoice.UNQUALIFIED
                        }
                    } || (baseTypeDefinition is ResolvedComplexType && baseTypeDefinition.mdlAttributeUses.keys.any { it.namespaceURI.isEmpty() })
                )
            }

            mdlAttributeWildcard = when {
                // 2.1
                derivation !is XSComplexContent.XSExtension -> completeWildcard
                else -> { // extension
                    // 2.2.1
                    val baseWildcard = (baseTypeDefinition as? ResolvedComplexType)?.mdlAttributeWildcard

                    when {
                        baseWildcard == null -> completeWildcard
                        completeWildcard == null -> baseWildcard
                        else -> ResolvedAnyAttribute(
                            mdlNamespaceConstraint = baseWildcard.mdlNamespaceConstraint.union(
                                completeWildcard.mdlNamespaceConstraint,
                                typeContext,
                                schema
                            ),
                            mdlProcessContents = completeWildcard.mdlProcessContents
                        )
                    }
                }
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
                        ((term is XSAll && !term.hasChildren()) || (term is XSSequence && !term.hasChildren())) ||
                        (term is XSChoice && term.minOccurs == VNonNegativeInteger.ZERO && !term.hasChildren()) -> null

                term.maxOccurs == VAllNNI.ZERO -> {
                    require(term.minOccurs == VNonNegativeInteger.ZERO) {
                        "Invalid range: ! ${term.minOccurs ?: "1"} <= ${term.maxOccurs ?: "1"}"
                    }
                    null
                }


                else -> ResolvedGroupParticle.invoke(typeContext, elemPart.wrap(term), schema)
            }

            val effectiveContent: ResolvedParticle<ResolvedModelGroup>? = explicitContent ?: when {
                !effectiveMixed -> null
                else -> ResolvedSequence(
                    typeContext,
                    elemPart.wrap(XSSequence(minOccurs = VNonNegativeInteger(1), maxOccurs = VAllNNI(1))),
                    schema
                )
            }

            val openContent: ResolvedOpenContent? = derivation.openContent?.let {
                ResolvedOpenContent(
                    it,
                    schema,
                    effectiveContent?.mdlTerm?.hasLocalNsInContext() ?: false
                )
            }

            val explicitContentType: ResolvedContentType = when {
                derivation is XSComplexContent.XSRestriction ||
                        derivation is XSComplexType.Shorthand -> // restriction (or shorthand)
                    typeContext.contentType(effectiveMixed, effectiveContent, schema, openContent)


                baseTypeDefinition !is ResolvedComplexType -> // simple type 4.2.1
                    typeContext.contentType(effectiveMixed, effectiveContent, schema, openContent)

                baseTypeDefinition.mdlContentType.mdlVariety.let { // simple content 4.2.1
                    it == Variety.SIMPLE || it == Variety.EMPTY
                } -> typeContext.contentType(effectiveMixed, effectiveContent, schema, openContent)

                effectiveContent == null -> baseTypeDefinition.mdlContentType
                else -> { // extension
                    val baseParticle = (baseTypeDefinition.mdlContentType as ElementContentType).mdlParticle
                    if (STRICT_ALL_IN_EXTENSION && baseTypeDefinition != AnyType) {
                        require(baseParticle.mdlTerm is IResolvedAll || term !is XSAll) {
                            "Somehow all in extension is not allowed (its fails various test suite tests - but should be valid on the spec)"
                        }
                    }

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
                            require(baseParticle.mdlTerm !is ResolvedAll) { "3.4.2.3.3(4.2.3.3) - All can not be part of a sequence" }

                            val p: List<ResolvedParticle<ResolvedTerm>> =
                                (listOf(baseParticle) + listOfNotNull(effectiveContent))

                            SyntheticSequence(
                                mdlMinOccurs = VNonNegativeInteger(1),
                                mdlMaxOccurs = VAllNNI(1),
                                mdlParticles = p
                            )
                        }
                    }
                    when {
                        effectiveMixed -> MixedContentType(part, typeContext, schema, openContent)
                        else -> ElementOnlyContentType(part, typeContext, schema, openContent)
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
                            mdlParticles = emptyList()
                        )

                // TODO Add wildcard union
                val w = wildcardElement.any ?: XSAny()
                val openContent = ResolvedOpenContent(
                    XSOpenContent(
                        mode = wildcardElement.mode,
                        any = w
                    ), schema, particle.mdlTerm.hasLocalNsInContext()
                )

                mdlContentType = when {
                    effectiveMixed -> MixedContentType(
                        mdlParticle = particle,
                        typeContext = typeContext,
                        schema = schema,
                        mdlOpenContent = openContent,
                    )

                    else -> ElementOnlyContentType(
                        mdlParticle = particle,
                        typeContext = typeContext,
                        schema = schema,
                        mdlOpenContent = openContent,
                    )
                }
            }
        }
    }

    protected abstract class SimpleModelBase<R : XSComplexType.Simple> internal constructor(
        context: ResolvedComplexType,
        elem: SchemaElement<R>,
        schema: ResolvedSchemaLike,
    ) : ModelBase<R>(context, elem, schema),
        ResolvedSimpleContentType {

        final override val mdlBaseTypeDefinition: ResolvedType =
            elem.elem.content.derivation.base?.let { schema.type(it) } ?: AnyType

        override val mdlDerivationMethod: VDerivationControl.Complex =
            elem.elem.content.derivation.derivationMethod

        override val mdlContentType: ResolvedSimpleContentType get() = this

        // mdlVariety is inherited from ResolvedSimpleContentType

        final override val mdlSimpleTypeDefinition: ResolvedSimpleType

        final override val mdlAttributeWildcard: ResolvedAnyAttribute?

        init {
            val rawPart = elem.elem
            require(rawPart.mixed != true) { "3.4.3(1) - Simple content can not have mixed=true" }

            val derivation = rawPart.content.derivation

            val baseType: ResolvedType = mdlBaseTypeDefinition

            when (derivation) {
                is XSSimpleContentExtension -> require(VDerivationControl.EXTENSION !in mdlBaseTypeDefinition.mdlFinal) {
                    "${derivation.base} is final for extension"
                }

                is XSSimpleContentRestriction -> {
                    require(VDerivationControl.RESTRICTION !in baseType.mdlFinal) {
                        "${derivation.base} is final for extension"
                    }
                }
            }


            val completeWildcard = rawPart.content.derivation.anyAttribute?.let {
                ResolvedAnyAttribute(
                    it,
                    schema,
                    hasUnqualifiedAttrs = rawPart.content.derivation.attributes.any {
                        when (it.form) {
                            VFormChoice.UNQUALIFIED -> true
                            VFormChoice.QUALIFIED -> false
                            null -> it.targetNamespace == null && schema.attributeFormDefault == VFormChoice.UNQUALIFIED
                        }
                    } || (baseType is ResolvedComplexType && baseType.mdlAttributeUses.keys.any { it.namespaceURI.isEmpty() })
                )
            }

            mdlAttributeWildcard = when {
                // 2.1
                derivation !is XSSimpleContentExtension -> completeWildcard
                else -> { // extension
                    // 2.2.1
                    val baseWildcard = (baseType as? ResolvedComplexType)?.mdlAttributeWildcard

                    when {
                        baseWildcard == null -> completeWildcard
                        completeWildcard == null -> baseWildcard
                        else -> ResolvedAnyAttribute(
                            mdlNamespaceConstraint = baseWildcard.mdlNamespaceConstraint.union(
                                completeWildcard.mdlNamespaceConstraint,
                                context,
                                schema
                            ),
                            mdlProcessContents = completeWildcard.mdlProcessContents
                        )
                    }
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
                        b.mdlFacets.overlay(FacetList.safe(derivation.facets, schema, b)),
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

        val flattened: FlattenedParticle

        /** Implementation of 3.4.6.4 */
        fun restricts(
            baseCT: ResolvedContentType,
            typeContext: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): Boolean {
            if (baseCT !is ElementContentType) return false
            // 1. every sequence of elements valid in this is also (locally -3.4.4.2) valid in B
            // 2. for sequences es that are valid, for elements e in es b's default binding subsumes r

            // we use indirect access to term as that will give us groups.
            val flattened = mdlParticle.mdlTerm.flatten(mdlParticle.range, typeContext, schema)
            val flattenedBase = baseCT.mdlParticle.mdlTerm.flatten(baseCT.mdlParticle.range, typeContext, schema)

            return flattened.restricts(flattenedBase, typeContext, schema)
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
                        if (p !is ResolvedProhibitedElement) {
                            collectElements(p.mdlTerm, target)
                        }
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
        typeContext: ResolvedComplexType,
        schema: ResolvedSchemaLike,
        override val mdlOpenContent: ResolvedOpenContent? = null,
    ) : VContentType.Mixed, ElementContentType {
        override val openContent: ResolvedOpenContent? get() = null

        override val flattened: FlattenedParticle = mdlParticle.mdlTerm.flatten(mdlParticle.range, typeContext, schema)
    }

    class ElementOnlyContentType(
        override val mdlParticle: ResolvedParticle<ResolvedModelGroup>,
        typeContext: ResolvedComplexType,
        schema: ResolvedSchemaLike,
        override val mdlOpenContent: ResolvedOpenContent? = null,
    ) : VContentType.ElementOnly, ElementContentType {
        override val openContent: ResolvedOpenContent? get() = null

        override val flattened: FlattenedParticle = mdlParticle.mdlTerm.flatten(mdlParticle.range, typeContext, schema)
    }

    interface ResolvedSimpleContentType : ResolvedContentType,
        VSimpleTypeScope.Member,
        VContentType.Simple {
        val mdlAttributeWildcard: ResolvedAnyAttribute?

        val mdlContentType: ResolvedSimpleContentType

        override val mdlSimpleTypeDefinition: ResolvedSimpleType

        val mdlBaseTypeDefinition: ResolvedType
        val mdlContext: VComplexTypeScope.Member
        val mdlAbstract: Boolean
        val mdlProhibitedSubstitutions: Set<VDerivationControl.Complex>
        val mdlFinal: Set<VDerivationControl.Complex>
        val mdlAttributeUses: Map<QName, IResolvedAttributeUse>
        val mdlDerivationMethod: VDerivationControl.Complex

    }

    internal fun contentType(
        effectiveMixed: Boolean,
        particle: ResolvedParticle<ResolvedModelGroup>?,
        schema: ResolvedSchemaLike,
        openContent: ResolvedOpenContent?
    ): ResolvedContentType {
        return when {
            particle == null -> EmptyContentType
            effectiveMixed -> MixedContentType(particle, this, schema, openContent)
            else -> ElementOnlyContentType(particle, this, schema, openContent)
        }
    }

    companion object {

        /**
         * This one isn't quire correct/ready
         */
        internal fun calculateAttributeUses(
            schema: ResolvedSchemaLike,
            elem: SchemaElement<XSIComplexType>,
            parent: ResolvedComplexType
        ): Map<QName, IResolvedAttributeUse> {
            val rawPart = elem.elem

            val defaultAttributeGroup = (schema as? ResolvedSchema)?.defaultAttributes
                ?.takeIf { rawPart.defaultAttributesApply != false }

            val prohibitedAttrNames = mutableSetOf<QName>()
            val seenAttrNames = mutableSetOf<QName>()

            val attributes = buildMap<QName, IResolvedAttributeUse> {
                // Defined attributes
                for (attr in rawPart.content.derivation.attributes) {
                    val resolvedAttribute = ResolvedLocalAttribute(parent, elem.wrap(attr), schema, schema.attributeFormDefault)
                    require(put(resolvedAttribute.mdlQName, resolvedAttribute) == null) {
                        "Duplicate attribute ${resolvedAttribute.mdlQName} on type $parent"
                    }
                    if (resolvedAttribute is ResolvedProhibitedAttribute) prohibitedAttrNames.add(resolvedAttribute.mdlQName)
                }

                // Defined attribute group references (including the default one)
                val groups = buildSet {
                    defaultAttributeGroup?.let { ag -> add(schema.attributeGroup(ag)) }
                    rawPart.content.derivation.attributeGroups.mapTo(this) { schema.attributeGroup(it.ref) }
                }
                for (group in groups) {
                    val groupAttributeUses = group.getAttributeUses()
                    val interSection = groupAttributeUses.map { it.mdlQName }.intersect(this.keys)
                    check(interSection.isEmpty()) { "Duplicate attributes ($interSection) in attribute group" }
                    for (use in groupAttributeUses) {
                        require(put(use.mdlQName, use) == null) { "Duplicate attribute and group '${use.mdlQName}' for group ${group.mdlQName}" }
                    }
                }

                // Extension/restriction. Only restriction can prohibit attributes.
                val baseType = parent.mdlBaseTypeDefinition as? ResolvedComplexType
                when (baseType?.mdlDerivationMethod) {
                    VDerivationControl.EXTENSION ->
                        for (a in baseType.mdlAttributeUses.values) {
                            val attrName = a.mdlQName
//                            require(a.mdlInheritable) { "Only inheritable attributes can be inherited ($attrName)" }
                            require(attrName !in prohibitedAttrNames) {
                                "Extensions can not prohibit existing attributes"
                            }
                            val existingAttr = get(attrName)
                            if (existingAttr == null) {
                                put(attrName, a)
                            } else if (schema.version == ResolvedSchema.Version.V1_1 && existingAttr is ResolvedLocalAttribute) {
                                throw IllegalArgumentException("Local attributes can not override parent attributes in V1.1")
                            }
                        }

                    VDerivationControl.RESTRICTION ->
                        for (baseAttrUse in baseType.mdlAttributeUses.values) {
                            val qName = baseAttrUse.mdlQName
                            val overriddenAttrUse = get(qName)
                            if (qName in prohibitedAttrNames) {
                                require(!baseAttrUse.mdlRequired) { "Required attribute can not be prohibited in restriction" }
                            } else {
                                if (overriddenAttrUse == null) {
                                    put(qName, baseAttrUse)
                                } else if (schema.version == ResolvedSchema.Version.V1_1 && overriddenAttrUse is ResolvedLocalAttribute) {
                                    val oldC = baseAttrUse.mdlValueConstraint
                                    if (oldC != null) {
                                        val newC = overriddenAttrUse.mdlValueConstraint
                                        when (newC) {
                                            null -> require(oldC !is ValueConstraint.Fixed) { "Fixed attributes cannot be overridden by attributes without fixed value" }
                                            else -> require(
                                                newC.isValidRestrictionOf(overriddenAttrUse.mdlTypeDefinition, oldC)
                                            ) {
                                                "Overridden attributes must be valid restrictions in 1.1"
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    null -> Unit
                }

            }
            val attributeUses = attributes.toMap()

            if (schema.version == ResolvedSchema.Version.V1_0) {
                // this is legal in 1.1
                var idAttrName: QName? = null
                for (use in attributes.values) {
                    if (use !is ResolvedProhibitedAttribute && use.mdlAttributeDeclaration.mdlTypeDefinition == IDType) {
                        require(idAttrName == null) { "Multiple attributes with id type: ${idAttrName} and ${use.mdlAttributeDeclaration.mdlQName}" }
                        require(use.mdlValueConstraint !is ValueConstraint.Fixed) {
                            "Fixed id attributes are not allowed in 1.0"
                        }
                        idAttrName = use.mdlQName
                    }
                }
            }
            return attributeUses
        }

    }

    enum class Variety { EMPTY, SIMPLE, ELEMENT_ONLY, MIXED }

}
