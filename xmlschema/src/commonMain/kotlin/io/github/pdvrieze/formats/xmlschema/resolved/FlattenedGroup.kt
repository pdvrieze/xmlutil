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

import io.github.pdvrieze.formats.xmlschema.impl.invariantNotNull
import io.github.pdvrieze.formats.xmlschema.resolved.FlattenedGroup.All
import io.github.pdvrieze.formats.xmlschema.resolved.FlattenedGroup.Choice
import io.github.pdvrieze.formats.xmlschema.resolved.FlattenedGroup.Sequence
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSchema.Companion.VALIDATE_PEDANTIC
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

    object EMPTY : Sequence(VAllNNI.ZERO..VAllNNI.ZERO, emptyList()) {
        override fun toString(): String = "()"

        override fun effectiveTotalRange(): AllNNIRange = range
        override fun single(): Sequence = this

        fun restricts3(
            reference: FlattenedParticle,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike,
        ): Boolean {
            return reference.effectiveTotalRange().start == VAllNNI.ZERO
        }
    }


    override fun removeFromWildcard(
        base: Wildcard,
        context: ResolvedComplexType,
        schema: ResolvedSchemaLike
    ): FlattenedParticle? {
        val effectiveTotalRange = effectiveTotalRange()
        if (! base.effectiveTotalRange().contains(effectiveTotalRange)) return null
        if (particles.any { base.consume(it, context, schema)==null }) return null
        return base - effectiveTotalRange
    }


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

        override fun effectiveTotalRange(): AllNNIRange {
            return particles.asSequence()
                .map { it.effectiveTotalRange() }
                .reduce { l, r -> l + r }
                .times(range)
        }

        fun restricts2(
            reference: FlattenedParticle,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike,
        ): Boolean {
            // case of 0 range should never happen
            return when (reference) {
                is Sequence -> restricts(reference.particles.first(), context, schema)

                /*
                                is Choice -> {
                                    reference.particles.any { this.restricts(it * reference.range, context, schema) }
                                }
                */

                is All -> {
                    var rng = reference.range.contains(range)

                    if (rng) {
                        val seenRefP = BooleanArray(reference.particles.size)
                        for (p in particles) {
                            val i = reference.particles.indexOfFirst { it.restricts(p, context, schema) }
                            if (i < 0) return false
                            seenRefP[i] = true
                        }
                        if (rng) {
                            for (i in seenRefP.indices) {
                                if (!seenRefP[i] && !reference.particles[i].isOptional) {
                                    rng = false
                                    break;
                                }
                            }
                        }
                    }
                    rng
                }

                is Wildcard -> {
                    reference.effectiveTotalRange().contains(effectiveTotalRange()) &&
                            particles.all { it.restrictsNoRange(reference, context, schema) }
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

        override fun consume(
            derived: FlattenedParticle,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? = derived.removeFromAll(this, context, schema)

        override fun removeFromWildcard(
            base: Wildcard,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? {
            if (minOccurs > base.maxOccurs) return null
            var b: FlattenedParticle? = base
            for (p in particles) {
                b = b?.consume(p, context, schema)
                if (b == null) return null
            }

            val effectiveTotalRange = effectiveTotalRange()
            if (! base.range.contains(effectiveTotalRange)) return null
            return b
        }

        override fun removeFromAll(
            base: All,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? {
            if (minOccurs > base.maxOccurs) return null
            val seenRefP = BooleanArray(base.particles.size)

            for (p in particles) {
                val i = base.particles.indexOfFirst { it.restricts(p, context, schema) }
                if (i < 0) return null
                seenRefP[i] = true
            }
            for (i in seenRefP.indices) {
                if (!seenRefP[i] && !base.particles[i].isOptional) {
                    return null
                }
            }

            return base - range
        }

        override fun times(otherRange: AllNNIRange): All {
            return All(range * otherRange, particles)
        }

        override fun minus(otherRange: AllNNIRange): All {
            return All(range.safeMinus(otherRange), particles)
        }

        override fun single(): All {
            return All(SINGLERANGE, particles)
        }

        override fun toString(): String = particles.joinToString(prefix = "{", postfix = range.toPostfix("}"))
    }

    class Choice(range: AllNNIRange, particles: List<FlattenedParticle>) :
        FlattenedGroup(range) {

        override val particles: List<FlattenedParticle>

        init {
            // when pedantic we "allow" single item choices (newer standard allows this to be eluded)
            if (!VALIDATE_PEDANTIC) require(particles.size > 1)

            // apply pointlessness rule
            val newParticles = particles.flatMap {
                if (it is Choice && it.range.isSimple) it.particles else listOf(it)
            }

            val seenNames = mutableSetOf<QName>()
            val seenWildcards = mutableListOf<ResolvedAny>()
            for (startElem in newParticles.flatMap { it.startingTerms() }) {
                when (startElem) {
                    is Element -> require(seenNames.add(startElem.term.mdlQName)) {
                        "Non-deterministic choice group: choice(${particles.joinToString(" | ")})"
                    }

                    is Wildcard -> require(seenWildcards.none { it.intersects(startElem.term) }) {
                        "Non-deterministic choice group: choice${particles.joinToString()}"
                    }
                }
            }
            this.particles = newParticles
        }

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

        fun restricts2(
            reference: FlattenedParticle,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike,
        ): Boolean {
            // case of 0 range should never happen
            return when (reference) {

                is Choice -> {
                    (reference.range.contains(range)) &&
                            particles.all { p -> reference.particles.any { p.restricts(it, context, schema) } }
                }

                is Wildcard -> {
                    reference.effectiveTotalRange().contains(effectiveTotalRange()) &&
                            particles.all { it.restricts(reference, context, schema) }
                }

                else -> false
            }
        }

        override fun consume(
            derived: FlattenedParticle,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? = derived.removeFromChoice(this, context, schema)

        override fun removeFromChoice(
            base: Choice,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? {
            if (minOccurs > base.maxOccurs) return null
            if (VALIDATE_PEDANTIC && !base.range.contains(range)) return null
            val newItems = arrayOfNulls <FlattenedParticle?>(base.particles.size)

            for (p in particles) {
                var match: FlattenedParticle? = null
                for (cIdx in base.particles.indices) {
                    if (newItems[cIdx] == null) {
                        match = base.particles[cIdx].consume(p, context, schema)
                        if (match != null) {
                            newItems[cIdx] = match
                            if (VALIDATE_PEDANTIC && match.minOccurs>VAllNNI.ZERO) return null
                            break
                        }
                    }
                }
                if(match == null) return null
            }

            val newParticles = newItems.filterNotNull()

            val newMin = base.minOccurs.safeMinus(minOccurs)
            val newMax = base.maxOccurs.safeMinus(maxOccurs, newMin)

            return Choice(newMin..newMax, newParticles)
        }

        override fun times(otherRange: AllNNIRange): Choice {
            return Choice(range * otherRange, particles)
        }

        override fun minus(otherRange: AllNNIRange): Choice {
            return Choice(range.safeMinus(otherRange), particles)
        }

        override fun single(): Choice {
            return Choice(SINGLERANGE, particles)
        }

        override fun toString(): String =
            particles.joinToString(separator = "| ", prefix = "(", postfix = range.toPostfix(")"))
    }

    open class Sequence internal constructor(
        range: AllNNIRange,
        final override val particles: List<FlattenedParticle>
    ) : FlattenedGroup(range) {

        override fun effectiveTotalRange(): AllNNIRange {
            return particles.asSequence()
                .map { it.effectiveTotalRange() }
                .reduce { l, r -> l + r }
                .times(range)
        }

        fun restricts2(
            reference: FlattenedParticle,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike,
        ): Boolean {
            return when (reference) {
                is Sequence -> restrictsSequence(reference, context, schema)

                is Choice -> { // check each side in turn (taking into account the particle range and choice range)
                    restrictsChoice(reference, context, schema)
                }

                is All -> {
                    if (!(reference.range.contains(range))) return false
                    val refCpy = reference.particles.toMutableList<FlattenedParticle?>()
                    for (e in particles) {
                        val matchIdx = refCpy.indexOfFirst { it != null && e.restricts(it, context, schema) }
                        if (matchIdx < 0) return false
                        refCpy[matchIdx] = null
                    }
                    for (r in refCpy) {
                        if (r != null && !r.isOptional) return false
                    }
                    true
                }

                is Wildcard -> {
                    reference.effectiveTotalRange().contains(effectiveTotalRange()) &&
                            particles.all {// cross-multiply ranges to make them equal
                                it.restrictsNoRange(reference, context, schema)
                            }
                }

                else -> false
            }
        }

        private fun restrictsChoice(
            reference: Choice,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): Boolean {
            if (!reference.effectiveTotalRange().contains(effectiveTotalRange())) return false
            // all particles in the sequence are present
            return particles.all { p -> p.restrictsNoRange(reference, context, schema) }
        }

        private fun restrictsSequence(
            reference: Sequence,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): Boolean {
            // Only if pedantic, otherwise we don't need range comparison
            if (VALIDATE_PEDANTIC && !reference.range.contains(range)) return false
            val refIt = reference.particles.iterator()
            if (!refIt.hasNext()) return false
            var currentParticle: FlattenedParticle = refIt.next()
            for (p in particles) {
                while (currentParticle.maxOccurs==VAllNNI.ZERO ||
                    ! p.restricts(currentParticle, context, schema)) {
                    // We can't go to the next particle
                    if (currentParticle.minOccurs>VAllNNI.ZERO) return false
                    if (!refIt.hasNext()) return false

                    currentParticle = refIt.next()
                }
                currentParticle = currentParticle - p.effectiveTotalRange()
            }
            // Tail that isn't optional
            if (currentParticle.minOccurs>VAllNNI.ZERO) return false

            // more tail
            while (refIt.hasNext()) {
                if (!refIt.next().isOptional) return false
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

        override fun consume(
            derived: FlattenedParticle,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? = derived.removeFromSequence(this, context, schema)

        override fun removeFromAll(
            base: All,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? {
            if (! base.range.contains(range)) return null

            val consumed = base.particles.toMutableList()

            val refIt = base.particles.iterator()
            if (!refIt.hasNext()) return null // empty never matches
            for (p in particles) {
                var found: Boolean = false
                for (i in consumed.indices) {
                    val r = consumed[i].consume(p, context, schema)
                    if (r != null) {
                        consumed[i] = r
                        found = true
                        break
                    }
                }
                if (!found) return null // no match
            }

            var isOptional = true
            val newConsumed = buildList {
                for (c in consumed) {
                    if (c.maxOccurs != VAllNNI.ZERO) {
                        if (!c.isOptional) isOptional = false
                        add(c)
                    }
                }
            }

            return when(newConsumed.size) {
                0 -> EMPTY
                1 -> newConsumed.single()
                else -> {
                    // note that base(all).range is never more than 1
                    val r = if (isOptional) OPTRANGE else base.range
                    All(r, newConsumed)
                }
            }
        }

        override fun removeFromChoice(
            base: Choice,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? {
            if (maxOccurs == VAllNNI.UNBOUNDED) {
                if (base.maxOccurs != VAllNNI.UNBOUNDED) return null
                // just check a single iteration against the (unbounded parent)
                return single().removeFromChoice(base, context, schema)
            }

            // TODO deal better with repeated sequences
            var baseIterCount = VAllNNI.ZERO
            var partial: FlattenedParticle? = null

            for (p in particles) {
                if (partial != null && partial.maxOccurs!= VAllNNI.ZERO) {
                    val wasOptional = partial.isOptional
                    partial = partial.consume(p, context, schema)
                    when {
                        partial != null -> continue
                        !wasOptional -> return null // incomplete match
                        // else just fall through with a new match
                    }
                }
                baseIterCount += VAllNNI.ONE
                if(baseIterCount>base.range.endInclusive) return null // range doesn't match
                for (c in base.particles) {
                    // partial will be null if reaching here
                    partial = c.consume(p, context, schema)
                    if (partial != null) break
                }
                if (partial == null) return null // no match found
            }
            invariantNotNull(partial)


            if (minOccurs * baseIterCount > base.maxOccurs) return null // can never be repeated sufficiently

            return when {
                baseIterCount == base.maxOccurs -> partial
                partial.maxOccurs == VAllNNI.ZERO -> {
                    val repeatedCount = range.start * baseIterCount
                    base - (repeatedCount..repeatedCount)
                }
                else -> { // The remaining current choice followed by the remaining iterations of the choice
                    val repeatedCount = range.start * baseIterCount
                    val tail = base - (repeatedCount..repeatedCount)
                    if (partial.isOptional && tail.isOptional) {
                        Sequence(OPTRANGE, listOf(partial, tail))
                    } else {
                        Sequence(SINGLERANGE, listOf(partial, tail))
                    }

                }
            }
        }

        override fun removeFromSequence(
            base: Sequence,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? {
            if (maxOccurs> VAllNNI.ZERO && !base.range.contains(range)) return null
            // Only if pedantic, otherwise we don't need range comparison
            if (particles.isEmpty()) return base // empty sequence is null opp

            val baseIt = base.particles.iterator()
            if (!baseIt.hasNext()) return null
            var currentParticle: FlattenedParticle = baseIt.next()
            for (p in particles) {
                while (true) {
                    if (currentParticle.maxOccurs != VAllNNI.ZERO) {
                        // can not complete current match
                        when (val l = currentParticle.consume(p, context, schema)) {
                            null -> if (!currentParticle.isOptional) return null
                            // fall through to go to next particle

                            else -> {
                                currentParticle = l
                                break
                            }
                        }
                    }
                    if (!baseIt.hasNext()) return null
                    currentParticle = baseIt.next()
                }
            }

            if (base.maxOccurs> VAllNNI.ONE) {
                if (! currentParticle.isOptional) return null
                while(baseIt.hasNext()) {
                    if (! baseIt.next().isOptional) return null
                }
                val count = minOf(base.maxOccurs, maxOccurs)
                return Sequence(base.range-count, base.particles)
            } else { // single distance sequences
                val newSeqContent = buildList {
                    if (currentParticle.maxOccurs>= VAllNNI.ZERO) add(currentParticle)
                    while (baseIt.hasNext()) {
                        add(baseIt.next())
                    }
                }

                return when (newSeqContent.size) {
                    0 -> EMPTY
                    1 -> newSeqContent.single()
                    else -> Sequence(SINGLERANGE, newSeqContent)
                }
            }
        }

        override fun times(otherRange: AllNNIRange): Sequence {
            // no need to validate again
            return Sequence(range * otherRange, particles)
        }

        override fun minus(otherRange: AllNNIRange): Sequence {
            // no need to validate again
            return Sequence(range.safeMinus(otherRange), particles)
        }

        override fun single(): Sequence {
            return Sequence(SINGLERANGE, particles)
        }

        override fun toString(): String = particles.joinToString(prefix = "(", postfix = range.toPostfix(")"))

    }

    companion object {
        @JvmStatic
        fun Sequence(
            range: AllNNIRange,
            particles: List<FlattenedParticle>,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle {
            // apply pointlessness rule
            val flattenedParticles = particles.flatMap { p->
                if (p is Sequence && p.range.isSimple) p.particles else listOf(p)
            }
            return when {
                flattenedParticles.isEmpty() -> EMPTY
                range.isSimple && flattenedParticles.size == 1 -> flattenedParticles.single()
                else -> {
                    checkSequence(flattenedParticles, context, schema)
                    Sequence(range, flattenedParticles)
                }
            }
        }

        private fun checkSequence(
            particles: List<FlattenedParticle>,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
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
                            require(lastAnys.none { it.matches(startName, context, schema) })
                        }

                        is Wildcard -> {
                            require(lastAnys.none { it.intersects(startTerm.term) }) {
                                "Non-deterministic choice group: ${particles.joinToString()}"
                            }
                            require(lastOptionals.none { startTerm.term.matches(it, context, schema) }) {
                                "Non-deterministic choice group: ${particles.joinToString()}"
                            }
                        }
                    }
                }



                lastOptionals = mutableListOf()
                lastAnys = mutableListOf()

                when {
                    p.isOptional && p.isVariable -> for (e in p.trailingTerms()) {
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


    }

}

sealed class FlattenedParticle(val range: AllNNIRange) {

    val maxOccurs get() = range.endInclusive
    val minOccurs get() = range.start

    val isOptional: Boolean get() = minOccurs == VAllNNI.ZERO
    val isVariable: Boolean get() = minOccurs != maxOccurs

    abstract fun effectiveTotalRange(): AllNNIRange

    abstract operator fun times(otherRange: AllNNIRange): FlattenedParticle
    abstract operator fun minus(otherRange: AllNNIRange): FlattenedParticle

    abstract fun startingTerms(): List<Term>
    abstract fun trailingTerms(): List<Term>

    abstract fun single(): FlattenedParticle

    /**
     * @param derived The particle to be consumed from this particle (if possible).
     * @return `null` if nothing can be consumed, or a new particle that is "consumed"
     */
    abstract fun consume(derived: FlattenedParticle, context: ResolvedComplexType, schema: ResolvedSchemaLike): FlattenedParticle?

    open fun restrictsNoRange(
        reference: FlattenedParticle,
        context: ResolvedComplexType,
        schema: ResolvedSchemaLike
    ): Boolean {
        return single().restricts(reference.single(), context, schema)
    }

    fun restricts(
        reference: FlattenedParticle,
        context: ResolvedComplexType,
        schema: ResolvedSchemaLike,
    ): Boolean {
        val r = reference.consume(this, context, schema)
        return r != null && r.effectiveTotalRange().start == VAllNNI.ZERO
    }

    open fun removeFromElement(base: Element, context: ResolvedComplexType, schema: ResolvedSchemaLike): FlattenedParticle? = null

    open fun removeFromWildcard(base: Wildcard, context: ResolvedComplexType, schema: ResolvedSchemaLike): FlattenedParticle? = null

    open fun removeFromAll(base: All, context: ResolvedComplexType, schema: ResolvedSchemaLike): FlattenedParticle? = null

    open fun removeFromChoice(base: Choice, context: ResolvedComplexType, schema: ResolvedSchemaLike): FlattenedParticle? = null

    open fun removeFromSequence(base: Sequence, context: ResolvedComplexType, schema: ResolvedSchemaLike): FlattenedParticle? = null

    sealed class Term(range: AllNNIRange) : FlattenedParticle(range) {
        abstract val term: ResolvedBasicTerm

        override fun effectiveTotalRange(): AllNNIRange = range

        abstract override fun single(): Term

        companion object {
            operator fun invoke(range: AllNNIRange, term: ResolvedBasicTerm): FlattenedParticle = when (term) {
                is ResolvedElement -> Element(range, term)
                is ResolvedAny -> Wildcard(range, term)
            }
        }
    }

    class Element internal constructor(range: AllNNIRange, override val term: ResolvedElement, dummy: Boolean) :
        Term(range) {
        override fun startingTerms(): List<Element> {
            return listOf(this)
        }

        override fun trailingTerms(): List<Element> = listOf(this)

        fun restricts2(
            reference: FlattenedParticle,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike,
        ): Boolean = when (reference) {
            is Element -> when {
                !(reference.range.contains(range)) -> false
                else -> reference.term.mdlQName.isEquivalent(term.mdlQName)
            }

            is Wildcard -> reference.range.contains(range) && reference.term.matches(term.mdlQName, context, schema)

            is Sequence -> restrictsSequence(reference, context, schema)

            is All -> {
                reference.range.contains(SINGLERANGE) &&
                        reference.particles.all {
                            it.isOptional ||
                                    this.restricts(it * reference.range, context, schema)
                        }
            }

            is Choice -> {
                // this would be a 1..1 group
                reference.range.contains(SINGLERANGE) &&
                        reference.particles.any { this.restricts(it, context, schema) }
            }

            else -> false
        }

        private fun restrictsSequence(
            sequence: Sequence,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): Boolean {
            val it = sequence.particles.iterator()
            var match: FlattenedParticle? = null
            while (match == null && it.hasNext()) {
                match = it.next().takeIf { this.restrictsNoRange(it, context, schema) }
            }
            if (match == null) return false
            while (it.hasNext()) {
                if (!it.next().isOptional) return false
            }
            return true
        }

        override fun consume(
            derived: FlattenedParticle,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? = derived.removeFromElement(this, context, schema)

        override fun removeFromElement(
            base: Element,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? {
            if (minOccurs > base.maxOccurs) return null
            if (! base.term.mdlQName.isEquivalent(term.mdlQName)) return null
            val newStart = base.minOccurs.safeMinus(minOccurs)
            val newEnd = base.maxOccurs.safeMinus(maxOccurs, newStart)
            return Element(newStart..newEnd, base.term)
        }

        override fun removeFromWildcard(
            base: Wildcard,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? {
            if (minOccurs > base.maxOccurs) return null
            if (! base.term.matches(term.mdlQName, context, schema)) return null
            val start = base.minOccurs.safeMinus(minOccurs)
            return Wildcard(start..base.maxOccurs.safeMinus(maxOccurs, start), base.term)
        }

        override fun removeFromAll(
            base: All,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? {
            return All(SINGLERANGE, listOf(this)).removeFromAll(base, context, schema)
        }

        override fun removeFromChoice(
            base: Choice,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? {
            return Choice(SINGLERANGE, listOf(this)).removeFromChoice(base, context, schema)
        }

        override fun removeFromSequence(
            base: Sequence,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? {
            return Sequence(SINGLERANGE, listOf(this)).removeFromSequence(base, context, schema)
        }

        override fun times(otherRange: AllNNIRange): Element {
            return Element(range * otherRange, term, true)
        }

        override fun minus(otherRange: AllNNIRange): Element {
            return Element(range.minus(otherRange), term, true)
        }

        override fun single(): Element = Element(SINGLERANGE, term, true)

        override fun toString(): String = range.toPostfix(term.mdlQName.toString())


    }

    class Wildcard(range: AllNNIRange, override val term: ResolvedAny) : Term(range) {

        override fun startingTerms(): List<Wildcard> {
            return listOf(this)
        }

        override fun trailingTerms(): List<Wildcard> = listOf(this)

        fun restricts2(
            reference: FlattenedParticle,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): Boolean {
            if (!reference.range.contains(range)) return false
            return when (reference) {
                is FlattenedGroup -> when {
                    !reference.effectiveTotalRange().contains(range) -> false

                    // duplicate checking for child range, but we don't want the complexity
                    else -> reference.particles.all { restrictsNoRange(it, context, schema) }
                }

                is Element -> false // wildcards can never restrict an element
                //term.matches(reference.term.mdlQName, context, schema)

                is Wildcard -> reference.term.mdlNamespaceConstraint.isSupersetOf(term.mdlNamespaceConstraint)
                else -> error("Unsupported particle kind: $reference")
            }
        }

        override fun consume(
            derived: FlattenedParticle,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? = derived.removeFromWildcard(this, context, schema)

        override fun removeFromWildcard(
            base: Wildcard,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? {
            if (minOccurs>base.maxOccurs) return null
            if (base.term.mdlNamespaceConstraint.isSupersetOf(term.mdlNamespaceConstraint)) {
                return base - range
            }
            return null
        }

        override fun times(otherRange: AllNNIRange): Wildcard {
            return Wildcard(range * otherRange, term)
        }

        override fun minus(otherRange: AllNNIRange): Wildcard {
            return Wildcard(range.safeMinus(otherRange), term)
        }

        override fun single(): Wildcard = Wildcard(SINGLERANGE, term)

        override fun toString(): String = range.toPostfix("<${term.mdlNamespaceConstraint}>")
    }

    companion object {
        private val INFRANGE: AllNNIRange = VAllNNI.ZERO..VAllNNI.UNBOUNDED
        internal val SINGLERANGE: AllNNIRange = VAllNNI.ONE..VAllNNI.ONE
        internal val OPTRANGE: AllNNIRange = VAllNNI.ZERO..VAllNNI.ONE

        @JvmStatic
        fun Element(range: AllNNIRange, term: ResolvedElement): FlattenedParticle = when {
            term !is ResolvedGlobalElement ||
                    term.mdlSubstitutionGroupMembers.isEmpty()
            -> Element(range, term, true)

            else -> {
                val elems = term.fullSubstitutionGroup().map { Element(SINGLERANGE, it, true) }
                Choice(range, elems)
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
            endInclusive == VAllNNI.ZERO -> prefix+"[0]"
            start == VAllNNI.ZERO -> prefix + '?'
            else -> prefix // both are 1
        }
    }


}


/** Safe minus operator that keeps values in range */
private fun AllNNIRange.safeMinus(other: AllNNIRange): AllNNIRange {
    val newStart = when {
        other.start > start -> VAllNNI.ZERO
        else -> other.start - start
    }
    val newEnd = when {
        other.endInclusive > (endInclusive + newStart) -> newStart
        else -> other.endInclusive - endInclusive
    }
    return AllNNIRange(newStart, endInclusive - other.start)
}

