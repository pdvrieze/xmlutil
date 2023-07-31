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

package io.github.pdvrieze.formats.xmlschema.resolved.particles

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.model.ParticleModel
import io.github.pdvrieze.formats.xmlschema.resolved.*
import io.github.pdvrieze.formats.xmlschema.types.T_AllNNI
import io.github.pdvrieze.formats.xmlschema.types.T_Particle


interface ResolvedParticle<out T : ResolvedTerm> : ResolvedPart, ResolvedAnnotated, T_Particle, ParticleModel<T> {
    override val rawPart: XSI_Particle

    fun normalizeTerm(
        minMultiplier: VNonNegativeInteger = VNonNegativeInteger.ONE,
        maxMultiplier: T_AllNNI = T_AllNNI.ONE
    ): ResolvedParticle<T>

    companion object {
        fun allMember(
            parent: ResolvedParticleParent,
            rawPart: XSI_AllParticle,
            schema: ResolvedSchemaLike
        ): ResolvedParticle<ResolvedAllMember> = when (rawPart) {
            is XSAny -> ResolvedAny(rawPart, schema)
            is XSGroupRefParticle -> ResolvedGroupRefParticle(rawPart, schema)
            is XSLocalElement -> ResolvedLocalElement(parent, rawPart, schema)
        }

        fun choiceSeqMember(
            parent: ResolvedParticleParent,
            rawPart: XSI_NestedParticle,
            schema: ResolvedSchemaLike
        ): ResolvedParticle<ResolvedChoiceSeqMember> = when (rawPart) {
            is XSChoice -> ResolvedChoice(parent, rawPart, schema)
            is XSAny -> ResolvedAny(rawPart, schema)
            is XSGroupRefParticle -> ResolvedGroupRefParticle(rawPart, schema)
            is XSLocalElement -> ResolvedLocalElement(parent, rawPart, schema)
            is XSSequence -> ResolvedSequence(parent, rawPart, schema)
        }

        operator fun invoke(
            parent: ResolvedParticleParent,
            rawPart: XSI_Particle,
            schema: ResolvedSchemaLike
        ): ResolvedParticle<ResolvedTerm> = when (rawPart) {
            is XSAll -> ResolvedAll(parent, rawPart, schema)
            is XSChoice -> ResolvedChoice(parent, rawPart, schema)
            is XSSequence -> ResolvedSequence(parent, rawPart, schema)
            is XSGroupRefParticle -> ResolvedGroupRefParticle(rawPart, schema)
            is XSGroupRef -> ResolvedGroupRef(rawPart, schema)
            is XSAny -> ResolvedAny(rawPart, schema)
            is XSLocalElement -> ResolvedLocalElement(requireNotNull(parent), rawPart, schema)
            XSI_Particle.DUMMY -> error("Dummy cannot be resolved")
        }

        operator fun invoke(
            parent: ResolvedParticleParent,
            rawPart: XSExplicitGroup,
            schema: ResolvedSchemaLike
        ): ResolvedParticle<*> = when (rawPart) {
            is XSAll -> ResolvedAll(parent, rawPart, schema)
            is XSChoice -> ResolvedChoice(parent, rawPart, schema)
            is XSSequence -> ResolvedSequence(parent, rawPart, schema)
        }
    }
}
