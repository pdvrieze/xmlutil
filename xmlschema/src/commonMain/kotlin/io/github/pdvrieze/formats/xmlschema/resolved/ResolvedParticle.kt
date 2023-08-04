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
import io.github.pdvrieze.formats.xmlschema.types.AllNNIRange
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import nl.adaptivity.xmlutil.QName


interface ResolvedParticle<out T : ResolvedTerm> : ResolvedPart, ResolvedAnnotated {
    override val rawPart: XSI_Particle

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

    override fun check(checkedTypes: MutableSet<QName>) {

        super<ResolvedPart>.check(checkedTypes)
        check(mdlMinOccurs <= mdlMaxOccurs) { "MinOccurs should be <= than maxOccurs" }

    }

    fun normalizeTerm(
        minMultiplier: VNonNegativeInteger = VNonNegativeInteger.ONE,
        maxMultiplier: VAllNNI = VAllNNI.ONE
    ): ResolvedParticle<T>

    fun mdlIsEmptiable(): Boolean {
        return effectiveTotalRange.start.toUInt() == 0u
    }

    companion object {
        fun allMember(
            parent: VElementScope.Member,
            rawPart: XSI_AllParticle,
            schema: ResolvedSchemaLike
        ): ResolvedParticle<ResolvedTerm> = when (rawPart) {
            is XSAny -> ResolvedAny(rawPart, schema)
            is XSGroupRef -> ResolvedGroupRef(
                rawPart,
                schema
            )

            is XSLocalElement -> IResolvedElementUse(parent, rawPart, schema)
        }

        fun choiceSeqMember(
            parent: VElementScope.Member,
            rawPart: XSI_NestedParticle,
            schema: ResolvedSchemaLike
        ): ResolvedParticle<ResolvedTerm> = when (rawPart) {
            is XSChoice -> ResolvedChoice(parent, rawPart, schema)
            is XSAny -> ResolvedAny(rawPart, schema)
            is XSGroupRef -> ResolvedGroupRef(rawPart, schema)

            is XSLocalElement -> IResolvedElementUse(parent, rawPart, schema)
            is XSSequence -> ResolvedSequence(parent, rawPart, schema)
            else -> error("Compiler issue")
        }

        operator fun invoke(
            parent: VElementScope.Member,
            rawPart: XSI_Particle,
            schema: ResolvedSchemaLike
        ): ResolvedParticle<ResolvedTerm> = when (rawPart) {
            is XSAll -> ResolvedAll(parent, rawPart, schema)
            is XSChoice -> ResolvedChoice(parent, rawPart, schema)
            is XSSequence -> ResolvedSequence(parent, rawPart, schema)
            is XSGroupRef -> ResolvedGroupRef(
                rawPart,
                schema
            )

            is XSGroupRef -> ResolvedGroupRef(rawPart, schema)
            is XSAny -> ResolvedAny(rawPart, schema)
            is XSLocalElement -> IResolvedElementUse(requireNotNull(parent), rawPart, schema)
            XSI_Particle.DUMMY -> error("Dummy cannot be resolved")
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