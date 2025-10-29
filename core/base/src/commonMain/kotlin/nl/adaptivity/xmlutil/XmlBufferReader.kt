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

import nl.adaptivity.xmlutil.XmlEvent.*
import nl.adaptivity.xmlutil.core.impl.NamespaceHolder


/**
 * A reader that presents a list of events as an xml reader. This is designed to work together with
 * [XmlBufferedWriter].
 */
public class XmlBufferReader private constructor(
    private val buffer: List<XmlEvent>,
    private val namespaceHolder: NamespaceHolder
) : XmlReader {
    private var currentPos = -1

    public constructor(buffer: List<XmlEvent>): this(buffer, NamespaceHolder())
    public constructor(buffer: List<XmlEvent>, initialContext: IterableNamespaceContext): this(
        buffer,
        NamespaceHolder().also {
            it.addPrefixesToContext(initialContext)
        }
    )

    override val eventType: EventType get() = buffer[currentPos].eventType

    override val depth: Int get() = namespaceHolder.depth

    override val namespaceURI: String
        get() = current<NamedEvent>().namespaceUri

    override val localName: String
        get() = current<NamedEvent>().localName

    override val prefix: String
        get() = current<NamedEvent>().prefix

    override val isStarted: Boolean
        get() = currentPos >= 0

    override val text: String
        get() = current<TextEvent>().text

    override val piTarget: String
        get() = current<ProcessingInstructionEvent>().target

    override val piData: String
        get() = current<ProcessingInstructionEvent>().data


    override val attributeCount: Int
        get() = current<StartElementEvent>().attributes.size

    override val namespaceDecls: List<Namespace>
        get() = current<StartElementEvent>().namespaceDecls.let { it as? List<Namespace> ?: it.toList() }

    override val extLocationInfo: XmlReader.LocationInfo?
        get() = buffer[currentPos].extLocationInfo

    override val namespaceContext: IterableNamespaceContext
        get() = namespaceHolder.namespaceContext

    override var encoding: String? = null
        private set

    override var standalone: Boolean? = null
        private set

    override var version: String? = null
        private set

    init {
        var p = 0
        while (p < buffer.size && buffer[p].eventType == EventType.IGNORABLE_WHITESPACE) {
            ++p
        }
        if (p < buffer.size && buffer[p].eventType == EventType.START_DOCUMENT) {
            val d = buffer[p] as StartDocumentEvent
            encoding = d.encoding
            version = d.version
            standalone = d.standalone
        }
    }

    /**
     * Reset the reader to the start
     */
    public fun reset() {
        currentPos = -1
        namespaceHolder.clear()
    }

    override fun hasNext(): Boolean = currentPos + 1 < buffer.size

    override fun next(): EventType {
        val buffer = buffer
        var cp = currentPos

        if (cp >= 0 && buffer[cp] is EndElementEvent) namespaceHolder.decDepth()

        cp += 1
        currentPos = cp
        if (cp >= buffer.size) throw NoSuchElementException("Reading beyond the end of the reader")

        val current = buffer[cp]
        when (current) {
            is StartElementEvent -> {
                namespaceHolder.incDepth()
                namespaceHolder.addPrefixesToContext(current.namespaceDecls)
            }

            else -> { // ignore
            }
        }

        return current.eventType
    }

    override fun getAttributeNamespace(index: Int): String {
        return current<StartElementEvent>().attributes[index].namespaceUri
    }

    override fun getAttributePrefix(index: Int): String {
        return current<StartElementEvent>().attributes[index].prefix
    }

    override fun getAttributeLocalName(index: Int): String {
        return current<StartElementEvent>().attributes[index].localName
    }

    override fun getAttributeValue(index: Int): String {
        return current<StartElementEvent>().attributes[index].value
    }

    override fun getAttributeValue(nsUri: String?, localName: String): String? {
        return current<StartElementEvent>().attributes.firstOrNull { a -> (nsUri?.let { it == a.namespaceUri } ?: true && localName == a.localName) }?.value
    }

    override fun getNamespacePrefix(namespaceUri: String): String? {
        return namespaceHolder.getPrefix(namespaceUri)
    }

    override fun getNamespaceURI(prefix: String): String? {
        return namespaceHolder.getNamespaceUri(prefix)
    }

    override fun close() {
        currentPos = buffer.size
    }

    private inline fun <reified T : XmlEvent> current(): T {
        return buffer[currentPos] as T
    }
}
