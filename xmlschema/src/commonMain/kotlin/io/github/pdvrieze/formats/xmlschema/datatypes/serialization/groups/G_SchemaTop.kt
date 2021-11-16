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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_TopLevelAttribute

/**
 * This group is for the elements which occur freely at the top level of schemas. All of their types
 * are based on the "annotated" type by extension.
 *
 * Choice G_Redefinable, E_Element, E_Attribute, E_Notation
 */
interface G_SchemaTop {

    val schemaTop: Base

    sealed interface Base

    interface Element: Base
    interface Attribute: Base, T_TopLevelAttribute
    interface Notation: Base
}

