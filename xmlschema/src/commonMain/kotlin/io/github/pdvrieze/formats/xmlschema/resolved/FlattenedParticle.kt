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

package io.github.pdvrieze.formats.xmlschema.resolved

import io.github.pdvrieze.formats.xmlschema.datatypes.AnyType
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.AllNNIRange
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.isEquivalent
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.namespaceURI
import kotlin.jvm.JvmStatic

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

    open fun extendsAll(base: FlattenedGroup.All, context: ContextT, schema: ResolvedSchemaLike): Boolean = false

    open fun extendsChoice(base: FlattenedGroup.Choice, context: ContextT, schema: ResolvedSchemaLike): Boolean = false

    open fun extendsSequence(base: FlattenedGroup.Sequence, isSiblingName: (QName) -> Boolean, schema: ResolvedSchemaLike): Boolean =
        false

    open fun restrictsElement(base: Element, context: ContextT, checkHelper: CheckHelper): Boolean = false

    open fun restrictsWildcard(base: Wildcard, isSiblingName: ContextT, checkHelper: CheckHelper): Boolean =
        false

    open fun restrictsAll(base: FlattenedGroup.All, isSiblingName: ContextT, checkHelper: CheckHelper): Boolean = false

    open fun restrictsChoice(base: FlattenedGroup.Choice, isSiblingName: ContextT, checkHelper: CheckHelper): Boolean = false

    open fun restrictsSequence(base: FlattenedGroup.Sequence, context: ContextT, checkHelper: CheckHelper): Boolean =
        false

    abstract operator fun times(range: AllNNIRange): FlattenedParticle?

    abstract fun remove(
        reference: FlattenedParticle,
        isSiblingName: ContextT,
        checkHelper: CheckHelper
    ): FlattenedParticle?

    open fun removeFromElement(
        reference: Element,
        context: ContextT,
        checkHelper: CheckHelper
    ): FlattenedParticle? = null

    open fun removeFromWildcard(
        reference: Wildcard,
        isSiblingName: ContextT,
        checkHelper: CheckHelper
    ): FlattenedParticle? = null

    open fun removeFromAll(
        reference: FlattenedGroup.All,
        isSiblingName: ContextT,
        checkHelper: CheckHelper
    ): FlattenedParticle? = null

    open fun removeFromChoice(
        reference: FlattenedGroup.Choice,
        isSiblingName: ContextT,
        checkHelper: CheckHelper
    ): FlattenedParticle? = null

    open fun removeFromSequence(
        reference: FlattenedGroup.Sequence,
        isSiblingName: ContextT,
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
            isSiblingName: ContextT,
            checkHelper: CheckHelper
        ): Boolean {
            // NSCompat 2
            if (!base.range.contains(range)) return false

            // NSCompat 1
            return base.term.matches(term.mdlQName, isSiblingName, checkHelper.schema)

        }

        override fun restrictsAll(base: FlattenedGroup.All, isSiblingName: ContextT, checkHelper: CheckHelper): Boolean {
            return FlattenedGroup.All(AllNNIRange.SINGLERANGE, listOf(this))
                .restrictsAll(base, isSiblingName, checkHelper)
        }

        override fun restrictsChoice(base: FlattenedGroup.Choice, isSiblingName: ContextT, checkHelper: CheckHelper): Boolean {
            // The option to do it either way is valid for 1.1
            return FlattenedGroup.Choice(AllNNIRange.SINGLERANGE, listOf(this))
                .restrictsChoice(base, isSiblingName, checkHelper) ||
                    (checkHelper.version == SchemaVersion.V1_1 &&
                            FlattenedGroup.Choice(range, listOf(single())).restricts(base, isSiblingName, checkHelper))
        }

        override fun restrictsSequence(
            base: FlattenedGroup.Sequence,
            context: ContextT,
            checkHelper: CheckHelper
        ): Boolean {
            return FlattenedGroup.Sequence(AllNNIRange.SINGLERANGE, listOf(this))
                .restrictsSequence(base, context, checkHelper)
        }

        override fun remove(
            reference: FlattenedParticle,
            isSiblingName: ContextT,
            checkHelper: CheckHelper
        ): FlattenedParticle? {
            return reference.removeFromElement(this, isSiblingName, checkHelper)
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
            isSiblingName: ContextT,
            checkHelper: CheckHelper
        ): FlattenedParticle? {
            if (!reference.term.matches(term.mdlQName, isSiblingName, checkHelper.schema)) return null

            return reference - range
        }

        override fun removeFromAll(
            reference: FlattenedGroup.All,
            isSiblingName: ContextT,
            checkHelper: CheckHelper
        ): FlattenedParticle? {
            return super.removeFromAll(reference, isSiblingName, checkHelper)
        }

        override fun removeFromChoice(
            reference: FlattenedGroup.Choice,
            isSiblingName: ContextT,
            checkHelper: CheckHelper
        ): FlattenedParticle? {
            val matchIdx = reference.particles.indexOfFirst { it.single().isRestrictedBy(single(),
                isSiblingName, checkHelper) }
            if (matchIdx < 0) return null
            val match = reference.particles[matchIdx]
            return if (maxOccurs == VAllNNI.UNBOUNDED) {
                when {
                    match.maxOccurs == VAllNNI.UNBOUNDED -> reference - AllNNIRange.SINGLERANGE
                    reference.maxOccurs == VAllNNI.UNBOUNDED -> FlattenedGroup.EMPTY
                    else -> match.remove(this, isSiblingName, checkHelper) // perhaps inside the match it is possible
                }
            } else { // consider further options
                when {
                    match.minOccurs * reference.minOccurs > minOccurs -> (match * reference.range)?.minus(range)
                    match.range.contains(range) -> reference - AllNNIRange.SINGLERANGE
                    match.range.isSimple -> reference - range
                    else -> match.remove(this, isSiblingName, checkHelper) // TODO a bit more options
                }
            }
        }

        override fun removeFromSequence(
            reference: FlattenedGroup.Sequence,
            isSiblingName: ContextT,
            checkHelper: CheckHelper
        ): FlattenedParticle? {
            if (reference.maxOccurs > VAllNNI.ONE) { // handle the case that there is more than 1 iteration
                val head = FlattenedGroup.Sequence(reference.minOccurs..VAllNNI.ONE, reference.particles)
                val tail = (reference - AllNNIRange.SINGLERANGE) ?: error("Should not happen because maxOccurs>0")
                val trimmedHead = removeFromSequence(head, isSiblingName, checkHelper) ?: return null
                if (trimmedHead.maxOccurs == VAllNNI.ZERO) return tail
                return FlattenedGroup.Sequence(AllNNIRange.SINGLERANGE, listOf(trimmedHead, tail))
            }
            val partIt = reference.particles.iterator()
            val newParticles = mutableListOf<FlattenedParticle>()
            while (partIt.hasNext()) {
                val part = partIt.next()
                val removed = part.remove(this, isSiblingName, checkHelper)
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
                0 -> FlattenedGroup.EMPTY
                1 -> newParticles.single() * range
                else -> FlattenedGroup.Sequence(AllNNIRange.SINGLERANGE, newParticles)
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
                other == FlattenedGroup.EMPTY -> this

                other is FlattenedGroup.All -> other + this

                other is Element && this.isMergable(other) ->
                    Element(range + other.range, term, false)

                else -> FlattenedGroup.All(AllNNIRange.SINGLERANGE, listOf(this, other))
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
            isSiblingName: ContextT,
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

        override fun restrictsAll(base: FlattenedGroup.All, isSiblingName: ContextT, checkHelper: CheckHelper): Boolean {
            return when (checkHelper.version) {
                SchemaVersion.V1_0 -> false
                else -> FlattenedGroup.All(AllNNIRange.SINGLERANGE, listOf(this), checkHelper.version).restrictsAll(
                    base,
                    isSiblingName,
                    checkHelper
                )
            }
        }

        override fun restrictsChoice(base: FlattenedGroup.Choice, isSiblingName: ContextT, checkHelper: CheckHelper): Boolean {
            return when (checkHelper.version) {
                SchemaVersion.V1_0 -> false
                else -> FlattenedGroup.Choice(AllNNIRange.SINGLERANGE, listOf(this), checkHelper.version).restrictsChoice(
                    base,
                    isSiblingName,
                    checkHelper
                )
            }
        }

        override fun restrictsSequence(base: FlattenedGroup.Sequence, context: ContextT, checkHelper: CheckHelper): Boolean {
            return when (checkHelper.version) {
                SchemaVersion.V1_0 -> false
                else -> FlattenedGroup.Sequence(AllNNIRange.SINGLERANGE, listOf(this))
                    .restrictsSequence(base, context, checkHelper)
            }
        }

        override fun plus(other: FlattenedParticle): FlattenedParticle = when {
            other == FlattenedGroup.EMPTY -> this
            other is Wildcard && this.isMergable(other) -> Wildcard(this.range + other.range, term)
            else -> FlattenedGroup.All(AllNNIRange.SINGLERANGE, listOf(this, other))
        }

        fun isMergable(other: Wildcard) =
            term.mdlNamespaceConstraint == other.term.mdlNamespaceConstraint &&
                    term.mdlProcessContents == other.term.mdlProcessContents &&
                    term.mdlNotQName == other.term.mdlNotQName

        override fun remove(
            reference: FlattenedParticle,
            isSiblingName: ContextT,
            checkHelper: CheckHelper
        ): FlattenedParticle? {
            return reference.removeFromWildcard(this, isSiblingName, checkHelper)
        }

        override fun removeFromWildcard(
            reference: Wildcard,
            isSiblingName: ContextT,
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
                    0 -> FlattenedGroup.EMPTY
                    else -> {
                        val elems = sg.map { Element(AllNNIRange.SINGLERANGE, it, true) }
                        FlattenedGroup.Choice(
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
