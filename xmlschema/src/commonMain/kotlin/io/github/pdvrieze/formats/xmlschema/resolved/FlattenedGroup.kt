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
import io.github.pdvrieze.formats.xmlschema.resolved.FlattenedGroup.All
import io.github.pdvrieze.formats.xmlschema.resolved.FlattenedGroup.Choice
import io.github.pdvrieze.formats.xmlschema.resolved.FlattenedGroup.Sequence
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

        override fun restricts(
            reference: FlattenedParticle,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): Boolean {
            return reference.isEmptiable
        }

    }

    override val isEmptiable: Boolean
        get() = minOccurs == VAllNNI.ZERO || effectiveTotalRange().start == VAllNNI.ZERO

    // Implements recurse (seq-seq or all-all)
    protected fun restrictsRecurse(
        base: FlattenedGroup,
        context: ResolvedComplexType,
        schema: ResolvedSchemaLike
    ): Boolean = when (schema.version) {
        ResolvedSchema.Version.V1_0 -> restrictsRecurse10(base, context, schema)
        else -> restrictsRecurse11(base, context, schema)
    }

    private fun restrictsRecurse10(
        base: FlattenedGroup,
        context: ResolvedComplexType,
        schema: ResolvedSchemaLike
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
                if (p.restricts(basePart, context, schema)) break

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
        context: ResolvedComplexType,
        schema: ResolvedSchemaLike
    ): Boolean {
        return (base.remove(this, context, schema) ?: return false).isEmptiable
    }

    // implements NSRecurse-CheckCardinality
    override fun restrictsWildcard(
        base: Wildcard,
        context: ResolvedComplexType,
        schema: ResolvedSchemaLike
    ): Boolean {
        // NSRecurse-CheckCardinality 2
        if (!base.effectiveTotalRange().contains(effectiveTotalRange())) return false

        // NSRecurse-CheckCardinality 1 // ignore count here as it will not match
        return particles.all { it.single().restricts(base.single(), context, schema) }
    }


    abstract val particles: List<FlattenedParticle>

    class All(range: AllNNIRange, override val particles: List<FlattenedParticle>) :
        FlattenedGroup(range) {

        constructor(range: AllNNIRange, particles: List<FlattenedParticle>, version: ResolvedSchema.Version) : this(
            range,
            when (version) {
                ResolvedSchema.Version.V1_0 -> particles
                else -> particles.sortedWith(particleComparator)
            }
        )

        override fun effectiveTotalRange(): AllNNIRange {
            return particles.asSequence()
                .map { it.effectiveTotalRange() }
                .reduce { l, r -> l + r }
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
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): Boolean = other.restrictsAll(this, context, schema)

        /*
        override fun removeFromWildcard(
            base: Wildcard,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? {
            if (minOccurs > base.maxOccurs) return null
            var b: FlattenedParticle? = base
            for (p in particles) {
                if (!p.restricts(base, context, schema)) return null
                b = b?.consume(p, context, schema)
                if (b == null) return null
            }

            val effectiveTotalRange = effectiveTotalRange()
            if (! base.range.contains(effectiveTotalRange)) return null
            return b
        }
*/

        /**
         * Recurse
         */
        override fun restrictsAll(base: All, context: ResolvedComplexType, schema: ResolvedSchemaLike): Boolean {
            return restrictsRecurse(base, context, schema)
        }

        override fun remove(
            reference: FlattenedParticle,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? {
            return reference.removeFromAll(this, context, schema)
        }

        override fun removeFromAll(
            base: All,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? {
            if (minOccurs > base.maxOccurs) return null

            val baseIt = base.particles.iterator()

            for (p in particles) {
                var match = false
                while (!match) {
                    if (!baseIt.hasNext()) return null
                    val basePart = baseIt.next()
                    if (!p.restricts(basePart, context, schema)) {
                        if (!basePart.isEmptiable) return null
                    } else {
                        match = true
                    }
                }
                if (!match) return null
            }
            while (baseIt.hasNext()) {
                if (!baseIt.next().isEmptiable) return null
            }

            return EMPTY
        }

        override fun single(): All {
            return All(SINGLERANGE, particles)
        }

        override fun times(range: AllNNIRange): All? {
            return this.range.times(range)?.let { All(it, particles) }
        }

        override fun minus(range: AllNNIRange): FlattenedParticle? {
            return this.range.minus(range)?.let { All(it, particles) }
        }

        override fun toString(): String = particles.joinToString(prefix = "{", postfix = range.toPostfix("}"))
    }

    class Choice(range: AllNNIRange, override val particles: List<FlattenedParticle>) :
        FlattenedGroup(range) {

        constructor(range: AllNNIRange, particles: List<FlattenedParticle>, version: ResolvedSchema.Version) : this(
            range,
            when (version) {
                ResolvedSchema.Version.V1_0 -> particles
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
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): Boolean = other.restrictsChoice(this, context, schema)

        // Recurse lax
        override fun restrictsChoice(base: Choice, context: ResolvedComplexType, schema: ResolvedSchemaLike): Boolean {
            if (!base.range.contains(range)) return false

            val baseIt = base.particles.iterator()

            for (p in particles) {
                while (true) {
                    if (!baseIt.hasNext()) return false
                    if (p.restricts(baseIt.next(), context, schema)) break
                }
            } // this doesn't need to check emptiability

            return true
        }

        override fun remove(
            reference: FlattenedParticle,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? {
            return reference.removeFromChoice(this, context, schema)
        }

        override fun removeFromChoice(
            base: Choice,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? {
            if (!base.range.contains(range)) return null

            val baseIt = base.particles.iterator()

            for (p in particles) {
                var match = false
                while (!match) {
                    if (!baseIt.hasNext()) return null
                    val basePart = baseIt.next()
                    if (p.restricts(basePart, context, schema)) {
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
            return Choice(SINGLERANGE, particles)
        }

        override fun times(range: AllNNIRange): Choice? {
            return (this.range * range)?.let { Choice(it, particles) }
        }

        override fun minus(range: AllNNIRange): FlattenedParticle? {
            return this.range.minus(range)?.let { Choice(it, particles) }
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
                .let { it.start * range.start..it.endInclusive * range.endInclusive }
        }

        override fun startingTerms(): List<Term> {
            return particles.firstOrNull()?.startingTerms() ?: emptyList()
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
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): Boolean = other.restrictsSequence(this, context, schema)

        // Restrict recurseUnordered
        override fun restrictsAll(base: All, context: ResolvedComplexType, schema: ResolvedSchemaLike): Boolean {
            if (!base.range.contains(range)) return false // 1

            val unprocessed = base.particles.toMutableList<FlattenedParticle?>() // 2.1

            for (p in particles) { // 2.2
                val matchIdx = unprocessed.indexOfFirst { it != null && p.restricts(it, context, schema) }
                if (matchIdx < 0) return false
                unprocessed[matchIdx] = null // 2.1
            }

            for (bp in unprocessed) { // 2.3
                if (bp != null && !bp.isEmptiable) return false
            }
            return true
        }

        /**
         * MapAndSum
         */
        override fun restrictsChoice(base: Choice, context: ResolvedComplexType, schema: ResolvedSchemaLike): Boolean {
            // MapAndSum 2
            val partSize = VAllNNI.Value(particles.size.toUInt())
            if (!base.range.contains((minOccurs * partSize)..(maxOccurs * partSize))) return false

            val minValues = Array(base.particles.size) { VAllNNI.ZERO }
            val maxValues = Array<VAllNNI>(base.particles.size) { VAllNNI.ZERO }

            // TODO implement "unfolding"
            for (p in particles) {
                val matchIdx = base.particles.indexOfFirst { p.single().restricts(it.single(), context, schema) }
                if (matchIdx < 0) return false
                val newConsumed = maxValues[matchIdx] + (p.maxOccurs * maxOccurs)
                val match = base.particles[matchIdx]
                if (newConsumed > (match.maxOccurs * base.maxOccurs)) return false // matches should be disjunct
                maxValues[matchIdx] = newConsumed
                minValues[matchIdx] += (p.minOccurs * minOccurs)
            }

            for (i in base.particles.indices) { // if consumed (and therefore maxValues>0) it must be within the range
                if (maxValues[i] > VAllNNI.ZERO) {
                    val collapsedRange = base.particles[i].range * base.range
                    if (collapsedRange?.contains(minValues[i]..maxValues[i]) != true) return false
                }
            }

            return true
        }

        override fun removeFromChoice(
            base: Choice,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? {
            val partSize = VAllNNI.Value(particles.size.toUInt())
            if (!base.range.contains((minOccurs * partSize)..(maxOccurs * partSize))) return null

            val baseIt = base.particles.iterator()
            // TODO deal better with repeated sequences
            for (p in particles) {
                if (base.particles.none { p.restricts(it, context, schema) }) return null
            }
            return null
        }

        override fun restrictsSequence(
            base: Sequence,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): Boolean {
            return restrictsRecurse(base, context, schema)
        }

        override fun remove(
            reference: FlattenedParticle,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? {
            return reference.removeFromSequence(this, context, schema)
        }

        override fun removeFromSequence(
            base: Sequence,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? {
            if (base.maxOccurs > VAllNNI.ONE) {
                val singleReduction = single().removeFromSequence(base.single(), context, schema)
                if (singleReduction != null && singleReduction.isEmptiable) {
                    // The sequences "match"
                    return base - range
                }
                val head = Sequence(base.minOccurs.. VAllNNI.ONE, base.particles)
                val tail = (base - SINGLERANGE)!!
                val reducedHead = removeFromSequence(head, context, schema) ?: return null
                return Sequence(SINGLERANGE, listOf(reducedHead, tail))
            } else { //base is optional or simple
                if (maxOccurs > VAllNNI.ONE) {
                    return Sequence(SINGLERANGE, listOf(this)).removeFromSequence(base, context, schema)
                }
                if (minOccurs == VAllNNI.ZERO && base.minOccurs == VAllNNI.ONE) return null
                val baseIt = base.particles.iterator()
                var pending: FlattenedParticle? = null
                for (p in particles) {
                    while(true) {
                        val bp = pending ?: if (baseIt.hasNext()) baseIt.next() else return null
                        pending = null
                        val reduced = bp.remove(p, context, schema)
                        if (reduced == null) {
                            if (!bp.isEmptiable) return null
                            if (p is Choice) {
                                var choiceCount = 0
                                val choiceMembers = ArrayList<FlattenedParticle?>(p.particles)
                                var bp2 = bp
                                while (true) {
                                    val i = choiceMembers.indexOfFirst { it != null && it.restricts(bp2, context, schema) }
                                    if (i < 0) {
                                        if (!bp.isEmptiable) return null
                                    } else {
                                        choiceMembers[i] = null
                                        choiceCount++
                                    }
                                    if (choiceCount < choiceMembers.size) {
                                        if (!baseIt.hasNext()) return null
                                        bp2 = baseIt.next()
                                    } else {
                                        break
                                    }
                                }
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
                    else -> Sequence(SINGLERANGE, newParticles)
                }
            }
        }

        override fun single(): Sequence {
            return Sequence(SINGLERANGE, particles)
        }

        override fun times(range: AllNNIRange): Sequence? {
            return (this.range * range)?.let { Sequence(it, particles) }
        }

        override fun minus(range: AllNNIRange): FlattenedParticle? {
            return this.range.minus(range)?.let { Sequence(it, particles) }
        }

        override fun toString(): String = particles.joinToString(prefix = "(", postfix = range.toPostfix(")"))

    }

    companion object {


        // TODO move to IResolvedSequence
        internal fun checkSequence(
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
                            if (schema.version == ResolvedSchema.Version.V1_0) {
                                // In version 1.1 resolving prioritises explicit elements, wildcards can omit
                                require(lastAnys.none { it.matches(startName, context, schema) }) {
                                    "Ambiguous sequence $startName - ${lastAnys}"
                                }
                            }
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
                    p.isEmptiable && p.isVariable -> for (e in p.trailingTerms()) {
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

    open val isEmptiable: Boolean get() = minOccurs == VAllNNI.ZERO
    val isVariable: Boolean get() = minOccurs != maxOccurs

    abstract fun effectiveTotalRange(): AllNNIRange

    abstract fun startingTerms(): List<Term>
    abstract fun trailingTerms(): List<Term>

    abstract fun single(): FlattenedParticle

    protected abstract fun isRestrictedBy(
        other: FlattenedParticle,
        context: ResolvedComplexType,
        schema: ResolvedSchemaLike
    ): Boolean

    open fun restricts(
        reference: FlattenedParticle,
        context: ResolvedComplexType,
        schema: ResolvedSchemaLike,
    ): Boolean {
        return reference.isRestrictedBy(this, context, schema)
    }

    open fun restrictsElement(base: Element, context: ResolvedComplexType, schema: ResolvedSchemaLike): Boolean = false

    open fun restrictsWildcard(base: Wildcard, context: ResolvedComplexType, schema: ResolvedSchemaLike): Boolean =
        false

    open fun restrictsAll(base: All, context: ResolvedComplexType, schema: ResolvedSchemaLike): Boolean = false

    open fun restrictsChoice(base: Choice, context: ResolvedComplexType, schema: ResolvedSchemaLike): Boolean = false

    open fun restrictsSequence(base: Sequence, context: ResolvedComplexType, schema: ResolvedSchemaLike): Boolean =
        false

    abstract operator fun times(range: AllNNIRange): FlattenedParticle?

    abstract fun remove(
        reference: FlattenedParticle,
        context: ResolvedComplexType,
        schema: ResolvedSchemaLike
    ): FlattenedParticle?

    open fun removeFromElement(
        reference: Element,
        context: ResolvedComplexType,
        schema: ResolvedSchemaLike
    ): FlattenedParticle? = null

    open fun removeFromWildcard(
        reference: Wildcard,
        context: ResolvedComplexType,
        schema: ResolvedSchemaLike
    ): FlattenedParticle? = null

    open fun removeFromAll(
        reference: All,
        context: ResolvedComplexType,
        schema: ResolvedSchemaLike
    ): FlattenedParticle? = null

    open fun removeFromChoice(
        reference: Choice,
        context: ResolvedComplexType,
        schema: ResolvedSchemaLike
    ): FlattenedParticle? = null

    open fun removeFromSequence(
        reference: Sequence,
        context: ResolvedComplexType,
        schema: ResolvedSchemaLike
    ): FlattenedParticle? = null

    sealed class Term(range: AllNNIRange) : FlattenedParticle(range) {
        abstract val term: ResolvedBasicTerm

        override fun effectiveTotalRange(): AllNNIRange = range

        abstract override fun single(): Term
    }

    class Element internal constructor(range: AllNNIRange, override val term: ResolvedElement, dummy: Boolean) :
        Term(range) {
        override fun startingTerms(): List<Element> {
            return listOf(this)
        }

        override fun trailingTerms(): List<Element> = listOf(this)

        override fun isRestrictedBy(
            other: FlattenedParticle,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): Boolean = other.restrictsElement(this, context, schema)

        override fun restrictsElement(
            base: Element,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): Boolean {
            if (!base.range.contains(range)) return false

            if (!base.term.mdlQName.isEquivalent(term.mdlQName)) return false

            return base.term.subsumes(term)
        }

        // Implements NSCompat
        override fun restrictsWildcard(
            base: Wildcard,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): Boolean {
            // NSCompat 2
            if (!base.range.contains(range)) return false

            // NSCompat 1
            return base.term.matches(term.mdlQName, context, schema)

        }

        override fun restrictsAll(base: All, context: ResolvedComplexType, schema: ResolvedSchemaLike): Boolean {
            return All(SINGLERANGE, listOf(this)).restrictsAll(base, context, schema)
        }

        override fun restrictsChoice(base: Choice, context: ResolvedComplexType, schema: ResolvedSchemaLike): Boolean {
            // The option to do it either way is valid for 1.1
            return Choice(SINGLERANGE, listOf(this)).restrictsChoice(base, context, schema) ||
                    (schema.version == ResolvedSchema.Version.V1_1 &&
                            Choice(range, listOf(single())).restricts(base, context, schema))
        }

        override fun restrictsSequence(
            base: Sequence,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): Boolean {
            return Sequence(SINGLERANGE, listOf(this)).restrictsSequence(base, context, schema)
        }

        override fun remove(
            reference: FlattenedParticle,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? {
            return reference.removeFromElement(this, context, schema)
        }

        override fun removeFromElement(
            reference: Element,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? {
            if (!reference.term.mdlQName.isEquivalent(term.mdlQName)) return null

            if (!reference.term.subsumes(term)) return null

            return reference.minus(range)
        }

        override fun removeFromWildcard(
            reference: Wildcard,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? {
            if (!reference.term.matches(term.mdlQName, context, schema)) return null

            return reference - range
        }

        override fun removeFromAll(
            reference: All,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? {
            return super.removeFromAll(reference, context, schema)
        }

        override fun removeFromChoice(
            reference: Choice,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? {
            val matchIdx = reference.particles.indexOfFirst { it.single().isRestrictedBy(single(), context, schema) }
            if (matchIdx < 0) return null
            val match = reference.particles[matchIdx]
            return if (maxOccurs == VAllNNI.UNBOUNDED) {
                when {
                    match.maxOccurs == VAllNNI.UNBOUNDED -> reference - SINGLERANGE
                    reference.maxOccurs == VAllNNI.UNBOUNDED -> FlattenedGroup.EMPTY
                    else -> null
                }
            } else { // consider further options
                when {
                    match.range.contains(range) -> reference - SINGLERANGE
                    match.range.isSimple -> reference - range
                    else -> null // TODO a bit more options
                }
            }
        }

        override fun removeFromSequence(
            reference: Sequence,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? {
            if (reference.maxOccurs > VAllNNI.ONE) { // handle the case that there is more than 1 iteration
                val head = Sequence(reference.minOccurs..VAllNNI.ONE, reference.particles)
                val tail = (reference - SINGLERANGE) ?: error("Should not happen because maxOccurs>0")
                val trimmedHead = removeFromSequence(head, context, schema) ?: return null
                if (trimmedHead.maxOccurs == VAllNNI.ZERO) return tail
                return Sequence(SINGLERANGE, listOf(trimmedHead, tail))
            }
            val partIt = reference.particles.iterator()
            val newParticles = mutableListOf<FlattenedParticle>()
            while (partIt.hasNext()) {
                val part = partIt.next()
                val removed = part.remove(this, context, schema)
                when {
                    removed == null -> if (!part.isEmptiable) return null
                    removed.maxOccurs > VAllNNI.ZERO -> {
                        newParticles.add(removed)
                        break
                    }
                }
            }
            while (partIt.hasNext()) {
                newParticles.add(partIt.next())
            } // flush remaining particles

            // We consumed part of the sequence so it must occur
            return Sequence(SINGLERANGE, newParticles)
        }

        override fun single(): Element = Element(SINGLERANGE, term, true)

        override fun times(range: AllNNIRange): Element? {
            return (this.range * range)?.let { Element(it, term, true) }
        }

        override fun minus(range: AllNNIRange): FlattenedParticle? {
            return this.range.minus(range)?.let { Element(it, term, true) }
        }

        override fun toString(): String = range.toPostfix(term.mdlQName.toString())


    }

    abstract operator fun minus(range: AllNNIRange): FlattenedParticle?

    class Wildcard(range: AllNNIRange, override val term: ResolvedAny) : Term(range) {

        override fun startingTerms(): List<Wildcard> {
            return listOf(this)
        }

        override fun trailingTerms(): List<Wildcard> = listOf(this)

        override fun isRestrictedBy(
            other: FlattenedParticle,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): Boolean = other.restrictsWildcard(this, context, schema)

        /**
         * NSSubset
         */
        override fun restrictsWildcard(
            base: Wildcard,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): Boolean {
            // NSSubset 1
            if (!base.range.contains(range)) return false

            // NSSubset 2, subset per Schema 1 - 3.10.6
            if (!term.mdlNamespaceConstraint.isSubsetOf(base.term.mdlNamespaceConstraint)) return false

            // NSSubset 3, (exception for the ur-wildcard is needed - although a shortcut may apply by just always
            // restricting AnyType)
            return base.term === AnyType.urWildcard || term.mdlProcessContents >= base.term.mdlProcessContents
        }

        override fun remove(
            reference: FlattenedParticle,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? {
            return reference.removeFromWildcard(this, context, schema)
        }

        override fun removeFromWildcard(
            reference: Wildcard,
            context: ResolvedComplexType,
            schema: ResolvedSchemaLike
        ): FlattenedParticle? {
            if (!reference.range.contains(range)) return null
            if (!term.mdlNamespaceConstraint.isSubsetOf(reference.term.mdlNamespaceConstraint)) return null
            if (reference.term !== AnyType.urWildcard && term.mdlProcessContents < reference.term.mdlProcessContents) return null
            return reference - range
        }

        override fun single(): Wildcard = Wildcard(SINGLERANGE, term)

        override fun times(range: AllNNIRange): Wildcard? {
            return (this.range * range)?.let { Wildcard(it, term) }
        }

        override fun minus(range: AllNNIRange): FlattenedParticle? {
            return this.range.minus(range)?.let { Wildcard(it, term) }
        }

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
                Choice(range, elems, ResolvedSchema.Version.V1_1) // force 1.1 as substitution groups are not ordered
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
