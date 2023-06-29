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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.model.*
import io.github.pdvrieze.formats.xmlschema.types.*

fun ResolvedExplicitGroup(
    parent: ResolvedComplexType?,
    rawPart: XSExplicitGroup,
    schema: ResolvedSchemaLike
): ResolvedExplicitGroup<*> = when (rawPart) {
    is XSAll -> ResolvedAll(parent, rawPart, schema)
    is XSChoice -> ResolvedChoice(parent, rawPart, schema)
    is XSSequence -> ResolvedSequence(parent, rawPart, schema)
    else -> error("Found unsupported group: $rawPart")
}

sealed class ResolvedExplicitGroup<out T: ResolvedExplicitGroup<T>>(
    parent: ResolvedComplexType?,
    override val schema: ResolvedSchemaLike
) : ResolvedPart, ResolvedAnnotated, T_ExplicitGroupParticle, ResolvedGroupParticle<T>, ModelGroupComponent {
    abstract override val rawPart: XSExplicitGroup

    final override val particles: List<T_Particle>
        get() = rawPart.particles

    final override val minOccurs: VNonNegativeInteger
        get() = mdlMinOccurs
    final override val mdlMinOccurs: VNonNegativeInteger
        get() = rawPart.minOccurs ?: VNonNegativeInteger(1)

    override val maxOccurs: T_AllNNI get() = mdlMaxOccurs

    final override val mdlMaxOccurs: T_AllNNI
        get() = rawPart.maxOccurs ?: T_AllNNI(1)

    final override val mdlAnnotations: ResolvedAnnotation? get() = rawPart.annotation.models()

    abstract override val mdlParticles: List<ResolvedParticle<*>>


    override fun check() {
        super<ResolvedAnnotated>.check()
        for (particle in mdlParticles) {
            particle.check()
        }
    }
}

