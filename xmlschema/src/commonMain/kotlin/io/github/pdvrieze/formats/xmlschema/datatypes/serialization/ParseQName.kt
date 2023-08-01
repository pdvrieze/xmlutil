/*
 * Copyright (c) 2023.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
