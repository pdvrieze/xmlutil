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

class FlattenedAll(range: AllNNIRange, override val particles: List<FlattenedParticle>) :
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

    override fun startingTerms(): List<FlattenedTerm> {
        return particles.flatMap { it.startingTerms() }
    }

    override fun trailingTerms(): List<FlattenedTerm> {
        return particles.flatMap { it.trailingTerms() }
    }

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun isRestrictedBy(
        other: FlattenedParticle
    ): Boolean = other.restrictsAll(this)

    context(siblingContext: SiblingContextProvider)
    override fun isExtendedBy(other: FlattenedParticle, schema: ResolvedSchemaLike): Boolean {
        return other.extendsAll(this, schema)
    }

    context(siblingContext: SiblingContextProvider)
    override fun extendsAll(base: FlattenedAll, schema: ResolvedSchemaLike): Boolean {
        // part 3.1
        if (minOccurs != base.minOccurs) return false

        // this is also true if both terms are equal
        return particles.containsAll(base.particles)
    }

    context(siblingContext: SiblingContextProvider)
    override fun extendsElement(base: FlattenedElement, schema: ResolvedSchemaLike): Boolean {
        return extendsAll(FlattenedAll(AllNNIRange.SINGLERANGE, listOf(base)), schema) ||
                (schema.version != SchemaVersion.V1_0 && extendsAll(FlattenedAll(base.range, listOf(base.single())), schema))
    }

    override fun plus(other: FlattenedParticle): FlattenedParticle = when {
        other == FlattenedEmptyGroup -> this
        other is FlattenedAll && range.isSimple && other.range.isSimple -> {
            val mergedParticles = (particles.asSequence() + other.particles.asSequence()).groupBy {
                it.toString()
            }.flatMap { (_, toMerge) ->
                val p = toMerge.reduce { l, r -> l + r }
                when {
                    p is FlattenedAll && p.range.isSimple -> p.particles
                    else -> listOf(p)
                }
            }
            FlattenedAll(AllNNIRange.SINGLERANGE, mergedParticles)
        }

        else -> FlattenedAll(AllNNIRange.SINGLERANGE, listOf(this, other))
    }

    /**
     * Recurse
     */
    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun restrictsAll(base: FlattenedAll): Boolean {
        return restrictsRecurse(base)
    }

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun removePrefix(
        prefixParticle: FlattenedParticle
    ): RemovalResult {
        return prefixParticle.removeFromAll(this)
    }

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun removeFromAll(
        reference: FlattenedAll
    ): RemovalResult {
        if (minOccurs > reference.maxOccurs) return RemovalResult.NoMatch

        val baseParts = reference.particles.toTypedArray<FlattenedParticle?>()

        for (p in particles) {
            val matchIdx = baseParts.indexOfFirst {
                it != null && with(siblingContext) {
                    p.single().restricts(it.single())
                }
            }
            if (matchIdx < 0) return RemovalResult.NoMatch

            val match = baseParts[matchIdx]!!
            if (p.maxOccurs > match.maxOccurs) return RemovalResult.NoMatch // can not work
            baseParts[matchIdx] = (match - p.range)?.takeIf { it.maxOccurs > VAllNNI.ZERO }
        }
        for (b in baseParts) {
            if (b != null && !b.isEmptiable) return RemovalResult.NoMatch
        }
        return RemovalResult(FlattenedAll(reference.range, baseParts.filterNotNull(), checkHelper.version))
    }

    override fun single(): FlattenedAll {
        return FlattenedAll(AllNNIRange.SINGLERANGE, particles)
    }

    override fun times(range: AllNNIRange): FlattenedAll? {
        return this.range.mergeRanges(range)?.let { FlattenedAll(it, particles) }
    }

    override fun minus(range: AllNNIRange): FlattenedParticle? {
        return this.range.minus(range)?.let { FlattenedAll(it, particles) }
    }

    override fun toString(): String = particles.joinToString(prefix = "{", postfix = range.toPostfix("}"))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as FlattenedAll

        return particles == other.particles
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + particles.hashCode()
        return result
    }
}
