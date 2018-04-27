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

package nl.adaptivity.xml

expect enum class EventType
{
  START_DOCUMENT,
    START_ELEMENT,
  END_ELEMENT,
  COMMENT,
  TEXT,
  CDSECT,
  DOCDECL,
  END_DOCUMENT,
  ENTITY_REF,
  IGNORABLE_WHITESPACE,
  ATTRIBUTE,
  PROCESSING_INSTRUCTION;

  val isIgnorable:Boolean

  open fun writeEvent(writer: XmlWriter, textEvent: XmlEvent.TextEvent): Unit

  abstract fun writeEvent(writer: XmlWriter, reader: XmlReader)

  abstract fun createEvent(reader: XmlReader): XmlEvent

}