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

@file:UseSerializers(QNameSerializer::class)
package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.*
import io.github.pdvrieze.formats.xmlschema.types.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.*

@Serializable
@XmlSerialName("element", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSLocalElement(
    @XmlBefore("type")
    override val name: VNCName? = null, // can be determined from ref
    @Serializable(AllDerivationSerializer::class)
    override val block: T_BlockSet? = null,
    override val default: VString? = null,
    override val fixed: VString? = null,
    @XmlElement(false)
    override val form: T_FormChoice? = null,
    @XmlId
    override val id: VID? = null,
    override val maxOccurs: T_AllNNI? = null,
    override val minOccurs: VNonNegativeInteger? = null,
    override val nillable: Boolean? = null,
    @XmlElement(false)
    override val ref: QName? = null,
    override val targetNamespace: VAnyURI? = null,
    @XmlElement(false)
    override val type: QName? = null,

    override val annotation: XSAnnotation? = null,
    override val localType: XSLocalType? = null,
    override val alternatives: List<T_AltType> = emptyList(),
    override val uniques: List<XSUnique> = emptyList(),
    override val keys: List<XSKey> = emptyList(),
    override val keyrefs: List<XSKeyRef> = emptyList(),
    @XmlOtherAttributes
    override val otherAttrs: Map<QName, String> = emptyMap(),
) : XSIElement, XSI_AllParticle {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as XSLocalElement

        if (name != other.name) return false
        if (block != other.block) return false
        if (default != other.default) return false
        if (fixed != other.fixed) return false
        if (form != other.form) return false
        if (id != other.id) return false
        if (maxOccurs != other.maxOccurs) return false
        if (minOccurs != other.minOccurs) return false
        if (nillable != other.nillable) return false
        if (ref != other.ref) return false
        if (targetNamespace != other.targetNamespace) return false
        if (type != other.type) return false
        if (annotation != other.annotation) return false
        if (localType != other.localType) return false
        if (alternatives != other.alternatives) return false
        if (uniques != other.uniques) return false
        if (keys != other.keys) return false
        if (keyrefs != other.keyrefs) return false
        if (otherAttrs != other.otherAttrs) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + (block?.hashCode() ?: 0)
        result = 31 * result + (default?.hashCode() ?: 0)
        result = 31 * result + (fixed?.hashCode() ?: 0)
        result = 31 * result + (form?.hashCode() ?: 0)
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + maxOccurs.hashCode()
        result = 31 * result + minOccurs.hashCode()
        result = 31 * result + (nillable?.hashCode() ?: 0)
        result = 31 * result + (ref?.hashCode() ?: 0)
        result = 31 * result + (targetNamespace?.hashCode() ?: 0)
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + (annotation?.hashCode() ?: 0)
        result = 31 * result + (localType?.hashCode() ?: 0)
        result = 31 * result + alternatives.hashCode()
        result = 31 * result + uniques.hashCode()
        result = 31 * result + keys.hashCode()
        result = 31 * result + keyrefs.hashCode()
        result = 31 * result + otherAttrs.hashCode()
        return result
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
        if (alternatives.isNotEmpty()) append(", alternatives=$alternatives")
        if (uniques.isNotEmpty()) append(", uniques=$uniques")
        if (keys.isNotEmpty()) append(", keys=$keys")
        if (keyrefs.isNotEmpty()) append(", keyrefs=$keyrefs")
        if (otherAttrs.isNotEmpty()) append(", otherAttrs=$otherAttrs")
        append(")")
    }


}
