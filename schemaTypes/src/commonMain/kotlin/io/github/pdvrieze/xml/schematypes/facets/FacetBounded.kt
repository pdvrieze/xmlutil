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

package io.github.pdvrieze.xml.schematypes.facets

import io.github.pdvrieze.xml.schematypes.types.BooleanType
import io.github.pdvrieze.xml.schematypes.values.XSBoolean

interface FacetBounded : FundamentalFacet, XSBoolean {
    val isBounded: Boolean
    override val schemaType: BooleanType<*> get() = BooleanType.Instance

    private enum class Bounded(override val xmlString: String): FacetBounded {
        FALSE("false") {
            override val value: Boolean get() = false
        },
        TRUE("true") {
            override val value: Boolean get() = true
        };

        override val isBounded: Boolean get() = value
    }

    companion object {
        val BOUNDED: FacetBounded get() = Bounded.TRUE
        val UNBOUNDED: FacetBounded get() = Bounded.FALSE

        operator fun invoke(value: XSBoolean): FacetBounded =
            if (value.value) Bounded.TRUE else Bounded.FALSE

        operator fun invoke(value: Boolean): FacetBounded =
            if (value) Bounded.TRUE else Bounded.FALSE

        operator fun invoke(xmlString: String): FacetBounded =
            Bounded.entries.single { it.xmlString ==xmlString }
    }
}
