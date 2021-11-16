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

/**
 * The three kinds of identity constraints, all with type of or derived from 'keybase'.
 *
 * Choice (XS_Unique | XS_Key | XS_Keyref)
 */
interface G_IdentityConstraint {

    val identityConstraint: Types

    sealed interface Types
    interface Unique: Types
    interface Key: Types
    interface Keyref: Types
}

