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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.PrimitiveDatatype
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnnotation
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.*
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedPart
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSchemaLike
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSimpleType

sealed class ResolvedFacet(override val schema: ResolvedSchemaLike) : ResolvedPart {
    abstract override val rawPart: XSFacet

    val fixed get() = rawPart.fixed

    val id: VID? get() = rawPart.id
    val annotation: XSAnnotation? get() = rawPart.annotation

    open fun check(type: ResolvedSimpleType) {}

    open fun validate(type: PrimitiveDatatype, decimal: VDecimal) {}

    open fun validate(float: VFloat) {}

    open fun validate(float: VDouble) {}

    companion object {
        operator fun invoke(rawPart: XSFacet, schema: ResolvedSchemaLike): ResolvedFacet = when (rawPart) {
            is XSAssertionFacet -> ResolvedAssertionFacet(rawPart, schema)
            is XSEnumeration -> ResolvedEnumeration(rawPart, schema)
            is XSExplicitTimezone -> ResolvedExplicitTimezone(rawPart, schema)
            is XSFractionDigits -> ResolvedFractionDigits(rawPart, schema)
            is XSLength -> ResolvedLength(rawPart, schema)
            is XSMaxExclusive -> ResolvedMaxExclusive(rawPart, schema)
            is XSMaxInclusive -> ResolvedMaxInclusive(rawPart, schema)
            is XSMaxLength -> ResolvedMaxLength(rawPart, schema)
            is XSMinExclusive -> ResolvedMinExclusive(rawPart, schema)
            is XSMinInclusive -> ResolvedMinInclusive(rawPart, schema)
            is XSMinLength -> ResolvedMinLength(rawPart, schema)
            is XSPattern -> ResolvedPattern(rawPart, schema)
            is XSTotalDigits -> ResolvedTotalDigits(rawPart, schema)
            is XSWhiteSpace -> ResolvedWhiteSpace(rawPart, schema)
        }
    }
}


