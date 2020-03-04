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

package nl.adaptivity.xmlutil.serialization.canary

import kotlinx.serialization.*
import kotlinx.serialization.internal.GeneratedSerializer
import kotlinx.serialization.modules.EmptyModule
import kotlinx.serialization.modules.SerialModule
import nl.adaptivity.xmlutil.serialization.impl.arrayMap
import kotlin.collections.set

val <T> DeserializationStrategy<T>.polymorphicDescriptor: SerialDescriptor
    get() {
        return when {
            this.descriptor.isNullable -> NullableSerialDescriptor(nonNullableDeserializer.polymorphicDescriptor)
            this is PolymorphicSerializer<*> -> PolymorphicParentDescriptor(this)
            else                        -> descriptor
        }
    }

private val DeserializationStrategy<*>.nonNullableDeserializer: DeserializationStrategy<*> get() {
    val canary = LoaderCanaryBase()
    deserialize(canary)
    return canary.actualDeserializer
}

fun DeserializationStrategy<*>.getChildDeserializer(index: Int): DeserializationStrategy<*> {
    val canary = ChildDeserializerCanary(index)
    try {
        deserialize(canary)
    } catch (e : RuntimeException) {}
    return canary.actualDeserializer
}

private open class LoaderCanaryBase: Decoder, CompositeDecoder {

    override val context: SerialModule get() = EmptyModule
    override val updateMode: UpdateMode get() = UpdateMode.BANNED
    lateinit var actualDeserializer: DeserializationStrategy<*>

    override fun <T : Any> decodeNullableSerializableElement(
        desc: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>
                                                            ): T? {
        return decodeSerializableElement(desc, index, deserializer)
    }

    override fun <T> decodeSerializableElement(
        desc: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>
                                              ): T {
        actualDeserializer = deserializer
        return null as T
    }

    override fun <T : Any> updateNullableSerializableElement(
        desc: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        old: T?
                                                            ): T? {
        return updateSerializableElement(desc, index, deserializer, old)
    }

    override fun <T> updateSerializableElement(
        desc: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        old: T
                                              ): T {
        return decodeSerializableElement(desc, index, deserializer)
    }

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
        return this
    }

    override fun endStructure(desc: SerialDescriptor) {}

    override fun decodeElementIndex(desc: SerialDescriptor): Int {
        throw UnsupportedOperationException("Not valid here")
    }

    override fun decodeBoolean(): Nothing
            = throw UnsupportedOperationException("Not valid here")

    override fun decodeByte(): Nothing
            = throw UnsupportedOperationException("Not valid here")

    override fun decodeChar(): Nothing
            = throw UnsupportedOperationException("Not valid here")

    override fun decodeDouble(): Nothing
            = throw UnsupportedOperationException("Not valid here")

    override fun decodeEnum(enumDescription: SerialDescriptor): Nothing
            = throw UnsupportedOperationException("Not valid here")

    override fun decodeFloat(): Nothing
            = throw UnsupportedOperationException("Not valid here")

    override fun decodeInt(): Nothing
            = throw UnsupportedOperationException("Not valid here")

    override fun decodeLong(): Nothing
            = throw UnsupportedOperationException("Not valid here")

    override fun decodeNotNullMark(): Boolean = true
    override fun decodeNull(): Nothing
            = throw UnsupportedOperationException("Not valid here")

    override fun decodeShort(): Nothing
            = throw UnsupportedOperationException("Not valid here")

    override fun decodeString(): Nothing
            = throw UnsupportedOperationException("Not valid here")

    override fun decodeUnit(): Nothing
            = throw UnsupportedOperationException("Not valid here")

    override fun decodeBooleanElement(desc: SerialDescriptor, index: Int): Nothing
            = throw UnsupportedOperationException("Not valid here")

    override fun decodeByteElement(desc: SerialDescriptor, index: Int): Nothing
            = throw UnsupportedOperationException("Not valid here")

    override fun decodeCharElement(desc: SerialDescriptor, index: Int): Nothing
            = throw UnsupportedOperationException("Not valid here")

    override fun decodeDoubleElement(desc: SerialDescriptor, index: Int): Nothing
            = throw UnsupportedOperationException("Not valid here")

    override fun decodeFloatElement(desc: SerialDescriptor, index: Int): Nothing
            = throw UnsupportedOperationException("Not valid here")

    override fun decodeIntElement(desc: SerialDescriptor, index: Int): Nothing
            = throw UnsupportedOperationException("Not valid here")

    override fun decodeLongElement(desc: SerialDescriptor, index: Int): Nothing
            = throw UnsupportedOperationException("Not valid here")

    override fun decodeShortElement(desc: SerialDescriptor, index: Int): Nothing
            = throw UnsupportedOperationException("Not valid here")

    override fun decodeStringElement(desc: SerialDescriptor, index: Int): Nothing
            = throw UnsupportedOperationException("Not valid here")

    override fun decodeUnitElement(desc: SerialDescriptor, index: Int): Nothing
            = throw UnsupportedOperationException("Not valid here")
}

object Canary {

    private val saverMap = mutableMapOf<SerializationStrategy<*>, ExtSerialDescriptor>()

    private val loaderMap = mutableMapOf<DeserializationStrategy<*>, ExtSerialDescriptor>()

    @UseExperimental(InternalSerializationApi::class)
    fun <T> serialDescriptor(saver: SerializationStrategy<T>, @Suppress("UNUSED_PARAMETER") obj: T): ExtSerialDescriptor {
        val current = saverMap[saver]?.also { return it }
        if (current != null) return current
        if (saver is GeneratedSerializer) {
            return ExtSerialDescriptorImpl(saver.descriptor,
                                           saver.childSerializers().arrayMap { it.polymorphicDescriptor }).also {
                saverMap[saver] = it
            }
        }
        val parentDesc = saver.descriptor
        try {
            val childDescs = Array(parentDesc.elementsCount) { parentDesc.getElementDescriptor(it) }
            return ExtSerialDescriptorImpl(parentDesc, childDescs).also { saverMap[saver] = it }
        } catch (e: SerializationException) {
            return ExtSerialDescriptorImpl(parentDesc, emptyArray()).also { saverMap[saver] = it }
        }
    }

    @UseExperimental(InternalSerializationApi::class)
    fun <T> serialDescriptor(loader: DeserializationStrategy<T>): ExtSerialDescriptor {
        loaderMap[loader]?.let { return it }

        val result = when {
            loader is PolymorphicSerializer<*>
            -> PolymorphicParentDescriptor(loader)

            loader is GeneratedSerializer
            -> ExtSerialDescriptorImpl(
                loader.descriptor,
                loader.childSerializers().arrayMap { serialDescriptor(it) })

            loader.descriptor.kind== StructureKind.LIST
            -> {
                val elementLoader = probeElementLoader(loader)
                ExtSerialDescriptorImpl(loader.descriptor, arrayOf(serialDescriptor(elementLoader)))
            }

            else -> {
                val parentDesc = loader.descriptor
                val childDescs = try {
                    Array(parentDesc.elementsCount) { parentDesc.getElementDescriptor(it) }
                } catch (e: SerializationException) {
                    emptyArray<SerialDescriptor>()
                }
                ExtSerialDescriptorImpl(parentDesc, childDescs)

            }

        }
        loaderMap[loader] = result
        return result
    }

    private fun probeElementLoader(loader: DeserializationStrategy<*>): DeserializationStrategy<*> {
        val canaryDecoder = CollectionElementLoaderCanary()
        loader.deserialize(canaryDecoder)
        return canaryDecoder.actualDeserializer
    }

    fun <T> pollDesc(saver: SerializationStrategy<T>): ExtSerialDescriptor? {
        return saverMap[saver]
    }

    internal fun registerDesc(saver: SerializationStrategy<*>, desc: ExtSerialDescriptor) {
        saverMap[saver] = desc
    }

    internal fun registerDesc(loader: DeserializationStrategy<*>, desc: ExtSerialDescriptor) {
        loaderMap[loader] = desc
    }

    fun <T> pollDesc(loader: DeserializationStrategy<T>): ExtSerialDescriptor? {
        return loaderMap[loader]
    }


    private class CollectionElementLoaderCanary : LoaderCanaryBase() {
        var nextIdx = 0

        override fun decodeCollectionSize(desc: SerialDescriptor): Int {
            return 1 // always one element
        }

        override fun decodeElementIndex(desc: SerialDescriptor): Int {
            if (nextIdx>=desc.elementsCount) return CompositeDecoder.READ_DONE
            return nextIdx++
        }

    }

}

private class ChildDeserializerCanary (val targetIndex: Int): LoaderCanaryBase(){
    override fun <T> decodeSerializableElement(
        desc: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>
                                              ): T {
        super.decodeSerializableElement(desc, index, deserializer)
        throw RuntimeException("Just a way to stop coding")
    }

    override fun decodeElementIndex(desc: SerialDescriptor): Int {
        return targetIndex
    }
}