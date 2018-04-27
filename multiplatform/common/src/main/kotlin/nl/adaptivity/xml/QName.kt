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

package nl.adaptivity.xml

expect class QName {
    constructor(namespaceURI: String, localPart: String, prefix: String)
    constructor(namespaceURI: String, localPart: String)
    constructor(localPart: String)

    fun getPrefix(): String
    fun getLocalPart(): String
    fun getNamespaceURI(): String
}

inline val QName.prefix get() = getPrefix()
inline val QName.localPart get() = getLocalPart()
inline val QName.namespaceURI get() = getNamespaceURI()