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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSEnumeration
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedAnnotated
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSimpleType
import io.github.pdvrieze.formats.xmlschema.resolved.SchemaVersion

class ResolvedEnumeration<out T : Any>(
    rawPart: XSEnumeration,
    dataType: ResolvedSimpleType
) : ResolvedFacet(rawPart) {
    override val model by lazy { ResolvedAnnotated.Model(rawPart) }

    override fun checkFacetValid(type: ResolvedSimpleType, version: SchemaVersion) {
        type.validateValue(value, version)
    }

    @Suppress("UNCHECKED_CAST")
    val value: T = dataType.value(rawPart.value) as T

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ResolvedEnumeration<*>

        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }


}
