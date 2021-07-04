/*
 * Copyright (c) 2019.
 *
 * This file is part of xmlutil.
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
 * Simple baseclass for a delating XmlReader.
 * It merely functions as a delegate With Kotlin it's not really needed, but nice.
 */
public open class XmlDelegatingReader protected constructor(protected open val delegate: XmlReader) :
    XmlReader by delegate {
    override fun nextTag(): EventType {
        /*
         * Needs to be overridden here so that when next is overridden it remains valid.
         */
        var eventType = next()

        while ((eventType == EventType.TEXT && isWhitespace()) // skip whitespace
            || (eventType == EventType.CDSECT && isWhitespace())
            // skip whitespace
            || eventType == EventType.IGNORABLE_WHITESPACE
            || eventType == EventType.PROCESSING_INSTRUCTION
            || eventType == EventType.COMMENT
        ) {
            eventType = next()
        }
        if (eventType != EventType.START_ELEMENT && eventType != EventType.END_ELEMENT) {
            throw XmlException("expected start or end tag")
        }
        return eventType;
    }
}
