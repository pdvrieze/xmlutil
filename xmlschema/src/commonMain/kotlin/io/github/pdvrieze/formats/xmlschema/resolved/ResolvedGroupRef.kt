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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSGroupRef
import io.github.pdvrieze.formats.xmlschema.resolved.particles.ResolvedParticle
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import nl.adaptivity.xmlutil.QName
import kotlin.reflect.KClass

class ResolvedGroupRef(
    override val rawPart: XSGroupRef,
    override val schema: ResolvedSchemaLike,
    override val minOccurs: VNonNegativeInteger? = rawPart.minOccurs,
    override val maxOccurs: VAllNNI? = rawPart.maxOccurs,
) : ResolvedGroupBase,
    ResolvedGroupParticle<ResolvedModelGroup> {

    override val mdlAnnotations: ResolvedAnnotation? = rawPart.annotation.models()

    override val mdlTerm: ResolvedModelGroup get() = referenced.mdlModelGroup

    val referenced: ResolvedGlobalGroup by lazy { schema.modelGroup(rawPart.ref) }

    val ref: QName get() = rawPart.ref

    override val mdlMinOccurs: VNonNegativeInteger get() = rawPart.minOccurs ?: VNonNegativeInteger.ONE

    override val mdlMaxOccurs: VAllNNI get() = rawPart.maxOccurs ?: VAllNNI.ONE


    override fun check(checkedTypes: MutableSet<QName>) {
        super<ResolvedGroupParticle>.check(checkedTypes)
        mdlTerm.check(checkedTypes)
    }

    override fun collectConstraints(collector: MutableList<ResolvedIdentityConstraint>) {
        mdlTerm.collectConstraints(collector)
    }

    override fun normalizeTerm(
        minMultiplier: VNonNegativeInteger,
        maxMultiplier: VAllNNI
    ): ResolvedParticle<ResolvedModelGroup> {
        return ResolvedGroupRef(
            rawPart,
            schema,
            minOccurs?.times(minMultiplier) ?: minMultiplier,
            maxOccurs?.times(maxMultiplier) ?: maxMultiplier,
        )
    }

    fun flattenToModelGroup(): ResolvedParticle<ResolvedTerm> {
        return mdlTerm.normalize(mdlMinOccurs, mdlMaxOccurs)
    }
}
