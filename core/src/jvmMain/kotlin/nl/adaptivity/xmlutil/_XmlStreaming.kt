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
@file:JvmName("JVMXmlStreamingKt")

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.XmlStreaming.deSerialize
import nl.adaptivity.xmlutil.core.impl.XmlStreamingJavaCommon
import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.util.*
import javax.xml.transform.Result
import javax.xml.transform.Source


actual object XmlStreaming : XmlStreamingJavaCommon() {

    private val serviceLoader: ServiceLoader<XmlStreamingFactory> by lazy {
        val service = XmlStreamingFactory::class.java
        ServiceLoader.load(service, service.classLoader)
    }

    @Suppress("ObjectPropertyName")
    private var _factory: XmlStreamingFactory? = StAXStreamingFactory()

    private val factory: XmlStreamingFactory
        get() = _factory ?: serviceLoader.first().apply { _factory = this }

    override fun newWriter(result: Result, repairNamespaces: Boolean): XmlWriter {
        return factory.newWriter(result, repairNamespaces)
    }

    override fun newWriter(outputStream: OutputStream, encoding: String, repairNamespaces: Boolean): XmlWriter {
        return factory.newWriter(outputStream, encoding, repairNamespaces)
    }

    actual override fun newWriter(
        writer: java.io.Writer,
        repairNamespaces: Boolean,
        xmlDeclMode: XmlDeclMode
                                 ): XmlWriter {
        return factory.newWriter(writer, repairNamespaces = repairNamespaces, xmlDeclMode = xmlDeclMode)
    }

    actual override fun newWriter(output: Appendable, repairNamespaces: Boolean, xmlDeclMode: XmlDeclMode): XmlWriter {
        return factory.newWriter(output, repairNamespaces, xmlDeclMode)
    }

    override fun newReader(inputStream: InputStream, encoding: String): XmlReader {
        return factory.newReader(inputStream, encoding)
    }

    override fun newReader(reader: Reader): XmlReader {
        return factory.newReader(reader)
    }

    @Deprecated("Note that sources are inefficient and poorly designed, relying on runtime types")
    override fun newReader(source: Source): XmlReader {
        return factory.newReader(source)
    }

    actual override fun newReader(input: CharSequence): XmlReader {
        return factory.newReader(input)
    }

    override fun newReader(inputStr: String): XmlReader {
        return factory.newReader(inputStr)
    }

    actual override fun setFactory(factory: XmlStreamingFactory?) {
        _factory = factory ?: StAXStreamingFactory()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Note that sources are inefficient and poorly designed, relying on runtime types")
    override fun toCharArray(content: Source): CharArray {
        return newReader(content).toCharArrayWriter().toCharArray()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Note that sources are inefficient and poorly designed, relying on runtime types")
    override fun toString(source: Source): String {
        return newReader(source).toCharArrayWriter().toString()
    }

}


inline fun <reified T : Any> deserialize(input: InputStream) = deSerialize(input, T::class.java)

inline fun <reified T : Any> deserialize(input: Reader) = deSerialize(input, T::class.java)

inline fun <reified T : Any> deserialize(input: String) = deSerialize(input, T::class.java)


@JvmField
@Deprecated("Don't use it", ReplaceWith("EventType.START_DOCUMENT", "XmlStreaming.EventType"))
val START_DOCUMENT: EventType = EventType.START_DOCUMENT
@JvmField
@Deprecated("Don't use it", ReplaceWith("EventType.START_ELEMENT", "XmlStreaming.EventType"))
val START_ELEMENT: EventType = EventType.START_ELEMENT
@JvmField
@Deprecated("Don't use it", ReplaceWith("EventType.END_ELEMENT", "XmlStreaming.EventType"))
val END_ELEMENT: EventType = EventType.END_ELEMENT
@JvmField
@Deprecated("Don't use it", ReplaceWith("EventType.COMMENT", "XmlStreaming.EventType"))
val COMMENT: EventType = EventType.COMMENT
@JvmField
@Deprecated("Don't use it", ReplaceWith("EventType.CDSECT", "XmlStreaming.EventType"))
val CDSECT: EventType = EventType.CDSECT
@JvmField
@Deprecated("Don't use it", ReplaceWith("EventType.DOCDECL", "XmlStreaming.EventType"))
val DOCDECL: EventType = EventType.DOCDECL
@JvmField
@Deprecated("Don't use it", ReplaceWith("EventType.ATTRIBUTE", "XmlStreaming.EventType"))
val ATTRIBUTE: EventType = EventType.ATTRIBUTE
@JvmField
@Deprecated("Don't use it", ReplaceWith("EventType.END_DOCUMENT", "XmlStreaming.EventType"))
val END_DOCUMENT: EventType = EventType.END_DOCUMENT
@JvmField
@Deprecated("Don't use it", ReplaceWith("EventType.ENTITY_REF", "XmlStreaming.EventType"))
val ENTITY_REF: EventType = EventType.ENTITY_REF
@JvmField
@Deprecated("Don't use it", ReplaceWith("EventType.IGNORABLE_WHITESPACE", "XmlStreaming.EventType"))
val IGNORABLE_WHITESPACE: EventType = EventType.IGNORABLE_WHITESPACE
@JvmField
@Deprecated("Don't use it", ReplaceWith("EventType.PROCESSING_INSTRUCTION", "XmlStreaming.EventType"))
val PROCESSING_INSTRUCTION: EventType = EventType.PROCESSING_INSTRUCTION

@JvmField
@Deprecated("Don't use it", ReplaceWith("EventType.CDSECT", "XmlStreaming.EventType"))
val CDATA = EventType.CDSECT

@Deprecated("Don't use it", ReplaceWith("EventType.TEXT", "XmlStreaming.EventType"))
@JvmField
val TEXT = EventType.TEXT
@Deprecated("Don't use it", ReplaceWith("EventType.TEXT", "XmlStreaming.EventType"))
@JvmField
val CHARACTERS = EventType.TEXT
