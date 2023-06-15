/*
 * Copyright (c) 2022.
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
import io.github.pdvrieze.formats.xmlschema.datatypes.impl.SingleLinkedList
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.*
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.util.CompactFragment

sealed class ResolvedSimpleDerivation(
    final override val schema: ResolvedSchemaLike
) : ResolvedPart, T_SimpleDerivation {
    abstract override val rawPart: T_SimpleDerivation

    abstract val baseType: T_SimpleBaseType

    override val annotations: List<XSAnnotation> get() = rawPart.annotations

    override val id: VID? get() = rawPart.id

    override val otherAttrs: Map<QName, String> get() = rawPart.otherAttrs

    abstract fun check(seenTypes: SingleLinkedList<QName>)
}

class ResolvedSimpleListDerivation(
    override val rawPart: T_SimpleListType,
    schema: ResolvedSchemaLike
) : ResolvedSimpleDerivation(schema), T_SimpleListType {

    override val itemTypeName: QName? get() = rawPart.itemTypeName
    override val simpleType: ResolvedLocalSimpleType? by lazy {
        rawPart.simpleType?.let { st ->
            ResolvedLocalSimpleType(st, schema)
        }
    }

    val itemType: ResolvedSimpleType by lazy {
        val itemTypeName = rawPart.itemTypeName
        when {
            itemTypeName != null -> schema.simpleType(itemTypeName)
            else -> {
                simpleType ?: error("Item type is not specified, either by name or member")
            }
        }
    }

    override val baseType: ResolvedSimpleType get() = AnySimpleType

    override fun check(seenTypes: SingleLinkedList<QName>) {
    }
}

class ResolvedSimpleUnionDerivation(
    override val rawPart: T_SimpleUnionType,
    schema: ResolvedSchemaLike
) : ResolvedSimpleDerivation(schema), T_SimpleUnionType {
    override val baseType: T_SimpleType get() = AnySimpleType

    override val simpleTypes: List<ResolvedLocalSimpleType> =
        DelegateList(rawPart.simpleTypes) { ResolvedLocalSimpleType(it, schema) }

    override val memberTypes: List<QName>?
        get() = rawPart.memberTypes

    val resolvedMembers: List<ResolvedSimpleType>

    override val annotations: List<XSAnnotation> get() = rawPart.annotations

    override val id: VID? get() = rawPart.id

    override val otherAttrs: Map<QName, String> get() = rawPart.otherAttrs

    init {
        val mt = rawPart.memberTypes
        resolvedMembers = when {
            mt == null -> simpleTypes
            rawPart.simpleTypes.isEmpty() -> DelegateList(mt) { schema.simpleType(it) }
            else -> CombiningList(
                simpleTypes,
                DelegateList(mt) { schema.simpleType(it) }
            )
        }

    }

    override fun check(seenTypes: SingleLinkedList<QName>) {
        require(resolvedMembers.isNotEmpty()) { "Union without elements" }
        for (m in resolvedMembers) {
            (m as? ResolvedToplevelType)?.let { require(it.qName !in seenTypes) { "Recursive presence of ${it.qName}" } }
        }
    }
}

abstract class ResolvedSimpleRestrictionDerivation(schema: ResolvedSchemaLike) : ResolvedSimpleDerivation(schema),
    T_SimpleRestrictionType

abstract class ResolvedSimpleExtensionDerivation(schema: ResolvedSchemaLike) : ResolvedSimpleDerivation(schema),
    T_SimpleExtensionType

fun ResolvedSimpleRestrictionDerivation(
    rawPart: T_SimpleRestrictionType,
    schema: ResolvedSchemaLike
): ResolvedSimpleRestrictionDerivation = ResolvedSimpleRestrictionDerivationImpl(rawPart, schema)

fun ResolvedSimpleExtensionDerivation(
    rawPart: XSSimpleContentExtension,
    schema: ResolvedSchemaLike
): ResolvedSimpleExtensionDerivation = ResolvedSimpleExtensionDerivationImpl(rawPart, schema)

class ResolvedSimpleRestrictionDerivationImpl(
    override val rawPart: T_SimpleRestrictionType,
    schema: ResolvedSchemaLike
) : ResolvedSimpleRestrictionDerivation(schema), T_SimpleRestrictionType {
    override val simpleTypes: List<ResolvedLocalSimpleType> = DelegateList(rawPart.simpleTypes) {
        ResolvedLocalSimpleType(it, schema)
    }

    override val facets: List<XSFacet> = rawPart.facets

    override val otherContents: List<CompactFragment> get() = rawPart.otherContents

    override val annotations: List<XSAnnotation> get() = rawPart.annotations

    override val id: VID? get() = rawPart.id

    override val otherAttrs: Map<QName, String> get() = rawPart.otherAttrs

    override val base: QName? get() = rawPart.base

    override val baseType: ResolvedSimpleType by lazy {
        when (val baseName = base) {
            null -> simpleTypes.single()
            else -> schema.simpleType(baseName)
        }
    }

    override fun check(seenTypes: SingleLinkedList<QName>) {
        val b = base
        if (b == null) {
            require(simpleTypes.size == 1)
        } else {
            require(simpleTypes.isEmpty())
        }
        if (b !in seenTypes) {
            val inherited = baseType.qName ?.let(::SingleLinkedList) ?: SingleLinkedList.empty()
            baseType.check(seenTypes, inherited)
            // Recursion is allowed
        }
    }
}

class ResolvedSimpleExtensionDerivationImpl(
    override val rawPart: T_SimpleExtensionType,
    schema: ResolvedSchemaLike
) : ResolvedSimpleExtensionDerivation(schema), T_SimpleExtensionType {
    override val asserts: List<T_Assertion> get() = rawPart.asserts
    override val attributes: List<ResolvedLocalAttribute> = DelegateList(rawPart.attributes) { ResolvedLocalAttribute(it, schema) }
    override val attributeGroups: List<ResolvedAttributeGroupRef> = DelegateList(rawPart.attributeGroups) { ResolvedAttributeGroupRef(it, schema) }
    override val anyAttribute: XSAnyAttribute? get() = rawPart.anyAttribute
    override val annotations: List<XSAnnotation> get() = rawPart.annotations

    override val id: VID? get() = rawPart.id

    override val otherAttrs: Map<QName, String> get() = rawPart.otherAttrs

    override val base: QName get() = rawPart.base

    override val baseType: ResolvedSimpleType by lazy {
        schema.simpleType(base)
    }

    override fun check(seenTypes: SingleLinkedList<QName>) {
        val b = base

        if (b !in seenTypes) {
            val inherited = baseType.qName ?.let(::SingleLinkedList) ?: SingleLinkedList.empty()
            baseType.check(seenTypes, inherited)
            // Recursion is allowed
        }
    }
}
