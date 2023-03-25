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

import nl.adaptivity.xmlutil.XmlEvent.*

/** Enum representing the type of an xml node/event. */
public enum class EventType {
    START_DOCUMENT {
        override val isIgnorable: Boolean get() = true

        override fun createEvent(reader: XmlReader): StartDocumentEvent = reader.run {
            StartDocumentEvent(locationInfo, version, encoding, standalone)
        }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) {
            writer.startDocument(reader.version, reader.encoding, reader.standalone)
        }
    },
    START_ELEMENT {
        override fun createEvent(reader: XmlReader): StartElementEvent =
            reader.run {
                StartElementEvent(
                    locationInfo,
                    namespaceURI,
                    localName,
                    prefix,
                    attributes,
                    reader.namespaceContext.freeze(),
                    namespaceDecls
                )
            }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) {
            writer.startTag(reader.namespaceURI, reader.localName, reader.prefix)

            for (attr in reader.namespaceDecls) {
                writer.namespaceAttr(attr.prefix, attr.namespaceURI)
            }
            for (i in 0 until reader.attributeCount) {
                val attrNs = reader.getAttributeNamespace(i)
                if (attrNs!=XMLConstants.XMLNS_ATTRIBUTE_NS_URI) {
                    writer.attribute(
                        attrNs,
                        reader.getAttributeLocalName(i),
                        null,
                        reader.getAttributeValue(i)
                    )
                }
            }
        }
    },
    END_ELEMENT {
        override fun createEvent(reader: XmlReader): EndElementEvent = reader.run {
            EndElementEvent(locationInfo, namespaceURI, localName, prefix, namespaceContext)
        }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) {
            writer.endTag(reader.namespaceURI, reader.localName, reader.prefix)
        }
    },
    COMMENT {
        override val isIgnorable: Boolean get() = true

        override val isTextElement: Boolean get() = true

        override fun createEvent(reader: XmlReader): TextEvent = reader.run {
            TextEvent(locationInfo, COMMENT, text)
        }

        override fun writeEvent(writer: XmlWriter, textEvent: TextEvent) {
            writer.comment(textEvent.text)
        }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) {
            writer.comment(reader.text)
        }
    },
    TEXT {
        override val isTextElement: Boolean get() = true

        override fun createEvent(reader: XmlReader): TextEvent = reader.run {
            TextEvent(locationInfo, TEXT, text)
        }

        override fun writeEvent(writer: XmlWriter, textEvent: TextEvent) {
            writer.text(textEvent.text)
        }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) {
            writer.text(reader.text)
        }
    },
    CDSECT {
        override val isTextElement: Boolean get() = true

        override fun createEvent(reader: XmlReader): TextEvent = reader.run {
            TextEvent(locationInfo, CDSECT, text)
        }

        override fun writeEvent(writer: XmlWriter, textEvent: TextEvent) {
            writer.cdsect(textEvent.text)
        }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) {
            writer.cdsect(reader.text)
        }
    },
    DOCDECL {
        override val isIgnorable: Boolean get() = true

        override fun createEvent(reader: XmlReader): TextEvent = reader.run {
            TextEvent(locationInfo, DOCDECL, text)
        }

        override fun writeEvent(writer: XmlWriter, textEvent: TextEvent) {
            writer.docdecl(textEvent.text)
        }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) {
            writer.docdecl(reader.text)
        }
    },
    END_DOCUMENT {
        override val isIgnorable: Boolean get() = true

        override fun createEvent(reader: XmlReader): EndDocumentEvent = reader.run {
            EndDocumentEvent(locationInfo)
        }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) {
            writer.endDocument()
        }
    },
    ENTITY_REF {
        override val isTextElement: Boolean get() = true

        override fun createEvent(reader: XmlReader): TextEvent = reader.run {
            EntityRefEvent(locationInfo, reader.localName, text)
        }

        override fun writeEvent(writer: XmlWriter, textEvent: TextEvent) {
            writer.text(textEvent.text)
        }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) {
            writer.text(reader.text)
        }
    },
    IGNORABLE_WHITESPACE {
        override val isIgnorable: Boolean get() = true
        override val isTextElement: Boolean get() = true

        override fun createEvent(reader: XmlReader): TextEvent = reader.run {
            TextEvent(locationInfo, IGNORABLE_WHITESPACE, text)
        }

        override fun writeEvent(writer: XmlWriter, textEvent: TextEvent) {
            writer.ignorableWhitespace(
                textEvent.text
            )
        }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) {
            writer.ignorableWhitespace(reader.text)
        }
    },
    ATTRIBUTE {
        override fun createEvent(reader: XmlReader): Attribute = reader.run {
            Attribute(locationInfo, this.namespaceURI, localName, prefix, text)
        }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) {
            writer.attribute(reader.namespaceURI, reader.localName, reader.prefix, reader.text)
        }
    },
    PROCESSING_INSTRUCTION {

        override val isIgnorable: Boolean get() = true

        override fun createEvent(reader: XmlReader): TextEvent =
            TextEvent(reader.locationInfo, PROCESSING_INSTRUCTION, reader.text)

        override fun writeEvent(writer: XmlWriter, textEvent: TextEvent) {
            writer.processingInstruction(textEvent.text)
        }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) {
            writer.processingInstruction(reader.text)
        }
    };

    /** Can this event type be ignored without losing meaning. */
    public open val isIgnorable: Boolean get() = false

    /** Is this an event for elements that have text content. */
    public open val isTextElement: Boolean get() = false

    /** Shortcut to allow writing text events (only for text event types).
     * The reader is expected to just have read the event with this type.
     */
    public open fun writeEvent(writer: XmlWriter, textEvent: TextEvent) {
        throw UnsupportedOperationException("This is not generally supported, only by text types")
    }

    /** Read the rest of the event from the [reader] and write it to the
     *  [writer]. The reader is expected to just have
     * read the event with this type.
     */
    public abstract fun writeEvent(writer: XmlWriter, reader: XmlReader)

    /**
     * Create an [XmlEvent] corresponding to the event type. The parameters
     * are taken from the [reader]. The reader is expected to just have
     * read the event with this type.
     */
    public abstract fun createEvent(reader: XmlReader): XmlEvent

}
