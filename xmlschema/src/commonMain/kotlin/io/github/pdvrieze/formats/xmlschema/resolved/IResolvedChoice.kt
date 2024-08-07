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
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.AllNNIRange
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import nl.adaptivity.xmlutil.QName

interface IResolvedChoice : ResolvedModelGroup {

    override val mdlParticles: List<ResolvedParticle<ResolvedTerm>>
    override val mdlCompositor: Compositor get() = Compositor.CHOICE


    override fun <R> visit(visitor: ResolvedTerm.Visitor<R>): R {
        return visitor.visitChoice(this)
    }

    override fun flatten(
        range: AllNNIRange,
        isSiblingName: (QName) -> Boolean,
        checkHelper: CheckHelper
    ): FlattenedParticle {
        val seenNames = mutableSetOf<QName>()
        val seenWildcards = mutableListOf<ResolvedAny>()

        val particles = mutableListOf<FlattenedParticle>()
        for (p in mdlParticles) {
            val f = p.flatten(::isSiblingName, checkHelper)

            when {
                f is FlattenedGroup.Choice && f.range.isSimple -> particles.addAll(f.particles)
                f.maxOccurs != VAllNNI.ZERO -> particles.add(f)
            }

            for (startElem in f.startingTerms()) {
                when (startElem) {
                    is FlattenedParticle.Element -> {
                        require(seenNames.add(startElem.term.mdlQName)) {
                            "Non-deterministic choice group: choice({${mdlParticles.joinToString()}})"
                        }
                    }

                    is FlattenedParticle.Wildcard -> {
                        if (startElem.term.mdlNamespaceConstraint.namespaces.singleOrNull()?.value?.isNotEmpty() ?: true) {
                            for (wc in seenWildcards) {
                                require(! wc.intersects(startElem.term, isSiblingName, checkHelper.schema)) {
                                    "Non-deterministic choice group (conflicting wildcards): $wc and $startElem in choice(${mdlParticles.joinToString()})"
                                }
                            }
                            seenWildcards.add(startElem.term)
                        }
                    }
                }

            }

        }

        return when {
            particles.isEmpty() -> FlattenedGroup.EMPTY
            particles.size == 1 -> when {
                checkHelper.version != SchemaVersion.V1_0 ->
                    particles.single() * range // multiply will be null if not valid

                range.isSimple -> particles.single()

                else -> null
            }

            particles.size == 1 && range.isSimple -> particles.single()
            else -> null
        } ?: FlattenedGroup.Choice(range, particles, checkHelper.version)
    }

    override fun checkTerm(checkHelper: CheckHelper) {
        super.checkTerm(checkHelper)
        // Trigger flatten check
        flatten(checkHelper)
    }
}
