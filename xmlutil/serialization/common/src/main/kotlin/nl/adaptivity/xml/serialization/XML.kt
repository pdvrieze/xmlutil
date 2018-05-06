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

package nl.adaptivity.xml.serialization

import kotlinx.serialization.*
import nl.adaptivity.xml.*
import kotlin.reflect.KClass

class XML(val context: SerialContext? = null, val repairNamespaces: Boolean = true, val omitXmlDecl: Boolean = true) {

    inline fun <reified T : Any> stringify(obj: T): String = stringify(context.klassSerializer(T::class), obj)

    fun <T : Any> stringify(kClass: KClass<out T>, saver: KSerialSaver<T>, obj: T): String {
        return buildString {
            val writer = XmlStreaming.newWriter(this, repairNamespaces, omitXmlDecl)
            try {
                toXml(kClass, saver, obj, writer)
            } finally {
                writer.close()
            }
        }
    }

    inline fun <reified T : Any> toXml(obj: T, target: XmlWriter) {
        toXml(T::class, context.klassSerializer(T::class), obj,
              target)
    }

    fun <T : Any> toXml(kClass: KClass<out T>, serializer: KSerialSaver<T>, obj: T, target: XmlWriter) {


        val output = XmlOutput(context, target, kClass)

        output.write(serializer, obj)
    }

    fun <T> parse(loader: KSerialLoader<T>, str: String): T {
        TODO("Implement")
/*


        val parser = Parser(str)
        val input = JsonInput(Mode.OBJ, parser)
        val result = input.read(loader)
        check(parser.tc == TC_EOF) { "Shall parse complete string"}
        return result
*/
    }

    companion object {
        fun <T : Any> stringify(saver: KSerialSaver<T>,
                                obj: T,
                                kClass: KClass<out T> = obj::class): String = XML().stringify(kClass, saver, obj)

        inline fun <reified T : Any> stringify(obj: T): String = stringify(T::class.serializer(), obj, T::class)

        fun <T> parse(loader: KSerialLoader<T>, str: String): T = XML().parse(loader, str)
        inline fun <reified T : Any> parse(str: String): T = parse(T::class.serializer(), str)
    }

    private open class XmlOutput(context: SerialContext?,
                                   private val target: XmlWriter,
                                   protected val serialName: QName?,
                                   protected val childName: QName? = null) : TaggedOutput<OutputDescriptor>() {

        constructor(context: SerialContext?, target: XmlWriter, targetType: KClass<*>?):
            this(context, target, targetType?.getSerialName(), targetType?.getChildName())

        init {
            this.context = context
        }

        override fun KSerialClassDesc.getTag(index: Int): OutputDescriptor {
            return OutputDescriptor(outputKind(index), getTagName(index))
        }

        /**
         * Called when staring to write the children of this element.
         * @param desc The descriptor of the current element
         * @param typeParams The serializers for the elements
         */
        override fun writeBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KOutput {
            val tagName = currentTagOrNull?.name ?: serialName ?: QName(desc.name)
            return when (desc.kind) {
                KSerialClassKind.LIST,
                KSerialClassKind.MAP,
                KSerialClassKind.SET         -> {
                    currentTagOrNull?.run { kind = OutputKind.Element }
                    if(childName!=null) {
                        target.doSmartStartTag(tagName)
                    }
                    RepeatedWriter(context, target, tagName, childName)
                }

                KSerialClassKind.CLASS,
                KSerialClassKind.OBJECT,
                KSerialClassKind.SEALED,
                KSerialClassKind.POLYMORPHIC -> {
                    target.doSmartStartTag(tagName)
                    XmlOutput(context, target, tagName)
                }

                KSerialClassKind.ENTRY       -> TODO("Maps are not yet supported")//MapEntryWriter(currentTagOrNull)
                else                         -> throw SerializationException(
                    "Primitives are not supported at top-level")
            }
        }

        /**
         * Called when finished writing the current complex element.
         */
        override fun writeFinished(desc: KSerialClassDesc) {
            val tagName = currentTagOrNull?.name ?: serialName ?: QName(desc.name)
            target.endTag(tagName)
        }

/*

        override fun writeElement(desc: KSerialClassDesc, index: Int): Boolean {
            if (desc.outputKind(index)) {
                target.smartStartTag(desc.getTagName(index).also { currentInnerTag = it })
            } else {
                pendingAttrName = desc.getTagName(index)
            }
            return true
        }
*/

        override fun <T> writeSerializableValue(saver: KSerialSaver<T>, value: T) {
            super.writeSerializableValue(saver, value)
        }

        override fun writeTaggedNull(tag: OutputDescriptor) {
            // Do nothing - in xml absense is null
        }

        override fun writeTaggedBoolean(tag: OutputDescriptor, value: Boolean) = writeTaggedString(tag, value.toString())

        override fun writeTaggedByte(tag: OutputDescriptor, value: Byte) = writeTaggedString(tag, value.toString())

        override fun writeTaggedChar(tag: OutputDescriptor, value: Char) = writeTaggedString(tag, value.toString())

        override fun writeTaggedDouble(tag: OutputDescriptor, value: Double) = writeTaggedString(tag, value.toString())

        override fun writeTaggedFloat(tag: OutputDescriptor, value: Float) = writeTaggedString(tag, value.toString())

        override fun writeTaggedInt(tag: OutputDescriptor, value: Int) = writeTaggedString(tag, value.toString())

        override fun writeTaggedLong(tag: OutputDescriptor, value: Long) = writeTaggedString(tag, value.toString())

        override fun writeTaggedShort(tag: OutputDescriptor, value: Short) = writeTaggedString(tag, value.toString())

        override fun writeTaggedString(tag: OutputDescriptor, value: String) {
            when (tag.kind) {
                OutputKind.Unknown -> {tag.kind = OutputKind.Attribute; writeTaggedString(tag, value) }
                OutputKind.Attribute -> target.writeAttribute(tag.name, value)
                OutputKind.Text -> target.text(value)
                OutputKind.Element -> {
                    target.doSmartStartTag(tag.name)
                }
            }
        }

        /**
         * Wrapper function that will allow queing events
         */
        open fun XmlWriter.doSmartStartTag(name: QName) = smartStartTag(name)

        fun KSerialClassDesc.getTagName(index: Int): QName {
            getAnnotationsForIndex(index).getXmlSerialName()?.let { return it }

            val name = getElementName(index)
            val i = name.indexOf(':')
            return when {
                i > 0 -> {
                    val prefix = name.substring(i + 1)
                    val ns = target.getNamespaceUri(prefix) ?: throw IllegalArgumentException(
                        "Missing namespace for prefix $prefix")
                    QName(ns, name.substring(0, i), prefix)
                }
                else  -> QName(name)
            }
        }


        private class RepeatedWriter(context: SerialContext?,
                                     target: XmlWriter,
                                     tagName: QName,
                                     childName: QName?) : XmlOutput(context, target, tagName, childName) {

            override fun shouldWriteElement(desc: KSerialClassDesc, tag: OutputDescriptor, index: Int): Boolean {
                tag.kind == OutputKind.Element
                // Don't write the element count in xml
                return index != 0
            }

            override fun KSerialClassDesc.getTag(index: Int): OutputDescriptor {
                val name = childName ?: getAnnotationsForIndex(index).getXmlSerialName() ?: serialName ?: QName(this.name)

                return OutputDescriptor(outputKind(index), name)
            }

            override fun writeFinished(desc: KSerialClassDesc) {
                if (childName!=null) {
                    super.writeFinished(desc)
                }
            }
        }

    }
}

fun Collection<Annotation>.getXmlSerialName(): QName? {
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

fun Collection<Annotation>.getChildName(): QName? {
    val childrenName = firstOrNull<XmlChildrenName>()
    return when {
        childrenName == null -> null
        childrenName.namespace == UNSET_ANNOTATION_VALUE
                             -> QName(childrenName.value)

        childrenName.prefix == UNSET_ANNOTATION_VALUE
                             -> QName(childrenName.namespace, childrenName.value)

        else                 -> QName(childrenName.namespace, childrenName.value, childrenName.prefix)
    }
}

fun <T : Any> KClass<T>.getSerialName(): QName? {
    annotations.getXmlSerialName()?.let { return it }
    return simpleName?.filter { it != ':' }?.let { QName(it) }
}

fun <T : Any> KClass<T>.getChildName(): QName? {
    return annotations.getChildName()
}

/**
 * Specify more detailed name information than can be provided by [SerialName].
 */
@SerialInfo
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class XmlSerialName(val value: String,
                               val namespace: String = UNSET_ANNOTATION_VALUE,
                               val prefix: String = UNSET_ANNOTATION_VALUE)

/**
 * Specify additional information about child values. This is only used for primitives, not for classes that have their
 * own independent name
 */
@SerialInfo
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class XmlChildrenName(val value: String,
                                 val namespace: String = UNSET_ANNOTATION_VALUE,
                                 val prefix: String = UNSET_ANNOTATION_VALUE)

/**
 * Force a property that could be an attribute to be an element
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class XmlElement(val value: Boolean = true)

/**
 * Force a property to be element content
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class XmlValue(val value: Boolean = true)

private enum class OutputKind { Element, Attribute, Text, Unknown }
private data class OutputDescriptor(var kind: OutputKind, val name: QName)

internal const val UNSET_ANNOTATION_VALUE = "ZXCVBNBVCXZ"

private inline fun <reified T> Iterable<*>.firstOrNull(): T? {
    for (e in this) {
        if (e is T) return e
    }
    return null
}


private fun KSerialClassDesc.outputKind(index: Int): OutputKind {
    getAnnotationsForIndex(
        index).firstOrNull<XmlElement>()?.let { return if (it.value) OutputKind.Element else OutputKind.Attribute }
    return OutputKind.Unknown
}
