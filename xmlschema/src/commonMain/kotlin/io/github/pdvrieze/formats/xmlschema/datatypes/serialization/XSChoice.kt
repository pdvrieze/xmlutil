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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.types.T_AllNNI
import io.github.pdvrieze.formats.xmlschema.types.T_Choice
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlBefore
import nl.adaptivity.xmlutil.serialization.XmlId
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("choice", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSChoice(
    override val particles: List<XSI_NestedParticle>,
    override val minOccurs: VNonNegativeInteger? = null,
    override val maxOccurs: T_AllNNI? = null,
    @XmlBefore("*")
    override val annotation: XSAnnotation? = null,
    @XmlId
    override val id: VID? = null,
    @XmlOtherAttributes
    override val otherAttrs: Map<QName, String> = emptyMap()
) : XSExplicitGroup, T_Choice, XSComplexContent.XSIDirectParticle, XSI_NestedParticle {
//    override val mdlAnnotations: AnnotationModel? get() = annotation.models()
//    override val mdlMinOccurs: VNonNegativeInteger get() = minOccurs ?: VNonNegativeInteger(1)
//    override val mdlMaxOccurs: T_AllNNI get() = maxOccurs ?: T_AllNNI(1)
//    override val mdlTerm: Term get() = this
}
