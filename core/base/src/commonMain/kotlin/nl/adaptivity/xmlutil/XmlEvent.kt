/*
 * Copyright (c) 2024-2025.
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

/**
 * A class to represent the events that can occur in XML Documents
 *
 */
public sealed class XmlEvent(public val extLocationInfo: XmlReader.LocationInfo?) {

    protected constructor(locationInfo: String?) : this(locationInfo?.let(XmlReader::StringLocationInfo))

    public companion object {

        public fun from(reader: XmlReader): XmlEvent = reader.eventType.createEvent(reader)

    }

    public open class TextEvent(
        extLocationInfo: XmlReader.LocationInfo?,
        override val eventType: EventType,
        public val text: String
    ) : XmlEvent(extLocationInfo) {

        public constructor(locationInfo: String, eventType: EventType, text: String) :
                this(locationInfo.let(XmlReader::StringLocationInfo), eventType, text)

        override fun writeTo(writer: XmlWriter): Unit = eventType.writeEvent(writer, this)

        override val isIgnorable: Boolean
            get() = super.isIgnorable ||
                    (eventType == EventType.TEXT && isXmlWhitespace(text))

        override fun toString(): String {
            return "$eventType - \"$text\" (${extLocationInfo ?: ""})"
        }
    }

    public class ProcessingInstructionEvent(
        extLocationInfo: XmlReader.LocationInfo?,
        public val target: String,
        public val data: String
    ) :
        TextEvent(extLocationInfo, EventType.PROCESSING_INSTRUCTION, "$target $data") {
        public constructor(locationInfo: String, target: String, data: String) :
                this(locationInfo.let(XmlReader::StringLocationInfo), target, data)

    }

    public class EntityRefEvent(
        extLocationInfo: XmlReader.LocationInfo?,
        public val localName: String,
        public val isResolved: Boolean,
        text: String
    ) : TextEvent(extLocationInfo, EventType.ENTITY_REF, text) {

        public constructor(
            extLocationInfo: XmlReader.LocationInfo?,
            localName: String,
            text: String
        ) : this(extLocationInfo, localName, text.isNotEmpty(), text)

        override fun writeTo(writer: XmlWriter): Unit = eventType.writeEvent(writer, this)

        override val isIgnorable: Boolean
            get() = false

        override fun toString(): String {
            return "$eventType - \"$text\" (${extLocationInfo ?: ""})"
        }
    }

    public class EndDocumentEvent(extLocationInfo: XmlReader.LocationInfo?) : XmlEvent(extLocationInfo) {
        public constructor(locationInfo: String) :
                this(locationInfo.let(XmlReader::StringLocationInfo))

        override fun writeTo(writer: XmlWriter): Unit = writer.endDocument()

        override val eventType: EventType get() = EventType.END_DOCUMENT

        override fun toString(): String {
            return "$eventType (${extLocationInfo ?: ""})"
        }

    }

    public class EndElementEvent(
        extLocationInfo: XmlReader.LocationInfo?,
        namespaceUri: String,
        localName: String,
        prefix: String,
        namespaceContext: IterableNamespaceContext,
    ) : NamedEvent(extLocationInfo, namespaceUri, localName, prefix) {

        public constructor(
            name: QName,
            parentNamespaceContext: IterableNamespaceContext,
            extLocationInfo: XmlReader.LocationInfo? = null
        ) : this(extLocationInfo, name.namespaceURI, name.localPart, name.prefix, parentNamespaceContext)


        public constructor(
            locationInfo: String, namespaceUri: String,
            localName: String,
            prefix: String,
            namespaceContext: IterableNamespaceContext
        ) : this(
            locationInfo.let(XmlReader::StringLocationInfo),
            namespaceUri,
            localName,
            prefix,
            namespaceContext
        )

        override fun writeTo(writer: XmlWriter): Unit = writer.endTag(namespaceUri, localName, prefix)

        override val eventType: EventType get() = EventType.END_ELEMENT

        public val namespaceContext: IterableNamespaceContext = namespaceContext.freeze()
    }

    public class StartDocumentEvent(
        extLocationInfo: XmlReader.LocationInfo?,
        public val encoding: String?,
        public val version: String?,
        public val standalone: Boolean?
    ) : XmlEvent(extLocationInfo) {

        public constructor(locationInfo: String, encoding: String?, version: String?, standalone: Boolean?) :
                this(locationInfo.let(XmlReader::StringLocationInfo), encoding, version, standalone)

        override fun writeTo(writer: XmlWriter): Unit = writer.startDocument(version, encoding, standalone)

        override val eventType: EventType get() = EventType.START_DOCUMENT

        override fun toString(): String {
            return "$eventType - encoding:$encoding, version: $version, standalone: $standalone (${extLocationInfo ?: ""})"
        }

    }

    public abstract class NamedEvent(
        extLocationInfo: XmlReader.LocationInfo?,
        public val namespaceUri: String,
        public val localName: String,
        public val prefix: String
    ) : XmlEvent(extLocationInfo) {

        public constructor(locationInfo: String, namespaceUri: String, localName: String, prefix: String) :
                this(locationInfo.let(XmlReader::StringLocationInfo), namespaceUri, localName, prefix)

        public fun isEqualNames(ev: NamedEvent): Boolean {
            return namespaceUri == ev.namespaceUri &&
                    localName == ev.localName &&
                    prefix == ev.prefix
        }

        public val name: QName get() = QName(namespaceUri, localName, prefix)

        override fun toString(): String {
            return "$eventType - {$namespaceUri}$prefix:$localName (${extLocationInfo ?: ""})"
        }

    }

    public class StartElementEvent(
        extLocationInfo: XmlReader.LocationInfo?,
        namespaceUri: String,
        localName: String,
        prefix: String,
        public val attributes: Array<out Attribute>,
        private val parentNamespaceContext: IterableNamespaceContext,
        namespaceDecls: List<Namespace>
    ) : NamedEvent(extLocationInfo, namespaceUri, localName, prefix) {

        public constructor(
            locationInfo: String,
            namespaceUri: String,
            localName: String,
            prefix: String,
            attributes: Array<out Attribute>,
            parentNamespaceContext: IterableNamespaceContext,
            namespaceDecls: List<Namespace>
        ) : this(
            locationInfo.let(XmlReader::StringLocationInfo),
            namespaceUri,
            localName,
            prefix,
            attributes,
            parentNamespaceContext,
            namespaceDecls
        )

        private val namespaceHolder: SimpleNamespaceContext = SimpleNamespaceContext(namespaceDecls)

        public constructor(
            name: QName,
            parentNamespaceContext: IterableNamespaceContext,
            extLocationInfo: XmlReader.LocationInfo? = null
        ) : this(extLocationInfo, name.namespaceURI, name.localPart, name.prefix, emptyArray(), parentNamespaceContext, emptyList())

        public constructor(
            namespaceUri: String,
            localName: String,
            prefix: String,
            parentNamespaceContext: IterableNamespaceContext
        ) : this(extLocationInfo = null, namespaceUri, localName, prefix, emptyArray(), parentNamespaceContext, emptyList())

        override fun writeTo(writer: XmlWriter) {
            writer.startTag(namespaceUri, localName, prefix)

            attributes.forEach { attr -> writer.attribute(attr.namespaceUri, attr.localName, attr.prefix, attr.value) }

            namespaceHolder.forEach { ns -> writer.namespaceAttr(ns.prefix, ns.namespaceURI) }
        }

        public val namespaceDecls: Iterable<Namespace>
            get() = namespaceHolder

        override val eventType: EventType get() = EventType.START_ELEMENT

        internal fun getPrefix(namespaceURI: String): String? {
            return namespaceHolder.getPrefix(namespaceURI)
                ?: parentNamespaceContext.getPrefix(namespaceUri)
        }

        internal fun getNamespaceURI(prefix: String): String? {
            return when (val decl = namespaceHolder.getNamespaceURI(prefix)) {
                null -> parentNamespaceContext.getNamespaceURI(prefix)
                else -> decl
            }
        }

        public val namespaceContext: IterableNamespaceContext
            get() = namespaceHolder + parentNamespaceContext

        internal fun getPrefixes(namespaceURI: String): Iterator<String> {
            return (namespaceHolder.getPrefixes(namespaceURI).asSequence() +
                    parentNamespaceContext.getPrefixes(namespaceURI).asSequence()
                    ).iterator()
        }

        override fun toString(): String {
            return "$eventType - {$namespaceUri}$prefix:$localName (${extLocationInfo ?: ""})" +
                    attributes.joinToString(
                        "\n    ",
                        if (attributes.isNotEmpty()) "\n    " else ""
                    ) { "${it.localName} = ${it.value} " }
        }

    }

    public class Attribute(
        extLocationInfo: XmlReader.LocationInfo?,
        namespaceUri: CharSequence,
        localName: CharSequence,
        prefix: CharSequence,
        value: CharSequence
    ) : XmlEvent(extLocationInfo) {

        public constructor(
            locationInfo: String,
            namespaceUri: CharSequence,
            localName: CharSequence,
            prefix: CharSequence,
            value: CharSequence
        ) : this(locationInfo.let(XmlReader::StringLocationInfo), namespaceUri, localName, prefix, value)

        public constructor(
            namespaceUri: CharSequence,
            localName: CharSequence,
            prefix: CharSequence,
            value: CharSequence
        ) : this(null, namespaceUri, localName, prefix, value)

        public val value: String = value.toString()
        public val prefix: String = prefix.toString()
        public val localName: String = localName.toString()
        public val namespaceUri: String = namespaceUri.toString()
        override val eventType: EventType get() = EventType.ATTRIBUTE

        public val name: QName get() = QName(namespaceUri, localName, prefix)

        override fun writeTo(writer: XmlWriter) {
            if (hasNamespaceUri()) {
                val nsPrefix = if (prefix.isEmpty()) "" else localName
                writer.namespaceAttr(nsPrefix, namespaceUri)
            } else {
                writer.attribute(namespaceUri, localName, prefix, value)
            }
        }

        public fun hasNamespaceUri(): Boolean {
            return XMLConstants.XMLNS_ATTRIBUTE_NS_URI == namespaceUri ||
                    (prefix.isEmpty() && XMLConstants.XMLNS_ATTRIBUTE == localName)
        }

        override fun toString(): String = when {
            namespaceUri.isBlank() -> "$localName=\"$value\""
            prefix.isBlank() -> "{$namespaceUri}$localName=\"$value\""
            else -> "{$namespaceUri}$prefix:$localName=\"$value\""
        }

        @Suppress("DuplicatedCode")
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Attribute

            if (value != other.value) return false
            if (prefix != other.prefix) return false
            if (localName != other.localName) return false
            if (namespaceUri != other.namespaceUri) return false

            return true
        }

        override fun hashCode(): Int {
            var result = value.hashCode()
            result = 31 * result + prefix.hashCode()
            result = 31 * result + localName.hashCode()
            result = 31 * result + namespaceUri.hashCode()
            return result
        }
    }

    public class NamespaceImpl public constructor(namespacePrefix: String, namespaceUri: String) : Namespace {

        override val prefix: String = namespacePrefix
        override val namespaceURI: String = namespaceUri

        public constructor(namespacePrefix: CharSequence, namespaceUri: CharSequence) :
                this(namespacePrefix.toString(), namespaceUri.toString())

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Namespace) return false

            if (prefix != other.prefix) return false
            if (namespaceURI != other.namespaceURI) return false

            return true
        }

        override fun hashCode(): Int {
            return 31 * prefix.hashCode() + namespaceURI.hashCode()
        }

        override fun toString(): String {
            return "{$prefix:$namespaceURI}"
        }

    }

    public abstract val eventType: EventType

    public open val isIgnorable: Boolean get() = eventType.isIgnorable

    public abstract fun writeTo(writer: XmlWriter)

}
