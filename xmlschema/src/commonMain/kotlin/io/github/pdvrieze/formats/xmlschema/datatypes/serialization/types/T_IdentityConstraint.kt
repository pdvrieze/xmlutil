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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSField
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSelector
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_IdentityConstraint
import nl.adaptivity.xmlutil.QName

interface T_IdentityConstraint: XSI_Annotated, I_OptNamedAttrs, G_IdentityConstraint.Base {
    val selector: XSSelector?

    /**
     * At least 1 if selector is present
     */
    val fields: List<XSField>

    override val name: VNCName?
    val ref: QName?
}

interface T_Key: T_IdentityConstraint, G_IdentityConstraint.Key
interface T_Unique: T_IdentityConstraint, G_IdentityConstraint.Unique
