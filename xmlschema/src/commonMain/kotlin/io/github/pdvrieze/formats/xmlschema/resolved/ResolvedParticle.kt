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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VUnsignedLong
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.AllNNIRange
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI


interface ResolvedParticle<out T : ResolvedTerm> : ResolvedAnnotated {

    val range: AllNNIRange get() = AllNNIRange(mdlMinOccurs, mdlMaxOccurs)
    val mdlMinOccurs: VNonNegativeInteger
    val mdlMaxOccurs: VAllNNI
    val mdlTerm: T

    val effectiveTotalRange: AllNNIRange
        get() = when (val t = mdlTerm) {
            is IResolvedAll,
            is IResolvedSequence -> {
                var min: VNonNegativeInteger = VUnsignedLong.ZERO
                var max: VAllNNI = VAllNNI.Value(0u)
                for (particle in (t as ResolvedModelGroup).mdlParticles) {
                    val r = particle.effectiveTotalRange
                    min += r.start
                    max += r.endInclusive
                }
                AllNNIRange(mdlMinOccurs * min, mdlMaxOccurs * max)
            }

            is IResolvedChoice -> {
                var minMin: VNonNegativeInteger = VUnsignedLong(ULong.MAX_VALUE)
                var maxMax: VAllNNI = VAllNNI.Value(0u)
                for (particle in t.mdlParticles) {
                    val r = particle.effectiveTotalRange
                    minMin = minMin.coerceAtMost(r.start)
                    maxMax = maxMax.coerceAtLeast(r.endInclusive)
                }
                minMin = minMin.coerceAtMost(maxMax)
                AllNNIRange(mdlMinOccurs * minMin, mdlMaxOccurs * maxMax)

            }

            else -> AllNNIRange(VAllNNI.Value(mdlMinOccurs), mdlMaxOccurs)
        }

    fun checkParticle(checkHelper: CheckHelper) {
        check(mdlMinOccurs <= mdlMaxOccurs) { "MinOccurs should be <= than maxOccurs" }
        mdlTerm.checkTerm(checkHelper)
    }

    fun mdlIsEmptiable(): Boolean {
        return effectiveTotalRange.start.toUInt() == 0u
    }

    fun normalizeTerm(): ResolvedParticle<T> {
        return this
    }

    fun collectConstraints(collector: MutableCollection<ResolvedIdentityConstraint>)

    companion object {

        operator fun invoke(
            parent: VElementScope.Member,
            rawPart: XSI_Particle,
            schema: ResolvedSchemaLike
        ): ResolvedParticle<ResolvedTerm> = when (rawPart) {
            is XSAll -> ResolvedAll(parent, rawPart, schema)
            is XSChoice -> ResolvedChoice(parent, rawPart, schema)
            is XSSequence -> ResolvedSequence(parent, rawPart, schema)
            is XSGroupRef -> ResolvedGroupRef(rawPart, schema)
            is XSAny -> ResolvedAny(rawPart, schema)
            is XSLocalElement -> IResolvedElementUse(parent, rawPart, schema)
        }

        operator fun invoke(
            parent: VElementScope.Member,
            rawPart: XSExplicitGroup,
            schema: ResolvedSchemaLike
        ): ResolvedParticle<*> = when (rawPart) {
            is XSAll -> ResolvedAll(parent, rawPart, schema)
            is XSChoice -> ResolvedChoice(parent, rawPart, schema)
            is XSSequence -> ResolvedSequence(parent, rawPart, schema)
        }
    }
}
