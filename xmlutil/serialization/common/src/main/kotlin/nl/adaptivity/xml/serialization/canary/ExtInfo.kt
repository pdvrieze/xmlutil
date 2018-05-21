/*
 * Copyright (c) 2018.
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

package nl.adaptivity.xml.serialization.canary

import kotlinx.serialization.KSerialClassKind
import nl.adaptivity.xml.serialization.OutputKind
import nl.adaptivity.xml.serialization.XmlChildrenName
import nl.adaptivity.xml.serialization.XmlElement
import nl.adaptivity.xml.serialization.XmlValue

interface BaseInfo {
    val kind: KSerialClassKind?
    val type: ChildType
    val isNullable: Boolean
}

class ExtInfo(override val kind: KSerialClassKind?,
              val childInfo: Array<ChildInfo>,
              override val type: ChildType,
              override val isNullable: Boolean): BaseInfo {
}