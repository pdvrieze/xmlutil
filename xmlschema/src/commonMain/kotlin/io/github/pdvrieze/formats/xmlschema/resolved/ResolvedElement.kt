/*
 * Copyright (c) 2021.
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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.IDType
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.types.*
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.localPart

sealed class ResolvedElement(final override val schema: ResolvedSchemaLike) : ResolvedPart,
    ResolvedSimpleTypeContext, ResolvedTypeContext, ResolvedBasicTerm,
    ResolvedAnnotated {

    abstract override val rawPart: XSElement

    final override val id: VID? get() = rawPart.id

    val valueConstraint: ValueConstraint? by lazy {
        val rawDefault = rawPart.default
        val rawFixed = rawPart.fixed
        when {
            rawDefault != null && rawFixed != null ->
                throw IllegalArgumentException("An element ${rawPart.name} cannot have default and fixed attributes both")

            rawDefault != null -> ValueConstraint.Default(rawDefault)
            rawFixed != null -> ValueConstraint.Fixed(rawFixed)
            else -> null
        }
    }

    val localType: XSIType?
        get() = rawPart.localType

    open val name: VNCName? get() = rawPart.name

    val uniques: List<ResolvedUnique> get() = mdlIdentityConstraints.filterIsInstance<ResolvedUnique>()

    val keys: List<ResolvedKey> get() = mdlIdentityConstraints.filterIsInstance<ResolvedKey>()

    val keyrefs: List<ResolvedKeyRef> get() = mdlIdentityConstraints.filterIsInstance<ResolvedKeyRef>()

    protected abstract val model: ModelImpl

    override val mdlAnnotations: ResolvedAnnotation? get() = model.mdlAnnotations
    abstract val mdlQName: QName
    @Deprecated("use mdlQName where appropriate")
    val mdlName: VNCName get() = VNCName(mdlQName.localPart)

    // target namespace just in the qName

    val mdlTypeDefinition: ResolvedType get() = model.mdlTypeDefinition
    val mdlTypeTable: ITypeTable? get() = model.mdlTypeTable
    val mdlValueConstraint: ValueConstraint? get() = valueConstraint

    val mdlNillable: Boolean get() = model.mdlNillable
    val mdlIdentityConstraints: Set<ResolvedIdentityConstraint> get() = model.mdlIdentityConstraints
    val mdlSubstitutionGroupAffiliations: List<ResolvedGlobalElement> get() = model.mdlSubstitutionGroupAffiliations
    val mdlDisallowedSubstitutions: VBlockSet get() = model.mdlDisallowedSubstitutions
    val mdlSubstitutionGroupExclusions: Set<T_BlockSetValues> get() = model.mdlSubstitutionGroupExclusions


    /**
     * disallowed substitutions
     */
    val block: Set<T_BlockSetValues> get() = rawPart.block ?: schema.blockDefault

    final override val otherAttrs: Map<QName, String> get() = rawPart.otherAttrs

    protected fun checkSingleType() {
        require(rawPart.type == null || rawPart.localType == null) {
            "Types can only be specified in one way"
        }
    }

    fun subsumes(specific: ResolvedElement): Boolean { // subsume 4 (elements)
        if (!mdlNillable && specific.mdlNillable) return false // subsume 4.1

        val vc = mdlValueConstraint // subsume 4.2
        if (vc is ValueConstraint.Fixed && (specific.mdlValueConstraint as? ValueConstraint.Fixed)?.value != vc.value) {
            return false
        }

        // subsume 4.3
        if (!specific.mdlIdentityConstraints.containsAll(mdlIdentityConstraints)) return false

        // subsume 4.4
        if (specific.mdlDisallowedSubstitutions.size > mdlDisallowedSubstitutions.size &&
            specific.mdlDisallowedSubstitutions.containsAll(mdlDisallowedSubstitutions)
        ) return false

        // subsume 4.5
        if (!specific.mdlTypeDefinition.isValidRestrictionOf(mdlTypeDefinition)) return false

        // subsume 4.6
        val gtt = mdlTypeTable
        if (gtt == null) {
            if (specific.mdlTypeTable != null) return false
        } else {
            val stt = specific.mdlTypeTable
            if (stt == null) return false
            if (!gtt.isEquivalent(stt)) return false
        }
        return true
    }

    override fun check(checkedTypes: MutableSet<QName>) {
        super<ResolvedPart>.check(checkedTypes)
        for (keyref in keyrefs) {
            keyref.check(checkedTypes)
            checkNotNull(keyref.mdlReferencedKey)
        }
        // Remove as it is in mdlValueConstraint
        rawPart.default?.let { d ->
            check(rawPart.fixed == null) { "fixed and default can not both be present on element: ${name ?: (this as Ref).mdlTerm.mdlName}" }
            check((mdlTypeDefinition as? ResolvedSimpleType)?.mdlPrimitiveTypeDefinition != IDType) {
                "ID types can not have fixed values"
            }
            mdlTypeDefinition.validate(d)
        }
        rawPart.fixed?.let { f ->
            check((mdlTypeDefinition as? ResolvedSimpleType)?.mdlPrimitiveTypeDefinition != IDType) {
                "ID types can not have fixed values"
            }
            mdlTypeDefinition.validate(f)
        }

    }

    override fun collectConstraints(collector: MutableList<ResolvedIdentityConstraint>) {
        collector.addAll(mdlIdentityConstraints)
        (mdlTypeDefinition as? ResolvedLocalComplexType)?.collectConstraints(collector)
    }

    interface Ref {
        val mdlTerm: ResolvedGlobalElement
    }

    interface Model {
    }

    protected abstract class ModelImpl(
        rawPart: XSElement,
        schema: ResolvedSchemaLike,
        context: ResolvedElement
    ) {

        final val mdlNillable: Boolean = rawPart.nillable ?: false

        final val mdlAnnotations: ResolvedAnnotation? =
            rawPart.annotation.models()

        final val mdlIdentityConstraints: Set<ResolvedIdentityConstraint> =
            rawPart.identityConstraints.mapTo(HashSet<ResolvedIdentityConstraint>()) {
                ResolvedIdentityConstraint(it, schema, context)
            }

        val mdlValueConstraint: ValueConstraint? = run {
            val rawDefault = rawPart.default
            val rawFixed = rawPart.fixed
            when {
                rawDefault != null && rawFixed != null ->
                    throw IllegalArgumentException("An element ${rawPart.name} cannot have default and fixed attributes both")

                rawDefault != null -> ValueConstraint.Default(rawDefault)
                rawFixed != null -> ValueConstraint.Fixed(rawFixed)
                else -> null
            }
            ValueConstraint(rawPart)
        }
        abstract val mdlTypeDefinition: ResolvedType
        abstract val mdlSubstitutionGroupAffiliations: List<ResolvedGlobalElement>
        abstract val mdlTypeTable: ITypeTable?
        abstract val mdlDisallowedSubstitutions: VBlockSet
        abstract val mdlSubstitutionGroupExclusions: Set<T_BlockSetValues>
    }

    abstract val mdlScope: VElementScope
}

