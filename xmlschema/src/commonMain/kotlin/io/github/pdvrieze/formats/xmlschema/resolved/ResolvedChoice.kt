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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSChoice
import io.github.pdvrieze.formats.xmlschema.model.ChoiceModel
import io.github.pdvrieze.formats.xmlschema.model.ModelGroupModel
import io.github.pdvrieze.formats.xmlschema.resolved.particles.ResolvedParticle
import io.github.pdvrieze.formats.xmlschema.types.T_Choice
import nl.adaptivity.xmlutil.QName

interface IResolvedChoice : ChoiceModel, ResolvedGroupLikeTerm, ModelGroupModel, IResolvedGroupMember {
    override val mdlParticles: List<ResolvedParticle<ResolvedChoiceSeqMember>>
    override val mdlCompositor: ModelGroupModel.Compositor get() = ModelGroupModel.Compositor.CHOICE

    override fun check(checkedTypes: MutableSet<QName>) {
        //TODO("not implemented")
    }
}

class ResolvedChoice(
    parent: ResolvedParticleParent,
    override val rawPart: XSChoice,
    schema: ResolvedSchemaLike
) : ResolvedGroupParticleTermBase<ResolvedChoice>(schema),
    IResolvedChoice,
    T_Choice,
    ResolvedComplexType.ResolvedDirectParticle<ResolvedChoice>,
    ChoiceModel,
    ResolvedChoiceSeqMember,
    ResolvedGroupParticle<ResolvedChoice> {

    override val mdlTerm: ResolvedChoice get() = this

    override val mdlParticles: List<ResolvedParticle<ResolvedChoiceSeqMember>> = DelegateList(rawPart.particles) {
        ResolvedParticle.choiceSeqMember(parent, it, schema)
    }

    override fun collectConstraints(collector: MutableList<ResolvedIdentityConstraint>) {
        mdlParticles
    }

    /*
            override val choices: List<ResolvedChoice> =
                DelegateList(rawPart.choices) { ResolvedChoice(parent, it, schema) }

            override val sequences: List<ResolvedSequence> =
                DelegateList(rawPart.sequences) { ResolvedSequence(parent, it, schema) }
        */

    /*
        init {
            require(minOccurs.toUInt() <= 1.toUInt()) { "minOccurs must be 0 or 1, but was $minOccurs"}
            require(maxOccurs.toUInt() <= 1.toUInt()) { "maxOccurs must be 0 or 1, but was $maxOccurs"}
        }
    */

    override fun check(checkedTypes: MutableSet<QName>) {
        super<ResolvedGroupParticleTermBase>.check(checkedTypes)
    }
}
