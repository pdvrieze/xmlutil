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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.types.T_AllNNI
import io.github.pdvrieze.formats.xmlschema.types.T_Sequence
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlBefore
import nl.adaptivity.xmlutil.serialization.XmlId
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("sequence", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSSequence(
    @XmlId
    override val id: VID? = null,
    override val minOccurs: VNonNegativeInteger? = null,
    override val maxOccurs: T_AllNNI? = null,
    override val particles: List<XSI_NestedParticle> = emptyList(),
    @XmlBefore("*")
    override val annotation: XSAnnotation? = null,

    @XmlOtherAttributes
    override val otherAttrs: Map<QName, String> = emptyMap()
): XSExplicitGroup, T_Sequence, XSComplexContent.XSIDirectParticle, XSI_NestedParticle {
//    override val mdlAnnotations: AnnotationModel? get() = annotation.models()
//    override val mdlMinOccurs: VNonNegativeInteger get() = minOccurs ?: VNonNegativeInteger(1)
//    override val mdlMaxOccurs: T_AllNNI get() = maxOccurs ?: T_AllNNI(1)
//    override val mdlTerm: Term get() = this
}

