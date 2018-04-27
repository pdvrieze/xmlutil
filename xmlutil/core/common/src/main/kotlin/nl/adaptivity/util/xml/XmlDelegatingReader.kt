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
 * Simple baseclass for a delagting XmlReader.
 * Created by pdvrieze on 16/11/15.
 */
open class XmlDelegatingReader protected constructor(protected val delegate: XmlReader): XmlReader {

  override fun hasNext(): Boolean
  {
    return delegate.hasNext()
  }

  override fun next(): EventType
  {
    return delegate.next()
  }

  override val isStarted: Boolean
    get() = delegate.isStarted

  override val namespaceUri: CharSequence
    get() = delegate.namespaceUri

  override val localName: CharSequence
    get() = delegate.localName

  override val prefix: CharSequence
    get() = delegate.prefix

  override val name: QName
    get() = delegate.name

  override fun require(type: EventType, namespace: CharSequence?, name: CharSequence?)
  {
    delegate.require(type, namespace, name)
  }

  override val depth: Int
    get() = delegate.depth

  override val text: CharSequence
    get() = delegate.text

  override val attributeCount: Int
    get() = delegate.attributeCount

  override fun getAttributeNamespace(i: Int): CharSequence
  {
    return delegate.getAttributeNamespace(i)
  }

  override fun getAttributePrefix(i: Int): CharSequence
  {
    return delegate.getAttributePrefix(i)
  }

  override fun getAttributeLocalName(i: Int): CharSequence
  {
    return delegate.getAttributeLocalName(i)
  }

  override fun getAttributeName(i: Int): QName
  {
    return delegate.getAttributeName(i)
  }

  override fun getAttributeValue(i: Int): CharSequence
  {
    return delegate.getAttributeValue(i)
  }

  override val eventType: EventType
    get() = delegate.eventType

  override fun getAttributeValue(nsUri: CharSequence?, localName: CharSequence): CharSequence?
  {
    return delegate.getAttributeValue(nsUri, localName)
  }

  override val namespaceStart: Int
    get() = delegate.namespaceStart

  override val namespaceEnd: Int
    get() = delegate.namespaceEnd

  override fun getNamespacePrefix(i: Int): CharSequence
  {
    return delegate.getNamespacePrefix(i)
  }

  override fun close()
  {
    delegate.close()
  }

  override fun getNamespaceUri(i: Int): CharSequence
  {
    return delegate.getNamespaceUri(i)
  }

  override fun getNamespacePrefix(namespaceUri: CharSequence): CharSequence?
  {
    return delegate.getNamespacePrefix(namespaceUri)
  }

  override fun isWhitespace(): Boolean
  {
    return delegate.isWhitespace()
  }

  override fun isEndElement(): Boolean
  {
    return delegate.isEndElement()
  }

  override fun isCharacters(): Boolean
  {
    return delegate.isCharacters()
  }

  override fun isStartElement(): Boolean
  {
    return delegate.isStartElement()
  }

  override fun getNamespaceUri(prefix: CharSequence): String?
  {
    return delegate.getNamespaceUri(prefix)
  }

  override val locationInfo: String?
    get() = delegate.locationInfo

  override val namespaceContext: NamespaceContext
    get() = delegate.namespaceContext

  override val encoding: CharSequence?
    get() = delegate.encoding

  override val standalone: Boolean?
    get() = delegate.standalone

  override val version: CharSequence?
    get() = delegate.version
}
