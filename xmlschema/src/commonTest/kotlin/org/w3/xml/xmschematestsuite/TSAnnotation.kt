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

package org.w3.xml.xmschematestsuite

import io.github.pdvrieze.formats.xmlschema.datatypes.AnyURI
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.serialization.CompactFragmentSerializer
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.util.CompactFragment

@Serializable
@XmlSerialName("annotation", TS_NAMESPACE, TS_PREFIX)
class TSAnnotation(
    val elements: List<AnnotationElement> = emptyList()
) {

    @Serializable
    sealed class AnnotationElement

    @Serializable
    @XmlSerialName("appinfo", TS_NAMESPACE, TS_PREFIX)
    class AppInfo(
        @XmlValue
        @Serializable(CompactFragmentSerializer::class)
        val info: CompactFragment,
        val source: AnyURI,
        @XmlOtherAttributes
        val otherAttributes: Map<@Serializable(QNameSerializer::class) QName, String> = emptyMap()
    )

    @Serializable
    @XmlSerialName("documentation", TS_NAMESPACE, TS_PREFIX)
    class Documentation(
        @XmlValue
        @Serializable(CompactFragmentSerializer::class)
        val info: CompactFragment,
        val source: AnyURI,
        @XmlSerialName("lang", XMLConstants.XML_NS_URI, XMLConstants.XML_NS_PREFIX)
        val lang: String? = null,
        @XmlOtherAttributes
        val otherAttributes: Map<@Serializable(QNameSerializer::class) QName, String> = emptyMap()
    )
}
