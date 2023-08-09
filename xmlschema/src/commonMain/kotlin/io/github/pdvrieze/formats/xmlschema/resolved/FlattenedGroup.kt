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
import nl.adaptivity.xmlutil.isEquivalent
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.namespaceURI
import kotlin.jvm.JvmStatic

sealed class FlattenedGroup(
    range: AllNNIRange,
) : FlattenedParticle(range) {

    object EMPTY : Sequence(VAllNNI.ZERO..VAllNNI.ZERO, emptyList(), false) {
        override fun toString(): String = "()"
    }

    abstract val particles: List<FlattenedParticle>

    class All(range: AllNNIRange, particles: List<FlattenedParticle>) :
        FlattenedGroup(range) {

        override val particles: List<FlattenedParticle> = particles.sortedWith(particleComparator)

        init {
            require(particles.size > 1)

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

        override fun effectiveTotalRange(): AllNNIRange {
            return particles.asSequence()
                .map { it.effectiveTotalRange() }
                .reduce { l, r -> l + r }
                .times(range)
        }

        override fun restricts(reference: FlattenedParticle): Boolean {
            // case of 0 range should never happen
            return when(reference) {
                is Sequence -> restricts(reference.particles.first())
                is Choice -> {
                    reference.particles.any { this.restricts(it * reference.range) }
                    // Take the easy way for now (without creating all potential paths
/*
                    val elemRange = particles.asSequence().map { range }.reduce { l, r -> l + r }.times(range)
                    val choiceRange = particles.asSequence().map { range }.reduce { l, r -> l + r }.times(range)
                    if (! elemRange.contains(choiceRange)) return false
*/
                }
                is All -> {
                    reference.range.contains(range) &&
                            particles.all { p->
                                reference.particles.any { p.restricts(it) }
                            }
                }
                is Wildcard -> {
                    reference.effectiveTotalRange().contains(effectiveTotalRange()) &&
                            particles.all { it.restricts(reference) }
                }

                else -> false
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

        override fun toString(): String = particles.joinToString(prefix = "{", postfix = range.toPostfix("}"))
    }

    class Choice(range: AllNNIRange, particles: List<FlattenedParticle>) :
        FlattenedGroup(range) {

        init {
            require(particles.size > 1)

            val seenNames = mutableSetOf<QName>()
            val seenWildcards = mutableListOf<ResolvedAny>()
            for (startElem in particles.flatMap { it.startingTerms() }) {
                when (startElem) {
                    is Element -> require(seenNames.add(startElem.term.mdlQName)) {
                        "Non-deterministic choice group: choice(${particles.joinToString(" | ")})"
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

        override fun effectiveTotalRange(): AllNNIRange {
            return particles.asSequence()
                .map { it.effectiveTotalRange() }
                .reduce { l, r ->
                    AllNNIRange(minOf(l.start, r.start), maxOf(l.endInclusive, r.endInclusive))
                }.times(range)
        }

        override fun restricts(reference: FlattenedParticle): Boolean {
            // case of 0 range should never happen
            return when(reference) {
                is Sequence -> restricts(reference.particles.first())
                is Choice -> {
                    reference.range.contains(range) &&
                        particles.all { p -> reference.particles.any { p.restricts(it) } }
                }

                is All -> false // can not happen

                is Wildcard -> {
                    reference.effectiveTotalRange().contains(effectiveTotalRange()) &&
                            particles.all { it.restricts(reference) }
                }

                else -> false
            }
        }

        override fun times(otherRange: AllNNIRange): Choice {
            return Choice(range * otherRange, particles)
        }

        override fun toString(): String = particles.joinToString(separator = "| ", prefix = "(", postfix = range.toPostfix(")"))
    }

    open class Sequence internal constructor(range: AllNNIRange, final override val particles: List<FlattenedParticle>, marker: Boolean) :
        FlattenedGroup(range) {

        init {
            var lastOptionals: MutableList<QName> = mutableListOf()
            var lastAnys: MutableList<ResolvedAny> = mutableListOf()
            for (p in particles) {
                for (startTerm in p.startingTerms()) {
                    when (startTerm) {
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
                            require(lastOptionals.none { startTerm.term.matches(it) }) {
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

        override fun effectiveTotalRange(): AllNNIRange {
            return particles.asSequence()
                .map { it.effectiveTotalRange() }
                .reduce { l, r -> l + r }
                .times(range)
        }

        override fun restricts(reference: FlattenedParticle): Boolean {
            return when(reference) {
                is Sequence -> restrictsSequence(reference)
                is Choice -> {
                    reference.particles.any { this.restricts(it * reference.range) }
                }

                is All -> {
                    val refCpy = reference.particles.toMutableList<FlattenedParticle?>()
                    for (e in particles) {
                        val matchIdx = refCpy.indexOfFirst { it!=null && e.restricts(it) }
                        if (matchIdx < 0) return false
                        refCpy[matchIdx] = null
                    }
                    false
                }

                is Wildcard -> {
                    reference.effectiveTotalRange().contains(effectiveTotalRange()) &&
                            particles.all { it.restricts(reference) }
                }

                else -> false
            }
        }

        private fun restrictsSequence(reference: Sequence): Boolean {
            val refIt = reference.particles.iterator()
            if (! refIt.hasNext()) return false
            var currentParticle: FlattenedParticle = refIt.next()
            var currentConsumed: VAllNNI = VAllNNI.ZERO
            for(p in particles) {
                while (currentConsumed >= currentParticle.maxOccurs || ! p.restricts(currentParticle)) {
                    // We can't go to the next particle
                    if (currentConsumed < currentParticle.minOccurs) return false
                    if (! refIt.hasNext()) return false

                    currentParticle = refIt.next()
                    currentConsumed = VAllNNI.ZERO
                }
                currentConsumed = currentConsumed + p.maxOccurs
            }
            // Tail that isn't optional
            if (currentConsumed < currentParticle.minOccurs) return false

            // more tail
            while (refIt.hasNext()) {
                if (! refIt.next().isOptional) return false
            }

            return true
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
            return Sequence(range * otherRange, particles, false)
        }

        override fun toString(): String = particles.joinToString(prefix = "(", postfix = range.toPostfix(")"))

    }

    companion object {
        @JvmStatic
        fun Sequence(range: AllNNIRange, particles: List<FlattenedParticle>): FlattenedParticle = when {
            particles.isEmpty() -> EMPTY
            particles.size == 1 -> particles.single()
            else -> Sequence(range, particles, false)
        }
    }

}

sealed class FlattenedParticle(val range: AllNNIRange) {

    val maxOccurs get() = range.endInclusive
    val minOccurs get() = range.start

    val isOptional: Boolean get() = minOccurs == VAllNNI.ZERO
    val isVariable: Boolean get() = minOccurs != maxOccurs

    abstract fun effectiveTotalRange(): AllNNIRange

    abstract operator fun times(otherRange: AllNNIRange): FlattenedParticle

    abstract fun startingTerms(): List<Term>
    abstract fun trailingTerms(): List<Term>

    abstract fun restricts(reference: FlattenedParticle): Boolean

    abstract class Term(range: AllNNIRange) : FlattenedParticle(range) {
        abstract val term: ResolvedBasicTerm

        override fun effectiveTotalRange(): AllNNIRange = range

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

        override fun restricts(reference: FlattenedParticle): Boolean = when (reference) {
            is Element -> when {
                ! reference.range.contains(range) -> false
                else -> reference.term.mdlQName.isEquivalent(term.mdlQName)
            }

            is Wildcard -> reference.range.contains(range) && reference.term.matches(term.mdlQName)

            is FlattenedGroup.Sequence -> restrictsSequence(reference)

            is FlattenedGroup.All -> reference.particles.all { it.isOptional || this.restricts(it * reference.range) }

            is FlattenedGroup.Choice -> reference.particles.any { restricts(it * reference.range) }

            else -> false
        }

        private fun restrictsSequence(sequence: FlattenedGroup.Sequence): Boolean {
            val it = sequence.particles.iterator()
            var match : FlattenedParticle? = null
            while (match == null && it.hasNext()) {
                match = it.next().takeIf { this.restricts(it) }
            }
            if (match==null) return false
            while(it.hasNext()) {
                if (! it.next().isOptional) return false
            }
            return true
        }

        override fun times(otherRange: AllNNIRange): Element {
            return Element(range * otherRange, term)
        }

        override fun toString(): String = range.toPostfix(term.mdlQName.toString())
    }

    class Wildcard(range: AllNNIRange, override val term: ResolvedAny) : Term(range) {

        override fun startingTerms(): List<Wildcard> {
            return listOf(this)
        }

        override fun trailingTerms(): List<Wildcard> = listOf(this)

        override fun restricts(reference: FlattenedParticle): Boolean {
            if (!reference.range.contains(range)) return false
            return when (reference) {
                is FlattenedGroup -> when {
                    !reference.effectiveTotalRange().contains(range) -> false

                    // duplicate checking for child range, but we don't want the complexity
                    else -> reference.particles.all { restricts(it) }
                }

                is Element -> term.matches(reference.term.mdlQName)

                is Wildcard -> reference.term.mdlNamespaceConstraint.contains(term.mdlNamespaceConstraint)
                else -> error("Unsupported particle kind: $reference")
            }
        }

        override fun times(otherRange: AllNNIRange): Wildcard {
            return Wildcard(range * otherRange, term)
        }

        override fun toString(): String = range.toPostfix("<*>")
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

        internal fun AllNNIRange.toPostfix(prefix: String=""): String = when {
            endInclusive == VAllNNI.UNBOUNDED -> when (start) {
                VAllNNI.ZERO -> prefix + '*'
                VAllNNI.ONE -> prefix + '+'
                else -> prefix + '[' + start.toULong() + "+]"
            }

            endInclusive > VAllNNI.ONE -> prefix +'[' + start.toULong()+ ".."+ (endInclusive as VAllNNI.Value).toULong() +']'
            // end inclusive 0 should not happen
            start == VAllNNI.ZERO -> prefix+'?'
            else -> prefix // both are 1
        }
    }


}