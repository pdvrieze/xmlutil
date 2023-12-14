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

import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedModelGroup.Compositor
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.AllNNIRange
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import nl.adaptivity.xmlutil.QName

interface IResolvedAll : ResolvedModelGroup {

    override val mdlParticles: List<ResolvedParticle<ResolvedTerm>>
    override val mdlCompositor: Compositor get() = Compositor.ALL

    override fun checkTerm(checkHelper: CheckHelper) {
        // super just calls check on the particles
        for (particle in mdlParticles) {
            particle.checkParticle(checkHelper)
            val maxOccurs = particle.mdlMaxOccurs
            check(maxOccurs <= VAllNNI(1uL)) {
                "All may only have maxOccurs<=1 for its particles. Not $maxOccurs"
            }
        }
    }

    override fun restricts(general: ResolvedModelGroup): Boolean {
        // TODO be order independent
        if (general !is IResolvedAll) return false
        val specificParticles = mdlParticles.toList()
        val generalParticles = general.mdlParticles.toList()

        var thisPos = 0

        for (generalPos in generalParticles.indices) {
            if (thisPos >= specificParticles.size) { // the particle must be ignorable
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
            if (!specificParticles[tailIdx].mdlIsEmptiable()) return false
        }
        return true
    }

    override fun flatten(range: AllNNIRange, typeContext: ResolvedComplexType, schema: ResolvedSchemaLike): FlattenedParticle {

        val particles = mutableListOf<FlattenedParticle>()
        val seenNames = mutableSetOf<QName>()
        val seenWildcards = mutableListOf<ResolvedAny>()
        for (p in mdlParticles) {
            val f = p.flatten(typeContext, schema)
            if (f.maxOccurs == VAllNNI.ZERO) continue // skip it
            particles.add(f)
            for(startElem in f.startingTerms()) {
                when (startElem) {
                    is FlattenedParticle.Element -> require(seenNames.add(startElem.term.mdlQName)) {
                        "Non-deterministic all group: all{${mdlParticles.joinToString()}}"
                    }

                    is FlattenedParticle.Wildcard -> {
                        require(seenWildcards.none { it.intersects(startElem.term) }) {
                            "Non-deterministic all group: all${mdlParticles.joinToString()}"
                        }
                        seenWildcards.add(startElem.term)
                    }
                }

            }
        }

        return when {
            particles.isEmpty() -> FlattenedGroup.EMPTY
            particles.size == 1 && range.isSimple -> particles.single()
            else -> FlattenedGroup.All(range, particles)
        }
    }

}
