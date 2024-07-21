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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.IDType
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSElement
import io.github.pdvrieze.formats.xmlschema.impl.flatMap
import io.github.pdvrieze.formats.xmlschema.resolved.FlattenedParticle.Element
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.AllNNIRange
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import nl.adaptivity.xmlutil.QName

sealed class ResolvedElement(rawPart: XSElement, schema: ResolvedSchemaLike) :
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

    abstract override val model: Model

    abstract val mdlQName: QName

    // target namespace just in the qName

    val mdlNillable: Boolean = rawPart.nillable ?: false

    val mdlTypeDefinition: ResolvedType get() = model.mdlTypeDefinition
        .onFailure { e -> throw IllegalStateException("No type definition found", e) }
        .getOrThrow()

    val mdlTypeTable: ITypeTable? get() = model.mdlTypeTable

    val mdlValueConstraint: ValueConstraint? get() = model.mdlValueConstraint

    val mdlIdentityConstraints: Set<ResolvedIdentityConstraint> get() = model.mdlIdentityConstraints

    val mdlDisallowedSubstitutions: Set<VDerivationControl.T_BlockSetValues> =
        rawPart.block ?: schema.blockDefault
            .filterIsInstanceTo<VDerivationControl.T_BlockSetValues, HashSet<VDerivationControl.T_BlockSetValues>>(
                HashSet()
            )

    abstract val mdlSubstitutionGroupExclusions: Set<VDerivationControl.T_BlockSetValues>

    abstract val mdlAbstract: Boolean

    override fun flatten(
        range: AllNNIRange,
        isSiblingName: (QName) -> Boolean,
        checkHelper: CheckHelper
    ): FlattenedParticle {
        return Element(range, this, true)
    }

    fun subsumes(specific: ResolvedElement, isLax: Boolean): Boolean { // subsume 4 (elements)
        if (!mdlNillable && specific.mdlNillable) return false // subsume 4.1

        val bvc = mdlValueConstraint // subsume 4.2
        val svc = specific.mdlValueConstraint
        if (bvc is ValueConstraint.Fixed) {
            if (svc !is ValueConstraint.Fixed) return false

            val t = model.mdlTypeDefinition.map {
                it as? ResolvedSimpleType ?: return false
            }.onFailure { if (!isLax) throw it }
                .getOrElse { AnySimpleType }

            val bVal = t.value(bvc.value)
            val sVal = t.value(svc.value)
            if (bVal != sVal) return false
        }

        // subsume 4.3
        if (!specific.mdlIdentityConstraints.containsAll(mdlIdentityConstraints)) return false

        // subsume 4.4
        if (!specific.mdlDisallowedSubstitutions.containsAll(mdlDisallowedSubstitutions)
        ) return false

        // subsume 4.5
        specific.model.mdlTypeDefinition.flatMap { std ->
            model.mdlTypeDefinition.onSuccess { btd ->
                if (!std.isValidRestrictionOf(btd)) return false
            }
        }.onFailure { if (!isLax) throw it }

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
        super.checkTerm(checkHelper)

        for (constraint in mdlIdentityConstraints) {
            checkHelper.checkConstraint(constraint)
        }

        model.mdlTypeDefinition
            .onFailure(checkHelper::checkLax)
            .onSuccess { td ->
                mdlValueConstraint?.let {
                    td.validate(it.value, checkHelper.version)
                    if (checkHelper.version == SchemaVersion.V1_0) {
                        check((td as? ResolvedSimpleType)?.mdlPrimitiveTypeDefinition != IDType) {
                            "ID types can not have fixed values"
                        }
                    }
                }

                checkHelper.checkType(td)
            }
    }

    override fun <R> visit(visitor: ResolvedTerm.Visitor<R>): R = visitor.visitElement(this)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ResolvedElement

        if (otherAttrs != other.otherAttrs) return false
        if (mdlQName != other.mdlQName) return false
        if (mdlNillable != other.mdlNillable) return false
        if (mdlDisallowedSubstitutions != other.mdlDisallowedSubstitutions) return false
        if (mdlSubstitutionGroupExclusions != other.mdlSubstitutionGroupExclusions) return false
        if (mdlAbstract != other.mdlAbstract) return false
        if (mdlScope != other.mdlScope) return false
        if (model != other.model) return false

        return true
    }

    override fun hashCode(): Int {
        var result = otherAttrs.hashCode()
        result = 31 * result + model.hashCode()
        result = 31 * result + mdlQName.hashCode()
        result = 31 * result + mdlNillable.hashCode()
        result = 31 * result + mdlDisallowedSubstitutions.hashCode()
        result = 31 * result + mdlSubstitutionGroupExclusions.hashCode()
        result = 31 * result + mdlAbstract.hashCode()
        result = 31 * result + mdlScope.hashCode()
        return result
    }

    abstract class Model(
        rawPart: XSElement,
        schema: ResolvedSchemaLike,
        context: ResolvedElement
    ) : ResolvedAnnotated.Model(rawPart) {

        abstract val mdlTypeDefinition: Result<ResolvedType>

        abstract val mdlTypeTable: ITypeTable?

        val mdlValueConstraint: ValueConstraint? = ValueConstraint(rawPart)

        val mdlIdentityConstraints: Set<ResolvedIdentityConstraint> =
            rawPart.identityConstraints.mapTo(HashSet()) {
                ResolvedIdentityConstraint(it, schema, context)
            }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Model

            if (mdlTypeDefinition != other.mdlTypeDefinition) return false
            if (mdlTypeTable != other.mdlTypeTable) return false
            if (mdlValueConstraint != other.mdlValueConstraint) return false
            if (mdlIdentityConstraints != other.mdlIdentityConstraints) return false

            return true
        }

        override fun hashCode(): Int {
            var result = mdlTypeDefinition.hashCode()
            result = 31 * result + (mdlTypeTable?.hashCode() ?: 0)
            result = 31 * result + (mdlValueConstraint?.hashCode() ?: 0)
            result = 31 * result + mdlIdentityConstraints.hashCode()
            return result
        }


    }



    abstract val mdlScope: VElementScope
}

