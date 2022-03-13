/*
 * Copyright (c) 2021.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

@file:UseSerializers(QNameSerializer::class)

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.AnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.ID
import io.github.pdvrieze.formats.xmlschema.datatypes.NCName
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_SchemaTop
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_LocalAttribute
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_AttributeBase
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_FormChoice
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("attribute", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
abstract class XSAttributeBase(
    final override val default: String? = null,
    final override val fixed: String? = null,
    final override val id: ID? = null,
    final override val name: NCName,
    final override val type: QName? = null,
    final override val inheritable: Boolean? = null,
    final override val annotations: List<XSAnnotation> = emptyList(),
    final override val simpleType: XSLocalSimpleType? = null,
    final override val otherAttrs: Map<QName, String> = emptyMap(),
) : T_AttributeBase

@Serializable
@XmlSerialName("attribute", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSAttribute : XSAttributeBase, G_SchemaTop.Attribute {
    constructor(
        default: String? = null,
        fixed: String? = null,
        id: ID? = null,
        name: NCName,
        type: QName? = null,
        inheritable: Boolean,
        annotations: List<XSAnnotation> = emptyList(),
        simpleType: XSLocalSimpleType? = null,
        otherAttrs: Map<QName, String> = emptyMap()
    ) : super(default, fixed, id, name, type, inheritable, annotations, simpleType, otherAttrs)
}

@Serializable
@XmlSerialName("attribute", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSLocalAttribute : XSAttributeBase, T_LocalAttribute {

    @XmlElement(false)
    override val form: T_FormChoice?
    override var ref: QName? = null
        private set
    @XmlElement(false)
    override val use: XSAttrUse?
    override val targetNamespace: AnyURI?


    constructor(
        default: String? = null,
        fixed: String? = null,
        form: T_FormChoice? = null,
        id: ID? = null,
        name: NCName,
        ref: QName? = null,
        type: QName? = null,
        use: XSAttrUse? = null,
        inheritable: Boolean = false,
        targetNamespace: AnyURI? = null,
        annotations: List<XSAnnotation> = emptyList(),
        simpleType: XSLocalSimpleType? = null,
        otherAttrs: Map<QName, String> = emptyMap()
    ) : super(default, fixed, id, name, type, inheritable, annotations, simpleType, otherAttrs) {
        this.form = form
        this.ref = ref
        this.use = use
        this.targetNamespace = targetNamespace
    }
}

