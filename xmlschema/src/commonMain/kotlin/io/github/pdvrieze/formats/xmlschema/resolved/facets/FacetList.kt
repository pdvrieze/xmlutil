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

import io.github.pdvrieze.formats.xmlschema.datatypes.AnySimpleType
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.IDateTime
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnySimpleType
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VString
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.*
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSFacet
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSPattern
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSchemaLike
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSimpleType
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSimpleType.Variety

class FacetList(
    val assertions: List<ResolvedAssertionFacet> = emptyList(),
    val minConstraint: ResolvedMinBoundFacet? = null,
    val maxConstraint: ResolvedMaxBoundFacet? = null,
    val enumeration: List<ResolvedEnumeration<VAnySimpleType>> = emptyList(),
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
        check(this == facet || facet?.mdlFixed != true) {
            "Fixed facet $facet cannot be overridden"
        }
        return this
    }

    private fun <T : IResolvedMinLength> T.checkNotFixed(facet: IResolvedMinLength?): T {
        check(this == facet || facet?.mdlFixed != true) { "Fixed facet ${facet} cannot be overridden" }
        return this
    }

    private fun <T : IResolvedMaxLength> T.checkNotFixed(facet: IResolvedMaxLength?): T {
        check(this == facet || facet?.mdlFixed != true) {
            "Fixed facet ${facet} cannot be overridden"
        }
        return this
    }

    /** Overlay according to 3.16.6.4 */
    fun overlay(newList: FacetList): FacetList {
        val otherFacets = mutableListOf<ResolvedFacet>().apply { addAll(otherFacets) }
        val assertions = mutableListOf<ResolvedAssertionFacet>().apply { addAll(assertions) }
        val enumeration = mutableListOf<ResolvedEnumeration<VAnySimpleType>>().apply { addAll(enumeration) }
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

        this.maxConstraint?.let { mc ->
            newList.maxConstraint?.let { nmc ->
                mc.validate(nmc.value)
            }
        }

        this.minConstraint?.let { mc ->
            newList.minConstraint?.let { nmc ->
                mc.validate(nmc.value)
            }
        }

        val totalDigits: ResolvedTotalDigits? = newList.totalDigits?.checkNotFixed(this.totalDigits) ?: this.totalDigits
        val whiteSpace: ResolvedWhiteSpace? = newList.whiteSpace?.let {
            if (this.whiteSpace!=null) check(it.value.canOverride(this.whiteSpace.value))
            it.checkNotFixed(this.whiteSpace)
        } ?: this.whiteSpace

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

    fun check(primitiveType: AnyPrimitiveDatatype?) {
        if (primitiveType!=null) {
            for (p in patterns) p.checkFacetValid(primitiveType)
        }

        if (minLength != null && maxLength != null) {
            check(minLength.value <= maxLength.value) { "minLength > maxLengh is not valid" }
        }

        when (primitiveType) {
            is IStringType -> {

                check(minConstraint == null) { "Strings can not have numeric facets" }
                check(maxConstraint == null) { "Strings can not have numeric facets" }
                check(totalDigits == null) { "totalDigits only applies to decimal types" }
                check(fractionDigits == null) { "totalDigits only applies to decimal types" }
            }

            is IDecimalType -> {
                if (minConstraint != null && maxConstraint != null) {
                    val minConstr = primitiveType.value(VString(minConstraint.value.xmlString))

                    val maxConstr = primitiveType.value(VString(maxConstraint.value.xmlString))

                    check(minConstr <= maxConstr) { "The minimum constraint ($minConstr) must be <= to the max constraint ($maxConstr)" }
                } else if (minConstraint != null) {
                    primitiveType.validateValue(minConstraint.value)
                } else if (maxConstraint != null) {
                    primitiveType.validateValue(maxConstraint.value)
                }
                if (totalDigits != null) {
                    check(totalDigits.value >0uL) { "Decimals must have at least 1 digit" }
                    if (fractionDigits!=null) {
                        check(fractionDigits.value<=totalDigits.value) { "Fraction digits must be less or equal to total digits" }
                    }
                }
                check(primitiveType == DecimalType || fractionDigits == null || fractionDigits.value.toInt() == 0) { "Only decimal type primitives can have fraction digits (${fractionDigits?.value} - ${primitiveType} instances can not)" }
            }

            is DoubleType -> {
                val minC = minConstraint?.let {
                    primitiveType.value(VString(it.value.xmlString)).also {
                        primitiveType.validateValue(it)
                    }
                }
                val maxC = maxConstraint?.let {
                    primitiveType.value(VString(it.value.xmlString)).also {
                        primitiveType.validateValue(it)
                    }
                }
                if (minC != null && maxC != null) {
                    check(minC.value <= maxC.value) { "Double constraints ($minC $maxC)  must be in order " }
                }

                check(totalDigits == null) { "totalDigits only applies to decimal types" }
            }

            is FloatType -> {
                val minC = minConstraint?.let {
                    primitiveType.value(VString(it.value.xmlString)).also {
                        primitiveType.validateValue(it)
                    }
                }
                val maxC = maxConstraint?.let {
                    primitiveType.value(VString(it.value.xmlString)).also {
                        primitiveType.validateValue(it)
                    }
                }
                if (minC != null && maxC != null) {
                    check(minC.value <= maxC.value) { "Float constraints must be in order" }
                }

                check(totalDigits == null) { "totalDigits only applies to decimal types" }
            }

            is DurationType -> {
                val minDuration = minConstraint?.let { primitiveType.value(it.value) }
                val maxDuration = maxConstraint?.let { primitiveType.value(it.value) }
                if (minDuration!=null && maxDuration!=null) {
                    check(minDuration <= maxDuration) { "Duration values not in range" }
                }
            }

            is DateType,
            is TimeType,
            is GYearType,
            is GYearMonthType,
            is GMonthDayType,
            is GDayType,
            is GMonthType,
            is DateTimeType -> {
                val minDateTime = minConstraint?.let { primitiveType.value(it.value) } as IDateTime?
                val maxDateTime = maxConstraint?.let { primitiveType.value(it.value) } as IDateTime?
                if (minDateTime != null && maxDateTime != null) {
                    check(minDateTime <= maxDateTime) { "DateTime values not in range" }
                }
            }

            else -> {}
        }

    }

    fun validate(primitiveType: AnyPrimitiveDatatype?, representation: VString) {
        val normalized = whiteSpace?.value?.normalize(representation) ?: representation
        val normalizedStr = normalized.toString()

        if (enumeration.isNotEmpty()) {
            check(enumeration.any { normalized.xmlString == it.value.xmlString }) { "Value: '${normalized}' is not in ${enumeration.joinToString { "'${it.value.xmlString}'"}}" }
        }

        for (pattern in patterns) {
            check(pattern.regex.matches(normalizedStr)) { "'$normalized' does not match expression '${pattern.value}'" }
        }

        when (primitiveType) {
            is IStringType -> {
                minLength?.let { check(normalizedStr.length >= it.value.toInt()) }
                maxLength?.let { check(normalizedStr.length <= it.value.toInt()) }
            }

            is IDecimalType -> {
                val actualValue = primitiveType.value(normalized)
                minConstraint?.validate(primitiveType, actualValue)
                maxConstraint?.validate(primitiveType, actualValue)
                totalDigits?.let { check(normalized.length <= it.value.toInt()) }

                primitiveType.validateValue(actualValue)
            }

            is DoubleType -> {
                val actualValue = primitiveType.value(normalized)
                minConstraint?.validate(actualValue)
                maxConstraint?.validate(actualValue)
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

        operator fun invoke(
            rawFacets: Iterable<XSFacet>,
            schemaLike: ResolvedSchemaLike,
            primitiveType: PrimitiveDatatype<VAnySimpleType>,
            relaxedLength: Boolean
        ): FacetList = FacetList(rawFacets.map { ResolvedFacet(it, schemaLike, primitiveType) }, relaxedLength)

        fun safe(facets: List<XSFacet>, schema: ResolvedSchemaLike, baseType: ResolvedSimpleType): FacetList {
            val relaxedLength = baseType is IDRefsType || baseType is NMTokensType
            return when {
                // String has no restrictions
                baseType == AnySimpleType -> FacetList(facets, schema, StringType, relaxedLength)

                // Use token as strings have collapsed strings
                baseType.mdlVariety == Variety.LIST -> FacetList(facets, schema, TokenType, relaxedLength)

                // Don't bother checking
                baseType.mdlVariety == Variety.UNION -> FacetList(facets, schema, StringType, relaxedLength)

                baseType.mdlPrimitiveTypeDefinition == null ->
                    error("No primitive type for base type: $baseType")

                else -> FacetList(facets, schema, baseType.mdlPrimitiveTypeDefinition!!, relaxedLength)
            }
        }

        operator fun invoke(facets: Iterable<ResolvedFacet>, relaxedLength: Boolean): FacetList {
            val otherFacets: MutableList<ResolvedFacet> = mutableListOf()
            val assertions: MutableList<ResolvedAssertionFacet> = mutableListOf()
            val enumeration: MutableList<ResolvedEnumeration<VAnySimpleType>> = mutableListOf()
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
                        if (maxConstraint != null) error("3.4.3(2) - multiple max facets") else maxConstraint = facet

                    is ResolvedMaxInclusive ->
                        if (maxConstraint != null) error("3.4.3(2) - multiple max facets") else maxConstraint = facet

                    is ResolvedMinExclusive ->
                        if (minConstraint != null) error("3.4.3(2) - multiple min facets") else minConstraint = facet

                    is ResolvedMinInclusive ->
                        if (minConstraint != null) error("3.4.3(2) - multiple min facets") else minConstraint = facet

                    is ResolvedEnumeration<*> -> enumeration.add(facet)
                    is ResolvedExplicitTimezone ->
                        if (explicitTimezone != null) error("3.4.3(2) - multiple explicitTimezone facets") else explicitTimezone =
                            facet

                    is ResolvedFractionDigits ->
                        if (fractionDigits != null) error("3.4.3(2) - multiple fractionDigits facets") else fractionDigits = facet

                    // use instances to allow for min/max lengths https://www.w3.org/Bugs/Public/show_bug.cgi?id=6446
                    is ResolvedLength -> {
                        when {
                            relaxedLength && minLength is ResolvedMinLength -> check(minLength.value== 1uL && facet.value>=1uL) { "minLength > length" }
                            minLength != null || maxLength != null -> error("3.4.3(2) - multiple length facets")
                        }
                        // outside of when, always override using the length
                        minLength = facet
                        maxLength = facet
                    }

                    is ResolvedMaxLength -> when {
                        maxLength != null -> error("3.4.3(2) - multiple maxLength facets")
                        else -> maxLength = facet
                    }

                    is ResolvedMinLength -> when {
                    // use instance check to both length and minimum https://www.w3.org/Bugs/Public/show_bug.cgi?id=6446
                        relaxedLength && minLength is ResolvedLength -> check(facet.value == 1uL && minLength.value>=1uL) { "MaxLength < length" }
                        minLength != null -> error("3.4.3(2) - multiple maxLength facets")
                        else -> minLength = facet
                    }

                    is ResolvedPattern -> pattern?.let {// combine by or
                        ResolvedPattern(XSPattern("(?:${it.value}|${facet.value})"), it.schema)
                    } ?: run {
                        pattern = facet
                    }

                    is ResolvedTotalDigits ->
                        if (totalDigits != null) error("3.4.3(2) - multiple totalDigits facets") else totalDigits = facet

                    is ResolvedWhiteSpace ->
                        if (whiteSpace != null) error("3.4.3(2) - multiple whiteSpace facets") else whiteSpace = facet
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
