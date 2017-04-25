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

import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlWriter

import javax.xml.namespace.NamespaceContext

import java.io.IOException
import java.io.Reader


/**
 *
 * Simple delegating writer that passes all calls on to the delegate. This class is abstract for the only reason that any
 * direct instances of this class make little sense.
 *
 *
 * Created by pdvrieze on 17/11/15.
 *
 */
abstract class XmlDelegatingWriter(private val delegate: XmlWriter) : XmlWriter
{

  @Throws(XmlException::class)
  override fun setPrefix(prefix: CharSequence, namespaceUri: CharSequence)
  {
    delegate.setPrefix(prefix, namespaceUri)
  }

  @Throws(XmlException::class)
  override fun startDocument(version: CharSequence?, encoding: CharSequence?, standalone: Boolean?)
  {
    delegate.startDocument(version, encoding, standalone)
  }

  override fun attribute(namespace: CharSequence?, name: CharSequence, prefix: CharSequence?, value: CharSequence)
  {
    delegate.attribute(namespace, name, prefix, value)
  }

  override fun text(text: CharSequence)
  {
    delegate.text(text)
  }

  override val namespaceContext: NamespaceContext
    get() = delegate.namespaceContext

  @Throws(XmlException::class)
  override fun close()
  {
    delegate.close()
  }

  @Throws(XmlException::class)
  override fun namespaceAttr(namespacePrefix: CharSequence, namespaceUri: CharSequence)
  {
    delegate.namespaceAttr(namespacePrefix, namespaceUri)
  }

  @Throws(XmlException::class)
  override fun endTag(namespace: CharSequence?, localName: CharSequence, prefix: CharSequence?)
  {
    delegate.endTag(namespace, localName, prefix)
  }

  override val depth: Int
    get() = delegate.depth

  @Throws(XmlException::class)
  override fun processingInstruction(text: CharSequence)
  {
    delegate.processingInstruction(text)
  }

  @Throws(XmlException::class)
  override fun docdecl(text: CharSequence)
  {
    delegate.docdecl(text)
  }

  @Throws(XmlException::class)
  override fun comment(text: CharSequence)
  {
    delegate.comment(text)
  }

  @Throws(XmlException::class)
  override fun flush()
  {
    delegate.flush()
  }

  @Throws(XmlException::class)
  override fun entityRef(text: CharSequence)
  {
    delegate.entityRef(text)
  }

  @Throws(XmlException::class)
  override fun cdsect(text: CharSequence)
  {
    delegate.cdsect(text)
  }

  @Throws(XmlException::class)
  override fun ignorableWhitespace(text: CharSequence)
  {
    delegate.ignorableWhitespace(text)
  }

  override fun startTag(namespace: CharSequence?, localName: CharSequence, prefix: CharSequence?)
  {
    delegate.startTag(namespace, localName, prefix)
  }

  @Throws(XmlException::class)
  override fun getNamespaceUri(prefix: CharSequence): CharSequence?
  {
    return delegate.getNamespaceUri(prefix)
  }

  @Throws(XmlException::class)
  override fun endDocument()
  {
    delegate.endDocument()
  }

  @Throws(XmlException::class)
  override fun getPrefix(namespaceUri: CharSequence?): CharSequence?
  {
    return delegate.getPrefix(namespaceUri)
  }

}
