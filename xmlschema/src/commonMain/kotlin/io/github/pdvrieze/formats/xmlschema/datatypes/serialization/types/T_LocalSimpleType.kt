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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSimpleDerivation
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_SimpleDerivation

interface T_LocalSimpleType: T_SimpleType, T_LocalType, T_Element.Simple {
    override val name: Nothing?
    override val simpleDerivation: T_SimpleDerivation
}

interface T_TopLevelSimpleType: T_SimpleType, T_TopLevelType {
    val final: Set<T_SimpleDerivationSetElem>
}

interface T_SimpleType: T_SimpleBaseType, G_SimpleDerivation

