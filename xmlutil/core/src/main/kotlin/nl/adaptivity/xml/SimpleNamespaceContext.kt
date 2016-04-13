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

import javax.xml.XMLConstants
import javax.xml.namespace.NamespaceContext

import java.util.*


/**
 * Created by pdvrieze on 24/08/15.
 */
open class SimpleNamespaceContext : NamespaceContext, Iterable<Namespace> {

  private inner class SimpleIterator : Iterator<Namespace> {
    private var pos = 0

    override fun hasNext(): Boolean {
      return pos < mStrings.size
    }

    override fun next(): Namespace {
      val result = SimpleNamespace(pos)
      pos += 2
      return result
    }
  }

  private inner class SimpleNamespace(private val pos: Int) : Namespace {

    override val prefix: String
      get() = mStrings[pos]

    override val namespaceURI: String
      get() = mStrings[pos + 1]
  }

  private val mStrings: Array<String>

  constructor(prefixMap: Map<out CharSequence, out CharSequence>) {
    mStrings = Array<String>(prefixMap.size * 2) {""}
    var i = 0
    prefixMap.entries.forEachIndexed { i, entry ->
      val nsUri = entry.value.toString()
      if (nsUri.isEmpty()) throw IllegalArgumentException("Null namespaces are illegal")

      mStrings[i * 2] = entry.key.toString()
      mStrings[i * 2 + 1] = nsUri
    }
  }

  constructor(prefixes: Array<CharSequence>, namespaces: Array<CharSequence>) {
    assert(prefixes.size == namespaces.size)
    mStrings = Array<String>(prefixes.size * 2) {""}
    for (i in prefixes.indices) {
      mStrings[i * 2] = prefixes[i].toString()
      mStrings[i * 2 + 1] = namespaces[i].toString()
    }
  }

  constructor(prefix: CharSequence, namespace: CharSequence) {
    mStrings = arrayOf(prefix.toString(),namespace.toString())
  }

  internal constructor(strings: Array<String>) {
    mStrings = strings
  }

  constructor(namespaces: Iterable<Namespace>) {
    if (namespaces is Collection<Any>) {
      val len = namespaces.size
      mStrings = Array<String>(len * 2) {""}
      namespaces.forEachIndexed { i, ns ->
        mStrings[i*2] = ns.prefix.toString()
        mStrings[i*2+1] = ns.namespaceURI.toString()
      }
    } else {
      val intermediate = ArrayList<String>()
      for (ns in namespaces) {
        intermediate.add(ns.prefix.toString())
        intermediate.add(ns.namespaceURI.toString())
      }
      mStrings = intermediate.toTypedArray()
    }
  }

  fun size(): Int {
    return mStrings.size / 2
  }

  fun combine(other: SimpleNamespaceContext): SimpleNamespaceContext {
    val result = TreeMap<String, String>()
    for (i in mStrings.size / 2 - 1 downTo 0) {
      result.put(mStrings[i * 2], mStrings[i * 2 + 1])
    }
    for (i in other.mStrings.size / 2 - 1 downTo 0) {
      result.put(other.mStrings[i * 2], other.mStrings[i * 2 + 1])
    }
    return SimpleNamespaceContext(result)
  }

  /**
   * Combine this context with the additional context. The prefixes already in this context prevail over the added ones.
   * @param other The namespaces to add
   * *
   * @return the new context
   */
  fun combine(other: Iterable<Namespace>?): SimpleNamespaceContext? {
    if (mStrings.size == 0) {
      return from(other)
    }
    if (other == null || !other.iterator().hasNext()) {
      return this
    }
    val result = TreeMap<String, String>()
    for (i in mStrings.size / 2 - 1 downTo 0) {
      result.put(mStrings[i * 2], mStrings[i * 2 + 1])
    }
    if (other is SimpleNamespaceContext) {
      for (i in other.mStrings.size / 2 - 1 downTo 0) {
        result.put(other.mStrings[i * 2], other.mStrings[i * 2 + 1])
      }
    } else {
      for (ns in other) {
        result.put(ns.prefix, ns.namespaceURI)
      }
    }
    return SimpleNamespaceContext(result)

  }

  override fun getNamespaceURI(prefix: String?): String {
    if (prefix == null) {
      throw IllegalArgumentException()
    }
    when (prefix) {
      XMLConstants.XML_NS_PREFIX   -> return XMLConstants.XML_NS_URI
      XMLConstants.XMLNS_ATTRIBUTE -> return XMLConstants.XMLNS_ATTRIBUTE_NS_URI
    }
    var i = mStrings.size - 2
    while (i >= 0) { // Should be backwards to allow overrriding
      if (prefix == mStrings[i]) {
        return mStrings[i + 1]
      }
      i -= 2
    }

    return XMLConstants.NULL_NS_URI
  }

  override fun getPrefix(namespaceURI: String): String? {
    return when (namespaceURI) {
      XMLConstants.XML_NS_URI             -> XMLConstants.XML_NS_PREFIX
      XMLConstants.NULL_NS_URI            -> XMLConstants.DEFAULT_NS_PREFIX
      XMLConstants.XMLNS_ATTRIBUTE_NS_URI -> XMLConstants.XMLNS_ATTRIBUTE
      else                                -> {
        ((mStrings.size-2)downTo 0 step 2)
              .filterIndexed { i, s -> mStrings[i+1] == namespaceURI }
              .map { mStrings[it] }
              .firstOrNull()

      }
    }
  }

  override fun getPrefixes(namespaceURI: String): Iterator<String> {
    when (namespaceURI) {
      XMLConstants.XML_NS_URI             -> return setOf(XMLConstants.XML_NS_PREFIX).iterator()
      XMLConstants.XMLNS_ATTRIBUTE_NS_URI -> return setOf(XMLConstants.XMLNS_ATTRIBUTE).iterator()
      else                                -> {
        val result = ArrayList<String>(mStrings.size / 2)
        var i = mStrings.size - 2
        while (i >= 0) {// Should be backwards to allow overrriding
          if (namespaceURI == mStrings[i + 1]) {
            result.add(mStrings[i])
          }
          i -= 2
        }
        if (result.size == 0) {
          return Collections.emptyIterator<String>()
        }
        return Collections.unmodifiableList(result).iterator()
      }
    }
  }

  fun getPrefix(index: Int): String {
    try {
      return mStrings[index * 2]
    } catch (e: ArrayIndexOutOfBoundsException) {
      throw ArrayIndexOutOfBoundsException(index)
    }

  }

  fun getNamespaceURI(index: Int): String {
    try {
      return mStrings[index * 2 + 1]
    } catch (e: ArrayIndexOutOfBoundsException) {
      throw ArrayIndexOutOfBoundsException(index)
    }

  }

  override fun iterator(): Iterator<Namespace> {
    return SimpleIterator()
  }

  override fun equals(o: Any?): Boolean {
    if (this === o) return true
    if (o == null || javaClass != o.javaClass) return false

    val that = o as SimpleNamespaceContext

    // Probably incorrect - comparing Object[] arrays with Arrays.equals
    return Arrays.equals(mStrings, that.mStrings)

  }

  override fun hashCode(): Int {
    return Arrays.hashCode(mStrings)
  }

  companion object {

    fun from(originalNSContext: Iterable<Namespace>?): SimpleNamespaceContext? {
      if (originalNSContext is SimpleNamespaceContext) {
        return originalNSContext
      } else if (originalNSContext == null) {
        return null
      } else {
        return SimpleNamespaceContext(originalNSContext)
      }
    }
  }
}
