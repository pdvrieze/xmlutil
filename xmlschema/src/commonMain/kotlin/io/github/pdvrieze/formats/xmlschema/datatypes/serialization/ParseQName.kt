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

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import kotlinx.serialization.SerializationException
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.XML

internal fun parseQName(d: XML.XmlInput?, str: String): QName {
    val cIdx = str.lastIndexOf(':')
    if (d==null) {
        if (cIdx<0) return QName(str)
        val localName = if(cIdx<0) str else str.substring(cIdx+1)
        if (str[0]!='{') throw SerializationException("Missing { before namespace")
        val clIdx = str.indexOf('}', 1)
        if (clIdx<1) throw SerializationException("Missing } after namespace")
        val namespace = str.substring(1, clIdx)
        val prefix = str.substring(clIdx+1, cIdx)
        return QName(namespace, localName, prefix)
    } else {
        val localName: String
        val prefix: String
        if(cIdx<0) {
            localName = str
            prefix = ""
        } else {
            localName = str.substring(cIdx+1)
            prefix = str.substring(0, cIdx)
        }
        val namespace = d.getNamespaceURI(prefix) ?: ""
        return QName(namespace, localName, prefix)
    }
}
