/*
 * Copyright (c) 2023.
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

@file:UseSerializers(QNameSerializer::class)

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.XMLConstants.XSD_NS_URI
import nl.adaptivity.xmlutil.XMLConstants.XSD_PREFIX
import nl.adaptivity.xmlutil.serialization.XmlBefore
import nl.adaptivity.xmlutil.serialization.XmlId
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("group", XSD_NS_URI, XSD_PREFIX)
class XSGroup(
    val name: VNCName,
    @XmlId
    override val id: VID? = null,
    val content: XSGroupElement,
    @XmlBefore("*")
    override val annotation: XSAnnotation? = null,
    @XmlOtherAttributes
    override val otherAttrs: Map<QName, String> = emptyMap()
) : XSI_Annotated {

    @Serializable
    sealed class XSGroupElement : XSI_Annotated {
        abstract val particles: List<XSI_NestedParticle>
    }

    @XmlSerialName("all", XSD_NS_URI, XSD_PREFIX)
    @Serializable
    class All(
        override val particles: List<XSI_AllParticle> = emptyList(),
        @XmlBefore("*")
        override val annotation: XSAnnotation? = null,
        @XmlId
        override val id: VID? = null,
        @XmlOtherAttributes
        override val otherAttrs: Map<QName, String> = emptyMap()
    ) : XSGroupElement()

    @XmlSerialName("choice", XSD_NS_URI, XSD_PREFIX)
    @Serializable
    class Choice(
        override val particles: List<XSI_NestedParticle>,
        @XmlBefore("*")
        override val annotation: XSAnnotation? = null,
        @XmlId
        override val id: VID? = null,
        @XmlOtherAttributes
        override val otherAttrs: Map<QName, String> = emptyMap()
    ) : XSGroupElement()

    @XmlSerialName("sequence", XSD_NS_URI, XSD_PREFIX)
    @Serializable
    class Sequence(
        override val particles: List<XSI_NestedParticle>,
        @XmlBefore("*")
        override val annotation: XSAnnotation? = null,
        @XmlId
        override val id: VID? = null,
        @XmlOtherAttributes
        override val otherAttrs: Map<QName, String> = emptyMap()
    ) : XSGroupElement()

}
