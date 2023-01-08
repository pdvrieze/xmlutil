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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAll
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSChoice
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSequence
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_All
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_Choice
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_GroupRef
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_Sequence

interface GX_TypeDefParticles {
    val groups: List<T_GroupRef>
    val alls: List<T_All>
    val choices: List<T_Choice>
    val sequences: List<T_Sequence>
}
