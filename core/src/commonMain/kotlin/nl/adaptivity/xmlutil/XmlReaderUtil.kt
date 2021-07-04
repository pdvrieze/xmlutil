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

@file:JvmMultifileClass
@file:JvmName("XmlReaderUtil")

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.core.impl.multiplatform.Throws
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

public fun XmlReader.asSubstream(): XmlReader =
    SubstreamFilterReader(this)

/**
 * A class that filters an xml stream such that it will only contain expected elements.
 */
private class SubstreamFilterReader(delegate: XmlReader) : XmlBufferedReader(delegate) {

    @Throws(XmlException::class)
    override fun doPeek(): List<XmlEvent> {
        return super.doPeek().filter {
            when (it.eventType) {
                EventType.START_DOCUMENT, EventType.PROCESSING_INSTRUCTION, EventType.DOCDECL, EventType.END_DOCUMENT -> false
                else -> true
            }
        }
    }
}

public fun XmlReader.toEvent(): XmlEvent = eventType.createEvent(this)
