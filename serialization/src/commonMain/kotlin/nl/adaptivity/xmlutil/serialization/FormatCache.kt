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

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.structure.SafeParentInfo
import nl.adaptivity.xmlutil.serialization.structure.XmlCompositeDescriptor
import nl.adaptivity.xmlutil.serialization.structure.XmlDescriptor
import nl.adaptivity.xmlutil.serialization.structure.XmlTypeDescriptor

public abstract class FormatCache internal constructor(){
    internal abstract fun lookupType(namespace: Namespace?, serialDesc: SerialDescriptor, defaultValue: () -> XmlTypeDescriptor): XmlTypeDescriptor

    /**
     * Lookup a type descriptor for this type with the given namespace.
     * @param parentName A key
     */
    internal abstract fun lookupType(parentName: QName, serialDesc: SerialDescriptor, defaultValue: () -> XmlTypeDescriptor): XmlTypeDescriptor

    @OptIn(ExperimentalSerializationApi::class)
    internal abstract fun lookupType(name: QName, kind: SerialKind, defaultValue: () -> XmlTypeDescriptor): XmlTypeDescriptor

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
        preserveSpace: Boolean,
    ): XmlCompositeDescriptor

    internal object Dummy: FormatCache() {
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

        @OptIn(ExperimentalSerializationApi::class)
        override fun lookupType(
            name: QName,
            kind: SerialKind,
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
            preserveSpace: Boolean
        ): XmlCompositeDescriptor = XmlCompositeDescriptor(codecConfig, serializerParent, tagParent, preserveSpace)
    }
}

