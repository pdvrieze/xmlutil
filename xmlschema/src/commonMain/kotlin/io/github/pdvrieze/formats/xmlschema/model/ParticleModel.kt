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

package io.github.pdvrieze.formats.xmlschema.model

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VUnsignedLong
import io.github.pdvrieze.formats.xmlschema.types.AllNNIRange
import io.github.pdvrieze.formats.xmlschema.types.T_AllNNI

interface ParticleModel<out T : Term> : IAnnotated {

    val mdlMinOccurs: VNonNegativeInteger
    val mdlMaxOccurs: T_AllNNI
    val mdlTerm: T

    val effectiveTotalRange: AllNNIRange
        get() = when (val t = mdlTerm) {
            is AllModel,
            is SequenceModel<*> -> {
                var min: VNonNegativeInteger = VUnsignedLong.ZERO
                var max: T_AllNNI = T_AllNNI.Value(0u)
                for (particle in (t as GroupLikeTermBase).mdlParticles) {
                    val r = particle.effectiveTotalRange
                    min += r.start
                    max += r.endInclusive
                }
                AllNNIRange(mdlMinOccurs * min, mdlMaxOccurs * max)
            }

            is ChoiceModel -> {
                var minMin: VNonNegativeInteger = VUnsignedLong(ULong.MAX_VALUE)
                var maxMax: T_AllNNI = T_AllNNI.Value(0u)
                for (particle in t.mdlParticles) {
                    val r = particle.effectiveTotalRange
                    minMin = minMin.coerceAtMost(r.start)
                    maxMax = maxMax.coerceAtLeast(r.endInclusive)
                }
                minMin = minMin.coerceAtMost(maxMax)
                AllNNIRange(mdlMinOccurs * minMin, mdlMaxOccurs * maxMax)

            }

            else -> AllNNIRange(T_AllNNI.Value(mdlMinOccurs), mdlMaxOccurs)
        }

    fun mdlIsEmptiable(): Boolean {
        return effectiveTotalRange.start.toUInt() == 0u
    }

    interface BasicTerm : AllMember
}

interface Term
