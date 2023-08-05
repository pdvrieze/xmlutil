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

interface IResolvedSequence : ResolvedModelGroup {

    override val mdlParticles: List<ResolvedParticle<ResolvedTerm>>

    override val mdlCompositor: Compositor get() = Compositor.SEQUENCE


    override fun normalize(
        minMultiplier: VNonNegativeInteger,
        maxMultiplier: VAllNNI
    ): SyntheticSequence {
        var newMin: VNonNegativeInteger = minMultiplier
        var newMax: VAllNNI = maxMultiplier
        if (this is ResolvedParticle<*>) {
            newMin *= mdlMinOccurs
            newMax *= mdlMaxOccurs
        }

        val newParticles = mutableListOf<ResolvedParticle<ResolvedTerm>>()
        for (particle in mdlParticles) {
            val cleanParticle = when (particle) {
                is ResolvedGroupRef -> particle.flattenToModelGroup()
                else -> particle
            }

            when (val term: ResolvedTerm = cleanParticle.mdlTerm) {
                is IResolvedSequence ->
                    for (child in term.mdlParticles) {
                        newParticles.add(child.normalizeTerm(particle.mdlMinOccurs, particle.mdlMaxOccurs))
                    }

                else -> newParticles.add(particle.normalizeTerm())
            }
        }
        return SyntheticSequence(newMin, newMax, newParticles, schema)

    }

    override fun restricts(general: ResolvedModelGroup): Boolean {
        if (general !is IResolvedSequence) return false
        val specificParticles = mdlParticles.toList()
        val generalParticles = general.mdlParticles.toList()

        var thisPos = 0

        for (generalPos in generalParticles.indices) {
            if (thisPos>=specificParticles.size) { // the particle must be ignorable
                if (!generalParticles[generalPos].mdlIsEmptiable()) return false
            } else {
                if (specificParticles[thisPos] != generalParticles[generalPos]) {
                    return false
                } else {
                    ++thisPos
                }
            }
        }
        for (tailIdx in thisPos until specificParticles.size) {
            if(!specificParticles[tailIdx].mdlIsEmptiable()) return false
        }
        return true
    }
}
