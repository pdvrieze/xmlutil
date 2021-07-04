/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package nl.adaptivity.xmlutil

/**
 * An xml reader that has a buffer that allows peeking events as well as injecting events into the stream. Note that
 * this class does not do any validation of the xml. If injecting/removing elements into/from the buffer you can create
 * invalid XML.
 */
public open class XmlBufferedReader constructor(delegate: XmlReader) : XmlBufferedReaderBase(delegate) {

    private val peekBuffer = ArrayDeque<XmlEvent>()

    @get:XmlUtilInternal
    override val hasPeekItems: Boolean
        get() = peekBuffer.isNotEmpty()

    /**
     * Peek the first element in the buffer, if it exists, otherwise `null`. Note that a null value does not
     * reflect an empty stream (or end of file), only an empty buffer.
     */
    override fun peekFirst(): XmlEvent? {
        return peekBuffer.firstOrNull()
    }

    /**
     * Peek the last element in the buffer, if it exists, otherwise `null`. Note that a null value does not
     * reflect an empty stream (or end of file), only an empty buffer.
     */
    @XmlUtilInternal
    override fun peekLast(): XmlEvent? {
        return peekBuffer.lastOrNull()
    }

    /**
     * Remove the top element in the peek buffer (the one returned by [peekFirst])
     */
    @XmlUtilInternal
    override fun bufferRemoveLast(): XmlEvent = peekBuffer.removeLast()

    /**
     * Remove the bottom element in the peek buffer (the one returned by [peekLast])
     */
    @XmlUtilInternal
    override fun bufferRemoveFirst(): XmlEvent = peekBuffer.removeFirst()

    /**
     * Add an element to the peek buffer.
     */
    @XmlUtilInternal
    override fun add(event: XmlEvent) {
        peekBuffer.addLast(event)
    }

    /**
     * Add events to the peek buffer.
     */
    @XmlUtilInternal
    override fun addAll(events: Collection<XmlEvent>) {
        peekBuffer.addAll(events)
    }

    override fun close() {
        super.close()
        peekBuffer.clear()
    }
}
