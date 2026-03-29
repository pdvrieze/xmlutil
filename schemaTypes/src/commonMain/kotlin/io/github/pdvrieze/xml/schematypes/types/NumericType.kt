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
import io.github.pdvrieze.xml.schematypes.values.XsdAnySimple
import io.github.pdvrieze.xml.schematypes.values.XsdDecimal
import io.github.pdvrieze.xml.schematypes.values.XsdDouble
import io.github.pdvrieze.xml.schematypes.values.XsdInteger
import io.github.pdvrieze.xml.schematypes.values.XsdNumeric
import io.github.pdvrieze.xml.schematypes.values.XsdQName
import nl.adaptivity.xmlutil.XMLConstants

/**
 * Union of all numeric types. Used by XPath/XQuery functions.
 */
interface NumericType<out T: XsdNumeric<*>> : AnySimpleUnion<T> {

    override val ordered: FacetOrdered get() = FacetOrdered.PARTIAL
    override val bounded: FacetBounded get() = FacetBounded.BOUNDED
    override val cardinality: FacetCardinality get() = FacetCardinality.FINITE
    override val numeric: FacetNumeric get() = FacetNumeric.TRUE

    override val name: XsdQName? get() = Instance.name

    override val members: Collection<AnySimpleType<T>>

    override val baseType: AnySimpleType<XsdAnySimple>

    fun fromString(value: CharSequence): XsdNumeric<*>

    override fun isPureUnion(): Boolean = true

    override val constrainingFacets: List<ConstrainingFacet>
        get() = Instance.constrainingFacets

    object Instance: NumericType<XsdNumeric<*>>, AnySimpleUnion<XsdNumeric<*>>, BuiltinType {
        override val name: XsdQName = XsdQName(XMLConstants.XSD_NS_URI, "numeric", "xs")

        override val baseType: AnySimpleType<XsdAnySimple>
            get() = AnySimpleType.Instance

        override val members: Collection<AnySimpleType<XsdNumeric<*>>> = listOf(
            DecimalType.Instance, FloatType.Instance, DoubleType.Instance,
        )


        override val constrainingFacets: List<ConstrainingFacet> = listOf(
            FacetWhiteSpace(WhitespaceValue.COLLAPSE, true)
        )

        override fun fromString(value: CharSequence): XsdNumeric<*> {
            return XsdDouble(value)
        }
    }





}
