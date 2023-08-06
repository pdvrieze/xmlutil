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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.IDType
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.*
import nl.adaptivity.xmlutil.QName

sealed class ResolvedElement(rawPart: XSElement, final override val schema: ResolvedSchemaLike) :
    VTypeScope.Member, ResolvedBasicTerm, ResolvedAnnotated {


    final override val otherAttrs: Map<QName, String> = rawPart.resolvedOtherAttrs()


    init {
        require(rawPart.type == null || rawPart.localType == null) {
            "3.3.3(3) - Elements may not have both a type attribute and an inline type definition"
        }

        rawPart.alternatives.dropLast(1).forEach {
            requireNotNull(it.test) {
                "3.3.3(5) check that they have a test attribute (except the last where it is optional)"
            }
        }

    }

    abstract override val rawPart: XSElement

    abstract override val model: Model

    abstract val mdlQName: QName

    // target namespace just in the qName

    val mdlNillable: Boolean = rawPart.nillable ?: false

    val mdlTypeDefinition: ResolvedType get() = model.mdlTypeDefinition

    val mdlTypeTable: ITypeTable? get() = model.mdlTypeTable

    val mdlValueConstraint: ValueConstraint? get() = model.mdlValueConstraint

    val mdlIdentityConstraints: Set<ResolvedIdentityConstraint> get() = model.mdlIdentityConstraints

    val mdlDisallowedSubstitutions: VBlockSet get() = (rawPart.block ?: schema.blockDefault)

    abstract val mdlSubstitutionGroupExclusions: Set<T_BlockSetValues>

    abstract val mdlAbstract: Boolean

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

    override fun checkTerm(checkHelper: CheckHelper) {
        for (constraint in mdlIdentityConstraints) {
            checkHelper.checkConstraint(constraint)
        }
        mdlValueConstraint?.let {
            mdlTypeDefinition.validate(it.value)
            check((mdlTypeDefinition as? ResolvedSimpleType)?.mdlPrimitiveTypeDefinition != IDType) {
                "ID types can not have fixed values"
            }
        }
        checkHelper.checkType(mdlTypeDefinition)
    }

    override fun collectConstraints(collector: MutableList<ResolvedIdentityConstraint>) {
        collector.addAll(mdlIdentityConstraints)
    }

    abstract class Model(
        rawPart: XSElement,
        schema: ResolvedSchemaLike,
        context: ResolvedElement
    ) : ResolvedAnnotated.Model(rawPart) {

        abstract val mdlTypeDefinition: ResolvedType

        abstract val mdlTypeTable: ITypeTable?

        val mdlValueConstraint: ValueConstraint? = ValueConstraint(rawPart)

        val mdlIdentityConstraints: Set<ResolvedIdentityConstraint> =
            rawPart.identityConstraints.mapTo(HashSet()) {
                ResolvedIdentityConstraint(it, schema, context)
            }
    }

    abstract val mdlScope: VElementScope
}

