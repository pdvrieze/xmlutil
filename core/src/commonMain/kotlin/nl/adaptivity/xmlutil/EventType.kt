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

@file:OptIn(XmlUtilInternal::class)

package nl.adaptivity.xmlutil

/** Enum representing the type of an xml node/event. */
enum class EventType {
    START_DOCUMENT {
        override val isIgnorable: Boolean get() = true

        override fun createEvent(reader: XmlReader): XmlEvent.StartDocumentEvent =
            reader.run {
                XmlEvent.StartDocumentEvent(
                    locationInfo,
                    version,
                    encoding,
                    standalone
                                           )
            }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.startDocument(
                reader.version,
                reader.encoding,
                reader.standalone
                                )
    },
    START_ELEMENT {
        override fun createEvent(reader: XmlReader): XmlEvent.StartElementEvent =
            reader.run {
                XmlEvent.StartElementEvent(
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
            writer.startTag(
                reader.namespaceURI,
                reader.localName,
                reader.prefix
                           )
            for (namespace in reader.namespaceContext.freeze())
            for (i in reader.namespaceStart until reader.namespaceEnd) {
                writer.namespaceAttr(
                    reader.getNamespacePrefix(i),
                    reader.getNamespaceURI(i)
                                    )
            }
            for (i in 0 until reader.attributeCount) {
                writer.attribute(
                    reader.getAttributeNamespace(i),
                    reader.getAttributeLocalName(i),
                    null,
                    reader.getAttributeValue(i)
                                )
            }
        }
    },
    END_ELEMENT {
        override fun createEvent(reader: XmlReader) = reader.run {
            XmlEvent.EndElementEvent(
                locationInfo,
                namespaceURI,
                localName,
                prefix,
                namespaceContext
                                    )
        }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.endTag(
                reader.namespaceURI,
                reader.localName,
                reader.prefix
                         )
    },
    COMMENT {
        override val isIgnorable: Boolean get() = true

        override fun createEvent(reader: XmlReader): XmlEvent.TextEvent =
            reader.run {
                XmlEvent.TextEvent(locationInfo, COMMENT, text)
            }

        override fun writeEvent(
            writer: XmlWriter,
            textEvent: XmlEvent.TextEvent
                               ) = writer.comment(textEvent.text)

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.comment(reader.text)
    },
    TEXT {
        override val isTextElement: Boolean get() = true

        override fun createEvent(reader: XmlReader) = reader.run {
            XmlEvent.TextEvent(locationInfo, TEXT, text)
        }

        override fun writeEvent(
            writer: XmlWriter,
            textEvent: XmlEvent.TextEvent
                               ) = writer.text(textEvent.text)

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.text(reader.text)
    },
    CDSECT {
        override val isTextElement: Boolean get() = true

        override fun createEvent(reader: XmlReader) = reader.run {
            XmlEvent.TextEvent(locationInfo, CDSECT, text)
        }

        override fun writeEvent(
            writer: XmlWriter,
            textEvent: XmlEvent.TextEvent
                               ) = writer.cdsect(textEvent.text)

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.cdsect(reader.text)
    },
    DOCDECL {
        override val isIgnorable: Boolean get() = true

        override fun createEvent(reader: XmlReader): XmlEvent.TextEvent =
            reader.run {
                XmlEvent.TextEvent(locationInfo, DOCDECL, text)
            }

        override fun writeEvent(
            writer: XmlWriter,
            textEvent: XmlEvent.TextEvent
                               ) = writer.docdecl(textEvent.text)

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.docdecl(reader.text)
    },
    END_DOCUMENT {
        override val isIgnorable: Boolean get() = true

        override fun createEvent(reader: XmlReader) = reader.run {
            XmlEvent.EndDocumentEvent(locationInfo)
        }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.endDocument()
    },
    ENTITY_REF {
        override val isTextElement: Boolean get() = true

        override fun createEvent(reader: XmlReader) = reader.run {
            XmlEvent.TextEvent(locationInfo, ENTITY_REF, text)
        }

        override fun writeEvent(
            writer: XmlWriter,
            textEvent: XmlEvent.TextEvent
                               ) = writer.entityRef(textEvent.text)

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.entityRef(reader.text)
    },
    IGNORABLE_WHITESPACE {
        override val isIgnorable: Boolean get() = true

        override fun createEvent(reader: XmlReader): XmlEvent.TextEvent =
            reader.run {
                XmlEvent.TextEvent(
                    locationInfo,
                    IGNORABLE_WHITESPACE,
                    text
                                  )
            }

        override fun writeEvent(
            writer: XmlWriter,
            textEvent: XmlEvent.TextEvent
                               ) = writer.ignorableWhitespace(
            textEvent.text
                                                             )

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.ignorableWhitespace(reader.text)
    },
    ATTRIBUTE {
        override fun createEvent(reader: XmlReader) = reader.run {
            XmlEvent.Attribute(
                locationInfo,
                this.namespaceURI,
                localName,
                prefix,
                text
                              )
        }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.attribute(
                reader.namespaceURI,
                reader.localName,
                reader.prefix,
                reader.text
                            )
    },
    PROCESSING_INSTRUCTION {

        override val isIgnorable: Boolean get() = true

        override fun createEvent(reader: XmlReader): XmlEvent.TextEvent =
            XmlEvent.TextEvent(
                reader.locationInfo,
                PROCESSING_INSTRUCTION,
                reader.text
                              )

        override fun writeEvent(
            writer: XmlWriter,
            textEvent: XmlEvent.TextEvent
                               ) = writer.processingInstruction(
            textEvent.text
                                                               )

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.processingInstruction(reader.text)
    };

    /** Can this event type be ignored without losing meaning. */
    open val isIgnorable: Boolean get() = false

    /** Is this an event for elements that have text content. */
    open val isTextElement: Boolean get() = false

    /** Shortcut to allow writing text events (only for text event types).
     * The reader is expected to just have read the event with this type.
     */
    open fun writeEvent(
        writer: XmlWriter,
        textEvent: XmlEvent.TextEvent
                       ): Unit = throw UnsupportedOperationException(
        "This is not generally supported, only by text types"
                                                                    )

    /** Read the rest of the event from the [reader] and write it to the
     *  [writer]. The reader is expected to just have
     * read the event with this type.
     */
    abstract fun writeEvent(writer: XmlWriter, reader: XmlReader)

    /**
     * Create an [XmlEvent] corresponding to the event type. The parameters
     * are taken from the [reader]. The reader is expected to just have
     * read the event with this type.
     */
    abstract fun createEvent(reader: XmlReader): XmlEvent

}

fun XmlReader.toEvent(): XmlEvent = eventType.createEvent(this)
