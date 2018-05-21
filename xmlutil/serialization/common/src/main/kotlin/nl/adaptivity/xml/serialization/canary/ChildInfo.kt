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

data class ChildInfo(val name: String,
                     val annotations: List<Annotation> = emptyList(),
                     override var kind: KSerialClassKind? = null,
                     var childCount: Int = 0,
                     override var type: ChildType = ChildType.UNKNOWN,
                     override var isNullable: Boolean = false) : BaseInfo

enum class ChildType {
    DOUBLE,
    INT,
    FLOAT,
    STRING,
    UNKNOWN,
    BOOLEAN,
    BYTE,
    UNIT,
    CHAR,
    ENUM,
    LONG,
    NONSERIALIZABLE,
    SHORT,
    ELEMENT;

    val isPrimitive
        get() = when(this) {
            ChildType.UNKNOWN,
            ChildType.UNIT,
            ChildType.ELEMENT,
            ChildType.NONSERIALIZABLE -> false
            else            -> true
        }
}