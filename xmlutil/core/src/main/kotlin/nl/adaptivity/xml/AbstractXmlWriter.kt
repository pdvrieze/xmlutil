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
import nl.adaptivity.util.xml.XmlDelegatingWriter
import nl.adaptivity.xml.XmlStreaming.EventType
import org.w3c.dom.Node
import javax.xml.namespace.QName
import javax.xml.transform.dom.DOMSource

/**
 * Created by pdvrieze on 16/11/15.
 */
abstract class AbstractXmlWriter : XmlWriter {

  /**
   * Default implementation that merely flushes the stream.
   * @throws XmlException When something fails
   */
  @Throws(XmlException::class)
  override fun close() = flush()
}

