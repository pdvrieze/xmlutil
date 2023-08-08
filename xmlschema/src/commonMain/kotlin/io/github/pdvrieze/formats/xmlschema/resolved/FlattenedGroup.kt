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

import io.github.pdvrieze.formats.xmlschema.types.AllNNIRange
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.namespaceURI

sealed class FlattenedGroup(
    range: AllNNIRange,
) : FlattenedParticle(range) {

    abstract val particles: List<FlattenedParticle>

    class All(range: AllNNIRange, particles: List<FlattenedParticle>) :
        FlattenedGroup(range) {

        override val particles: List<FlattenedParticle> = particles.sortedWith(particleComparator)

        init {
            val seenNames = mutableSetOf<QName>()
            val seenWildcards = mutableListOf<ResolvedAny>()
            for (startElem in particles.flatMap { it.startingTerms() }) {
                when (startElem) {
                    is Element -> require(seenNames.add(startElem.term.mdlQName)) {
                            "Non-deterministic all group: all{${particles.joinToString()}}"
                        }

                    is Wildcard -> require(seenWildcards.none { it.intersects(startElem.term) }) {
                        "Non-deterministic all group: all${particles.joinToString()}"
                    }
                }
                // alls don't care about wildcards (we don't check overlaps)
            }
        }

        override fun startingTerms(): List<Term> {
            return particles.flatMap { it.startingTerms() }
        }

        override fun trailingTerms(): List<Term> {
            return particles.flatMap { it.trailingTerms() }
        }

        override fun times(otherRange: AllNNIRange): All {
            return All(range * otherRange, particles)
        }

        override fun toString(): String = particles.joinToString(prefix = "{", postfix = "}")
    }

    class Choice(range: AllNNIRange, particles: List<FlattenedParticle>) :
        FlattenedGroup(range) {

        init {
            val seenNames = mutableSetOf<QName>()
            val seenWildcards = mutableListOf<ResolvedAny>()
            for (startElem in particles.flatMap { it.startingTerms() }) {
                when (startElem) {
                    is Element -> require(seenNames.add(startElem.term.mdlQName)) {
                        "Non-deterministic choice group: choice(${particles.joinToString("| ")})"
                    }

                    is Wildcard -> require(seenWildcards.none { it.intersects(startElem.term) }) {
                        "Non-deterministic choice group: choice${particles.joinToString()}"
                    }
                }
            }
        }

        override val particles: List<FlattenedParticle> = particles.sortedWith(particleComparator)

        override fun startingTerms(): List<Term> {
            return particles.flatMap { it.startingTerms() }
        }

        override fun trailingTerms(): List<Term> {
            return particles.flatMap { it.trailingTerms() }
        }

        override fun times(otherRange: AllNNIRange): Choice {
            return Choice(range * otherRange, particles)
        }

        override fun toString(): String = particles.joinToString(separator = "| ", prefix = "(", postfix = ")")
    }

    class Sequence(range: AllNNIRange, override val particles: List<FlattenedParticle>) :
        FlattenedGroup(range) {

        init {
            var lastOptionals: MutableList<QName> = mutableListOf()
            var lastAnys: MutableList<ResolvedAny> = mutableListOf()
            for (p in particles) {
                for (startTerm in p.startingTerms()) {
                    when(startTerm) {
                        is Element -> {
                            val startName = startTerm.term.mdlQName
                            require(startName !in lastOptionals) {
                                "Non-deterministic sequence: sequence${particles.joinToString()}"
                            }
                            require(lastAnys.none { it.matches(startName) })
                        }
                        is Wildcard -> {
                            require(lastAnys.none { it.intersects(startTerm.term) }) {
                                "Non-deterministic choice group: choice${particles.joinToString()}"
                            }
                        }
                    }
                }



                lastOptionals = mutableListOf()
                lastAnys = mutableListOf()

                when {
                    p.isVariable -> for (e in p.trailingTerms()) {
                        when (e) {
                            is Wildcard -> lastAnys.add(e.term)
                            is Element -> lastOptionals.add(e.term.mdlQName)
                        }
                    }

                    else -> for (e in p.trailingTerms()) {
                        if (e.isVariable) when (e) {
                            is Wildcard -> lastAnys.add(e.term)
                            is Element -> lastOptionals.add(e.term.mdlQName)
                        }
                    }
                }

            }
        }

        override fun startingTerms(): List<Term> {
            return particles.firstOrNull()?.startingTerms() ?: emptyList()
        }

        override fun trailingTerms(): List<Term> {
            val result = mutableListOf<Term>()
            for (particle in particles.asReversed()) {
                result.addAll(particle.trailingTerms())
                if (!particle.isOptional) return result
            }
            return result
        }

        override fun times(otherRange: AllNNIRange): Sequence {
            return Sequence(range * otherRange, particles)
        }

        override fun toString(): String = particles.joinToString(prefix = "(", postfix = ")")

    }

}

sealed class FlattenedParticle(val range: AllNNIRange) {

    val maxOccurs get() = range.endInclusive
    val minOccurs get() = range.start

    val isOptional: Boolean get() = minOccurs == VAllNNI.ZERO
    val isVariable: Boolean get() = minOccurs != maxOccurs

    abstract operator fun times(otherRange: AllNNIRange): FlattenedParticle

    abstract fun startingTerms(): List<Term>
    abstract fun trailingTerms(): List<Term>

    abstract class Term(range: AllNNIRange) : FlattenedParticle(range) {
        abstract val term: ResolvedBasicTerm

        companion object {
            operator fun invoke(range: AllNNIRange, term: ResolvedBasicTerm): Term = when (term) {
                is ResolvedElement -> Element(range, term)
                is ResolvedAny -> Wildcard(range, term)
            }
        }
    }

    class Element(range: AllNNIRange, override val term: ResolvedElement) : Term(range) {
        override fun startingTerms(): List<Element> {
            return listOf(this)
        }

        override fun trailingTerms(): List<Element> = listOf(this)

        override fun times(otherRange: AllNNIRange): Element {
            return Element(range * otherRange, term)
        }

        override fun toString(): String = term.mdlQName.toString()
    }

    class Wildcard(range: AllNNIRange, override val term: ResolvedAny) : Term(range) {

        override fun startingTerms(): List<Wildcard> {
            return listOf(this)
        }

        override fun trailingTerms(): List<Wildcard> = listOf(this)

        override fun times(otherRange: AllNNIRange): Wildcard {
            return Wildcard(range * otherRange, term)
        }

        override fun toString(): String = "*"
    }

    companion object {
        val particleComparator: Comparator<in FlattenedParticle> = Comparator { a, b ->
            when (a) {
                is Term -> when (b) {
                    is Term -> when (val at = a.term) {
                        is ResolvedAny -> when (b.term) {
                            is ResolvedAny -> 0
                            is ResolvedElement -> 1 // Any after element
                        }

                        is ResolvedElement -> when (val bt = b.term) {
                            is ResolvedAny -> 0
                            is ResolvedElement -> at.mdlQName.compareTo(bt.mdlQName)
                        }
                    }

                    is FlattenedGroup -> -1 // groups after terms
                }

                is FlattenedGroup -> when (b) {
                    is Term -> 1
                    is FlattenedGroup -> a.compareTo(b)
                }
            }
        }

        private val FlattenedGroup.kindKey: Int
            get() = when (this) {
                is FlattenedGroup.All -> 0
                is FlattenedGroup.Choice -> 1
                is FlattenedGroup.Sequence -> 2
            }

        private operator fun FlattenedGroup.compareTo(other: FlattenedGroup): Int {
            val k = kindKey - other.kindKey
            if (k != 0) return k
            for (i in 0 until minOf(particles.size, other.particles.size)) {
                val c = particleComparator.compare(particles[i], other.particles[i])
                if (c != 0) return c
            }
            return particles.size - other.particles.size
        }

        private operator fun QName.compareTo(other: QName): Int {
            return when (val l = localPart.compareTo(other.localPart)) {
                0 -> namespaceURI.compareTo(other.namespaceURI)
                else -> l
            }
        }
    }


}
