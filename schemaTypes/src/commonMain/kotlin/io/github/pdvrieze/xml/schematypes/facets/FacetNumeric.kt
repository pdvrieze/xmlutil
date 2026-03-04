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

import io.github.pdvrieze.xml.schematypes.values.XsdQName
import nl.adaptivity.xmlutil.XMLConstants

interface FacetNumeric : FundamentalFacet {
    val isNumeric: Boolean
    override val facetName: XsdQName
        get() = name

    private enum class Numeric(override val isNumeric: Boolean): FacetNumeric {
        TRUE(true), FALSE(false);
    }

    companion object {
        val TRUE: FacetNumeric get() = Numeric.TRUE
        val FALSE: FacetNumeric get() = Numeric.FALSE

        operator fun invoke(isNumeric: Boolean): FacetNumeric =
            if(isNumeric) Numeric.TRUE else Numeric.FALSE

        val name: XsdQName = XsdQName(XMLConstants.XSD_NS_URI, "numeric", XMLConstants.XSD_PREFIX)

    }
}
