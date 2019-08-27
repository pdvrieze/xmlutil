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
import nl.adaptivity.xmlutil.serialization.XmlDefault
import kotlin.reflect.KClass

interface ExtSerialDescriptor : SerialDescriptor {
    fun getSafeElementDescriptor(index: Int): SerialDescriptor?
}

/**
 * An implementation of SerialDescriptor
 *
 * @param base The serial class descriptor that is the basis. This is used for delegation so not directly accessible
 * @param childDescriptors An array with all child descriptors
 */
internal class ExtSerialDescriptorImpl(
    private val base: SerialDescriptor,
    private val childDescriptors: Array<SerialDescriptor>
                                      ) : ExtSerialDescriptor {

    override val isNullable: Boolean get() = base.isNullable

    override val name: String get() = base.name
    override val kind: SerialKind get() = base.kind

    override fun getElementName(index: Int): String = base.getElementName(index)
    override fun getElementIndex(name: String): Int = base.getElementIndex(name)

    override fun getEntityAnnotations(): List<Annotation> = base.getEntityAnnotations()

    override fun getElementAnnotations(index: Int) =
        if (index < elementsCount) base.getElementAnnotations(index) else emptyList()

    override val elementsCount: Int get() = base.elementsCount

    override fun getElementDescriptor(index: Int): SerialDescriptor = childDescriptors[index]

    override fun getSafeElementDescriptor(index: Int): SerialDescriptor? = when {
        index < childDescriptors.size -> childDescriptors[index]
        else                          -> null
    }

    override fun isElementOptional(index: Int): Boolean = getElementAnnotations(index).any { it is XmlDefault }

    override fun toString(): String {
        return buildString {
            append(name)
            (0 until elementsCount).joinTo(this, prefix = "(", postfix = ")") { idx ->
                val elemDesc = try {
                    getElementDescriptor(idx)
                } catch (e: Exception) {
                    null
                }
                "${getElementName(idx)}:${elemDesc?.name}${if (elemDesc?.isNullable == true) "?" else ""}"
            }
        }
    }
}

class NullableSerialDescriptor(val original: SerialDescriptor) : SerialDescriptor by original {
    override val isNullable: Boolean get() = true
}


internal class PolymorphicParentDescriptor(private val base: SerialDescriptor, val baseClass: KClass<*>) :
    SerialDescriptor by base, ExtSerialDescriptor {
    constructor(deserializer: PolymorphicSerializer<*>) : this(deserializer.descriptor, deserializer.baseClass)

    override fun getSafeElementDescriptor(index: Int): SerialDescriptor? {
        return base.getSafeElementDescriptor(index)
    }
}

fun SerialDescriptor.getSafeElementDescriptor(index: Int): SerialDescriptor? = when (this) {
    is PolymorphicClassDescriptor -> null
    is ExtSerialDescriptor -> this.getSafeElementDescriptor(index)
    else                          -> try {
        getElementDescriptor(index)
    } catch (e: SerializationException) {
        null
    }
}