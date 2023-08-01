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

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.XPathExpression
import io.github.pdvrieze.formats.xmlschema.types.T_XPathDefaultNamespace
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("assert", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSAssert : XSAnnotatedBase, XSIAssertCommon {
    override val test: XPathExpression?
    override val xPathDefaultNamespace: T_XPathDefaultNamespace?

    constructor(
        test: XPathExpression,
        xPathDefaultNamespace: T_XPathDefaultNamespace? = null,
        id: VID?,
        annotation: XSAnnotation? = null,
        otherAttrs: Map<QName, String> = emptyMap()
    ) : super(id, annotation, otherAttrs) {
        this.test = test
        this.xPathDefaultNamespace = xPathDefaultNamespace
    }

}

