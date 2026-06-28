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

package io.github.pdvrieze.formats.xmlschema.resolved.flattened

import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedAny
import io.github.pdvrieze.formats.xmlschema.resolved.SchemaVersion
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.AllNNIRange
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import nl.adaptivity.xmlutil.QName

sealed class FlattenedGroup(
    range: AllNNIRange,
) : FlattenedParticle(range) {

    override val isEmptiable: Boolean
        get() = minOccurs == VAllNNI.ZERO || effectiveTotalRange().start == VAllNNI.ZERO

    // Implements recurse (seq-seq or all-all)
    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    protected fun restrictsRecurse(base: FlattenedGroup): Boolean = when (checkHelper.version) {
        SchemaVersion.V1_0 -> restrictsRecurse10(base)
        else -> restrictsRecurse11(base)
    }

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    private fun restrictsRecurse10(base: FlattenedGroup): Boolean {
        // 1
        if (!base.range.contains(range)) return false

        val baseIt = base.particles.iterator()

        for (p in particles) {
            // particles size should always be more than 1
            while (true) {
                if (!baseIt.hasNext()) return false
                val basePart = baseIt.next()

                // 2.1
                if (with(siblingContext) {
                        p.restricts(basePart)
                    }) break

                // otherwise skip 2.2
                if (!basePart.isEmptiable) return false
            }
        }
        while (baseIt.hasNext()) {
            if (!baseIt.next().isEmptiable) return false
        }

        return true
    }

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    private fun restrictsRecurse11(base: FlattenedGroup): Boolean {
        return base.removePrefix(this).isEmptiable
    }

    // implements NSRecurse-CheckCardinality
    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun restrictsWildcard(
        base: FlattenedWildcard
    ): Boolean {
        // NSRecurse-CheckCardinality 2
        if (!base.effectiveTotalRange().contains(effectiveTotalRange())) return false

        // NSRecurse-CheckCardinality 1 // ignore count here as it will not match
        return particles.all {
            with(siblingContext) {
                it.single().restricts(base.single())
            }
        }
    }

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun removeFromWildcard(
        reference: FlattenedWildcard
    ): RemovalResult {
        if (particles.any { !it.single().restricts(reference.single()) }) return RemovalResult.NoMatch
        return RemovalResult(reference - effectiveTotalRange()) // this should already cause range checking
    }

    abstract val particles: List<FlattenedParticle>

    companion object {

        // TODO move to IResolvedSequence
        context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
        internal fun checkSequence(
            particles: List<FlattenedParticle>
        ) {
            var lastOptionals: MutableList<QName> = mutableListOf()
            var lastAnys: MutableList<ResolvedAny> = mutableListOf()
            for (p in particles) {
                for (startTerm in p.startingTerms()) {
                    when (startTerm) {
                        is FlattenedElement -> {
                            val startName = startTerm.term.mdlQName
                            require(startName !in lastOptionals) {
                                "Non-deterministic sequence: sequence${particles.joinToString()}"
                            }
                            if (checkHelper.version == SchemaVersion.V1_0) {
                                // In version 1.1 resolving prioritises explicit elements, wildcards can omit
                                require(lastAnys.none { it.matches(startName, siblingContext, checkHelper.schema) }) {
                                    "Ambiguous sequence $startName - $lastAnys"
                                }
                            }
                        }

                        is FlattenedWildcard -> {
                            require(lastAnys.none {
                                it.intersects(startTerm.term, siblingContext, checkHelper.schema)
                            }) {
                                "Non-deterministic sequence group: ${particles.joinToString()}"
                            }
                            if (checkHelper.version == SchemaVersion.V1_0) {
                                require(lastOptionals.none {
                                    startTerm.term.matches(it, siblingContext, checkHelper.schema)
                                }) {
                                    "Non-deterministic sequence group (wildcards): ${particles.joinToString()}"
                                }
                            }
                        }
                    }
                }



                lastOptionals = mutableListOf()
                lastAnys = mutableListOf()

                when {
                    p.isEmptiable && p.isVariable -> {
                        for (e in p.trailingTerms()) {
                            if (e.isVariable) {
                                when (e) {
                                    is FlattenedWildcard -> lastAnys.add(e.term)
                                    is FlattenedElement -> lastOptionals.add(e.term.mdlQName)
                                }
                            }
                        }
                        for (e in p.startingTerms()) {
                            when (e) {
                                is FlattenedWildcard -> lastAnys.add(e.term)
                                is FlattenedElement -> lastOptionals.add(e.term.mdlQName)
                            }
                        }
                    }

                    else -> for (e in p.trailingTerms()) {
                        if (e.isVariable) when (e) {
                            is FlattenedWildcard -> lastAnys.add(e.term)
                            is FlattenedElement -> lastOptionals.add(e.term.mdlQName)
                        }
                    }
                }

            }
        }


    }

}

