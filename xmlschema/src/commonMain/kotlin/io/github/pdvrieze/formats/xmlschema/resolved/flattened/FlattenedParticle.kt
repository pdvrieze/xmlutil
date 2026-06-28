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

import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedAny
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedElement
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedGlobalElement
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSchemaLike
import io.github.pdvrieze.formats.xmlschema.resolved.SchemaVersion
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.AllNNIRange
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.namespaceURI
import kotlin.jvm.JvmStatic

sealed class FlattenedParticle(val range: AllNNIRange) {

    val maxOccurs get() = range.endInclusive
    val minOccurs get() = range.start

    open val isEmptiable: Boolean get() = minOccurs == VAllNNI.ZERO
    val isVariable: Boolean get() = minOccurs != maxOccurs

    abstract fun effectiveTotalRange(): AllNNIRange

    abstract fun startingTerms(): List<FlattenedTerm>
    abstract fun trailingTerms(): List<FlattenedTerm>

    abstract fun single(): FlattenedParticle

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    internal abstract fun isRestrictedBy(other: FlattenedParticle): Boolean

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    open fun restricts(reference: FlattenedParticle): Boolean {
        return reference.isRestrictedBy(this@FlattenedParticle)
    }

    context(siblingContext: SiblingContextProvider)
    open fun extends(base: FlattenedParticle, schema: ResolvedSchemaLike): Boolean {
        return base.isExtendedBy(this, schema)
    }

    context(siblingContext: SiblingContextProvider)
    protected abstract fun isExtendedBy(
        other: FlattenedParticle,
        schema: ResolvedSchemaLike
    ): Boolean

    context(siblingContext: SiblingContextProvider)
    open fun extendsElement(base: FlattenedElement, schema: ResolvedSchemaLike): Boolean = false

    context(siblingContext: SiblingContextProvider)
    open fun extendsWildcard(base: FlattenedWildcard, schema: ResolvedSchemaLike): Boolean = false

    context(siblingContext: SiblingContextProvider)
    open fun extendsAll(base: FlattenedAll, schema: ResolvedSchemaLike): Boolean = false

    context(siblingContext: SiblingContextProvider)
    open fun extendsChoice(base: FlattenedChoice, schema: ResolvedSchemaLike): Boolean = false

    context(siblingContext: SiblingContextProvider)
    open fun extendsSequence(base: FlattenedSequence, schema: ResolvedSchemaLike): Boolean =
        false

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    open fun restrictsElement(base: FlattenedElement): Boolean = false

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    open fun restrictsWildcard(base: FlattenedWildcard): Boolean = false

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    open fun restrictsAll(base: FlattenedAll): Boolean = false

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    open fun restrictsChoice(base: FlattenedChoice): Boolean = false

    context(checkHelper: CheckHelper, context: SiblingContextProvider)
    open fun restrictsSequence(base: FlattenedSequence): Boolean = false

    abstract operator fun times(range: AllNNIRange): FlattenedParticle?

    /**
     * Remove this particle from the [prefixParticle] sequence
     * @return The resulting sequence if the reference was removed, `null` if not
     */
    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    abstract fun removePrefix(prefixParticle: FlattenedParticle): RemovalResult

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    open fun removeFromElement(reference: FlattenedElement): RemovalResult = RemovalResult.NoMatch

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    open fun removeFromWildcard(reference: FlattenedWildcard): RemovalResult = RemovalResult.NoMatch

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    open fun removeFromAll(reference: FlattenedAll): RemovalResult = RemovalResult.NoMatch

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    open fun removeFromChoice(reference: FlattenedChoice): RemovalResult = RemovalResult.NoMatch

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    open fun removeFromSequence(reference: FlattenedSequence): RemovalResult = RemovalResult.NoMatch

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
                -> FlattenedElement(range, term, true)

            else -> {
                val sg = term.fullSubstitutionGroup(schemaVersion)
                when (sg.size) {
                    0 -> FlattenedEmptyGroup
                    else -> {
                        val elems = sg.map { FlattenedElement(AllNNIRange.SINGLERANGE, it, true) }
                        FlattenedChoice(
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
                is FlattenedTerm -> when (b) {
                    is FlattenedTerm -> when (val at = a.term) {
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
                    is FlattenedTerm -> 1
                    is FlattenedGroup -> a.compareTo(b)
                }
            }
        }

        private val FlattenedGroup.kindKey: Int
            get() = when (this) {
                is FlattenedAll -> 0
                is FlattenedChoice -> 1
                is FlattenedSequence -> 2
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

            endInclusive > VAllNNI.ONE -> prefix + '[' + start.toULong() + "" + (endInclusive as VAllNNI.Value).toULong() + ']'
            // end inclusive 0 can happen due to subtraction in the sequence algorithm
            endInclusive == VAllNNI.ZERO -> prefix + "[0]"
            start == VAllNNI.ZERO -> prefix + '?'
            else -> prefix // both are 1
        }
    }


}
