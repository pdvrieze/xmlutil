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
import nl.adaptivity.util.multiplatform.assert
import nl.adaptivity.util.multiplatform.name
import nl.adaptivity.util.xml.CompactFragment
import nl.adaptivity.xml.*
import nl.adaptivity.xml.serialization.canary.*
import kotlin.reflect.KClass

data class NameHolder(val name: QName, val specified: Boolean)

class XmlNameMap {
    private val classMap = mutableMapOf<QName, String>()
    private val nameMap = mutableMapOf<String, NameHolder>()

    fun lookupName(kClass: KClass<*>) = lookupName(kClass.name)
    fun lookupClass(name: QName) = classMap[name.copy(prefix = "")]
    fun lookupName(kClass: String) = nameMap[kClass]

    fun registerClass(kClass: KClass<*>) {
        val serialName = kClass.myAnnotations.getXmlSerialName(null)//getSerialName(kClass.serializer())

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
        val extInfo = try {
            Canary.extInfo(serializer, obj)
        } catch (e: Exception) {
            println(e.message)
            throw e
        }


        val output = XmlOutputBase(context, target).Initial(kClass.getSerialName(serializer as? KSerializer<*>, prefix),
                                                            kClass.getChildName(), kClass.name, extInfo)

        output.write(serializer, obj)
    }

    inline fun <reified T : Any> parse(reader: XmlReader) = parse(T::class, reader)

    fun <T : Any> parse(kClass: KClass<T>,
                        reader: XmlReader,
                        loader: KSerialLoader<T> = context.klassSerializer(kClass)): T {
        val serialName = kClass.getSerialName(loader as? KSerializer<*>)
        val extInfo = Canary.extInfo(loader)
        val input = XmlInputBase(reader).Initial(serialName, extInfo)
        return input.read(loader)
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

    internal interface XmlCommon<QN : QName?> {
        var serialName: QN
        val childName: QName?
        val myCurrentTag: OutputDescriptor
        val namespaceContext: NamespaceContext

        fun getTagName(desc: KSerialClassDesc) =
            myCurrentTag.name//.also { serialName = it }


        fun KSerialClassDesc.getTagName(index: Int): QName {
            getAnnotationsForIndex(index).getXmlSerialName(serialName)?.let {
                return it
            }

            val name = getElementName(index)
            val i = name.indexOf(':')
            return when {
                i > 0 -> {
                    val prefix = name.substring(i + 1)
                    val ns = namespaceContext.getNamespaceURI(prefix) ?: throw IllegalArgumentException(
                        "Missing namespace for prefix $prefix")
                    QName(ns, name.substring(0, i), prefix)
                }
                else  -> QName(serialName?.namespaceURI ?: "", name, /*serialName?.prefix ?: */"")
            }
        }

        fun KSerialClassDesc.getValueChild(): Int {
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

        fun doGetTag(classDesc: KSerialClassDesc, index: Int): OutputDescriptor {
            return OutputDescriptor(classDesc, index, classDesc.outputKind(index), classDesc.getTagName(index))
        }

        fun QName.normalize(): QName {
            return when {
                namespaceURI.isEmpty() -> copy(namespaceURI = namespaceContext.getNamespaceURI(prefix) ?: "",
                                               prefix = "")
                else                   -> copy(prefix = "")
            }
        }

        fun polyTagName(parentTag: QName,
                        polyChild: String,
                        currentTypeName: String?,
                        itemIdx: Int): PolyInfo {
            val currentPkg = currentTypeName?.substringBeforeLast('.', "") ?: ""
            val eqPos = polyChild.indexOf('=')
            val pkgPos: Int
            val prefPos: Int
            val typeNameBase: String
            val prefix: String
            val localPart: String

            if (eqPos < 0) {
                typeNameBase = polyChild
                pkgPos = polyChild.lastIndexOf('.')
                prefPos = -1
                prefix = parentTag.prefix
                localPart = if (pkgPos < 0) polyChild else polyChild.substring(pkgPos + 1)
            } else {
                typeNameBase = polyChild.substring(0, eqPos).trim()
                pkgPos = polyChild.lastIndexOf('.', eqPos - 1)
                prefPos = polyChild.indexOf(':', eqPos + 1)

                if (prefPos < 0) {
                    prefix = parentTag.prefix
                    localPart = polyChild.substring(eqPos + 1).trim()
                } else {
                    prefix = polyChild.substring(eqPos + 1, prefPos).trim()
                    localPart = polyChild.substring(prefPos + 1).trim()
                }
            }


            val ns = if (prefPos >= 0) namespaceContext.getNamespaceURI(prefix)
                                       ?: parentTag.namespaceURI else parentTag.namespaceURI

            val typename = if (pkgPos >= 0 || currentPkg.isEmpty()) typeNameBase else "$currentPkg.$typeNameBase"

            val name = QName(ns, localPart, prefix)

            return PolyInfo(typename, name, itemIdx)
        }

        fun PolyInfo(parentTag: QName,
                     currentTypeName: String?,
                     polyChildren: Array<String>): XmlNameMap {
            val result = XmlNameMap()

            for (polyChild in polyChildren) {
                val polyInfo = polyTagName(parentTag, polyChild, currentTypeName, -1)

                result.registerClass(polyInfo.tagName, polyInfo.kClass, polyChild.indexOf('=') >= 0)
            }

            return result
        }

    }

    interface XmlOutput {
        val serialName: QName
        val context: SerialContext?
        val target: XmlWriter
        val currentTypeName: String?
    }

    open class XmlOutputBase internal constructor(val context: SerialContext?,
                                                  val target: XmlWriter) {

        internal fun XmlOutput.writeBegin(desc: KSerialClassDesc,
                                          useAnnotations: List<Annotation>,
                                          tagName: QName,
                                          childName: QName?,
                                          extInfo: ExtInfo): KOutput {
            return when (desc.kind) {
                KSerialClassKind.LIST,
                KSerialClassKind.MAP,
                KSerialClassKind.SET    -> {
                    val tname: String?
                    if (childName != null) {
                        target.smartStartTag(tagName)

                        // If the child tag has a different namespace uri that requires a namespace declaration
                        // And we didn't just declare the prefix here already then we will declare it here rather
                        // than on each child
                        if (tagName.prefix != childName.prefix && target.getNamespaceUri(
                                childName.prefix) != childName.namespaceURI) {
                            target.namespaceAttr(childName.prefix, childName.namespaceURI)
                        }
                        tname = desc.name
                    } else {
                        tname = currentTypeName
                    }

                    ListWriter(tagName, childName, tname, useAnnotations, extInfo)
                }
                KSerialClassKind.POLYMORPHIC,
                KSerialClassKind.SEALED -> {
//                    val currentTypeName = (this as? XmlCommon<*>)?.myCurrentTag?.desc?.name
                    val polyChildren = useAnnotations.firstOrNull<XmlPolyChildren>()
                    val transparent = desc.associatedFieldsCount == 1 || polyChildren != null
                    if (!transparent) {
                        target.smartStartTag(tagName)
                    }

                    PolymorphicWriter(serialName, tagName, transparent, currentTypeName, polyChildren, extInfo)
                }
                KSerialClassKind.CLASS,
                KSerialClassKind.OBJECT -> {
                    target.smartStartTag(tagName)
                    Base(tagName, childName, desc.associatedFieldsCount > 1, extInfo)
                }

                KSerialClassKind.ENTRY  -> TODO("Maps are not yet supported")//MapEntryWriter(currentTagOrNull)
                else                    -> throw SerializationException(
                    "Primitives are not supported at top-level")
            }
        }

        internal inner open class Base(override var serialName: QName,
                                       override var childName: QName?,
                                       private var deferring: Boolean = true,
                                       private val extInfo: ExtInfo) : TaggedOutput<OutputDescriptor>(), XmlCommon<QName>, XmlOutput {


            val deferredBuffer = mutableListOf<Pair<OutputDescriptor, TaggedOutput<OutputDescriptor>.() -> Unit>>()

            override val currentTypeName: String?
                get() = currentTag.desc.name

            override val myCurrentTag: OutputDescriptor get() = currentTag

            override val namespaceContext: NamespaceContext get() = target.namespaceContext
            override val target: XmlWriter get() = this@XmlOutputBase.target

            open fun OutputKind.effectiveKind() = when (this) {
                OutputKind.Unknown -> OutputKind.Attribute
                else               -> this
            }

            /**
             * Called when staring to write the children of this element. This function still has access to the
             * tag. As such the additional information determined on use for the type can be extracted from there
             * and stored into the new output
             *
             * @param desc The descriptor of the current element
             * @param typeParams The serializers for the elements
             */
            override fun writeBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KOutput {
                val tag = currentTag
                return writeBegin(desc, tag.currentAnnotations, tag.name, tag.childName, tag.extInfo!!)
            }

            /**
             * Called when finished writing the current complex element.
             */
            override fun writeFinished(desc: KSerialClassDesc) {
                deferring = false
                for (deferred in deferredBuffer) {
                    TagSaver(deferred.first, this).apply(deferred.second)
                }
                target.endTag(serialName)
            }

            override fun doGetTag(classDesc: KSerialClassDesc, index: Int): OutputDescriptor {
                if (index < extInfo.childInfo.size) {
                    return OutputDescriptor(classDesc, index, classDesc.outputKind(index, extInfo),
                                            classDesc.getTagName(index))
                } else {
                    return OutputDescriptor(classDesc, index, OutputKind.Unknown, QName("value"), null)
                }
            }

            final override fun KSerialClassDesc.getTag(index: Int): OutputDescriptor {
                // just delegate to a function that can be overridden properly with super calls
                return doGetTag(this, index)
            }

            override fun writeTaggedNull(tag: OutputDescriptor) {
                // Do nothing - in xml absense is null
            }

            override fun writeTaggedValue(tag: OutputDescriptor, value: Any) {
                if (value is XmlSerializable) {
                    value.serialize(target)
                } else {
                    super.writeTaggedValue(tag, value)
                }
            }

            override fun <T> writeSerializableValue(saver: KSerialSaver<T>, value: T) {
                val t = currentTag
                val extInfo = Canary.extInfo(saver, value)
                t.extInfo = extInfo


                if (t.kind == OutputKind.Unknown) {
                    t.kind = when (extInfo.type) {
                        ChildType.ELEMENT -> OutputKind.Element
                        else              -> OutputKind.Attribute
                    }
                }

                when (t.kind) {
                    OutputKind.Element,
                    OutputKind.Text -> defer(t) { saver.save(this, value) }
                    else            -> saver.save(this, value)
                }

            }

            override fun writeTaggedBoolean(tag: OutputDescriptor, value: Boolean) = writeTaggedString(tag,
                                                                                                       value.toString())

            override fun writeTaggedByte(tag: OutputDescriptor, value: Byte) = writeTaggedString(tag, value.toString())

            override fun writeTaggedChar(tag: OutputDescriptor, value: Char) = writeTaggedString(tag, value.toString())

            override fun writeTaggedDouble(tag: OutputDescriptor, value: Double) = writeTaggedString(tag,
                                                                                                     value.toString())

            override fun writeTaggedFloat(tag: OutputDescriptor, value: Float) = writeTaggedString(tag,
                                                                                                   value.toString())

            override fun writeTaggedInt(tag: OutputDescriptor, value: Int) = writeTaggedString(tag, value.toString())

            override fun writeTaggedLong(tag: OutputDescriptor, value: Long) = writeTaggedString(tag, value.toString())

            override fun writeTaggedShort(tag: OutputDescriptor, value: Short) = writeTaggedString(tag,
                                                                                                   value.toString())

            override fun writeTaggedString(tag: OutputDescriptor, value: String) {
                val defaultValue = tag.currentAnnotations.firstOrNull<XmlDefault>()?.value
                if (value == defaultValue) return // Don't write the default value if defined

                when (tag.kind.effectiveKind()) {
                    OutputKind.Unknown   -> throw UnsupportedOperationException("Unknown should never happen")
                    OutputKind.Attribute -> target.doWriteAttribute(tag.name, value)
                    OutputKind.Text      -> defer(tag) { target.doText(value) }
                    OutputKind.Element   -> defer(tag) {
                        doSmartStartTag(tag.name) {
                            text(value)
                        }
                    }
                }
            }

            open fun defer(tag: OutputDescriptor, deferred: TaggedOutput<OutputDescriptor>.() -> Unit) {

                if (tag.kind == OutputKind.Attribute) {
                    deferred()
                } else if (deferring) {
                    deferredBuffer.add(Pair(tag, deferred))
                } else {
                    deferred()
                }
            }

            open fun Any.doWriteAttribute(name: QName, value: String) {
                val actualAttrName: QName = when {
                    name.getNamespaceURI().isEmpty() ||
                    (serialName.getNamespaceURI() == name.getNamespaceURI() &&
                     (serialName.prefix == name.prefix)) -> QName(
                        name.localPart) // Breaks in android otherwise

                    else -> name
                }

                target.writeAttribute(actualAttrName, value)
            }

            inline fun doSmartStartTag(name: QName, body: XmlWriter.() -> Unit) = target.run {
                smartStartTag(name)
                body()
                endTag(name)
            }

            open fun Any.doText(value: String) = target.text(value)

        }

        inner class Initial(override val serialName: QName,
                            val childName: QName?,
                            override val currentTypeName: String?,
                            val extInfo: ExtInfo) : ElementValueOutput(), XmlOutput {
            init {
                this.context = this@XmlOutputBase.context
            }

            override val target: XmlWriter get() = this@XmlOutputBase.target

            /**
             * Create an output for the element. We only need this as xml serialization must always start with an
             * element. We should not have any tag defined yet
             *
             * @param desc The description for the *new* element
             */
            override fun writeBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KOutput {
                val tagName = serialName
                return writeBegin(desc, emptyList(), tagName, childName, extInfo)
            }
        }

        private inner class PolymorphicWriter(parentName: QName,
                                              serialName: QName,
                                              val transparent: Boolean,
                                              override val currentTypeName: String?,
                                              polyChildren: XmlPolyChildren?,
                                              extInfo: ExtInfo) :
            Base(serialName, null, extInfo = extInfo), XmlOutput {

            val polyChildren = polyChildren?.let { PolyInfo(parentName, currentTypeName, it.value) }

            override fun doGetTag(classDesc: KSerialClassDesc, index: Int): OutputDescriptor {
                val tagName: QName
                val outputKind: OutputKind
                when (index) {
                    0    -> {
                        tagName = QName("type")
                        outputKind = OutputKind.Attribute
                    }
                    else -> {
                        tagName = if (transparent) serialName else classDesc.getTagName(index)
                        outputKind = OutputKind.Element
                    }
                }

                return OutputDescriptor(classDesc, index, outputKind, tagName, childName)
            }

            override fun writeBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KOutput {
                return super.writeBegin(desc, *typeParams)
            }

            override fun defer(tag: OutputDescriptor, deferred: TaggedOutput<OutputDescriptor>.() -> Unit) {
                deferred()
            }

            override fun writeTaggedString(tag: OutputDescriptor, value: String) {
                if (transparent && tag.index == 0) {
                    val regName = polyChildren?.lookupName(value)
                    serialName = if (regName?.specified == true) regName.name else QName(
                        value.substringAfterLast('.'))
                } else {
                    super.writeTaggedString(tag, value)
                }
            }

            override fun writeFinished(desc: KSerialClassDesc) {
                // Don't write anything if we're transparent
                if (!transparent) {
                    super.writeFinished(desc)
                }
            }
        }

        private inner class TagSaver(val tag: OutputDescriptor,
                                     val delegate: Base) : TaggedOutput<OutputDescriptor>(), XmlOutput {

            override fun writeBegin(desc: KSerialClassDesc,
                                    collectionSize: Int,
                                    vararg typeParams: KSerializer<*>): KOutput {
                return writeBegin(desc)
            }

            override val serialName: QName get() = delegate.serialName

            override val target: XmlWriter get() = delegate.target

            override val currentTypeName: String? get() = tag.desc.name

            override fun KSerialClassDesc.getTag(index: Int): OutputDescriptor {
                return delegate.doGetTag(this, index)
            }

            override fun <T> writeSerializableValue(saver: KSerialSaver<T>, value: T) {
                throw UnsupportedOperationException("Nesting saving of tags is unsupported")
            }

            override fun shouldWriteElement(desc: KSerialClassDesc, tag: OutputDescriptor, index: Int) =
                delegate.shouldWriteElement(desc, tag, index)

            override fun writeFinished(desc: KSerialClassDesc) = delegate.writeFinished(desc)

            override fun writeTaggedBoolean(tag: OutputDescriptor, value: Boolean) =
                delegate.writeTaggedBoolean(tag, value)

            override fun writeTaggedByte(tag: OutputDescriptor, value: Byte) = delegate.writeTaggedByte(tag, value)

            override fun writeTaggedChar(tag: OutputDescriptor, value: Char) = delegate.writeTaggedChar(tag, value)

            override fun writeTaggedDouble(tag: OutputDescriptor, value: Double) =
                delegate.writeTaggedDouble(tag, value)

            override fun <E : Enum<E>> writeTaggedEnum(tag: OutputDescriptor, enumClass: KClass<E>, value: E) =
                delegate.writeTaggedEnum(tag, enumClass, value)

            override fun writeTaggedFloat(tag: OutputDescriptor, value: Float) = delegate.writeTaggedFloat(tag, value)

            override fun writeTaggedInt(tag: OutputDescriptor, value: Int) = delegate.writeTaggedInt(tag, value)

            override fun writeTaggedLong(tag: OutputDescriptor, value: Long) = delegate.writeTaggedLong(tag, value)

            override fun writeTaggedNotNullMark(tag: OutputDescriptor) = delegate.writeTaggedNotNullMark(tag)

            override fun writeTaggedNull(tag: OutputDescriptor) = delegate.writeTaggedNull(tag)

            override fun writeTaggedShort(tag: OutputDescriptor, value: Short) = delegate.writeTaggedShort(tag, value)

            override fun writeTaggedString(tag: OutputDescriptor, value: String) =
                delegate.writeTaggedString(tag, value)

            override fun writeTaggedUnit(tag: OutputDescriptor) = delegate.writeTaggedUnit(tag)

            override fun writeTaggedValue(tag: OutputDescriptor, value: Any) {
                delegate.writeTaggedValue(tag, value)
            }

            override fun writeBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KOutput {
                val tag = tag
                return writeBegin(desc, tag.currentAnnotations, tag.name, tag.childName, tag.extInfo!!)
            }
        }

        /** Writer that does not actually write an outer tag unless a childName is specified */
        private inner class ListWriter(serialName: QName,
                                       childName: QName?,
                                       override val currentTypeName: String?,
                                       val useAnnotations: List<Annotation>,
                                       extInfo: ExtInfo) : Base(serialName, childName, extInfo = extInfo), XmlOutput {

            override fun OutputKind.effectiveKind() = when (this) {
                OutputKind.Unknown,
                OutputKind.Attribute -> OutputKind.Element

                else                 -> this
            }

            override fun defer(tag: OutputDescriptor, deferred: TaggedOutput<OutputDescriptor>.() -> Unit) {
                deferred()
            }

            override fun writeBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KOutput {
                val tag = currentTag
                return writeBegin(desc, useAnnotations, tag.name, tag.childName, tag.extInfo!!)
            }

            override fun shouldWriteElement(desc: KSerialClassDesc, tag: OutputDescriptor, index: Int): Boolean {
                return index != 0
            }

            override fun doGetTag(classDesc: KSerialClassDesc, index: Int): OutputDescriptor {
                val name = childName // If there is a child name, use that
                           ?: serialName // otherwise use the parent name

                val effectiveKind = classDesc.outputKind(index).effectiveKind()
                return OutputDescriptor(classDesc, index, effectiveKind, name)
            }

            override fun writeFinished(desc: KSerialClassDesc) {
                if (childName != null) {
                    super.writeFinished(desc)
                }
            }
        }

    }

    interface XmlInput {
        val input: XmlReader
    }

    open class XmlInputBase internal constructor(val input: XmlReader) {

        var skipRead = false

        inner class Initial(val serialName: QName, val extInfo: ExtInfo) : ElementValueInput(), XmlInput {
            override val input: XmlReader
                get() = this@XmlInputBase.input

            override fun readBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KInput {
                input.nextTag()

                return readBegin(desc, serialName, null, false, extInfo)
            }
        }

        internal abstract inner class Base(val desc: KSerialClassDesc,
                                           override var serialName: QName,
                                           override val childName: QName?) : TaggedInput<OutputDescriptor>(), XmlCommon<QName>, XmlInput {


            private var nameToMembers: Map<QName, Int>? = null
            protected var polyChildren: Map<QName, PolyInfo>? = null

            override val input: XmlReader get() = this@XmlInputBase.input

            override val myCurrentTag: OutputDescriptor get() = currentTag
            override val namespaceContext: NamespaceContext get() = input.namespaceContext

            final override fun KSerialClassDesc.getTag(index: Int): OutputDescriptor {
                return doGetTag(this, index)
            }

            override fun doGetTag(classDesc: KSerialClassDesc, index: Int): OutputDescriptor {
                val tagName = classDesc.getTagName(index)
                return OutputDescriptor(classDesc, index, classDesc.outputKind(index), tagName)
            }

            override fun readBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KInput {
                val tag = currentTag
                val tagName = tag.name

                val polyInfo = polyChildren?.values?.firstOrNull { it.index == tag.index && it.tagName.normalize() == input.name.normalize() }

                return readBegin(desc, tagName, polyInfo, false, tag.extInfo!!)
            }

            override fun <T> readSerializableValue(loader: KSerialLoader<T>): T {
                currentTag.extInfo = Canary.extInfo(loader)
                return super.readSerializableValue(loader)
            }

            override fun <T : Any> updateNullableSerializableValue(loader: KSerialLoader<T?>,
                                                                   desc: KSerialClassDesc,
                                                                   old: T?): T? {
                currentTag.extInfo = Canary.extInfo(loader)
                return super.updateNullableSerializableValue(loader, desc, old)
            }

            override fun <T> updateSerializableValue(loader: KSerialLoader<T>, desc: KSerialClassDesc, old: T): T {
                currentTag.extInfo = Canary.extInfo(loader)
                return super.updateSerializableValue(loader, desc, old)
            }

            open fun KSerialClassDesc.indexOf(name: QName, attr: Boolean): Int {
                val polyMap: Map<QName, PolyInfo>
                val nameMap: Map<QName, Int>

                if (polyChildren != null && nameToMembers != null) {
                    polyMap = polyChildren!!
                    nameMap = nameToMembers!!
                } else {
                    polyMap = mutableMapOf(); polyChildren = polyMap
                    nameMap = mutableMapOf(); nameToMembers = nameMap


                    for (fieldNo in 0 until associatedFieldsCount) {
                        val polyChildren = getAnnotationsForIndex(fieldNo).firstOrNull<XmlPolyChildren>()
                        if (polyChildren != null) {
                            for (child in polyChildren.value) {
                                val polyInfo = polyTagName(serialName, child, this.name, fieldNo)
                                polyMap[polyInfo.tagName.normalize()] = polyInfo
                            }
                        } else {
                            nameMap[getTagName(fieldNo).normalize()] = fieldNo
                        }
                    }

                }
                val normalName = name.normalize()
                nameMap[normalName]?.let { return it }
                polyMap[normalName]?.let { return it.index }

                if (attr && name.namespaceURI.isEmpty()) {
                    val attrName = normalName.copy(namespaceURI = serialName.namespaceURI)
                    nameMap[attrName]?.let { return it }
                    polyMap[normalName.copy(namespaceURI = serialName.namespaceURI)]?.let { return it.index }
                }

//                val pkg = desc.name.substringBeforeLast('.', "")

                throw SerializationException("Could not find a field for name $name\n  candidates " +
                                             "were: ${(nameMap.keys + polyMap.keys).joinToString()}")
            }

            override fun readElement(desc: KSerialClassDesc): Int {
                if (skipRead) { // reading values will already move to the end tag.
                    assert(input.isEndElement())
                    skipRead = false
                    return readElementEnd(desc)
                }
                // TODO validate correctness of type
                for (eventType in input) {
                    if (!eventType.isIgnorable) {
                        // The android reader doesn't check whitespaceness

                        when (eventType) {
                            EventType.END_ELEMENT   -> return readElementEnd(desc)
                            EventType.TEXT          -> if (!input.isWhitespace()) return desc.getValueChild()
                            EventType.ATTRIBUTE     -> return desc.indexOf(input.name, true)
                            EventType.START_ELEMENT -> return desc.indexOf(input.name, false)
                            else                    -> throw SerializationException("Unexpected event in stream")
                        }
                    }
                }
                return KInput.READ_DONE
            }

            open fun readElementEnd(desc: KSerialClassDesc): Int {
                return READ_DONE
            }

            open fun doReadAttribute(tag: OutputDescriptor): String =
                throw UnsupportedOperationException("Base doesn't support reading attributes")

            override fun readTaggedString(tag: OutputDescriptor): String {
                return when (tag.kind) {
                    OutputKind.Element   -> input.readSimpleElement()
                    OutputKind.Text      -> {
                        skipRead = true
                        input.allText()
                    }
                    OutputKind.Attribute -> doReadAttribute(tag)
                    OutputKind.Unknown   -> run { tag.kind = OutputKind.Attribute; doReadAttribute(tag) }
                }
            }

            override fun readTaggedBoolean(tag: OutputDescriptor) = readTaggedString(tag).toBoolean()

            override fun readTaggedByte(tag: OutputDescriptor) = readTaggedString(tag).toByte()

            override fun readTaggedChar(tag: OutputDescriptor) = readTaggedString(tag).single()

            override fun readTaggedDouble(tag: OutputDescriptor) = readTaggedString(tag).toDouble()

            override fun readTaggedFloat(tag: OutputDescriptor) = readTaggedString(tag).toFloat()

            override fun readTaggedInt(tag: OutputDescriptor) = when (tag.desc.kind) {
                KSerialClassKind.SET,
                KSerialClassKind.LIST,
                KSerialClassKind.MAP -> if (tag.index == 0) 1 else readTaggedString(
                    tag).toInt() // Always return elements one by one (there is no list size)

                else                 -> readTaggedString(tag).toInt()
            }

            override fun readTaggedLong(tag: OutputDescriptor) = readTaggedString(tag).toLong()

            override fun readTaggedShort(tag: OutputDescriptor) = readTaggedString(tag).toShort()

            override fun readTaggedValue(tag: OutputDescriptor): Any {
                throw UnsupportedOperationException(
                    "Unable to read object ${tag.desc.getElementName(tag.index)} with tag $tag")
            }
        }

        internal inner class Element(desc: KSerialClassDesc,
                                     serialName: QName,
                                     childName: QName?,
                                     private var attrIndex: Int = 0,
                                     var extInfo: ExtInfo) : Base(desc, serialName, childName) {
            private val seenItems = BooleanArray(desc.associatedFieldsCount)

            private var nulledItemsIdx = -1

            override fun readBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KInput {
                val tag = currentTag
                val tagName = tag.name

                val polyInfo = polyChildren?.values?.firstOrNull { it.index == tag.index && it.tagName.normalize() == input.name.normalize() }


                return readBegin(desc, tagName, polyInfo, nulledItemsIdx >= 0, tag.extInfo!!)
            }

            fun nextNulledItemsIdx() {
                for (i in (nulledItemsIdx + 1) until seenItems.size) {
                    if (!seenItems[i]) {
                        val childInfo = extInfo.childInfo[i]
                        val default = childInfo.annotations.firstOrNull<XmlDefault>()
                        // If a
                        val defaultOrList = childInfo.isNullable || default != null || when (childInfo.kind) {
                            KSerialClassKind.SET,
                            KSerialClassKind.MAP,
                            KSerialClassKind.LIST -> true
                            else                  -> false
                        }
                        if (defaultOrList) {
                            nulledItemsIdx = i
                            return
                        }
                    }
                }
                nulledItemsIdx = seenItems.size
            }

            override fun doGetTag(classDesc: KSerialClassDesc, index: Int): OutputDescriptor {
                markItemSeen(index)

                if (index < extInfo.childInfo.size) {
                    return OutputDescriptor(classDesc, index, classDesc.outputKind(index, extInfo),
                                            classDesc.getTagName(index))
                } else {
                    return OutputDescriptor(classDesc, index, OutputKind.Unknown, QName("value"), null)
                }
            }

            fun markItemSeen(index: Int) {
                seenItems[index] = true
            }

            override fun readElement(desc: KSerialClassDesc): Int {
                if (nulledItemsIdx >= 0) {
                    val sn = serialName
                    input.require(EventType.END_ELEMENT, sn.namespaceURI, sn.localPart)

                    if (nulledItemsIdx >= seenItems.size) return KInput.READ_DONE
                    val i = nulledItemsIdx
                    nextNulledItemsIdx()
                    return i
                }

                if (attrIndex >= 0 && attrIndex < input.attributeCount) {

                    val name = input.getAttributeName(attrIndex)

                    return if (name.getNamespaceURI() == XMLConstants.XMLNS_ATTRIBUTE_NS_URI ||
                               name.prefix == XMLConstants.XMLNS_ATTRIBUTE ||
                               (name.prefix.isEmpty() && name.localPart == XMLConstants.XMLNS_ATTRIBUTE)) {
                        // Ignore namespace decls
                        readElement(desc)
                    } else {
                        desc.indexOf(name, true)
                    }
                }
                attrIndex = -1 // Ensure to reset here

                return super.readElement(desc)
            }

            override fun readElementEnd(desc: KSerialClassDesc): Int {
                nextNulledItemsIdx()
                return when {
                    nulledItemsIdx < seenItems.size -> nulledItemsIdx
                    else                            -> READ_DONE
                }
            }

            override fun readTaggedNotNullMark(tag: OutputDescriptor): Boolean {
                return nulledItemsIdx < 0 // If we are not yet reading "missing values" we have no nulls
            }

            override fun doReadAttribute(tag: OutputDescriptor): String {
                return input.getAttributeValue(attrIndex).also { attrIndex++ }
            }

            override fun <T> readSerializableValue(loader: KSerialLoader<T>): T {
                if (attrIndex >= 0) {
                    if (currentTag.kind == OutputKind.Element) { // We are having an element masquerading as attribute.
                        // Whatever we do, increase the index
                        attrIndex++
                    }
                }
                return super.readSerializableValue(loader)
            }

            override fun readTaggedString(tag: OutputDescriptor): String {
                if (nulledItemsIdx >= 0) {
                    return tag.currentAnnotations.firstOrNull<XmlDefault>()?.value ?: throw MissingFieldException(
                        "${tag.desc.getElementName(tag.index)}:${tag.index}")
                }
                return super.readTaggedString(tag)
            }
        }

        internal inner class AnonymousListInput(desc: KSerialClassDesc,
                                                childName: QName,
                                                val polyInfo: PolyInfo?,
                                                var finished: Boolean,
                                                val childInfo: ChildInfo) :
            Base(desc, childName, null), XmlInput {

            lateinit var extInfo: ExtInfo

            override fun readBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KInput {
                return readBegin(desc, serialName, polyInfo, false, currentTag.extInfo!!)
            }

            override fun readElement(desc: KSerialClassDesc): Int {
                return when {
                    finished -> KInput.READ_DONE
                    else     -> {
                        finished = true; 1
                    }
                }
            }

            override fun <T> updateSerializableElementValue(desc: KSerialClassDesc,
                                                            index: Int,
                                                            loader: KSerialLoader<T>,
                                                            old: T): T {
                extInfo = Canary.extInfo(loader)
                val outKind = desc.outputKind(index, extInfo)
                if (outKind == OutputKind.Attribute && index > 1) {
                    throw IndexOutOfBoundsException("Cannot read more than one attribute of name $serialName")
                }
                return super.updateSerializableElementValue(desc, index, loader, old)
            }

            override fun doReadAttribute(tag: OutputDescriptor): String {
                val expectedNS = serialName.namespaceURI
                val expectedName = serialName.localPart
                val allowEmpty = serialName.namespaceURI == expectedNS

                var index = -1
                for (i in 0 until input.attributeCount) {
                    if (input.getAttributeLocalName(i) == expectedName) {
                        val actualNS = input.getAttributeNamespace(i)
                        if (actualNS == expectedNS) {
                            index = i; break;
                        } else if (actualNS.isEmpty() && allowEmpty) {
                            index = i
                        }
                    }
                }
                if (index < 0) throw SerializationException(
                    "No attribute for name $serialName found on element of name ${input.name}")

                return input.getAttributeValue(index)
            }

            override fun doGetTag(classDesc: KSerialClassDesc, index: Int): OutputDescriptor {
                val tagName = serialName
                val suggestedOutputKind = when (index) {
                    0    -> classDesc.outputKind(index)
                    else -> if (childInfo.type.isPrimitive) {
                        OutputKind.Text
                    } else {
                        classDesc.outputKind(childInfo)
                    }
                }
                val outputKind = when (suggestedOutputKind) {
                    OutputKind.Attribute -> OutputKind.Text
                    else                 -> suggestedOutputKind
                }

                return OutputDescriptor(classDesc, index, outputKind, tagName)
            }
        }

        internal inner class NamedListInput(desc: KSerialClassDesc, serialName: QName, childName: QName) :
            Base(desc, serialName, childName) {
            var childCount = 0

            override fun readElement(desc: KSerialClassDesc): Int {
                return when (input.nextTag()) {
                    EventType.END_ELEMENT -> KInput.READ_DONE
                    else                  -> ++childCount // This is important to ensure appending in the list.
                }
            }

            override fun doGetTag(classDesc: KSerialClassDesc, index: Int): OutputDescriptor {
                // The classDesc only has indices 0 and 1. Just make sure to return the needed tag, rather than the "truth"
                return OutputDescriptor(classDesc, 1, OutputKind.Element, childName!!)
            }
        }

        internal inner class PolymorphicInput(desc: KSerialClassDesc,
                                              val polyInfo: PolyInfo?,
                                              val transparent: Boolean = polyInfo != null) : Base(desc,
                                                                                                  QName(
                                                                                                      "--invalid--"),
                                                                                                  null) {
            override fun readBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KInput {
                return super.readBegin(desc, *typeParams)
            }

            override fun readElement(desc: KSerialClassDesc): Int {
                return KInput.READ_ALL
            }

            override fun doGetTag(classDesc: KSerialClassDesc, index: Int): OutputDescriptor {
                return when (index) {
                    0    -> OutputDescriptor(classDesc, index, OutputKind.Attribute, QName("type"))
                    else -> OutputDescriptor(classDesc, index, OutputKind.Element,
                                             polyInfo?.tagName ?: childName ?: classDesc.getTagName(index))
                }
            }

            override fun doReadAttribute(tag: OutputDescriptor): String {
                return if (!transparent) {
                    input.getAttributeValue(0)
                } else {
                    polyInfo?.kClass ?: input.name.localPart // Likely to fail unless the tagname matches the type
                }
            }

            override fun KSerialClassDesc.indexOf(name: QName, attr: Boolean): Int {
                return if (name.namespaceURI == "" && name.localPart == "type") 0 else 1
            }

            override fun <T> readSerializableValue(loader: KSerialLoader<T>): T {
                if (!transparent) input.nextTag()
                return super.readSerializableValue(loader)
            }

            override fun readEnd(desc: KSerialClassDesc) {
                if (!transparent) {
                    input.nextTag()
                    require(input.isEndElement())
                }
                super.readEnd(desc)
            }
        }

        internal fun XmlInput.readBegin(desc: KSerialClassDesc,
                                        tagName: QName,
                                        polyInfo: PolyInfo?,
                                        isReadingNulls: Boolean,
                                        extInfo: ExtInfo): KInput {

            return when (desc.kind) {
                KSerialClassKind.LIST,
                KSerialClassKind.MAP,
                KSerialClassKind.SET    -> {
                    val t = (this as XmlCommon<*>).myCurrentTag
                    t.kind = OutputKind.Element

                    val childName = t.desc.getAnnotationsForIndex(t.index).getChildName()
                    return when (childName) {
                        null -> AnonymousListInput(desc, tagName, polyInfo, isReadingNulls, extInfo.childInfo[1])
                        else -> {
                            input.require(EventType.START_ELEMENT, tagName.namespaceURI, tagName.localPart)
                            NamedListInput(desc, tagName, childName)
                        }
                    }
                }

                KSerialClassKind.POLYMORPHIC,
                KSerialClassKind.SEALED -> {
                    PolymorphicInput(desc, polyInfo, tagName.copy(prefix = "") != input.name.copy(prefix = ""))
                }

                KSerialClassKind.CLASS,
                KSerialClassKind.OBJECT -> {
                    input.require(EventType.START_ELEMENT, tagName.namespaceURI, tagName.localPart)
                    Element(desc, tagName, null, extInfo = extInfo)
                }

                KSerialClassKind.ENTRY  -> TODO("Maps are not yet supported")//MapEntryWriter(currentTagOrNull)
                else                    -> throw SerializationException(
                    "Primitives are not supported at top-level")
            }
        }

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

        else                 -> QName(childrenName.namespace, childrenName.value, childrenName.prefix)
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

/**
 * Specify more detailed name information than can be provided by [SerialName].
 */
@SerialInfo
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class XmlSerialName(val value: String,
                               val namespace: String/* = UNSET_ANNOTATION_VALUE*/,
                               val prefix: String/* = UNSET_ANNOTATION_VALUE*/)

/**
 * Indicate the valid poly children for this element
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class XmlPolyChildren(val value: Array<String>)

/**
 * Specify additional information about child values. This is only used for primitives, not for classes that have their
 * own independent name
 */
@SerialInfo
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class XmlChildrenName(val value: String,
                                 val namespace: String/* = UNSET_ANNOTATION_VALUE*/,
                                 val prefix: String/* = UNSET_ANNOTATION_VALUE*/)

/**
 * Force a property that could be an attribute to be an element
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class XmlElement(val value: Boolean/* = true*/)

/**
 * Force a property to be element content
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class XmlValue(val value: Boolean /*= true*/)

/**
 * Allow a property to be omitted with a default serialized string
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class XmlDefault(val value: String)

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

internal data class OutputDescriptor(val desc: KSerialClassDesc,
                                     val index: Int,
                                     var kind: OutputKind,
                                     var name: QName,
                                     val childName: QName? = if (index < desc.associatedFieldsCount) desc.getAnnotationsForIndex(
                                         index).getChildName() else null,
                                     var _polyInfo: PolyInfo? = null,
                                     var extInfo: ExtInfo? = null) {


    //    val childName: QName? by lazy { desc.getAnnotationsForIndex(index).getChildName() }
    val currentAnnotations get() = desc.getAnnotationsForIndex(index)
}

internal data class PolyInfo(val kClass: String, val tagName: QName, val index: Int)

internal const val UNSET_ANNOTATION_VALUE = "ZXCVBNBVCXZ"

private inline fun <reified T> Iterable<*>.firstOrNull(): T? {
    for (e in this) {
        if (e is T) return e
    }
    return null
}

private fun KSerialClassDesc.outputKind(index: Int, extInfo: ExtInfo? = null): OutputKind {
    // lists will always be elements
    if (index < associatedFieldsCount) {
        getAnnotationsForIndex(index).forEach {
            if (it is XmlChildrenName) return OutputKind.Element
            if (it is XmlElement) return if (it.value) OutputKind.Element else OutputKind.Attribute
            if (it is XmlValue && it.value) return OutputKind.Text
        }
    }

    val childInfo = extInfo?.childInfo?.let {
        if (index < it.size) it[index] else null
    }

    return outputKind(childInfo)
}

private fun KSerialClassDesc.outputKind(index: Int, childInfo: ChildInfo): OutputKind {
    // lists will always be elements
    if (index < associatedFieldsCount) {
        getAnnotationsForIndex(index).forEach {
            if (it is XmlChildrenName) return OutputKind.Element
            if (it is XmlElement) return if (it.value) OutputKind.Element else OutputKind.Attribute
            if (it is XmlValue && it.value) return OutputKind.Text
        }
    }

    return outputKind(childInfo)
}

private fun KSerialClassDesc.outputKind(baseInfo: BaseInfo? = null): OutputKind {

    val childType = when {
        baseInfo == null -> null
        else             -> baseInfo.type
    }

    return when (childType) {
        ChildType.ELEMENT -> OutputKind.Element
        null,
        ChildType.UNKNOWN -> OutputKind.Unknown
        else              -> OutputKind.Attribute
    }
}

private fun KSerialClassDesc.isOptional(index: Int): Boolean {
    return getAnnotationsForIndex(index).firstOrNull<Optional>() != null
}

fun QName.copy(namespaceURI: String = this.namespaceURI,
                                 localPart: String = this.localPart,
                                 prefix: String = this.prefix) = QName(namespaceURI, localPart,
                                                                                         prefix)

fun QName.copy(prefix: String = this.prefix) = if (prefix == this.prefix) this else QName(
    namespaceURI, localPart,
    prefix)

inline fun <reified T : Any> T.writeAsXML(out: XmlWriter) = XML().toXml(this, out)

@Suppress("NOTHING_TO_INLINE")
inline fun <T : Any> T.writeAsXML(kClass: KClass<T>, out: XmlWriter) = XML().toXml(kClass, this, out)
