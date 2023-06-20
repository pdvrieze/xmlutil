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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSElement
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_DerivationSet
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_Scope
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_TopLevelElement
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.toDerivationSet
import nl.adaptivity.xmlutil.QName

class ResolvedToplevelElement(
    override val rawPart: XSElement,
    schema: ResolvedSchemaLike
) : ResolvedElement(schema), T_TopLevelElement {
    override fun check() {
        super<ResolvedElement>.check()
        checkSubstitutionGroupChain(SingleLinkedList(qName))
        typeDef.check(SingleLinkedList(), SingleLinkedList())
    }

    private fun checkSubstitutionGroupChain(seenElements: SingleLinkedList<QName>) {
        for (substitutionGroupHead in substitutionGroups) {
            require(substitutionGroupHead.qName !in seenElements) {
                "Recursive subsitution group: $qName"
            }
            substitutionGroupHead.checkSubstitutionGroupChain(seenElements + qName)
        }
    }

    val substitutionGroups: List<ResolvedToplevelElement> =
        DelegateList(rawPart.substitutionGroup ?: emptyList()) { schema.element(it) }

    /** Substitution group exclusions */
    override val final: T_DerivationSet
        get() = rawPart.final ?: schema.finalDefault.toDerivationSet()

    override val targetNamespace: VAnyURI get() = schema.targetNamespace

    override val name: VNCName get() = rawPart.name
    override val qName: QName
        get() = name.toQname(targetNamespace)

    val typeDef: ResolvedType = rawPart.localType?.let { ResolvedLocalType(it, schema) }
        ?: type?.let {
            schema.type(it)
        }
        ?: rawPart.substitutionGroup?.firstOrNull()?.let { schema.element(it).typeDef }
        ?: AnyType


    val typeTable: TypeTable? by lazy {
        when (rawPart.alternatives.size) {
            0 -> null
            else -> TypeTable(
                alternatives = rawPart.alternatives.filter { it.test != null },
                default = rawPart.alternatives.lastOrNull()?.let {
                    null //TODO actually use resolved types
                } ?: null
            )
        }
    }

    override val scope: T_Scope get() = T_Scope.GLOBAL

    val affiliatedSubstitutionGroups: List<ResolvedElement> = rawPart.substitutionGroup?.let {
        DelegateList(it) { schema.element(it) }
    } ?: emptyList()


    val identityConstraints: List<ResolvedIdentityConstraint> by lazy {
        keys + uniques + keyrefs // TODO make resolved versions
    }

    override val uniques: List<ResolvedUnique> = DelegateList(rawPart.uniques) { ResolvedUnique(it, schema, this) }

    override val keys: List<ResolvedKey> = DelegateList(rawPart.keys) { ResolvedKey(it, schema, this) }

    override val keyrefs: List<ResolvedKeyRef> = DelegateList(rawPart.keyrefs) { ResolvedKeyRef(it, schema, this) }

    override val substitutionGroup: List<QName>?
        get() = rawPart.substitutionGroup

    override val abstract: Boolean get() = rawPart.abstract ?: false

    override val ref: Nothing?
        get() = rawPart.ref

    override val minOccurs: Nothing? get() = null

    override val maxOccurs: Nothing? get() = null

    override val form: Nothing? get() = null
}
