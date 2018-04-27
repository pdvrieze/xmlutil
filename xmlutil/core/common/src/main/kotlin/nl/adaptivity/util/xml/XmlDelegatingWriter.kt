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

import nl.adaptivity.xml.NamespaceContext
import nl.adaptivity.xml.XmlWriter

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

  override fun setPrefix(prefix: CharSequence, namespaceUri: CharSequence)
  {
    delegate.setPrefix(prefix, namespaceUri)
  }

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

  override fun close()
  {
    delegate.close()
  }

  override fun namespaceAttr(namespacePrefix: CharSequence, namespaceUri: CharSequence)
  {
    delegate.namespaceAttr(namespacePrefix, namespaceUri)
  }

  override fun endTag(namespace: CharSequence?, localName: CharSequence, prefix: CharSequence?)
  {
    delegate.endTag(namespace, localName, prefix)
  }

  override val depth: Int
    get() = delegate.depth

  override fun processingInstruction(text: CharSequence)
  {
    delegate.processingInstruction(text)
  }

  override fun docdecl(text: CharSequence)
  {
    delegate.docdecl(text)
  }

  override fun comment(text: CharSequence)
  {
    delegate.comment(text)
  }

  override fun flush()
  {
    delegate.flush()
  }

  override fun entityRef(text: CharSequence)
  {
    delegate.entityRef(text)
  }

  override fun cdsect(text: CharSequence)
  {
    delegate.cdsect(text)
  }

  override fun ignorableWhitespace(text: CharSequence)
  {
    delegate.ignorableWhitespace(text)
  }

  override fun startTag(namespace: CharSequence?, localName: CharSequence, prefix: CharSequence?)
  {
    delegate.startTag(namespace, localName, prefix)
  }

  override fun getNamespaceUri(prefix: CharSequence): CharSequence?
  {
    return delegate.getNamespaceUri(prefix)
  }

  override fun endDocument()
  {
    delegate.endDocument()
  }

  override fun getPrefix(namespaceUri: CharSequence?): CharSequence?
  {
    return delegate.getPrefix(namespaceUri)
  }

}
