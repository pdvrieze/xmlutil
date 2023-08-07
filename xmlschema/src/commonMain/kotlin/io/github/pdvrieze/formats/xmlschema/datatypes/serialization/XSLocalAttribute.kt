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

import io.github.pdvrieze.formats.xmlschema.impl.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VString
import io.github.pdvrieze.formats.xmlschema.types.VFormChoice
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.SerializableQName
import nl.adaptivity.xmlutil.serialization.XmlBefore
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("attribute", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSLocalAttribute : XSAttribute {

    @XmlBefore("type")
    override val name: VNCName?

    @XmlElement(false)
    val form: VFormChoice?
    var ref: SerializableQName? = null
        private set

    @XmlElement(false)
    val use: XSAttrUse?

    val targetNamespace: VAnyURI?


    constructor(
        name: VNCName? = null,
        form: VFormChoice? = null,
        ref: QName? = null,
        use: XSAttrUse? = null,
        default: VString? = null,
        fixed: VString? = null,
        type: QName? = null,
        targetNamespace: VAnyURI? = null,
        inheritable: Boolean? = null,
        simpleType: XSLocalSimpleType? = null,
        id: VID? = null,
        annotation: XSAnnotation? = null,
        otherAttrs: Map<QName, String> = emptyMap()
    ) : super(default, fixed, type, inheritable, simpleType, id, annotation, otherAttrs) {
        this.name = name
        this.form = form
        this.ref = ref
        this.use = use
        this.targetNamespace = targetNamespace
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as XSLocalAttribute

        if (name != other.name) return false
        if (form != other.form) return false
        if (ref != other.ref) return false
        if (use != other.use) return false
        if (targetNamespace != other.targetNamespace) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (form?.hashCode() ?: 0)
        result = 31 * result + (ref?.hashCode() ?: 0)
        result = 31 * result + (use?.hashCode() ?: 0)
        result = 31 * result + (targetNamespace?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String = buildString {
        append("XSLocalAttribute(")
        if (name != null) append("name=$name, ")
        if (form != null) append("form=$form, ")
        if (ref != null) append("ref=$ref, ")
        if (use != null) append("use=$use, ")
        if (targetNamespace != null) append("targetNamespace=$targetNamespace")
        append(")")
    }

}
