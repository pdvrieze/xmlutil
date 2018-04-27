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
 * Despite the name it is reasonably sophisticated, but it structures the parsing of the children and
 * provides them individually. This in contrast to [ExtXmlDeserializable] that provides full access
 * to parse the content whatever way desired.
 * Created by pdvrieze on 04/11/15.
 */
interface SimpleXmlDeserializable : XmlDeserializable {


  /**
   * Handle the current child element
   * @param reader The reader to read from. It is at the relevant start node.
   *
   * @return `true`, if processed, `false` if not (will trigger an error)
   *
   * @throws XmlException If something else failed.
   */
  fun deserializeChild(reader: XmlReader): Boolean = false

  /**
   * Handle text content in the node. This may be called multiple times in a single element if there are tags in between
   * or the parser isn't coalescing.
   * @param elementText The read text
   *
   * @return true if handled, false if not (whitespace will be ignored later on though, other text will trigger a failure)
   */
  fun deserializeChildText(elementText: CharSequence): Boolean = false
}
