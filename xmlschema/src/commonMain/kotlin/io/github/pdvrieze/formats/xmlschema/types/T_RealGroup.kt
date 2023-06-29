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

interface T_RealGroup : XSI_Annotated {

    interface Particle: T_Particle
    interface All: T_Group.All, XSI_OpenAttrs {
        val minOccurs: Nothing?
        val maxOccurs: Nothing?
    }
    interface Choice: T_Group.Choice, T_SimpleExplicitGroup
    interface Sequence: T_Group.Sequence, T_SimpleExplicitGroup
}


