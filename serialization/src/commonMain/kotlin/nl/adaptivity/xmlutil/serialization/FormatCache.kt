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
import nl.adaptivity.xmlutil.serialization.FormatCache.Dummy
import nl.adaptivity.xmlutil.serialization.structure.*

/**
 * The FormatCache caches the calculations needed to determine the correct format for a specific
 * serializable tree. There are 3 options provided by default:
 *
 * - [Dummy] This is a cache that doesn't actually cache, it disables caching
 * - [DefaultFormatCache] This is a simple cache that does **not** use any locking enabling thread safety
 * - [defaultSharedFormatCache] This function will provide wrapper for the default cache that
 *   uses threadLocals for native/jvm/android to provide thread safety (at the cost of performance). Note
 *   that the native implementation is not particularly in the case of individual formats.
 *
 */
public abstract class FormatCache internal constructor() {
    internal abstract fun lookupType(namespace: Namespace?, serialDesc: SerialDescriptor, defaultValue: () -> XmlTypeDescriptor): XmlTypeDescriptor

    internal abstract fun copy(): FormatCache

    /** Retrieve a cache implementation that is not thread safe. Used by the format to avoid looking up thread locals. */
    internal abstract fun unsafeCache(): FormatCache

    /**
     * Lookup a type descriptor for this type with the given namespace.
     * @param parentName A key
     */
    internal abstract fun lookupType(parentName: QName, serialDesc: SerialDescriptor, defaultValue: () -> XmlTypeDescriptor): XmlTypeDescriptor

    internal abstract fun lookupDescriptor(
        overridenSerializer: KSerializer<*>?,
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        canBeAttribute: Boolean,
        defaultValue: () -> XmlDescriptor
    ): XmlDescriptor

    internal abstract fun getCompositeDescriptor(
        codecConfig: XML.XmlCodecConfig,
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        preserveSpace: TypePreserveSpace,
    ): XmlCompositeDescriptor

    public object Dummy: FormatCache() {

        override fun copy(): FormatCache = this

        override fun unsafeCache(): Dummy = this

        override fun lookupType(
            namespace: Namespace?,
            serialDesc: SerialDescriptor,
            defaultValue: () -> XmlTypeDescriptor
        ): XmlTypeDescriptor = defaultValue()

        override fun lookupType(
            parentName: QName,
            serialDesc: SerialDescriptor,
            defaultValue: () -> XmlTypeDescriptor
        ): XmlTypeDescriptor = defaultValue()

        override fun lookupDescriptor(
            overridenSerializer: KSerializer<*>?,
            serializerParent: SafeParentInfo,
            tagParent: SafeParentInfo,
            canBeAttribute: Boolean,
            defaultValue: () -> XmlDescriptor
        ): XmlDescriptor = defaultValue()

        override fun getCompositeDescriptor(
            codecConfig: XML.XmlCodecConfig,
            serializerParent: SafeParentInfo,
            tagParent: SafeParentInfo,
            preserveSpace: TypePreserveSpace
        ): XmlCompositeDescriptor = XmlCompositeDescriptor(codecConfig, serializerParent, tagParent, preserveSpace)
    }
}

/**
 * Get an instance of the default format cache that where supported uses threadlocals for thread safety.
 */
public expect fun defaultSharedFormatCache(): FormatCache
