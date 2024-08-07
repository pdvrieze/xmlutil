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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.IDateTime
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnySimpleType
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VDecimal
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VString
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.AnyPrimitiveDatatype
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.PrimitiveDatatype
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSMaxInclusive
import io.github.pdvrieze.formats.xmlschema.resolved.SchemaVersion

class ResolvedMaxInclusive internal constructor(
    rawPart: XSMaxInclusive,
    override val value: VAnySimpleType,
) : ResolvedMaxBoundFacet(rawPart) {
    override val isInclusive: Boolean get() = true

    override fun validate(type: AnyPrimitiveDatatype, decimal: VDecimal, version: SchemaVersion) {
        type.validateValue(value, version)
        val v = (type.value(value) as VDecimal)
        check(decimal <= v)
    }

    override fun validate(value: VAnySimpleType) = when (this.value) {
        is VDecimal -> {
            check(value is VDecimal)
            check(value <= this.value)
        }
        is IDateTime -> {
            check(value is IDateTime)
            check(value <= this.value)
        }
        else -> error("Value $value cannot be validated")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ResolvedMaxInclusive

        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String = "-$value]"

    companion object {
        operator fun invoke(rawPart: XSMaxInclusive, primitiveDatatype: PrimitiveDatatype<*>): ResolvedMaxInclusive {
            return ResolvedMaxInclusive(rawPart, primitiveDatatype.mdlPrimitiveTypeDefinition!!.value(rawPart.value))
        }

        internal fun createUnverified(value: VAnySimpleType): ResolvedMaxInclusive {
            return ResolvedMaxInclusive(XSMaxInclusive(VString(value.xmlString)), value)
        }
    }

}
