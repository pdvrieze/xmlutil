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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.types.VXPathDefaultNamespace
import io.github.pdvrieze.formats.xpath.XPathExpression
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.SerializableQName
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("alternative", XMLConstants.XSD_NS_URI, XMLConstants.XSD_PREFIX)
class XSAlternative : XSAnnotatedBase {

    val localType: XSLocalType?

    @XmlElement(false)
    val test: XPathExpression?

    @XmlElement(false)
    val type: SerializableQName?

    val xpathDefaultNamespace: VXPathDefaultNamespace?

    constructor(
        localType: XSLocalType? = null,
        test: XPathExpression? = null,
        type: QName? = null,
        xpathDefaultNamespace: VXPathDefaultNamespace? = null,
        id: VID? = null,
        annotation: XSAnnotation? = null,
        otherAttrs: Map<SerializableQName, String> = emptyMap(),
    ) : super(id, annotation, otherAttrs) {
        this.localType = localType
        this.test = test
        this.type = type
        this.xpathDefaultNamespace = xpathDefaultNamespace
    }
}
