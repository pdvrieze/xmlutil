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
import io.github.pdvrieze.formats.xmlschema.types.AllNNIRange
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI

interface IResolvedChoice : ResolvedModelGroup {

    override val mdlParticles: List<ResolvedParticle<ResolvedTerm>>
    override val mdlCompositor: Compositor get() = Compositor.CHOICE


    override fun flatten(range: AllNNIRange): FlattenedParticle {
        if (range.endInclusive == VAllNNI.ZERO) return FlattenedGroup.EMPTY
        val newParticles = mutableListOf<FlattenedParticle>()
        for (p in mdlParticles) {
            if (p !is ResolvedProhibitedElement) {
                val f = p.flatten()
                if (f.maxOccurs > VAllNNI.ZERO) {
                    when (f) {
                        is FlattenedGroup.Choice -> f.particles.asSequence()
                            .filter { it.maxOccurs > VAllNNI.ZERO }
                            .mapTo(newParticles) { it * range }

                        else -> newParticles.add(f)
                    }
                }
            }
        }
        return when (newParticles.size) {
            0 -> FlattenedGroup.EMPTY
            1 -> newParticles.single() * range
            else -> FlattenedGroup.Choice(range, newParticles)
        }
    }

    override fun restricts(general: ResolvedModelGroup): Boolean {
        TODO("not implemented")
    }
}
