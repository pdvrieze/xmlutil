/*
 * Copyright (c) 2023-2026.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

@file:UseSerializers(QNameSerializer::class)
package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSchemaLike
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import io.github.pdvrieze.formats.xmlschema.types.VFormChoice
import io.github.pdvrieze.xml.schematypes.values.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.SerializableQName
import nl.adaptivity.xmlutil.XMLConstants.XSD_NS_URI
import nl.adaptivity.xmlutil.XMLConstants.XSD_PREFIX
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("element", XSD_NS_URI, XSD_PREFIX)
class XSLocalElement : XSElement, XSI_AllParticle {

    override val name: XsdNCName?

    @XmlElement(false)
    val form: VFormChoice?

    override val minOccurs: XsdNonNegativeInteger?

    override val maxOccurs: VAllNNI?

    @XmlElement(false)
    val ref: XsdQName?

    val targetNamespace: XsdAnyURI?

    constructor(
        block: Set<VDerivationControl.T_BlockSetValues>? = null,
        default: XsdString? = null,
        fixed: XsdString? = null,
        form: VFormChoice? = null,
        id: XsdID? = null,
        maxOccurs: VAllNNI? = null,
        minOccurs: XsdNonNegativeInteger? = null,
        name: XsdNCName? = null,
        nillable: Boolean? = null,
        ref: XsdQName? = null,
        targetNamespace: XsdAnyURI? = null,
        type: XsdQName? = null,
        annotation: XSAnnotation? = null,
        localType: XSLocalType? = null,
        alternatives: List<XSAlternative> = emptyList(),
        identityConstraints: List<XSIdentityConstraint> = emptyList(),
        otherAttrs: Map<SerializableQName, String> = emptyMap()
    ) : super(block, default, fixed, id, name, nillable, type, annotation, localType, identityConstraints, alternatives, otherAttrs) {
        this.name = name
        this.form = form
        this.minOccurs = minOccurs
        this.maxOccurs = maxOccurs
        this.ref = ref
        this.targetNamespace = targetNamespace
    }

    override fun hasLocalNsInContext(schema: ResolvedSchemaLike): Boolean {
        return form == VFormChoice.UNQUALIFIED || (targetNamespace!=null && schema.elementFormDefault == VFormChoice.UNQUALIFIED)
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
