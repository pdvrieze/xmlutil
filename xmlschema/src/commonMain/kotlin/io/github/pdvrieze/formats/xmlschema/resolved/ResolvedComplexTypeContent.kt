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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.types.*
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.util.CompactFragment

sealed class ResolvedComplexTypeContent(
    override val schema: ResolvedSchemaLike
) : T_ComplexType.Content, ResolvedPart {
    abstract fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>)

    override abstract val rawPart: XSI_ComplexContent
}

class ResolvedComplexContent(
    parent: ResolvedComplexType,
    override val rawPart: XSComplexContent,
    schema: ResolvedSchemaLike
) : ResolvedComplexTypeContent(schema),
    T_ComplexType.ComplexContent {

    override val derivation: ResolvedDerivation by lazy {
        when (val d = rawPart.derivation) {
            is XSComplexContent.XSExtension -> ResolvedComplexExtension(d, parent, schema)
            is XSComplexContent.XSRestriction -> ResolvedComplexRestriction(d, parent, schema)
        }
    }


    override fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>) {
        derivation.check(seenTypes, inheritedTypes)
    }
}

sealed class ResolvedDerivation(scope: ResolvedComplexType, override val schema: ResolvedSchemaLike): T_ComplexDerivation, ResolvedPart {
    override abstract val rawPart : XSComplexContent.XSComplexDerivationBase

    final override val term: ResolvedComplexType.ResolvedDirectParticle<*>? by lazy {
        when (val t = rawPart.term) {
            is XSAll -> ResolvedAll(scope, t, schema)
            is XSChoice -> ResolvedChoice(scope, t, schema)
//            is XSGroupRefParticle -> ResolvedGroupRefParticle(t, schema)
            is XSSequence -> ResolvedSequence(scope, t, schema)
            null -> null
        }
    }

    final override val asserts: List<T_Assertion> get() = rawPart.asserts
    abstract override val attributes: List<ResolvedLocalAttribute>
    abstract override val attributeGroups: List<ResolvedAttributeGroupRef>
    final override val anyAttribute: XSAnyAttribute? get() = rawPart.anyAttribute
    final override val annotation: XSAnnotation? get() = rawPart.annotation
    final override val id: VID? get() = rawPart.id
    final override val otherAttrs: Map<QName, String> get() = rawPart.otherAttrs
    final override val base: QName? get() = rawPart.base
    final override val openContent: XSOpenContent? get() = rawPart.openContent

    val baseType: ResolvedGlobalType by lazy {
        schema.type(base ?: AnyType.qName)
    }

    open fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>) {
        super<ResolvedPart>.check()
        val b = base
        if (b != null && b !in seenTypes) { // Recursion is allowed, but must be managed
            baseType.check(seenTypes, inheritedTypes)
        }

        term?.check()
        attributes.forEach(ResolvedLocalAttribute::check)
        attributeGroups.forEach(ResolvedAttributeGroupRef::check)

    }
}

class ResolvedComplexExtension(
    override val rawPart: XSComplexContent.XSExtension,
    scope: ResolvedComplexType,
    schema: ResolvedSchemaLike
) : ResolvedDerivation(scope, schema), T_ComplexExtensionType {

    override val attributes: List<ResolvedLocalAttribute> =
        DelegateList(rawPart.attributes) { ResolvedLocalAttribute(scope, it, schema)}

    override val attributeGroups: List<ResolvedAttributeGroupRef> =
        DelegateList(rawPart.attributeGroups) { ResolvedAttributeGroupRef(it, schema) }

    override fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>) {
        super<ResolvedDerivation>.check(seenTypes, inheritedTypes)
        require(base !in inheritedTypes.dropLastOrEmpty(1)) { "Recursive type use in complex content: $base" }
    }
}

class ResolvedComplexRestriction(
    override val rawPart: XSComplexContent.XSRestriction,
    scope: ResolvedComplexType,
    schema: ResolvedSchemaLike
) : ResolvedDerivation(scope, schema), T_ComplexRestrictionType {

    override val simpleType: ResolvedLocalSimpleType? by lazy { rawPart.simpleType?.let { ResolvedLocalSimpleType(it, schema, scope) } }

    override val facets: List<XSFacet> get() = rawPart.facets

    override val otherContents: List<CompactFragment>
        get() = rawPart.otherContents

    override val attributes: List<ResolvedLocalAttribute> =
        DelegateList(rawPart.attributes) { ResolvedLocalAttribute(scope, it, schema)}

    override val attributeGroups: List<ResolvedAttributeGroupRef> =
        DelegateList(rawPart.attributeGroups) { ResolvedAttributeGroupRef(it, schema) }

    override fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>) {
        super<ResolvedDerivation>.check(seenTypes, inheritedTypes)
    }
}

class ResolvedComplexShorthandContent(
    scope: ResolvedComplexType,
    override val rawPart: IXSComplexTypeShorthand,
    schema: ResolvedSchemaLike
) : ResolvedComplexTypeContent(schema),
    T_ComplexType.ShorthandContent {
    override val term: ResolvedGroupParticle<*>? by lazy {
        when (val t = rawPart.term) {
            is XSAll -> ResolvedAll(scope, t, schema)
            is XSChoice -> ResolvedChoice(scope, t, schema)
            is XSGroupRef -> ResolvedGroupRef(t, schema)
            is XSSequence -> ResolvedSequence(scope, t, schema)
            null -> null
        }
    }

    override val asserts: List<T_Assertion> get() = rawPart.asserts
    override val attributes: List<ResolvedLocalAttribute> = DelegateList(rawPart.attributes) { ResolvedLocalAttribute(scope, it, schema) }
    override val attributeGroups: List<ResolvedAttributeGroupRef> = DelegateList(rawPart.attributeGroups) { ResolvedAttributeGroupRef(it, schema) }
    override val anyAttribute: XSAnyAttribute? get() = rawPart.anyAttribute
    val openContent: XSOpenContent? get() = rawPart.openContent

    override fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>) {
        super.check()
        term?.check()
        attributes.forEach(ResolvedLocalAttribute::check)
        attributeGroups.forEach(ResolvedAttributeGroupRef::check)
    }
}

