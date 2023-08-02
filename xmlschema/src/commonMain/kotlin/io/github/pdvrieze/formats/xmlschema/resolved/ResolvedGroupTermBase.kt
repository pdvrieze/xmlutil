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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.resolved.particles.ResolvedParticle
import nl.adaptivity.xmlutil.QName

/**
 * Base class for all terms that contain groups: ResolvedAll, ResolvedChoice, ResolvedSequence, ResolvedGroup
 */
sealed class ResolvedGroupTermBase(
    final override val schema: ResolvedSchemaLike,
) : ResolvedPart, OldResolvedAnnotated, ResolvedTerm {

    abstract override val rawPart: XSI_Grouplike

    val particles: List<XSI_Particle>
        get() = rawPart.particles

    final val mdlAnnotations: ResolvedAnnotation? get() = rawPart.annotation.models()

    abstract val mdlParticles: List<ResolvedParticle<ResolvedTerm>>


    override fun check(checkedTypes: MutableSet<QName>) {
        super<OldResolvedAnnotated>.check(checkedTypes)
        for (particle in mdlParticles) {
            particle.check(checkedTypes)
        }
    }

    companion object {
        operator fun invoke(
            parent: ResolvedComplexType,
            rawPart: XSExplicitGroup,
            schema: ResolvedSchemaLike
        ): ResolvedGroupTermBase = when (rawPart) {
            is XSAll -> ResolvedAll(parent, rawPart, schema)
            is XSChoice -> ResolvedChoice(parent, rawPart, schema)
            is XSSequence -> ResolvedSequence(parent, rawPart, schema)
            else -> error("Found unsupported group: $rawPart")
        }
    }
}

