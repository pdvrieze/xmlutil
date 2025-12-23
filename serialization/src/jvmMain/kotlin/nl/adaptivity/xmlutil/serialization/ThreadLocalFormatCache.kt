/*
 * Copyright (c) 2024-2025.
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
import nl.adaptivity.xmlutil.serialization.structure.*
import java.lang.ref.SoftReference
import java.util.function.Supplier

public class ThreadLocalFormatCache(private val baseCacheFactory: Supplier<FormatCache> = Supplier { DefaultFormatCache() }) : FormatCache() {

    private var threadLocal = ThreadLocal.withInitial(Weaken(baseCacheFactory))

    override fun copy(): ThreadLocalFormatCache {
        return ThreadLocalFormatCache(baseCacheFactory)
    }

    private fun unsafeCache(): FormatCache {
        var f: FormatCache? = threadLocal.get().get()
        while (f == null) {
            threadLocal.remove()
            f = threadLocal.get().get()
        }
        return f
    }

    override fun <R> useUnsafe(action: (FormatCache) -> R): R {
        return action(unsafeCache())
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
        return unsafeCache().lookupDescriptorOrStore(
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
        return unsafeCache().getCompositeDescriptor(codecConfig, serializerParent, tagParent, preserveSpace)
    }

    private class Weaken(private val f: Supplier<FormatCache>) : Supplier<SoftReference<FormatCache>> {
        override fun get() = SoftReference(f.get())
    }
}
