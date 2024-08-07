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
import io.github.pdvrieze.formats.xmlschema.types.*
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
abstract class XSAnyBase : XSAnnotatedBase {
    abstract val notQName: VQNameListBase<out VQNameListBase.IElem>?

    @XmlElement(false)
    val namespace: VNamespaceList?

    @XmlElement(false)
    val notNamespace: VNotNamespaceList?

    @XmlElement(false)
    @XmlSerialName("processContents", "", "")
    abstract val processContents: VProcessContents?

    constructor(
        namespace: VNamespaceList? = null,
        notNamespace: VNotNamespaceList? = null,
        id: VID?,
        annotation: XSAnnotation?,
        otherAttrs: Map<QName, String>
    ) : super(id, annotation, otherAttrs) {
        this.namespace = namespace
        this.notNamespace = notNamespace
    }
}
