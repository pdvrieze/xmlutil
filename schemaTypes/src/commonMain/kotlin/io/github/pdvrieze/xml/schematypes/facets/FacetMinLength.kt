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

package io.github.pdvrieze.xml.schematypes.facets

import io.github.pdvrieze.xml.schematypes.values.XsdQName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.XMLConstants.XSD_NS_URI
import nl.adaptivity.xmlutil.XMLConstants.XSD_PREFIX
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("minLength", XSD_NS_URI, XSD_PREFIX)
class FacetMinLength(
    override val value: ULong,
    override val fixed: Boolean? = null,
) : ConstrainingFacet.Numeric, ConstrainingFacet.Fixed {
    override val facetName: XsdQName get() = NAME

    companion object {
        val NAME: XsdQName = XsdQName(XSD_NS_URI, "minLength", XSD_PREFIX)
    }

}
