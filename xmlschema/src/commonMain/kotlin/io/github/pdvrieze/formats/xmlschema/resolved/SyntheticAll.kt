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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSI_Particle
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import nl.adaptivity.xmlutil.QName

class SyntheticAll(
    override val mdlMinOccurs: VNonNegativeInteger,
    override val mdlMaxOccurs: VAllNNI,
    override val mdlParticles: List<ResolvedParticle<ResolvedTerm>>,
    override val schema: ResolvedSchemaLike,
) : ResolvedParticle<SyntheticAll>, IResolvedAll {

    override val otherAttrs: Map<QName, Nothing> get() = emptyMap()

    override val mdlTerm: SyntheticAll get() = this

    override val rawPart: XSI_Particle get() = XSI_Particle.DUMMY
    override val mdlAnnotations: ResolvedAnnotation? get() = null

    override fun collectConstraints(collector: MutableList<ResolvedIdentityConstraint>) {}
    override fun check(checkedTypes: MutableSet<QName>) {
        super<IResolvedAll>.check(checkedTypes)
        // TODO implement something
    }

    override fun check() {
    }

    override fun normalizeTerm(minMultiplier: VNonNegativeInteger, maxMultiplier: VAllNNI): SyntheticAll {
        return SyntheticAll(mdlMinOccurs*minMultiplier, mdlMaxOccurs*maxMultiplier, mdlParticles, schema)
    }
}
