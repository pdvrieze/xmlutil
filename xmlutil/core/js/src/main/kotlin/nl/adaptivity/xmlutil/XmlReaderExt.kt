/*
 * Copyright (c) 2018.
 *
 * This file is part of xmlutil.
 *
 * xmlutil is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * xmlutil is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with xmlutil.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.util.CompactFragment

/**
 * Functions that work on both js/jvm but have different implementations
 */

/**
 * Read the current element (and content) and all its siblings into a fragment.
 * @receiver The source stream.
 *
 * @return the fragment
 *
 * @throws XmlException parsing failed
 */
actual fun XmlReader.siblingsToFragment(): CompactFragment
{
  val dest = (this as JSDomReader).delegate.ownerDocument!!.createDocumentFragment()
  if (!isStarted) {
    if (hasNext()) {
      next()
    } else {
      return CompactFragment(dest)
    }
  }

  val startLocation = locationInfo
  try {

    val missingNamespaces = mutableMapOf<String, String>()
    // If we are at a start tag, the depth will already have been increased. So in that case, reduce one.
    val initialDepth = depth - if (eventType === EventType.START_ELEMENT) 1 else 0
    var type: EventType? = eventType
    while (type !== EventType.END_DOCUMENT && type !== EventType.END_ELEMENT && depth >= initialDepth) {
        when(type) {
            EventType.START_ELEMENT -> {
                val out = JSDomWriter(dest, true)
                writeCurrent(out) // writes the start tag
                out.addUndeclaredNamespaces(this, missingNamespaces)
                out.writeElementContent(missingNamespaces, this) // writes the children and end tag
                out.close()
            }
            EventType.IGNORABLE_WHITESPACE,
            EventType.TEXT          -> dest.append(dest.ownerDocument!!.createTextNode(text))
            EventType.CDSECT        -> dest.append(dest.ownerDocument!!.createCDATASection(text))
            EventType.COMMENT       -> dest.append(dest.ownerDocument!!.createComment(text))
        }
      type = if (hasNext()) next() else null
    }
    return CompactFragment(dest)
  } catch (e: XmlException) {
    throw XmlException("Failure to parse children into string at $startLocation", e)
  } catch (e: RuntimeException) {
    throw XmlException("Failure to parse children into string at $startLocation", e)
  }

}
