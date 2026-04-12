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
import io.github.pdvrieze.xml.schematypes.values.instances.XsdDateTimeStampImpl
import nl.adaptivity.xmlutil.XMLConstants

interface DateTimeStampType<out T : XsdDateTimeStamp> : DateTimeType<T> {
    override val baseType: DateTimeType<*> get() = DateTimeType.Instance

    override val ordered: FacetOrdered get() = FacetOrdered.PARTIAL
    override val bounded: FacetBounded get() = FacetBounded.UNBOUNDED
    override val cardinality: FacetCardinality get() = FacetCardinality.COUNTABLY_INFINITE
    override val numeric: FacetNumeric get() = FacetNumeric.FALSE

    override val name: XsdQName? get() = Instance.name

    override val constrainingFacets: List<ConstrainingFacet>
        get() = Instance.constrainingFacets

    object Instance: DateTimeStampType<XsdDateTimeStamp>, BuiltinType {
        override val name: XsdQName = XsdQName(XMLConstants.XSD_NS_URI, "dateTimeStamp", "xs")

        override val constrainingFacets: List<ConstrainingFacet> = listOf(
            FacetWhiteSpace(WhitespaceValue.COLLAPSE, true),
            FacetExplicitTimezone.REQUIRED,
        )

        override fun fromString(value: CharSequence): XsdDateTimeStamp = XsdDateTimeStamp(value)

        override fun castFrom(other: XsdAtomic): XsdDateTimeStamp = when (other) {
            is XsdDateTimeStampImpl -> other
            is XsdDateTimeStamp -> XsdDateTimeStampImpl(other.instant(), other.timezone)
            is XsdDateTime -> {
                val tz = requireNotNull(other.timezone) { "No timezone for casting to dateTimeStamp"}
                XsdDateTimeStampImpl(other.instant(), tz)
            }
            is XsdDate -> {
                val tz = requireNotNull(other.timezone) { "No timezone for casting to dateTimeStamp"}
                val inst = XsdDateTime(other, XsdTime(0u,0u,0u)).instant()
                XsdDateTimeStampImpl(inst, tz)
            }
            is UntypedAtomicType.XsdUntyped, is XsdString -> XsdDateTimeStampImpl(other.xmlString)
            else -> throw IllegalArgumentException("Cannot cast from $other of type ${other.schemaType}")
        }

        override fun canCastFrom(sourceType: AnySimpleType<*>): Boolean = when (sourceType) {
            is DateTimeType -> true
            is DateType -> true
            is UntypedAtomicType, is XsdString -> true
            else -> false
        }
    }

}
