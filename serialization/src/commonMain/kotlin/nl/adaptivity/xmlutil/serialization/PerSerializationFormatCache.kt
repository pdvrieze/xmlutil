/*
 * Copyright (c) 2026.
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
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.structure.*

@ExperimentalXmlUtilApi
/**
 * A format cache wrapper that creates a new cache for each serialization invocation. This should
 * help with complex formats where longer term caching is not desirable. The only worthwhile
 * implementation is in the `useUnsafe` function that allows the action to be done on the base.
 * This allows the serialization format to use a single cache throughout.
 */
public class PerSerializationFormatCache(private val baseCacheGen: () -> FormatCache): FormatCache {
    override fun lookupTypeOrStore(
        namespace: Namespace?,
        serialDesc: SerialDescriptor,
        defaultValue: () -> XmlTypeDescriptor
    ): XmlTypeDescriptor {
        return defaultValue()
    }

    override fun lookupTypeOrStore(
        parentName: QName,
        serialDesc: SerialDescriptor,
        defaultValue: () -> XmlTypeDescriptor
    ): XmlTypeDescriptor {
        return defaultValue()
    }

    override fun copy(): PerSerializationFormatCache = this

    override fun <R> useUnsafe(action: (FormatCache) -> R): R {
        return action(baseCacheGen())
    }

    override fun lookupDescriptorOrStore(
        overridenSerializer: KSerializer<*>?,
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        canBeAttribute: Boolean,
        defaultValue: () -> XmlDescriptor
    ): XmlDescriptor {
        return defaultValue()
    }

    override fun getCompositeDescriptor(
        codecConfig: XML.XmlCodecConfig,
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        preserveSpace: Boolean
    ): XmlCompositeDescriptor {
        return XmlCompositeDescriptor(codecConfig, serializerParent, tagParent, preserveSpace)
    }
}
