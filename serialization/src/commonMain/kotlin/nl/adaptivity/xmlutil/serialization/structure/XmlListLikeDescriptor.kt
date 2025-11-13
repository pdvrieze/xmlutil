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
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.serialization.XML

public sealed class XmlListLikeDescriptor(
    codecConfig: XML.XmlCodecConfig,
    serializerParent: SafeParentInfo,
    tagParent: SafeParentInfo = serializerParent,
    @ExperimentalXmlUtilApi
    override val defaultPreserveSpace: TypePreserveSpace,
) : XmlDescriptor(codecConfig, serializerParent, tagParent) {

    public open val isListEluded: Boolean = when {
        tagParent is DetachedParent && tagParent.isDocumentRoot -> false
        else -> codecConfig.config.policy.isListEluded(serializerParent, tagParent)
    }

    @ExperimentalSerializationApi
    final override val doInline: Boolean get() = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as XmlListLikeDescriptor

        if (isListEluded != other.isListEluded) return false
        if (defaultPreserveSpace != other.defaultPreserveSpace) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + isListEluded.hashCode()
        result = 31 * result + defaultPreserveSpace.hashCode()
        return result
    }

}
