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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAll
import io.github.pdvrieze.formats.xmlschema.resolved.particles.ResolvedParticle
import io.github.pdvrieze.formats.xmlschema.types.T_All
import io.github.pdvrieze.formats.xmlschema.types.T_AllNNI
import nl.adaptivity.xmlutil.QName


class ResolvedAll private constructor(
    override val rawPart: XSAll,
    schema: ResolvedSchemaLike,
    override val mdlParticles: List<ResolvedParticle<ResolvedAllMember>>,
    override val minOccurs: VNonNegativeInteger?,
    override val maxOccurs: T_AllNNI.Value?,
) : ResolvedGroupParticleTermBase<IResolvedAll>(schema),
    IResolvedAll,
    ResolvedComplexType.ResolvedDirectParticle<IResolvedAll>,
    ResolvedGroupLikeTerm,
    ResolvedGroupParticle<IResolvedAll>,
    T_All {

    constructor(
        parent: ResolvedParticleParent,
        rawPart: XSAll,
        schema: ResolvedSchemaLike,
    ) : this(
        rawPart,
        schema,
        DelegateList(rawPart.particles) {
            ResolvedParticle.allMember(parent, it, schema)
        },
        rawPart.minOccurs,
        rawPart.maxOccurs
    )

    override val mdlTerm: ResolvedAll get() = this

    init {
        require(mdlMinOccurs.toUInt() <= 1.toUInt()) { "minOccurs must be 0 or 1, but was $minOccurs" }
        require(mdlMaxOccurs.let { it is T_AllNNI.Value && it.toUInt() <= 1.toUInt() }) { "maxOccurs must be 0 or 1, but was $maxOccurs" }
    }

    override fun collectConstraints(collector: MutableList<ResolvedIdentityConstraint>) {
        mdlParticles.forEach { particle -> particle.mdlTerm.collectConstraints(collector) }
    }

    override fun check(checkedTypes: MutableSet<QName>) {
        super<IResolvedAll>.check(checkedTypes)
    }

    override fun check() {
        super<IResolvedAll>.check(mutableSetOf())
        rawPart.check(mutableSetOf())
    }

    override fun normalizeTerm(
        minMultiplier: VNonNegativeInteger,
        maxMultiplier: T_AllNNI
    ): ResolvedParticle<IResolvedAll> {
        TODO("not implemented")
    }
}

