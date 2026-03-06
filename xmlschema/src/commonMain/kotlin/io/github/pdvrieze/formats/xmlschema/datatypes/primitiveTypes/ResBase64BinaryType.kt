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
import io.github.pdvrieze.formats.xmlschema.regex.XRegex
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedBuiltinAtomicType
import io.github.pdvrieze.formats.xmlschema.resolved.SchemaVersion
import io.github.pdvrieze.formats.xmlschema.resolved.facets.FacetList
import io.github.pdvrieze.formats.xmlschema.resolved.facets.ResolvedWhiteSpace
import io.github.pdvrieze.formats.xmlschema.types.FundamentalFacets
import io.github.pdvrieze.xml.schematypes.WhitespaceValue
import io.github.pdvrieze.xml.schematypes.facets.FacetCardinality
import io.github.pdvrieze.xml.schematypes.facets.FacetOrdered
import io.github.pdvrieze.xml.schematypes.types.Base64BinaryType
import io.github.pdvrieze.xml.schematypes.values.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
object ResBase64BinaryType : ResPrimitiveDatatype<XsdBase64Binary>,
    ResolvedBuiltinAtomicType<XsdBase64Binary>,
    Base64BinaryType<XsdBase64Binary> {

    override val name: XsdQName get() = Base64BinaryType.Instance.name

    fun length(representation: String): Int {
        // TODO don't actually decode just for length.
        return Base64.decode(representation).size
    }

    val regex = XRegex(
        "((([A-Za-z0-9+/] ?){4})*(([A-Za-z0-9+/] ?){3}[A-Za-z0-9+/]|([A-Za-z0-9+/] ?){2}[AEIMQUYcgkosw048] ?=|[A-Za-z0-9+/] ?[AQgw] ?= ?=))?",
        SchemaVersion.V1_0
    )

    override val baseType: ResAnyAtomicType get() = ResAnyAtomicType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = FacetOrdered.Companion.FALSE,
        bounded = false,
        cardinality = FacetCardinality.Companion.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XsdString): XsdBase64Binary {
        check(regex.matches(normalized))
        return XsdBase64Binary.Companion(Base64.Default.decode(normalized))
    }

    override fun value(maybeValue: XsdAnySimple): XsdByteArray {
        return maybeValue as? XsdByteArray ?: value((maybeValue as? XsdString) ?: XsdString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XsdByteArray)
    }

    override fun validate(representation: XsdString, version: SchemaVersion) {
        val _ = value(representation)
    }
}
