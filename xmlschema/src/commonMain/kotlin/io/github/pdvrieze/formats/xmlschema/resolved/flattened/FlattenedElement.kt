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

import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedElement
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSchemaLike
import io.github.pdvrieze.formats.xmlschema.resolved.SchemaVersion
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.AllNNIRange
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import nl.adaptivity.xmlutil.isEquivalent

class FlattenedElement internal constructor(
    range: AllNNIRange,
    override val term: ResolvedElement,
    @Suppress("UNUSED_PARAMETER") dummy: Boolean
) :
    FlattenedTerm(range) {
    override fun startingTerms(): List<FlattenedElement> {
        return listOf(this)
    }

    override fun trailingTerms(): List<FlattenedElement> = listOf(this)

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun isRestrictedBy(
        other: FlattenedParticle
    ): Boolean = other.restrictsElement(this)

    context(siblingContext: SiblingContextProvider)
    override fun isExtendedBy(other: FlattenedParticle, schema: ResolvedSchemaLike): Boolean {
        return other.extendsElement(this, schema)
    }

    context(siblingContext: SiblingContextProvider)
    override fun extendsElement(base: FlattenedElement, schema: ResolvedSchemaLike): Boolean {
        return range == base.range && term == base.term
    }

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun restrictsElement(
        base: FlattenedElement
    ): Boolean {
        if (!base.range.contains(range)) return false

        if (!base.term.mdlQName.isEquivalent(term.mdlQName)) return false

        return base.term.subsumes(term, checkHelper.isLax)
    }

    // Implements NSCompat
    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun restrictsWildcard(base: FlattenedWildcard): Boolean {
        // NSCompat 2
        if (!base.range.contains(range)) return false

        // NSCompat 1
        return base.term.matches(term.mdlQName, siblingContext, checkHelper.schema)

    }

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun restrictsAll(base: FlattenedAll): Boolean {
        return FlattenedAll(AllNNIRange.SINGLERANGE, listOf(this))
            .restrictsAll(base)
    }

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun restrictsChoice(base: FlattenedChoice): Boolean {
        // The option to do it either way is valid for 1.1
        return FlattenedChoice(AllNNIRange.SINGLERANGE, listOf(this))
            .restrictsChoice(base) ||
                (checkHelper.version == SchemaVersion.V1_1 &&
                        with(siblingContext) {
                            FlattenedChoice(range, listOf(single())).restricts(base)
                        })
    }

    context(checkHelper: CheckHelper, context: SiblingContextProvider)
    override fun restrictsSequence(
        base: FlattenedSequence
    ): Boolean {
        return FlattenedSequence(AllNNIRange.SINGLERANGE, listOf(this))
            .restrictsSequence(base)
    }

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun removePrefix(
        prefixParticle: FlattenedParticle
    ): RemovalResult {
        return prefixParticle.removeFromElement(this)
    }

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun removeFromElement(reference: FlattenedElement): RemovalResult {
        if (!reference.term.mdlQName.isEquivalent(term.mdlQName)) return RemovalResult.NoMatch

        if (!reference.term.subsumes(term, checkHelper.isLax)) return RemovalResult.NoMatch

        return RemovalResult(reference.minus(range))
    }

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun removeFromWildcard(
        reference: FlattenedWildcard
    ): RemovalResult {
        if (!reference.term.matches(term.mdlQName, siblingContext, checkHelper.schema)) return RemovalResult.NoMatch

        return RemovalResult(reference - range)
    }

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun removeFromAll(
        reference: FlattenedAll
    ): RemovalResult {
        return super.removeFromAll(reference)
    }

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun removeFromChoice(
        reference: FlattenedChoice
    ): RemovalResult {
        val matchIdx = reference.particles.indexOfFirst {
            it.single().isRestrictedBy(single())
        }
        if (matchIdx < 0) return RemovalResult.NoMatch
        val match = reference.particles[matchIdx]
        return if (maxOccurs == VAllNNI.UNBOUNDED) {
            when {
                match.maxOccurs == VAllNNI.UNBOUNDED -> RemovalResult(reference - AllNNIRange.SINGLERANGE)
                reference.maxOccurs == VAllNNI.UNBOUNDED -> RemovalResult.FullMatch
                else -> match.removePrefix(this@FlattenedElement)
                    // perhaps inside the match it is possible
            }
        } else { // consider further options
            when {
                match.minOccurs * reference.minOccurs > minOccurs -> RemovalResult((match * reference.range)?.minus(range))
                match.range.contains(range) -> RemovalResult(reference - AllNNIRange.SINGLERANGE)
                match.range.isSimple -> RemovalResult(reference - range)
                else -> match.removePrefix(this@FlattenedElement)
                        // TODO a bit more options
            }
        }
    }

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun removeFromSequence(
        reference: FlattenedSequence
    ): RemovalResult {
        if (reference.maxOccurs > VAllNNI.ONE) { // handle the case that there is more than 1 iteration
            val head = FlattenedSequence(reference.minOccurs..VAllNNI.ONE, reference.particles)
            val tail = (reference - AllNNIRange.SINGLERANGE) ?: error("Should not happen because maxOccurs>0")

            return removeFromSequence(head).map { trimmedHead ->
                FlattenedSequence(AllNNIRange.SINGLERANGE, listOf(trimmedHead, tail))
            }
        }
        val partIt = reference.particles.iterator()
        val newParticles = mutableListOf<FlattenedParticle>()
        while (partIt.hasNext()) {
            val part = partIt.next()
            when (val r = part.removePrefix(this@FlattenedElement)) {
                is RemovalResult.NoMatch -> return r
                is RemovalResult.FullMatch -> {}

                is RemovalResult.PrefixMatch -> newParticles.add(r.suffix)
            }
        }
        while (partIt.hasNext()) {
            newParticles.add(partIt.next())
        } // flush remaining particles

        // We consumed part of the sequence so it must occur
        return when (newParticles.size) {
            0 -> RemovalResult.FullMatch
            1 -> RemovalResult(newParticles.single() * range)
            else -> RemovalResult(FlattenedSequence(AllNNIRange.SINGLERANGE, newParticles))
        }
    }

    override fun single(): FlattenedElement = FlattenedElement(AllNNIRange.SINGLERANGE, term, true)

    override fun times(range: AllNNIRange): FlattenedElement? {
        return this.range.mergeRanges(range)?.let { FlattenedElement(it, term, true) }
    }

    override fun minus(range: AllNNIRange): FlattenedParticle? {
        return this.range.minus(range)?.let { FlattenedElement(it, term, true) }
    }

    override fun plus(other: FlattenedParticle): FlattenedParticle {
        return when {
            other == FlattenedEmptyGroup -> this

            other is FlattenedAll -> other + this

            other is FlattenedElement && this.isMergable(other) ->
                FlattenedElement(range + other.range, term, false)

            else -> FlattenedAll(AllNNIRange.SINGLERANGE, listOf(this, other))
        }
    }

    private fun isMergable(other: FlattenedElement): Boolean {
        return term.mdlAbstract == other.term.mdlAbstract &&
                term.mdlQName == other.term.mdlQName
    }

    override fun toString(): String = range.toPostfix(term.mdlQName.toString())


}
