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
import io.github.pdvrieze.formats.xmlschema.datatypes.impl.SingleLinkedList
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSimpleUnion
import io.github.pdvrieze.formats.xmlschema.types.T_SimpleType
import nl.adaptivity.xmlutil.QName

class ResolvedUnionDerivation(
    override val rawPart: XSSimpleUnion,
    schema: ResolvedSchemaLike
) : ResolvedSimpleDerivationBase(schema),
    T_SimpleType.T_Union {
    override val baseType: ResolvedSimpleType get() = AnySimpleType

    override val simpleTypes: List<ResolvedLocalSimpleType> =
        DelegateList(rawPart.simpleTypes) { ResolvedLocalSimpleType(it, schema) }

    override val memberTypes: List<QName>?
        get() = rawPart.memberTypes

    val resolvedMembers: List<ResolvedSimpleType>

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

    override fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>) {
        require(resolvedMembers.isNotEmpty()) { "Union without elements" }
        for (m in resolvedMembers) {
            (m as? ResolvedGlobalType)?.let {
                require(it.qName !in inheritedTypes) { "Recursive presence of ${it.qName}" }
                if (it.qName !in seenTypes) {
                    m.check(seenTypes, inheritedTypes)
                }
            }
        }
    }
}
