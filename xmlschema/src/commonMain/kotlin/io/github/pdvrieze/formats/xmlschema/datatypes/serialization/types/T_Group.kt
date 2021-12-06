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

import io.github.pdvrieze.formats.xmlschema.datatypes.NCName
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalAll
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_Particle

/** Base for XS_Group */
interface T_Group: T_GroupBase {
    val particles: List<Particle>

    sealed interface Particle
    interface All: Particle, G_Particle.All
    interface Group: Particle, G_Particle.Group
    interface Choice: Particle, G_Particle.Choice
    interface Sequence: Particle, G_Particle.Sequence
    interface Any: Particle, G_Particle.Any
    interface Element: Particle, G_Particle.Element

}
