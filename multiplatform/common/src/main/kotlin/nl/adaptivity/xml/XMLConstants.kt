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

object XMLConstants {
    val DEFAULT_NS_PREFIX: String = ""
    val NULL_NS_URI: String get() = ""
    val XMLNS_ATTRIBUTE_NS_URI: String get() = "http://www.w3.org/2000/xmlns/"
    val XMLNS_ATTRIBUTE: String get() = "xmlns"
    val XML_NS_PREFIX: String get() = "xml"
    val XML_NS_URI: String get() = "http://www.w3.org/XML/1998/namespace"
}
