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
import io.github.pdvrieze.formats.xmlschema.impl.flatMap
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
        get() = model.mdlSubstitutionGroupAffiliations.map {
            it.getOrElse { e-> throw IllegalStateException("Non-existing element in substitution group", e) }
        }

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

    fun fullSubstitutionGroup(version: SchemaVersion): List<ResolvedGlobalElement> {
        val map = mutableMapOf<QName, ResolvedGlobalElement>()
        fullSubstitutionGroup(map)
        return when(version) {
            //abstract members are not part of the substitution group (in 1.0)
            SchemaVersion.V1_0 -> map.values.filter { ! it.mdlAbstract }
            else -> map.values.toList()
        }
    }

    override fun checkTerm(checkHelper: CheckHelper) {
        super.checkTerm(checkHelper)
        checkSubstitutionGroupChain(SingleLinkedList(mdlQName), checkHelper)
        model.mdlTypeDefinition
            .onFailure(checkHelper::checkLax)
            .onSuccess { checkHelper.checkType(it) }

        if (SUBSTITUTION in mdlSubstitutionGroupExclusions) {
            check(mdlSubstitutionGroupMembers.isEmpty()) { "Element blocks substitution but is used as head of a substitution group" }
        }

        val otherExcluded = mdlSubstitutionGroupExclusions.toDerivationSet()
        for (member in mdlSubstitutionGroupMembers) {
            require(member.isSubstitutableFor(this, checkHelper)) {
                "Element ${member.mdlQName} is not not substitutable for ${this.mdlQName} but in its substitution group"
            }
        }

        if (mdlSubstitutionGroupMembers.isNotEmpty() && otherExcluded.isNotEmpty()) {
            for (substGroupMember in mdlSubstitutionGroupMembers) {
                substGroupMember.model.mdlTypeDefinition.map { derivType ->
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
                }.onFailure(checkHelper::checkLax)
            }
        }

    }

    /** Implements substitutable as define in 3.3.6.3 */
    private fun isSubstitutableFor(head: ResolvedGlobalElement, checkHelper: CheckHelper): Boolean {
        return model.mdlTypeDefinition.flatMap { td ->
            head.model.mdlTypeDefinition.map { htd ->
                td.isValidSubtitutionFor(htd, false)
            }
        }.onFailure(checkHelper::checkLax)
            .getOrDefault(false)
    }

    private fun checkSubstitutionGroupChain(seenElements: SingleLinkedList<QName>, checkHelper: CheckHelper) {
        for (substitutionGroupHead in model.mdlSubstitutionGroupAffiliations) {
            substitutionGroupHead.map { h ->
                require(h.mdlQName !in seenElements) {
                    "Recursive subsitution group: $mdlQName"
                }
                h.checkSubstitutionGroupChain(seenElements + mdlQName, checkHelper)
            }.onFailure(checkHelper::checkLax)
        }
    }

    override fun flatten(
        range: AllNNIRange,
        isSiblingName: (QName) -> Boolean,
        checkHelper: CheckHelper
    ): FlattenedParticle {
        // this factory handles substitution groups
        return FlattenedParticle.elementOrSubstitution(range, this, checkHelper.version)
    }

    override fun toString(): String {
        return "ResolvedGlobalElement($mdlQName, type=${model.mdlTypeDefinition.getOrDefault("<missing type>")})"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as ResolvedGlobalElement

        if (substitutionGroups != other.substitutionGroups) return false
        if (mdlQName != other.mdlQName) return false
        if (mdlSubstitutionGroupExclusions != other.mdlSubstitutionGroupExclusions) return false
        if (mdlAbstract != other.mdlAbstract) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (substitutionGroups?.hashCode() ?: 0)
        result = 31 * result + mdlQName.hashCode()
        result = 31 * result + mdlSubstitutionGroupExclusions.hashCode()
        result = 31 * result + mdlAbstract.hashCode()
        return result
    }

    class Model internal constructor(elemPart: SchemaElement<XSGlobalElement>, schema: ResolvedSchemaLike, context: ResolvedGlobalElement) :
        ResolvedElement.Model(elemPart.elem, schema, context) {

        val mdlTargetNamespace: VAnyURI? = schema.targetNamespace

        val mdlSubstitutionGroupAffiliations: List<Result<ResolvedGlobalElement>> =
            elemPart.elem.substitutionGroup?.map { kotlin.runCatching { schema.element(it) } } ?: emptyList()

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

        override val mdlTypeDefinition: Result<ResolvedType>

        init {
            val localType: SchemaElement<XSLocalType?> = elemPart.wrap { localType }

            mdlTypeDefinition = when {
                localType.elem != null -> { // local element first
                    Result.success(ResolvedLocalType(localType.cast<XSLocalType>(), schema, context))
                }

                elemPart.elem.type != null -> when (val t = schema.maybeType(elemPart.elem.type)) { // otherwise look up the type
                    null -> Result.failure(NoSuchElementException("No type with name '$elemPart.elem.type' found"))
                    else -> Result.success(t)
                }

                // Then the type of the first member of the substitution group (or AnyType in other cases)
                else -> when (val sgFirstName = elemPart.elem.substitutionGroup?.firstOrNull()) {
                    null -> Result.success(AnyType)
                    else -> when (val e = schema.maybeElement(sgFirstName)) {
                        null -> Result.failure(NoSuchElementException("No element with name $sgFirstName found"))
                        else -> e.model.mdlTypeDefinition
                    }
                }
            }
        }


        private fun checkSubstitutionGroupChainRecursion(
            qName: QName,
            substitutionGroups: List<ResolvedGlobalElement>,
            seenElements: SingleLinkedList<QName>
        ) {
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            if (!super.equals(other)) return false

            other as Model

            if (mdlTargetNamespace != other.mdlTargetNamespace) return false
            if (mdlSubstitutionGroupAffiliations != other.mdlSubstitutionGroupAffiliations) return false
            if (mdlTypeDefinition != other.mdlTypeDefinition) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + (mdlTargetNamespace?.hashCode() ?: 0)
            result = 31 * result + mdlSubstitutionGroupAffiliations.hashCode()
            result = 31 * result + mdlTypeDefinition.hashCode()
            return result
        }

    }


}
