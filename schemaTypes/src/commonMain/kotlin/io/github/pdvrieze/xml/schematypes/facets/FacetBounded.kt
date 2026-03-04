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

import io.github.pdvrieze.xml.schematypes.values.XsdBoolean
import io.github.pdvrieze.xml.schematypes.values.XsdQName
import nl.adaptivity.xmlutil.XMLConstants

interface FacetBounded : FundamentalFacet {
    val isBounded: Boolean

    override val facetName: XsdQName get() = name

    private enum class Bounded(val xmlString: String): FacetBounded {
        FALSE("false"),
        TRUE("true");

        override val isBounded: Boolean get() = this == TRUE
    }

    companion object {
        val BOUNDED: FacetBounded get() = Bounded.TRUE
        val UNBOUNDED: FacetBounded get() = Bounded.FALSE

        val name: XsdQName = XsdQName(XMLConstants.XSD_NS_URI, "bounded", XMLConstants.XSD_PREFIX)

        operator fun invoke(value: XsdBoolean): FacetBounded =
            if (value.value) Bounded.TRUE else Bounded.FALSE

        operator fun invoke(value: Boolean): FacetBounded =
            if (value) Bounded.TRUE else Bounded.FALSE

        operator fun invoke(xmlString: String): FacetBounded =
            Bounded.entries.single { it.xmlString ==xmlString }
    }
}
