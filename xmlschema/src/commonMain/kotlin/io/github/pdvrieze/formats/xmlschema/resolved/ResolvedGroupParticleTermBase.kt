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
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import nl.adaptivity.xmlutil.QName

sealed class ResolvedGroupParticleTermBase<T : ResolvedModelGroup>(
    rawPart: XSI_Grouplike,
    final override val schema: ResolvedSchemaLike
) : ResolvedGroupParticle<T> {

    final override val otherAttrs: Map<QName, String> = rawPart.resolvedOtherAttrs()

    override val mdlMinOccurs: VNonNegativeInteger
        get() = rawPart.minOccurs ?: VNonNegativeInteger.ONE

    override val mdlMaxOccurs: VAllNNI
        get() = rawPart.maxOccurs ?: VAllNNI.ONE

    abstract override val rawPart: XSI_Grouplike
    val particles: List<XSI_Particle>
        get() = rawPart.particles
    final override val mdlAnnotations: ResolvedAnnotation? by lazy { rawPart.annotation.models() }
    abstract val mdlParticles: List<ResolvedParticle<ResolvedTerm>>

    override fun check(checkedTypes: MutableSet<QName>) {
        super.check(checkedTypes)
        for (particle in mdlParticles) {
            particle.check(checkedTypes)
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
}
