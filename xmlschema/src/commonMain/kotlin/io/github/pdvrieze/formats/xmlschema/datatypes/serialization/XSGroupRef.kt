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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.model.AnnotationModel
import io.github.pdvrieze.formats.xmlschema.model.GroupDefModel
import io.github.pdvrieze.formats.xmlschema.model.ParticleModel
import io.github.pdvrieze.formats.xmlschema.model.Term
import io.github.pdvrieze.formats.xmlschema.resolved.models
import io.github.pdvrieze.formats.xmlschema.types.T_AllNNI
import io.github.pdvrieze.formats.xmlschema.types.T_GroupRef
import io.github.pdvrieze.formats.xmlschema.types.T_GroupRefParticle
import io.github.pdvrieze.formats.xmlschema.types.XSI_Annotated
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlBefore
import nl.adaptivity.xmlutil.serialization.XmlId
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName

/**
 * Used directly in derivations.
 */
@Serializable
@XmlSerialName("group", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSGroupRef(
    @XmlId
    override val id: VID?,
    override val ref: @Serializable(QNameSerializer::class) QName,
    override val minOccurs: VNonNegativeInteger? = null,
    override val maxOccurs: T_AllNNI? = null,
    @XmlBefore("*")
    override val annotation: XSAnnotation? = null,
    @XmlOtherAttributes
    override val otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String>
) : XSComplexContent.XSIDerivationParticle, XSI_Particle, T_GroupRef, XSI_Annotated, Term {

}

@Serializable
@XmlSerialName("group", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSGroupRefParticle(
    @XmlId
    override val id: VID?,
    override val minOccurs: VNonNegativeInteger? = null,
    override val maxOccurs: T_AllNNI? = null,
    override val ref: @Serializable(QNameSerializer::class) QName,
    @XmlBefore("*")
    override val annotation: XSAnnotation? = null,
    @XmlOtherAttributes
    override val otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String>
) : T_GroupRefParticle, XSI_AllParticle {
//    override val mdlAnnotations: AnnotationModel? get() = annotation.models()
//    override val mdlMinOccurs: VNonNegativeInteger get() = minOccurs ?: VNonNegativeInteger(1)
//    override val mdlMaxOccurs: T_AllNNI get() = maxOccurs ?: T_AllNNI(1)
//    override val mdlTerm: GroupDefModel get() = this
}
