/*
 * Copyright (c) 2023-2026.
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

package io.github.pdvrieze.formats.xmlschema.resolved.facets

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VDecimal
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VDouble
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VDuration
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VFloat
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.ResPrimitiveDatatype
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.*
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedAnnotated
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSchemaLike
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSimpleType
import io.github.pdvrieze.formats.xmlschema.resolved.SchemaVersion
import io.github.pdvrieze.xml.schematypes.facets.ConstrainingFacet
import io.github.pdvrieze.xml.schematypes.values.XsdAnySimple

sealed class ResolvedFacet(rawPart: XSFacet) :
    ResolvedAnnotated, ConstrainingFacet {
    abstract override val model: ResolvedAnnotated.IModel

    val mdlFixed = rawPart.fixed

    open fun checkFacetValid(type: ResolvedSimpleType<*>, version: SchemaVersion) {}

    open fun validate(type: ResPrimitiveDatatype<*>, decimal: VDecimal, version: SchemaVersion) {}

    open fun validate(value: XsdAnySimple) {}

    open fun validate(float: VFloat) {}

    open fun validate(double: VDouble) {}

    open fun validate(duration: VDuration) {}

    companion object {
        operator fun invoke(
            rawPart: XSFacet,
            schema: ResolvedSchemaLike,
            dataType: ResolvedSimpleType<*>
        ): ResolvedFacet = when (rawPart) {
            is XSAssertionFacet -> ResolvedAssertionFacet(rawPart)
            is XSEnumeration -> ResolvedEnumeration<XsdAnySimple>(rawPart, dataType)
            is XSExplicitTimezone -> ResolvedExplicitTimezone(rawPart)
            is XSFractionDigits -> ResolvedFractionDigits(rawPart)
            is XSLength -> ResolvedLength(rawPart)
            is XSMaxExclusive -> ResolvedMaxExclusive(rawPart, dataType.mdlPrimitiveTypeDefinition!!)
            is XSMaxInclusive -> ResolvedMaxInclusive(rawPart, dataType.mdlPrimitiveTypeDefinition!!)
            is XSMaxLength -> ResolvedMaxLength(rawPart)
            is XSMinExclusive -> ResolvedMinExclusive(rawPart, dataType.mdlPrimitiveTypeDefinition!!)
            is XSMinInclusive -> ResolvedMinInclusive(rawPart, dataType.mdlPrimitiveTypeDefinition!!)
            is XSMinLength -> ResolvedMinLength(rawPart)
            is XSPattern -> ResolvedPattern(rawPart, schema.version)
            is XSTotalDigits -> ResolvedTotalDigits(rawPart)
            is XSWhiteSpace -> ResolvedWhiteSpace(rawPart)
        }
    }
}


