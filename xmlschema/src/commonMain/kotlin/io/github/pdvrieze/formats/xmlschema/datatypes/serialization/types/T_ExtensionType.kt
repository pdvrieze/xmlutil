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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSOpenContent
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.GX_TypeDefParticles
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_Assertions
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_AttrDecls
import nl.adaptivity.xmlutil.QName

interface T_ExtensionType: T_Annotated, GX_TypeDefParticles, G_AttrDecls, G_Assertions {
    val openContents: List<XSOpenContent>
    val base: QName
}
