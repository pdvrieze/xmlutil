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

interface T_GlobalComplexType_Base: T_ComplexType, T_GlobalType {
    override val name: VNCName

    /**
     * Default: false
     */
    val abstract: Boolean?
    val final: Set<out ComplexTypeModel.Derivation>?
    val block: Set<out ComplexTypeModel.Derivation>?

}

interface T_TopLevelComplexType_Simple: T_GlobalComplexType_Base,
    T_ComplexType.Simple

interface T_TopLevelComplexType_Complex: T_GlobalComplexType_Base,
    T_ComplexType.Complex

interface T_TopLevelComplexType_Shorthand: T_GlobalComplexType_Base,
    T_ComplexType.Shorthand
