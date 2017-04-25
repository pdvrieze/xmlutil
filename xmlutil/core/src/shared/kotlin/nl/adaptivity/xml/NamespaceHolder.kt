/*
 * Copyright (c) 2017. 
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

import javax.xml.XMLConstants.NULL_NS_URI
import javax.xml.XMLConstants.XMLNS_ATTRIBUTE
import javax.xml.XMLConstants.XML_NS_PREFIX
import javax.xml.XMLConstants.XML_NS_URI
import javax.xml.XMLConstants.DEFAULT_NS_PREFIX
import javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI
import javax.xml.namespace.NamespaceContext


/**
 * A utility class that helps with maintaining a namespace context in a parser.
 * Created by pdvrieze on 16/11/15.
 */
class NamespaceHolder {

  private var nameSpaces = arrayOfNulls<String>(10)
  private var namespaceCounts = IntArray(20)
  var depth = 0
    private set

  fun incDepth() {
    ++depth
    if (depth >= namespaceCounts.size) {
      namespaceCounts = namespaceCounts.copyOf(namespaceCounts.size * 2)
    }
    namespaceCounts[depth] = namespaceCounts[depth - 1]
  }

  fun decDepth() {
    nameSpaces.fill(element = null,
                    fromIndex = if (depth == 0) 0 else arrayUseAtDepth(depth-1),
                    toIndex = arrayUseAtDepth(depth)) // Clear out all unused namespaces
    namespaceCounts[depth] = 0
    --depth
  }

  val totalNamespaceCount:Int
    get() = namespaceCounts[depth]

  private fun arrayUseAtDepth(depth:Int) =
    namespaceCounts[depth] * 2

  private fun prefixArrayPos(pairPos:Int) = pairPos*2

  private fun nsArrayPos(pairPos:Int) = pairPos*2+1

  private fun setPrefix(pos:Int, value:CharSequence?) {
    nameSpaces[prefixArrayPos(pos)] = value?.toString() ?: ""
  }

  private fun getPrefix(pos:Int): CharSequence =
    nameSpaces[prefixArrayPos(pos)]!!

  private fun setNamespace(pos:Int, value:CharSequence?) {
    nameSpaces[nsArrayPos(pos)] = value?.toString() ?: ""
  }

  private fun getNamespace(pos:Int): CharSequence =
        nameSpaces[nsArrayPos(pos)]!!


  fun clear() {
    nameSpaces = arrayOfNulls<String>(10)
    namespaceCounts = IntArray(20)
    depth = 0
  }

  fun addPrefixToContext(ns: Namespace) {
    addPrefixToContext(ns.prefix, ns.namespaceURI)
  }


  fun addPrefixToContext(prefix: CharSequence?, namespaceUri: CharSequence?) {
    val nextPair = namespaceCounts[depth]
    if (nsArrayPos(nextPair) >= nameSpaces.size) enlargeNamespaceBuffer()

    setPrefix(nextPair, prefix)
    setNamespace(nextPair, namespaceUri)

    namespaceCounts[depth]++
  }

  private fun enlargeNamespaceBuffer() {
    nameSpaces = nameSpaces.copyOf(nameSpaces.size * 2)
  }

  // From first namespace
  val namespaceContext: NamespaceContext
    get() {
      val pairs = nameSpaces.sliceArray(0..(arrayUseAtDepth(depth) - 1)).requireNoNulls()
      return SimpleNamespaceContext(pairs)
    }

  fun getNamespaceUri(prefix: CharSequence): CharSequence? {
    val prefixStr = prefix.toString()
    return when (prefixStr) {
      XML_NS_PREFIX   -> return XML_NS_URI
      XMLNS_ATTRIBUTE -> return XMLNS_ATTRIBUTE_NS_URI

      else            -> ((totalNamespaceCount-1) downTo 0)
              .firstOrNull { getPrefix(it) == prefixStr }
              ?.let {getNamespace(it)} ?: if (prefixStr.isEmpty()) NULL_NS_URI else null
    }
  }

  fun getPrefix(namespaceUri: CharSequence): CharSequence? {
    val namespaceUriStr = namespaceUri.toString()
    return when (namespaceUriStr) {
      XML_NS_URI             -> XML_NS_PREFIX
      XMLNS_ATTRIBUTE_NS_URI -> XMLNS_ATTRIBUTE
      else                   -> ((totalNamespaceCount - 1) downTo 0)
              .firstOrNull { getNamespace(it) == namespaceUriStr }
              ?.let { getPrefix(it) }
              ?: if (namespaceUriStr == NULL_NS_URI) DEFAULT_NS_PREFIX else null

    }
  }
}
