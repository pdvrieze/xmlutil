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

package io.github.pdvrieze.formats.xmlschema.types

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.model.ComplexTypeModel
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.XmlElement

interface T_Element : T_Particle, XSI_Annotated, I_OptNamed, XSI_OpenAttrs {
    val localType: T_Type?

    val alternatives: List<T_AltType>

    val type: QName?

    /** Attribute */
    val substitutionGroup: List<QName>?

    val default: CharSequence?

    @XmlElement(false)
    val fixed: CharSequence?

    val nillable: Boolean?

    /** Optional, default false */
    val abstract: Boolean?

    val final: Set<out ComplexTypeModel.Derivation>?

    val block: T_BlockSet?

    val form: T_FormChoice?

    val keyrefs: List<T_KeyRef>

    override val name: VNCName?

    val ref: QName?

    val uniques: List<T_Unique>

    val keys: List<T_Key>

}
