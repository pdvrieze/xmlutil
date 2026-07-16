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
import io.github.pdvrieze.xml.schematypes.values.instances.BigDecimal
import io.github.pdvrieze.xml.schematypes.values.instances.XsdBigDecimal
import nl.adaptivity.xmlutil.XMLConstants

interface DecimalType<out T: XsdDecimal> : PrimitiveType<T>, NumericType<T> {

    override val ordered: FacetOrdered get() = FacetOrdered.TOTAL
    override val bounded: FacetBounded get() = FacetBounded.UNBOUNDED
    override val cardinality: FacetCardinality get() = FacetCardinality.COUNTABLY_INFINITE
    override val numeric: FacetNumeric get() = FacetNumeric.TRUE

    override val name: XsdQName? get() = Instance.name
    override val baseType: AnyAtomicType<*>
    override val primitiveType: PrimitiveTypeInstance<XsdDecimal> get() = Instance

    override val members: Collection<DecimalType<T>> get() = emptyList()

    override val constrainingFacets: List<ConstrainingFacet>
        get() = Instance.constrainingFacets

    override fun isBaseOf(maybeSubType: AnyType): Boolean {
        return super<PrimitiveType>.isBaseOf(maybeSubType)
    }

    override fun castFrom(other: XsdAtomic): T {
        return super<PrimitiveType>.castFrom(other)
    }

    object Instance: DecimalType<XsdDecimal>, PrimitiveTypeInstance<XsdDecimal>, BuiltinType {
        override val name: XsdQName = XsdQName(XMLConstants.XSD_NS_URI, "decimal", "xs")

        override val baseType: AnyAtomicType<*> get() = AnyAtomicType.Instance

        override val constrainingFacets: List<ConstrainingFacet> = listOf(
            FacetWhiteSpace(WhitespaceValue.COLLAPSE, true)
        )

        override fun fromString(value: CharSequence): XsdDecimal {
            return BigDecimal(value)
        }

        override fun castFrom(other: XsdAtomic): XsdDecimal = when (other) {
            is XsdBigDecimal -> other
            is XsdFloat,
            is XsdDouble -> BigDecimal(other.xmlString)
            is XsdDecimal -> other.toBigDecimal()
            is XsdBoolean -> BigDecimal(if (other.value) 1 else 0)
            is XsdString -> BigDecimal(other.xmlString)
            else -> throw IllegalArgumentException("Cannot cast $other to decimal")
        }
    }




}
