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

import nl.adaptivity.xml.*
import org.w3c.dom.*

import javax.xml.XMLConstants
import javax.xml.bind.annotation.adapters.XmlAdapter
import javax.xml.namespace.QName
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.stream.*
import javax.xml.transform.dom.DOMResult
import javax.xml.transform.dom.DOMSource


interface XmlSerializable {

  /**
   * Write the object to an xml stream. The object is expected to write itself and its children.
   * @param out The stream to write to.
   * *
   * @throws XMLStreamException When something breaks.
   */
  @Throws(XmlException::class)
  fun serialize(out: XmlWriter)

}
