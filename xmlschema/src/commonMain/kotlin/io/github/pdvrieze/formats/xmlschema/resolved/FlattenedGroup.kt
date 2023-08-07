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

        override fun times(otherRange: AllNNIRange): All {
            return All(range * otherRange, particles)
        }
    }

    class Choice(range: AllNNIRange, particles: List<FlattenedParticle>) :
        FlattenedGroup(range) {
        override val particles: List<FlattenedParticle> = particles.sortedWith(particleComparator)
        override fun times(otherRange: AllNNIRange): Choice {
            return Choice(range * otherRange, particles)
        }
    }

    class Sequence(range: AllNNIRange, particles: List<FlattenedParticle>) :
        FlattenedGroup(range) {
        override val particles: List<FlattenedParticle> = particles

        override fun times(otherRange: AllNNIRange): Sequence {
            return Sequence(range*otherRange, particles)
        }

    }

}

sealed class FlattenedParticle(val range: AllNNIRange) {

    val maxOccurs get() = range.endInclusive
    val minOccurs get() = range.start

    abstract operator fun times(otherRange: AllNNIRange): FlattenedParticle



    class Term(range: AllNNIRange, val term: ResolvedBasicTerm) : FlattenedParticle(range) {

        override fun times(otherRange: AllNNIRange): Term {
            return Term(range*otherRange, term)
        }
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

        private val FlattenedGroup.kindKey: Int get() = when(this) {
            is FlattenedGroup.All -> 0
            is FlattenedGroup.Choice -> 1
            is FlattenedGroup.Sequence -> 2
        }

        private operator fun FlattenedGroup.compareTo(other: FlattenedGroup): Int {
            val k = kindKey - other.kindKey
            if (k!=0) return k
            for(i in 0 until minOf(particles.size, other.particles.size)) {
                val c = particleComparator.compare(particles[i], other.particles[i])
                if (c!=0) return c
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
