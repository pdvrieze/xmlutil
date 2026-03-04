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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSAssertionFacet
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedAnnotated
import io.github.pdvrieze.formats.xpath.XPathExpression
import io.github.pdvrieze.xml.schematypes.facets.FacetAssertion
import io.github.pdvrieze.xml.schematypes.values.XsdString

class ResolvedAssertionFacet(rawPart: XSAssertionFacet) :
    ResolvedFacet(rawPart), FacetAssertion {

    override val model by lazy { ResolvedAnnotated.Model(rawPart) }

    override val test: XsdString get() = XsdString(testExpr.xmlString)

    val testExpr: XPathExpression = rawPart.testExpr
    override val xpathDefaultNamespace = rawPart.xpathDefaultNamespace

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ResolvedAssertionFacet

        if (testExpr != other.testExpr) return false
        return xpathDefaultNamespace == other.xpathDefaultNamespace
    }

    override fun hashCode(): Int {
        var result = testExpr?.hashCode() ?: 0
        result = 31 * result + (xpathDefaultNamespace?.hashCode() ?: 0)
        return result
    }


}
