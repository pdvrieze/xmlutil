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

enum class EventType {
    START_DOCUMENT(isIgnorable = true) {
        override fun createEvent(reader: XmlReader) = reader.run {
            XmlEvent.StartDocumentEvent(locationInfo, version, encoding, standalone)
        }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.startDocument(reader.version, reader.encoding, reader.standalone)
    },
    START_ELEMENT {
        override fun createEvent(reader: XmlReader) = reader.run {
            XmlEvent.StartElementEvent(locationInfo, namespaceURI, localName, prefix, attributes,
                                       namespaceDecls)
        }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) {
            writer.startTag(reader.namespaceURI, reader.localName, reader.prefix)
            for (i in reader.namespaceStart until reader.namespaceEnd) {
                writer.namespaceAttr(reader.getNamespacePrefix(i), reader.getNamespaceURI(i))
            }
            for (i in 0 until reader.attributeCount) {
                writer.attribute(reader.getAttributeNamespace(i), reader.getAttributeLocalName(i), null,
                                 reader.getAttributeValue(i))
            }
        }
    },
    END_ELEMENT {
        override fun createEvent(reader: XmlReader) = reader.run {
            XmlEvent.EndElementEvent(locationInfo, namespaceURI, localName, prefix)
        }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.endTag(reader.namespaceURI, reader.localName, reader.prefix)
    },
    COMMENT(isIgnorable = true) {
        override fun createEvent(reader: XmlReader) = reader.run {
            XmlEvent.TextEvent(locationInfo, COMMENT, text)
        }

        override fun writeEvent(writer: XmlWriter, textEvent: XmlEvent.TextEvent) = writer.comment(textEvent.text)

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.comment(reader.text)
    },
    TEXT {
        override fun createEvent(reader: XmlReader) = reader.run {
            XmlEvent.TextEvent(locationInfo, TEXT, text)
        }

        override fun writeEvent(writer: XmlWriter, textEvent: XmlEvent.TextEvent) = writer.text(textEvent.text)

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.text(reader.text)
    },
    CDSECT {
        override fun createEvent(reader: XmlReader) = reader.run {
            XmlEvent.TextEvent(locationInfo, CDSECT, text)
        }

        override fun writeEvent(writer: XmlWriter, textEvent: XmlEvent.TextEvent) = writer.cdsect(textEvent.text)

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.cdsect(reader.text)
    },
    DOCDECL(isIgnorable = true) {
        override fun createEvent(reader: XmlReader) = reader.run {
            XmlEvent.TextEvent(locationInfo, DOCDECL, text)
        }

        override fun writeEvent(writer: XmlWriter, textEvent: XmlEvent.TextEvent) = writer.docdecl(textEvent.text)

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.docdecl(reader.text)
    },
    END_DOCUMENT(isIgnorable = true) {
        override fun createEvent(reader: XmlReader) = reader.run {
            XmlEvent.EndDocumentEvent(locationInfo)
        }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.endDocument()
    },
    ENTITY_REF {
        override fun createEvent(reader: XmlReader) = reader.run {
            XmlEvent.TextEvent(locationInfo, ENTITY_REF, text)
        }

        override fun writeEvent(writer: XmlWriter, textEvent: XmlEvent.TextEvent) = writer.entityRef(textEvent.text)

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.entityRef(reader.text)
    },
    IGNORABLE_WHITESPACE(isIgnorable = true) {
        override fun createEvent(reader: XmlReader) = reader.run {
            XmlEvent.TextEvent(locationInfo, IGNORABLE_WHITESPACE, text)
        }

        override fun writeEvent(writer: XmlWriter, textEvent: XmlEvent.TextEvent) = writer.ignorableWhitespace(
            textEvent.text)

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.ignorableWhitespace(reader.text)
    },
    ATTRIBUTE {
        override fun createEvent(reader: XmlReader) = reader.run {
            XmlEvent.Attribute(locationInfo, this.namespaceURI, localName, prefix, text)
        }

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.attribute(reader.namespaceURI, reader.localName, reader.prefix, reader.text)
    },
    PROCESSING_INSTRUCTION(isIgnorable = true) {
        override fun createEvent(reader: XmlReader) = XmlEvent.TextEvent(
            reader.locationInfo, PROCESSING_INSTRUCTION, reader.text)


        override fun writeEvent(writer: XmlWriter, textEvent: XmlEvent.TextEvent) = writer.processingInstruction(
            textEvent.text)

        override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.processingInstruction(reader.text)
    };

    val isIgnorable: Boolean

    constructor() {
        isIgnorable = false
    }

    constructor(isIgnorable: Boolean) {
        this.isIgnorable = isIgnorable
    }

    open fun writeEvent(writer: XmlWriter, textEvent: XmlEvent.TextEvent): Unit = throw UnsupportedOperationException(
        "This is not generally supported, only by text types")

    abstract fun writeEvent(writer: XmlWriter, reader: XmlReader)

    abstract fun createEvent(reader: XmlReader): XmlEvent

}
