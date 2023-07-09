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
import io.github.pdvrieze.formats.xmlschema.model.ComplexTypeModel
import io.github.pdvrieze.formats.xmlschema.model.ElementModel
import io.github.pdvrieze.formats.xmlschema.model.SimpleTypeModel
import io.github.pdvrieze.formats.xmlschema.model.ValueConstraintModel
import io.github.pdvrieze.formats.xmlschema.types.*
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.isEquivalent

class ResolvedGlobalElement(
    override val rawPart: XSElement,
    schema: ResolvedSchemaLike
) : ResolvedElement(schema),
    T_GlobalElement,
    ElementModel.Global, ResolvedElement.Use {

    override val mdlName: VNCName get() = rawPart.name

    override fun check() {
        super<ResolvedElement>.check()
        checkSingleType()
        checkSubstitutionGroupChain(SingleLinkedList(qName))
        typeDef.check(SingleLinkedList(), SingleLinkedList())
        if (T_DerivationControl.SUBSTITUTION in mdlSubstitutionGroupExclusions) {
            check(mdlSubstitutionGroupMembers.isEmpty()) { "Element blocks substitution but is used as head of a substitution group" }
        }

        val otherExcluded = mdlSubstitutionGroupExclusions.toDerivationSet()
        if (mdlSubstitutionGroupMembers.isNotEmpty() && otherExcluded.isNotEmpty()) {
            for (substGroupMember in mdlSubstitutionGroupMembers) {
                val deriv = when (val t = substGroupMember.mdlTypeDefinition) {
                    is ResolvedComplexType -> t.mdlDerivationMethod
                    is ResolvedSimpleType -> when (t.mdlVariety) {
                        SimpleTypeModel.Variety.ATOMIC -> T_DerivationControl.RESTRICTION
                        SimpleTypeModel.Variety.LIST -> T_DerivationControl.LIST
                        SimpleTypeModel.Variety.UNION -> T_DerivationControl.UNION
                        SimpleTypeModel.Variety.NIL -> null
                    }
                    else -> null // shouldn't happen
                }
                if (deriv != null) {
                    check(deriv !in otherExcluded)
                }

            }
        }
    }

    private fun checkSubstitutionGroupChain(seenElements: SingleLinkedList<QName>) {
        for (substitutionGroupHead in substitutionGroups) {
            require(substitutionGroupHead.qName !in seenElements) {
                "Recursive subsitution group: $qName"
            }
            substitutionGroupHead.checkSubstitutionGroupChain(seenElements + qName)
        }
    }

    override fun toString(): String {
        return "ResolvedGlobalElement($qName, typeDef=$typeDef)"
    }

    val substitutionGroups: List<ResolvedGlobalElement> =
        DelegateList(rawPart.substitutionGroup ?: emptyList()) { schema.element(it) }

    /** Substitution group exclusions */
    override val final: Set<ComplexTypeModel.Derivation>
        get() = rawPart.final ?: schema.finalDefault.toDerivationSet()

    override val targetNamespace: VAnyURI? /*get()*/ = schema.targetNamespace

    override val name: VNCName get() = rawPart.name

    override val qName: QName
        get() = name.toQname(targetNamespace)

    val typeDef: ResolvedType = rawPart.localType?.let { ResolvedLocalType(it, schema, this) }
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

    val affiliatedSubstitutionGroups: List<ResolvedGlobalElement> = rawPart.substitutionGroup?.let {
        DelegateList(it) { schema.element(it) }
    } ?: emptyList()

    val mdlSubstitutionGroupMembers: List<ResolvedGlobalElement> get() = model.mdlSubstitutionGroupMembers

    val identityConstraints: List<ResolvedIdentityConstraint> by lazy {
        @Suppress("UNCHECKED_CAST")
        (keys + uniques + keyrefs) as List<ResolvedIdentityConstraint> // TODO make resolved versions
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

    override val model: Model by lazy { ModelImpl(rawPart, schema, this) }

    override val mdlScope: ElementModel.Scope.Global get() = model.mdlScope

    override val mdlTargetNamespace: VAnyURI? get() = model.mdlTargetNamespace

    interface Model : ResolvedElement.Model, ElementModel.Global, ElementModel.Scope.Global {
        val mdlSubstitutionGroupMembers: List<ResolvedGlobalElement>
    }

    private class ModelImpl(rawPart: XSElement, schema: ResolvedSchemaLike, context: ResolvedElement) :
        ResolvedElement.ModelImpl(rawPart, schema, context), Model {
        override val mdlScope: ElementModel.Scope.Global get() = this

        override val mdlName: VNCName = rawPart.name

        override val mdlSubstitutionGroupAffiliations: List<ResolvedGlobalElement> =
            rawPart.substitutionGroup?.map { schema.element(it) } ?: emptyList()

        override val mdlSubstitutionGroupMembers: List<ResolvedGlobalElement> by lazy {
            // Has to be lazy due to initialization loop

            val thisName: QName = context.qName!!
            checkSubstitutionGroupChain(thisName, mdlSubstitutionGroupAffiliations, SingleLinkedList.empty())

            val group = HashSet<ResolvedGlobalElement>()
            schema.elements
                .filterTo(group) { it.rawPart.substitutionGroup?.any { it.isEquivalent(thisName) } ?: false }

            for (child in group.toList()) {
                for (m in child.mdlSubstitutionGroupMembers) {
                    if (!group.add(m)) throw IllegalStateException("Substitution group is cyclic")
                }
                group.addAll(child.mdlSubstitutionGroupMembers)
            }
            group.toList()
        }

        override val mdlTargetNamespace: VAnyURI? =
            rawPart.targetNamespace ?: schema.targetNamespace

        override val mdlTypeTable: ElementModel.TypeTable? = rawPart.alternatives.takeIf { it.isNotEmpty() }?.let {

            TODO()
        }

        override val mdlValueConstraint: ValueConstraintModel?
            get() = TODO("not implemented")

        private fun checkSubstitutionGroupChain(qName: QName, substitutionGroups: List<ResolvedGlobalElement>, seenElements: SingleLinkedList<QName>) {
            for (substitutionGroupHead in substitutionGroups) {
                require(substitutionGroupHead.qName !in seenElements) {
                    "Recursive subsitution group: $qName"
                }
                substitutionGroupHead.checkSubstitutionGroupChain(seenElements + qName)
            }
        }

    }


}
