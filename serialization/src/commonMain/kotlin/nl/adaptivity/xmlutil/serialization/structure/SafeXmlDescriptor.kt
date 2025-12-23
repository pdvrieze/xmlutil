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

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.core.impl.multiplatform.MpJvmDefaultWithCompatibility
import nl.adaptivity.xmlutil.serialization.OutputKind

/**
 * Interface describing a type without providing access to child xml descriptors
 */
@MpJvmDefaultWithCompatibility
public interface SafeXmlDescriptor {
    @ExperimentalSerializationApi
    public val isNullable: Boolean
        get() = serialDescriptor.isNullable

    @ExperimentalSerializationApi
    public val doInline: Boolean

    @ExperimentalSerializationApi
    public val kind: SerialKind
        get() = serialDescriptor.kind

    @ExperimentalXmlUtilApi
    public val defaultPreserveSpace: TypePreserveSpace

    public val typeDescriptor: XmlTypeDescriptor

    public val serializerParent: SafeParentInfo
    public val tagParent: SafeParentInfo
    public val tagName: QName
    public val serialDescriptor: SerialDescriptor
    public val outputKind: OutputKind
    public val elementsCount: Int
    public val overriddenSerializer: KSerializer<*>?

    public val isCData: Boolean get() = false

    @ExperimentalSerializationApi
    public val serialKind: SerialKind

    @ExperimentalSerializationApi
    public fun isElementOptional(index: Int): Boolean =
        serialDescriptor.isElementOptional(index)

}
