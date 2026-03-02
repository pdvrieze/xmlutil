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

package io.github.pdvrieze.xml.schematypes.builtins

import io.github.pdvrieze.xml.schematypes.WhitespaceValue
import io.github.pdvrieze.xml.schematypes.facets.*
import io.github.pdvrieze.xml.schematypes.values.XSQName
import io.github.pdvrieze.xml.schematypes.values.instances.XSLanguageImpl
import nl.adaptivity.xmlutil.XMLConstants

interface LanguageType : BuiltinPrimitiveDatatype<XSLanguageImpl> {
    override val baseType: TokenType get() = TokenType.Instance

    override val ordered: FacetOrdered get() = FacetOrdered.FALSE
    override val bounded: FacetBounded get() = FacetBounded.UNBOUNDED
    override val cardinality: FacetCardinality get() = FacetCardinality.COUNTABLY_INFINITE
    override val numeric: FacetNumeric get() = FacetNumeric.FALSE

    override val name: XSQName get() = Instance.name

    override val constrainingFacets: List<ConstrainingFacet>
        get() = Instance.constrainingFacets

    object Instance : LanguageType {
        override val name: XSQName get() = XSQName(XMLConstants.XSD_NS_URI, "language", "xs")

        override val constrainingFacets: List<ConstrainingFacet> = listOf(
            FacetWhiteSpace(WhitespaceValue.COLLAPSE, true),
            FacetPattern("[a-zA-Z]{1,8}(-[a-zA-Z0-9]{1,8})*")
        )
    }

}
