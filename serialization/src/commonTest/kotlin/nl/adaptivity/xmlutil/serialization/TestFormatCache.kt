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

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.structure.*
import kotlin.test.assertEquals

/**
 * Opaque caching class that allows for caching format related data (to speed up reuse). This is
 * intended to be stored on the config, thus reused through multiple serializations.
 * Note that this requires the `serialName` attribute of `SerialDescriptor` instances to be unique.
 */
public class TestFormatCache(private val nested: FormatCache) : FormatCache() {

    override fun copy(): TestFormatCache = TestFormatCache(nested.copy())

    override fun <R> useUnsafe(action: (FormatCache) -> R): R {
        return action(this)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun lookupTypeOrStore(
        namespace: Namespace?,
        serialDesc: SerialDescriptor,
        defaultValue: () -> XmlTypeDescriptor
    ): XmlTypeDescriptor {
        val actual = nested.lookupTypeOrStore(namespace, serialDesc, defaultValue)
        val expected = defaultValue()
        assertEquals(expected, actual, "Cache mismatch found looking up type")

        return actual
    }

    /**
     * Lookup a type descriptor for this type with the given namespace.
     * @param parentName A key
     */
    @OptIn(ExperimentalSerializationApi::class)
    override fun lookupTypeOrStore(
        parentName: QName,
        serialDesc: SerialDescriptor,
        defaultValue: () -> XmlTypeDescriptor
    ): XmlTypeDescriptor {
        val expected = defaultValue()
        val actual = nested.lookupTypeOrStore(parentName, serialDesc, defaultValue)
        assertEquals(expected, actual, "Cache mismatch found looking up type")

        return actual
    }


    override fun lookupDescriptorOrStore(
        overridenSerializer: KSerializer<*>?,
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        canBeAttribute: Boolean,
        defaultValue: () -> XmlDescriptor
    ): XmlDescriptor {
        val expected = defaultValue()
        val actual = nested.lookupDescriptorOrStore(overridenSerializer, serializerParent, tagParent, canBeAttribute, defaultValue)
        assertEquals(expected, actual, "Cache mismatch found looking up descriptor")

        return actual
    }

    override fun getCompositeDescriptor(
        codecConfig: XML.XmlCodecConfig,
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        preserveSpace: TypePreserveSpace
    ): XmlCompositeDescriptor {
        return nested.getCompositeDescriptor(codecConfig, serializerParent, tagParent, preserveSpace)
    }
}
