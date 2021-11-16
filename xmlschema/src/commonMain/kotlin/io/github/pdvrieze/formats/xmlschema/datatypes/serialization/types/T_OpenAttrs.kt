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

import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes

/**
 * This type is extended by almost all schema types to allow attributes from other namespaces to be
 * added to user schemas.
 */
interface T_OpenAttrs {
    @XmlOtherAttributes
    val otherAttrs: Map<QName, String>
}
