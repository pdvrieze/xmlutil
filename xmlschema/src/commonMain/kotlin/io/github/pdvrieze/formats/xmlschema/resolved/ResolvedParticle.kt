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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.model.*
import io.github.pdvrieze.formats.xmlschema.types.T_Particle


fun ResolvedGroupParticle(
    parent: ResolvedComplexType,
    term: XSComplexContent.XSIDerivationParticle,
    schema: ResolvedSchemaLike
): ResolvedGroupParticle<*> = when (term) {
    is XSAll -> ResolvedAll(parent, term, schema)
    is XSChoice -> ResolvedChoice(parent, term, schema)
    is XSSequence -> ResolvedSequence(parent, term, schema)
    is XSGroupRef -> ResolvedGroupRef(term, schema)
    else -> error("Unsupported particle")
}


fun ResolvedParticle(
    parent: ResolvedComplexType?,
    rawPart: XSI_Particle,
    schema: ResolvedSchemaLike
): ResolvedParticle<*> = when (rawPart) {
    is XSExplicitGroup -> ResolvedExplicitGroup(parent, rawPart, schema)
    is XSGroupRefParticle -> ResolvedGroupRefParticle(rawPart, schema)
    is XSGroupRef -> ResolvedGroupRef(rawPart, schema)
    is XSChoice -> ResolvedChoice(parent, rawPart, schema)
    is XSAny -> ResolvedAny(rawPart, schema)
    is XSLocalElement -> ResolvedLocalElement(requireNotNull(parent), rawPart, schema)
    is XSSequence -> ResolvedSequence(parent, rawPart, schema)
    XSI_Particle.DUMMY ->error("Dummy cannot be resolved")
}

fun ResolvedParticle(
    parent: ResolvedComplexType?,
    rawPart: XSI_NestedParticle,
    schema: ResolvedSchemaLike
): ResolvedParticle<ResolvedChoiceSeqTerm> = when (rawPart) {
    is XSChoice -> ResolvedChoice(parent, rawPart, schema)
    is XSAny -> ResolvedAny(rawPart, schema)
    is XSGroupRefParticle -> ResolvedGroupRefParticle(rawPart, schema)
    is XSLocalElement -> ResolvedLocalElement(requireNotNull(parent), rawPart, schema)
    is XSSequence -> ResolvedSequence(parent, rawPart, schema)
}

fun ResolvedParticle(
    parent: ResolvedComplexType?,
    rawPart: XSI_AllParticle,
    schema: ResolvedSchemaLike
): ResolvedParticle<ResolvedAllTerm> = when (rawPart) {
    is XSAny -> ResolvedAny(rawPart, schema)
    is XSGroupRefParticle -> ResolvedGroupRefParticle(rawPart, schema)
    is XSLocalElement -> ResolvedLocalElement(requireNotNull(parent), rawPart, schema)
}

sealed interface ResolvedTerm : Term {
    fun collectConstraints(collector: MutableList<ResolvedIdentityConstraint>)
}

sealed interface ResolvedChoiceSeqTerm: ResolvedTerm, ChoiceSeqTerm

sealed interface ResolvedDerivationTerm: ResolvedChoiceSeqTerm, DerivationTerm

sealed interface ResolvedAllTerm: ResolvedDerivationTerm, AllTerm

sealed interface ResolvedBasicTerm: ResolvedAllTerm, ParticleModel.BasicTerm

interface ResolvedParticle<out T : ResolvedTerm> : ResolvedPart, ResolvedAnnotated, T_Particle, ParticleModel<T> {
    override val rawPart: XSI_Particle

}

sealed interface ResolvedGroupParticle<out T : ResolvedTerm> : ResolvedParticle<T>, ResolvedTerm, ParticleModel<T>
