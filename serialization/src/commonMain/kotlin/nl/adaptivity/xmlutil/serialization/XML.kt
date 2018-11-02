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
import kotlinx.serialization.context.SerialContext
import kotlinx.serialization.context.getOrDefault
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.multiplatform.name
import nl.adaptivity.xmlutil.util.CompactFragment
import kotlin.reflect.KClass

internal data class NameHolder(val name: QName, val specified: Boolean)

internal class XmlNameMap {
    private val classMap = mutableMapOf<QName, String>()
    private val nameMap = mutableMapOf<String, NameHolder>()

    fun lookupName(kClass: KClass<*>) = lookupName(kClass.name)
    fun lookupClass(name: QName) = classMap[name.copy(prefix = "")]
    fun lookupName(kClass: String) = nameMap[kClass]

    @ImplicitReflectionSerializer
    fun registerClass(kClass: KClass<*>) {
        val serialInfo = kClass.serializer().descriptor
        val serialName = serialInfo.getEntityAnnotations().getXmlSerialName()

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
 * The serialization can be configured with various annotations: [XmlSerialName], [XmlChildrenName], [XmlPolyChildren],
 * [XmlDefault], [XmlElement] [XmlValue]. These control the way the content is actually serialized. Those tags that support
 * being set on types (rather than properties) prefer values set on properties (the property can override settings on the
 * type).
 *
 * Serialization normally prefers to store values as attributes. This can be overridden by the [XmlElement] annotation
 * to force a tag child. Similarly [XmlValue] can be used for the child to be marked as textual content of the parent tag
 * - only one such child is allowed.
 *
 * Naming of tags and attributes follows some special rules. In particular attributes will be named based on their use
 * where tags are named based on their type. Both can be overridden by specifying [XmlSerialName] on the property.
 *
 * When names are not specified on types, their class name is used, but the package is normally omitted if it matches the
 * package name of the class that represents the parent. Attributes get their use site name which is either the property
 * name or the name modified through [SerialName]
 *
 * @property context The serialization context used to resolve serializers etc.
 * @property repairNamespaces Option for the serializer whether it should repair namespaces. Does not affect reading
 * @property omitXmlDecl When writing do not emit a `<?xml ... ?>` processing instruction. This is passed to the
 *                       [XmlWriter] constructor
 * @property indent The indentation to use when writing XML
 */
class XML(
    val context: SerialContext = defaultSerialContext(),
    val repairNamespaces: Boolean = true,
    val omitXmlDecl: Boolean = true,
    var indent: Int = 0
         ) {

    /**
     * Transform the object into an XML String. This is a shortcut for the non-reified version that takes a
     * KClass parameter
     */
    @ImplicitReflectionSerializer
    @Suppress("unused")
    inline fun <reified T : Any> stringify(obj: T, prefix: String? = null): String =
        stringify(obj, context.getOrDefault(T::class), prefix)

    /**
     * Transform into a string. This function is expected to be called indirectly.
     *
     * @param kClass The type of the object being serialized
     * @param obj The actual object
     * @param prefix The prefix (if any) to use for the namespace
     */
    @ImplicitReflectionSerializer
    fun <T : Any> stringify(
        kClass: KClass<T>,
        obj: T,
        prefix: String? = null
                           ): String {
        return stringify(obj, context.getOrDefault(kClass), prefix)
    }

    /**
     * Transform into a string. This function is expected to be called indirectly.
     *
     * @param obj The actual object
     * @param saver The serializer/saver to use to write
     * @param prefix The prefix (if any) to use for the namespace
     */
    fun <T : Any> stringify(
        obj: T,
        saver: SerializationStrategy<T>,
        prefix: String? = null
                           ): String {
        val stringWriter = StringWriter()
        val xmlWriter = XmlStreaming.newWriter(stringWriter, repairNamespaces, omitXmlDecl)

        var ex: Exception? = null
        try {
            toXml(xmlWriter, obj, saver, prefix)
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

    @ImplicitReflectionSerializer
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
    @ImplicitReflectionSerializer
    inline fun <reified T : Any> toXml(target: XmlWriter, obj: T, prefix: String? = null) {
        toXml(target, obj, context.getOrDefault(T::class), prefix)
    }

    @ImplicitReflectionSerializer
    @Deprecated(
        "Replaced by version with consistent parameter order",
        ReplaceWith("toXml(target, kClass, obj, serializer, prefix)")
               )
    fun <T : Any> toXml(
        kClass: KClass<T>,
        obj: T,
        target: XmlWriter,
        prefix: String? = null,
        serializer: SerializationStrategy<T> = context.getOrDefault(kClass)
                       ) =
        toXml(target, obj, serializer, prefix)

    /**
     * Transform into a string. This function is expected to be called indirectly.
     *
     * @param kClass The type of the object being serialized
     * @param obj The actual object
     * @param target The [XmlWriter] to append the object to
     * @param saver The serializer/saver to use to write
     * @param prefix The prefix (if any) to use for the namespace
     */
    @ImplicitReflectionSerializer
    fun <T : Any> toXml(
        target: XmlWriter,
        obj: T,
        kClass: KClass<T>,
        prefix: String? = null
                       ) {
        toXml(target, obj, context.getOrDefault(kClass), prefix)
    }

    /**
     * Transform into a string. This function is expected to be called indirectly.
     *
     * @param target The [XmlWriter] to append the object to
     * @param obj The actual object
     * @param saver The serializer/saver to use to write
     * @param prefix The prefix (if any) to use for the namespace
     */
    fun <T : Any> toXml(
        target: XmlWriter,
        obj: T,
        saver: SerializationStrategy<T>,
        prefix: String? = null
                       ) {
        target.indent = indent

        val serialDescriptor = saver.descriptor

        val serialName = saver.descriptor.getSerialName(prefix)
        val encoder = XmlEncoderBase(context, target).XmlEncoder(
            DummyParentDescriptor(serialName, serialDescriptor),
            0,
            serialDescriptor
                                                                )

        saver.serialize(encoder, obj)
    }

    /**
     * Parse an object of the type [T] out of the reader
     */
    @ImplicitReflectionSerializer
    @Suppress("unused")
    inline fun <reified T : Any> parse(reader: XmlReader) = parse(reader, T::class)

    @ImplicitReflectionSerializer
    @Deprecated("Replaced by version with consistent parameter order", ReplaceWith("parse(reader, kClass, loader)"))
    fun <T : Any> parse(
        kClass: KClass<T>,
        reader: XmlReader,
        loader: DeserializationStrategy<T> = context.getOrDefault(kClass)
                       ): T =
        parse(reader, loader)

    @ImplicitReflectionSerializer
    fun <T : Any> parse(
        reader: XmlReader,
        kClass: KClass<T>
                       ): T =
        parse(reader, context.getOrDefault(kClass))

    /**
     * Parse an object of the type [T] out of the reader. This function is intended mostly to be used indirectly where
     * though the reified function.
     *
     * @param reader An [XmlReader] that contains the XML from which to read the object
     * @param loader The loader to use to read the object
     */
    fun <T : Any> parse(
        reader: XmlReader,
        loader: DeserializationStrategy<T>
                       ): T {

        val serialName = loader.descriptor.getSerialName()
        val serialDescriptor = loader.descriptor

        val decoder = XmlDecoderBase(context, reader).XmlDecoder(
            DummyParentDescriptor(serialName, serialDescriptor),
            0,
            serialDescriptor
                                                                )

        // We skip all ignorable content here. To get started while supporting direct content we need to put the parser
        // in the correct state of having just read the startTag (that would normally be read by the code that determines
        // what to parse (before calling readSerializableValue on the value)
        reader.skipPreamble()
        return decoder.decodeSerializableValue(loader)
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
    @ImplicitReflectionSerializer
    fun <T : Any> parse(str: String, kClass: KClass<T>): T {
        return parse(XmlStreaming.newReader(str), context.getOrDefault(kClass))
    }

    fun <T : Any> parse(str: String, loader: DeserializationStrategy<T>): T {
        return parse(XmlStreaming.newReader(str), loader)
    }

    companion object {

        /**
         * Transform the object into an XML string. This requires the object to be serializable by the kotlin
         * serialization library (either it has a built-in serializer or it is [kotlinx.serialization.Serializable].
         * @param obj The object to transform
         * @param serializer The serializer to user
         * @param prefix The namespace prefix to use
         */
        fun <T : Any> stringify(
            obj: T,
            serializer: SerializationStrategy<T>,
            prefix: String? = null
                               ): String =
            XML().stringify(obj, serializer, prefix)

        /**
         * Transform the object into an XML string. This requires the object to be serializable by the kotlin
         * serialization library (either it has a built-in serializer or it is [kotlinx.serialization.Serializable].
         * @param obj The object to transform
         * @param kClass The class where to get the serializer from
         * @param prefix The namespace prefix to use
         */
        @ImplicitReflectionSerializer
        @Suppress("UNCHECKED_CAST")
        fun <T : Any> stringify(obj: T, kClass: KClass<T> = obj::class as KClass<T>, prefix: String? = null): String =
            XML().run { this.stringify(kClass, obj, prefix) }

        /**
         * Transform the object into an XML string. This requires the object to be serializable by the kotlin
         * serialization library (either it has a built-in serializer or it is [kotlinx.serialization.Serializable].
         * @param obj The object to transform
         * @param prefix The namespace prefix to use
         */
        @ImplicitReflectionSerializer
        inline fun <reified T : Any> stringify(obj: T, prefix: String? = null): String =
            stringify(obj, T::class, prefix)

        /**
         * Transform into a string. This function is expected to be called indirectly.
         *
         * @param kClass The type of the object being serialized
         * @param obj The actual object
         * @param dest The [XmlWriter] to append the object to
         * @param prefix The prefix (if any) to use for the namespace
         */
        @ImplicitReflectionSerializer
        @Suppress("UNCHECKED_CAST")
        fun <T : Any> toXml(
            dest: XmlWriter,
            obj: T,
            kClass: KClass<T> = obj::class as KClass<T>,
            prefix: String? = null
                           ) =
            XML().run { this.toXml(dest, obj, kClass, prefix) }

        /**
         * Write the object to the given writer
         *
         * @param obj The actual object
         * @param dest The [XmlWriter] to append the object to
         * @param prefix The prefix (if any) to use for the namespace
         */
        @ImplicitReflectionSerializer
        @Suppress("unused")
        inline fun <reified T : Any> toXml(dest: XmlWriter, obj: T, prefix: String?) =
            toXml(dest, obj, T::class, prefix)

        /**
         * Parse an object of the type [T] out of the reader
         * @param str The source of the XML events
         * @param kClass The class to parse. Used for class annotations.
         */
        @ImplicitReflectionSerializer
        fun <T : Any> parse(kClass: KClass<T>, str: String): T = XML().parse(str, kClass)

        @Deprecated(
            "Replaced by version with consistent parameter order",
            ReplaceWith("parse(str, loader)")
                   )
        fun <T : Any> parse(kClass: KClass<T>, str: String, loader: DeserializationStrategy<T>): T =
            parse(str, loader)

        /**
         * Parse an object of the type [T] out of the reader
         * @param str The source of the XML events
         * @param kClass The class to parse. Used for class annotations.
         */
        @ImplicitReflectionSerializer
        fun <T : Any> parse(str: String, kClass: KClass<T>): T =
            XML().parse(str, kClass)

        /**
         * Parse an object of the type [T] out of the reader
         * @param str The source of the XML events
         */
        @ImplicitReflectionSerializer
        inline fun <reified T : Any> parse(str: String): T = parse(T::class, str)

        /**
         * Parse an object of the type [T] out of the reader
         * @param str The source of the XML events
         * @param loader The loader to use
         */
        @Suppress("unused")
        fun <T : Any> parse(str: String, loader: DeserializationStrategy<T>): T = XML().parse(str, loader)

        @ImplicitReflectionSerializer
        @Deprecated(
            "Replaced by version with consistent parameter order",
            ReplaceWith("parse(reader, kClass)")
                   )
        fun <T : Any> parse(kClass: KClass<T>, reader: XmlReader): T =
            parse(reader, kClass)

        /**
         * Parse an object of the type [T] out of the reader
         * @param reader The source of the XML events
         * @param kClass The class to parse. Used for class annotations.
         */
        @ImplicitReflectionSerializer
        fun <T : Any> parse(reader: XmlReader, kClass: KClass<T>): T =
            XML().parse(reader, kClass)

        @Deprecated(
            "Replaced by version with consistent parameter order",
            ReplaceWith("parse(reader, kClass, loader)")
                   )
        fun <T : Any> parse(kClass: KClass<T>, reader: XmlReader, loader: DeserializationStrategy<T>): T =
            parse(reader, kClass, loader)

        /**
         * Parse an object of the type [T] out of the reader
         * @param reader The source of the XML events
         * @param loader The loader to use (rather than the default)
         */
        @Deprecated(
            "Use the version that doesn't take a KClass",
            ReplaceWith("parse(reader, loader)", "nl.adaptivity.xmlutil.serialization.XML.Companion.parse")
                   )
        fun <T : Any> parse(reader: XmlReader, kClass: KClass<T>, loader: DeserializationStrategy<T>): T =
            parse(reader, loader)

        /**
         * Parse an object of the type [T] out of the reader
         * @param reader The source of the XML events
         */
        @ImplicitReflectionSerializer
        inline fun <reified T : Any> parse(reader: XmlReader): T = XML().parse(reader, T::class)

        /**
         * Parse an object of the type [T] out of the reader
         * @param reader The source of the XML events
         * @param loader The loader to use (rather than the default)
         */
        @Suppress("unused")
        fun <T : Any> parse(reader: XmlReader, loader: DeserializationStrategy<T>): T =
            XML().parse(reader, loader)
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

private object DEFAULTSERIALCONTEXT : SerialContext {
    override fun <T : Any> get(kclass: KClass<T>): KSerializer<T>? {
        return if (kclass == CompactFragment::class) (CompactFragmentSerializer as KSerializer<T>) else null
    }

    override fun <T : Any> getByValue(value: T): KSerializer<T>? {
        return get(value::class) as KSerializer<T>
    }
}

private fun defaultSerialContext(): SerialContext = DEFAULTSERIALCONTEXT

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

        else                 -> QName(
            childrenName.namespace, childrenName.value,
            childrenName.prefix
                                     )
    }
}

internal fun <T : Any> KClass<T>.getSerialName(serializer: KSerializer<*>?, prefix: String?): QName {
    return getSerialName(serializer).let {
        if (prefix == null) it else it.copy(prefix = prefix)
    }
}

internal fun <T : Any> KClass<T>.getSerialName(serializer: SerializationStrategy<*>?): QName {
    return myAnnotations.getXmlSerialName()
        ?: serializer?.run { QName(descriptor.name.substringAfterLast('.')) }
        ?: QName(name.substringAfterLast('.'))
}

internal fun SerialDescriptor.getSerialName(prefix: String? = null): QName {
    return getEntityAnnotations().getXmlSerialName()?.let { if (prefix == null) it else it.copy(prefix) }
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
fun QName.copy(
    namespaceURI: String = this.namespaceURI,
    localPart: String = this.localPart,
    prefix: String = this.prefix
              ) =
    QName(namespaceURI, localPart, prefix)

/** Shortcircuit copy function that creates a new version (if needed) with the new prefix only */
internal fun QName.copy(prefix: String = this.prefix) = when (prefix) {
    this.prefix -> this
    else        -> QName(namespaceURI, localPart, prefix)
}

/**
 * Extension function for writing an object as XML.
 *
 * @param out The writer to use for writing the XML
 * @param serializer The serializer to use. Often `T.Companion.serializer()`
 */
fun <T : Any> T.writeAsXml(out: XmlWriter, serializer: SerializationStrategy<T>) =
    XML().toXml(out, this, serializer)

/**
 * Extension function that allows any (serializable) object to be written to an XmlWriter.
 */
@ImplicitReflectionSerializer
inline fun <reified T : Any> T.writeAsXML(out: XmlWriter) = XML().toXml(out, this)

/**
 * Extension function that allows any (serializable) object to be written to an XmlWriter. This version takes a [KClass]
 * object rather than having a generic specification.
 */
@ImplicitReflectionSerializer
@Suppress("NOTHING_TO_INLINE")
inline fun <T : Any> T.writeAsXML(kClass: KClass<T>, out: XmlWriter) =
    XML().toXml(out, obj = this, kClass = kClass)
