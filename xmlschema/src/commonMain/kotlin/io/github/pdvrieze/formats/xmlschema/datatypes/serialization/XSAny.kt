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


package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.types.*
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("any", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSAny : XSAnyBase, XSI_AllParticle {
    override val maxOccurs: VAllNNI?
    override val minOccurs: VNonNegativeInteger?
    
    @XmlElement(false)
    val notQName: VQNameList?

    @XmlElement(false)
    @XmlSerialName("processContents", "", "")
    override val processContents: VProcessContents?

    constructor(
        processContents: VProcessContents? = null,
        notQName: VQNameList? = null,
        namespace: VNamespaceList? = null,
        notNamespace: VNotNamespaceList? = null,
        minOccurs: VNonNegativeInteger? = null,
        maxOccurs: VAllNNI? = null,
        id: VID? = null,
        annotation: XSAnnotation? = null,
        otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String> = emptyMap()
    ) : super(namespace, notNamespace, id, annotation, otherAttrs) {
        this.maxOccurs = maxOccurs
        this.minOccurs = minOccurs
        this.notQName = notQName
        this.processContents = processContents
    }
}
