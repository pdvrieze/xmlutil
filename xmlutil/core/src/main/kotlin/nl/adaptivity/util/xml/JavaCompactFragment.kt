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

package nl.adaptivity.util.xml

import nl.adaptivity.xml.*


/**
 * A class representing an xml fragment compactly.
 * Created by pdvrieze on 06/11/15.2
 */
internal class JavaCompactFragment : CompactFragment
{

  class Factory : XmlDeserializerFactory<CompactFragment>
  {

    @Throws(XmlException::class)
    override fun deserialize(reader: XmlReader): CompactFragment
    {
      return Companion.deserialize(reader)
    }
  }

  override val isEmpty: Boolean
    get() = content.isEmpty()

  override val namespaces: SimpleNamespaceContext
  override val content: CharArray

  constructor(namespaces: Iterable<Namespace>, content: CharArray)
  {
    this.namespaces = SimpleNamespaceContext.from(namespaces)!!
    this.content = content
  }

  /** Convenience constructor for content without namespaces.  */
  constructor(string: String) : this(emptyList<Namespace>(), string.toCharArray())
  {
  }

  constructor(orig: CompactFragment)
  {
    namespaces = SimpleNamespaceContext.from(orig.namespaces)!!
    content = orig.content
  }

  @Throws(XmlException::class)
  constructor(content: XmlSerializable)
  {
    namespaces = SimpleNamespaceContext(emptyList<Namespace>())
    this.content = content.toCharArray()
  }

  @Throws(XmlException::class)
  override fun serialize(out: XmlWriter)
  {
    XMLFragmentStreamReader.from(this).use { reader ->
      out.serialize(reader)
    }
  }

  override fun equals(o: Any?): Boolean
  {
    if (this === o) return true
    if (o == null || javaClass != o.javaClass) return false

    val that = o as CompactFragment?

    if (namespaces != that!!.namespaces) return false
    return content.contentEquals(that.content)

  }

  override fun hashCode(): Int
  {
    var result = namespaces.hashCode()
    result = 31 * result + content.contentHashCode()
    return result
  }

  override fun toString(): String
  {
    return buildString {
      append("namespaces=[")
      (0 until namespaces.size).joinTo(this) { "${namespaces.getPrefix(it)} -> ${namespaces.getNamespaceURI(it)}" }

      append("], content=")
        .append(String(content))
        .append('}')
    }
  }

  override val contentString: String
    get() = String(content)

  companion object
  {


    @Throws(XmlException::class)
    fun deserialize(reader: XmlReader): CompactFragment
    {
      return reader.siblingsToFragment()
    }
  }
}
val FACTORY: XmlDeserializerFactory<CompactFragment> = JavaCompactFragment.Factory()
