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

@file:JvmMultifileClass
@file:JvmName("XmlReaderUtil")

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.multiplatform.JvmMultifileClass
import nl.adaptivity.xmlutil.multiplatform.JvmName
import nl.adaptivity.xmlutil.multiplatform.Throws

fun XmlReader.asSubstream(): XmlReader = SubstreamFilterReader(
    this)

/**
 * A class that filters an xml stream such that it will only contain expected elements.
 */
private class SubstreamFilterReader(delegate: XmlReader) : XmlBufferedReader(delegate) {

  @Throws(XmlException::class)
  override fun doPeek(): List<XmlEvent> {
    return super.doPeek().filter {
      when (it.eventType) {
        EventType.START_DOCUMENT, EventType.PROCESSING_INSTRUCTION, EventType.DOCDECL, EventType.END_DOCUMENT -> false
        else                                                                                                  -> true
      }
    }
  }
}

