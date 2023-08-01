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
import io.github.pdvrieze.formats.xmlschema.types.T_DerivationControl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("extension", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSSimpleContentExtension: XSSimpleContentDerivation { // TODO should not implement SimpleExtensionType

    constructor(
        base: QName,
        id: VID? = null,
        attributes: List<XSLocalAttribute> = emptyList(),
        attributeGroups: List<XSAttributeGroupRef> = emptyList(),
        anyAttribute: XSAnyAttribute? = null,
        assertions: List<XSAssert> = emptyList(),
        annotation: XSAnnotation? = null,
        otherAttrs: Map<QName, String> = emptyMap()
    ) : super(id, attributes, attributeGroups, anyAttribute, assertions, annotation, otherAttrs) {
        this.base = base
    }

    override val base: @Serializable(QNameSerializer::class) QName

    override val derivationMethod: T_DerivationControl.EXTENSION get() = T_DerivationControl.EXTENSION
}
