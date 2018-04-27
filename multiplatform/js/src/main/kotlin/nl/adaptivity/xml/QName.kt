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

actual class QName actual constructor(private val namespaceURI: String, private val localPart: String, private val prefix: String) {

    actual constructor(namespaceURI: String, localPart: String): this(namespaceURI, localPart, XMLConstants.DEFAULT_NS_PREFIX)

    actual constructor(localPart: String): this(XMLConstants.NULL_NS_URI, localPart, XMLConstants.DEFAULT_NS_PREFIX)

    actual fun getPrefix(): String = prefix

    actual fun getLocalPart(): String = localPart

    actual fun getNamespaceURI(): String = namespaceURI

}