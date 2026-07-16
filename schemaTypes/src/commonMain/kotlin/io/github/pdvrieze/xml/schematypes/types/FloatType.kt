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

package io.github.pdvrieze.xml.schematypes.types

import io.github.pdvrieze.xml.schematypes.WhitespaceValue
import io.github.pdvrieze.xml.schematypes.facets.*
import io.github.pdvrieze.xml.schematypes.values.*
import nl.adaptivity.xmlutil.XMLConstants

interface FloatType<out T : XsdFloat> : PrimitiveType<T>, NumericType<T> {

    override val ordered: FacetOrdered get() = FacetOrdered.PARTIAL
    override val bounded: FacetBounded get() = FacetBounded.BOUNDED
    override val cardinality: FacetCardinality get() = FacetCardinality.FINITE
    override val numeric: FacetNumeric get() = FacetNumeric.TRUE

    override val name: XsdQName? get() = Instance.name
    override val primitiveType: PrimitiveTypeInstance<XsdFloat> get() = Instance

    override val members: Collection<FloatType<T>> get() = emptyList()
    override val constrainingFacets: List<ConstrainingFacet>
        get() = Instance.constrainingFacets

    override fun isBaseOf(maybeSubType: AnyType): Boolean {
        return super<PrimitiveType>.isBaseOf(maybeSubType)
    }

    override fun fromString(value: CharSequence): T

    override fun castFrom(other: XsdAtomic): T {
        return super<PrimitiveType>.castFrom(other)
    }

    object Instance : FloatType<XsdFloat>, PrimitiveTypeInstance<XsdFloat>, BuiltinType {
        override val name: XsdQName = XsdQName(XMLConstants.XSD_NS_URI, "float", "xs")

        override val baseType: AnyAtomicType<*> get() = AnyAtomicType.Instance

        override val constrainingFacets: List<ConstrainingFacet> = listOf(
            FacetWhiteSpace(WhitespaceValue.COLLAPSE, true)
        )

        override fun fromString(value: CharSequence): XsdFloat {
            return XsdFloat(value)
        }

        override fun castFrom(other: XsdAtomic): XsdFloat = when (other) {
            is XsdFloat -> other
            is XsdDouble -> XsdFloat(other.value.toFloat())
            is XsdDecimal -> XsdFloat(other.toFloat())
            else -> fromString(other.xmlString)
        }
    }

}
