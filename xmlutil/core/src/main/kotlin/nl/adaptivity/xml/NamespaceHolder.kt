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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xml

import net.devrieze.util.StringUtil
import nl.adaptivity.xml.Namespace
import nl.adaptivity.xml.SimpleNamespaceContext

import javax.xml.XMLConstants
import javax.xml.namespace.NamespaceContext

import java.util.Arrays


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
      enlarge()
    }
    mNamespaceCounts[depth] = mNamespaceCounts[depth - 1]
  }

  fun decDepth() { // XXX consider shrinking the arrays.
    Arrays.fill(mNamespaces,
                if (depth == 0) 0 else mNamespaceCounts[depth - 1] * 2,
                mNamespaceCounts[depth] * 2,
                null) // Clear out all unused namespaces
    mNamespaceCounts[depth] = 0
    --depth
  }

  fun clear() {
    mNamespaces = arrayOfNulls<String>(10)
    mNamespaceCounts = IntArray(20)
    depth = 0
  }

  fun addPrefixToContext(ns: Namespace) {
    addPrefixToContext(ns.prefix, ns.namespaceURI)
  }

  fun addPrefixToContext(prefix: CharSequence?, namespaceUri: CharSequence?) {
    val nextNamespacePos = 2 * mNamespaceCounts[depth]
    if (nextNamespacePos >= mNamespaces.size) {
      enlarge()
    }
    mNamespaces[nextNamespacePos] = if (prefix == null) "" else prefix.toString()
    mNamespaces[nextNamespacePos + 1] = if (namespaceUri == null) "" else namespaceUri.toString()
    mNamespaceCounts[depth]++
  }

  private fun enlarge() {
    mNamespaceCounts = Arrays.copyOf(mNamespaceCounts, mNamespaceCounts.size*2)
  }

  // From first namespace
  val namespaceContext: NamespaceContext
    get() {
      val startPos = 0
      val endPos = mNamespaceCounts[depth] * 2
      val pairs = Arrays.copyOfRange(mNamespaces, startPos, endPos, Array<String>::class.java)
      return SimpleNamespaceContext(pairs)
    }

  fun getNamespaceUri(prefix: CharSequence): CharSequence? {
    when (prefix.toString()) {
      XMLConstants.XML_NS_PREFIX   -> return XMLConstants.XML_NS_URI
      XMLConstants.XMLNS_ATTRIBUTE -> return XMLConstants.XMLNS_ATTRIBUTE_NS_URI

      else                         -> {
        ((mNamespaceCounts[depth]*2 -1) downTo 0 step 2)
              .filter { mNamespaces[it]== prefix }
              .map { mNamespaces[it+1] }
              .firstOrNull()?.let { return it }
      }
    }
    if (prefix.length == 0) {
      return XMLConstants.NULL_NS_URI
    }
    return null
  }

  fun getPrefix(namespaceUri: CharSequence): CharSequence? {
    when (namespaceUri.toString()) {
      XMLConstants.XML_NS_URI             -> return XMLConstants.XML_NS_PREFIX
      XMLConstants.XMLNS_ATTRIBUTE_NS_URI -> return XMLConstants.XMLNS_ATTRIBUTE
      XMLConstants.NULL_NS_URI            -> {
        val count = mNamespaceCounts[depth] * 2
        var i = 0
        while (i < count) {
          if (mNamespaces[i]!!.length == 0 && mNamespaces[i + 1]!!.length > 1) {
            // The default prefix is bound to a non-null namespace
            return null
          }
          i += 2
        }
        return XMLConstants.DEFAULT_NS_PREFIX
      }

      else                                -> {
        val count = mNamespaceCounts[depth] * 2
        var i = 1
        while (i < count) {
          if (StringUtil.isEqual(namespaceUri, mNamespaces[i])) {
            return mNamespaces[i - 1]
          }
          i += 2
        }
      }
    }
    return null
  }
}
