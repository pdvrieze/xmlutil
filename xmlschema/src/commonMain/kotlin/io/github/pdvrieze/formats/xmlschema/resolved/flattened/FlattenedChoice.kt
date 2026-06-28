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
import io.github.pdvrieze.formats.xmlschema.types.isContentEqual

class FlattenedChoice(range: AllNNIRange, override val particles: List<FlattenedParticle>) :
    FlattenedGroup(range) {

    constructor(range: AllNNIRange, particles: List<FlattenedParticle>, version: SchemaVersion) : this(
        range,
        when (version) {
            SchemaVersion.V1_0 -> particles
            else -> particles.sortedWith(particleComparator)
        }
    )

    override fun startingTerms(): List<FlattenedTerm> {
        return particles.flatMap { it.startingTerms() }
    }

    override fun trailingTerms(): List<FlattenedTerm> {
        return particles.flatMap { it.trailingTerms() }
    }

    override fun effectiveTotalRange(): AllNNIRange {
        return particles.asSequence()
            .map { it.effectiveTotalRange() }
            .reduce { l, r ->
                AllNNIRange(minOf(l.start, r.start), maxOf(l.endInclusive, r.endInclusive))
            }.let { it.start * range.start..it.endInclusive * range.endInclusive }
    }

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun isRestrictedBy(
        other: FlattenedParticle
    ): Boolean = other.restrictsChoice(this)

    context(siblingContext: SiblingContextProvider)
    override fun isExtendedBy(other: FlattenedParticle, schema: ResolvedSchemaLike): Boolean {
        return other.extendsChoice(this, schema)
    }

    context(siblingContext: SiblingContextProvider)
    override fun extendsChoice(base: FlattenedChoice, schema: ResolvedSchemaLike): Boolean {
        return range == base.range && particles.isContentEqual(base.particles)
    }

    context(siblingContext: SiblingContextProvider)
    override fun extendsElement(base: FlattenedElement, schema: ResolvedSchemaLike): Boolean {
        return extendsChoice(FlattenedChoice(AllNNIRange.SINGLERANGE, listOf(base)), schema) ||
                (schema.version != SchemaVersion.V1_0 && extendsChoice(
                    FlattenedChoice(base.range, listOf(base.single())),
                    schema
                ))
    }

    // Recurse lax
    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun restrictsChoice(base: FlattenedChoice): Boolean {
        if (!base.range.contains(range)) return false

        val baseIt = base.particles.iterator()

        for (p in particles) {
            while (true) {
                if (!baseIt.hasNext()) return false
                if (with(siblingContext) {
                        p.restricts(baseIt.next())
                    }) break
            }
        } // this doesn't need to check emptiability

        return true
    }

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun restrictsAll(base: FlattenedAll): Boolean {
        if (checkHelper.version == SchemaVersion.V1_0) return false
        return particles.all {
            val reRanged = it * range
            reRanged != null && reRanged.restrictsAll(base)
        }
    }

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun removePrefix(
        prefixParticle: FlattenedParticle
    ): RemovalResult {
        return prefixParticle.removeFromChoice(this)
    }

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun removeFromChoice(
        reference: FlattenedChoice
    ): RemovalResult {
        if (!reference.effectiveTotalRange().contains(effectiveTotalRange())) return RemovalResult.NoMatch

        val baseIt = reference.particles.iterator()

        for (p in particles) {
            var match = false
            while (!match) {
                if (!baseIt.hasNext()) return RemovalResult.NoMatch
                val basePart = baseIt.next()
                if (with(siblingContext) {
                        p.restricts(basePart)
                    }) {
                    match = true
                }
            }
            if (!match) return RemovalResult.NoMatch
        }

        val newMin = reference.minOccurs.safeMinus(minOccurs)
        val newMax = reference.maxOccurs.safeMinus(maxOccurs, newMin)

        return RemovalResult(FlattenedChoice(newMin..newMax, reference.particles))
    }

    override fun single(): FlattenedChoice {
        return FlattenedChoice(AllNNIRange.SINGLERANGE, particles)
    }

    override fun times(range: AllNNIRange): FlattenedChoice? {
        return this.range.mergeRanges(range)?.let { FlattenedChoice(it, particles) }
    }

    override fun minus(range: AllNNIRange): FlattenedParticle? {
        return this.range.minus(range)?.let { FlattenedChoice(it, particles) }
    }

    override fun plus(other: FlattenedParticle): FlattenedParticle = when {
        other == FlattenedEmptyGroup -> this
        else -> FlattenedAll(AllNNIRange.SINGLERANGE, listOf(this, other))
    }

    override fun toString(): String =
        particles.joinToString(separator = "| ", prefix = "(", postfix = range.toPostfix(")"))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as FlattenedChoice

        return particles == other.particles
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + particles.hashCode()
        return result
    }
}
