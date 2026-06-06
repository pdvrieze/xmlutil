/*
 * Copyright (c) 2024-2026.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.XmlEvent.*
import nl.adaptivity.xmlutil.core.KtXmlReader

/** Enum representing the type of an xml node/event. */
public enum class EventType {
    /** Event representing the start of a document. */
    START_DOCUMENT {
        override val isIgnorable: Boolean get() = true

        override fun createEvent(reader: XmlReader): StartDocumentEvent = reader.run {
            StartDocumentEvent(startLocationInfo, version, encoding, standalone)
        }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) {
            writer.startDocument(reader.version, reader.encoding, reader.standalone)
        }
    },
    /** Event representing a start tag. Self-closing tags will also have an [END_ELEMENT] generated. */
    START_ELEMENT {
        override fun createEvent(reader: XmlReader): StartElementEvent =
            reader.run {
                StartElementEvent(
                    startLocationInfo,
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
            // Note that some readers expose namespace attributes as attributes (DOM!!), others don't.
            // Both need to be handled
            for (attr in reader.namespaceDecls) {
                writer.namespaceAttr(attr.prefix, attr.namespaceURI)
            }
            for (i in 0 until reader.attributeCount) {
                val attrNs = reader.getAttributeNamespace(i)
                if (attrNs != XMLConstants.XMLNS_ATTRIBUTE_NS_URI) {
                    val attrPrefix = reader.getAttributePrefix(i)
                    val prefix = when (attrNs) {
                        "" -> ""
                        writer.namespaceContext.getNamespaceURI(attrPrefix) -> attrPrefix
                        else -> writer.namespaceContext.getPrefix(attrNs) ?: attrPrefix
                    }
                    writer.attribute(
                        attrNs,
                        reader.getAttributeLocalName(i),
                        prefix,
                        reader.getAttributeValue(i)
                    )
                }
            }
        }
    },

    /** Event representing an end tag. This event is also generated for self-closing tags. */
    END_ELEMENT {
        override fun createEvent(reader: XmlReader): EndElementEvent = reader.run {
            EndElementEvent(startLocationInfo, namespaceURI, localName, prefix, namespaceContext)
        }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) {
            writer.endTag(reader.namespaceURI, reader.localName, reader.prefix)
        }
    },

    /** Event representing an XML comment. */
    COMMENT {
        override val isIgnorable: Boolean get() = true

        override val isTextElement: Boolean get() = true

        override fun createEvent(reader: XmlReader): TextEvent = reader.run {
            TextEvent(startLocationInfo, COMMENT, text)
        }

        override fun writeEvent(writer: XmlWriter, textEvent: TextEvent) {
            writer.comment(textEvent.text)
        }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) {
            writer.comment(reader.text)
        }
    },

    /** Event representing a text (content) event. */
    TEXT {
        override val isTextElement: Boolean get() = true

        override fun createEvent(reader: XmlReader): TextEvent = reader.run {
            TextEvent(startLocationInfo, TEXT, text)
        }

        override fun writeEvent(writer: XmlWriter, textEvent: TextEvent) {
            writer.text(textEvent.text)
        }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) {
            writer.text(reader.text)
        }
    },

    /** Event representing a CDATA sequence. */
    CDSECT {
        override val isTextElement: Boolean get() = true

        override fun createEvent(reader: XmlReader): TextEvent = reader.run {
            TextEvent(startLocationInfo, CDSECT, text)
        }

        override fun writeEvent(writer: XmlWriter, textEvent: TextEvent) {
            writer.cdsect(textEvent.text)
        }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) {
            writer.cdsect(reader.text)
        }
    },

    /** Event representing a document declaration. */
    DOCDECL {
        override val isIgnorable: Boolean get() = true

        override fun createEvent(reader: XmlReader): DocumentDeclEvent = reader.run {
            if (reader is KtXmlReader) {
                val docTypeName = requireNotNull(reader.docTypeName) { "Document type name is required if a documen type is specified" }
                return@run DocumentDeclEvent(startLocationInfo, docTypeName, reader.docTypePublicId, reader.docTypeSystemId)
            } else {
                val t = text
                var i = t.indexOfAny(charArrayOf(' ', '\t', '\n', '\r'))
                val docTypeName = t.substring(0, i)
                var publicId: String? = null
                var systemId: String? = null
                while (i < t.length && t[i] in " \t\n\r") i += 1
                val systemOrPublicStart = t.getOrNull(i)
                if (systemOrPublicStart == 'P') {
                    require(t.regionMatches(i+1, "UBLIC", 0, 5)) { "Expected PUBLIC" }
                    i += 6
                    while (i < t.length && t[i] in " \t\n\r") i += 1
                    var delim = t[i]
                    if (delim != '\'' && delim != '"') throw XmlException("Expected ' or \" as delimiter")
                    var end = t.indexOf(delim, i+1)
                    publicId = t.substring(i + 1, end)

                    i = end + 1
                    while (i < t.length && t[i] in " \t\n\r") i += 1
                    delim = t[i]
                    if (delim != '\'' && delim != '"') throw XmlException("Expected ' or \" as delimiter")
                    end = t.indexOf(delim, i+1)
                    systemId = t.substring(i + 1, end)
                } else if (systemOrPublicStart=='S') {
                    require(t.regionMatches(i+1, "SYSTEM", 0, 5)) { "Expected PUBLIC" }
                    i += 6
                    while (i < t.length && t[i] in " \t\n\r") i += 1
                    var delim = t[i]
                    if (delim != '\'' && delim != '"') throw XmlException("Expected ' or \" as delimiter")
                    var end = t.indexOf(delim, i+1)
                    systemId = t.substring(i + 1, end)
                }
                return DocumentDeclEvent(startLocationInfo, docTypeName, publicId, systemId)
            }
        }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) {
            createEvent(reader).writeTo(writer)
        }
    },

    /** Event representing the end of a document. */
    END_DOCUMENT {
        override val isIgnorable: Boolean get() = true

        override fun createEvent(reader: XmlReader): EndDocumentEvent = reader.run {
            EndDocumentEvent(startLocationInfo)
        }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) {
            writer.endDocument()
        }
    },

    /** Event representing an entity reference. */
    ENTITY_REF {
        override val isTextElement: Boolean get() = true

        override fun createEvent(reader: XmlReader): TextEvent = reader.run {
            EntityRefEvent(startLocationInfo, reader.localName, text)
        }

        override fun writeEvent(writer: XmlWriter, textEvent: TextEvent) {
            writer.text(textEvent.text)
        }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) {
            writer.text(reader.text)
        }
    },

    /** Event representing ignorable whitespace. */
    IGNORABLE_WHITESPACE {
        override val isIgnorable: Boolean get() = true
        override val isTextElement: Boolean get() = true

        override fun createEvent(reader: XmlReader): TextEvent = reader.run {
            TextEvent(startLocationInfo, IGNORABLE_WHITESPACE, text)
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

    /** Event representing an attribute (note that generally these events are not generated). */
    ATTRIBUTE {
        override fun createEvent(reader: XmlReader): Attribute = reader.run {
            Attribute(startLocationInfo, this.namespaceURI, localName, prefix, text)
        }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) {
            writer.attribute(reader.namespaceURI, reader.localName, reader.prefix, reader.text)
        }
    },

    /** Event representing a processing instruction. */
    PROCESSING_INSTRUCTION {

        override val isIgnorable: Boolean get() = true

        override val isTextElement: Boolean get() = true

        override fun createEvent(reader: XmlReader): TextEvent =
            ProcessingInstructionEvent(reader.startLocationInfo, reader.piTarget, reader.piData)

        override fun writeEvent(writer: XmlWriter, textEvent: TextEvent): Unit = when (textEvent) {
            is ProcessingInstructionEvent -> writer.processingInstruction(textEvent.target, textEvent.data)
            else -> writer.processingInstruction(textEvent.text)
        }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) {
            writer.processingInstruction(reader.piTarget, reader.piData)
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
