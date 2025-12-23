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

package nl.adaptivity.xmlutil.serialization.structure

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.serialization.OutputKind
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlConfig
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy
import nl.adaptivity.xmlutil.toNamespace

internal class DetachedParent(
    namespace: Namespace?,
    override val elementTypeDescriptor: XmlTypeDescriptor,
    override val elementUseNameInfo: XmlSerializationPolicy.DeclaredNameInfo,
    val isDocumentRoot: Boolean = false,
    override val elementUseOutputKind: OutputKind? = null,
    override val overriddenSerializer: KSerializer<*>? = null,
) : SafeParentInfo {

    constructor(
        codecConfig: XML.XmlCodecConfig,
        serialDescriptor: SerialDescriptor,
        elementUseNameInfo: XmlSerializationPolicy.DeclaredNameInfo,
        isDocumentRoot: Boolean,
    ) : this(
        namespace = elementUseNameInfo.annotatedName?.toNamespace(),
        elementTypeDescriptor = (elementUseNameInfo.annotatedName?.toNamespace()
            ?: DEFAULT_NAMESPACE).let { namespace ->
            codecConfig.config.formatCache.lookupTypeOrStore(namespace, serialDescriptor) {
                XmlTypeDescriptor(codecConfig.config, serialDescriptor, namespace)
            }
        },
        elementUseNameInfo = elementUseNameInfo,
        isDocumentRoot = isDocumentRoot
    )

    override val namespace: Namespace = namespace ?: DEFAULT_NAMESPACE

    override fun copy(
        config: XmlConfig,
        overriddenSerializer: KSerializer<*>?,
    ): DetachedParent {
        val newElementTypeDescriptor = overriddenSerializer?.let { config.lookupTypeDesc(namespace, it.descriptor) }
            ?: elementTypeDescriptor

        return DetachedParent(namespace, newElementTypeDescriptor, elementUseNameInfo)
    }

    @Suppress("DuplicatedCode")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DetachedParent

        if (isDocumentRoot != other.isDocumentRoot) return false
        if (elementTypeDescriptor != other.elementTypeDescriptor) return false
        if (elementUseNameInfo != other.elementUseNameInfo) return false
        if (elementUseOutputKind != other.elementUseOutputKind) return false
        if (overriddenSerializer != other.overriddenSerializer) return false
        if (namespace != other.namespace) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isDocumentRoot.hashCode()
        result = 31 * result + elementTypeDescriptor.hashCode()
        result = 31 * result + elementUseNameInfo.hashCode()
        result = 31 * result + (elementUseOutputKind?.hashCode() ?: 0)
        result = 31 * result + (overriddenSerializer?.hashCode() ?: 0)
        result = 31 * result + namespace.hashCode()
        return result
    }

    override fun toString(): String = when {
        isDocumentRoot -> "<Root>"
        else -> "<Detached>"
    }

    override val index: Int get() = -1

    override val descriptor: Nothing? get() = null

    override val parentIsInline: Boolean get() = false

    override val elementUseAnnotations: Collection<Annotation> get() = emptyList()


}
