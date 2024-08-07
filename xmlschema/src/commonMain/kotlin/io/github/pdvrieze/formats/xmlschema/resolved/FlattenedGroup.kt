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

import io.github.pdvrieze.formats.xmlschema.datatypes.AnyType
import io.github.pdvrieze.formats.xmlschema.resolved.FlattenedGroup.*
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.AllNNIRange
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import io.github.pdvrieze.formats.xmlschema.types.isContentEqual
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.isEquivalent
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.namespaceURI
import kotlin.jvm.JvmStatic

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
        override fun restrictsAll(base: All, context: ContextT, checkHelper: CheckHelper): Boolean {
            return restrictsRecurse(base, context, checkHelper)
        }

        override fun remove(
            reference: FlattenedParticle,
            context: ContextT,
            checkHelper: CheckHelper
        ): FlattenedParticle? {
            return reference.removeFromAll(this, context, checkHelper)
        }

        override fun removeFromAll(
            base: All,
            isSiblingName: ContextT,
            checkHelper: CheckHelper
        ): FlattenedParticle? {
            if (minOccurs > base.maxOccurs) return null

            val baseParts = base.particles.toTypedArray<FlattenedParticle?>()

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
            return All(base.range, baseParts.filterNotNull(), checkHelper.version)
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

        override fun restrictsAll(base: All, context: ContextT, checkHelper: CheckHelper): Boolean {
            if (checkHelper.version == SchemaVersion.V1_0) return false
            return particles.all {
                val reRanged = it * range
                reRanged != null && reRanged.restrictsAll(base, context, checkHelper)
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
            base: Choice,
            isSiblingName: ContextT,
            checkHelper: CheckHelper
        ): FlattenedParticle? {
            if (!base.effectiveTotalRange().contains(effectiveTotalRange())) return null

            val baseIt = base.particles.iterator()

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

            val newMin = base.minOccurs.safeMinus(minOccurs)
            val newMax = base.maxOccurs.safeMinus(maxOccurs, newMin)

            return Choice(newMin..newMax, base.particles)
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
                .flatMap { ch -> ch.particles.map { it * ch.range ?: return false } }
                .groupBy { toString() }
                .map { (strRep, parts) -> parts.reduce { l, r -> l + r } }

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
        override fun restrictsChoice(base: Choice, context: ContextT, checkHelper: CheckHelper): Boolean {
            return restrictsChoice_1_0(base, context, checkHelper) ||
                    (checkHelper.version == SchemaVersion.V1_1 && Choice(
                        AllNNIRange.SINGLERANGE,
                        listOf(this)
                    ).restrictsChoice(base, context, checkHelper))
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
            base: Choice,
            context: ContextT,
            checkHelper: CheckHelper
        ): FlattenedParticle? {
            // try to match the sequence to a single element in the sequence
            if (base.maxOccurs > VAllNNI.ONE) {
                val reduced = base.particles.asSequence().mapNotNull { it.remove(this, context, checkHelper) }.firstOrNull()
                if (reduced != null) {
                    val tail = base - AllNNIRange.SINGLERANGE
                    return when {
                        reduced.maxOccurs == VAllNNI.ZERO -> return tail ?: EMPTY
                        tail == null -> reduced
                        else -> Sequence(AllNNIRange.SINGLERANGE, listOf(reduced, tail))
                    }
                }
            } else {
                val reduced = base.particles.asSequence().mapNotNull {
                    it.remove(this, context, checkHelper)
                }.firstOrNull()
                if (reduced != null) return reduced
            }

            if (base.maxOccurs <= VAllNNI.ONE) return null

            var reduced: FlattenedParticle = base
            for (p in particles) {
                reduced = reduced.remove(p, context, checkHelper) ?: return null
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
            context: ContextT,
            checkHelper: CheckHelper
        ): FlattenedParticle? {
            return reference.removeFromSequence(this, context, checkHelper)
        }

        override fun removeFromSequence(
            base: Sequence,
            isSiblingName: ContextT,
            checkHelper: CheckHelper
        ): FlattenedParticle? {
            if (base.maxOccurs > VAllNNI.ONE) {
                val singleReduction = single().removeFromSequence(base.single(), isSiblingName, checkHelper)
                if (singleReduction != null && singleReduction.isEmptiable) {
                    // The sequences "match"
                    return base - range
                }
                val head = Sequence(base.minOccurs..VAllNNI.ONE, base.particles)
                val tail = (base - AllNNIRange.SINGLERANGE)!!
                val reducedHead = removeFromSequence(head, isSiblingName, checkHelper) ?: return null
                return Sequence(AllNNIRange.SINGLERANGE, listOf(reducedHead, tail))
            } else { //base is optional or simple
                if (maxOccurs > VAllNNI.ONE) {
                    return Sequence(AllNNIRange.SINGLERANGE, listOf(this)).removeFromSequence(
                        base,
                        isSiblingName,
                        checkHelper
                    )
                }
                if (minOccurs == VAllNNI.ZERO && base.effectiveTotalRange().start != VAllNNI.ZERO) return null
                val baseIt = base.particles.iterator()
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

sealed class FlattenedParticle(val range: AllNNIRange) {

    val maxOccurs get() = range.endInclusive
    val minOccurs get() = range.start

    open val isEmptiable: Boolean get() = minOccurs == VAllNNI.ZERO
    val isVariable: Boolean get() = minOccurs != maxOccurs

    abstract fun effectiveTotalRange(): AllNNIRange

    abstract fun startingTerms(): List<Term>
    abstract fun trailingTerms(): List<Term>

    abstract fun single(): FlattenedParticle

    protected abstract fun isRestrictedBy(
        other: FlattenedParticle,
        context: ContextT,
        checkHelper: CheckHelper
    ): Boolean

    open fun restricts(
        reference: FlattenedParticle,
        isSiblingName: (QName) -> Boolean,
        checkHelper: CheckHelper,
    ): Boolean {
        return reference.isRestrictedBy(this, isSiblingName, checkHelper)
    }

    open fun extends(base: FlattenedParticle, isSiblingName: (QName) -> Boolean, schema: ResolvedSchemaLike): Boolean {
        return base.isExtendedBy(this, isSiblingName, schema)
    }

    protected abstract fun isExtendedBy(
        other: FlattenedParticle,
        context: ContextT,
        schema: ResolvedSchemaLike
    ): Boolean

    open fun extendsElement(base: Element, context: ContextT, schema: ResolvedSchemaLike): Boolean = false

    open fun extendsWildcard(base: Wildcard, context: ContextT, schema: ResolvedSchemaLike): Boolean =
        false

    open fun extendsAll(base: All, context: ContextT, schema: ResolvedSchemaLike): Boolean = false

    open fun extendsChoice(base: Choice, context: ContextT, schema: ResolvedSchemaLike): Boolean = false

    open fun extendsSequence(base: Sequence, isSiblingName: (QName) -> Boolean, schema: ResolvedSchemaLike): Boolean =
        false

    open fun restrictsElement(base: Element, context: ContextT, checkHelper: CheckHelper): Boolean = false

    open fun restrictsWildcard(base: Wildcard, context: ContextT, checkHelper: CheckHelper): Boolean =
        false

    open fun restrictsAll(base: All, context: ContextT, checkHelper: CheckHelper): Boolean = false

    open fun restrictsChoice(base: Choice, context: ContextT, checkHelper: CheckHelper): Boolean = false

    open fun restrictsSequence(base: Sequence, context: ContextT, checkHelper: CheckHelper): Boolean =
        false

    abstract operator fun times(range: AllNNIRange): FlattenedParticle?

    abstract fun remove(
        reference: FlattenedParticle,
        context: ContextT,
        checkHelper: CheckHelper
    ): FlattenedParticle?

    open fun removeFromElement(
        reference: Element,
        context: ContextT,
        checkHelper: CheckHelper
    ): FlattenedParticle? = null

    open fun removeFromWildcard(
        reference: Wildcard,
        context: ContextT,
        checkHelper: CheckHelper
    ): FlattenedParticle? = null

    open fun removeFromAll(
        reference: All,
        context: ContextT,
        checkHelper: CheckHelper
    ): FlattenedParticle? = null

    open fun removeFromChoice(
        reference: Choice,
        context: ContextT,
        checkHelper: CheckHelper
    ): FlattenedParticle? = null

    open fun removeFromSequence(
        reference: Sequence,
        context: ContextT,
        checkHelper: CheckHelper
    ): FlattenedParticle? = null

    sealed class Term(range: AllNNIRange) : FlattenedParticle(range) {
        abstract val term: ResolvedBasicTerm

        override fun effectiveTotalRange(): AllNNIRange = range

        abstract override fun single(): Term

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Term

            return term == other.term
        }

        override fun hashCode(): Int {
            return term.hashCode()
        }


    }

    class Element internal constructor(
        range: AllNNIRange,
        override val term: ResolvedElement,
        @Suppress("UNUSED_PARAMETER") dummy: Boolean
    ) :
        Term(range) {
        override fun startingTerms(): List<Element> {
            return listOf(this)
        }

        override fun trailingTerms(): List<Element> = listOf(this)

        override fun isRestrictedBy(
            other: FlattenedParticle,
            context: ContextT,
            checkHelper: CheckHelper
        ): Boolean = other.restrictsElement(this, context, checkHelper)

        override fun isExtendedBy(other: FlattenedParticle, context: ContextT, schema: ResolvedSchemaLike): Boolean {
            return other.extendsElement(this, context, schema)
        }

        override fun extendsElement(base: Element, context: ContextT, schema: ResolvedSchemaLike): Boolean {
            return range == base.range && term == base.term
        }

        override fun restrictsElement(
            base: Element,
            context: ContextT,
            checkHelper: CheckHelper
        ): Boolean {
            if (!base.range.contains(range)) return false

            if (!base.term.mdlQName.isEquivalent(term.mdlQName)) return false

            return base.term.subsumes(term, checkHelper.isLax)
        }

        // Implements NSCompat
        override fun restrictsWildcard(
            base: Wildcard,
            context: ContextT,
            checkHelper: CheckHelper
        ): Boolean {
            // NSCompat 2
            if (!base.range.contains(range)) return false

            // NSCompat 1
            return base.term.matches(term.mdlQName, context, checkHelper.schema)

        }

        override fun restrictsAll(base: All, context: ContextT, checkHelper: CheckHelper): Boolean {
            return All(AllNNIRange.SINGLERANGE, listOf(this)).restrictsAll(base, context, checkHelper)
        }

        override fun restrictsChoice(base: Choice, isSiblingName: ContextT, checkHelper: CheckHelper): Boolean {
            // The option to do it either way is valid for 1.1
            return Choice(AllNNIRange.SINGLERANGE, listOf(this)).restrictsChoice(base, isSiblingName, checkHelper) ||
                    (checkHelper.version == SchemaVersion.V1_1 &&
                            Choice(range, listOf(single())).restricts(base, isSiblingName, checkHelper))
        }

        override fun restrictsSequence(
            base: Sequence,
            context: ContextT,
            checkHelper: CheckHelper
        ): Boolean {
            return Sequence(AllNNIRange.SINGLERANGE, listOf(this)).restrictsSequence(base, context, checkHelper)
        }

        override fun remove(
            reference: FlattenedParticle,
            context: ContextT,
            checkHelper: CheckHelper
        ): FlattenedParticle? {
            return reference.removeFromElement(this, context, checkHelper)
        }

        override fun removeFromElement(
            reference: Element,
            context: ContextT,
            checkHelper: CheckHelper
        ): FlattenedParticle? {
            if (!reference.term.mdlQName.isEquivalent(term.mdlQName)) return null

            if (!reference.term.subsumes(term, checkHelper.isLax)) return null

            return reference.minus(range)
        }

        override fun removeFromWildcard(
            reference: Wildcard,
            context: ContextT,
            checkHelper: CheckHelper
        ): FlattenedParticle? {
            if (!reference.term.matches(term.mdlQName, context, checkHelper.schema)) return null

            return reference - range
        }

        override fun removeFromAll(
            reference: All,
            context: ContextT,
            checkHelper: CheckHelper
        ): FlattenedParticle? {
            return super.removeFromAll(reference, context, checkHelper)
        }

        override fun removeFromChoice(
            reference: Choice,
            context: ContextT,
            checkHelper: CheckHelper
        ): FlattenedParticle? {
            val matchIdx = reference.particles.indexOfFirst { it.single().isRestrictedBy(single(), context, checkHelper) }
            if (matchIdx < 0) return null
            val match = reference.particles[matchIdx]
            return if (maxOccurs == VAllNNI.UNBOUNDED) {
                when {
                    match.maxOccurs == VAllNNI.UNBOUNDED -> reference - AllNNIRange.SINGLERANGE
                    reference.maxOccurs == VAllNNI.UNBOUNDED -> EMPTY
                    else -> match.remove(this, context, checkHelper) // perhaps inside the match it is possible
                }
            } else { // consider further options
                when {
                    match.minOccurs * reference.minOccurs > minOccurs -> (match * reference.range)?.minus(range)
                    match.range.contains(range) -> reference - AllNNIRange.SINGLERANGE
                    match.range.isSimple -> reference - range
                    else -> match.remove(this, context, checkHelper) // TODO a bit more options
                }
            }
        }

        override fun removeFromSequence(
            reference: Sequence,
            context: ContextT,
            checkHelper: CheckHelper
        ): FlattenedParticle? {
            if (reference.maxOccurs > VAllNNI.ONE) { // handle the case that there is more than 1 iteration
                val head = Sequence(reference.minOccurs..VAllNNI.ONE, reference.particles)
                val tail = (reference - AllNNIRange.SINGLERANGE) ?: error("Should not happen because maxOccurs>0")
                val trimmedHead = removeFromSequence(head, context, checkHelper) ?: return null
                if (trimmedHead.maxOccurs == VAllNNI.ZERO) return tail
                return Sequence(AllNNIRange.SINGLERANGE, listOf(trimmedHead, tail))
            }
            val partIt = reference.particles.iterator()
            val newParticles = mutableListOf<FlattenedParticle>()
            while (partIt.hasNext()) {
                val part = partIt.next()
                val removed = part.remove(this, context, checkHelper)
                when {
                    removed == null -> if (!part.isEmptiable) return null

                    removed.maxOccurs > VAllNNI.ZERO -> {
                        newParticles.add(removed)
                        break
                    }

                    else -> break
                }
            }
            while (partIt.hasNext()) {
                newParticles.add(partIt.next())
            } // flush remaining particles

            // We consumed part of the sequence so it must occur
            return when (newParticles.size) {
                0 -> EMPTY
                1 -> newParticles.single() * range
                else -> Sequence(AllNNIRange.SINGLERANGE, newParticles)
            }
        }

        override fun single(): Element = Element(AllNNIRange.SINGLERANGE, term, true)

        override fun times(range: AllNNIRange): Element? {
            return this.range.mergeRanges(range)?.let { Element(it, term, true) }
        }

        override fun minus(range: AllNNIRange): FlattenedParticle? {
            return this.range.minus(range)?.let { Element(it, term, true) }
        }

        override fun plus(other: FlattenedParticle): FlattenedParticle {
            return when {
                other == EMPTY -> this

                other is All -> return other + this

                other is Element && this.isMergable(other) ->
                    Element(range + other.range, term, false)

                else -> All(AllNNIRange.SINGLERANGE, listOf(this, other))
            }
        }

        private fun isMergable(other: Element): Boolean {
            return term.mdlAbstract == other.term.mdlAbstract &&
                    term.mdlQName == other.term.mdlQName
        }

        override fun toString(): String = range.toPostfix(term.mdlQName.toString())


    }

    abstract operator fun minus(range: AllNNIRange): FlattenedParticle?
    abstract operator fun plus(other: FlattenedParticle): FlattenedParticle
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as FlattenedParticle

        return range == other.range
    }

    override fun hashCode(): Int {
        return range.hashCode()
    }

    class Wildcard(range: AllNNIRange, override val term: ResolvedAny) : Term(range) {

        override fun startingTerms(): List<Wildcard> {
            return listOf(this)
        }

        override fun trailingTerms(): List<Wildcard> = listOf(this)

        override fun isRestrictedBy(
            other: FlattenedParticle,
            context: ContextT,
            checkHelper: CheckHelper
        ): Boolean = other.restrictsWildcard(this, context, checkHelper)

        override fun isExtendedBy(other: FlattenedParticle, context: ContextT, schema: ResolvedSchemaLike): Boolean {
            return other.extendsWildcard(this, context, schema)
        }

        override fun extendsWildcard(base: Wildcard, context: ContextT, schema: ResolvedSchemaLike): Boolean {
            return range == base.range && term == base.term
        }

        /**
         * NSSubset
         */
        override fun restrictsWildcard(
            base: Wildcard,
            context: ContextT,
            checkHelper: CheckHelper
        ): Boolean {
            // NSSubset 1
            if (!base.range.contains(range)) return false

            // NSSubset 2, subset per Schema 1 - 3.10.6
            if (!term.mdlNamespaceConstraint.isSubsetOf(base.term.mdlNamespaceConstraint, checkHelper.version)) return false

            // NSSubset 3, (exception for the ur-wildcard is needed - although a shortcut may apply by just always
            // restricting AnyType)
            return base.term === AnyType.urWildcard || term.mdlProcessContents >= base.term.mdlProcessContents
        }

        override fun restrictsAll(base: All, context: ContextT, checkHelper: CheckHelper): Boolean {
            return when (checkHelper.version) {
                SchemaVersion.V1_0 -> false
                else -> All(AllNNIRange.SINGLERANGE, listOf(this), checkHelper.version).restrictsAll(
                    base,
                    context,
                    checkHelper
                )
            }
        }

        override fun restrictsChoice(base: Choice, context: ContextT, checkHelper: CheckHelper): Boolean {
            return when (checkHelper.version) {
                SchemaVersion.V1_0 -> false
                else -> Choice(AllNNIRange.SINGLERANGE, listOf(this), checkHelper.version).restrictsChoice(
                    base,
                    context,
                    checkHelper
                )
            }
        }

        override fun restrictsSequence(base: Sequence, context: ContextT, checkHelper: CheckHelper): Boolean {
            return when (checkHelper.version) {
                SchemaVersion.V1_0 -> false
                else -> Sequence(AllNNIRange.SINGLERANGE, listOf(this)).restrictsSequence(base, context, checkHelper)
            }
        }

        override fun plus(other: FlattenedParticle): FlattenedParticle = when {
            other == EMPTY -> this
            other is Wildcard && this.isMergable(other) -> Wildcard(this.range + other.range, term)
            else -> All(AllNNIRange.SINGLERANGE, listOf(this, other))
        }

        fun isMergable(other: Wildcard) =
            term.mdlNamespaceConstraint == other.term.mdlNamespaceConstraint &&
                    term.mdlProcessContents == other.term.mdlProcessContents &&
                    term.mdlNotQName == other.term.mdlNotQName

        override fun remove(
            reference: FlattenedParticle,
            context: ContextT,
            checkHelper: CheckHelper
        ): FlattenedParticle? {
            return reference.removeFromWildcard(this, context, checkHelper)
        }

        override fun removeFromWildcard(
            reference: Wildcard,
            context: ContextT,
            checkHelper: CheckHelper
        ): FlattenedParticle? {
            if (!reference.range.contains(range)) return null
            if (!term.mdlNamespaceConstraint.isSubsetOf(
                    reference.term.mdlNamespaceConstraint,
                    checkHelper.version
                )
            ) return null
            if (reference.term !== AnyType.urWildcard && term.mdlProcessContents < reference.term.mdlProcessContents) return null
            return reference - range
        }

        override fun single(): Wildcard = Wildcard(AllNNIRange.SINGLERANGE, term)

        override fun times(range: AllNNIRange): Wildcard? {
            return this.range.mergeRanges(range)?.let { Wildcard(it, term) }
        }

        override fun minus(range: AllNNIRange): FlattenedParticle? {
            return this.range.minus(range)?.let { Wildcard(it, term) }
        }

        override fun toString(): String = range.toPostfix("<${term.mdlNamespaceConstraint}>")

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            if (!super.equals(other)) return false

            other as Wildcard

            return term == other.term
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + term.hashCode()
            return result
        }
    }

    companion object {
        /**
         * Either create an element, or a choice for the substitution group (if it exists)
         */
        @JvmStatic
        fun elementOrSubstitution(
            range: AllNNIRange,
            term: ResolvedElement,
            schemaVersion: SchemaVersion
        ): FlattenedParticle = when {
            term !is ResolvedGlobalElement ||
                    term.mdlSubstitutionGroupMembers.isEmpty()
            -> Element(range, term, true)

            else -> {
                val sg = term.fullSubstitutionGroup(schemaVersion)
                when (sg.size) {
                    0 -> EMPTY
                    else -> {
                        val elems = sg.map { Element(AllNNIRange.SINGLERANGE, it, true) }
                        Choice(
                            range,
                            elems,
                            SchemaVersion.V1_1
                        ) // force 1.1 to "sort" the elements as substitution groups are not ordered
                    }
                }
            }
        }


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
                is All -> 0
                is Choice -> 1
                is Sequence -> 2
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

        internal fun AllNNIRange.toPostfix(prefix: String = ""): String = when {
            endInclusive == VAllNNI.UNBOUNDED -> when (start) {
                VAllNNI.ZERO -> prefix + '*'
                VAllNNI.ONE -> prefix + '+'
                else -> prefix + '[' + start.toULong() + "+]"
            }

            endInclusive > VAllNNI.ONE -> prefix + '[' + start.toULong() + ".." + (endInclusive as VAllNNI.Value).toULong() + ']'
            // end inclusive 0 can happen due to subtraction in the sequence algorithm
            endInclusive == VAllNNI.ZERO -> prefix + "[0]"
            start == VAllNNI.ZERO -> prefix + '?'
            else -> prefix // both are 1
        }
    }


}
