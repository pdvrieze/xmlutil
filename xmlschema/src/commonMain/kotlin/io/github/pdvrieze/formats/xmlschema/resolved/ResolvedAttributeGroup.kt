/*
 * Copyright (c) 2022. 
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

package io.github.pdvrieze.formats.xmlschema.resolved

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnnotation
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnyAttribute
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAttributeGroup
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAttributeGroupRef
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_Redefinable
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.*
import nl.adaptivity.xmlutil.QName

sealed class ResolvedAttributeGroup(
    override val schema: ResolvedSchemaLike
) : ResolvedPart, G_Redefinable.AttributeGroup, XSI_Annotated {
    abstract override val rawPart: XSAttributeGroup
}

class ResolvedAttributeGroupRef(
    override val rawPart: T_AttributeGroupRef,
    override val schema: ResolvedSchemaLike
) : ResolvedPart, T_AttributeGroupRef, XSI_Annotated {
    val resolvedGroup: ResolvedDirectAttributeGroup by lazy { schema.attributeGroup(rawPart.ref) }

    override val attributes: List<T_LocalAttribute>
        get() = resolvedGroup.attributes

    override val attributeGroups: List<T_AttributeGroupRef>
        get() = resolvedGroup.attributeGroups

    override val anyAttribute: XSAnyAttribute?
        get() = rawPart.anyAttribute

    override val annotation: XSAnnotation?
        get() = rawPart.annotation

    override val id: VID?
        get() = rawPart.id

    override val ref: QName
        get() = rawPart.ref

    override val otherAttrs: Map<QName, String>
        get() = rawPart.otherAttrs

    override fun check() {
        super<ResolvedPart>.check()
        checkNotNull(resolvedGroup) // force resolve
    }
}

class ResolvedDirectAttributeGroup(
    override val rawPart: XSAttributeGroup,
    override val schema: ResolvedSchemaLike
) : NamedPart, T_NamedAttributeGroup, XSI_Annotated {

    override val attributes: List<ResolvedLocalAttribute> = DelegateList(rawPart.attributes) {
        ResolvedLocalAttribute(it, schema)
    }

    override val attributeGroups: List<ResolvedAttributeGroupRef>
        get() = DelegateList(rawPart.attributeGroups) { ResolvedAttributeGroupRef(it, schema) }

    override val anyAttribute: XSAnyAttribute?
        get() = rawPart.anyAttribute

    override val annotation: XSAnnotation?
        get() = rawPart.annotation

    override val id: VID?
        get() = rawPart.id

    override val name: VNCName
        get() = rawPart.name

    override val otherAttrs: Map<QName, String>
        get() = rawPart.otherAttrs

    override val targetNamespace: VAnyURI
        get() = super<NamedPart>.targetNamespace

    override fun check() {
        super<NamedPart>.check()
        for (a in attributes) { a.check() }
        for (ag in attributeGroups) { ag.check() }
    }
}
