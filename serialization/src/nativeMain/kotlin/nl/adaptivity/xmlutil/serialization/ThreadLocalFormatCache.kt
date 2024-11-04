/*
 * Copyright (c) 2024.
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
import nl.adaptivity.xmlutil.serialization.structure.SafeParentInfo
import nl.adaptivity.xmlutil.serialization.structure.XmlCompositeDescriptor
import nl.adaptivity.xmlutil.serialization.structure.XmlDescriptor
import nl.adaptivity.xmlutil.serialization.structure.XmlTypeDescriptor
import kotlin.native.concurrent.ThreadLocal


@ThreadLocal
private var threadLocalFormatCache: ArrayDeque<Pair<ThreadLocalFormatCache, FormatCache>>? = null

/**
 * Native implementation of a format cache using threadLocals. Note that this uses a fifo queue of
 * caches (up to [capacity] count) and it will evict the oldest element (independent of usage)
 */
public class ThreadLocalFormatCache(
    public val capacity: Int = 20,
    private val baseCacheFactory: () -> FormatCache = { DefaultFormatCache() }
) : FormatCache() {

    private val threadLocal: FormatCache
        get() {
            val cache: ArrayDeque<Pair<ThreadLocalFormatCache, FormatCache>> = threadLocalFormatCache?.apply {
                firstOrNull { it.first == this@ThreadLocalFormatCache }?.let { return it.second }
            } ?: ArrayDeque<Pair<ThreadLocalFormatCache, FormatCache>>().also { threadLocalFormatCache = it }

            val formatCache = baseCacheFactory()
            cache.addLast(this to formatCache)
            if (cache.size > capacity) { cache.removeFirst() }
            return formatCache
        }

    override fun copy(): ThreadLocalFormatCache {
        return ThreadLocalFormatCache(capacity, baseCacheFactory)
    }

    override fun unsafeCache(): FormatCache {
        return threadLocal
    }

    override fun lookupType(
        namespace: Namespace?,
        serialDesc: SerialDescriptor,
        defaultValue: () -> XmlTypeDescriptor
    ): XmlTypeDescriptor {
        return threadLocal.lookupType(namespace, serialDesc, defaultValue)
    }

    override fun lookupType(
        parentName: QName,
        serialDesc: SerialDescriptor,
        defaultValue: () -> XmlTypeDescriptor
    ): XmlTypeDescriptor {
        return threadLocal.lookupType(parentName, serialDesc, defaultValue)
    }

    override fun lookupDescriptor(
        overridenSerializer: KSerializer<*>?,
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        canBeAttribute: Boolean,
        defaultValue: () -> XmlDescriptor
    ): XmlDescriptor {
        return threadLocal.lookupDescriptor(overridenSerializer, serializerParent, tagParent, canBeAttribute, defaultValue)
    }

    override fun getCompositeDescriptor(
        codecConfig: XML.XmlCodecConfig,
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        preserveSpace: Boolean
    ): XmlCompositeDescriptor {
        return threadLocal.getCompositeDescriptor(codecConfig, serializerParent, tagParent, preserveSpace)
    }
}
