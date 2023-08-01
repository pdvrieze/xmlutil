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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSequence
import io.github.pdvrieze.formats.xmlschema.model.ModelGroupModel
import io.github.pdvrieze.formats.xmlschema.resolved.particles.ResolvedParticle
import io.github.pdvrieze.formats.xmlschema.types.T_AllNNI
import nl.adaptivity.xmlutil.QName

class ResolvedSequence private constructor(
    override val rawPart: XSSequence,
    override val mdlParticles: List<ResolvedParticle<ResolvedChoiceSeqMember>>,
    schema: ResolvedSchemaLike,
    override val minOccurs: VNonNegativeInteger?,
    override val maxOccurs: T_AllNNI?,
) : ResolvedGroupParticleTermBase<IResolvedSequence>(schema),
    ResolvedComplexType.ResolvedDirectParticle<IResolvedSequence>,
    ResolvedGroupParticle<IResolvedSequence>,
    ResolvedGroupLikeTerm,
    ModelGroupModel,
    IResolvedSequence {

    override val mdlTerm: ResolvedSequence get() = this

    constructor(
        parent: ResolvedParticleParent,
        rawPart: XSSequence,
        schema: ResolvedSchemaLike
    ) : this(
        rawPart,
        DelegateList(rawPart.particles) {
            ResolvedParticle(parent, it, schema) as ResolvedParticle<ResolvedChoiceSeqMember>
        },
        schema,
        rawPart.minOccurs,
        rawPart.maxOccurs
    )

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

    override fun collectConstraints(collector: MutableList<ResolvedIdentityConstraint>) {
        mdlParticles.forEach { particle -> particle.mdlTerm.collectConstraints(collector) }
    }

    override fun check(checkedTypes: MutableSet<QName>) {
        super<ResolvedGroupParticleTermBase>.check(checkedTypes)
        val names = mutableSetOf<QName>()
        for (elem in mdlParticles) {
            if (elem is ResolvedLocalElement) {
                check(names.add(elem.mdlQName)) { "Duplicate element with name ${elem.mdlQName} found in sequence" }
            }
        }
    }


    override fun check() {
        super<IResolvedSequence>.check(mutableSetOf())
        rawPart.check(mutableSetOf())
    }

    override fun normalizeTerm(minMultiplier: VNonNegativeInteger, maxMultiplier: T_AllNNI): ResolvedSequence {
        return ResolvedSequence(
            rawPart,
            mdlParticles,
            schema,
            minOccurs?.times(minMultiplier) ?: minMultiplier,
            maxOccurs?.times(maxMultiplier)?: maxMultiplier
        )
    }

    override fun toString(): String {
        return buildString {
            append("ResolvedSequence(")
            if (minOccurs!=null) append("minOccurs=$minOccurs, ")
            if (maxOccurs!=null) append("maxOccurs=$maxOccurs, ")
            append(mdlParticles)
            append(")")
        }
    }


}

