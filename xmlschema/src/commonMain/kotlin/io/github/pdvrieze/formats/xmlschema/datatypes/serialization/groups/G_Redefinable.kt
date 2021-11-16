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

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups

/**
 * This group is for the elements which can self-redefine (see &lt;redefine> below).
 *
 * Choice E_SimpleType, E_ComplexType, E_Group and E_AttributeGroup
 */
interface G_Redefinable: G_SchemaTop {

    val redefinable: Base

    sealed interface Base: G_SchemaTop.Base

    interface SimpleType: Base
    interface ComplexType: Base
    interface Group: Base
    interface AttributeGroup: Base
}
