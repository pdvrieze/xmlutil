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

package nl.adaptivity.util.xml

import nl.adaptivity.xml.*


/**
 * A class representing an xml fragment compactly.
 * Created by pdvrieze on 06/11/15.2
 */
actual class CompactFragment: ICompactFragment {

  actual class Factory : XmlDeserializerFactory<CompactFragment> {

    @Throws(XmlException::class)
    override fun deserialize(reader: XmlReader): CompactFragment
    {
        @Suppress("RedundantCompanionReference")
        return Companion.deserialize(reader)
    }
  }

  override val isEmpty: Boolean
      get() = content.isEmpty()

  override val namespaces: IterableNamespaceContext
  override val content: CharArray

  actual constructor(namespaces: Iterable<Namespace>, content: CharArray?) {
    this.namespaces = SimpleNamespaceContext.from(namespaces)
    this.content = content ?: CharArray(0)
  }

  /** Convenience constructor for content without namespaces.  */
  actual constructor(content: String) : this(emptyList<Namespace>(), content.toCharArray())

  actual constructor(orig: ICompactFragment) {
    namespaces = SimpleNamespaceContext.from(orig.namespaces)
    content = orig.content
  }

  @Throws(XmlException::class)
  actual constructor(content: XmlSerializable) {
    namespaces = SimpleNamespaceContext(emptyList())
    this.content = content.toCharArray()
  }

  constructor(namespaces: Iterable<Namespace>, content: String?):
      this(namespaces, content?.toCharArray() ?: kotlin.CharArray(0))


    @Throws(XmlException::class)
  override fun serialize(out: XmlWriter)
  {
    XMLFragmentStreamReader.from(this).use { reader ->
      out.serialize(reader)
    }
  }

  override fun getXmlReader(): XmlReader = XMLFragmentStreamReader.from(this)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false

    val that = other as ICompactFragment?

    if (namespaces != that!!.namespaces) return false
    return content.contentEquals(that.content)

  }

  override fun hashCode(): Int
  {
    var result = namespaces.hashCode()
    result = 31 * result + content.contentHashCode()
    return result
  }

  override fun toString(): String {
      return namespaces.joinToString(prefix="{namespaces=[", postfix = "], content=$contentString}") { "${it.prefix} -> ${it.namespaceURI} }" }
  }

  override val contentString: String
    get() = content.contentToString()

  actual companion object {

    @JvmStatic
    val FACTORY: XmlDeserializerFactory<CompactFragment> = CompactFragment.Factory()

    @Throws(XmlException::class)
    actual fun deserialize(reader: XmlReader): CompactFragment
    {
      return reader.siblingsToFragment()
    }
  }
}

val COMPACTFRAGMENTFACTORY: XmlDeserializerFactory<CompactFragment> = CompactFragment.Factory()
