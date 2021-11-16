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

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_ComplexTypeModel

sealed interface T_ComplexType_Base: T_Annotated, G_ComplexTypeModel {
    /**
     * May not have simpleContent child
     */
    val mixed: Boolean

    /** Default: false */
    val defaultAttributesApply: Boolean // default true

    /** Either this or shorthand content */
    override val content: G_ComplexTypeModel.Base
}

interface T_ComplexType_Simple: T_ComplexType_Base {
    override val content: G_ComplexTypeModel.SimpleContent
}

interface T_ComplexType_Complex: T_ComplexType_Base {
    override val content: G_ComplexTypeModel.ComplexContent
}

interface T_ComplexType_Shorthand: T_ComplexType_Base, G_ComplexTypeModel.Shorthand {
    override val content: G_ComplexTypeModel.Shorthand get() = this
}
