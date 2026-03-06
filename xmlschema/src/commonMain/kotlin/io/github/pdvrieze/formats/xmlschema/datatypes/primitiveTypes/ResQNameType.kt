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

package io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSWhiteSpace
import io.github.pdvrieze.formats.xmlschema.resolved.SchemaVersion
import io.github.pdvrieze.formats.xmlschema.resolved.facets.FacetList
import io.github.pdvrieze.formats.xmlschema.resolved.facets.ResolvedWhiteSpace
import io.github.pdvrieze.formats.xmlschema.types.FundamentalFacets
import io.github.pdvrieze.xml.schematypes.WhitespaceValue
import io.github.pdvrieze.xml.schematypes.facets.FacetCardinality
import io.github.pdvrieze.xml.schematypes.facets.FacetOrdered
import io.github.pdvrieze.xml.schematypes.types.QNameType
import io.github.pdvrieze.xml.schematypes.values.XsdAnySimple
import io.github.pdvrieze.xml.schematypes.values.XsdQName
import io.github.pdvrieze.xml.schematypes.values.XsdString
import io.github.pdvrieze.xml.schematypes.values.instances.XsdPrefixString

object ResQNameType : ResPrimitiveDatatype<XsdQName>, QNameType<XsdQName> {
    override val baseType: ResAnyAtomicType get() = ResAnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = FacetOrdered.FALSE,
        bounded = false,
        cardinality = FacetCardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XsdString): XsdQName {
        return (normalized as? XsdPrefixString)?.toQName() ?: XsdQName(normalized.xmlString)
    }

    override fun value(maybeValue: XsdAnySimple): XsdQName {
        return maybeValue as? XsdQName ?: value((maybeValue as? XsdString) ?: XsdString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XsdQName)
    }

    override fun validate(representation: XsdString, version: SchemaVersion) {
        val localName = when (representation) {
            is XsdPrefixString -> representation.localname
            else -> representation.xmlString
        }
        check(localName.indexOf(':') < 0) { "local names cannot contain : characters" }
    }
}
