/*
 * Copyright (c) 2023.
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

package nl.adaptivity.xmlutil.serialization.impl

import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.OutputKind
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy
import nl.adaptivity.xmlutil.serialization.structure.SafeParentInfo

internal class PrefixWrappingPolicy(val basePolicy: XmlSerializationPolicy, val prefixMap: Map<String, String>) : XmlSerializationPolicy by basePolicy {
    private fun QName.remapPrefix(): QName {
        val prefixMap = prefixMap
        return remapPrefix(prefixMap)
    }

    override fun effectiveName(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        outputKind: OutputKind,
        useName: XmlSerializationPolicy.DeclaredNameInfo
    ): QName {
        return basePolicy.effectiveName(serializerParent, tagParent, outputKind, useName).remapPrefix()
    }

    override fun serialTypeNameToQName(
        typeNameInfo: XmlSerializationPolicy.DeclaredNameInfo,
        parentNamespace: Namespace
    ): QName {
        return basePolicy.serialTypeNameToQName(typeNameInfo, parentNamespace).remapPrefix()
    }

    override fun serialUseNameToQName(
        useNameInfo: XmlSerializationPolicy.DeclaredNameInfo,
        parentNamespace: Namespace
    ): QName {
        return basePolicy.serialUseNameToQName(useNameInfo, parentNamespace).remapPrefix()
    }

    @Deprecated("It is recommended to override serialTypeNameToQName and serialUseNameToQName instead")
    override fun serialNameToQName(serialName: String, parentNamespace: Namespace): QName {
        @Suppress("DEPRECATION")
        return basePolicy.serialNameToQName(serialName, parentNamespace).remapPrefix()
    }

    override fun mapEntryName(serializerParent: SafeParentInfo, isListEluded: Boolean): QName {
        return super.mapEntryName(serializerParent, isListEluded).remapPrefix()
    }
}

internal fun QName.remapPrefix(prefixMap: Map<String, String>): QName {
    return QName(namespaceURI, localPart, prefixMap[namespaceURI] ?: prefix)
}
