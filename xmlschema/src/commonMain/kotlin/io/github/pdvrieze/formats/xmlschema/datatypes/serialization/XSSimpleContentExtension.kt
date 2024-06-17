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
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.XMLConstants.XSD_NS_URI
import nl.adaptivity.xmlutil.XMLConstants.XSD_PREFIX
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("extension", XSD_NS_URI, XSD_PREFIX)
class XSSimpleContentExtension : XSSimpleContentDerivation {

    constructor(
        base: QName,
        id: VID? = null,
        attributes: List<XSLocalAttribute> = emptyList(),
        attributeGroups: List<XSAttributeGroupRef> = emptyList(),
        anyAttribute: XSAnyAttribute? = null,
        asserts: List<XSAssert> = emptyList(),
        annotation: XSAnnotation? = null,
        otherAttrs: Map<QName, String> = emptyMap()
    ) : super(id, attributes, attributeGroups, anyAttribute, asserts, annotation, otherAttrs) {
        this.base = base
    }

    override val base: @Serializable(QNameSerializer::class) QName

    /**
     * Mark the derivation as extension
     */
    @Deprecated("Use 'is XSSimpleContentExtension' instead")
    override val derivationMethod: VDerivationControl.EXTENSION get() = VDerivationControl.EXTENSION
}
