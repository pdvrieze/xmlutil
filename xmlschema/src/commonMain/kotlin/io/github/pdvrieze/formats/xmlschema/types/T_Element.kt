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

package io.github.pdvrieze.formats.xmlschema.types

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.model.ComplexTypeModel
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.XmlElement

interface T_Element : T_Particle, XSI_Annotated, I_OptNamedAttrs {
    val localType: T_Type?

    val alternatives: List<T_AltType>

    val type: QName?

    /** Attribute */
    val substitutionGroup: List<QName>?

    val default: String?

    @XmlElement(false)
    val fixed: String?

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
