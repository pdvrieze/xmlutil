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
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import nl.adaptivity.xmlutil.QName

class ResolvedChoice private constructor(
    override val rawPart: XSChoice,
    schema: ResolvedSchemaLike,
    override val mdlParticles: List<ResolvedParticle<ResolvedTerm>>,
    override val mdlMinOccurs: VNonNegativeInteger,
    override val mdlMaxOccurs: VAllNNI,
) : ResolvedGroupParticleTermBase<IResolvedChoice>(schema),
    IResolvedChoice {

    constructor(
        parent: VElementScope.Member,
        rawPart: XSChoice,
        schema: ResolvedSchemaLike,
    ) : this(
        rawPart,
        schema,
        DelegateList(rawPart.particles) {
            ResolvedParticle.choiceSeqMember(parent, it, schema)
        },
        rawPart.minOccurs ?: VNonNegativeInteger.ONE,
        rawPart.maxOccurs ?: VAllNNI.ONE,
    )


    override val mdlTerm: ResolvedChoice get() = this

    override fun collectConstraints(collector: MutableList<ResolvedIdentityConstraint>) {
        mdlParticles
    }

    override fun check(checkedTypes: MutableSet<QName>) {
        super<ResolvedGroupParticleTermBase>.check(checkedTypes)
    }


    override fun check() {
        check(mutableSetOf())
    }

    override fun normalizeTerm(
        minMultiplier: VNonNegativeInteger,
        maxMultiplier: VAllNNI
    ): ResolvedChoice {
        return ResolvedChoice(
            rawPart,
            schema,
            mdlParticles,
            mdlMinOccurs * minMultiplier,
            mdlMaxOccurs * maxMultiplier
        )
    }

    override fun toString(): String {
        return buildString {
            append("ResolvedChoice(")
            mdlParticles.joinTo(this)
            append(")")
        }
    }
}
