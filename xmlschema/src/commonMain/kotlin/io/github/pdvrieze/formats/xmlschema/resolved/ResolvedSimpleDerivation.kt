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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_SimpleDerivation
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_SimpleRestrictionType
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_SimpleType
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_SimpleUnionType
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.util.CompactFragment

sealed class ResolvedSimpleDerivation(
    override val schema: ResolvedSchemaLike
) : ResolvedPart, G_SimpleDerivation.Types {
    abstract override val rawPart: XSSimpleDerivation

    abstract val baseType: T_SimpleType

    abstract fun check(owner: ResolvedSimpleType, seenTypes: SingleLinkedList<QName>)
}

class ResolvedSimpleListDerivation(
    override val rawPart: XSSimpleList,
    schema: ResolvedSchemaLike
) : ResolvedSimpleDerivation(schema), G_SimpleDerivation.List {
    override val baseType: T_SimpleType get() = AnySimpleType

    override fun check(owner: ResolvedSimpleType, seenTypes: SingleLinkedList<QName>) {
        TODO("not implemented")
    }
}

class ResolvedSimpleUnionDerivation(
    override val rawPart: XSSimpleUnion,
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

    override fun check(owner: ResolvedSimpleType, seenTypes: SingleLinkedList<QName>) {
        require(resolvedMembers.isNotEmpty()) { "Union without elements" }
        for (m in resolvedMembers) {
            (m as? ResolvedToplevelType)?.let { require(it.qName !in seenTypes) { "Recursive presence of ${it.qName}"} }
        }
    }
}

class ResolvedSimpleRestrictionDerivation(
    override val rawPart: XSSimpleRestriction,
    schema: ResolvedSchemaLike
) : ResolvedSimpleDerivation(schema), T_SimpleRestrictionType {
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

    override fun check(owner: ResolvedSimpleType, seenTypes: SingleLinkedList<QName>) {
        val b = base
        if (b == null) {
            require(simpleTypes.size == 1)
        } else {
            require(simpleTypes.isEmpty())
        }

        baseType.check(seenTypes)
    }
}
