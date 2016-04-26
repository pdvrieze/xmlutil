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

import java.util.*
import javax.xml.XMLConstants.*
import javax.xml.namespace.NamespaceContext


/**
 * A simple namespace context that stores namespaces in a single array
 * Created by pdvrieze on 24/08/15.
 */
open class SimpleNamespaceContext internal constructor(val buffer:Array<out String>) : NamespaceContext, Iterable<Namespace> {

  private inner class SimpleIterator : Iterator<Namespace> {
    private var pos = 0

    override fun hasNext() = pos < size

    override fun next(): Namespace = SimpleNamespace(pos++)
  }

  private inner class SimpleNamespace(private val pos: Int) : Namespace {

    override val prefix: String
      get() = getPrefix(pos)

    override val namespaceURI: String
      get() = getNamespaceURI(pos)
  }

  constructor(prefixMap: Map<out CharSequence, CharSequence>):
    this(flatten(prefixMap.entries, { key.toString() }, { value.toString() }))

  constructor(prefixes: Array<out CharSequence>, namespaces: Array<out CharSequence>):
  this(Array(prefixes.size*2, { (if (it %2==0) prefixes[it/2] else namespaces[it/2]).toString() }))

  constructor(prefix: CharSequence, namespace: CharSequence):
    this(arrayOf(prefix.toString(),namespace.toString()))

  constructor(namespaces: Collection<Namespace>):
    this(flatten(namespaces, { prefix }, { namespaceURI } ))

  constructor(namespaces: Iterable<Namespace>):
    this(namespaces as? Collection<Namespace> ?: namespaces.toList() )

  val indices:IntRange get() = 0..(size-1)

  val size:Int get() = buffer.size / 2

  @Deprecated("There is a nice property now, use that", replaceWith = ReplaceWith("size"), level = DeprecationLevel.ERROR)
  fun size(): Int = size

  /**
   * Create a context that combines both. This will "forget" overlapped prefixes.
   */
  fun combine(other: SimpleNamespaceContext): SimpleNamespaceContext {
    val result = TreeMap<String, String>()
    for (i in indices.reversed()) {
      result.put(getPrefix(i), getNamespaceURI(i))
    }
    for (i in other.indices.reversed()) {
      result.put(other.getPrefix(i), other.getNamespaceURI(i))
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
    if (size == 0) {
      return from(other)
    }
    if (other is SimpleNamespaceContext) { return combine(other) }
    else if (other == null || !other.iterator().hasNext()) {
      return this
    }
    val result = TreeMap<String, String>()
    for (i in indices.reversed()) {
      result.put(getPrefix(i), getNamespaceURI(i))
    }
    for (ns in other) {
      result.put(ns.prefix, ns.namespaceURI)
    }
    return SimpleNamespaceContext(result)

  }

  override fun getNamespaceURI(prefix: String): String {
    return when (prefix) {
      XML_NS_PREFIX   -> XML_NS_URI
      XMLNS_ATTRIBUTE -> XMLNS_ATTRIBUTE_NS_URI
      else -> indices.reversed()
            .filter {getPrefix(it)==prefix}
            .map { getNamespaceURI(it) }
            .firstOrNull()?: NULL_NS_URI
    }
  }

  override fun getPrefix(namespaceURI: String) = getPrefixSequence(namespaceURI).firstOrNull()

  fun getPrefixSequence(namespaceURI: String):Sequence<String> {
    return when (namespaceURI) {
      XML_NS_URI             -> sequenceOf(XML_NS_PREFIX)
      NULL_NS_URI            -> sequenceOf(DEFAULT_NS_PREFIX)
      XMLNS_ATTRIBUTE_NS_URI -> sequenceOf(XMLNS_ATTRIBUTE)
      else                   -> {
        indices.reversed().asSequence()
              .filter { getNamespaceURI(it)== namespaceURI }
              .map { getPrefix(it) }
      }
    }
  }

  override fun getPrefixes(namespaceURI: String) = getPrefixSequence(namespaceURI).iterator()

  fun getPrefix(index: Int): String {
    try {
      return buffer[index * 2]
    } catch (e: ArrayIndexOutOfBoundsException) {
      throw ArrayIndexOutOfBoundsException(index)
    }
  }

  fun getNamespaceURI(index: Int): String {
    try {
      return buffer[index * 2 + 1]
    } catch (e: ArrayIndexOutOfBoundsException) {
      throw ArrayIndexOutOfBoundsException(index)
    }
  }

  override fun iterator(): Iterator<Namespace> {
    return SimpleIterator()
  }

  override fun equals(other: Any?): Boolean{
    if (this === other) return true
    if (other?.javaClass != javaClass) return false

    other as SimpleNamespaceContext

    if (!Arrays.equals(buffer, other.buffer)) return false

    return true
  }

  override fun hashCode(): Int{
    return Arrays.hashCode(buffer)
  }

  companion object {

    @JvmStatic
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


private inline fun <T> flatten(namespaces: Collection<T>, crossinline prefix:T.()->String, crossinline namespace:T.()->String):Array<String> {
  val filler:Iterator<String> = namespaces.asSequence().flatMap { sequenceOf(it.prefix(), it.namespace()) }.iterator()
  return Array(namespaces.size*2, { filler.next() })
}
