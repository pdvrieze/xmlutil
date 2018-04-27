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

interface Namespace {

    /**
     * Gets the prefix, returns "" if this is a default
     * namespace declaration.
     */
    val prefix: String

    /**
     * Gets the uri bound to the prefix of this namespace
     */
    val namespaceURI: String

}

@Deprecated("Use the property version", ReplaceWith("this.prefix"))
inline fun Namespace.getPrefix() = prefix
@Deprecated("Use the property version", ReplaceWith("this.namespaceURI"))
inline fun Namespace.getNamespaceURI() = namespaceURI
