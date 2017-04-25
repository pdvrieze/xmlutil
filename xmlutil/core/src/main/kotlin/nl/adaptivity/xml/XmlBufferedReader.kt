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

package nl.adaptivity.xml

import java.util.*

/**
 * Created by pdvrieze on 03/04/17.
 */
open class XmlBufferedReader(delegate:XmlReader): XmlBufferedReaderBase(delegate)
{
  private val peekBuffer = ArrayDeque<XmlEvent>()

  override protected val hasPeekItems get() = peekBuffer.isNotEmpty()

  override protected fun peekFirst(): XmlEvent {
    return peekBuffer.peekFirst()
  }

  override protected fun peekLast(): XmlEvent {
    return peekBuffer.peekLast()
  }

  override fun bufferRemoveLast() = peekBuffer.removeLast()

  override fun bufferRemoveFirst() = peekBuffer.removeFirst()

  override protected fun add(event: XmlEvent) {
    peekBuffer.addLast(event)
  }

  override protected fun addAll(events: Collection<XmlEvent>) {
    peekBuffer.addAll(events)
  }

  override fun close()
  {
    super.close()
    peekBuffer.clear()
  }
}