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

package nl.adaptivity.xmlutil.util

import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.XmlReader


/**
 * Simple baseclass for a delating XmlReader.
 * It merely functions as a delegate With Kotlin it's not really needed, but nice.
 */
open class XmlDelegatingReader protected constructor(protected open val delegate: XmlReader) : XmlReader by delegate {
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
               || eventType == EventType.COMMENT) {
            eventType = next();
        }
        if (eventType != EventType.START_ELEMENT && eventType != EventType.END_ELEMENT) {
            throw XmlException("expected start or end tag");
        }
        return eventType;
    }
}
