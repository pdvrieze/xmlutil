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

import io.github.pdvrieze.formats.xmlschema.datatypes.impl.SingleLinkedList
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSFacet
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSimpleContentRestriction
import io.github.pdvrieze.formats.xmlschema.types.T_LocalSimpleType
import io.github.pdvrieze.formats.xmlschema.types.T_SimpleType
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.util.CompactFragment

/**
 * Restriction is used for simple types.
 */
class ResolvedSimpleContentRestriction(
    override val rawPart: XSSimpleContentRestriction,
    schema: ResolvedSchemaLike
) : ResolvedSimpleContentDerivation(schema), T_SimpleType.T_Restriction {
    override val otherContents: List<CompactFragment> get() = rawPart.otherContents

    override val base: QName? get() = rawPart.base

    override val facets: List<XSFacet> get() = rawPart.facets

    override val simpleType: ResolvedLocalSimpleType? by lazy {
        rawPart.simpleType?.let { ResolvedLocalSimpleType(it, schema) }
    }

    override val baseType: ResolvedType by lazy { base?.let{ schema.type(it) } ?: checkNotNull(simpleType) }

    override fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>) {
        val b = base
        if (b == null) {
            requireNotNull(simpleType)
        } else {
            require(simpleType == null)
        }
        check(b !in inheritedTypes.dropLastOrEmpty()) { "Indirect recursive use of simple base types: $b in ${inheritedTypes.last()}"}
        if (b !in seenTypes) {
            val inherited = (baseType as? OptNamedPart)?.qName?.let(::SingleLinkedList) ?: SingleLinkedList.empty()
            baseType.check(seenTypes, inherited)
            // Recursion is allowed
        }
    }
}
