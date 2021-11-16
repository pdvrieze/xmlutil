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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSOpenContent

/**
 * XS_SimpleContent | XS_ComplexContent | (XS_OpenContent* G_TypeDefParticle* G_AttrDecls G_Assertions )
 */
interface G_ComplexTypeModel {

    val content: Base

    sealed interface Base

    interface SimpleContent: Base
    interface ComplexContent: Base

    interface Shorthand: Base, GX_TypeDefParticles, G_AttrDecls, G_Assertions {
        val openContents: List<XSOpenContent>
    }
}
