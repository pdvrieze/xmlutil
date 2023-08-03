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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalElement
import io.github.pdvrieze.formats.xmlschema.impl.invariantNotNull
import io.github.pdvrieze.formats.xmlschema.resolved.particles.ResolvedParticle
import io.github.pdvrieze.formats.xmlschema.types.T_BlockSetValues
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import io.github.pdvrieze.formats.xmlschema.types.VBlockSet
import io.github.pdvrieze.formats.xmlschema.types.VFormChoice
import nl.adaptivity.xmlutil.QName

class ResolvedElementRef(
    val parent: VElementScope.Member,
    override val rawPart: XSLocalElement,
    override val schema: ResolvedSchemaLike,
    override val minOccurs: VNonNegativeInteger? = rawPart.minOccurs,
    override val maxOccurs: VAllNNI? = rawPart.maxOccurs,
) : IResolvedElementUse,
    ResolvedParticle<ResolvedElement>,
    ResolvedComplexTypeContext {

    val ref: QName = invariantNotNull(rawPart.ref) { "Element references must have a ref property" }

    init {
        check(rawPart.name == null) { "XXX" }
    }

    val referenced: ResolvedGlobalElement get() = mdlElementDeclaration

    override val mdlElementDeclaration: ResolvedGlobalElement get() = model.mdlElementDeclaration

    val mdlQName: QName = mdlElementDeclaration.mdlQName

    val form: VFormChoice? get() = rawPart.form

    private val model: Model by lazy { Model(rawPart, schema, this) }

    val mdlScope: VElementScope.Local get() = VElementScope.Local(parent)

    override val mdlTerm: ResolvedGlobalElement get() = mdlElementDeclaration

    val mdlTargetNamespace: VAnyURI? get() = model.mdlTargetNamespace
    override val mdlMinOccurs: VNonNegativeInteger get() = rawPart.minOccurs ?: VNonNegativeInteger.ONE
    override val mdlMaxOccurs: VAllNNI get() = rawPart.maxOccurs ?: VAllNNI.ONE

    override fun check(checkedTypes: MutableSet<QName>) {
        if (rawPart.ref != null) {
            referenced// Don't check as that would already be done at top level
            check(rawPart.name == null) { "Local elements can not have both a name and ref attribute specified" }
            check(rawPart.block.isNullOrEmpty()) { "Local element references cannot have the block attribute specified: $rawPart" }
            check(rawPart.type == null) { "Local element references cannot have the type attribute specified" }
            check(rawPart.nillable == null) {
                "Local element references cannot have the nillable attribute specified"
            }
            check(rawPart.default == null) { "Local element references cannot have the default attribute specified" }
            check(rawPart.fixed == null) { "Local element references cannot have the default attribute specified" }
            check(rawPart.form == null) { "Local element references cannot have the default attribute specified" }
        } else {
            check(rawPart.name != null) { "Missing name for local (non-referencing) element" }
        }
    }

    override fun normalizeTerm(
        minMultiplier: VNonNegativeInteger,
        maxMultiplier: VAllNNI
    ): ResolvedElementRef = when {
        minMultiplier != VNonNegativeInteger.ONE || maxMultiplier != VAllNNI.ONE -> {
            ResolvedElementRef(
                parent,
                rawPart,
                schema,
                minOccurs?.times(minMultiplier) ?: minMultiplier,
                maxOccurs?.times(maxMultiplier) ?: maxMultiplier,
            )
        }

        else -> this
    }

    override fun toString(): String {
        return buildString {
            append("ResolvedLocalElement(")
            append("mdlName=$mdlQName, ")
            if (minOccurs != null) append("minOccurs=$minOccurs, ")
            if (maxOccurs != null) append("maxOccurs=$maxOccurs, ")
            append("type=${referenced.mdlTypeDefinition}")
            append(")")
        }
    }

    protected inner class Model(rawPart: XSLocalElement, schema: ResolvedSchemaLike, context: ResolvedElementRef) {

        val mdlElementDeclaration: ResolvedGlobalElement =
            schema.element(invariantNotNull(rawPart.ref) { "Element references must have a ref property" })

        val mdlTargetNamespace: VAnyURI? get() = rawPart.targetNamespace ?: schema.targetNamespace

        val mdlTerm: ResolvedElementRef get() = this@ResolvedElementRef


        val mdlMinOccurs: VNonNegativeInteger = rawPart.minOccurs ?: VNonNegativeInteger.ONE

        val mdlMaxOccurs: VAllNNI = rawPart.maxOccurs ?: VAllNNI.ONE

        val mdlTypeTable: ITypeTable
            get() = TODO("not implemented")

        val mdlDisallowedSubstitutions: VBlockSet =
            (rawPart.block ?: schema.blockDefault)

        val mdlSubstitutionGroupExclusions: Set<T_BlockSetValues> =
            schema.finalDefault.filterIsInstanceTo(HashSet())

        val mdlTypeDefinition: ResolvedType =
            rawPart.localType?.let { ResolvedLocalType(it, schema, context) }
                ?: rawPart.type?.let { schema.type(it) }
                ?: AnyType

    }

    interface Parent

}
