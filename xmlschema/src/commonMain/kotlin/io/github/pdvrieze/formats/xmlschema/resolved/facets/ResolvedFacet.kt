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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VDecimal
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VDouble
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VFloat
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.PrimitiveDatatype
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.*
import io.github.pdvrieze.formats.xmlschema.resolved.*

sealed class ResolvedFacet(rawPart: XSFacet, override val schema: ResolvedSchemaLike) :
    ResolvedAnnotated {
    abstract override val model: ResolvedAnnotated.IModel

    abstract override val rawPart: XSFacet

    val mdlFixed = rawPart.fixed

    open fun checkFacetValid(type: ResolvedSimpleType) {}

    open fun validate(type: PrimitiveDatatype, decimal: VDecimal) {}

    open fun validate(float: VFloat) {}

    open fun validate(float: VDouble) {}

    companion object {
        operator fun invoke(rawPart: XSFacet, schema: ResolvedSchemaLike, primitiveDatatype: PrimitiveDatatype?): ResolvedFacet = when (rawPart) {
            is XSAssertionFacet -> ResolvedAssertionFacet(rawPart, schema)
            is XSEnumeration -> ResolvedEnumeration(rawPart, schema, primitiveDatatype)
            is XSExplicitTimezone -> ResolvedExplicitTimezone(rawPart, schema)
            is XSFractionDigits -> ResolvedFractionDigits(rawPart, schema)
            is XSLength -> ResolvedLength(rawPart, schema)
            is XSMaxExclusive -> ResolvedMaxExclusive(rawPart, schema, primitiveDatatype)
            is XSMaxInclusive -> ResolvedMaxInclusive(rawPart, schema, primitiveDatatype)
            is XSMaxLength -> ResolvedMaxLength(rawPart, schema)
            is XSMinExclusive -> ResolvedMinExclusive(rawPart, schema, primitiveDatatype)
            is XSMinInclusive -> ResolvedMinInclusive(rawPart, schema, primitiveDatatype)
            is XSMinLength -> ResolvedMinLength(rawPart, schema)
            is XSPattern -> ResolvedPattern(rawPart, schema)
            is XSTotalDigits -> ResolvedTotalDigits(rawPart, schema)
            is XSWhiteSpace -> ResolvedWhiteSpace(rawPart, schema)
        }
    }
}


