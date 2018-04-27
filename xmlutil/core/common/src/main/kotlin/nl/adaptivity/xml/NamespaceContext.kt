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

expect interface NamespaceContext {
    fun getNamespaceURI(prefix: String): String?
    fun getPrefix(namespaceURI: String): String?
    @Deprecated("Don't use as unsafe")
    fun getPrefixes(namespaceURI: String): Iterator<Any?>
}

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE", "DEPRECATION")
inline fun NamespaceContext.prefixesFor(namespaceURI: String): Iterator<String> = getPrefixes(namespaceURI) as Iterator<String>