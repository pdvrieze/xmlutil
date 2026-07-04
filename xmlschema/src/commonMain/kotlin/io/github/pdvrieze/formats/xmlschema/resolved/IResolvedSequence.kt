/*
 * Copyright (c) 2023-2026.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.github.pdvrieze.formats.xmlschema.resolved

import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedModelGroup.Compositor
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.resolved.flattened.FlattenedEmptyGroup
import io.github.pdvrieze.formats.xmlschema.resolved.flattened.FlattenedGroup
import io.github.pdvrieze.formats.xmlschema.resolved.flattened.FlattenedParticle
import io.github.pdvrieze.formats.xmlschema.resolved.flattened.FlattenedSequence
import io.github.pdvrieze.formats.xmlschema.resolved.flattened.SiblingContextProvider
import io.github.pdvrieze.formats.xmlschema.types.AllNNIRange
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI

interface IResolvedSequence : ResolvedModelGroup {

    override val mdlParticles: List<ResolvedParticle<ResolvedTerm>>

    override val mdlCompositor: Compositor get() = Compositor.SEQUENCE

    override fun <R> visit(visitor: ResolvedTerm.Visitor<R>): R = visitor.visitSequence(this)

    context(checkHelper: CheckHelper)
    override fun flatten(
        range: AllNNIRange,
        siblingContext: SiblingContextProvider
    ): FlattenedParticle {

        val particles = mdlParticles.flatMap {
            val f = it.flatten(::isSiblingName)
            when {
                f is FlattenedSequence && f.range.isSimple -> f.particles
                f.maxOccurs == VAllNNI.ZERO -> emptyList()
                else -> listOf(f)
            }
        }

        // TODO move to this class
        context(siblingContext) {
            FlattenedGroup.checkSequence(particles)
        }

        return when {
            particles.isEmpty() -> FlattenedEmptyGroup
            particles.size == 1 -> when {
                checkHelper.version != SchemaVersion.V1_0 ->
                    particles.single() * range // multiply will be null if not valid

                range.isSimple -> particles.single()

                else -> null
            }

            particles.size == 1 && range.isSimple -> particles.single()
            else -> null
        } ?: FlattenedSequence(range, particles)
    }
}
