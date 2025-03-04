/*
 * Copyright (c) 2025.
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

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.impl.CompatLock
import nl.adaptivity.xmlutil.serialization.structure.SafeParentInfo
import nl.adaptivity.xmlutil.serialization.structure.TypePreserveSpace
import nl.adaptivity.xmlutil.serialization.structure.XmlCompositeDescriptor
import nl.adaptivity.xmlutil.serialization.structure.XmlDescriptor
import nl.adaptivity.xmlutil.serialization.structure.XmlTypeDescriptor

public class LayeredCache private constructor(
    private val baseCache: DefaultFormatCache
): FormatCache() {

    public constructor() : this(DefaultFormatCache())

    private val lock = CompatLock()

    override fun copy(): FormatCache {
        return LayeredCache(baseCache.copy())
    }

    private fun unsafeCache(): FormatCache {
        return Layer(baseCache)
    }

    override fun <R> useUnsafe(action: (FormatCache) -> R): R {
        val cache = Layer(baseCache)

        return action(cache).also {
            lock.invoke {
                baseCache.appendFrom(cache.extCache)
            }
        }
    }

    override fun lookupTypeOrStore(
        namespace: Namespace?,
        serialDesc: SerialDescriptor,
        defaultValue: () -> XmlTypeDescriptor
    ): XmlTypeDescriptor {
        return unsafeCache().lookupTypeOrStore(namespace, serialDesc, defaultValue)
    }

    override fun lookupTypeOrStore(
        parentName: QName,
        serialDesc: SerialDescriptor,
        defaultValue: () -> XmlTypeDescriptor
    ): XmlTypeDescriptor {
        return unsafeCache().lookupTypeOrStore(parentName, serialDesc, defaultValue)
    }

    override fun lookupDescriptorOrStore(
        overridenSerializer: KSerializer<*>?,
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        canBeAttribute: Boolean,
        defaultValue: () -> XmlDescriptor
    ): XmlDescriptor {
        return unsafeCache().lookupDescriptorOrStore(overridenSerializer, serializerParent, tagParent, canBeAttribute, defaultValue)
    }

    override fun getCompositeDescriptor(
        codecConfig: XML.XmlCodecConfig,
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        preserveSpace: TypePreserveSpace
    ): XmlCompositeDescriptor {
        return XmlCompositeDescriptor(codecConfig, serializerParent, tagParent, preserveSpace)
    }

    private class Layer constructor(
        val base: DelegatableFormatCache,
        val extCache: DefaultFormatCache = DefaultFormatCache()
    ) : FormatCache() {

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

        override fun copy(): FormatCache {
            return Layer(base, extCache.copy())
        }

        override fun <R> useUnsafe(action: (FormatCache) -> R): R = action(this)

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

        override fun getCompositeDescriptor(
            codecConfig: XML.XmlCodecConfig,
            serializerParent: SafeParentInfo,
            tagParent: SafeParentInfo,
            preserveSpace: TypePreserveSpace
        ): XmlCompositeDescriptor {
            return extCache.getCompositeDescriptor(codecConfig, serializerParent, tagParent, preserveSpace)
        }
    }
}
