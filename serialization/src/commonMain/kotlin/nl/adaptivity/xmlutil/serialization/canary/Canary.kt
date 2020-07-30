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
import kotlinx.serialization.modules.SerialModule
import kotlinx.serialization.modules.SerialModuleCollector
import nl.adaptivity.xmlutil.serialization.impl.arrayMap
import kotlin.collections.set
import kotlin.reflect.KClass

val <T> DeserializationStrategy<T>.polymorphicDescriptor: SerialDescriptor
    get() {
        return when {
            this.descriptor.isNullable              -> NullableSerialDescriptor(nonNullableDeserializer.polymorphicDescriptor)
            descriptor.kind is PolymorphicKind.OPEN -> PolymorphicParentDescriptor(descriptor, peekBaseClass())
            else                                    -> descriptor
        }
    }

internal fun DeserializationStrategy<*>.peekBaseClass(): KClass<*> = when (this) {
    is PolymorphicSerializer ->
        baseClass
    else                     -> {
        val canary = PolyCanary()
        try {
            this.deserialize(canary)
            throw UnsupportedOperationException("This should not be reached")
        } catch (e: CanaryException) {
        }
        canary.baseClass
    }
}

internal fun <T> SerializationStrategy<T>.peekBaseClassInvalid(): KClass<*> = when (this) {
    is PolymorphicSerializer ->
        baseClass
    else                     -> {
        val canary = PolyCanary()
        try {
            this.serialize(canary, Any() as T)
            throw UnsupportedOperationException("This should not be reached")
        } catch (e: CanaryException) {
        }
        canary.baseClass
    }
}

internal class PolyCanary : CanaryBase() {
    override val context: SerialModule = object : SerialModule {
        override fun <T : Any> getPolymorphic(baseClass: KClass<T>, value: T): Nothing {
            this@PolyCanary.baseClass == baseClass
            throw CanaryException()
        }

        override fun <T : Any> getPolymorphic(baseClass: KClass<T>, serializedClassName: String): Nothing {
            this@PolyCanary.baseClass == baseClass
            throw CanaryException()
        }

        override fun dumpTo(collector: SerialModuleCollector) {
            throw UnsupportedOperationException("Doesn't work in canary mode")
        }

        override fun <T : Any> getContextual(kclass: KClass<T>): KSerializer<T>? {
            throw UnsupportedOperationException("Not supported here")
        }
    }

    lateinit var baseClass: KClass<*>
        private set
}

private val DeserializationStrategy<*>.nonNullableDeserializer: DeserializationStrategy<*>
    get() {
        val canary = CanaryBase()
        deserialize(canary)
        return canary.actualDeserializer
    }

fun DeserializationStrategy<*>.getChildDeserializer(index: Int): DeserializationStrategy<*> {
    val canary = ChildDeserializerCanary(index)
    try {
        deserialize(canary)
    } catch (e: CanaryException) {
    }
    return canary.actualDeserializer
}

object Canary {

    private val saverMap = mutableMapOf<SerializationStrategy<*>, ExtSerialDescriptor>()

    private val loaderMap = mutableMapOf<DeserializationStrategy<*>, ExtSerialDescriptor>()

    @OptIn(InternalSerializationApi::class)
    fun <T> serialDescriptor(
        saver: SerializationStrategy<T>,
        @Suppress("UNUSED_PARAMETER") obj: T
                            ): ExtSerialDescriptor {
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

    @OptIn(InternalSerializationApi::class)
    fun <T> serialDescriptor(loader: DeserializationStrategy<T>): ExtSerialDescriptor {
        loaderMap[loader]?.let { return it }
        // temporarilly set this to a delayed descriptor
        loaderMap[loader] = RecursiveDescriptor(loader)

        val result = when {
            loader.descriptor.isNullable
                 -> ExtNullableSerialDescriptor(serialDescriptor(loader.nonNullableDeserializer))

            loader is PolymorphicSerializer<*>
                 -> PolymorphicParentDescriptor(loader)

            loader is GeneratedSerializer
                 -> ExtSerialDescriptorImpl(
                loader.descriptor,
                loader.childSerializers().arrayMap { serialDescriptor(it) })

            loader.descriptor.kind == StructureKind.LIST
                 -> {
                val elementLoader = probeElementLoader(loader)
                ExtSerialDescriptorImpl(loader.descriptor, arrayOf(serialDescriptor(elementLoader)))
            }

            else -> {
                val parentDesc = loader.descriptor
                val childDescs: Array<SerialDescriptor> = try {
                    Array(parentDesc.elementsCount) { parentDesc.getElementDescriptor(it) }
                } catch (e: SerializationException) {
                    emptyArray()
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


    private class CollectionElementLoaderCanary : CanaryBase() {
        var nextIdx = 0

        override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
            return 1 // always one element
        }

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            if (nextIdx >= descriptor.elementsCount) return CompositeDecoder.READ_DONE
            return nextIdx++
        }

    }

    private class RecursiveDescriptor(private val loader: DeserializationStrategy<*>) : ExtSerialDescriptor {
        val actualDescriptor: ExtSerialDescriptor by lazy {
            val d = loaderMap[loader]!!
            if (d is RecursiveDescriptor) throw IllegalStateException("Resolving descriptor that was unresolved")
            d
        }

        override fun getSafeElementDescriptor(index: Int) = actualDescriptor.getSafeElementDescriptor(index)

        override val elementsCount: Int get() = actualDescriptor.elementsCount

        override val kind: SerialKind get() = actualDescriptor.kind

        override val serialName: String get() = actualDescriptor.serialName

        override fun getElementAnnotations(index: Int) = actualDescriptor.getElementAnnotations(index)

        override fun getElementDescriptor(index: Int): SerialDescriptor = actualDescriptor.getElementDescriptor(index)

        override fun getElementIndex(name: String): Int = actualDescriptor.getElementIndex(name)

        override fun getElementName(index: Int): String = actualDescriptor.getElementName(index)

        override fun isElementOptional(index: Int): Boolean = actualDescriptor.isElementOptional(index)
    }

}

private class ChildDeserializerCanary(val targetIndex: Int) : CanaryBase() {
    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>
                                              ): T {
        super.decodeSerializableElement(descriptor, index, deserializer)
        throw CanaryException("Just a way to stop coding")
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return targetIndex
    }
}

internal class CanaryException(msg: String = "") : RuntimeException(msg)