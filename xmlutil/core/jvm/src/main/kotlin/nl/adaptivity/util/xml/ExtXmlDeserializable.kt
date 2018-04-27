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

import nl.adaptivity.xml.XmlDeserializable
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlReader


/**
 * Interface that allows more customization on child deserialization than [SimpleXmlDeserializable].
 * Created by pdvrieze on 04/11/15.
 */
interface ExtXmlDeserializable : XmlDeserializable {

  /**
   * Called to have all children of the current node deserialized. The attributes have already been parsed. The expected
   * end state is that the streamreader is at the corresponding endElement.
   * @param `in` The streamreader that is the source of the events.
   */
  fun deserializeChildren(reader: XmlReader)
}
