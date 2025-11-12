/*
 * Copyright (c) 2025.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.namespaceURI
import nl.adaptivity.xmlutil.serialization.impl.CompatLock
import nl.adaptivity.xmlutil.serialization.structure.*

public class LayeredCache private constructor(
    private var baseCache: DelegatableFormatCache
): FormatCache() {

    public constructor() : this(DefaultFormatCache())

    private val lock = CompatLock()

    override fun copy(): FormatCache {
        val cpy = lock.invoke {
            baseCache.copy()
        }
        return LayeredCache(cpy)
    }

    private fun unsafeCache(): AbstractLayer {
        return when (val b = baseCache) {
            is DefaultFormatCache -> DefaultLayer(b)
            else -> FallbackLayer(b)
        }
    }

    override fun <R> useUnsafe(action: (FormatCache) -> R): R {
        val cache = lock.invoke { unsafeCache() }

        return action(cache).also {
            lock.invoke {
                baseCache = baseCache.appendFrom(cache.extCache)
            }
        }
    }

    override fun lookupTypeOrStore(
        namespace: Namespace?,
        serialDesc: SerialDescriptor,
        defaultValue: () -> XmlTypeDescriptor
    ): XmlTypeDescriptor {
        return when (val b = baseCache) {
            is DefaultFormatCache -> lock.invoke { b.lookupTypeOrStore(namespace, serialDesc, defaultValue) }
            else -> useUnsafe { it.lookupTypeOrStore(namespace, serialDesc, defaultValue) }
        }
    }

    override fun lookupTypeOrStore(
        parentName: QName,
        serialDesc: SerialDescriptor,
        defaultValue: () -> XmlTypeDescriptor
    ): XmlTypeDescriptor {
        return when (val b = baseCache) {
            is DefaultFormatCache -> lock.invoke { b.lookupTypeOrStore(parentName, serialDesc, defaultValue) }
            else -> useUnsafe { it.lookupTypeOrStore(parentName, serialDesc, defaultValue) }
        }
    }

    override fun lookupDescriptorOrStore(
        overridenSerializer: KSerializer<*>?,
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        canBeAttribute: Boolean,
        defaultValue: () -> XmlDescriptor
    ): XmlDescriptor {
        return when (val b = baseCache) {
            is DefaultFormatCache -> lock.invoke {
                b.lookupDescriptorOrStore(overridenSerializer, serializerParent, tagParent, canBeAttribute, defaultValue)
            }

            else -> useUnsafe {
                it.lookupDescriptorOrStore(overridenSerializer, serializerParent, tagParent, canBeAttribute, defaultValue)
            }
        }
    }

    override fun getCompositeDescriptor(
        codecConfig: XML.XmlCodecConfig,
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        preserveSpace: TypePreserveSpace
    ): XmlCompositeDescriptor {
        return XmlCompositeDescriptor(codecConfig, serializerParent, tagParent, preserveSpace)
    }

    private abstract class AbstractLayer(
        val extCache: DefaultFormatCache
    ): FormatCache() {
        abstract val base: DelegatableFormatCache
        abstract override fun copy(): AbstractLayer


        final override fun <R> useUnsafe(action: (FormatCache) -> R): R = action(this)

        final override fun getCompositeDescriptor(
            codecConfig: XML.XmlCodecConfig,
            serializerParent: SafeParentInfo,
            tagParent: SafeParentInfo,
            preserveSpace: TypePreserveSpace
        ): XmlCompositeDescriptor {
            return extCache.getCompositeDescriptor(codecConfig, serializerParent, tagParent, preserveSpace)
        }

    }

    private class DefaultLayer constructor(
        override val base: DefaultFormatCache,
        extCache: DefaultFormatCache = DefaultFormatCache()
    ) : AbstractLayer(extCache) {

        override fun lookupTypeOrStore(
            namespace: Namespace?,
            serialDesc: SerialDescriptor,
            defaultValue: () -> XmlTypeDescriptor
        ): XmlTypeDescriptor {
            val key = DefaultFormatCache.TypeKey(namespace?.namespaceURI, serialDesc)

            return base.lookupType(key, serialDesc.kind) ?: extCache.lookupTypeOrStore(
                key,
                serialDesc.kind,
                defaultValue
            )

        }

        override fun lookupTypeOrStore(
            parentName: QName,
            serialDesc: SerialDescriptor,
            defaultValue: () -> XmlTypeDescriptor
        ): XmlTypeDescriptor {
            val key = DefaultFormatCache.TypeKey(parentName.namespaceURI, serialDesc)

            return base.lookupType(key, serialDesc.kind) ?: extCache.lookupTypeOrStore(
                key,
                serialDesc.kind,
                defaultValue
            )

        }

        override fun copy(): DefaultLayer {
            return DefaultLayer(base, extCache.copy())
        }

        override fun lookupDescriptorOrStore(
            overridenSerializer: KSerializer<*>?,
            serializerParent: SafeParentInfo,
            tagParent: SafeParentInfo,
            canBeAttribute: Boolean,
            defaultValue: () -> XmlDescriptor
        ): XmlDescriptor {
            val key = DefaultFormatCache.DescKey(overridenSerializer, serializerParent, tagParent, canBeAttribute)

            return base.lookupDescriptor(key)
                ?: extCache.lookupDescriptorOrStore(key, defaultValue)
        }
    }

    private class FallbackLayer(
        override val base: DelegatableFormatCache,
        extCache: DefaultFormatCache = DefaultFormatCache()
    ) : AbstractLayer(extCache) {

        override fun lookupTypeOrStore(
            namespace: Namespace?,
            serialDesc: SerialDescriptor,
            defaultValue: () -> XmlTypeDescriptor
        ): XmlTypeDescriptor {
            return base.lookupType(namespace, serialDesc) ?: extCache.lookupTypeOrStore(
                namespace,
                serialDesc,
                defaultValue
            )

        }

        override fun lookupTypeOrStore(
            parentName: QName,
            serialDesc: SerialDescriptor,
            defaultValue: () -> XmlTypeDescriptor
        ): XmlTypeDescriptor {
            return base.lookupType(parentName, serialDesc) ?: extCache.lookupTypeOrStore(
                parentName,
                serialDesc,
                defaultValue
            )

        }

        override fun copy(): FallbackLayer {
            return FallbackLayer(base, extCache.copy())
        }

        override fun lookupDescriptorOrStore(
            overridenSerializer: KSerializer<*>?,
            serializerParent: SafeParentInfo,
            tagParent: SafeParentInfo,
            canBeAttribute: Boolean,
            defaultValue: () -> XmlDescriptor
        ): XmlDescriptor {
            return base.lookupDescriptor(overridenSerializer, serializerParent, tagParent, canBeAttribute)
                ?: extCache.lookupDescriptorOrStore(
                    overridenSerializer,
                    serializerParent,
                    tagParent,
                    canBeAttribute,
                    defaultValue
                )
        }

    }
}
