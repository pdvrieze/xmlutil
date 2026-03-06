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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.ResPrimitiveDatatype
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSMaxExclusive
import io.github.pdvrieze.formats.xmlschema.resolved.SchemaVersion
import io.github.pdvrieze.xml.schematypes.facets.FacetMaxExclusive
import io.github.pdvrieze.xml.schematypes.values.*

class ResolvedMaxExclusive(
    rawPart: XSMaxExclusive,
    override val value: XsdAnySimple
) : ResolvedMaxBoundFacet(rawPart), FacetMaxExclusive {
    override val isInclusive: Boolean get() = false
    override val fixed: Boolean? = rawPart.fixed

    override fun validate(type: ResPrimitiveDatatype<*>, decimal: XsdDecimal, version: SchemaVersion) {
        type.validateValue(value, version)
        val v = (type.value(value) as XsdDecimal)
        check(decimal < v)
    }

    override fun validate(value: XsdAnySimple) = when (this.value) {
        is XsdDecimal -> {
            check(value is XsdDecimal)
            check(value < this.value)
        }
        is IXsdDateTime -> {
            check(value is IXsdDateTime)
            check(value < this.value)
        }
        else -> error("Value $value cannot be validated")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ResolvedMaxExclusive

        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String = "-$value)"

    companion object {
        operator fun invoke(rawPart: XSMaxExclusive, primitiveDatatype: ResPrimitiveDatatype<XsdAtomic>): ResolvedMaxExclusive {
            return ResolvedMaxExclusive(rawPart, primitiveDatatype.mdlPrimitiveTypeDefinition.value(rawPart.value))
        }

        internal fun createUnverified(value: XsdAnySimple): ResolvedMaxExclusive {
            return ResolvedMaxExclusive(XSMaxExclusive(XsdString(value.xmlString)), value)
        }
    }

}
