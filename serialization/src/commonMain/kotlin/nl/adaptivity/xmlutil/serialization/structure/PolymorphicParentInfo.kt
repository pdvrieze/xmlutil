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
import nl.adaptivity.xmlutil.toNamespace

internal class PolymorphicParentInfo private constructor(
    override val descriptor: XmlPolymorphicDescriptor,
    override val namespace: Namespace,
    override val elementTypeDescriptor: XmlTypeDescriptor,
    override val elementUseNameInfo: XmlSerializationPolicy.DeclaredNameInfo,
    override val elementUseOutputKind: OutputKind? = null,
    override val elementUseAnnotations: Collection<Annotation>,
    override val overriddenSerializer: KSerializer<*>?,
) : SafeParentInfo {

    constructor(
        parentDescriptor: XmlPolymorphicDescriptor,
        elementTypeDescriptor: XmlTypeDescriptor,
        elementUseNameInfo: XmlSerializationPolicy.DeclaredNameInfo,
        elementUseOutputKind: OutputKind? = null,
        overriddenSerializer: KSerializer<*>? = null
    ): this (
        parentDescriptor,
        (elementUseNameInfo.annotatedName ?: parentDescriptor.tagName).toNamespace(),
        elementTypeDescriptor,
        elementUseNameInfo,
        elementUseOutputKind,
        parentDescriptor.serializerParent.elementUseAnnotations,
        overriddenSerializer
    )

    override val index: Int get() = -1 // no valid index
    override val parentIsInline: Boolean get() = false
    override fun copy(
        config: XmlConfig,
        overriddenSerializer: KSerializer<*>?
    ): SafeParentInfo {
        val newElementTypeDescriptor = overriddenSerializer?.let { config.lookupTypeDesc(namespace, it.descriptor) }
            ?: elementTypeDescriptor

        return PolymorphicParentInfo(
            descriptor,
            namespace,
            newElementTypeDescriptor,
            elementUseNameInfo,
            elementUseOutputKind,
            elementUseAnnotations,
            overriddenSerializer?: this.overriddenSerializer
        )
    }

}
