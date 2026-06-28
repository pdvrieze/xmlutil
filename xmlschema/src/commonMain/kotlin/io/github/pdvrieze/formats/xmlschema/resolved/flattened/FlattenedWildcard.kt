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

import io.github.pdvrieze.formats.xmlschema.datatypes.AnyType
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedAny
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSchemaLike
import io.github.pdvrieze.formats.xmlschema.resolved.SchemaVersion
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.AllNNIRange

class FlattenedWildcard(range: AllNNIRange, override val term: ResolvedAny) : FlattenedTerm(range) {

    override fun startingTerms(): List<FlattenedWildcard> {
        return listOf(this)
    }

    override fun trailingTerms(): List<FlattenedWildcard> = listOf(this)

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun isRestrictedBy(
        other: FlattenedParticle
    ): Boolean = other.restrictsWildcard(this)

    context(siblingContext: SiblingContextProvider)
    override fun isExtendedBy(other: FlattenedParticle, schema: ResolvedSchemaLike): Boolean {
        return other.extendsWildcard(this, schema)
    }

    context(siblingContext: SiblingContextProvider)
    override fun extendsWildcard(base: FlattenedWildcard, schema: ResolvedSchemaLike): Boolean {
        return range == base.range && term == base.term
    }

    /**
     * NSSubset
     */
    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun restrictsWildcard(base: FlattenedWildcard): Boolean {
        // NSSubset 1
        if (!base.range.contains(range)) return false

        // NSSubset 2, subset per Schema 1 - 3.10.6
        if (!term.mdlNamespaceConstraint.isSubsetOf(base.term.mdlNamespaceConstraint, checkHelper.version)) return false

        // NSSubset 3, (exception for the ur-wildcard is needed - although a shortcut may apply by just always
        // restricting AnyType)
        return base.term === AnyType.urWildcard || term.mdlProcessContents >= base.term.mdlProcessContents
    }

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun restrictsAll(base: FlattenedAll): Boolean {
        return when (checkHelper.version) {
            SchemaVersion.V1_0 -> false
            else -> FlattenedAll(AllNNIRange.SINGLERANGE, listOf(this), checkHelper.version).restrictsAll(
                base
            )
        }
    }

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun restrictsChoice(base: FlattenedChoice): Boolean {
        return when (checkHelper.version) {
            SchemaVersion.V1_0 -> false
            else -> FlattenedChoice(AllNNIRange.SINGLERANGE, listOf(this), checkHelper.version)
                .restrictsChoice(base)
        }
    }

    context(checkHelper: CheckHelper, context: SiblingContextProvider)
    override fun restrictsSequence(base: FlattenedSequence): Boolean {
        return when (checkHelper.version) {
            SchemaVersion.V1_0 -> false
            else -> FlattenedSequence(AllNNIRange.SINGLERANGE, listOf(this))
                .restrictsSequence(base)
        }
    }

    override fun plus(other: FlattenedParticle): FlattenedParticle = when {
        other == FlattenedEmptyGroup -> this
        other is FlattenedWildcard && this.isMergable(other) -> FlattenedWildcard(this.range + other.range, term)
        else -> FlattenedAll(AllNNIRange.SINGLERANGE, listOf(this, other))
    }

    fun isMergable(other: FlattenedWildcard) =
        term.mdlNamespaceConstraint == other.term.mdlNamespaceConstraint &&
                term.mdlProcessContents == other.term.mdlProcessContents &&
                term.mdlNotQName == other.term.mdlNotQName

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun removePrefix(
        prefixParticle: FlattenedParticle
    ): RemovalResult {
        return prefixParticle.removeFromWildcard(this)
    }

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun removeFromWildcard(
        reference: FlattenedWildcard
    ): RemovalResult {
        if (!reference.range.contains(range)) return RemovalResult.NoMatch
        if (!term.mdlNamespaceConstraint.isSubsetOf(
                reference.term.mdlNamespaceConstraint,
                checkHelper.version
            )
        ) return RemovalResult.NoMatch
        if (reference.term !== AnyType.urWildcard && term.mdlProcessContents < reference.term.mdlProcessContents) {
            return RemovalResult.NoMatch
        }
        return RemovalResult(reference - range)
    }

    override fun single(): FlattenedWildcard = FlattenedWildcard(AllNNIRange.SINGLERANGE, term)

    override fun times(range: AllNNIRange): FlattenedWildcard? {
        return this.range.mergeRanges(range)?.let { FlattenedWildcard(it, term) }
    }

    override fun minus(range: AllNNIRange): FlattenedParticle? {
        return this.range.minus(range)?.let { FlattenedWildcard(it, term) }
    }

    override fun toString(): String = range.toPostfix("<${term.mdlNamespaceConstraint}>")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as FlattenedWildcard

        return term == other.term
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + term.hashCode()
        return result
    }
}
