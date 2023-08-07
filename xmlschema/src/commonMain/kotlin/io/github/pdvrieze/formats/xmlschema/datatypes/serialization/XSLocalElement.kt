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

import io.github.pdvrieze.formats.xmlschema.impl.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.*
import io.github.pdvrieze.formats.xmlschema.types.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.*

@Serializable
@XmlSerialName("element", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSLocalElement : XSElement, XSI_AllParticle {

    override val name: VNCName?

    @XmlElement(false)
    val form: VFormChoice?

    override val minOccurs: VNonNegativeInteger?

    override val maxOccurs: VAllNNI?

    @XmlElement(false)
    val ref: QName?

    val targetNamespace: VAnyURI?

    constructor(
        block: VBlockSet? = null,
        default: VString? = null,
        fixed: VString? = null,
        form: VFormChoice? = null,
        id: VID? = null,
        maxOccurs: VAllNNI? = null,
        minOccurs: VNonNegativeInteger? = null,
        name: VNCName? = null,
        nillable: Boolean? = null,
        ref: QName? = null,
        targetNamespace: VAnyURI? = null,
        type: QName? = null,
        annotation: XSAnnotation? = null,
        localType: XSLocalType? = null,
        identityConstraints: List<XSIdentityConstraint> = emptyList(),
        otherAttrs: Map<QName, String> = emptyMap()
    ) : super(block, default, fixed, id, name, nillable, type, annotation, localType, identityConstraints, otherAttrs) {
        this.name = name
        this.form = form
        this.minOccurs = minOccurs
        this.maxOccurs = maxOccurs
        this.ref = ref
        this.targetNamespace = targetNamespace
    }

    override fun toString(): String = buildString {
        append("XSLocalElement(")
        append("name=$name")
        if (block != null) append(", block=$block, ")
        if (default != null) append(", default=$default")
        if (fixed != null) append(", fixed=$fixed")
        if (form != null) append(", form=$form")
        if (id != null) append(", id=$id")
        if (maxOccurs != null) append(", maxOccurs=$maxOccurs")
        if (minOccurs != null) append(", minOccurs=$minOccurs")
        if (nillable != null) append(", nillable=$nillable")
        if (ref != null) append(", ref=$ref")
        if (targetNamespace != null) append(", targetNamespace=$targetNamespace")
        if (type != null) append(", type=$type")
        if (annotation != null) append(", annotation=$annotation")
        if (localType != null) append(", localType=$localType")
        if (identityConstraints.isNotEmpty()) append(", identityConstraints=$identityConstraints")
        if (otherAttrs.isNotEmpty()) append(", otherAttrs=$otherAttrs")
        append(")")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as XSLocalElement

        if (form != other.form) return false
        if (ref != other.ref) return false
        if (targetNamespace != other.targetNamespace) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (form?.hashCode() ?: 0)
        result = 31 * result + (ref?.hashCode() ?: 0)
        result = 31 * result + (targetNamespace?.hashCode() ?: 0)
        return result
    }


}
