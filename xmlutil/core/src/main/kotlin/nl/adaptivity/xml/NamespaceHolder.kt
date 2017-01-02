/*
 * Copyright (c) 2016.
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

import java.util.*
import javax.xml.XMLConstants.*
import javax.xml.namespace.NamespaceContext


/**
 * A utility class that helps with maintaining a namespace context in a parser.
 * Created by pdvrieze on 16/11/15.
 */
class NamespaceHolder {

  private var mNamespaces = arrayOfNulls<String>(10)
  private var mNamespaceCounts = IntArray(20)
  var depth = 0
    private set

  fun incDepth() {
    ++depth
    if (depth >= mNamespaceCounts.size) {
      mNamespaceCounts = Arrays.copyOf(mNamespaceCounts, mNamespaceCounts.size*2)
    }
    mNamespaceCounts[depth] = mNamespaceCounts[depth - 1]
  }

  fun decDepth() {
    Arrays.fill(mNamespaces,
                /*fromIndex =*/ if (depth == 0) 0 else arrayUseAtDepth(depth-1),
                /*toIndex =*/ arrayUseAtDepth(depth),
                /*`val` =*/ null) // Clear out all unused namespaces
    mNamespaceCounts[depth] = 0
    --depth
  }

  val totalNamespaceCount:Int
    get() = mNamespaceCounts[depth]

  private fun arrayUseAtDepth(depth:Int) =
    mNamespaceCounts[depth]*2

  private fun prefixArrayPos(pairPos:Int) = pairPos*2

  private fun nsArrayPos(pairPos:Int) = pairPos*2+1

  private fun setPrefix(pos:Int, value:CharSequence?) {
    mNamespaces[prefixArrayPos(pos)] = value?.toString() ?:""
  }

  private fun getPrefix(pos:Int): CharSequence =
    mNamespaces[prefixArrayPos(pos)]!!

  private fun setNamespace(pos:Int, value:CharSequence?) {
    mNamespaces[nsArrayPos(pos)] = value?.toString() ?:""
  }

  private fun getNamespace(pos:Int): CharSequence =
        mNamespaces[nsArrayPos(pos)]!!


  fun clear() {
    mNamespaces = arrayOfNulls<String>(10)
    mNamespaceCounts = IntArray(20)
    depth = 0
  }

  fun addPrefixToContext(ns: Namespace) {
    addPrefixToContext(ns.prefix, ns.namespaceURI)
  }


  fun addPrefixToContext(prefix: CharSequence?, namespaceUri: CharSequence?) {
    val nextPair = mNamespaceCounts[depth]
    if (nsArrayPos(nextPair) >= mNamespaces.size) enlargeNamespaceBuffer()

    setPrefix(nextPair, prefix)
    setNamespace(nextPair, namespaceUri)

    mNamespaceCounts[depth]++
  }

  private fun enlargeNamespaceBuffer() {
    mNamespaces = Arrays.copyOf(mNamespaces, mNamespaces.size*2)
  }

  // From first namespace
  val namespaceContext: NamespaceContext
    get() {
      val pairs = mNamespaces.sliceArray(0..(arrayUseAtDepth(depth)-1)).requireNoNulls()
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
