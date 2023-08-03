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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VString
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedIdentityConstraint
import io.github.pdvrieze.formats.xmlschema.types.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.*

@Serializable
@XmlSerialName("element", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSGlobalElement : XSElement {

    override val name: VNCName

    val abstract: Boolean?

    @XmlElement(false) @Serializable(ComplexDerivationSerializer::class)
    val final: Set<VDerivationControl.Complex>?

    @XmlElement(false)
    val substitutionGroup: List<QName>?

    constructor(
        block: VBlockSet? = null,
        default: VString? = null,
        fixed: VString? = null,
        id: VID? = null,
        name: VNCName,
        nillable: Boolean? = null,
        type: QName? = null,
        abstract: Boolean? = null,
        substitutionGroup: List<QName>? = null,
        final: Set<@Contextual VDerivationControl.Complex>? = null,
        annotation: XSAnnotation? = null,
        localType: XSLocalType? = null,
        identityConstraints: List<XSIdentityConstraint> = emptyList()
    ) : super(block, default, fixed, id, name, nillable, type, annotation, localType, identityConstraints) {
        this.name = name
        this.abstract = abstract
        this.substitutionGroup = substitutionGroup
        this.final = final
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as XSGlobalElement

        if (abstract != other.abstract) return false
        if (final != other.final) return false
        if (substitutionGroup != other.substitutionGroup) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (abstract?.hashCode() ?: 0)
        result = 31 * result + (final?.hashCode() ?: 0)
        result = 31 * result + (substitutionGroup?.hashCode() ?: 0)
        return result
    }

}
