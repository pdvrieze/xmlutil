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

package io.github.pdvrieze.formats.xmlschema.resolved.facets

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VString
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.*
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSFacet
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSPattern
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSchemaLike

class FacetList(
    val assertions: List<ResolvedAssertionFacet> = emptyList(),
    val minConstraint: ResolvedMinBoundFacet? = null,
    val maxConstraint: ResolvedMaxBoundFacet? = null,
    val enumeration: List<ResolvedEnumeration> = emptyList(),
    val explicitTimezone: ResolvedExplicitTimezone? = null,
    val fractionDigits: ResolvedFractionDigits? = null,
    val minLength: IResolvedMinLength? = null,
    val maxLength: IResolvedMaxLength? = null,
    val patterns: List<ResolvedPattern> = emptyList(),
    val totalDigits: ResolvedTotalDigits? = null,
    val whiteSpace: ResolvedWhiteSpace? = null,
    val otherFacets: List<ResolvedFacet> = emptyList()
) {

    private fun <T : ResolvedFacet> T.checkNotFixed(facet: ResolvedFacet?): T {
        check(this == facet || facet?.fixed != true) {
            "Fixed facet ${facet} cannot be overridden"
        }
        return this
    }

    private fun <T : IResolvedMinLength> T.checkNotFixed(facet: IResolvedMinLength?): T {
        check(this == facet || facet?.fixed != true) { "Fixed facet ${facet} cannot be overridden" }
        return this
    }

    private fun <T : IResolvedMaxLength> T.checkNotFixed(facet: IResolvedMaxLength?): T {
        check(this == facet || facet?.fixed != true) {
            "Fixed facet ${facet} cannot be overridden"
        }
        return this
    }

    fun override(newList: FacetList): FacetList {
        val otherFacets = mutableListOf<ResolvedFacet>().apply { addAll(otherFacets) }
        val assertions = mutableListOf<ResolvedAssertionFacet>().apply { addAll(assertions) }
        val enumeration = mutableListOf<ResolvedEnumeration>().apply { addAll(enumeration) }
        val patterns = mutableListOf<ResolvedPattern>().apply { addAll(patterns) }
        otherFacets.addAll(newList.otherFacets)
        assertions.addAll(newList.assertions)
        enumeration.addAll(newList.enumeration)
        patterns.addAll(newList.patterns)

        // TODO("Handle min/max constraints properly")
        val minConstraint: ResolvedMinBoundFacet? =
            newList.minConstraint?.checkNotFixed(this.minConstraint) ?: this.minConstraint
        val maxConstraint: ResolvedMaxBoundFacet? =
            newList.maxConstraint?.checkNotFixed(this.maxConstraint) ?: this.maxConstraint

        val explicitTimezone: ResolvedExplicitTimezone? =
            newList.explicitTimezone?.checkNotFixed(this.explicitTimezone) ?: this.explicitTimezone
        val fractionDigits: ResolvedFractionDigits? =
            newList.fractionDigits?.checkNotFixed(this.fractionDigits) ?: this.fractionDigits

        val minLength: IResolvedMinLength? = run {
            val old = this.minLength?.value
            val new = newList.minLength?.value
            when {
                old == null -> newList.minLength
                new == null -> this.minLength
                old <= new -> newList.minLength
                else -> error("Restriction can only increase the minLength")
            }
        }?.checkNotFixed(this.minLength)

        val maxLength: IResolvedMaxLength? = run {
            val old = this.maxLength?.value
            val new = newList.maxLength?.value
            when {
                old == null -> newList.maxLength
                new == null -> this.maxLength
                old < new -> error("Restriction can only reduce the max length")
                else -> this.maxLength
            }
        }?.checkNotFixed(this.maxLength)

        val totalDigits: ResolvedTotalDigits? = newList.totalDigits?.checkNotFixed(this.totalDigits) ?: this.totalDigits
        val whiteSpace: ResolvedWhiteSpace? = newList.whiteSpace?.checkNotFixed(this.whiteSpace) ?: this.whiteSpace

        return FacetList(
            assertions,
            minConstraint,
            maxConstraint,
            enumeration,
            explicitTimezone,
            fractionDigits,
            minLength,
            maxLength,
            patterns,
            totalDigits,
            whiteSpace,
            otherFacets
        )

    }

    fun validate(primitiveType: PrimitiveDatatype?, representation: VString) {
        val normalized = whiteSpace?.value?.normalize(representation) ?: representation
        val normalizedStr = normalized.toString()

        if (enumeration.isNotEmpty()) {
            check(enumeration.any { normalized == it.value })
        }

        for (pattern in patterns) {
            check(pattern.regex.matches(normalizedStr)) { "'$normalized' does not match expression '${pattern.value}'" }
        }

        when (primitiveType) {
            is IStringType -> {
                minLength?.let { check(normalizedStr.length >= it.value.toInt()) }
                maxLength?.let { check(normalizedStr.length <= it.value.toInt()) }
                check(minConstraint == null) { "Strings can not have numeric facets" }
                check(maxConstraint == null) { "Strings can not have numeric facets" }
                check(totalDigits == null) { "totalDigits only applies to decimal types" }
                check(fractionDigits == null) { "totalDigits only applies to decimal types" }
            }

            is IDecimalType -> {
                val actualValue = primitiveType.value(normalized)
                minConstraint?.validate(primitiveType, actualValue)
                maxConstraint?.validate(primitiveType, actualValue)
                totalDigits?.let { check(normalized.length <= it.value.toInt()) }
                check(fractionDigits == null) { "totalDigits only applies to decimal types" }

                primitiveType.validateValue(actualValue)
            }

            is DoubleType -> {
                val actualValue = primitiveType.value(normalized)
                minConstraint?.validate(actualValue)
                maxConstraint?.validate(actualValue)
                check(totalDigits == null) { "totalDigits only applies to decimal types" }
                fractionDigits?.let { check(normalizedStr.substringAfterLast('.', "").length <= it.value.toInt()) }

                primitiveType.validateValue(actualValue)
            }

            is FloatType -> {
                val actualValue = primitiveType.value(normalized)
                minConstraint?.validate(actualValue)
                maxConstraint?.validate(actualValue)
                check(totalDigits == null) { "totalDigits only applies to decimal types" }
                fractionDigits?.let { check(normalizedStr.substringAfterLast('.', "").length <= it.value.toInt()) }

                primitiveType.validateValue(actualValue)
            }

            else -> {}
        }
    }

    fun checkList() {
        check(assertions.isEmpty())
        check(explicitTimezone == null) { "lists don't have a timezone facet" }
        check(fractionDigits == null) { "lists don't have a fractionDigits facet" }
        check(minConstraint == null) { "lists don't have a minConstraint facet" }
        check(maxConstraint == null) { "lists don't have a maxConstraint facet" }
        check(totalDigits == null) { "lists don't have a totalDigits facet" }
    }


    companion object {
        val EMPTY: FacetList = FacetList()

        operator fun invoke(rawFacets: Iterable<XSFacet>, schemaLike: ResolvedSchemaLike): FacetList =
            FacetList(rawFacets.map { ResolvedFacet(it, schemaLike) })

        operator fun invoke(facets: Iterable<ResolvedFacet>): FacetList {
            val otherFacets: MutableList<ResolvedFacet> = mutableListOf()
            val assertions: MutableList<ResolvedAssertionFacet> = mutableListOf()
            val enumeration: MutableList<ResolvedEnumeration> = mutableListOf()
            var minConstraint: ResolvedMinBoundFacet? = null
            var maxConstraint: ResolvedMaxBoundFacet? = null
            var explicitTimezone: ResolvedExplicitTimezone? = null
            var fractionDigits: ResolvedFractionDigits? = null
            var minLength: IResolvedMinLength? = null
            var maxLength: IResolvedMaxLength? = null
            var pattern: ResolvedPattern? = null
            var totalDigits: ResolvedTotalDigits? = null
            var whiteSpace: ResolvedWhiteSpace? = null

            for (facet in facets) {
                when (facet) {
                    is ResolvedAssertionFacet -> assertions.add(facet)
                    is ResolvedMaxExclusive ->
                        if (maxConstraint != null) error("multiple max facets") else maxConstraint = facet

                    is ResolvedMaxInclusive ->
                        if (maxConstraint != null) error("multiple max facets") else maxConstraint = facet

                    is ResolvedMinExclusive ->
                        if (minConstraint != null) error("multiple min facets") else minConstraint = facet

                    is ResolvedMinInclusive ->
                        if (minConstraint != null) error("multiple min facets") else minConstraint = facet

                    is ResolvedEnumeration -> enumeration.add(facet)
                    is ResolvedExplicitTimezone ->
                        if (explicitTimezone != null) error("multiple explicitTimezone facets") else explicitTimezone =
                            facet

                    is ResolvedFractionDigits ->
                        if (fractionDigits != null) error("multiple fractionDigits facets") else fractionDigits = facet

                    is ResolvedLength ->
                        if (minLength != null || maxLength != null) error("multiple length facets") else {
                            minLength = facet
                            maxLength = facet
                        }

                    is ResolvedMaxLength ->
                        if (maxLength != null) error("multiple maxLength facets") else maxLength = facet

                    is ResolvedMinLength ->
                        if (minLength != null) error("multiple maxLength facets") else minLength = facet

                    is ResolvedPattern -> pattern?.let {// combine by or
                        ResolvedPattern(XSPattern("(?:${it.value}|${facet.value})"), it.schema)
                    } ?: run {
                        pattern = facet
                    }

                    is ResolvedTotalDigits ->
                        if (totalDigits != null) error("multiple totalDigits facets") else totalDigits = facet

                    is ResolvedWhiteSpace ->
                        if (whiteSpace != null) error("multiple whiteSpace facets") else whiteSpace = facet
                }
            }

            return FacetList(
                assertions,
                minConstraint,
                maxConstraint,
                enumeration,
                explicitTimezone,
                fractionDigits,
                minLength,
                maxLength,
                listOfNotNull(pattern),
                totalDigits,
                whiteSpace,
                otherFacets
            )
        }
    }
}
