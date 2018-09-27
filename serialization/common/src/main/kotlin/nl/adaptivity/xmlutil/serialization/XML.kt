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

@file:Suppress("KDocUnresolvedReference")

package nl.adaptivity.xmlutil.serialization

import kotlinx.io.StringWriter
import kotlinx.serialization.*
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.multiplatform.name
import nl.adaptivity.xmlutil.serialization.canary.*
import nl.adaptivity.xmlutil.serialization.compat.DecoderInput
import nl.adaptivity.xmlutil.serialization.compat.DummyParentDescriptor
import nl.adaptivity.xmlutil.serialization.compat.EncoderOutput
import nl.adaptivity.xmlutil.serialization.compat.SerialDescriptor
import nl.adaptivity.xmlutil.util.CompactFragment
import kotlin.reflect.KClass

internal data class NameHolder(val name: QName, val specified: Boolean)

internal class XmlNameMap {
    private val classMap = mutableMapOf<QName, String>()
    private val nameMap = mutableMapOf<String, NameHolder>()

    fun lookupName(kClass: KClass<*>) = lookupName(kClass.name)
    fun lookupClass(name: QName) = classMap[name.copy(prefix = "")]
    fun lookupName(kClass: String) = nameMap[kClass]

    fun registerClass(kClass: KClass<*>) {
        val serialInfo = kClass.serializer().serialClassDesc
        val serialName = serialInfo.getAnnotationsForClass().getXmlSerialName()

        val name: QName
        val specified: Boolean
        if (serialName == null) {
            specified = false
            name = QName(kClass.name.substringAfterLast('.'))
        } else {
            specified = true
            name = serialName
        }
        registerClass(name, kClass.name, specified)
    }

    fun registerClass(name: QName, kClass: String, specified: Boolean) {
        classMap[name.copy(prefix = "")] = kClass
        nameMap[kClass] = NameHolder(name, specified)
    }
}

@Suppress("MemberVisibilityCanBePrivate")
/**
 * Class that provides access to XML parsing and serialization for the Kotlin Serialization system. In most cases the
 * companion functions would be used. Creating an object explicitly however allows for the serialization to be configured.
 *
 * **Note** that at this point not all configuration options will work on all platforms.
 *
 * @property context The serialization context used to resolve serializers etc.
 * @property repairNamespaces Option for the serializer whether it should repair namespaces. Does not affect reading
 * @property omitXmlDecl When writing do not emit a `<?xml ... ?>` processing instruction. This is passed to the
 *                       [XmlWriter] constructor
 * @property indent The indentation to use when writing XML
 */
class XML(val context: SerialContext? = defaultSerialContext(),
          val repairNamespaces: Boolean = true,
          val omitXmlDecl: Boolean = true,
          var indent: Int = 0) {

    /**
     * Transform the object into an XML String. This is a shortcut for the non-reified version that takes a
     * KClass parameter
     */
    @Suppress("unused")
    inline fun <reified T : Any> stringify(obj: T, prefix: String? = null): String =
            stringify(T::class, obj, context.klassSerializer(T::class), prefix)

    /**
     * Transform into a string. This function is expected to be called indirectly.
     *
     * @param kClass The type of the object being serialized
     * @param obj The actual object
     * @param saver The serializer/saver to use to write
     * @param prefix The prefix (if any) to use for the namespace
     */
    fun <T : Any> stringify(kClass: KClass<T>,
                            obj: T,
                            saver: KSerialSaver<T> = context.klassSerializer(kClass),
                            prefix: String? = null): String {
        val stringWriter = StringWriter()
        val xmlWriter = XmlStreaming.newWriter(stringWriter, repairNamespaces, omitXmlDecl)

        var ex: Exception? = null
        try {
            toXml(xmlWriter, kClass, obj, saver, prefix)
        } catch (e: Exception) {
            ex = e
        } finally {
            try {
                xmlWriter.close()
            } finally {
                ex?.let { throw it }
            }

        }
        return stringWriter.toString()
    }

    @Deprecated("Replaced by version with consistent parameter order", ReplaceWith("toXml(target, obj, prefix)"))
    inline fun <reified T : Any> toXml(obj: T, target: XmlWriter, prefix: String? = null) =
            toXml(target, obj, prefix)

    /**
     * Write the object to the given writer
     *
     * @param obj The actual object
     * @param target The [XmlWriter] to append the object to
     * @param prefix The prefix (if any) to use for the namespace
     */
    inline fun <reified T : Any> toXml(target: XmlWriter, obj: T, prefix: String? = null) {
        toXml(target, T::class, obj, context.klassSerializer(T::class), prefix)
    }

    @Deprecated("Replaced by version with consistent parameter order",
                ReplaceWith("toXml(target, kClass, obj, serializer, prefix)"))
    fun <T : Any> toXml(kClass: KClass<T>,
                        obj: T,
                        target: XmlWriter,
                        prefix: String? = null,
                        serializer: KSerialSaver<T> = context.klassSerializer(kClass)) =
            toXml(target, kClass, obj, serializer, prefix)

    /**
     * Transform into a string. This function is expected to be called indirectly.
     *
     * @param kClass The type of the object being serialized
     * @param obj The actual object
     * @param target The [XmlWriter] to append the object to
     * @param saver The serializer/saver to use to write
     * @param prefix The prefix (if any) to use for the namespace
     */
    fun <T : Any> toXml(target: XmlWriter,
                        kClass: KClass<T>,
                        obj: T,
                        saver: KSerialSaver<T> = context.klassSerializer(kClass),
                        prefix: String? = null) {
        target.indent = indent

        val serialDescriptor = Canary.serialDescriptor(saver, obj)

        val serialName = kClass.getSerialName(saver as? KSerializer<*>, prefix)
        val encoder = XmlEncoderBase(context, target).XmlEncoder(DummyParentDescriptor(serialName, serialDescriptor), 0)

        val output = EncoderOutput(encoder, serialDescriptor)

        output.write(saver, obj)
    }

    /**
     * Parse an object of the type [T] out of the reader
     */
    @Suppress("unused")
    inline fun <reified T : Any> parse(reader: XmlReader) = parse(reader, T::class)

    @Deprecated("Replaced by version with consistent parameter order", ReplaceWith("parse(reader, kClass, loader)"))
    fun <T : Any> parse(kClass: KClass<T>,
                        reader: XmlReader,
                        loader: KSerialLoader<T> = context.klassSerializer(kClass)): T =
            parse(reader, kClass, loader)

    /**
     * Parse an object of the type [T] out of the reader. This function is intended mostly to be used indirectly where
     * though the reified function. The loader defaults to the loader for [kClass].
     *
     * @param kClass The actual class object to parse the object from.
     * @param reader An [XmlReader] that contains the XML from which to read the object
     * @param loader The loader to use to read the object
     */
    fun <T : Any> parse(reader: XmlReader,
                        kClass: KClass<T>,
                        loader: KSerialLoader<T> = context.klassSerializer(kClass)): T {

        val serialName = kClass.getSerialName(loader as? KSerializer<*>)
        val serialDescriptor = Canary.serialDescriptor(loader)

        val decoder = XmlDecoderBase(context, reader).XmlDecoder(DummyParentDescriptor(serialName, serialDescriptor), 0)
        val input = DecoderInput(decoder, serialDescriptor)

        // We skip all ignorable content here. To get started while supporting direct content we need to put the parser
        // in the correct state of having just read the startTag (that would normally be read by the code that determines
        // what to parse (before calling readSerializableValue on the value)
        reader.skipPreamble()
        return input.readSerializableValue(loader)
    }

    /**
     * Parse an object of the type [T] out of the string. It merely creates an xml reader and forwards the request.
     * This function is intended mostly to be used indirectly where
     * though the reified function. The loader defaults to the loader for [kClass]
     *
     * @param kClass The actual class object to parse the object from.
     * @param str The string that contains the XML from which to read the object
     * @param loader The loader to use to read the object
     */
    fun <T : Any> parse(kClass: KClass<T>, str: String, loader: KSerialLoader<T> = context.klassSerializer(kClass)): T {
        return parse(XmlStreaming.newReader(str), kClass, loader)
    }

    companion object {
        /**
         * Transform the object into an XML string. This requires the object to be serializable by the kotlin
         * serialization library (either it has a built-in serializer or it is [kotlinx.serialization.Serializable].
         * @param obj The object to transform
         * @param kClass The class where to get the serializer from
         * @param prefix The namespace prefix to use
         */
        @Suppress("UNCHECKED_CAST")
        fun <T : Any> stringify(obj: T, kClass: KClass<T> = obj::class as KClass<T>, prefix: String? = null): String =
                XML().run { stringify(kClass, obj, context.klassSerializer(kClass), prefix) }

        /**
         * Transform the object into an XML string. This requires the object to be serializable by the kotlin
         * serialization library (either it has a built-in serializer or it is [kotlinx.serialization.Serializable].
         * @param obj The object to transform
         * @param prefix The namespace prefix to use
         */
        inline fun <reified T : Any> stringify(obj: T, prefix: String? = null): String = stringify(obj, T::class,
                                                                                                   prefix)

        /**
         * Transform into a string. This function is expected to be called indirectly.
         *
         * @param kClass The type of the object being serialized
         * @param obj The actual object
         * @param dest The [XmlWriter] to append the object to
         * @param prefix The prefix (if any) to use for the namespace
         */
        @Suppress("UNCHECKED_CAST")
        fun <T : Any> toXml(dest: XmlWriter,
                            obj: T,
                            kClass: KClass<T> = obj::class as KClass<T>,
                            prefix: String? = null) =
                XML().run { toXml(dest, kClass, obj, context.klassSerializer(kClass), prefix) }

        /**
         * Write the object to the given writer
         *
         * @param obj The actual object
         * @param dest The [XmlWriter] to append the object to
         * @param prefix The prefix (if any) to use for the namespace
         */
        @Suppress("unused")
        inline fun <reified T : Any> toXml(dest: XmlWriter, obj: T, prefix: String?) = toXml(dest, obj, T::class,
                                                                                             prefix)

        /**
         * Parse an object of the type [T] out of the reader
         * @param str The source of the XML events
         * @param kClass The class to parse. Used for class annotations.
         */
        fun <T : Any> parse(kClass: KClass<T>, str: String): T = XML().parse(kClass, str)

        @Deprecated("Replaced by version with consistent parameter order",
                    ReplaceWith("parse(str, kClass, loader)"))
        fun <T : Any> parse(kClass: KClass<T>, str: String, loader: KSerialLoader<T>): T =
                parse(str, kClass, loader)

        /**
         * Parse an object of the type [T] out of the reader
         * @param str The source of the XML events
         * @param kClass The class to parse. Used for class annotations.
         * @param loader The loader to use (rather than the default)
         */
        fun <T : Any> parse(str: String, kClass: KClass<T>, loader: KSerialLoader<T>): T =
                XML().parse(kClass, str, loader)

        /**
         * Parse an object of the type [T] out of the reader
         * @param str The source of the XML events
         */
        inline fun <reified T : Any> parse(str: String): T = parse(T::class, str)

        /**
         * Parse an object of the type [T] out of the reader
         * @param str The source of the XML events
         * @param loader The loader to use (rather than the default)
         */
        @Suppress("unused")
        inline fun <reified T : Any> parse(str: String, loader: KSerialLoader<T>): T = parse(str, T::class, loader)

        @Deprecated("Replaced by version with consistent parameter order",
                    ReplaceWith("parse(reader, kClass)"))
        fun <T : Any> parse(kClass: KClass<T>, reader: XmlReader): T =
                parse(reader, kClass)

        /**
         * Parse an object of the type [T] out of the reader
         * @param reader The source of the XML events
         * @param kClass The class to parse. Used for class annotations.
         */
        fun <T : Any> parse(reader: XmlReader, kClass: KClass<T>): T =
                XML().parse(reader, kClass)

        @Deprecated("Replaced by version with consistent parameter order",
                    ReplaceWith("parse(reader, kClass, loader)"))
        fun <T : Any> parse(kClass: KClass<T>, reader: XmlReader, loader: KSerialLoader<T>): T =
                parse(reader, kClass, loader)

        /**
         * Parse an object of the type [T] out of the reader
         * @param reader The source of the XML events
         * @param kClass The class to parse. Used for class annotations.
         * @param loader The loader to use (rather than the default)
         */
        fun <T : Any> parse(reader: XmlReader, kClass: KClass<T>, loader: KSerialLoader<T>): T =
                XML().parse(reader, kClass, loader)

        /**
         * Parse an object of the type [T] out of the reader
         * @param reader The source of the XML events
         */
        inline fun <reified T : Any> parse(reader: XmlReader): T = parse(reader, T::class)

        /**
         * Parse an object of the type [T] out of the reader
         * @param reader The source of the XML events
         * @param loader The loader to use (rather than the default)
         */
        @Suppress("unused")
        inline fun <reified T : Any> parse(reader: XmlReader, loader: KSerialLoader<T>): T =
                parse(reader, T::class, loader)
    }

    /**
     * An interface that allows custom serializers to special case being serialized to XML and retrieve the underlying
     * [XmlWriter]. This is used for example by [CompactFragment] to make the fragment transparent when serializing to
     * XML.
     */
    interface XmlOutput {
        /**
         * The name for the current tag
         */
        val serialName: QName

        @Deprecated("It is not used or tested. Candidate for removal")
        @Suppress("unused")
         /**
         * The currently active serialization context
         */
        val context: SerialContext?
        /**
         * The XmlWriter used. Can be used directly by serializers
         */
        val target: XmlWriter

        @Deprecated("Not used will always return null", ReplaceWith("null"))
        val currentTypeName: Nothing?
            get() = null
    }

    /**
     * An interface that allows custom serializers to special case being serialized to XML and retrieve the underlying
     * [XmlReader]. This is used for example by [CompactFragment] to read arbitrary XML from the stream and store it inside
     * the buffer (without attempting to use the serializer/decoder for it.
     */
    interface XmlInput {
        val input: XmlReader
    }

}

private fun defaultSerialContext() = SerialContext().apply {
    //    registerSerializer(ICompactFragment::class, ICompactFragmentSerializer())
    registerSerializer(CompactFragment::class, CompactFragmentSerializer)
}

private fun Collection<Annotation>.getXmlSerialName(): QName? {
    val serialName = firstOrNull<XmlSerialName>()
    return when {
        serialName == null -> null
        serialName.namespace == UNSET_ANNOTATION_VALUE
                           -> QName(serialName.value)

        serialName.prefix == UNSET_ANNOTATION_VALUE
                           -> QName(serialName.namespace, serialName.value)

        else               -> QName(serialName.namespace, serialName.value, serialName.prefix)
    }
}

internal fun Collection<Annotation>.getChildName(): QName? {
    val childrenName = firstOrNull<XmlChildrenName>()
    return when {
        childrenName == null -> null
        childrenName.namespace == UNSET_ANNOTATION_VALUE
                             -> QName(childrenName.value)

        childrenName.prefix == UNSET_ANNOTATION_VALUE
                             -> QName(childrenName.namespace, childrenName.value)

        else                 -> QName(childrenName.namespace, childrenName.value,
                                      childrenName.prefix)
    }
}

internal fun <T : Any> KClass<T>.getSerialName(serializer: KSerializer<*>?, prefix: String?): QName {
    return getSerialName(serializer).let {
        if (prefix == null) it else it.copy(prefix = prefix)
    }
}

internal fun <T : Any> KClass<T>.getSerialName(serializer: KSerializer<*>?): QName {
    return myAnnotations.getXmlSerialName()
           ?: serializer?.run { QName(serialClassDesc.name.substringAfterLast('.')) }
           ?: QName(name.substringAfterLast('.'))
}

internal enum class OutputKind { Element, Attribute, Text; }

internal fun XmlSerialName.toQName() = QName(namespace, value, prefix)

internal fun XmlChildrenName.toQName() = QName(namespace, value, prefix)

internal data class PolyInfo(val kClass: String, val tagName: QName, val index: Int)

internal inline fun <reified T> Iterable<*>.firstOrNull(): T? {
    for (e in this) {
        if (e is T) return e
    }
    return null
}


internal fun SerialDescriptor.getValueChild(): Int {
    if (elementsCount == 1) {
        return 0
    } else {
        for (i in 0 until elementsCount) {
            if (getElementAnnotations(i).any { it is XmlValue }) return i
        }
        return KInput.UNKNOWN_NAME
    }
}

/** Straightforward copy function */
fun QName.copy(namespaceURI: String = this.namespaceURI,
               localPart: String = this.localPart,
               prefix: String = this.prefix) =
        QName(namespaceURI, localPart, prefix)

/** Shortcircuit copy function that creates a new version (if needed) with the new prefix only */
internal fun QName.copy(prefix: String = this.prefix) = when (prefix) {
    this.prefix -> this
    else        -> QName(namespaceURI, localPart, prefix)
}

/**
 * Extension function that allows any (serializable) object to be written to an XmlWriter.
 */
inline fun <reified T : Any> T.writeAsXML(out: XmlWriter) = XML().toXml(out, this)

/**
 * Extension function that allows any (serializable) object to be written to an XmlWriter. This version takes a [KClass]
 * object rather than having a generic specification.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <T : Any> T.writeAsXML(kClass: KClass<T>, out: XmlWriter) =
        XML().toXml(out, kClass, obj = this)
