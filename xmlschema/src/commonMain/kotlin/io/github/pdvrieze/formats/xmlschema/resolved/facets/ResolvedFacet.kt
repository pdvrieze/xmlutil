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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.*
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.AnyPrimitiveDatatype
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.*
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedAnnotated
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSchemaLike
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSimpleType
import io.github.pdvrieze.formats.xmlschema.resolved.SchemaVersion

sealed class ResolvedFacet(rawPart: XSFacet, val schema: ResolvedSchemaLike) :
    ResolvedAnnotated {
    abstract override val model: ResolvedAnnotated.IModel

    val mdlFixed = rawPart.fixed

    open fun checkFacetValid(type: ResolvedSimpleType, version: SchemaVersion) {}

    open fun validate(type: AnyPrimitiveDatatype, decimal: VDecimal, version: SchemaVersion) {}

    open fun validate(value: VAnySimpleType) {}

    open fun validate(float: VFloat) {}

    open fun validate(double: VDouble) {}

    open fun validate(duration: VDuration) {}

    companion object {
        operator fun invoke(
            rawPart: XSFacet,
            schema: ResolvedSchemaLike,
            dataType: ResolvedSimpleType
        ): ResolvedFacet = when (rawPart) {
            is XSAssertionFacet -> ResolvedAssertionFacet(rawPart, schema)
            is XSEnumeration -> ResolvedEnumeration<VAnySimpleType>(rawPart, schema, dataType)
            is XSExplicitTimezone -> ResolvedExplicitTimezone(rawPart, schema)
            is XSFractionDigits -> ResolvedFractionDigits(rawPart, schema)
            is XSLength -> ResolvedLength(rawPart, schema)
            is XSMaxExclusive -> ResolvedMaxExclusive(rawPart, schema, dataType.mdlPrimitiveTypeDefinition!!)
            is XSMaxInclusive -> ResolvedMaxInclusive(rawPart, schema, dataType.mdlPrimitiveTypeDefinition!!)
            is XSMaxLength -> ResolvedMaxLength(rawPart, schema)
            is XSMinExclusive -> ResolvedMinExclusive(rawPart, schema, dataType.mdlPrimitiveTypeDefinition!!)
            is XSMinInclusive -> ResolvedMinInclusive(rawPart, schema, dataType.mdlPrimitiveTypeDefinition!!)
            is XSMinLength -> ResolvedMinLength(rawPart, schema)
            is XSPattern -> ResolvedPattern(rawPart, schema)
            is XSTotalDigits -> ResolvedTotalDigits(rawPart, schema)
            is XSWhiteSpace -> ResolvedWhiteSpace(rawPart, schema)
        }
    }
}


