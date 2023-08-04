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
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import nl.adaptivity.xmlutil.QName

class ResolvedSequence private constructor(
    override val rawPart: XSSequence,
    override val mdlParticles: List<ResolvedParticle<ResolvedTerm>>,
    schema: ResolvedSchemaLike,
    override val mdlMinOccurs: VNonNegativeInteger,
    override val mdlMaxOccurs: VAllNNI,
) : ResolvedGroupParticleTermBase<IResolvedSequence>(schema),
    IResolvedSequence {

    override val mdlTerm: ResolvedSequence get() = this

    constructor(
        parent: VElementScope.Member,
        rawPart: XSSequence,
        schema: ResolvedSchemaLike
    ) : this(
        rawPart,
        DelegateList(rawPart.particles) {
            ResolvedParticle(parent, it, schema) as ResolvedParticle<ResolvedTerm>
        },
        schema,
        rawPart.minOccurs ?: VNonNegativeInteger.ONE,
        rawPart.maxOccurs ?: VAllNNI.ONE
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
        check(mutableSetOf())
    }

    override fun normalizeTerm(minMultiplier: VNonNegativeInteger, maxMultiplier: VAllNNI): ResolvedSequence {
        return ResolvedSequence(
            rawPart,
            mdlParticles,
            schema,
            mdlMinOccurs.times(minMultiplier),
            mdlMaxOccurs.times(maxMultiplier)
        )
    }

    override fun toString(): String {
        return buildString {
            append("ResolvedSequence(")
            if (mdlMinOccurs != VNonNegativeInteger.ONE) append("minOccurs=$mdlMinOccurs, ")
            if (mdlMaxOccurs != VAllNNI.ONE) append("maxOccurs=$mdlMaxOccurs, ")
            append(mdlParticles)
            append(")")
        }
    }


}

