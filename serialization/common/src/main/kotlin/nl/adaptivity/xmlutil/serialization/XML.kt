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

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.*
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.multiplatform.assert
import nl.adaptivity.xmlutil.multiplatform.name
import nl.adaptivity.xmlutil.serialization.canary.*
import nl.adaptivity.xmlutil.serialization.compat.DecoderInput
import nl.adaptivity.xmlutil.serialization.compat.DummyParentDescriptor
import nl.adaptivity.xmlutil.serialization.compat.EncoderOutput
import nl.adaptivity.xmlutil.serialization.compat.SerialDescriptor
import nl.adaptivity.xmlutil.util.CompactFragment
import kotlin.reflect.KClass

data class NameHolder(val name: QName, val specified: Boolean)

class XmlNameMap {
    private val classMap = mutableMapOf<QName, String>()
    private val nameMap = mutableMapOf<String, NameHolder>()

    fun lookupName(kClass: KClass<*>) = lookupName(kClass.name)
    fun lookupClass(name: QName) = classMap[name.copy(prefix = "")]
    fun lookupName(kClass: String) = nameMap[kClass]

    fun registerClass(kClass: KClass<*>) {
        val serialInfo = kClass.serializer().serialClassDesc
        val serialName = serialInfo.getAnnotationsForClass().getXmlSerialName(null)//getSerialName(kClass.serializer())

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

class XML(val context: SerialContext? = defaultSerialContext(),
          val repairNamespaces: Boolean = true,
          val omitXmlDecl: Boolean = true,
          var indent: Int = 0) {

    val nameMap = XmlNameMap()

    fun registerClass(kClass: KClass<*>) = nameMap.registerClass(kClass)

    fun registerClass(name: QName, kClass: KClass<*>) = nameMap.registerClass(name, kClass.name, true)

    inline fun <reified T : Any> stringify(obj: T, prefix: String? = null): String = stringify(T::class, obj,
                                                                                               context.klassSerializer(
                                                                                                   T::class), prefix)

    fun <T : Any> stringify(kClass: KClass<T>,
                            obj: T,
                            saver: KSerialSaver<T> = context.klassSerializer(kClass),
                            prefix: String? = null): String {
        return buildString {
            val writer = XmlStreaming.newWriter(this, repairNamespaces, omitXmlDecl)

            var ex: Exception? = null
            try {
                toXml(kClass, obj, writer, prefix, saver)
            } catch (e: Exception) {
                ex = e
            } finally {
                try {
                    writer.close()
                } finally {
                    ex?.let { throw it }
                }

            }
        }
    }

    inline fun <reified T : Any> toXml(obj: T, target: XmlWriter, prefix: String? = null) {
        toXml(T::class, obj, target, prefix, context.klassSerializer(T::class))
    }

    fun <T : Any> toXml(kClass: KClass<T>,
                        obj: T,
                        target: XmlWriter,
                        prefix: String? = null,
                        serializer: KSerialSaver<T> = context.klassSerializer(kClass)) {
        target.indent = indent

        val serialDescriptor = Canary.serialDescriptor(serializer, obj)

        val serialName = kClass.getSerialName(serializer as? KSerializer<*>, prefix)
        val encoder = XmlEncoderBase(context, target).XmlEncoder(DummyParentDescriptor(serialName, serialDescriptor), 0)

        val output = EncoderOutput(encoder, serialDescriptor)

        output.write(serializer, obj)
    }

    inline fun <reified T : Any> parse(reader: XmlReader) = parse(T::class, reader)

    fun <T : Any> parse(kClass: KClass<T>,
                        reader: XmlReader,
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

    fun <T : Any> parse(kClass: KClass<T>, str: String, loader: KSerialLoader<T> = context.klassSerializer(kClass)): T {
        return parse(kClass, XmlStreaming.newReader(str), loader)
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <T : Any> stringify(obj: T, kClass: KClass<T> = obj::class as KClass<T>, prefix: String? = null): String =
            XML().run { stringify(kClass, obj, context.klassSerializer(kClass), prefix) }

        inline fun <reified T : Any> stringify(obj: T, prefix: String?): String = stringify(obj, T::class, prefix)

        @Suppress("UNCHECKED_CAST")
        fun <T : Any> toXml(dest: XmlWriter,
                            obj: T,
                            kClass: KClass<T> = obj::class as KClass<T>,
                            prefix: String? = null) =
            XML().run { toXml(kClass, obj, dest, prefix, context.klassSerializer(kClass)) }

        inline fun <reified T : Any> toXml(dest: XmlWriter, obj: T, prefix: String?) = toXml(dest, obj, T::class,
                                                                                                                   prefix)

        fun <T : Any> parse(kClass: KClass<T>, str: String): T = XML().parse(kClass, str)
        fun <T : Any> parse(kClass: KClass<T>, str: String, loader: KSerialLoader<T>): T = XML().parse(kClass, str,
                                                                                                       loader)

        inline fun <reified T : Any> parse(str: String): T = parse(T::class, str)
        inline fun <reified T : Any> parse(str: String, loader: KSerialLoader<T>): T = parse(T::class, str, loader)

        fun <T : Any> parse(kClass: KClass<T>, reader: XmlReader): T = XML().parse(kClass, reader)
        fun <T : Any> parse(kClass: KClass<T>, reader: XmlReader, loader: KSerialLoader<T>): T = XML().parse(kClass,
                                                                                                                                   reader,
                                                                                                                                   loader)

        inline fun <reified T : Any> parse(reader: XmlReader): T = parse(T::class, reader)
        inline fun <reified T : Any> parse(reader: XmlReader, loader: KSerialLoader<T>): T = parse(T::class, reader,
                                                                                                                         loader)
    }

    interface XmlOutput {
        /**
         * The name for the current tag
         */
        val serialName: QName
        /**
         * The currently active serialization context
         */
        val context: SerialContext?
        /**
         * The XmlWriter used. Can be used directly by serializers
         */
        val target: XmlWriter

        @Deprecated("Not used will always return null", ReplaceWith("null"))
        val currentTypeName: Nothing? get() = null
    }

    interface XmlInput {
        val input: XmlReader
    }

}

private fun defaultSerialContext() = SerialContext().apply {
    //    registerSerializer(ICompactFragment::class, ICompactFragmentSerializer())
    registerSerializer(CompactFragment::class, CompactFragmentSerializer)
}

fun Collection<Annotation>.getXmlSerialName(current: QName?): QName? {
    val serialName = firstOrNull<XmlSerialName>()
    return when {
        serialName == null -> null
        serialName.namespace == UNSET_ANNOTATION_VALUE
                           -> if (current == null) {
            QName(serialName.value)
        } else {
            QName(current.namespaceURI, serialName.value, current.prefix)
        }

        serialName.prefix == UNSET_ANNOTATION_VALUE
                           -> QName(serialName.namespace, serialName.value)

        else               -> QName(serialName.namespace, serialName.value, serialName.prefix)
    }
}

fun Collection<Annotation>.getChildName(): QName? {
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

fun <T : Any> KClass<T>.getSerialName(serializer: KSerializer<*>?, prefix: String?): QName {
    return getSerialName(serializer).let {
        if (prefix == null) it else it.copy(prefix = prefix)
    }
}

fun <T : Any> KClass<T>.getSerialName(serializer: KSerializer<*>?): QName {
    return myAnnotations.getXmlSerialName(null)
           ?: serializer?.run { QName(serialClassDesc.name.substringAfterLast('.')) }
           ?: QName(name.substringAfterLast('.'))
}

fun <T : Any> KClass<T>.getChildName(): QName? {
    return myAnnotations.getChildName()
}

enum class OutputKind {
    Element, Attribute, Text, Unknown;

    fun matchesExpectationBy(expectedOutputKind: OutputKind): Boolean {
        return when (expectedOutputKind) {
            this    -> true
            Unknown -> true
            else    -> false
        }
    }
}

internal fun XmlSerialName.toQName() = QName(namespace, value, prefix)

internal fun XmlChildrenName.toQName() = QName(namespace, value, prefix)

internal data class PolyInfo(val kClass: String, val tagName: QName, val index: Int)

internal const val UNSET_ANNOTATION_VALUE = "ZXCVBNBVCXZ"

internal inline fun <reified T> Iterable<*>.firstOrNull(): T? {
    for (e in this) {
        if (e is T) return e
    }
    return null
}


internal fun SerialDescriptor.getValueChild(): Int {
    if (associatedFieldsCount == 1) {
        return 0
    } else {
        for (i in 0 until associatedFieldsCount) {
            if (getAnnotationsForIndex(i).any { it is XmlValue }) return i
        }
        // TODO actually determine this properly
        return KInput.UNKNOWN_NAME
    }
}


fun QName.copy(namespaceURI: String = this.namespaceURI,
                                     localPart: String = this.localPart,
                                     prefix: String = this.prefix) = QName(namespaceURI,
                                                                                                 localPart,
                                                                                                 prefix)

fun QName.copy(prefix: String = this.prefix) = if (prefix == this.prefix) this else QName(
    namespaceURI, localPart,
    prefix)

inline fun <reified T : Any> T.writeAsXML(out: XmlWriter) = XML().toXml(this, out)

@Suppress("NOTHING_TO_INLINE")
inline fun <T : Any> T.writeAsXML(kClass: KClass<T>, out: XmlWriter) = XML().toXml(kClass, this, out)
