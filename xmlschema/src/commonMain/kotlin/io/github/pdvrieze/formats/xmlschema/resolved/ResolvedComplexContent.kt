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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.*
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.util.CompactFragment

sealed class ResolvedComplexContent(
    override val schema: ResolvedSchemaLike
) : T_ComplexTypeContent, ResolvedPart {
    abstract fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>)

    override abstract val rawPart: T_ComplexTypeContent
}

class ResolvedComplexComplexContent(
    override val rawPart: T_ComplexTypeComplexContent,
    schema: ResolvedSchemaLike
) : ResolvedComplexContent(schema),
    T_ComplexTypeComplexContent {

    override val derivation: ResolvedDerivation by lazy {
        when (val d = rawPart.derivation as T_ComplexDerivationSealedBase) {
            is T_ComplexExtensionType -> ResolvedComplexExtension(d, schema)
            is T_ComplexRestrictionType -> ResolvedComplexRestriction(d, schema)
        }
    }


    override fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>) {
        derivation.check(seenTypes, inheritedTypes)
    }
}

sealed class ResolvedDerivation(override val schema: ResolvedSchemaLike): T_ComplexDerivation, ResolvedPart {
    override abstract val rawPart : T_ComplexDerivationSealedBase

    override val groups: List<T_GroupRef> get() = rawPart.groups

    override val alls: List<T_All> get() = rawPart.alls
    override val choices: List<T_Choice> get() = rawPart.choices
    override val sequences: List<T_Sequence> get() = rawPart.sequences
    override val asserts: List<T_Assertion> get() = rawPart.asserts
    override val attributes: List<T_LocalAttribute> get() = rawPart.attributes
    override val attributeGroups: List<T_AttributeGroupRef> get() = rawPart.attributeGroups
    override val anyAttribute: XSAnyAttribute? get() = rawPart.anyAttribute
    override val annotations: List<XSAnnotation> get() = rawPart.annotations
    override val id: VID? get() = rawPart.id
    override val otherAttrs: Map<QName, String> get() = rawPart.otherAttrs
    override val base: QName? get() = rawPart.base
    override val openContents: List<XSOpenContent> get() = rawPart.openContents

    val baseType: ResolvedToplevelType by lazy {
        schema.type(base ?: AnyType.qName)
    }

    open fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>) {
        val b = base
        if (b!=null && b !in seenTypes) { // Recursion is allowed, but must be managed
            baseType.check(seenTypes, inheritedTypes)
        }
    }
}

class ResolvedComplexExtension(
    override val rawPart: T_ComplexExtensionType,
    schema: ResolvedSchemaLike
) : ResolvedDerivation(schema), T_ComplexExtensionType {
    override fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>) {
        super.check(seenTypes, inheritedTypes)
        require(base !in inheritedTypes.dropLastOrEmpty(1)) { "Recursive type use in complex content: $base" }
//        TODO("not implemented")
    }
}

class ResolvedComplexRestriction(
    override val rawPart: T_ComplexRestrictionType,
    schema: ResolvedSchemaLike
) : ResolvedDerivation(schema), T_ComplexRestrictionType {

    override val simpleTypes: List<ResolvedLocalSimpleType> =
        DelegateList(rawPart.simpleTypes) { ResolvedLocalSimpleType(it, schema) }

    override val facets: List<XSFacet> get() = rawPart.facets

    override val otherContents: List<CompactFragment>
        get() = rawPart.otherContents

    override fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>) {
        super.check(seenTypes, inheritedTypes)
//        TODO("not implemented")
    }
}

class ResolvedComplexShorthandContent(
    parent: ResolvedComplexType,
    override val rawPart: IXSComplexTypeShorthand,
    schema: ResolvedSchemaLike
) : ResolvedComplexContent(schema),
    T_ComplexTypeShorthandContent {

    override val groups: List<ResolvedGroupRef> = DelegateList(rawPart.groups) { ResolvedGroupRef(it, schema) }
    override val alls: List<ResolvedAll> = DelegateList(rawPart.alls) { ResolvedAll(parent, it, schema) }
    override val choices: List<ResolvedChoice> = DelegateList(rawPart.choices) { ResolvedChoice(parent, it, schema) }
    override val sequences: List<ResolvedSequence> = DelegateList(rawPart.sequences) { ResolvedSequence(parent, it, schema) }
    override val asserts: List<T_Assertion> get() = rawPart.asserts
    override val attributes: List<ResolvedLocalAttribute> = DelegateList(rawPart.attributes) { ResolvedLocalAttribute(it, schema) }
    override val attributeGroups: List<ResolvedAttributeGroupRef> = DelegateList(rawPart.attributeGroups) { ResolvedAttributeGroupRef(it, schema) }
    override val anyAttribute: XSAnyAttribute? get() = rawPart.anyAttribute
    override val openContents: List<XSOpenContent> get() = rawPart.openContents

    override fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>) {
        for (group in groups) { group.check() }
    }
}

class ResolvedComplexSimpleContent(
    override val rawPart: T_ComplexTypeSimpleContent,
    schema: ResolvedSchemaLike
) : ResolvedComplexContent(schema),
    T_ComplexTypeSimpleContent {


    override fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>) {
        //TODO("not implemented")
    }
}
