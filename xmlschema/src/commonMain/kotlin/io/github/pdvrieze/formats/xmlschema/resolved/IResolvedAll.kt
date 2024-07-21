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
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.AllNNIRange
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import nl.adaptivity.xmlutil.QName

interface IResolvedAll : ResolvedModelGroup {

    override val mdlParticles: List<ResolvedParticle<ResolvedTerm>>
    override val mdlCompositor: Compositor get() = Compositor.ALL

    override fun checkTerm(checkHelper: CheckHelper) {
        val seenWildcards = mutableListOf<ResolvedAny>()
        // super just calls check on the particles
        for (particle in mdlParticles) {
            if (particle is ResolvedAny) {
                for (seen in seenWildcards) {
                    require(!seen.intersects(particle, ::isSiblingName, checkHelper.schema)) {
                        "Intersecting wildcards in all group: $particle and $seen"
                    }
                }
                seenWildcards.add(particle)
            }
            particle.checkParticle(checkHelper)
            if (checkHelper.version == SchemaVersion.V1_0) {
                val maxOccurs = particle.mdlMaxOccurs
                check(maxOccurs <= VAllNNI(1uL)) {
                    "All may only have maxOccurs<=1 for its particles. Not $maxOccurs"
                }
                if (particle !is ResolvedProhibitedElement) {
                    check(particle.mdlTerm is ResolvedElement) { "For version 1.0 all groups may only contain elements, not any's" }
                }
            } else { // v1.1
                if (particle is ResolvedGroupRef) { // groupRefs are not permitted in 1.0
                    require(particle.mdlMinOccurs == VNonNegativeInteger.ONE) { "Particle references in all must have 1 as minOccurs" }
                    require(particle.mdlMaxOccurs == VAllNNI.ONE) { "Particle references in all must have 1 as maxOccurs" }

                    check(particle.mdlTerm is IResolvedAll) { "Group references in all must be referencing all particles" }
                }
            }
        }
    }

    override fun <R> visit(visitor: ResolvedTerm.Visitor<R>): R = visitor.visitAll(this)

    override fun flatten(range: AllNNIRange, isSiblingName: (QName) -> Boolean, checkHelper: CheckHelper): FlattenedParticle {
        val particles = mutableListOf<FlattenedParticle>()
        val seenNames = mutableSetOf<QName>()
        val seenWildcards = mutableListOf<ResolvedAny>()
        for (p in mdlParticles) {
            val f = p.flatten(::isSiblingName, checkHelper)
            if (f.maxOccurs == VAllNNI.ZERO) continue // skip it
            particles.add(f)
            for(startElem in f.startingTerms()) {
                when (startElem) { // allow repetition
                    is FlattenedParticle.Element -> require(seenNames.add(startElem.term.mdlQName)) {
                        "Non-deterministic all group (${checkHelper.version}): all{${mdlParticles.joinToString()}}"
                    }

                    is FlattenedParticle.Wildcard -> {
                        require(seenWildcards.none { it.intersects(startElem.term, isSiblingName, checkHelper.schema) }) {
                            "Non-deterministic all group: all${mdlParticles.joinToString()}"
                        }
                        seenWildcards.add(startElem.term)
                    }
                }

            }
        }

        return when {
            particles.isEmpty() -> FlattenedGroup.EMPTY
            particles.size == 1 -> when {
                checkHelper.version != SchemaVersion.V1_0 ->
                    particles.single() * range // multiply will be null if not valid

                range.isSimple -> particles.single()

                else -> null
            }

            particles.size == 1 && range.isSimple -> particles.single()
            else -> null
        } ?: FlattenedGroup.All(range, particles, checkHelper.version)
    }

}
