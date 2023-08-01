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

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.model.I_Assertions
import io.github.pdvrieze.formats.xmlschema.types.T_ComplexType
import io.github.pdvrieze.formats.xmlschema.types.T_DerivationControl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlId
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes

@Serializable
sealed class XSSimpleContentDerivation(
    @XmlId
    override val id: VID?,
    override val attributes: List<XSLocalAttribute>,
    override val attributeGroups: List<XSAttributeGroupRef>,
    override val anyAttribute: XSAnyAttribute?,
    override val asserts: List<XSAssert>,
    override val annotation: XSAnnotation? = null,
    @XmlOtherAttributes
    override val otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String>,
) : XSComplexType.Derivation, T_ComplexType.SimpleDerivation, I_Assertions {
    abstract val base: QName?
    abstract val derivationMethod: T_DerivationControl.ComplexBase
}
