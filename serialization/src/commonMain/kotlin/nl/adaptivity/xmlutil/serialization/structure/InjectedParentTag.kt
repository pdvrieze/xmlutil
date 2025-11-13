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
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.serialization.OutputKind
import nl.adaptivity.xmlutil.serialization.XmlConfig
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy

internal class InjectedParentTag(
    override val index: Int,
    override val elementTypeDescriptor: XmlTypeDescriptor,
    override val elementUseNameInfo: XmlSerializationPolicy.DeclaredNameInfo,
    override val namespace: Namespace,
    override val elementUseOutputKind: OutputKind? = null,
    override val overriddenSerializer: KSerializer<*>? = null
) : SafeParentInfo {
    override val parentIsInline: Boolean get() = false

    override val descriptor: Nothing? get() = null

    override val elementUseAnnotations: Collection<Annotation>
        get() = emptyList()

    override fun copy(
        config: XmlConfig,
        overriddenSerializer: KSerializer<*>?
    ): InjectedParentTag {
        val newElementTypeDescriptor = overriddenSerializer?.let { config.lookupTypeDesc(namespace, it.descriptor) }
            ?: elementTypeDescriptor
        return InjectedParentTag(
            index,
            newElementTypeDescriptor,
            elementUseNameInfo,
            namespace,
            elementUseOutputKind,
            overriddenSerializer
        )
    }

    @Suppress("DuplicatedCode")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as InjectedParentTag

        if (index != other.index) return false
        if (elementTypeDescriptor != other.elementTypeDescriptor) return false
        if (elementUseNameInfo != other.elementUseNameInfo) return false
        if (namespace != other.namespace) return false
        if (elementUseOutputKind != other.elementUseOutputKind) return false
        if (overriddenSerializer != other.overriddenSerializer) return false

        return true
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + elementTypeDescriptor.hashCode()
        result = 31 * result + elementUseNameInfo.hashCode()
        result = 31 * result + namespace.hashCode()
        result = 31 * result + (elementUseOutputKind?.hashCode() ?: 0)
        result = 31 * result + (overriddenSerializer?.hashCode() ?: 0)
        return result
    }


}
