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
import io.github.pdvrieze.formats.xmlschema.types.VNamespaceList
import io.github.pdvrieze.formats.xmlschema.types.VNotNamespaceList
import io.github.pdvrieze.formats.xmlschema.types.VProcessContents
import io.github.pdvrieze.formats.xmlschema.types.VAttrQNameList
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("anyAttribute", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSAnyAttribute : XSAnyBase {
    val notQName: VAttrQNameList?

    @XmlElement(false)
    @XmlSerialName("processContents", "", "")
    override val processContents: VProcessContents?

    constructor(
        notQName: VAttrQNameList? = null,
        namespace: VNamespaceList? = null,
        notNamespace: VNotNamespaceList? = null,
        processContents: VProcessContents? = VProcessContents.STRICT,
        id: VID? = null,
        annotation: XSAnnotation? = null,
        otherAttrs: Map<QName, String> = emptyMap()
    ) : super(namespace, notNamespace, id, annotation, otherAttrs) {
        this.notQName = notQName
        this.processContents = processContents
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as XSAnyAttribute

        if (annotation != other.annotation) return false
        if (id != other.id) return false
        if (notQName != other.notQName) return false
        if (namespace != other.namespace) return false
        if (notNamespace != other.notNamespace) return false
        if (processContents != other.processContents) return false
        return otherAttrs == other.otherAttrs
    }

    override fun hashCode(): Int {
        var result = annotation?.hashCode() ?: 0
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + (notQName?.hashCode() ?: 0)
        result = 31 * result + (namespace?.hashCode() ?: 0)
        result = 31 * result + (notNamespace?.hashCode() ?: 0)
        result = 31 * result + processContents.hashCode()
        result = 31 * result + otherAttrs.hashCode()
        return result
    }

}
