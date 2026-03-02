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

import io.github.pdvrieze.xml.schematypes.values.XSAnySimple
import io.github.pdvrieze.xml.schematypes.values.XSQName
import nl.adaptivity.xmlutil.XMLConstants.XSD_NS_URI
import nl.adaptivity.xmlutil.XMLConstants.XSD_PREFIX

interface FacetMaxInclusive : ConstrainingFacet.Fixed {

    val value: XSAnySimple

    private class Impl(
        override val value: XSAnySimple,
        override val fixed: Boolean?
    ): FacetMinInclusive

    override val name: XSQName get() = NAME

    companion object {
        operator fun invoke(value: XSAnySimple, fixed: Boolean? = null): FacetMinInclusive = Impl(value, fixed)

        val NAME: XSQName = XSQName(XSD_NS_URI, "maxInclusive", XSD_PREFIX)
    }

}
