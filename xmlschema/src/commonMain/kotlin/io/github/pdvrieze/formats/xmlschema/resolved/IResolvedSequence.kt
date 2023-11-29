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

import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedModelGroup.Compositor
import io.github.pdvrieze.formats.xmlschema.types.AllNNIRange
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI

interface IResolvedSequence : ResolvedModelGroup {

    override val mdlParticles: List<ResolvedParticle<ResolvedTerm>>

    override val mdlCompositor: Compositor get() = Compositor.SEQUENCE

    override fun <R> visit(visitor: ResolvedTerm.Visitor<R>): R = visitor.visitSequence(this)

    override fun flatten(
        range: AllNNIRange,
        nameContext: ContextT,
        schema: ResolvedSchemaLike
    ): FlattenedParticle {

        val particles = mdlParticles.flatMap {
            val f = it.flatten(nameContext, schema)
            when {
                f is FlattenedGroup.Sequence && f.range.isSimple -> f.particles
                f.maxOccurs == VAllNNI.ZERO -> emptyList()
                else -> listOf(f)
            }
        }

        val names = nameContext

        // TODO move to this class
        FlattenedGroup.checkSequence(particles, names, schema)

        return when {
            particles.isEmpty() -> FlattenedGroup.EMPTY
            particles.size == 1 -> when {
                schema.version != SchemaVersion.V1_0 ->
                    particles.single() * range // multiply will be null if not valid

                range.isSimple -> particles.single()

                else -> null
            }

            particles.size == 1 && range.isSimple -> particles.single()
            else -> null
        } ?: FlattenedGroup.Sequence(range, particles)
    }
}
