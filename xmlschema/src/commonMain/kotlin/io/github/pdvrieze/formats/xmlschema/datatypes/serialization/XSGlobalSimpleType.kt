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

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.types.*
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.*

sealed interface XSISimpleType : T_SimpleType {
    abstract override val simpleDerivation: XSSimpleDerivation
}

@Serializable
@XmlSerialName("simpleType", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSGlobalSimpleType(
    @XmlElement(false)
    override val name: VNCName,
    @XmlAfter("annotation")
    override val simpleDerivation: XSSimpleDerivation,
    @XmlElement(false)
    @Serializable(SchemaEnumSetSerializer::class)
    override val final: T_FullDerivationSet = emptySet(),
    override val id: VID? = null,
    @XmlBefore("*")
    override val annotation: XSAnnotation? = null,

    @XmlOtherAttributes
    override val otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String>,
) : XSISimpleType, T_GlobalSimpleType {
    override val targetNamespace: Nothing? get() = null
}
