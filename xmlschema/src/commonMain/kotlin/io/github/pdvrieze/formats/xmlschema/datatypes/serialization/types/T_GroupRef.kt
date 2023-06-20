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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.attrGroups.AG_Occurs
import nl.adaptivity.xmlutil.QName

interface T_GroupRef : T_RealGroup, XSI_Annotated, T_Particle {
    val ref: QName // required

    val name: Nothing? get() = null

    override val particle: T_RealGroup.RG_Particle? get() = null
    override val particles: List<T_RealGroup.RG_Particle> get() = emptyList()
}
