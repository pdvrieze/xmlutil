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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.*
import io.github.pdvrieze.formats.xmlschema.types.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.SerializableQName
import nl.adaptivity.xmlutil.serialization.XmlBefore
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlId
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes

@Serializable
sealed class XSElement : XSI_Annotated {
    @Serializable(BlockSetSerializer::class)
    val block: Set<VDerivationControl.T_BlockSetValues>?
    val default: VString?
    val fixed: VString?
    @XmlId
    final override val id: VID?
    abstract val name: VNCName?

    @SerialName("nillable")
    private val _nillable: VBoolean?
    val nillable: Boolean? get() = _nillable?.value

    @XmlElement(false)
    val type: SerializableQName?

    @XmlBefore("*")
    final override val annotation: XSAnnotation?

    @XmlBefore("alternatives")
    val localType: XSLocalType?

    val identityConstraints: List<XSIdentityConstraint>

    @XmlBefore("identityConstraints")
    val alternatives: List<XSAlternative>// get() = emptyList()

    @XmlOtherAttributes
    final override val otherAttrs: Map<SerializableQName, String>

    // alternative
    constructor(
        block: Set<VDerivationControl.T_BlockSetValues>?,
        default: VString?,
        fixed: VString?,
        id: VID?,
        name: VNCName?,
        nillable: Boolean?,
        type: SerializableQName?,
        annotation: XSAnnotation?,
        localType: XSLocalType?,
        identityConstraints: List<XSIdentityConstraint>,
        alternatives: List<XSAlternative>,
        otherAttrs: Map<SerializableQName, String> = emptyMap()
    ) {
        this.block = block
        this.default = default
        this.fixed = fixed
        this.id = id
        this._nillable = nillable?.let(::VBoolean)
        this.type = type
        this.annotation = annotation
        this.localType = localType
        this.identityConstraints = identityConstraints
        this.alternatives = alternatives
        this.otherAttrs = otherAttrs
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as XSElement

        if (block != other.block) return false
        if (default != other.default) return false
        if (fixed != other.fixed) return false
        if (id != other.id) return false
        if (name != other.name) return false
        if (nillable != other.nillable) return false
        if (type != other.type) return false
        if (annotation != other.annotation) return false
        if (localType != other.localType) return false
        if (identityConstraints != other.identityConstraints) return false
        if (otherAttrs != other.otherAttrs) return false

        return true
    }

    override fun hashCode(): Int {
        var result = block?.hashCode() ?: 0
        result = 31 * result + (default?.hashCode() ?: 0)
        result = 31 * result + (fixed?.hashCode() ?: 0)
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (nillable?.hashCode() ?: 0)
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + (annotation?.hashCode() ?: 0)
        result = 31 * result + (localType?.hashCode() ?: 0)
        result = 31 * result + identityConstraints.hashCode()
        result = 31 * result + otherAttrs.hashCode()
        return result
    }
}
