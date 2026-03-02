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

import io.github.pdvrieze.xml.schematypes.values.XSQName
import kotlinx.serialization.SerialName
import nl.adaptivity.xmlutil.XMLConstants.XSD_NS_URI
import nl.adaptivity.xmlutil.XMLConstants.XSD_PREFIX

interface FacetExplicitTimezone : ConstrainingFacet {
    val isOptional: Boolean
    val isProhibited: Boolean
    val isRequired: Boolean

    private enum class Impl : FacetExplicitTimezone {
        @SerialName("optional")
        OPTIONAL {
            override val isOptional: Boolean get() = true
        },

        @SerialName("required")
        REQUIRED {
            override val isRequired: Boolean get() = true
        },

        @SerialName("prohibited")
        PROHIBITED {
            override val isProhibited: Boolean get() = true
        };

        override val isOptional: Boolean get() = false
        override val isProhibited: Boolean get() = false
        override val isRequired: Boolean get() = false
    }

    override val name: XSQName get() = NAME

    companion object {
        val NAME: XSQName = XSQName(XSD_NS_URI, "explicitTimezone", XSD_PREFIX)
        val OPTIONAL: FacetExplicitTimezone = Impl.OPTIONAL
        val REQUIRED: FacetExplicitTimezone = Impl.REQUIRED
        val PROHIBITED: FacetExplicitTimezone = Impl.PROHIBITED
    }


}
