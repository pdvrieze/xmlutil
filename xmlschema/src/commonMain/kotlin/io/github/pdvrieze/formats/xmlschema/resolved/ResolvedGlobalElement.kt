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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSGlobalElement
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalType
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.AllNNIRange
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl.SUBSTITUTION
import io.github.pdvrieze.formats.xmlschema.types.toDerivationSet
import nl.adaptivity.xmlutil.QName

class ResolvedGlobalElement private constructor(
    elemPart: SchemaElement<XSGlobalElement>,
    schema: ResolvedSchemaLike,
    val location: String = "",
) : ResolvedElement(elemPart.elem, schema), NamedPart {

    val mdlSubstitutionGroupAffiliations: List<ResolvedGlobalElement>
        get() = model.mdlSubstitutionGroupAffiliations

    override val model: Model by lazy { Model(elemPart, schema, this) }

    internal val substitutionGroups: List<QName>? = elemPart.elem.substitutionGroup

    override val mdlQName: QName =
        elemPart.elem.name.toQname(schema.targetNamespace) // does not take elementFormDefault into account

    override val mdlSubstitutionGroupExclusions: Set<VDerivationControl.T_BlockSetValues> =
        (elemPart.elem.final ?: schema.finalDefault).filterIsInstanceTo(HashSet())

    val mdlSubstitutionGroupMembers: List<ResolvedGlobalElement>
        get() = model.mdlSubstitutionGroupMembers

    override val mdlAbstract: Boolean = elemPart.elem.abstract ?: false

    override val mdlScope: VElementScope.Global get() = VElementScope.Global

    internal constructor(elemPart: SchemaElement<XSGlobalElement>, schema: ResolvedSchemaLike) :
            this(elemPart, elemPart.effectiveSchema(schema), elemPart.schemaLocation)

    private fun fullSubstitutionGroup(collector: MutableMap<QName, ResolvedGlobalElement>) {
        val old = collector.put(mdlQName, this)
        if (old == null) {
            for(m in mdlSubstitutionGroupMembers) {
                m.fullSubstitutionGroup(collector)
            }
        }
    }

    fun fullSubstitutionGroup(schemaVersion: SchemaVersion): List<ResolvedGlobalElement> {
        val map = mutableMapOf<QName, ResolvedGlobalElement>()
        fullSubstitutionGroup(map)
        if (schemaVersion==SchemaVersion.V1_0) {
            return map.values.filter { !it.mdlAbstract } //abstract members are not part of the substitution group
        } else {
            return map.values.toList() // in version 1.1 they are part of the substitution group (but not legal for use)
        }
    }

    override fun checkTerm(checkHelper: CheckHelper) {
        super.checkTerm(checkHelper)
        checkSubstitutionGroupChain(SingleLinkedList(mdlQName))
        checkHelper.checkType(mdlTypeDefinition)

        if (SUBSTITUTION in mdlSubstitutionGroupExclusions) {
            check(mdlSubstitutionGroupMembers.isEmpty()) { "Element blocks substitution but is used as head of a substitution group" }
        }

        val otherExcluded = mdlSubstitutionGroupExclusions.toDerivationSet()
        for (member in mdlSubstitutionGroupMembers) {
            require(member.isSubstitutableFor(this)) {
                "Element ${member.mdlQName} is not not substitutable for ${this.mdlQName} but in its substitution group"
            }
        }

        if (mdlSubstitutionGroupMembers.isNotEmpty() && otherExcluded.isNotEmpty()) {
            for (substGroupMember in mdlSubstitutionGroupMembers) {
                val derivType = substGroupMember.mdlTypeDefinition
                val derivMethod = when (derivType) {
                    is ResolvedComplexType -> derivType.mdlDerivationMethod
                    is ResolvedSimpleType -> when (derivType.mdlVariety) {
                        ResolvedSimpleType.Variety.ATOMIC -> VDerivationControl.RESTRICTION
                        ResolvedSimpleType.Variety.LIST -> VDerivationControl.LIST
                        ResolvedSimpleType.Variety.UNION -> VDerivationControl.UNION
                        ResolvedSimpleType.Variety.NIL -> null
                    }

                    else -> null // shouldn't happen
                }
                if (derivMethod != null) {
                    check(derivMethod !in otherExcluded)
                }
            }
        }

    }

    /** Implements substitutable as define in 3.3.6.3 */
    private fun isSubstitutableFor(head: ResolvedGlobalElement): Boolean {
        return mdlTypeDefinition.isValidSubtitutionFor(head.mdlTypeDefinition, false)
    }

    private fun checkSubstitutionGroupChain(seenElements: SingleLinkedList<QName>) {
        for (substitutionGroupHead in mdlSubstitutionGroupAffiliations) {
            require(substitutionGroupHead.mdlQName !in seenElements) {
                "Recursive subsitution group: $mdlQName"
            }
            substitutionGroupHead.checkSubstitutionGroupChain(seenElements + mdlQName)
        }
    }

    override fun flatten(
        range: AllNNIRange,
        isSiblingName: (QName) -> Boolean,
        schema: ResolvedSchemaLike
    ): FlattenedParticle {
        // this factory handles substitution groups
        return FlattenedParticle.elementOrSubstitution(range, this, schema.version)
    }

    override fun toString(): String {
        return "ResolvedGlobalElement($mdlQName, type=${mdlTypeDefinition})"
    }

    class Model internal constructor(elemPart: SchemaElement<XSGlobalElement>, schema: ResolvedSchemaLike, context: ResolvedGlobalElement) :
        ResolvedElement.Model(elemPart.elem, schema, context) {

        val mdlTargetNamespace: VAnyURI? = schema.targetNamespace

        val mdlSubstitutionGroupAffiliations: List<ResolvedGlobalElement> =
            elemPart.elem.substitutionGroup?.map { schema.element(it) } ?: emptyList()

        val mdlSubstitutionGroupMembers: List<ResolvedGlobalElement> by lazy {
            // Has to be lazy due to initialization loop

            val thisName: QName = context.mdlQName

            when {
                SUBSTITUTION in (elemPart.elem.block ?: emptySet()) -> emptyList()

                else -> {
                    val group = HashSet<ResolvedGlobalElement>()
                    group.addAll(schema.substitutionGroupMembers(thisName))

                    for (child in group.toList()) {
                        group.addAll(child.mdlSubstitutionGroupMembers)
                    }
                    group.toList()
                }
            }
        }

        override val mdlTypeTable: ITypeTable? get() = null

        override val mdlTypeDefinition: ResolvedType =
            (elemPart.wrap { localType }.let { if(it.elem!=null) ResolvedLocalType(it.cast<XSLocalType>(), schema, context) else null }
                ?: elemPart.elem.type?.let { schema.type(it) }
                ?: elemPart.elem.substitutionGroup?.firstOrNull()
                    ?.let { schema.element(it).mdlTypeDefinition }
                ?: AnyType) as ResolvedType


        private fun checkSubstitutionGroupChainRecursion(
            qName: QName,
            substitutionGroups: List<ResolvedGlobalElement>,
            seenElements: SingleLinkedList<QName>
        ) {
        }

    }


}
