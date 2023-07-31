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
import io.github.pdvrieze.formats.xmlschema.resolved.particles.ResolvedParticle
import nl.adaptivity.xmlutil.QName
import kotlin.reflect.KClass

/**
 * Base interface for all group-like resolved types: all, seq, choice, group
 */
sealed interface ResolvedGroupParticle<out T : ResolvedTerm> : ResolvedComplexType.ResolvedDirectParticle<T> {
    override fun check(checkedTypes: MutableSet<QName>) = super.check(checkedTypes)

    companion object {

        operator fun invoke(
            parent: ResolvedParticleParent,
            term: XSComplexContent.XSIDerivationParticle,
            schema: ResolvedSchemaLike
        ): ResolvedGroupParticle<*> = when (term) {
            is XSAll -> ResolvedAll(parent, term, schema)
            is XSChoice -> ResolvedChoice(parent, term, schema)
            is XSSequence -> ResolvedSequence(parent, term, schema)
            is XSGroupRef -> ResolvedGroupRef(term, schema)
            else -> error("Unsupported particle")
        }

    }
}
