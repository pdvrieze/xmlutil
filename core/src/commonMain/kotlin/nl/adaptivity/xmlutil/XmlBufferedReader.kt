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

import nl.adaptivity.xmlutil.multiplatform.SimpleQueue
import nl.adaptivity.xmlutil.multiplatform.addAll
import nl.adaptivity.xmlutil.multiplatform.isNotEmpty

/**
 * An xml reader that has a buffer that allows peeking events as well as injecting events into the stream. Note that
 * this class does not do any validation of the xml. If injecting/removing elements into/from the buffer you can create
 * invalid XML.
 */
open class XmlBufferedReader constructor(delegate: XmlReader) : XmlBufferedReaderBase(delegate) {

    override val namespaceContext: NamespaceContext
        get() = super.namespaceContext

    private val peekBuffer = SimpleQueue<XmlEvent>()

    override val hasPeekItems get() = peekBuffer.isNotEmpty()

    /**
     * Peek the first element in the buffer, if it exists, otherwise `null`. Note that a null value does not
     * reflect an empty stream (or end of file), only an empty buffer.
     */
    override fun peekFirst(): XmlEvent? {
        return peekBuffer.peekFirst()
    }

    /**
     * Peek the last element in the buffer, if it exists, otherwise `null`. Note that a null value does not
     * reflect an empty stream (or end of file), only an empty buffer.
     */
    override fun peekLast(): XmlEvent? {
        return peekBuffer.peekLast()
    }

    /**
     * Remove the top element in the peek buffer (the one returned by [peekFirst])
     */
    override fun bufferRemoveLast() = peekBuffer.removeLast()

    /**
     * Remove the bottom element in the peek buffer (the one returned by [peekLast])
     */
    override fun bufferRemoveFirst() = peekBuffer.removeFirst()

    /**
     * Add an element to the peek buffer.
     */
    override fun add(event: XmlEvent) {
        peekBuffer.addLast(event)
    }

    /**
     * Add events to the peek buffer.
     */
    override fun addAll(events: Collection<XmlEvent>) {
        peekBuffer.addAll(events)
    }

    override fun close() {
        super.close()
        peekBuffer.clear()
    }
}