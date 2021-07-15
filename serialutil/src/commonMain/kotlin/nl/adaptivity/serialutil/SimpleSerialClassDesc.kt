/*
 * Copyright (c) 2019.
 *
 * This file is part of xmlutil.
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

package nl.adaptivity.serialutil

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.serialutil.impl.arrayMap
import nl.adaptivity.serialutil.impl.maybeAnnotations
import nl.adaptivity.serialutil.impl.name
import kotlin.jvm.JvmName

@Deprecated("Use the standard library buildSerialDescriptor function")
@OptIn(InternalSerializationApi::class)
@ExperimentalSerializationApi
inline fun <reified T> simpleSerialClassDesc(
    kind: SerialKind,
    vararg elements: Pair<String, SerialDescriptor>
): SerialDescriptor {
    return buildSerialDescriptor(T::class.name, kind) {
        elements.forEach { (name, desc) -> element(name, desc) }
    }
}

@Deprecated("Use the standard library buildSerialDescriptor function")
@OptIn(InternalSerializationApi::class)
@ExperimentalSerializationApi
inline fun <reified T> simpleSerialClassDesc(
    kind: SerialKind,
    entityAnnotations: List<Annotation>,
    vararg elements: Pair<String, SerialDescriptor>
): SerialDescriptor {
    return buildSerialDescriptor(T::class.name, kind) {
        annotations = entityAnnotations
        elements.forEach { (name, desc) -> element(name, desc) }
    }
}

@Deprecated("Use the standard library buildClassSerialDescriptor function")
@ExperimentalSerializationApi
inline fun <reified T> simpleSerialClassDesc(): SerialDescriptor {
    return buildClassSerialDescriptor(T::class.name) {
        annotations = T::class.maybeAnnotations
    }
}

@Deprecated("Use the standard library buildClassSerialDescriptor function")
@ExperimentalSerializationApi
@JvmName("simpleSerialClassDescFromSerializer")
inline fun <reified T> simpleSerialClassDesc(vararg elements: Pair<String, KSerializer<*>>): SerialDescriptor {
    return buildClassSerialDescriptor(T::class.name) {
        annotations = T::class.maybeAnnotations
        for (e in elements) {
            element(e.first, e.second.descriptor)
        }
    }
}

@ExperimentalSerializationApi
@JvmName("simpleSerialClassDescFromSerializer")
inline fun <reified T> simpleSerialClassDesc(
    entityAnnotations: List<Annotation>,
    vararg elements: Pair<String, KSerializer<*>>
): SerialDescriptor {
    return buildClassSerialDescriptor(T::class.name) {
        annotations = entityAnnotations
        for (e in elements) {
            element(e.first, e.second.descriptor)
        }
    }
}

@Deprecated(
    "Now supported by the standard library",
    ReplaceWith(
        "PrimitiveSerialDescriptor(name, kind)",
        "kotlinx.serialization.descriptors.PrimitiveSerialDescriptor"
    ),
    DeprecationLevel.HIDDEN
)
@ExperimentalSerializationApi
class SimpleSerialClassDescPrimitive(override val kind: PrimitiveKind, name: String) : SerialDescriptor {
    override val serialName: String = name

    override val elementsCount: Int get() = 0

    override fun getElementIndex(name: String) = CompositeDecoder.UNKNOWN_NAME

    override fun getElementAnnotations(index: Int): Nothing = throw IllegalStateException("No Children")

    override fun getElementName(index: Int): Nothing = throw IllegalStateException("No Children")

    override fun getElementDescriptor(index: Int): Nothing = throw IllegalStateException("No Children")

    override fun isElementOptional(index: Int): Boolean = false
}

/**
 * Simple impplementation of SerialClassDesc. It is used by the serialization code
 * as well, so exported, but not designed for use outside the xmlutil project.
 */
@Deprecated("This class is no longer needed, it can be replaced by buildSerialDescriptor and buildClassSerialDescriptor")
@ExperimentalSerializationApi
class SimpleSerialClassDesc(
    override val kind: SerialKind = StructureKind.CLASS,
    name: String,
    override val annotations: List<Annotation>,
    vararg val elements: Pair<String, SerialDescriptor>
) : SerialDescriptor {

    constructor(
        name: String,
        kind: SerialKind = StructureKind.CLASS,
        entityAnnotations: List<Annotation>,
        vararg elements: Pair<String, KSerializer<*>>
    ) : this(kind, name, entityAnnotations, *(elements.arrayMap { it.first to it.second.descriptor }))

    override val serialName: String = name

    override fun getElementIndex(name: String): Int {
        val index = elements.indexOfFirst { it.first == name }
        return when {
            index >= 0 -> index
            else -> CompositeDecoder.UNKNOWN_NAME
        }
    }

    override fun getElementDescriptor(index: Int): SerialDescriptor {
        return elements[index].second
    }

    override fun getElementName(index: Int) = elements[index].first

    override fun getElementAnnotations(index: Int): List<Annotation> {
        if (index < 0 || index > elements.size) throw IndexOutOfBoundsException(index.toString())
        return emptyList()
    }

    override fun isElementOptional(index: Int): Boolean = false

    override val elementsCount: Int get() = elements.size
}

@ExperimentalSerializationApi
fun SerialDescriptor.withName(name: String): SerialDescriptor = RenameDesc(this, name)

@ExperimentalSerializationApi
private class RenameDesc(val delegate: SerialDescriptor, override val serialName: String) : SerialDescriptor by delegate

abstract class DelegateSerializer<T>(val delegate: KSerializer<T>) : KSerializer<T> {
    override val descriptor: SerialDescriptor get() = delegate.descriptor

    override fun deserialize(decoder: Decoder): T = delegate.deserialize(decoder)

    override fun serialize(encoder: Encoder, value: T) = delegate.serialize(encoder, value)
}
