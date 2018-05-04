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
@file:JvmName("JVMXmlStreamingKt")
package nl.adaptivity.xml

import nl.adaptivity.xml.XmlStreaming.deSerialize
import java.io.*
import java.util.*
import javax.xml.transform.Result
import javax.xml.transform.Source


/**
 * Utility class with factories and constants for the [XmlReader] and [XmlWriter] interfaces.
 * Created by pdvrieze on 15/11/15.
 */
actual object XmlStreaming {


    private val serviceLoader: ServiceLoader<XmlStreamingFactory> by lazy {
        val service = XmlStreamingFactory::class.java
        ServiceLoader.load(service, service.classLoader)
    }

    @Suppress("ObjectPropertyName")
    private var _factory: XmlStreamingFactory? = StAXStreamingFactory()

    private val factory: XmlStreamingFactory
        get() {
            return _factory ?: serviceLoader.first().apply { _factory = this }
        }

    @JvmStatic
    @JvmOverloads
    fun newWriter(result: Result, repairNamespaces: Boolean = false): XmlWriter {
        return factory.newWriter(result, repairNamespaces)
    }

    @JvmOverloads
    @JvmStatic
    fun newWriter(outputStream: OutputStream, encoding: String, repairNamespaces: Boolean = false): XmlWriter {
        return factory.newWriter(outputStream, encoding, repairNamespaces)
    }

    @JvmOverloads
    @JvmStatic
    fun newWriter(writer: Writer, repairNamespaces: Boolean = false): XmlWriter {
        return factory.newWriter(writer, repairNamespaces)
    }

    @JvmStatic
    fun newReader(inputStream: InputStream, encoding: String): XmlReader {
        return factory.newReader(inputStream, encoding)
    }

    @JvmStatic
    fun newReader(reader: Reader): XmlReader {
        return factory.newReader(reader)
    }

    @JvmStatic
    fun newReader(source: Source): XmlReader {
        return factory.newReader(source)
    }

    @JvmStatic
    actual fun setFactory(factory: XmlStreamingFactory?) {
        _factory = factory
    }

    /*
      @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.START_DOCUMENT", "XmlStreaming.EventType"))
      val START_DOCUMENT: EventType = EventType.START_DOCUMENT
      @JvmField@Deprecated("Don't use it", ReplaceWith("EventType.START_ELEMENT", "XmlStreaming.EventType"))
      val START_ELEMENT : EventType = EventType.START_ELEMENT
      @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.END_ELEMENT", "XmlStreaming.EventType"))
      val END_ELEMENT : EventType = EventType.END_ELEMENT
      @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.COMMENT", "XmlStreaming.EventType"))
      val COMMENT : EventType = EventType.COMMENT
      @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.CDSECT", "XmlStreaming.EventType"))
      val CDSECT : EventType = EventType.CDSECT
      @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.DOCDECL", "XmlStreaming.EventType"))
      val DOCDECL : EventType = EventType.DOCDECL
      @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.ATTRIBUTE", "XmlStreaming.EventType"))
      val ATTRIBUTE : EventType = EventType.ATTRIBUTE
      @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.END_DOCUMENT", "XmlStreaming.EventType"))
      val END_DOCUMENT : EventType = EventType.END_DOCUMENT
      @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.ENTITY_REF", "XmlStreaming.EventType"))
      val ENTITY_REF : EventType = EventType.ENTITY_REF
      @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.IGNORABLE_WHITESPACE", "XmlStreaming.EventType"))
      val IGNORABLE_WHITESPACE : EventType = EventType.IGNORABLE_WHITESPACE
      @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.PROCESSING_INSTRUCTION", "XmlStreaming.EventType"))
      val PROCESSING_INSTRUCTION : EventType = EventType.PROCESSING_INSTRUCTION

      @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.CDSECT", "XmlStreaming.EventType"))
      val CDATA = EventType.CDSECT

      @Deprecated("Don't use it", ReplaceWith("EventType.TEXT", "XmlStreaming.EventType"))
      @JvmField val TEXT = EventType.TEXT
      @Deprecated("Don't use it", ReplaceWith("EventType.TEXT", "XmlStreaming.EventType"))
      @JvmField val CHARACTERS = EventType.TEXT
    */
    @JvmStatic
    fun <T> deSerialize(input: InputStream, type: Class<T>): T {
        return XmlStreaming.newReader(input, "UTF-8").deSerialize(type)
    }

    @JvmStatic
    fun <T> deSerialize(input: Reader, type: Class<T>): T {
        return XmlStreaming.newReader(input).deSerialize(type)
    }

    @JvmStatic
    fun <T> deSerialize(input: String, type: Class<T>): T {
        return XmlStreaming.newReader(StringReader(input)).deSerialize(type)
    }

    @JvmStatic
    fun <T> deSerialize(inputs: Iterable<String>, type: Class<T>): List<T> {
        return inputs.map { input -> XmlStreaming.newReader(StringReader(input)).deSerialize(type) }
    }

    actual inline fun <reified T:Any> deSerialize(input:String): T {
        return deSerialize(input, T::class.java)
    }

    @JvmStatic
    fun <T> deSerialize(reader: Source, type: Class<T>): T {
        return XmlStreaming.newReader(reader).deSerialize(type)
    }

    @JvmStatic
    fun toCharArray(content: Source): CharArray {
        return XmlStreaming.newReader(content).toCharArrayWriter().toCharArray()
    }

    @JvmStatic
    fun toString(source: Source): String {
        return XmlStreaming.newReader(source).toCharArrayWriter().toString()
    }

    actual fun toString(value: XmlSerializable): String {
        return StringWriter().apply {
            val w = XmlStreaming.newWriter(this@apply)
            try {
                value.serialize(w)
            } finally {
                w.close()
            }
        }.toString()
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
