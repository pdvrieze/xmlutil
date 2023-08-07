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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import nl.adaptivity.xmlutil.QName

sealed class ResolvedGroupParticleTermBase<T : ResolvedModelGroup>(
    parent: VElementScope.Member,
    rawPart: XSI_Grouplike,
    schema: ResolvedSchemaLike,
    final override val mdlMinOccurs: VNonNegativeInteger = rawPart.minOccurs ?: VNonNegativeInteger.ONE,
    final override val mdlMaxOccurs: VAllNNI = rawPart.maxOccurs ?: VAllNNI.ONE,
) : ResolvedGroupParticle<T>, ResolvedTerm {

    final override val model: Model by lazy {
        Model(parent, rawPart, schema)
    }

    val mdlParticles: List<ResolvedParticle<ResolvedTerm>> get() = model.particles

    final override fun collectConstraints(collector: MutableList<ResolvedIdentityConstraint>) {
        mdlParticles.forEach { particle ->
            particle.mdlTerm.takeIf { it!is ResolvedGlobalElement }?.collectConstraints(collector)
        }
    }

    operator fun invoke(
        parent: ResolvedComplexType,
        rawPart: XSExplicitGroup,
        schema: ResolvedSchemaLike
    ): ResolvedGroupParticle<ResolvedModelGroup> = when (rawPart) {
        is XSAll -> ResolvedAll(parent, rawPart, schema)
        is XSChoice -> ResolvedChoice(parent, rawPart, schema)
        is XSSequence -> ResolvedSequence(parent, rawPart, schema)
        else -> error("Found unsupported group: $rawPart")
    }

    class Model : ResolvedAnnotated.Model {
        val particles: List<ResolvedParticle<ResolvedTerm>>

        constructor(
            particles: List<ResolvedParticle<ResolvedTerm>>,
            annotations: List<ResolvedAnnotation> = emptyList(),
            id: VID? = null,
            otherAttrs: Map<QName, String> = emptyMap(),
        ) : super(annotations, id, otherAttrs) {
            this.particles = particles
        }

        constructor(
            parent: VElementScope.Member,
            rawPart: XSI_Grouplike,
            schema: ResolvedSchemaLike,
        ) : super(rawPart) {
            particles = rawPart.particles.map { ResolvedParticle(parent, it, schema) }
        }
    }
}
