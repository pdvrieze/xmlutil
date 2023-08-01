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

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.SerializableQName
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlId
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("union", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSSimpleUnion : XSSimpleDerivation {
    val simpleTypes: List<XSLocalSimpleType>

    @XmlElement(false)
    val memberTypes: List<SerializableQName>?

    constructor(
        simpleTypes: List<XSLocalSimpleType> = emptyList(),
        memberTypes: List<@Serializable(QNameSerializer::class) QName>? = null,
        id: VID? = null,
        annotation: XSAnnotation? = null,
        otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String> = emptyMap()
    ) : super(id, annotation, otherAttrs) {
        this.simpleTypes = simpleTypes
        this.memberTypes = memberTypes
    }
}
