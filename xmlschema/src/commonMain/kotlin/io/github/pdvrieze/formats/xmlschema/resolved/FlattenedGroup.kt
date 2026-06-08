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

import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.AllNNIRange
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import io.github.pdvrieze.formats.xmlschema.types.isContentEqual
import nl.adaptivity.xmlutil.QName

internal typealias ContextT = (QName) -> Boolean

sealed class FlattenedGroup(
    range: AllNNIRange,
) : FlattenedParticle(range) {

    object EMPTY : Sequence(VAllNNI.ZERO..VAllNNI.ZERO, emptyList()) {
        override fun toString(): String = "()"

        override fun effectiveTotalRange(): AllNNIRange = range
        override fun single(): Sequence = this

        override fun restricts(
            reference: FlattenedParticle,
            isSiblingName: (QName) -> Boolean,
            checkHelper: CheckHelper
        ): Boolean {
            return reference.isEmptiable
        }

        override fun plus(other: FlattenedParticle): FlattenedParticle {
            return other // empty is never anything
        }
    }

    override val isEmptiable: Boolean
        get() = minOccurs == VAllNNI.ZERO || effectiveTotalRange().start == VAllNNI.ZERO

    // Implements recurse (seq-seq or all-all)
    protected fun restrictsRecurse(
        base: FlattenedGroup,
        context: ContextT,
        checkHelper: CheckHelper
    ): Boolean = when (checkHelper.version) {
        SchemaVersion.V1_0 -> restrictsRecurse10(base, context, checkHelper)
        else -> restrictsRecurse11(base, context, checkHelper)
    }

    private fun restrictsRecurse10(
        base: FlattenedGroup,
        isSiblingName: ContextT,
        checkHelper: CheckHelper
    ): Boolean {
        // 1
        if (!base.range.contains(range)) return false

        val baseIt = base.particles.iterator()

        for (p in particles) {
            // particles size should always be more than 1
            while (true) {
                if (!baseIt.hasNext()) return false
                val basePart = baseIt.next()

                // 2.1
                if (p.restricts(basePart, isSiblingName, checkHelper)) break

                // otherwise skip 2.2
                if (!basePart.isEmptiable) return false
            }
        }
        while (baseIt.hasNext()) {
            if (!baseIt.next().isEmptiable) return false
        }

        return true
    }

    private fun restrictsRecurse11(
        base: FlattenedGroup,
        context: ContextT,
        checkHelper: CheckHelper
    ): Boolean {
        return (base.remove(this, context, checkHelper) ?: return false).isEmptiable
    }

    // implements NSRecurse-CheckCardinality
    override fun restrictsWildcard(
        base: Wildcard,
        isSiblingName: ContextT,
        checkHelper: CheckHelper
    ): Boolean {
        // NSRecurse-CheckCardinality 2
        if (!base.effectiveTotalRange().contains(effectiveTotalRange())) return false

        // NSRecurse-CheckCardinality 1 // ignore count here as it will not match
        return particles.all { it.single().restricts(base.single(), isSiblingName, checkHelper) }
    }

    override fun removeFromWildcard(
        reference: Wildcard,
        isSiblingName: ContextT,
        checkHelper: CheckHelper
    ): FlattenedParticle? {
        if (particles.any { !it.single().restricts(reference.single(), isSiblingName, checkHelper) }) return null
        return reference - effectiveTotalRange() // this should already cause range checking
    }

    abstract val particles: List<FlattenedParticle>

    class All(range: AllNNIRange, override val particles: List<FlattenedParticle>) :
        FlattenedGroup(range) {

        constructor(range: AllNNIRange, particles: List<FlattenedParticle>, version: SchemaVersion) : this(
            range,
            when (version) {
                SchemaVersion.V1_0 -> particles
                else -> particles.sortedWith(particleComparator)
            }
        )

        override fun effectiveTotalRange(): AllNNIRange {
            return particles.asSequence()
                .map { it.effectiveTotalRange() }
                .fold(VAllNNI.ZERO..VAllNNI.ZERO) { l, r -> l + r }
                .let { it.start * range.start..it.endInclusive * range.endInclusive }
        }

        override fun startingTerms(): List<Term> {
            return particles.flatMap { it.startingTerms() }
        }

        override fun trailingTerms(): List<Term> {
            return particles.flatMap { it.trailingTerms() }
        }

        override fun isRestrictedBy(
            other: FlattenedParticle,
            context: ContextT,
            checkHelper: CheckHelper
        ): Boolean = other.restrictsAll(this, context, checkHelper)

        override fun isExtendedBy(other: FlattenedParticle, context: ContextT, schema: ResolvedSchemaLike): Boolean {
            return other.extendsAll(this, context, schema)
        }

        override fun extendsAll(base: All, context: ContextT, schema: ResolvedSchemaLike): Boolean {
            // part 3.1
            if (minOccurs != base.minOccurs) return false

            // this is also true if both terms are equal
            return particles.containsAll(base.particles)
        }

        override fun extendsElement(base: Element, context: ContextT, schema: ResolvedSchemaLike): Boolean {
            return extendsAll(All(AllNNIRange.SINGLERANGE, listOf(base)), context, schema) ||
                    (schema.version != SchemaVersion.V1_0 && extendsAll(All(base.range, listOf(base.single())), context, schema))
        }

        override fun plus(other: FlattenedParticle): FlattenedParticle = when {
            other == EMPTY -> this
            other is All && range.isSimple && other.range.isSimple -> {
                val mergedParticles = (particles.asSequence() + other.particles.asSequence()).groupBy {
                    it.toString()
                }.flatMap { (_, toMerge) ->
                    val p = toMerge.reduce { l, r -> l + r }
                    when {
                        p is All && p.range.isSimple -> p.particles
                        else -> listOf(p)
                    }
                }
                All(AllNNIRange.SINGLERANGE, mergedParticles)
            }

            else -> All(AllNNIRange.SINGLERANGE, listOf(this, other))
        }

        /**
         * Recurse
         */
        override fun restrictsAll(base: All, isSiblingName: ContextT, checkHelper: CheckHelper): Boolean {
            return restrictsRecurse(base, isSiblingName, checkHelper)
        }

        override fun remove(
            reference: FlattenedParticle,
            isSiblingName: ContextT,
            checkHelper: CheckHelper
        ): FlattenedParticle? {
            return reference.removeFromAll(this, isSiblingName, checkHelper)
        }

        override fun removeFromAll(
            reference: All,
            isSiblingName: ContextT,
            checkHelper: CheckHelper
        ): FlattenedParticle? {
            if (minOccurs > reference.maxOccurs) return null

            val baseParts = reference.particles.toTypedArray<FlattenedParticle?>()

            for (p in particles) {
                val matchIdx = baseParts.indexOfFirst {
                    it != null && p.single().restricts(it.single(), isSiblingName, checkHelper)
                }
                if (matchIdx < 0) return null

                val match = baseParts[matchIdx]!!
                if (p.maxOccurs > match.maxOccurs) return null // can not work
                baseParts[matchIdx] = (match - p.range)?.takeIf { it.maxOccurs > VAllNNI.ZERO }
            }
            for (b in baseParts) {
                if (b != null && !b.isEmptiable) return null
            }
            return All(reference.range, baseParts.filterNotNull(), checkHelper.version)
        }

        override fun single(): All {
            return All(AllNNIRange.SINGLERANGE, particles)
        }

        override fun times(range: AllNNIRange): All? {
            return this.range.mergeRanges(range)?.let { All(it, particles) }
        }

        override fun minus(range: AllNNIRange): FlattenedParticle? {
            return this.range.minus(range)?.let { All(it, particles) }
        }

        override fun toString(): String = particles.joinToString(prefix = "{", postfix = range.toPostfix("}"))

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            if (!super.equals(other)) return false

            other as All

            return particles == other.particles
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + particles.hashCode()
            return result
        }
    }

    class Choice(range: AllNNIRange, override val particles: List<FlattenedParticle>) :
        FlattenedGroup(range) {

        constructor(range: AllNNIRange, particles: List<FlattenedParticle>, version: SchemaVersion) : this(
            range,
            when (version) {
                SchemaVersion.V1_0 -> particles
                else -> particles.sortedWith(particleComparator)
            }
        )

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
                }.let { it.start * range.start..it.endInclusive * range.endInclusive }
        }

        override fun isRestrictedBy(
            other: FlattenedParticle,
            context: ContextT,
            checkHelper: CheckHelper
        ): Boolean = other.restrictsChoice(this, context, checkHelper)

        override fun isExtendedBy(other: FlattenedParticle, context: ContextT, schema: ResolvedSchemaLike): Boolean {
            return other.extendsChoice(this, context, schema)
        }

        override fun extendsChoice(base: Choice, context: ContextT, schema: ResolvedSchemaLike): Boolean {
            return range == base.range && particles.isContentEqual(base.particles)
        }

        override fun extendsElement(base: Element, context: ContextT, schema: ResolvedSchemaLike): Boolean {
            return extendsChoice(Choice(AllNNIRange.SINGLERANGE, listOf(base)), context, schema) ||
                    (schema.version != SchemaVersion.V1_0 && extendsChoice(Choice(base.range, listOf(base.single())), context, schema))
        }

        // Recurse lax
        override fun restrictsChoice(base: Choice, isSiblingName: ContextT, checkHelper: CheckHelper): Boolean {
            if (!base.range.contains(range)) return false

            val baseIt = base.particles.iterator()

            for (p in particles) {
                while (true) {
                    if (!baseIt.hasNext()) return false
                    if (p.restricts(baseIt.next(), isSiblingName, checkHelper)) break
                }
            } // this doesn't need to check emptiability

            return true
        }

        override fun restrictsAll(base: All, isSiblingName: ContextT, checkHelper: CheckHelper): Boolean {
            if (checkHelper.version == SchemaVersion.V1_0) return false
            return particles.all {
                val reRanged = it * range
                reRanged != null && reRanged.restrictsAll(base, isSiblingName, checkHelper)
            }
        }

        override fun remove(
            reference: FlattenedParticle,
            isSiblingName: ContextT,
            checkHelper: CheckHelper
        ): FlattenedParticle? {
            return reference.removeFromChoice(this, isSiblingName, checkHelper)
        }

        override fun removeFromChoice(
            reference: Choice,
            isSiblingName: ContextT,
            checkHelper: CheckHelper
        ): FlattenedParticle? {
            if (!reference.effectiveTotalRange().contains(effectiveTotalRange())) return null

            val baseIt = reference.particles.iterator()

            for (p in particles) {
                var match = false
                while (!match) {
                    if (!baseIt.hasNext()) return null
                    val basePart = baseIt.next()
                    if (p.restricts(basePart, isSiblingName, checkHelper)) {
                        match = true
                    }
                }
                if (!match) return null
            }

            val newMin = reference.minOccurs.safeMinus(minOccurs)
            val newMax = reference.maxOccurs.safeMinus(maxOccurs, newMin)

            return Choice(newMin..newMax, reference.particles)
        }

        override fun single(): Choice {
            return Choice(AllNNIRange.SINGLERANGE, particles)
        }

        override fun times(range: AllNNIRange): Choice? {
            return this.range.mergeRanges(range)?.let { Choice(it, particles) }
        }

        override fun minus(range: AllNNIRange): FlattenedParticle? {
            return this.range.minus(range)?.let { Choice(it, particles) }
        }

        override fun plus(other: FlattenedParticle): FlattenedParticle = when {
            other == EMPTY -> this
            else -> All(AllNNIRange.SINGLERANGE, listOf(this, other))
        }

        override fun toString(): String =
            particles.joinToString(separator = "| ", prefix = "(", postfix = range.toPostfix(")"))

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            if (!super.equals(other)) return false

            other as Choice

            return particles == other.particles
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + particles.hashCode()
            return result
        }
    }

    open class Sequence internal constructor(
        range: AllNNIRange,
        final override val particles: List<FlattenedParticle>
    ) : FlattenedGroup(range) {

        override fun effectiveTotalRange(): AllNNIRange {
            return particles.asSequence()
                .map { it.effectiveTotalRange() }
                .reduce { l, r -> l + r }
                .let { it.start * range.start..it.endInclusive * range.endInclusive }
        }

        override fun startingTerms(): List<Term> {
            val result = mutableListOf<Term>()
            for (particle in particles) {
                result.addAll(particle.startingTerms())
                if (!particle.isEmptiable) return result
            }
            return result
        }

        override fun trailingTerms(): List<Term> {
            val result = mutableListOf<Term>()
            for (particle in particles.asReversed()) {
                result.addAll(particle.trailingTerms())
                if (!particle.isEmptiable) return result
            }
            return result
        }

        override fun isRestrictedBy(
            other: FlattenedParticle,
            context: ContextT,
            checkHelper: CheckHelper
        ): Boolean = other.restrictsSequence(this, context, checkHelper)

        override fun isExtendedBy(other: FlattenedParticle, context: ContextT, schema: ResolvedSchemaLike): Boolean {
            return other.extendsSequence(this, context, schema)
        }

        override fun extendsElement(base: Element, context: ContextT, schema: ResolvedSchemaLike): Boolean {
            // 3.9.6.2 step 2
            return (range.isSimple && particles.isNotEmpty() && particles.first().extends(base, context, schema)) ||
                    extendsSequence(Sequence(AllNNIRange.SINGLERANGE, listOf(base)), context, schema) ||
                    (schema.version != SchemaVersion.V1_0 && extendsSequence(Sequence(base.range, listOf(base.single())), context, schema))
        }

        override fun extendsWildcard(base: Wildcard, context: ContextT, schema: ResolvedSchemaLike): Boolean {
            // 3.9.6.2 step 2
            return range.isSimple && particles.isNotEmpty() && particles.first().extends(base, context, schema)
        }

        override fun extendsAll(base: All, context: ContextT, schema: ResolvedSchemaLike): Boolean {
            // 3.9.6.2 step 2
            return range.isSimple && particles.isNotEmpty() && particles.first().extends(base, context, schema)
        }

        override fun extendsChoice(base: Choice, context: ContextT, schema: ResolvedSchemaLike): Boolean {
            // 3.9.6.2 step 2
            return range.isSimple && particles.isNotEmpty() && particles.first().extends(base, context, schema)
        }

        override fun extendsSequence(
            base: Sequence,
            isSiblingName: (QName) -> Boolean,
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
            return range.isSimple && particles.first().extends(base, isSiblingName, schema)
        }

        // Restrict recurseUnordered
        override fun restrictsAll(base: All, isSiblingName: ContextT, checkHelper: CheckHelper): Boolean {
            if (!base.range.contains(range)) return false // 1

            val unprocessed = base.particles.toMutableList<FlattenedParticle?>() // 2.1

            val pendingChoiceParticles = mutableListOf<Choice>()
            for (p in particles) { // 2.2
                val matchIdx = unprocessed.indexOfFirst { it != null && p.restricts(it, isSiblingName, checkHelper) }
                when {
                    matchIdx >= 0 -> {
                        val newMatch = (unprocessed[matchIdx]!! - p.range)?.takeIf { it.maxOccurs > VAllNNI.ZERO }
                        unprocessed[matchIdx] = newMatch // 2.1
                    }

                    p is Choice -> pendingChoiceParticles.add(p)
                    else -> return false
                }
            }

            for (bp in unprocessed) { // 2.3
                if (bp != null && !bp.isEmptiable) return false
            }

            val remaining = All(range, unprocessed.filterNotNull())

            val choices = pendingChoiceParticles
                .flatMap { ch -> ch.particles.map { (it * ch.range) ?: return false } }
                .groupBy { toString() }
                .map { (_, parts) -> parts.reduce { l, r -> l + r } }

            // XXX strange unused variables
            for (choice in pendingChoiceParticles) {

                for (e in choice.particles) {
                    if (unprocessed.none { it != null && e.restricts(it, isSiblingName, checkHelper) }) return false
                }
            }

            return true
        }

        /**
         * MapAndSum
         */
        override fun restrictsChoice(base: Choice, isSiblingName: ContextT, checkHelper: CheckHelper): Boolean {
            return restrictsChoice_1_0(base, isSiblingName, checkHelper) ||
                    (checkHelper.version == SchemaVersion.V1_1 && Choice(
                        AllNNIRange.SINGLERANGE,
                        listOf(this)
                    ).restrictsChoice(base, isSiblingName, checkHelper))
        }

        private fun restrictsChoice_1_0(
            base: Choice,
            isSiblingName: ContextT,
            checkHelper: CheckHelper
        ): Boolean {
            // MapAndSum 2
            val partSize = VAllNNI.Value(particles.size.toUInt())
            if (!base.range.contains((minOccurs * partSize)..(maxOccurs * partSize))) return false

            val minValues = Array(base.particles.size) { VAllNNI.ZERO }
            val maxValues = Array<VAllNNI>(base.particles.size) { VAllNNI.ZERO }

            // TODO implement "unfolding"
            for (p in particles) {
                val matchIdx = base.particles.indexOfFirst { p.single().restricts(
                    it.single(),
                    isSiblingName,
                    checkHelper
                ) }
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

        override fun removeFromChoice(
            reference: Choice,
            isSiblingName: ContextT,
            checkHelper: CheckHelper
        ): FlattenedParticle? {
            // try to match the sequence to a single element in the sequence
            if (reference.maxOccurs > VAllNNI.ONE) {
                val reduced = reference.particles.asSequence().mapNotNull { it.remove(this, isSiblingName, checkHelper) }.firstOrNull()
                if (reduced != null) {
                    val tail = reference - AllNNIRange.SINGLERANGE
                    return when {
                        reduced.maxOccurs == VAllNNI.ZERO -> tail ?: EMPTY
                        tail == null -> reduced
                        else -> Sequence(AllNNIRange.SINGLERANGE, listOf(reduced, tail))
                    }
                }
            } else {
                val reduced = reference.particles.asSequence().mapNotNull {
                    it.remove(this, isSiblingName, checkHelper)
                }.firstOrNull()
                if (reduced != null) return reduced
            }

            if (reference.maxOccurs <= VAllNNI.ONE) return null

            var reduced: FlattenedParticle = reference
            for (p in particles) {
                reduced = reduced.remove(p, isSiblingName, checkHelper) ?: return null
            }
            return reduced
        }

        override fun restrictsSequence(
            base: Sequence,
            context: ContextT,
            checkHelper: CheckHelper
        ): Boolean {
            return restrictsRecurse(base, context, checkHelper)
        }

        override fun remove(
            reference: FlattenedParticle,
            isSiblingName: ContextT,
            checkHelper: CheckHelper
        ): FlattenedParticle? {
            return reference.removeFromSequence(this, isSiblingName, checkHelper)
        }

        override fun removeFromSequence(
            reference: Sequence,
            isSiblingName: ContextT,
            checkHelper: CheckHelper
        ): FlattenedParticle? {
            if (reference.maxOccurs > VAllNNI.ONE) {
                val singleReduction = single().removeFromSequence(reference.single(), isSiblingName, checkHelper)
                if (singleReduction != null && singleReduction.isEmptiable) {
                    // The sequences "match"
                    return reference - range
                }
                val head = Sequence(reference.minOccurs..VAllNNI.ONE, reference.particles)
                val tail = (reference - AllNNIRange.SINGLERANGE)!!
                val reducedHead = removeFromSequence(head, isSiblingName, checkHelper) ?: return null
                return Sequence(AllNNIRange.SINGLERANGE, listOf(reducedHead, tail))
            } else { //base is optional or simple
                if (maxOccurs > VAllNNI.ONE) {
                    return Sequence(AllNNIRange.SINGLERANGE, listOf(this)).removeFromSequence(
                        reference,
                        isSiblingName,
                        checkHelper
                    )
                }
                if (minOccurs == VAllNNI.ZERO && reference.effectiveTotalRange().start != VAllNNI.ZERO) return null
                val baseIt = reference.particles.iterator()
                var pending: FlattenedParticle? = null
                for (p in particles) {
                    while (true) {
                        val bp = pending ?: if (baseIt.hasNext()) baseIt.next() else return null
                        pending = null
                        val reduced = bp.remove(p, isSiblingName, checkHelper)
                        if (reduced == null) {
                            if (!bp.isEmptiable) return null
                            if (p is Choice) {
                                var choiceCount = 0
                                val choiceMembers = ArrayList<FlattenedParticle?>(p.particles)
                                pending = bp
                                while (choiceCount < choiceMembers.size) {
                                    val bp2 = pending ?: if (baseIt.hasNext()) baseIt.next() else return null
                                    pending = null
                                    // TODO use removal rather than restriction
                                    val i = choiceMembers.indexOfFirst {
                                        it != null && it.restricts(
                                            bp2,
                                            isSiblingName,
                                            checkHelper
                                        )
                                    }
                                    if (i < 0) {
                                        if (!bp.isEmptiable) return null
                                    } else {
                                        pending = bp2.remove(choiceMembers[i]!!, isSiblingName, checkHelper)
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
                            break
                        }
                    }
                }
                val newParticles = mutableListOf<FlattenedParticle>()
                if (pending != null) newParticles.add(pending)

                while (baseIt.hasNext()) newParticles.add(baseIt.next())
                return when (newParticles.size) {
                    0 -> EMPTY
                    1 -> newParticles.single()
                    else -> Sequence(AllNNIRange.SINGLERANGE, newParticles)
                }
            }
        }

        override fun single(): Sequence {
            return Sequence(AllNNIRange.SINGLERANGE, particles)
        }

        override fun times(range: AllNNIRange): Sequence? {
            return this.range.mergeRanges(range)?.let { Sequence(it, particles) }
        }

        override fun minus(range: AllNNIRange): FlattenedParticle? {
            return this.range.minus(range)?.let { Sequence(it, particles) }
        }

        override fun plus(other: FlattenedParticle): FlattenedParticle = when {
            other == EMPTY -> this
            else -> All(AllNNIRange.SINGLERANGE, listOf(this, other))
        }

        override fun toString(): String = particles.joinToString(prefix = "(", postfix = range.toPostfix(")"))

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            if (!super.equals(other)) return false

            other as Sequence

            return particles == other.particles
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + particles.hashCode()
            return result
        }

    }

    companion object {


        // TODO move to IResolvedSequence
        internal fun checkSequence(
            particles: List<FlattenedParticle>,
            isSiblingName: (QName) -> Boolean,
            checkHelper: CheckHelper
        ) {
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
                            if (checkHelper.version == SchemaVersion.V1_0) {
                                // In version 1.1 resolving prioritises explicit elements, wildcards can omit
                                require(lastAnys.none { it.matches(startName, isSiblingName, checkHelper.schema) }) {
                                    "Ambiguous sequence $startName - ${lastAnys}"
                                }
                            }
                        }

                        is Wildcard -> {
                            require(lastAnys.none { it.intersects(startTerm.term, isSiblingName, checkHelper.schema) }) {
                                "Non-deterministic sequence group: ${particles.joinToString()}"
                            }
                            if (checkHelper.version == SchemaVersion.V1_0) {
                                require(lastOptionals.none { startTerm.term.matches(it, isSiblingName, checkHelper.schema) }) {
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
                                    is Wildcard -> lastAnys.add(e.term)
                                    is Element -> lastOptionals.add(e.term.mdlQName)
                                }
                            }
                        }
                        for (e in p.startingTerms()) {
                            when (e) {
                                is Wildcard -> lastAnys.add(e.term)
                                is Element -> lastOptionals.add(e.term.mdlQName)
                            }
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


    }

}

