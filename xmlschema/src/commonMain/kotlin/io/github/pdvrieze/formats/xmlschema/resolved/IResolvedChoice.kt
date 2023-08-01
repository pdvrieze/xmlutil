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
import io.github.pdvrieze.formats.xmlschema.model.ChoiceModel
import io.github.pdvrieze.formats.xmlschema.model.ChoiceSeqMember
import io.github.pdvrieze.formats.xmlschema.model.ModelGroupModel
import io.github.pdvrieze.formats.xmlschema.resolved.particles.ResolvedParticle
import io.github.pdvrieze.formats.xmlschema.types.T_AllNNI
import nl.adaptivity.xmlutil.QName

interface IResolvedChoice : ChoiceModel, ResolvedGroupLikeTerm, IResolvedGroupMember, ResolvedChoiceSeqMember {
    override val mdlParticles: List<ResolvedParticle<ResolvedChoiceSeqMember>>
    override val mdlCompositor: ModelGroupModel.Compositor get() = ModelGroupModel.Compositor.CHOICE

    override fun check(checkedTypes: MutableSet<QName>) {
        super<ResolvedGroupLikeTerm>.check(checkedTypes)
        //TODO("not implemented")
    }


    override fun normalize(
        minMultiplier: VNonNegativeInteger,
        maxMultiplier: T_AllNNI
    ): SyntheticChoice {
        var newMin: VNonNegativeInteger = minMultiplier
        var newMax: T_AllNNI = maxMultiplier
        if (this is ResolvedParticle<*>) {
            newMin*=mdlMinOccurs
            newMax*=mdlMaxOccurs
        }

        val newParticles = mutableListOf<ResolvedParticle<ResolvedChoiceSeqMember>>()
        for (particle in mdlParticles) {
            val cleanParticle = when (particle) {
                is ResolvedGroupRef -> particle.flattenToModelGroup(ResolvedChoiceSeqMember::class)
                else -> particle
            }

            when (val term: ResolvedChoiceSeqMember = cleanParticle.mdlTerm) {
                is IResolvedChoice ->
                    for (child in term.mdlParticles) {
                        newParticles.add(child.normalizeTerm(particle.mdlMinOccurs, particle.mdlMaxOccurs))
                    }

                else -> newParticles.add(particle.normalizeTerm())
            }
        }
        return SyntheticChoice(newMin, newMax, newParticles, schema)

    }

    override fun restricts(general: ResolvedGroupLikeTerm): Boolean {
        TODO("not implemented")
    }
}
