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

/**
 * Created by pdvrieze on 13/04/16.
 */
@file:JvmName("XmlUtil")
package nl.adaptivity.xml

import nl.adaptivity.util.multiplatform.JvmName
import nl.adaptivity.xml.XMLConstants.DEFAULT_NS_PREFIX
import nl.adaptivity.xml.XMLConstants.NULL_NS_URI

/** Determine whether the character is xml whitespace. */
fun isXmlWhitespace(char:Char) =
      char == '\u000A' || char =='\u0009' || char =='\u000d' || char == ' '

fun isXmlWhitespace(data: CharArray) = data.all { isXmlWhitespace(it) }

fun isXmlWhitespace(data: CharSequence) = data.all { isXmlWhitespace(it) }

fun qname(namespaceUri:CharSequence?, localname:CharSequence, prefix:CharSequence? = DEFAULT_NS_PREFIX) =
    QName(namespaceUri?.toString() ?: NULL_NS_URI,
                            localname.toString(),
                            prefix?.toString() ?: DEFAULT_NS_PREFIX)


fun CharSequence.toQname(): QName {
  val split = indexOf('}')
  val localname: String
  val nsUri: String
  if (split >= 0) {
    if (this[0] != '{') throw IllegalArgumentException("Not a valid qname literal")
    nsUri = substring(1, split)
    localname = substring(split + 1)
  } else {
    nsUri = XMLConstants.NULL_NS_URI
    localname = toString()
  }
  return QName(nsUri, localname)
}

fun QName.toCName(): String {
  if (NULL_NS_URI == getPrefix()) return getLocalPart()
  return "${getPrefix()}:${getLocalPart()}"
}


/**
 * Convert a prefixed element name (CNAME) to a qname. If there is no prefix, the default prefix is used.
 * @param reference The namespace context to use to resolve the name.
 *
 * @param name The name to resolve
 *
 * @return A resolved qname.
 */
fun NamespaceContext.asQName(name: String): QName {
    val reference: NamespaceContext = this
    val colPos = name.indexOf(':')
    if (colPos >= 0) {
        val prefix = name.substring(0, colPos)
        return QName(reference.getNamespaceURI(prefix) ?: NULL_NS_URI, name.substring(colPos + 1), prefix)
    } else {
        return QName(reference.getNamespaceURI(DEFAULT_NS_PREFIX) ?: NULL_NS_URI, name, DEFAULT_NS_PREFIX)
    }

}

fun XmlReader.isXml():Boolean {
    try {
        while (hasNext()) next()
    } catch (e:XmlException) {
        return false
    }
    return true;
}

fun CharSequence.xmlEncode(): String {

  return buildString {
    for (c in this@xmlEncode) {
      when (c) {
        '<'  -> append("&lt;")
        '>'  -> append("&gt;")
        '&'  -> append("&amp;")
        else -> append(c)
      }
    }
  }
}


private class NamespaceInfo(val prefix: String, val url: String)

@Deprecated("Use toString comparison", ReplaceWith("this.toString() == other.toString()"))
fun CharSequence.matches(other:CharSequence) = this.toString()==other.toString()