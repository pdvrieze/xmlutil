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
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedModelGroup.Compositor
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import nl.adaptivity.xmlutil.QName

interface IResolvedChoice : ResolvedModelGroup {

    override val mdlParticles: List<ResolvedParticle<ResolvedTerm>>
    override val mdlCompositor: Compositor get() = Compositor.CHOICE

    override fun check(checkedTypes: MutableSet<QName>) {
        super.check(checkedTypes)
        //TODO("not implemented")
    }


    override fun normalize(
        minMultiplier: VNonNegativeInteger,
        maxMultiplier: VAllNNI
    ): SyntheticChoice {
        var newMin: VNonNegativeInteger = minMultiplier
        var newMax: VAllNNI = maxMultiplier
        if (this is ResolvedParticle<*>) {
            newMin*=mdlMinOccurs
            newMax*=mdlMaxOccurs
        }

        val newParticles = mutableListOf<ResolvedParticle<ResolvedTerm>>()
        for (particle in mdlParticles) {
            val cleanParticle = when (particle) {
                is ResolvedGroupRef -> particle.flattenToModelGroup()
                else -> particle
            }

            when (val term: ResolvedTerm = cleanParticle.mdlTerm) {
                is IResolvedChoice ->
                    for (child in term.mdlParticles) {
                        newParticles.add(child.normalizeTerm(particle.mdlMinOccurs, particle.mdlMaxOccurs))
                    }

                else -> newParticles.add(particle.normalizeTerm())
            }
        }
        return SyntheticChoice(newMin, newMax, newParticles, schema)

    }

    override fun restricts(general: ResolvedModelGroup): Boolean {
        TODO("not implemented")
    }
}
