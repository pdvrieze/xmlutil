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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSChoice
import io.github.pdvrieze.formats.xmlschema.model.ChoiceModel
import io.github.pdvrieze.formats.xmlschema.resolved.particles.ResolvedParticle
import io.github.pdvrieze.formats.xmlschema.types.T_AllNNI
import io.github.pdvrieze.formats.xmlschema.types.T_Choice
import nl.adaptivity.xmlutil.QName

class ResolvedChoice private constructor(
    override val rawPart: XSChoice,
    schema: ResolvedSchemaLike,
    override val mdlParticles: List<ResolvedParticle<ResolvedChoiceSeqMember>>,
    override val minOccurs: VNonNegativeInteger?,
    override val maxOccurs: T_AllNNI?,
) : ResolvedGroupParticleTermBase<IResolvedChoice>(schema),
    IResolvedChoice,
    T_Choice,
    ResolvedComplexType.ResolvedDirectParticle<IResolvedChoice>,
    ChoiceModel,
    ResolvedGroupParticle<IResolvedChoice> {

    constructor(
        parent: ResolvedParticleParent,
        rawPart: XSChoice,
        schema: ResolvedSchemaLike,
    ) : this(
        rawPart,
        schema,
        DelegateList(rawPart.particles) {
            ResolvedParticle.choiceSeqMember(parent, it, schema)
        },
        rawPart.minOccurs,
        rawPart.maxOccurs
    )


    override val mdlTerm: ResolvedChoice get() = this

    override fun collectConstraints(collector: MutableList<ResolvedIdentityConstraint>) {
        mdlParticles
    }

    override fun check(checkedTypes: MutableSet<QName>) {
        super<ResolvedGroupParticleTermBase>.check(checkedTypes)
    }


    override fun check() {
        super<IResolvedChoice>.check(mutableSetOf())
        rawPart.check(mutableSetOf())
    }

    override fun normalizeTerm(
        minMultiplier: VNonNegativeInteger,
        maxMultiplier: T_AllNNI
    ): ResolvedChoice {
        return ResolvedChoice(
            rawPart,
            schema,
            mdlParticles,
            minOccurs?.times(minMultiplier) ?: minMultiplier,
            maxOccurs?.times(maxMultiplier) ?: maxMultiplier
        )
    }
}
