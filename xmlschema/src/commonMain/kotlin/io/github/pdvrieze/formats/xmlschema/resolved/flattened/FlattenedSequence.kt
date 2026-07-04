/*
 * Copyright (c) 2026.
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

import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSchemaLike
import io.github.pdvrieze.formats.xmlschema.resolved.SchemaVersion
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.AllNNIRange
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import io.github.pdvrieze.formats.xmlschema.types.isContentEqual

open class FlattenedSequence internal constructor(
    range: AllNNIRange,
    final override val particles: List<FlattenedParticle>
) : FlattenedGroup(range) {

    override fun effectiveTotalRange(): AllNNIRange {
        return particles.asSequence()
            .map { it.effectiveTotalRange() }
            .reduce { l, r -> l + r }
            .let { it.start * range.start..it.endInclusive * range.endInclusive }
    }

    override fun startingTerms(): List<FlattenedTerm> {
        val result = mutableListOf<FlattenedTerm>()
        for (particle in particles) {
            result.addAll(particle.startingTerms())
            if (!particle.isEmptiable) return result
        }
        return result
    }

    override fun trailingTerms(): List<FlattenedTerm> {
        val result = mutableListOf<FlattenedTerm>()
        for (particle in particles.asReversed()) {
            result.addAll(particle.trailingTerms())
            if (!particle.isEmptiable) return result
        }
        return result
    }

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun isRestrictedBy(
        other: FlattenedParticle
    ): Boolean = other.restrictsSequence(this)

    context(siblingContext: SiblingContextProvider)
    override fun isExtendedBy(other: FlattenedParticle, schema: ResolvedSchemaLike): Boolean {
        return other.extendsSequence(this, schema)
    }

    context(siblingContext: SiblingContextProvider)
    override fun extendsElement(base: FlattenedElement, schema: ResolvedSchemaLike): Boolean {
        // 3.9.6.2 step 2
        return (range.isSimple && particles.isNotEmpty() && particles.first().extends(base, schema)) ||
                    extendsSequence(FlattenedSequence(AllNNIRange.SINGLERANGE, listOf(base)), schema) ||
                (schema.version != SchemaVersion.V1_0 && extendsSequence(
                    FlattenedSequence(base.range, listOf(base.single())),
                    schema
                ))
    }

    context(siblingContext: SiblingContextProvider)
    override fun extendsWildcard(base: FlattenedWildcard, schema: ResolvedSchemaLike): Boolean {
        // 3.9.6.2 step 2
        return range.isSimple && particles.isNotEmpty() && particles.first().extends(base, schema)
    }

    context(siblingContext: SiblingContextProvider)
    override fun extendsAll(base: FlattenedAll, schema: ResolvedSchemaLike): Boolean {
        // 3.9.6.2 step 2
        return range.isSimple && particles.isNotEmpty() && particles.first().extends(base, schema)
    }

    context(siblingContext: SiblingContextProvider)
    override fun extendsChoice(base: FlattenedChoice, schema: ResolvedSchemaLike): Boolean {
        // 3.9.6.2 step 2
        return range.isSimple && particles.isNotEmpty() && particles.first().extends(base, schema)
    }

    context(siblingContext: SiblingContextProvider)
    override fun extendsSequence(
        base: FlattenedSequence,
        schema: ResolvedSchemaLike
    ): Boolean {
        // 3.9.6.2
        // part 1
        if (particles.size >= base.particles.size &&
            particles.subList(0, base.particles.size).isContentEqual(base.particles)
        ) {
            return range == base.range
        }
        // part 2
        return range.isSimple && particles.first().extends(base, schema)

    }

    // Restrict recurseUnordered
    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun restrictsAll(base: FlattenedAll): Boolean {
        if (!base.range.contains(range)) return false // 1

        val unprocessed = base.particles.toMutableList<FlattenedParticle?>() // 2.1

        val pendingChoiceParticles = mutableListOf<FlattenedChoice>()
        for (p in particles) { // 2.2
            val matchIdx = unprocessed
                .indexOfFirst { it != null && p.restricts(it) }

            when {
                matchIdx >= 0 -> {
                    val newMatch = (unprocessed[matchIdx]!! - p.range)?.takeIf { it.maxOccurs > VAllNNI.ZERO }
                    unprocessed[matchIdx] = newMatch // 2.1
                }

                p is FlattenedChoice -> pendingChoiceParticles.add(p)
                else -> return false
            }
        }

        for (bp in unprocessed) { // 2.3
            if (bp != null && !bp.isEmptiable) return false
        }
//
//            val remaining = All(range, unprocessed.filterNotNull())
//
//            val choices = pendingChoiceParticles
//                .flatMap { ch -> ch.particles.map { (it * ch.range) ?: return false } }
//                .groupBy { toString() }
//                .map { (_, parts) -> parts.reduce { l, r -> l + r } }

        // XXX strange unused variables
        for (choice in pendingChoiceParticles) {

            for (e in choice.particles) {
                if (unprocessed.none { it != null && e.restricts(it) }) return false
            }
        }

        return true
    }

    /**
     * MapAndSum
     */
    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun restrictsChoice(base: FlattenedChoice): Boolean {
        return restrictsChoice_1_0(base) ||
                (checkHelper.version == SchemaVersion.V1_1 && FlattenedChoice(
                    AllNNIRange.SINGLERANGE,
                    listOf(this)
                ).restrictsChoice(base))
    }

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    private fun restrictsChoice_1_0(
        base: FlattenedChoice
    ): Boolean {
        // MapAndSum 2
        val partSize = VAllNNI.Value(particles.size.toUInt())
        if (!base.range.contains((minOccurs * partSize)..(maxOccurs * partSize))) return false

        val minValues = Array(base.particles.size) { VAllNNI.ZERO }
        val maxValues = Array<VAllNNI>(base.particles.size) { VAllNNI.ZERO }

        // TODO implement "unfolding"
        for (p in particles) {
            val matchIdx = base.particles.indexOfFirst {
                p.single().restricts(it.single())
            }
            if (matchIdx < 0) return false
            val newConsumed = maxValues[matchIdx] + (p.maxOccurs * maxOccurs)
            val match = base.particles[matchIdx]
            if (newConsumed > (match.maxOccurs * base.maxOccurs)) return false // matches should be disjunct
            maxValues[matchIdx] = newConsumed
            minValues[matchIdx] += (p.minOccurs * minOccurs)
        }

        for (i in base.particles.indices) { // if consumed (and therefore maxValues>0) it must be within the range
            if (maxValues[i] > VAllNNI.ZERO) {
                // This is more restrictive than needed, and can cause failures with open ranges
                //                    val collapsedRange = base.particles[i].range * base.range
                val collapsedRange =
                    base.particles[i].let { (minOccurs * it.minOccurs)..(maxOccurs * it.maxOccurs) }
                if (!collapsedRange.contains(minValues[i]..maxValues[i])) return false
            }
        }

        return true
    }

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun removeFromChoice(
        reference: FlattenedChoice
    ): RemovalResult {
        // try to match the sequence to a single element in the sequence
        if (reference.maxOccurs > VAllNNI.ONE) {
            val reduced = reference.particles.firstNotNullOfOrNull {
                it.removePrefix(this@FlattenedSequence).takeIf { it !is RemovalResult.NoMatch }
            }
            if (reduced != null) {
                val tail = reference - AllNNIRange.SINGLERANGE
                return when (reduced) {
                    RemovalResult.FullMatch -> tail?.let { RemovalResult(it) } ?: RemovalResult.FullMatch
                    RemovalResult.NoMatch -> RemovalResult.NoMatch
                    is RemovalResult.PrefixMatch -> when (tail) {
                        null -> reduced
                        else -> RemovalResult(FlattenedSequence(AllNNIRange.SINGLERANGE, listOf(reduced.suffix, tail)))
                    }
                }
            }
        } else {
            val reduced = reference.particles.firstNotNullOfOrNull {
                it.removePrefix(this@FlattenedSequence)
            }
            if (reduced != null) return reduced
        }

        if (reference.maxOccurs <= VAllNNI.ONE) return RemovalResult.NoMatch

        var reduced: FlattenedParticle = reference
        for (p in particles) {
            reduced = when (val r = reduced.removePrefix(p)) {
                RemovalResult.FullMatch -> FlattenedEmptyGroup
                RemovalResult.NoMatch -> return r
                is RemovalResult.PrefixMatch -> r.suffix
            }
        }
        return RemovalResult(reduced)
    }

    context(checkHelper: CheckHelper, context: SiblingContextProvider)
    override fun restrictsSequence(base: FlattenedSequence): Boolean {
        return restrictsRecurse(base)
    }

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun removePrefix(prefixParticle: FlattenedParticle): RemovalResult {
        return prefixParticle.removeFromSequence(this)
    }

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun removeFromSequence(reference: FlattenedSequence): RemovalResult {
        if (reference.maxOccurs > VAllNNI.ONE) {
            val singleReduction = single().removeFromSequence(reference.single())
            if (singleReduction.isEmptiable) {
                // The sequences "match"
                return RemovalResult(reference - range)
            }
            val head = FlattenedSequence(reference.minOccurs..VAllNNI.ONE, reference.particles)
            val tail = (reference - AllNNIRange.SINGLERANGE)!!

            return removeFromSequence(head).map { reducedHead ->
                FlattenedSequence(AllNNIRange.SINGLERANGE, listOf(reducedHead, tail))
            }

            //base is optional or simple
        }

        if (maxOccurs > VAllNNI.ONE) {
            return FlattenedSequence(AllNNIRange.SINGLERANGE, listOf(this))
                .removeFromSequence(reference)
        }

        if (minOccurs == VAllNNI.ZERO && reference.effectiveTotalRange().start != VAllNNI.ZERO) return RemovalResult.NoMatch

        val toRemoveIterator = particles.iterator()

        val originalIterator = reference.particles.iterator()

//            var pending: FlattenedParticle? = null
        var partiallyConsumed: FlattenedParticle? = null
        toRemoveLoop@while (toRemoveIterator.hasNext()) {
            val removeHead = toRemoveIterator.next()
            originLoop@while (originalIterator.hasNext()) {
                partiallyConsumed = originalIterator.next()
                while (partiallyConsumed != null) {
                    val reducedHead = partiallyConsumed.removePrefix(removeHead)

                    when (reducedHead) {
                        RemovalResult.NoMatch -> { // can't be removed
                            if (!partiallyConsumed.isEmptiable) return RemovalResult.NoMatch // nothing to do

/*
                            if (removeHead is FlattenedChoice) { // see if we can remove one of the choice elements
                                var choiceCount = 0
                                val choiceMembers = ArrayList<FlattenedParticle?>(removeHead.particles)
                                while (choiceCount < choiceMembers.size) {
                                    partiallyConsumed = partiallyConsumed
                                        ?: if (originalIterator.hasNext()) originalIterator.next() else return RemovalResult.NoMatch
                                }

                                //            TODO("Trigger error")
                            }
*/

                            partiallyConsumed = null
                        }

                        RemovalResult.FullMatch -> {
                            partiallyConsumed = null
                        }

                        is RemovalResult.PrefixMatch -> {
                            partiallyConsumed = reducedHead.suffix
                        }
                    }

                }

            }


        }
/*
        for (p in particles) {
            // the next statement will be the condition for the orig seq elements
            while (true) {
                val bp: FlattenedParticle = pending ?: if (originalIterator.hasNext()) originalIterator.next() else return null
                // we know that there is no pending particle now.
                pending = null


                val reduced = bp.remove(p, isSiblingName)
                if (reduced == null) {
                    if (!bp.isEmptiable) return null
                    if (p is Choice) {
                        var choiceCount = 0
                        val choiceMembers = ArrayList<FlattenedParticle?>(p.particles)
                        pending = bp
                        while (choiceCount < choiceMembers.size) {
                            val bp2: FlattenedParticle = pending
                                ?: if (originalIterator.hasNext()) originalIterator.next() else return null
                            pending = null
                            // TODO use removal rather than restriction
                            val i = choiceMembers.indexOfFirst {
                                it != null && it.restricts(
                                    bp2,
                                    isSiblingName
                                )
                            }
                            if (i < 0) {
                                if (!bp.isEmptiable) return null
                            } else {
                                pending = bp2.remove(choiceMembers[i]!!, isSiblingName)
                                choiceMembers[i] = null
                                choiceCount++
                            }
                        }
                        pending = null
                        break // all choice members should have been consumed
                    }
                    // emptiable, thus ignore
                } else {
                    if (reduced.maxOccurs > VAllNNI.ZERO) pending = reduced
                }
            }
        }
*/
        val newParticles = mutableListOf<FlattenedParticle>()
        if (partiallyConsumed != null) newParticles.add(partiallyConsumed)

        while (originalIterator.hasNext()) newParticles.add(originalIterator.next())

        return when (newParticles.size) {
            0 -> RemovalResult.FullMatch
            1 -> RemovalResult.PrefixMatch(newParticles.single())
            else -> RemovalResult(FlattenedSequence(AllNNIRange.SINGLERANGE, newParticles))
        }
    }

    override fun single(): FlattenedSequence {
        return FlattenedSequence(AllNNIRange.SINGLERANGE, particles)
    }

    override fun times(range: AllNNIRange): FlattenedSequence? {
        return this.range.mergeRanges(range)?.let { FlattenedSequence(it, particles) }
    }

    override fun minus(range: AllNNIRange): FlattenedParticle? {
        return this.range.minus(range)?.let { FlattenedSequence(it, particles) }
    }

    override fun plus(other: FlattenedParticle): FlattenedParticle = when {
        other == FlattenedEmptyGroup -> this
        else -> FlattenedAll(AllNNIRange.SINGLERANGE, listOf(this, other))
    }

    override fun toString(): String = particles.joinToString(prefix = "(", postfix = range.toPostfix(")"))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as FlattenedSequence

        return particles == other.particles
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + particles.hashCode()
        return result
    }

}
