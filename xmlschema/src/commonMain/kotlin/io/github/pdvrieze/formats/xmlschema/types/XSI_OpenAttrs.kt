/*
 * Copyright (c) 2021.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.github.pdvrieze.formats.xmlschema.types

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes

/**
 * This type is extended by almost all schema types to allow attributes from other namespaces to be
 * added to user schemas.
 */
interface XSI_OpenAttrs {
    @XmlOtherAttributes
    val otherAttrs: Map<SerializableQName, String>

    fun check(checkedTypes: MutableSet<QName>) {
        val xsAttrs = otherAttrs.keys.filter { it.prefix=="" || it.namespaceURI==XMLConstants.XSD_NS_URI }
        check(xsAttrs.isEmpty()) { "Open attributes in the empty or xmlschema namespace found: [${xsAttrs.joinToString()}]" }
    }
}

